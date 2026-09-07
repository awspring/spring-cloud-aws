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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointer;
import io.awspring.cloud.kinesis.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.kinesis.listener.errorhandler.LoggingErrorHandler;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders;
import io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
class KclShardRecordProcessorTests {

	private static final ErrorHandler RETHROWING = (message, throwable) -> {
		throw new IllegalStateException(throwable);
	};

	@Test
	@DisplayName("converts each record to a Spring Message with Kinesis headers and the checkpointer")
	void convertsRecordsToMessages() {
		List<Message<?>> received = new ArrayList<>();
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.MANUAL, received::add, RETHROWING);

		processor.processRecords(input(checkpointer, record("pk-1", "seq-1", "hello")));

		assertThat(received).hasSize(1);
		Message<?> message = received.get(0);
		assertThat((byte[]) message.getPayload()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
		assertThat(message.getHeaders().get(KinesisMessageHeaders.PARTITION_KEY)).isEqualTo("pk-1");
		assertThat(message.getHeaders().get(KinesisMessageHeaders.SHARD_ID)).isEqualTo("shard-1");
		assertThat(message.getHeaders().get(KinesisMessageHeaders.STREAM_NAME)).isEqualTo("my-stream");
		assertThat(message.getHeaders().get(KinesisMessageHeaders.CHECKPOINTER)).isInstanceOf(KclCheckpointer.class);
	}

	@Test
	@DisplayName("RECORD checkpoint mode checkpoints after each record")
	void recordModeCheckpointsPerRecord() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.RECORD, message -> {
		}, RETHROWING);

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a"), record("pk", "seq-2", "b")));

		verify(checkpointer).checkpoint("seq-1");
		verify(checkpointer).checkpoint("seq-2");
	}

	@Test
	@DisplayName("BATCH checkpoint mode checkpoints once after the batch")
	void batchModeCheckpointsOnce() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.BATCH, message -> {
		}, RETHROWING);

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a"), record("pk", "seq-2", "b")));

		verify(checkpointer, times(1)).checkpoint();
		verify(checkpointer, never()).checkpoint(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	@DisplayName("MANUAL checkpoint mode never checkpoints automatically")
	void manualModeNeverCheckpoints() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.MANUAL, message -> {
		}, RETHROWING);

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a")));

		verifyNoInteractions(checkpointer);
	}

	@Test
	@DisplayName("BATCH listener mode delivers the whole batch once and checkpoints once")
	void batchListenerModeDeliversWholeBatch() throws Exception {
		List<Collection<Message<?>>> batches = new ArrayList<>();
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(),
				KclListenerMode.BATCH, KclCheckpointMode.BATCH, 1000L, Duration.ofSeconds(60), null, batches::add,
				RETHROWING, "my-stream");
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a"), record("pk", "seq-2", "b")));

		assertThat(batches).hasSize(1);
		assertThat(batches.get(0)).hasSize(2);
		verify(checkpointer, times(1)).checkpoint();
	}

	@Test
	@DisplayName("a swallowing error handler lets processing continue and checkpoint")
	void swallowingErrorHandlerAllowsCheckpoint() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Throwable> handled = new ArrayList<>();
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.BATCH, message -> {
			throw new RuntimeException("boom");
		}, (message, throwable) -> handled.add(throwable));

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a")));

		assertThat(handled).hasSize(1);
		verify(checkpointer, times(1)).checkpoint();
	}

	@Test
	@DisplayName("the default logging error handler rethrows, preventing checkpoint")
	void loggingErrorHandlerRethrowsAndPreventsCheckpoint() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.BATCH, message -> {
			throw new RuntimeException("boom");
		}, new LoggingErrorHandler());

		assertThatThrownBy(() -> processor.processRecords(input(checkpointer, record("pk", "seq-1", "a"))))
				.isInstanceOf(RuntimeException.class);
		verify(checkpointer, never()).checkpoint();
	}

	@Test
	@DisplayName("shardEnded always checkpoints")
	void shardEndedCheckpoints() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.MANUAL, message -> {
		}, RETHROWING);

		processor.shardEnded(ShardEndedInput.builder().checkpointer(checkpointer).build());

		verify(checkpointer).checkpoint();
	}

	@Test
	@DisplayName("aggregated records (subsequence > 0) checkpoint with the subsequence number")
	void aggregatedRecordCheckpointsWithSubsequence() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.RECORD, message -> {
		}, RETHROWING);

		processor.processRecords(input(checkpointer, aggregatedRecord("pk", "seq-1", 7L, "a")));

		verify(checkpointer).checkpoint("seq-1", 7L);
	}

	@Test
	@DisplayName("a record that fails to convert is routed to the error handler")
	void conversionFailureRoutedToErrorHandler() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KinesisMessagingMessageConverter converter = mock(KinesisMessagingMessageConverter.class);
		KinesisClientRecord good = record("pk", "seq-1", "a");
		KinesisClientRecord bad = record("pk", "seq-2", "b");
		Message<byte[]> goodMessage = new KinesisMessagingMessageConverter().toMessagingMessage(good, "shard-1",
				"my-stream");
		when(converter.toMessagingMessage(good, "shard-1", "my-stream")).thenReturn(goodMessage);
		when(converter.toMessagingMessage(bad, "shard-1", "my-stream"))
				.thenThrow(new IllegalArgumentException("bad record"));
		List<Message<?>> delivered = new ArrayList<>();
		List<Throwable> handled = new ArrayList<>();
		KclShardRecordProcessor processor = new KclShardRecordProcessor(converter, KclListenerMode.SINGLE_RECORD,
				KclCheckpointMode.BATCH, 1000L, Duration.ofSeconds(60), delivered::add, null,
				(message, throwable) -> handled.add(throwable), "my-stream");
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer, good, bad));

		assertThat(delivered).hasSize(1);
		assertThat(handled).hasSize(1);
		assertThat(handled.get(0)).isInstanceOf(IllegalArgumentException.class);
		verify(checkpointer, times(1)).checkpoint();
	}

	@Test
	@DisplayName("PERIODIC checkpoint mode checkpoints on an empty batch once the interval elapsed with pending records")
	void periodicModeCheckpointsWhileIdle() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(),
				KclListenerMode.SINGLE_RECORD, KclCheckpointMode.PERIODIC, 1000L, Duration.ofMillis(200), message -> {
				}, null, RETHROWING, "my-stream");
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a")));
		verifyNoInteractions(checkpointer);

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			processor.processRecords(input(checkpointer));
			verify(checkpointer, times(1)).checkpoint();
		});
	}

	@Test
	@DisplayName("PERIODIC checkpoint mode does not checkpoint an idle shard with no pending records")
	void periodicModeSkipsIdleShardWithoutPendingRecords() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(),
				KclListenerMode.SINGLE_RECORD, KclCheckpointMode.PERIODIC, 1000L, Duration.ZERO, message -> {
				}, null, RETHROWING, "my-stream");
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer));

		verifyNoInteractions(checkpointer);
	}

	private KclShardRecordProcessor singleRecordProcessor(KclCheckpointMode checkpointMode, MessageListener listener,
			ErrorHandler errorHandler) {
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(),
				KclListenerMode.SINGLE_RECORD, checkpointMode, 1000L, Duration.ofSeconds(60), listener, null,
				errorHandler, "my-stream");
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());
		return processor;
	}

	private ProcessRecordsInput input(RecordProcessorCheckpointer checkpointer, KinesisClientRecord... records) {
		return ProcessRecordsInput.builder().records(List.of(records)).checkpointer(checkpointer).build();
	}

	private KinesisClientRecord record(String partitionKey, String sequenceNumber, String body) {
		return KinesisClientRecord.builder().partitionKey(partitionKey).sequenceNumber(sequenceNumber)
				.subSequenceNumber(0L).approximateArrivalTimestamp(Instant.ofEpochMilli(1000L))
				.data(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8))).build();
	}

	private KinesisClientRecord aggregatedRecord(String partitionKey, String sequenceNumber, long subSequenceNumber,
			String body) {
		return KinesisClientRecord.builder().partitionKey(partitionKey).sequenceNumber(sequenceNumber)
				.subSequenceNumber(subSequenceNumber).approximateArrivalTimestamp(Instant.ofEpochMilli(1000L))
				.data(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8))).build();
	}

}
