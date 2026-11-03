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
import static org.assertj.core.api.Assertions.assertThatCode;
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
import io.awspring.cloud.kinesis.listener.errorhandler.FailedDelivery;
import io.awspring.cloud.kinesis.listener.errorhandler.LoggingErrorHandler;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageConverter;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders;
import io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
class KclShardRecordProcessorTests {

	private static final ErrorHandler RETHROWING = failedDelivery -> {
		throw new IllegalStateException(failedDelivery.getCause());
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
		}, failedDelivery -> handled.add(failedDelivery.getCause()));

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a")));

		assertThat(handled).hasSize(1);
		verify(checkpointer, times(1)).checkpoint();
	}

	@Test
	@DisplayName("the default logging error handler halts the shard without throwing to KCL or checkpointing")
	void loggingErrorHandlerHaltsShardWithoutThrowing() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Message<?>> received = new ArrayList<>();
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.BATCH, message -> {
			received.add(message);
			throw new RuntimeException("boom");
		}, new LoggingErrorHandler());

		assertThatCode(() -> processor.processRecords(input(checkpointer, record("pk", "seq-1", "a"))))
				.doesNotThrowAnyException();

		assertThat(processor.isHalted()).isTrue();
		verify(checkpointer, never()).checkpoint();
	}

	@Test
	@DisplayName("an unresolved failure stops the rest of the batch and ignores later batches")
	void unresolvedFailureStopsBatchAndLaterBatches() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<String> delivered = new ArrayList<>();
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.RECORD, message -> {
			String sequenceNumber = (String) message.getHeaders().get(KinesisMessageHeaders.SEQUENCE_NUMBER);
			delivered.add(sequenceNumber);
			if ("seq-2".equals(sequenceNumber)) {
				throw new RuntimeException("boom");
			}
		}, RETHROWING);

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a"), record("pk", "seq-2", "b"),
				record("pk", "seq-3", "c")));
		processor.processRecords(input(checkpointer, record("pk", "seq-4", "d")));

		assertThat(delivered).containsExactly("seq-1", "seq-2");
		verify(checkpointer).checkpoint("seq-1");
		verify(checkpointer, never()).checkpoint("seq-2");
		verify(checkpointer, never()).checkpoint("seq-3");
		verify(checkpointer, never()).checkpoint();
	}

	@Test
	@DisplayName("an error handler can retry the listener and resolve the failure")
	void errorHandlerRetriesListener() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Integer> attempts = new ArrayList<>();
		int[] calls = { 0 };
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.BATCH, message -> {
			if (++calls[0] < 3) {
				throw new RuntimeException("transient");
			}
		}, failedDelivery -> {
			while (true) {
				try {
					failedDelivery.retry();
					attempts.add(failedDelivery.getAttempts());
					return;
				}
				catch (RuntimeException ex) {
					attempts.add(failedDelivery.getAttempts());
				}
			}
		});

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a")));

		assertThat(calls[0]).isEqualTo(3);
		assertThat(attempts).containsExactly(2, 3);
		assertThat(processor.isHalted()).isFalse();
		verify(checkpointer, times(1)).checkpoint();
	}

	@Test
	@DisplayName("a batch listener failure hands the whole batch to the error handler with a retry hook")
	void batchListenerFailureRetriesWholeBatch() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Integer> batchSizes = new ArrayList<>();
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(),
				KclListenerMode.BATCH, KclCheckpointMode.BATCH, 1000L, Duration.ofSeconds(60), null, messages -> {
					batchSizes.add(messages.size());
					if (batchSizes.size() == 1) {
						throw new RuntimeException("boom");
					}
				}, failedDelivery -> {
					assertThat(failedDelivery.getMessage().getPayload()).isInstanceOf(Collection.class);
					failedDelivery.retry();
				}, "my-stream");
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a"), record("pk", "seq-2", "b")));

		assertThat(batchSizes).containsExactly(2, 2);
		verify(checkpointer, times(1)).checkpoint();
	}

	@Test
	@DisplayName("a halted shard releases its lease after the configured delay, for a single-stream lease key")
	void haltedShardReleasesLeaseAfterDelay() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<String> released = new CopyOnWriteArrayList<>();
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		try {
			KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(),
					null, KclListenerMode.SINGLE_RECORD, KclCheckpointMode.BATCH, 1000L, Duration.ofSeconds(60),
					message -> {
						throw new RuntimeException("boom");
					}, null, new LoggingErrorHandler(), "my-stream", null, Duration.ofMillis(100), released::add,
					scheduler, (shardId, cause, lag) -> {
					});
			processor.initialize(InitializationInput.builder().shardId("shard-1").build());

			processor.processRecords(input(checkpointer, record("pk", "seq-1", "a")));

			assertThat(processor.isHalted()).isTrue();
			await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(released).containsExactly("shard-1"));
		}
		finally {
			scheduler.shutdownNow();
		}
	}

	@Test
	@DisplayName("a halted shard of a multi-stream application releases the serialized stream-identifier lease key")
	void haltedShardReleasesMultiStreamLeaseKey() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<String> released = new CopyOnWriteArrayList<>();
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		try {
			KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(),
					null, KclListenerMode.SINGLE_RECORD, KclCheckpointMode.BATCH, 1000L, Duration.ofSeconds(60),
					message -> {
						throw new RuntimeException("boom");
					}, null, new LoggingErrorHandler(), "my-stream", "123456789012:my-stream:1700000000000",
					Duration.ofMillis(100), released::add, scheduler, (shardId, cause, lag) -> {
					});
			processor.initialize(InitializationInput.builder().shardId("shard-1").build());

			processor.processRecords(input(checkpointer, record("pk", "seq-1", "a")));

			await().atMost(Duration.ofSeconds(5)).untilAsserted(
					() -> assertThat(released).containsExactly("123456789012:my-stream:1700000000000:shard-1"));
		}
		finally {
			scheduler.shutdownNow();
		}
	}

	@Test
	@DisplayName("releasing is disabled when no delay is configured")
	void noReleaseWhenDelayUnset() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<String> released = new CopyOnWriteArrayList<>();
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		try {
			KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(),
					null, KclListenerMode.SINGLE_RECORD, KclCheckpointMode.BATCH, 1000L, Duration.ofSeconds(60),
					message -> {
						throw new RuntimeException("boom");
					}, null, new LoggingErrorHandler(), "my-stream", null, null, released::add, scheduler,
					(shardId, cause, lag) -> {
					});
			processor.initialize(InitializationInput.builder().shardId("shard-1").build());

			processor.processRecords(input(checkpointer, record("pk", "seq-1", "a")));

			Thread.sleep(300);
			assertThat(released).isEmpty();
		}
		finally {
			scheduler.shutdownNow();
		}
	}

	@Test
	@DisplayName("a scheduled release is cancelled when the processor's lease is lost before it fires")
	void pendingReleaseCancelledOnLeaseLost() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<String> released = new CopyOnWriteArrayList<>();
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		try {
			KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(),
					null, KclListenerMode.SINGLE_RECORD, KclCheckpointMode.BATCH, 1000L, Duration.ofSeconds(60),
					message -> {
						throw new RuntimeException("boom");
					}, null, new LoggingErrorHandler(), "my-stream", null, Duration.ofSeconds(30), released::add,
					scheduler, (shardId, cause, lag) -> {
					});
			processor.initialize(InitializationInput.builder().shardId("shard-1").build());
			processor.processRecords(input(checkpointer, record("pk", "seq-1", "a")));

			processor.leaseLost(software.amazon.kinesis.lifecycle.events.LeaseLostInput.builder().build());

			Thread.sleep(300);
			assertThat(released).isEmpty();
		}
		finally {
			scheduler.shutdownNow();
		}
	}

	@Test
	@DisplayName("the halt callback is notified once with the shard, cause and lag")
	void haltCallbackNotifiedOnce() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<String> haltedShards = new CopyOnWriteArrayList<>();
		List<Throwable> causes = new CopyOnWriteArrayList<>();
		List<Long> lags = new CopyOnWriteArrayList<>();
		RuntimeException boom = new RuntimeException("boom");
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(), null,
				KclListenerMode.SINGLE_RECORD, KclCheckpointMode.BATCH, 1000L, Duration.ofSeconds(60), message -> {
					throw boom;
				}, null, new LoggingErrorHandler(), "my-stream", null, null, null, null, (shardId, cause, lag) -> {
					haltedShards.add(shardId);
					causes.add(cause);
					lags.add(lag);
				});
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(ProcessRecordsInput.builder().records(List.of(record("pk", "seq-1", "a")))
				.checkpointer(checkpointer).millisBehindLatest(1234L).build());
		// A later batch is ignored and must not notify again.
		processor.processRecords(input(checkpointer, record("pk", "seq-2", "b")));

		assertThat(haltedShards).containsExactly("shard-1");
		assertThat(causes).containsExactly(boom);
		assertThat(lags).containsExactly(1234L);
	}

	@Test
	@DisplayName("a halt that reaches shard end keeps the pending release scheduled so the shard is reprocessed")
	void haltedShardEndKeepsReleaseScheduled() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<String> released = new CopyOnWriteArrayList<>();
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		try {
			KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(),
					null, KclListenerMode.SINGLE_RECORD, KclCheckpointMode.BATCH, 1000L, Duration.ofSeconds(60),
					message -> {
						throw new RuntimeException("boom");
					}, null, new LoggingErrorHandler(), "my-stream", null, Duration.ofMillis(150), released::add,
					scheduler, (shardId, cause, lag) -> {
					});
			processor.initialize(InitializationInput.builder().shardId("shard-1").build());
			processor.processRecords(input(checkpointer, record("pk", "seq-1", "a")));
			assertThat(processor.isHalted()).isTrue();

			// The halted shard reaches its end before the release fires.
			processor.shardEnded(ShardEndedInput.builder().checkpointer(checkpointer).build());

			// The release is not cancelled by shardEnded while halted, so the shard is still released and reprocessed.
			await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(released).containsExactly("shard-1"));
			// A halted shard never checkpoints the end of the shard.
			verify(checkpointer, never()).checkpoint();
		}
		finally {
			scheduler.shutdownNow();
		}
	}

	@Test
	@DisplayName("a halted shard does not checkpoint at shard end or on shutdown")
	void haltedShardDoesNotCheckpointAtShardEnd() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.BATCH, message -> {
			throw new RuntimeException("boom");
		}, RETHROWING);
		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a")));

		processor.shardEnded(ShardEndedInput.builder().checkpointer(checkpointer).build());
		processor.shutdownRequested(ShutdownRequestedInput.builder().checkpointer(checkpointer).build());

		verifyNoInteractions(checkpointer);
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
	@DisplayName("the first aggregated record checkpoints with subsequence zero")
	void firstAggregatedRecordCheckpointsWithSubsequenceZero() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.RECORD, message -> {
		}, RETHROWING);

		processor.processRecords(input(checkpointer, aggregatedRecord("pk", "seq-1", 0L, "a")));

		verify(checkpointer).checkpoint("seq-1", 0L);
	}

	@Test
	@DisplayName("a later aggregated record checkpoints with its subsequence")
	void laterAggregatedRecordCheckpointsWithSubsequence() throws Exception {
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
				KclCheckpointMode.BATCH, 1000L, Duration.ofSeconds(60), delivered::add, null, failedDelivery -> {
					assertThat(failedDelivery.isRetryable()).isFalse();
					handled.add(failedDelivery.getCause());
				}, "my-stream");
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

	@Test
	@DisplayName("a custom converter produces the messages handed to the listener")
	void customConverterIsUsed() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Message<?>> received = new ArrayList<>();
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new EnvelopeConverter(), null,
				KclListenerMode.SINGLE_RECORD, KclCheckpointMode.RECORD, 1000L, Duration.ofSeconds(60), received::add,
				null, RETHROWING, "my-stream");
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "tenant-a|hello")));

		Message<?> message = received.get(0);
		assertThat((byte[]) message.getPayload()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
		assertThat(message.getHeaders().get("tenant")).isEqualTo("tenant-a");
		assertThat(message.getHeaders().get(KinesisMessageHeaders.CHECKPOINTER)).isInstanceOf(KclCheckpointer.class);
		assertThat(message.getHeaders()).doesNotContainKey(MessageHeaders.CONTENT_TYPE);
	}

	@Test
	@DisplayName("the payload content type is added to messages whose converter set none")
	void payloadContentTypeIsAddedWhenAbsent() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Message<?>> received = new ArrayList<>();
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new EnvelopeConverter(),
				MimeTypeUtils.APPLICATION_JSON, KclListenerMode.SINGLE_RECORD, KclCheckpointMode.MANUAL, 1000L,
				Duration.ofSeconds(60), received::add, null, RETHROWING, "my-stream");
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(
				input(checkpointer, record("pk", "seq-1", "tenant-a|{}"), record("pk", "seq-2", "text|plain")));

		assertThat(received.get(0).getHeaders().get(MessageHeaders.CONTENT_TYPE))
				.isEqualTo(MimeTypeUtils.APPLICATION_JSON);
		assertThat(received.get(1).getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeTypeUtils.TEXT_PLAIN);
	}

	@Test
	@DisplayName("a converter returning null fails the record like a conversion failure")
	void converterReturningNullIsConversionFailure() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		KinesisMessageConverter returningNull = mock(KinesisMessageConverter.class);
		List<FailedDelivery> failures = new ArrayList<>();
		KclShardRecordProcessor processor = new KclShardRecordProcessor(returningNull, KclListenerMode.SINGLE_RECORD,
				KclCheckpointMode.MANUAL, 1000L, Duration.ofSeconds(60), message -> {
				}, null, failures::add, "my-stream");
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a")));

		assertThat(failures).singleElement().satisfies(failure -> {
			assertThat(failure.isRetryable()).isFalse();
			assertThat(failure.getCause()).isInstanceOf(IllegalStateException.class).hasMessageContaining("null");
			assertThat(failure.getMessage().getPayload()).isInstanceOf(KinesisClientRecord.class);
		});
	}

	@Test
	@DisplayName("SINGLE_RECORD MANUAL: checkpoint() on record 1 of a batch checkpoints record 1, not the batch")
	void singleRecordManualCheckpointCommitsThatRecordOnly() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Message<?>> received = new ArrayList<>();
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.MANUAL, received::add, RETHROWING);

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a"), record("pk", "seq-2", "b"),
				record("pk", "seq-3", "c")));

		assertThat(received).hasSize(3);
		KclCheckpointer first = received.get(0).getHeaders().get(KinesisMessageHeaders.CHECKPOINTER,
				KclCheckpointer.class);
		first.checkpoint();

		// Record 1's position, never the batch's latest position.
		verify(checkpointer).checkpoint("seq-1");
		verify(checkpointer, never()).checkpoint();
		verify(checkpointer, never()).checkpoint("seq-2");
		verify(checkpointer, never()).checkpoint("seq-3");
	}

	@Test
	@DisplayName("SINGLE_RECORD MANUAL: checkpoint() on an aggregated record checkpoints its subsequence number")
	void singleRecordManualCheckpointUsesSubsequence() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Message<?>> received = new ArrayList<>();
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.MANUAL, received::add, RETHROWING);

		processor.processRecords(input(checkpointer, aggregatedRecord("pk", "seq-1", 5L, "a")));

		received.get(0).getHeaders().get(KinesisMessageHeaders.CHECKPOINTER, KclCheckpointer.class).checkpoint();

		verify(checkpointer).checkpoint("seq-1", 5L);
	}

	@Test
	@DisplayName("SINGLE_RECORD MANUAL: checkpoint(Message) commits the position of the given message")
	void singleRecordManualCheckpointWithMessageDelegates() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Message<?>> received = new ArrayList<>();
		KclShardRecordProcessor processor = singleRecordProcessor(KclCheckpointMode.MANUAL, received::add, RETHROWING);

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a"), record("pk", "seq-2", "b")));

		KclCheckpointer first = received.get(0).getHeaders().get(KinesisMessageHeaders.CHECKPOINTER,
				KclCheckpointer.class);
		first.checkpoint(received.get(1));

		verify(checkpointer).checkpoint("seq-2");
	}

	@Test
	@DisplayName("BATCH MANUAL: checkpoint() checkpoints the latest position of the whole delivered batch")
	void batchManualCheckpointCommitsWholeBatch() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Collection<Message<?>>> batches = new ArrayList<>();
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new KinesisMessagingMessageConverter(),
				KclListenerMode.BATCH, KclCheckpointMode.MANUAL, 1000L, Duration.ofSeconds(60), null, batches::add,
				RETHROWING, "my-stream");
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer, record("pk", "seq-1", "a"), record("pk", "seq-2", "b")));

		Message<?> firstOfBatch = batches.get(0).iterator().next();
		firstOfBatch.getHeaders().get(KinesisMessageHeaders.CHECKPOINTER, KclCheckpointer.class).checkpoint();

		// The batch checkpointer commits the latest position of the batch, not a single record.
		verify(checkpointer).checkpoint();
		verify(checkpointer, never()).checkpoint(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	@DisplayName("the canonical Kinesis headers override a converter that omits or spoofs them")
	void canonicalHeadersOverrideConverter() {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Message<?>> received = new ArrayList<>();
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new SpoofingConverter(), null,
				KclListenerMode.SINGLE_RECORD, KclCheckpointMode.MANUAL, 1000L, Duration.ofSeconds(60), received::add,
				null, RETHROWING, "my-stream");
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer, record("pk-real", "seq-real", "body")));

		MessageHeaders headers = received.get(0).getHeaders();
		// The converter dropped the sequence number and spoofed the partition key; the overlay restores the truth.
		assertThat(headers.get(KinesisMessageHeaders.SEQUENCE_NUMBER)).isEqualTo("seq-real");
		assertThat(headers.get(KinesisMessageHeaders.PARTITION_KEY)).isEqualTo("pk-real");
		assertThat(headers.get(KinesisMessageHeaders.SHARD_ID)).isEqualTo("shard-1");
		assertThat(headers.get(KinesisMessageHeaders.STREAM_NAME)).isEqualTo("my-stream");
	}

	@Test
	@DisplayName("a spoofed sequence number cannot make a manual checkpoint commit the wrong record")
	void spoofedSequenceNumberCannotMisdirectCheckpoint() throws Exception {
		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		List<Message<?>> received = new ArrayList<>();
		KclShardRecordProcessor processor = new KclShardRecordProcessor(new SpoofingConverter(), null,
				KclListenerMode.SINGLE_RECORD, KclCheckpointMode.MANUAL, 1000L, Duration.ofSeconds(60), received::add,
				null, RETHROWING, "my-stream");
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		processor.processRecords(input(checkpointer, record("pk-real", "seq-real", "body")));

		received.get(0).getHeaders().get(KinesisMessageHeaders.CHECKPOINTER, KclCheckpointer.class).checkpoint();

		verify(checkpointer).checkpoint("seq-real");
		verify(checkpointer, never()).checkpoint("seq-spoofed");
	}

	/**
	 * Drops the sequence number and spoofs the partition key, so the canonical-header overlay can be verified.
	 */
	private static class SpoofingConverter implements KinesisMessageConverter {

		@Override
		public Message<byte[]> toMessagingMessage(KinesisClientRecord record, @Nullable String shardId,
				String streamName) {
			return MessageBuilder.withPayload(new byte[0]).setHeader(KinesisMessageHeaders.PARTITION_KEY, "pk-spoofed")
					.setHeader(KinesisMessageHeaders.SEQUENCE_NUMBER, "seq-spoofed").build();
		}

		@Override
		public byte[] fromMessagingMessage(Message<?> message) {
			throw new UnsupportedOperationException();
		}

	}

	/**
	 * Splits {@code <tenant>|<body>} records, mapping the {@code text} tenant to a {@code text/plain} content type.
	 */
	private static class EnvelopeConverter implements KinesisMessageConverter {

		@Override
		public Message<byte[]> toMessagingMessage(KinesisClientRecord record, @Nullable String shardId,
				String streamName) {
			String[] parts = StandardCharsets.UTF_8.decode(record.data().duplicate()).toString().split("\\|", 2);
			MessageBuilder<byte[]> builder = MessageBuilder.withPayload(parts[1].getBytes(StandardCharsets.UTF_8))
					.setHeader("tenant", parts[0])
					.setHeader(KinesisMessageHeaders.SEQUENCE_NUMBER, record.sequenceNumber());
			if ("text".equals(parts[0])) {
				builder.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN);
			}
			return builder.build();
		}

		@Override
		public byte[] fromMessagingMessage(Message<?> message) {
			throw new UnsupportedOperationException();
		}

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
				.subSequenceNumber(subSequenceNumber).aggregated(true)
				.approximateArrivalTimestamp(Instant.ofEpochMilli(1000L))
				.data(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8))).build();
	}

}
