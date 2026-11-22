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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.StreamDescriptionSummary;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.common.StreamConfig;
import software.amazon.kinesis.common.StreamIdentifier;
import software.amazon.kinesis.processor.FormerStreamsLeasesDeletionStrategy;
import software.amazon.kinesis.processor.MultiStreamTracker;
import software.amazon.kinesis.processor.SingleStreamTracker;
import software.amazon.kinesis.processor.StreamTracker;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
final class StreamTrackerFactory {

	private static final Logger logger = LoggerFactory.getLogger(StreamTrackerFactory.class);

	private StreamTrackerFactory() {
	}

	/**
	 * Create the tracker for the given streams.
	 * @param streamNames the streams to consume.
	 * @param initialPosition the position to start from when a shard has no checkpoint.
	 * @param kinesisClient the client used to resolve the identifiers of a multi-stream application.
	 * @param formerStreamsLeaseDeletionDelay how long a multi-stream application waits before deleting the leases of a
	 *     stream it no longer consumes; ignored for a single stream.
	 * @return the stream tracker.
	 */
	static StreamTracker createStreamTracker(Collection<String> streamNames,
			InitialPositionInStreamExtended initialPosition, KinesisAsyncClient kinesisClient,
			Duration formerStreamsLeaseDeletionDelay) {
		return createStreamTracker(streamNames, initialPosition, kinesisClient, formerStreamsLeaseDeletionDelay,
				Clock.systemUTC());
	}

	static StreamTracker createStreamTracker(Collection<String> streamNames,
			InitialPositionInStreamExtended initialPosition, KinesisAsyncClient kinesisClient,
			Duration formerStreamsLeaseDeletionDelay, Clock clock) {
		Assert.notEmpty(streamNames, "streamNames must not be empty");
		Assert.notNull(formerStreamsLeaseDeletionDelay, "formerStreamsLeaseDeletionDelay must not be null");
		if (streamNames.size() == 1) {
			return new SingleStreamTracker(streamNames.iterator().next(), initialPosition);
		}
		return new DescribedMultiStreamTracker(streamNames, initialPosition, kinesisClient,
				formerStreamsLeaseDeletionDelay, clock);
	}

	private static final class DescribedMultiStreamTracker implements MultiStreamTracker {

		/**
		 * How long a resolved stream identifier is reused. KCL asks for the stream list on every stream sync (about
		 * once a minute), so caching keeps DescribeStreamSummary calls low, while expiring the entries lets a stream
		 * that was deleted and recreated under the same name be picked up with its new creation time.
		 */
		private static final Duration STREAM_CONFIG_REFRESH_INTERVAL = Duration.ofMinutes(5);

		private final Collection<String> streamNames;

		private final InitialPositionInStreamExtended initialPosition;

		private final KinesisAsyncClient kinesisClient;

		private final Clock clock;

		private final Map<String, CachedStreamConfig> streamConfigCache = new ConcurrentHashMap<>();

		private final FormerStreamsLeasesDeletionStrategy leasesDeletionStrategy;

		private DescribedMultiStreamTracker(Collection<String> streamNames,
				InitialPositionInStreamExtended initialPosition, KinesisAsyncClient kinesisClient,
				Duration formerStreamsLeaseDeletionDelay, Clock clock) {
			this.streamNames = List.copyOf(streamNames);
			this.clock = clock;
			this.initialPosition = initialPosition;
			this.kinesisClient = kinesisClient;
			// The deferral protects checkpoints during rolling deployments: a leader
			// still running the previous stream
			// list would otherwise delete the leases of a stream the new version just
			// added.
			this.leasesDeletionStrategy = new FormerStreamsLeasesDeletionStrategy.AutoDetectionAndDeferredDeletionStrategy() {

				@Override
				public Duration waitPeriodToDeleteFormerStreams() {
					return formerStreamsLeaseDeletionDelay;
				}

			};
		}

		@Override
		public List<StreamConfig> streamConfigList() {
			List<StreamConfig> streamConfigs = new ArrayList<>(this.streamNames.size());
			for (String streamName : this.streamNames) {
				streamConfigs.add(resolveStreamConfig(streamName));
			}
			return streamConfigs;
		}

		private StreamConfig resolveStreamConfig(String streamName) {
			Instant now = this.clock.instant();
			CachedStreamConfig cached = this.streamConfigCache.get(streamName);
			if (cached != null && now.isBefore(cached.refreshAt())) {
				return cached.config();
			}
			StreamConfig config;
			try {
				config = describeStream(streamName);
			}
			catch (RuntimeException ex) {
				if (cached == null) {
					throw ex;
				}
				// Keep consuming with the last known identifier rather than failing the
				// whole stream sync; the
				// refresh is retried on the next sync.
				logger.warn("Failed to refresh the identifier of stream '{}', keeping the cached one: {}", streamName,
						ex.getMessage());
				return cached.config();
			}
			this.streamConfigCache.put(streamName,
					new CachedStreamConfig(config, now.plus(STREAM_CONFIG_REFRESH_INTERVAL)));
			return config;
		}

		private StreamConfig describeStream(String streamName) {
			StreamDescriptionSummary summary = this.kinesisClient
					.describeStreamSummary(request -> request.streamName(streamName)).join().streamDescriptionSummary();
			StreamIdentifier streamIdentifier = StreamIdentifier.multiStreamInstance(
					Arn.fromString(summary.streamARN()), summary.streamCreationTimestamp().getEpochSecond());
			return new StreamConfig(streamIdentifier, this.initialPosition);
		}

		@Override
		public FormerStreamsLeasesDeletionStrategy formerStreamsLeasesDeletionStrategy() {
			return this.leasesDeletionStrategy;
		}

		private record CachedStreamConfig(StreamConfig config, Instant refreshAt) {
		}

	}

}
