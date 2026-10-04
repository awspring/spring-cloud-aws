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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 *
 * @author Matej Nedic
 * @since 4.2.0
 */
final class StreamTrackerFactory {

	private StreamTrackerFactory() {
	}

	static StreamTracker createStreamTracker(Collection<String> streamNames,
			InitialPositionInStreamExtended initialPosition, KinesisAsyncClient kinesisClient) {
		Assert.notEmpty(streamNames, "streamNames must not be empty");
		if (streamNames.size() == 1) {
			return new SingleStreamTracker(streamNames.iterator().next(), initialPosition);
		}
		return new DescribedMultiStreamTracker(streamNames, initialPosition, kinesisClient);
	}

	private static final class DescribedMultiStreamTracker implements MultiStreamTracker {

		private final Collection<String> streamNames;

		private final InitialPositionInStreamExtended initialPosition;

		private final KinesisAsyncClient kinesisClient;

		private final Map<String, StreamConfig> streamConfigCache = new ConcurrentHashMap<>();

		private final FormerStreamsLeasesDeletionStrategy leasesDeletionStrategy = new FormerStreamsLeasesDeletionStrategy.AutoDetectionAndDeferredDeletionStrategy() {

			@Override
			public Duration waitPeriodToDeleteFormerStreams() {
				return Duration.ZERO;
			}

		};

		private DescribedMultiStreamTracker(Collection<String> streamNames,
				InitialPositionInStreamExtended initialPosition, KinesisAsyncClient kinesisClient) {
			this.streamNames = List.copyOf(streamNames);
			this.initialPosition = initialPosition;
			this.kinesisClient = kinesisClient;
		}

		@Override
		public List<StreamConfig> streamConfigList() {
			List<StreamConfig> streamConfigs = new ArrayList<>(this.streamNames.size());
			for (String streamName : this.streamNames) {
				streamConfigs.add(this.streamConfigCache.computeIfAbsent(streamName, this::describeStream));
			}
			return streamConfigs;
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

	}

}
