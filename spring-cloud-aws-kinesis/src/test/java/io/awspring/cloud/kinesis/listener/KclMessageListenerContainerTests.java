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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

class KclMessageListenerContainerTests {

	@Test
	void haltedShardIsTrackedWithItsStreamAndClearedWhenTheProcessorEnds() {
		KclMessageListenerContainer container = container();
		container.setMessageListener(message -> {
			throw new RuntimeException("boom");
		});
		KclShardRecordProcessor processor = recordProcessor(container, "stream", null);
		processor.initialize(init("shard-1"));

		processor.processRecords(input(record("pk", "seq-1", "a")));

		assertThat(container.getHaltedShards()).containsExactly(new HaltedShard("stream", "shard-1"));

		processor.leaseLost(LeaseLostInput.builder().build());

		assertThat(container.getHaltedShards()).isEmpty();
	}

	@Test
	void aNewProcessorForTheSameShardClearsTheStaleHaltedEntry() {
		KclMessageListenerContainer container = container();
		container.setMessageListener(message -> {
			throw new RuntimeException("boom");
		});
		KclShardRecordProcessor first = recordProcessor(container, "stream", null);
		first.initialize(init("shard-1"));
		first.processRecords(input(record("pk", "seq-1", "a")));
		assertThat(container.getHaltedShards()).containsExactly(new HaltedShard("stream", "shard-1"));

		// The shard is released and a fresh processor initializes for it; the stale halted entry is cleared.
		KclShardRecordProcessor second = recordProcessor(container, "stream", null);
		second.initialize(init("shard-1"));

		assertThat(container.getHaltedShards()).isEmpty();
	}

	@Test
	void haltedShardsOfDifferentStreamsDoNotCollide() {
		KclMessageListenerContainer container = new KclMessageListenerContainer(mock(KinesisAsyncClient.class),
				mock(DynamoDbAsyncClient.class), mock(CloudWatchAsyncClient.class), List.of("stream-a", "stream-b"),
				"application", KclContainerOptions.builder().build());
		container.setMessageListener(message -> {
			throw new RuntimeException("boom");
		});
		KclShardRecordProcessor processorA = recordProcessor(container, "stream-a", "stream-a-prefix");
		KclShardRecordProcessor processorB = recordProcessor(container, "stream-b", "stream-b-prefix");
		processorA.initialize(init("shardId-000000000000"));
		processorB.initialize(init("shardId-000000000000"));

		processorA.processRecords(input(record("pk", "seq-1", "a")));
		processorB.processRecords(input(record("pk", "seq-2", "b")));

		// Same shard id, different streams: both are tracked separately.
		assertThat(container.getHaltedShards()).containsExactlyInAnyOrder(
				new HaltedShard("stream-a", "shardId-000000000000"),
				new HaltedShard("stream-b", "shardId-000000000000"));

		processorA.leaseLost(LeaseLostInput.builder().build());

		assertThat(container.getHaltedShards()).containsExactly(new HaltedShard("stream-b", "shardId-000000000000"));
	}

	private static KclShardRecordProcessor recordProcessor(KclMessageListenerContainer container, String streamName,
			String leaseKeyPrefix) {
		return (KclShardRecordProcessor) ReflectionTestUtils.invokeMethod(container, "createRecordProcessor",
				streamName, leaseKeyPrefix);
	}

	private static InitializationInput init(String shardId) {
		return InitializationInput.builder().shardId(shardId).build();
	}

	private static ProcessRecordsInput input(KinesisClientRecord... records) {
		return ProcessRecordsInput.builder().records(List.of(records))
				.checkpointer(mock(RecordProcessorCheckpointer.class)).build();
	}

	private static KinesisClientRecord record(String partitionKey, String sequenceNumber, String body) {
		return KinesisClientRecord.builder().partitionKey(partitionKey).sequenceNumber(sequenceNumber)
				.subSequenceNumber(0L).approximateArrivalTimestamp(Instant.ofEpochMilli(1000L))
				.data(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8))).build();
	}

	@Test
	void schedulerFailureClearsActiveLifecycleState() {
		KclMessageListenerContainer container = container();
		Scheduler scheduler = mock(Scheduler.class);
		ExecutorService executor = mock(ExecutorService.class);
		doThrow(new IllegalStateException("boom")).when(scheduler).run();
		ReflectionTestUtils.setField(container, "scheduler", scheduler);
		ReflectionTestUtils.setField(container, "executorService", executor);
		ReflectionTestUtils.setField(container, "running", true);

		ReflectionTestUtils.invokeMethod(container, "runScheduler", scheduler);

		assertThat(container.isRunning()).isFalse();
		assertThat(ReflectionTestUtils.getField(container, "scheduler")).isNull();
		assertThat(ReflectionTestUtils.getField(container, "executorService")).isNull();
		verify(executor).shutdown();
	}

	@Test
	void staleSchedulerCannotClearReplacementLifecycleState() {
		KclMessageListenerContainer container = container();
		Scheduler staleScheduler = mock(Scheduler.class);
		Scheduler activeScheduler = mock(Scheduler.class);
		ExecutorService executor = mock(ExecutorService.class);
		doThrow(new IllegalStateException("boom")).when(staleScheduler).run();
		ReflectionTestUtils.setField(container, "scheduler", activeScheduler);
		ReflectionTestUtils.setField(container, "executorService", executor);
		ReflectionTestUtils.setField(container, "running", true);

		ReflectionTestUtils.invokeMethod(container, "runScheduler", staleScheduler);

		assertThat(container.isRunning()).isTrue();
		assertThat(ReflectionTestUtils.getField(container, "scheduler")).isSameAs(activeScheduler);
		assertThat(ReflectionTestUtils.getField(container, "executorService")).isSameAs(executor);
		verify(executor, never()).shutdown();
	}

	@Test
	void startFailsWhenSingleRecordModeHasOnlyBatchListener() {
		KclMessageListenerContainer container = container();
		container.setBatchMessageListener(messages -> {
		});

		assertThatThrownBy(container::start).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("SINGLE_RECORD listener mode but no messageListener is set");
		assertThat(container.isRunning()).isFalse();
	}

	@Test
	void startFailsWhenBatchModeHasOnlyMessageListener() {
		KclMessageListenerContainer container = container();
		container.setMessageListener(message -> {
		});
		container.setListenerMode(KclListenerMode.BATCH);

		assertThatThrownBy(container::start).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("BATCH listener mode but no batchMessageListener is set");
		assertThat(container.isRunning()).isFalse();
	}

	private KclMessageListenerContainer container() {
		return new KclMessageListenerContainer(mock(KinesisAsyncClient.class), mock(DynamoDbAsyncClient.class),
				mock(CloudWatchAsyncClient.class), "stream", "application", KclContainerOptions.builder().build());
	}

}
