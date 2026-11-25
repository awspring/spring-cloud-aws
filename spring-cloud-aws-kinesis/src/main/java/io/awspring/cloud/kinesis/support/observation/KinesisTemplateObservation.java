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
import io.micrometer.observation.transport.SenderContext;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Observation for {@link io.awspring.cloud.kinesis.operations.KinesisTemplate} send operations, following the
 * OpenTelemetry messaging semantic conventions used by the SQS template observation.
 * <p>
 * Kinesis records carry no headers, so no trace context is injected into the sent records; the observation records the
 * producer side of the operation only.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public final class KinesisTemplateObservation {

	private KinesisTemplateObservation() {
	}

	/**
	 * {@link ObservationDocumentation} for the template observation.
	 */
	public enum Documentation implements ObservationDocumentation {

		/**
		 * Observation created when the template sends a single record or a batch.
		 */
		TEMPLATE_OBSERVATION {
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
			 * Messaging operation, always {@code publish}.
			 */
			MESSAGING_OPERATION {
				@Override
				public String asString() {
					return "messaging.operation";
				}
			},

			/**
			 * Name of the stream records are sent to.
			 */
			MESSAGING_DESTINATION_NAME {
				@Override
				public String asString() {
					return "messaging.destination.name";
				}
			},

			/**
			 * Destination kind, always {@code stream}.
			 */
			MESSAGING_DESTINATION_KIND {
				@Override
				public String asString() {
					return "messaging.destination.kind";
				}
			},

			/**
			 * Whether the operation sent a single record ({@code false}) or a batch ({@code true}).
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
			 * Sequence number of a successfully sent single record.
			 */
			MESSAGE_ID {
				@Override
				public String asString() {
					return "messaging.message.id";
				}
			},

			/**
			 * Partition key of a sent single record.
			 */
			PARTITION_KEY {
				@Override
				public String asString() {
					return "messaging.kinesis.partition_key";
				}
			},

			/**
			 * Shard a single record was written to.
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
			},

			/**
			 * Number of records of a batch that Kinesis rejected.
			 */
			BATCH_FAILED_COUNT {
				@Override
				public String asString() {
					return "messaging.kinesis.batch.failed_count";
				}
			}

		}

	}

	/**
	 * {@link SenderContext} for a template send operation.
	 * <p>
	 * Kinesis records carry no native headers, so trace context is only propagated when embedded headers are enabled:
	 * the carrier is a {@link Map} the tracing instrumentation writes {@code traceparent}, {@code tracestate},
	 * {@code b3} and {@code baggage} into, and the template embeds those into the record data. With plain records the
	 * collected map is simply never read, so the observation covers the producer side only.
	 */
	public static class Context extends SenderContext<Map<String, String>> {

		private static final String BAGGAGE_KEY = "baggage";

		private static final String TRACEPARENT_KEY = "traceparent";

		private static final String TRACESTATE_KEY = "tracestate";

		private static final String B3_KEY = "b3";

		private final String streamName;

		@Nullable
		private final String partitionKey;

		private final int recordCount;

		private final boolean batch;

		@Nullable
		private String sequenceNumber;

		@Nullable
		private String shardId;

		private int failedRecordCount;

		/**
		 * Context for a single-record send.
		 * @param streamName the target stream.
		 * @param partitionKey the record partition key.
		 * @return the context.
		 */
		public static Context forRecord(String streamName, String partitionKey) {
			return new Context(streamName, partitionKey, 1, false);
		}

		/**
		 * Context for a batch send.
		 * @param streamName the target stream.
		 * @param recordCount the number of records in the batch.
		 * @return the context.
		 */
		public static Context forBatch(String streamName, int recordCount) {
			return new Context(streamName, null, recordCount, true);
		}

		private Context(String streamName, @Nullable String partitionKey, int recordCount, boolean batch) {
			// Collect only the trace-propagation headers; a record with embedded headers then carries them.
			super((carrier, key, value) -> {
				if (carrier != null && isAllowedKey(key)) {
					carrier.put(key, value);
				}
			});
			setCarrier(new HashMap<>());
			Assert.hasText(streamName, "streamName must not be empty");
			this.streamName = streamName;
			this.partitionKey = partitionKey;
			this.recordCount = recordCount;
			this.batch = batch;
			setRemoteServiceName("AWS Kinesis");
		}

		private static boolean isAllowedKey(String key) {
			return BAGGAGE_KEY.equals(key) || TRACEPARENT_KEY.equals(key) || TRACESTATE_KEY.equals(key)
					|| B3_KEY.equals(key);
		}

		/**
		 * The trace-propagation headers the observation injected ({@code traceparent}, {@code tracestate}, {@code b3},
		 * {@code baggage}), to be embedded in the sent record when embedded headers are enabled. Empty when no tracer
		 * is configured.
		 * @return the injected propagation headers.
		 */
		public Map<String, String> getPropagationHeaders() {
			Map<String, String> carrier = getCarrier();
			return carrier != null ? carrier : Map.of();
		}

		public String getStreamName() {
			return this.streamName;
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

		@Nullable
		public String getSequenceNumber() {
			return this.sequenceNumber;
		}

		@Nullable
		public String getShardId() {
			return this.shardId;
		}

		public int getFailedRecordCount() {
			return this.failedRecordCount;
		}

		/**
		 * Record the outcome of a successful single-record send.
		 * @param sequenceNumber the assigned sequence number.
		 * @param shardId the shard the record was written to.
		 */
		public void setRecordResult(@Nullable String sequenceNumber, @Nullable String shardId) {
			this.sequenceNumber = sequenceNumber;
			this.shardId = shardId;
		}

		/**
		 * Record the number of rejected records of a completed batch send.
		 * @param failedRecordCount the number of failed records.
		 */
		public void setFailedRecordCount(int failedRecordCount) {
			this.failedRecordCount = failedRecordCount;
		}

	}

	/**
	 * {@link ObservationConvention} for the template observation. Implement to customize the name or key values.
	 */
	public interface Convention extends ObservationConvention<Context> {

		@Override
		default boolean supportsContext(Observation.Context context) {
			return context instanceof Context;
		}

		@Override
		default String getName() {
			return "spring.aws.kinesis.template";
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
							Documentation.LowCardinalityTags.MESSAGING_OPERATION.withValue("publish"),
							Documentation.LowCardinalityTags.MESSAGING_DESTINATION_NAME
									.withValue(context.getStreamName()),
							Documentation.LowCardinalityTags.MESSAGING_DESTINATION_KIND.withValue("stream"),
							Documentation.LowCardinalityTags.MESSAGING_BATCH
									.withValue(String.valueOf(context.isBatch())))
					.and(getCustomLowCardinalityKeyValues(context));
		}

		@Override
		public KeyValues getHighCardinalityKeyValues(Context context) {
			KeyValues keyValues = KeyValues.empty();
			if (context.isBatch()) {
				keyValues = keyValues.and(
						Documentation.HighCardinalityTags.BATCH_MESSAGE_COUNT
								.withValue(String.valueOf(context.getRecordCount())),
						Documentation.HighCardinalityTags.BATCH_FAILED_COUNT
								.withValue(String.valueOf(context.getFailedRecordCount())));
			}
			else {
				keyValues = withIfPresent(keyValues, Documentation.HighCardinalityTags.PARTITION_KEY,
						context.getPartitionKey());
				keyValues = withIfPresent(keyValues, Documentation.HighCardinalityTags.MESSAGE_ID,
						context.getSequenceNumber());
				keyValues = withIfPresent(keyValues, Documentation.HighCardinalityTags.SHARD_ID, context.getShardId());
			}
			return keyValues.and(getCustomHighCardinalityKeyValues(context));
		}

		@Override
		public String getContextualName(Context context) {
			return context.getStreamName() + " send";
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
