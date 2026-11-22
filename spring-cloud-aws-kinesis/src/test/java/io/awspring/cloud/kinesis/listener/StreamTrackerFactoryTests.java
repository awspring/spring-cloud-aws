/*
 * Copyright 2013-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.kinesis.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryResponse;
import software.amazon.awssdk.services.kinesis.model.StreamDescriptionSummary;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.processor.FormerStreamsLeasesDeletionStrategy;
import software.amazon.kinesis.processor.MultiStreamTracker;
import software.amazon.kinesis.processor.SingleStreamTracker;
import software.amazon.kinesis.processor.StreamTracker;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
class StreamTrackerFactoryTests {

	private static final InitialPositionInStreamExtended INITIAL_POSITION = InitialPositionInStreamExtended
			.newInitialPosition(InitialPositionInStream.TRIM_HORIZON);

	private static final Duration DELETION_DELAY = Duration.ofHours(1);

	private final KinesisAsyncClient kinesisClient = mock(KinesisAsyncClient.class);

	@Test
	@DisplayName("a single stream uses a SingleStreamTracker without describing the stream")
	void singleStreamUsesSingleStreamTracker() {
		StreamTracker tracker = StreamTrackerFactory.createStreamTracker(List.of("orders"), INITIAL_POSITION,
				this.kinesisClient, DELETION_DELAY);

		assertThat(tracker).isInstanceOf(SingleStreamTracker.class);
		assertThat(tracker.streamConfigList()).singleElement()
				.satisfies(config -> assertThat(config.streamIdentifier().streamName()).isEqualTo("orders"));
	}

	@Test
	@DisplayName("several streams use a MultiStreamTracker with an identifier resolved per stream")
	void multipleStreamsUseMultiStreamTracker() {
		stubDescribeStreamSummary();

		StreamTracker tracker = StreamTrackerFactory.createStreamTracker(List.of("orders", "shipments"),
				INITIAL_POSITION, this.kinesisClient, DELETION_DELAY);

		assertThat(tracker).isInstanceOf(MultiStreamTracker.class);
		assertThat(tracker.streamConfigList()).extracting(config -> config.streamIdentifier().streamName())
				.containsExactly("orders", "shipments");
		assertThat(tracker.streamConfigList()).allSatisfy(config -> {
			assertThat(config.streamIdentifier().accountIdOptional()).contains("123456789012");
			assertThat(config.streamIdentifier().streamCreationEpochOptional()).isPresent();
			assertThat(config.initialPositionInStreamExtended()).isEqualTo(INITIAL_POSITION);
		});
		assertThat(((MultiStreamTracker) tracker).formerStreamsLeasesDeletionStrategy()).isNotNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	@DisplayName("stream identifiers are resolved once, since KCL asks for them on every shard-sync cycle")
	void describesEachStreamOnlyOnce() {
		stubDescribeStreamSummary();
		StreamTracker tracker = StreamTrackerFactory.createStreamTracker(List.of("orders", "shipments"),
				INITIAL_POSITION, this.kinesisClient, DELETION_DELAY);

		tracker.streamConfigList();
		tracker.streamConfigList();
		tracker.streamConfigList();

		verify(this.kinesisClient, times(2)).describeStreamSummary(any(Consumer.class));
	}

	@Test
	@DisplayName("the leases of a removed stream are deleted only after the configured delay")
	void defersFormerStreamLeaseDeletionByTheConfiguredDelay() {
		StreamTracker tracker = StreamTrackerFactory.createStreamTracker(List.of("orders", "shipments"),
				INITIAL_POSITION, this.kinesisClient, Duration.ofHours(6));

		FormerStreamsLeasesDeletionStrategy strategy = ((MultiStreamTracker) tracker)
				.formerStreamsLeasesDeletionStrategy();
		assertThat(strategy)
				.isInstanceOf(FormerStreamsLeasesDeletionStrategy.AutoDetectionAndDeferredDeletionStrategy.class);
		assertThat(strategy.waitPeriodToDeleteFormerStreams()).isEqualTo(Duration.ofHours(6));
	}

	@SuppressWarnings("unchecked")
	@Test
	@DisplayName("stream identifiers are refreshed after the refresh interval, picking up a recreated stream")
	void refreshesStreamIdentifiers() {
		AtomicLong creationEpoch = new AtomicLong(1700000000L);
		stubDescribeStreamSummary(creationEpoch);
		MutableClock clock = new MutableClock();
		StreamTracker tracker = StreamTrackerFactory.createStreamTracker(List.of("orders", "shipments"),
				INITIAL_POSITION, this.kinesisClient, DELETION_DELAY, clock);
		assertThat(creationEpochs(tracker)).containsOnly(1700000000L);

		creationEpoch.set(1800000000L);
		clock.advance(Duration.ofMinutes(4));
		assertThat(creationEpochs(tracker)).containsOnly(1700000000L);

		clock.advance(Duration.ofMinutes(2));
		assertThat(creationEpochs(tracker)).containsOnly(1800000000L);
		verify(this.kinesisClient, times(4)).describeStreamSummary(any(Consumer.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	@DisplayName("a failed refresh keeps the cached stream identifier")
	void keepsCachedIdentifierWhenRefreshFails() {
		stubDescribeStreamSummary();
		MutableClock clock = new MutableClock();
		StreamTracker tracker = StreamTrackerFactory.createStreamTracker(List.of("orders", "shipments"),
				INITIAL_POSITION, this.kinesisClient, DELETION_DELAY, clock);
		tracker.streamConfigList();

		when(this.kinesisClient.describeStreamSummary(any(Consumer.class)))
				.thenReturn(CompletableFuture.failedFuture(new IllegalStateException("throttled")));
		clock.advance(Duration.ofMinutes(10));

		assertThat(creationEpochs(tracker)).containsOnly(1700000000L);
	}

	private static List<Long> creationEpochs(StreamTracker tracker) {
		return tracker.streamConfigList().stream()
				.map(config -> config.streamIdentifier().streamCreationEpochOptional().orElseThrow()).toList();
	}

	private void stubDescribeStreamSummary() {
		stubDescribeStreamSummary(new AtomicLong(1700000000L));
	}

	private static final class MutableClock extends Clock {

		private Instant now = Instant.parse("2026-01-01T00:00:00Z");

		void advance(Duration duration) {
			this.now = this.now.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return this.now;
		}

	}

	@SuppressWarnings("unchecked")
	private void stubDescribeStreamSummary(AtomicLong creationEpoch) {
		when(this.kinesisClient.describeStreamSummary(any(Consumer.class))).thenAnswer(invocation -> {
			DescribeStreamSummaryRequest.Builder builder = DescribeStreamSummaryRequest.builder();
			invocation.getArgument(0, Consumer.class).accept(builder);
			String streamName = builder.build().streamName();
			return CompletableFuture.completedFuture(DescribeStreamSummaryResponse.builder()
				.streamDescriptionSummary(StreamDescriptionSummary.builder()
					.streamName(streamName)
					.streamARN("arn:aws:kinesis:eu-west-1:123456789012:stream/" + streamName)
					.streamCreationTimestamp(Instant.ofEpochSecond(creationEpoch.get()))
					.build())
				.build());
		});
	}

}
