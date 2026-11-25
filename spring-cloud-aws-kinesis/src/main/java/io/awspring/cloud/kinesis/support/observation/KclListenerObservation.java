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
package io.awspring.cloud.kinesis.support.observation;

import io.micrometer.common.KeyValues;
import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;
import io.micrometer.observation.transport.ReceiverContext;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Observation for the {@code @KclListener} container, following the OpenTelemetry messaging semantic conventions used
 * by the SQS listener observation and the {@link KinesisTemplateObservation Kinesis template observation}.
 * <p>
 * One observation is recorded per record in single-record mode and one per batch in batch mode. In single-record mode
 * the carrier is the converted message, so a {@code traceparent} decoded from embedded headers (see
 * {@link io.awspring.cloud.kinesis.support.converter.EmbeddedHeadersKinesisMessageConverter}) continues the producer
 * trace. A batch mixes records from many producers, so its context carries no headers and starts a new trace.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public final class KclListenerObservation {

	private KclListenerObservation() {
	}

	/**
	 * {@link ObservationDocumentation} for the listener observation.
	 */
	public enum Documentation implements ObservationDocumentation {

		/**
		 * Observation created when the listener processes a record or a batch.
		 */
		LISTENER_OBSERVATION {
			@Override
			public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
				return DefaultConvention.class;
			}

			@Override
			public KeyName[] getLowCardinalityKeyNames() {
				return LowCardinalityTags.values();
			}

			@Override
			public KeyName[] getHighCardinalityKeyNames() {
				return HighCardinalityTags.values();
			}
		};

		/**
		 * Low cardinality tags.
		 */
		public enum LowCardinalityTags implements KeyName {

			/**
			 * Messaging system, always {@code aws_kinesis}.
			 */
			MESSAGING_SYSTEM {
				@Override
				public String asString() {
					return "messaging.system";
				}
			},

			/**
			 * Messaging operation, always {@code receive}.
			 */
			MESSAGING_OPERATION {
				@Override
				public String asString() {
					return "messaging.operation";
				}
			},

			/**
			 * Name of the stream records are read from.
			 */
			MESSAGING_SOURCE_NAME {
				@Override
				public String asString() {
					return "messaging.source.name";
				}
			},

			/**
			 * Source kind, always {@code stream}.
			 */
			MESSAGING_SOURCE_KIND {
				@Override
				public String asString() {
					return "messaging.source.kind";
				}
			},

			/**
			 * Whether the observation covers a single record ({@code false}) or a batch ({@code true}).
			 */
			MESSAGING_BATCH {
				@Override
				public String asString() {
					return "messaging.batch";
				}
			}

		}

		/**
		 * High cardinality tags.
		 */
		public enum HighCardinalityTags implements KeyName {

			/**
			 * Sequence number of a single record.
			 */
			MESSAGE_ID {
				@Override
				public String asString() {
					return "messaging.message.id";
				}
			},

			/**
			 * Partition key of a single record.
			 */
			PARTITION_KEY {
				@Override
				public String asString() {
					return "messaging.kinesis.partition_key";
				}
			},

			/**
			 * Shard the records were read from.
			 */
			SHARD_ID {
				@Override
				public String asString() {
					return "messaging.kinesis.shard_id";
				}
			},

			/**
			 * Number of records in a batch.
			 */
			BATCH_MESSAGE_COUNT {
				@Override
				public String asString() {
					return "messaging.batch.message_count";
				}
			}

		}

	}

	/**
	 * {@link ReceiverContext} for a listener invocation. In single-record mode the carrier is the message, so trace
	 * context carried in its headers is extracted; in batch mode the carrier has no headers and a new trace starts.
	 */
	public static class Context extends ReceiverContext<Message<?>> {

		private final String streamName;

		@Nullable
		private final String shardId;

		@Nullable
		private final String sequenceNumber;

		@Nullable
		private final String partitionKey;

		private final int recordCount;

		private final boolean batch;

		@Nullable
		private final Long millisBehindLatest;

		/**
		 * Context for a single record.
		 * @param message the converted message, used as the propagation carrier.
		 * @param streamName the source stream.
		 * @param shardId the source shard, or {@code null} if not known.
		 * @param sequenceNumber the record sequence number, or {@code null} if not known.
		 * @param partitionKey the record partition key, or {@code null} if not known.
		 * @param millisBehindLatest the lag of the batch the record was read from, or {@code null} if not known.
		 * @return the context.
		 */
		public static Context forRecord(Message<?> message, String streamName, @Nullable String shardId,
				@Nullable String sequenceNumber, @Nullable String partitionKey, @Nullable Long millisBehindLatest) {
			return new Context(message, streamName, shardId, sequenceNumber, partitionKey, 1, false,
					millisBehindLatest);
		}

		/**
		 * Context for a batch. The records come from many producers, so no trace context is extracted.
		 * @param streamName the source stream.
		 * @param shardId the source shard, or {@code null} if not known.
		 * @param recordCount the number of records in the batch.
		 * @param millisBehindLatest the lag of the batch, or {@code null} if not known.
		 * @return the context.
		 */
		public static Context forBatch(String streamName, @Nullable String shardId, int recordCount,
				@Nullable Long millisBehindLatest) {
			return new Context(null, streamName, shardId, null, null, recordCount, true, millisBehindLatest);
		}

		private Context(@Nullable Message<?> message, String streamName, @Nullable String shardId,
				@Nullable String sequenceNumber, @Nullable String partitionKey, int recordCount, boolean batch,
				@Nullable Long millisBehindLatest) {
			super((carrier, key) -> carrier != null ? carrier.getHeaders().get(key, String.class) : null);
			Assert.hasText(streamName, "streamName must not be empty");
			if (message != null) {
				setCarrier(message);
			}
			this.streamName = streamName;
			this.shardId = shardId;
			this.sequenceNumber = sequenceNumber;
			this.partitionKey = partitionKey;
			this.recordCount = recordCount;
			this.batch = batch;
			this.millisBehindLatest = millisBehindLatest;
			setRemoteServiceName("AWS Kinesis");
		}

		public String getStreamName() {
			return this.streamName;
		}

		@Nullable
		public String getShardId() {
			return this.shardId;
		}

		@Nullable
		public String getSequenceNumber() {
			return this.sequenceNumber;
		}

		@Nullable
		public String getPartitionKey() {
			return this.partitionKey;
		}

		public int getRecordCount() {
			return this.recordCount;
		}

		public boolean isBatch() {
			return this.batch;
		}

		/**
		 * How far behind the tip of the shard the batch this observation covers was, from
		 * {@link software.amazon.kinesis.lifecycle.events.ProcessRecordsInput#millisBehindLatest()}. For a single
		 * record it is the lag of the batch the record was read from. It is not a default key value; a custom
		 * {@link Convention} can add it as a tag.
		 * @return the lag in milliseconds, or {@code null} if not known.
		 */
		@Nullable
		public Long getMillisBehindLatest() {
			return this.millisBehindLatest;
		}

	}

	/**
	 * {@link ObservationConvention} for the listener observation. Implement to customize the name or key values.
	 */
	public interface Convention extends ObservationConvention<Context> {

		@Override
		default boolean supportsContext(Observation.Context context) {
			return context instanceof Context;
		}

		@Override
		default String getName() {
			return "spring.aws.kinesis.listener";
		}

	}

	/**
	 * Default {@link Convention}. Extend and override {@link #getCustomLowCardinalityKeyValues(Context)} or
	 * {@link #getCustomHighCardinalityKeyValues(Context)} to add tags.
	 */
	public static class DefaultConvention implements Convention {

		@Override
		public KeyValues getLowCardinalityKeyValues(Context context) {
			return KeyValues
					.of(Documentation.LowCardinalityTags.MESSAGING_SYSTEM.withValue("aws_kinesis"),
							Documentation.LowCardinalityTags.MESSAGING_OPERATION.withValue("receive"),
							Documentation.LowCardinalityTags.MESSAGING_SOURCE_NAME.withValue(context.getStreamName()),
							Documentation.LowCardinalityTags.MESSAGING_SOURCE_KIND.withValue("stream"),
							Documentation.LowCardinalityTags.MESSAGING_BATCH
									.withValue(String.valueOf(context.isBatch())))
					.and(getCustomLowCardinalityKeyValues(context));
		}

		@Override
		public KeyValues getHighCardinalityKeyValues(Context context) {
			KeyValues keyValues = KeyValues.empty();
			if (context.isBatch()) {
				keyValues = withIfPresent(keyValues, Documentation.HighCardinalityTags.SHARD_ID, context.getShardId());
				keyValues = keyValues.and(Documentation.HighCardinalityTags.BATCH_MESSAGE_COUNT
						.withValue(String.valueOf(context.getRecordCount())));
			}
			else {
				keyValues = withIfPresent(keyValues, Documentation.HighCardinalityTags.MESSAGE_ID,
						context.getSequenceNumber());
				keyValues = withIfPresent(keyValues, Documentation.HighCardinalityTags.PARTITION_KEY,
						context.getPartitionKey());
				keyValues = withIfPresent(keyValues, Documentation.HighCardinalityTags.SHARD_ID, context.getShardId());
			}
			return keyValues.and(getCustomHighCardinalityKeyValues(context));
		}

		@Override
		public String getContextualName(Context context) {
			return context.getStreamName() + " receive";
		}

		protected KeyValues getCustomLowCardinalityKeyValues(Context context) {
			return KeyValues.empty();
		}

		protected KeyValues getCustomHighCardinalityKeyValues(Context context) {
			return KeyValues.empty();
		}

		private static KeyValues withIfPresent(KeyValues keyValues, KeyName keyName, @Nullable String value) {
			return value != null ? keyValues.and(keyName.withValue(value)) : keyValues;
		}

	}

}
