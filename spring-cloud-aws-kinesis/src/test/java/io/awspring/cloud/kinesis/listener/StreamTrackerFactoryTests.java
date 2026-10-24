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

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryResponse;
import software.amazon.awssdk.services.kinesis.model.StreamDescriptionSummary;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.processor.MultiStreamTracker;
import software.amazon.kinesis.processor.SingleStreamTracker;
import software.amazon.kinesis.processor.StreamTracker;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
class StreamTrackerFactoryTests {

	private static final InitialPositionInStreamExtended INITIAL_POSITION = InitialPositionInStreamExtended
			.newInitialPosition(InitialPositionInStream.TRIM_HORIZON);

	private final KinesisAsyncClient kinesisClient = mock(KinesisAsyncClient.class);

	@Test
	@DisplayName("a single stream uses a SingleStreamTracker without describing the stream")
	void singleStreamUsesSingleStreamTracker() {
		StreamTracker tracker = StreamTrackerFactory.createStreamTracker(List.of("orders"), INITIAL_POSITION,
				this.kinesisClient);

		assertThat(tracker).isInstanceOf(SingleStreamTracker.class);
		assertThat(tracker.streamConfigList()).singleElement()
				.satisfies(config -> assertThat(config.streamIdentifier().streamName()).isEqualTo("orders"));
	}

	@Test
	@DisplayName("several streams use a MultiStreamTracker with an identifier resolved per stream")
	void multipleStreamsUseMultiStreamTracker() {
		stubDescribeStreamSummary();

		StreamTracker tracker = StreamTrackerFactory.createStreamTracker(List.of("orders", "shipments"),
				INITIAL_POSITION, this.kinesisClient);

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
				INITIAL_POSITION, this.kinesisClient);

		tracker.streamConfigList();
		tracker.streamConfigList();
		tracker.streamConfigList();

		verify(this.kinesisClient, times(2)).describeStreamSummary(any(Consumer.class));
	}

	@SuppressWarnings("unchecked")
	private void stubDescribeStreamSummary() {
		when(this.kinesisClient.describeStreamSummary(any(Consumer.class))).thenAnswer(invocation -> {
			DescribeStreamSummaryRequest.Builder builder = DescribeStreamSummaryRequest.builder();
			invocation.getArgument(0, Consumer.class).accept(builder);
			String streamName = builder.build().streamName();
			return CompletableFuture.completedFuture(DescribeStreamSummaryResponse.builder()
					.streamDescriptionSummary(StreamDescriptionSummary.builder().streamName(streamName)
							.streamARN("arn:aws:kinesis:eu-west-1:123456789012:stream/" + streamName)
							.streamCreationTimestamp(Instant.ofEpochSecond(1700000000L)).build())
					.build());
		});
	}

}
