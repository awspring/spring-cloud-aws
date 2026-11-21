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
import static org.mockito.Mockito.mock;

import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.kinesis.listener.errorhandler.LoggingErrorHandler;
import io.awspring.cloud.kinesis.support.converter.EmbeddedHeadersKinesisMessageConverter;
import io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter;
import io.awspring.cloud.kinesis.support.observation.KclListenerObservation;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
class KclListenerObservationTests {

	private static final ErrorHandler RETHROWING = failedDelivery -> {
		throw new IllegalStateException(failedDelivery.getCause());
	};

	private final TestObservationRegistry registry = TestObservationRegistry.create();

	@Test
	@DisplayName("one observation per record is recorded in single-record mode with the Kinesis key values")
	void singleRecordObservation() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = singleRecordProcessor(message -> {
		}, RETHROWING);

		processor.processRecords(input(checkpointer, record("pk-1", "seq-1", "a"), record("pk-2", "seq-2", "b")));

		TestObservationRegistryAssert.then(this.registry).hasNumberOfObservationsEqualTo(2);
		TestObservationRegistryAssert.then(this.registry).hasObservationWithNameEqualTo("spring.aws.kinesis.listener")
				.that().hasContextualNameEqualTo("my-stream receive").hasBeenStopped()
				.hasLowCardinalityKeyValue("messaging.system", "aws_kinesis")
				.hasLowCardinalityKeyValue("messaging.operation", "receive")
				.hasLowCardinalityKeyValue("messaging.source.name", "my-stream")
				.hasLowCardinalityKeyValue("messaging.source.kind", "stream")
				.hasLowCardinalityKeyValue("messaging.batch", "false")
				.hasHighCardinalityKeyValue("messaging.message.id", "seq-1")
				.hasHighCardinalityKeyValue("messaging.kinesis.partition_key", "pk-1")
				.hasHighCardinalityKeyValue("messaging.kinesis.shard_id", "shard-1");
	}

	@Test
	@DisplayName("one observation per batch is recorded in batch mode with the batch count")
	void batchObservation() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(), null,
				KclListenerMode.BATCH, KclCheckpointMode.BATCH, 1000L, Duration.ofSeconds(60), null, messages -> {
				}, RETHROWING, "my-stream", this.registry, null, null, null, null, null, null);
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer, record("pk-1", "seq-1", "a"), record("pk-2", "seq-2", "b")));

		TestObservationRegistryAssert.then(this.registry).hasNumberOfObservationsEqualTo(1);
		TestObservationRegistryAssert.then(this.registry).hasObservationWithNameEqualTo("spring.aws.kinesis.listener")
				.that().hasBeenStopped().hasLowCardinalityKeyValue("messaging.batch", "true")
				.hasLowCardinalityKeyValue("messaging.source.name", "my-stream")
				.hasHighCardinalityKeyValue("messaging.kinesis.shard_id", "shard-1")
				.hasHighCardinalityKeyValue("messaging.batch.message_count", "2");
	}

	@Test
	@DisplayName("an unresolved failure records the original error on the observation")
	void unresolvedFailureRecordsError() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		RuntimeException boom = new RuntimeException("boom");
		KclShardRecordProcessor processor = singleRecordProcessor(message -> {
			throw boom;
		}, new LoggingErrorHandler());

		processor.processRecords(input(checkpointer, record("pk-1", "seq-1", "a")));

		TestObservationRegistryAssert.then(this.registry).hasSingleObservationThat().hasBeenStopped().assertThatError()
				.isSameAs(boom);
	}

	@Test
	@DisplayName("a resolved failure records no error on the observation")
	void resolvedFailureRecordsNoError() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = singleRecordProcessor(message -> {
			throw new RuntimeException("boom");
		}, failedDelivery -> {
		});

		processor.processRecords(input(checkpointer, record("pk-1", "seq-1", "a")));

		TestObservationRegistryAssert.then(this.registry).hasSingleObservationThat().hasBeenStopped()
				.doesNotHaveError();
	}

	@Test
	@DisplayName("a listener call runs inside the observation scope, so its work becomes a child observation")
	void listenerRunsInsideObservationScope() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		AtomicReference<Observation> currentDuringListener = new AtomicReference<>();
		KclShardRecordProcessor processor = singleRecordProcessor(
				message -> currentDuringListener.set(this.registry.getCurrentObservation()), RETHROWING);

		processor.processRecords(input(checkpointer, record("pk-1", "seq-1", "a")));

		assertThat(currentDuringListener.get()).isNotNull();
	}

	@Test
	@DisplayName("a traceparent decoded from embedded headers continues the producer trace")
	void continuesTraceFromEmbeddedHeaders() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Message<?>> received = new ArrayList<>();
		EmbeddedHeadersKinesisMessageConverter converter = new EmbeddedHeadersKinesisMessageConverter(
				new KinesisMessagingMessageConverter());
		KclShardRecordProcessor processor = new KclShardRecordProcessor(converter, null, KclListenerMode.SINGLE_RECORD,
				KclCheckpointMode.MANUAL, 1000L, Duration.ofSeconds(60), received::add, null, RETHROWING, "my-stream",
				this.registry, null, null, null, null, null, null);
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		byte[] embedded = new EmbeddedJsonHeadersMessageMapper().fromMessage(MessageBuilder
				.withPayload("hello".getBytes(StandardCharsets.UTF_8)).setHeader("traceparent", "00-tp-sp-01").build());

		processor.processRecords(input(checkpointer, record("pk-1", "seq-1", embedded)));

		assertThat(received).singleElement()
				.satisfies(message -> assertThat(message.getHeaders().get("traceparent")).isEqualTo("00-tp-sp-01"));
		TestObservationRegistryAssert.then(this.registry).hasSingleObservationThat().hasBeenStopped();
	}

	@Test
	@DisplayName("a custom convention replaces the default key values")
	void customConventionIsApplied() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclListenerObservation.Convention convention = new KclListenerObservation.DefaultConvention() {
			@Override
			protected KeyValues getCustomLowCardinalityKeyValues(KclListenerObservation.Context context) {
				return KeyValues.of("team", "orders");
			}
		};
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(), null,
				KclListenerMode.SINGLE_RECORD, KclCheckpointMode.MANUAL, 1000L, Duration.ofSeconds(60), message -> {
				}, null, RETHROWING, "my-stream", this.registry, convention, null, null, null, null, null);
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer, record("pk-1", "seq-1", "a")));

		TestObservationRegistryAssert.then(this.registry).hasSingleObservationThat()
				.hasLowCardinalityKeyValue("team", "orders")
				.hasLowCardinalityKeyValue("messaging.system", "aws_kinesis");
	}

	@Test
	@DisplayName("a custom convention can add millisBehindLatest from the single-record context")
	void customConventionAddsMillisBehindLatestForRecord() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclListenerObservation.Convention convention = new KclListenerObservation.DefaultConvention() {
			@Override
			protected KeyValues getCustomHighCardinalityKeyValues(KclListenerObservation.Context context) {
				Long lag = context.getMillisBehindLatest();
				return lag != null ? KeyValues.of("messaging.kinesis.millis_behind_latest", String.valueOf(lag))
						: KeyValues.empty();
			}
		};
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(), null,
				KclListenerMode.SINGLE_RECORD, KclCheckpointMode.MANUAL, 1000L, Duration.ofSeconds(60), message -> {
				}, null, RETHROWING, "my-stream", this.registry, convention, null, null, null, null, null);
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(ProcessRecordsInput.builder().records(List.of(record("pk-1", "seq-1", "a")))
				.checkpointer(checkpointer).millisBehindLatest(4321L).build());

		TestObservationRegistryAssert.then(this.registry).hasSingleObservationThat()
				.hasHighCardinalityKeyValue("messaging.kinesis.millis_behind_latest", "4321");
	}

	@Test
	@DisplayName("a custom convention can add millisBehindLatest from the batch context")
	void customConventionAddsMillisBehindLatestForBatch() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclListenerObservation.Convention convention = new KclListenerObservation.DefaultConvention() {
			@Override
			protected KeyValues getCustomHighCardinalityKeyValues(KclListenerObservation.Context context) {
				Long lag = context.getMillisBehindLatest();
				return lag != null ? KeyValues.of("messaging.kinesis.millis_behind_latest", String.valueOf(lag))
						: KeyValues.empty();
			}
		};
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(), null,
				KclListenerMode.BATCH, KclCheckpointMode.BATCH, 1000L, Duration.ofSeconds(60), null, messages -> {
				}, RETHROWING, "my-stream", this.registry, convention, null, null, null, null, null);
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(ProcessRecordsInput.builder().records(List.of(record("pk-1", "seq-1", "a")))
				.checkpointer(checkpointer).millisBehindLatest(99L).build());

		TestObservationRegistryAssert.then(this.registry).hasSingleObservationThat()
				.hasHighCardinalityKeyValue("messaging.kinesis.millis_behind_latest", "99");
	}

	@Test
	@DisplayName("millisBehindLatest is not a default key value")
	void millisBehindLatestIsNotADefaultKeyValue() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = singleRecordProcessor(message -> {
		}, RETHROWING);

		processor.processRecords(ProcessRecordsInput.builder().records(List.of(record("pk-1", "seq-1", "a")))
				.checkpointer(checkpointer).millisBehindLatest(4321L).build());

		TestObservationRegistryAssert.then(this.registry).hasSingleObservationThat()
				.doesNotHaveHighCardinalityKeyValueWithKey("messaging.kinesis.millis_behind_latest");
	}

	@Test
	@DisplayName("a record that fails conversion is routed to the error handler without an observation")
	void conversionFailureIsNotObserved() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KinesisMessagingMessageConverter converter = mock(KinesisMessagingMessageConverter.class);
		KinesisClientRecord bad = record("pk-1", "seq-1", "a");
		org.mockito.Mockito.when(converter.toMessagingMessage(bad, "shard-1", "my-stream"))
				.thenThrow(new IllegalArgumentException("bad record"));
		List<Throwable> handled = new ArrayList<>();
		KclShardRecordProcessor processor = new KclShardRecordProcessor(converter, null, KclListenerMode.SINGLE_RECORD,
				KclCheckpointMode.MANUAL, 1000L, Duration.ofSeconds(60), message -> {
				}, null, failedDelivery -> handled.add(failedDelivery.getCause()), "my-stream", this.registry, null,
				null, null, null, null, null);
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer, bad));

		assertThat(handled).hasSize(1);
		TestObservationRegistryAssert.then(this.registry).hasNumberOfObservationsEqualTo(0);
	}

	private KclShardRecordProcessor singleRecordProcessor(MessageListener listener, ErrorHandler errorHandler) {
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(), null,
				KclListenerMode.SINGLE_RECORD, KclCheckpointMode.MANUAL, 1000L, Duration.ofSeconds(60), listener, null,
				errorHandler, "my-stream", this.registry, null, null, null, null, null, null);
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());
		return processor;
	}

	private ProcessRecordsInput input(RecordProcessorCheckpointer checkpointer, KinesisClientRecord... records) {
		return ProcessRecordsInput.builder().records(List.of(records)).checkpointer(checkpointer).build();
	}

	private KinesisClientRecord record(String partitionKey, String sequenceNumber, String body) {
		return record(partitionKey, sequenceNumber, body.getBytes(StandardCharsets.UTF_8));
	}

	private KinesisClientRecord record(String partitionKey, String sequenceNumber, byte[] body) {
		return KinesisClientRecord.builder().partitionKey(partitionKey).sequenceNumber(sequenceNumber)
				.subSequenceNumber(0L).approximateArrivalTimestamp(Instant.ofEpochMilli(1000L))
				.data(ByteBuffer.wrap(body)).build();
	}

}
