/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.kinesis.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.ExpiredIteratorException;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsResponse;
import software.amazon.awssdk.services.kinesis.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.Shard;

/**
 * @author Artem Bilan
 * @author Matthias Wesolowski
 * @author Greg Eales
 * @author Asiel Caballero
 * @author Jonathan Nagayoshi
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
class KinesisMessageDrivenChannelAdapterTests {

	private static final String STREAM1 = "stream1";

	private static final String STREAM_FOR_RESHARDING = "streamForResharding";

	@Autowired
	private QueueChannel kinesisChannel;

	@Autowired
	@Qualifier("kinesisMessageDrivenChannelAdapter")
	private KinesisMessageDrivenChannelAdapter kinesisMessageDrivenChannelAdapter;

	@Autowired
	private MetadataStore checkpointStore;

	@Autowired
	private MetadataStore reshardingCheckpointStore;

	@Autowired
	@Qualifier("reshardingChannelAdapter")
	private KinesisMessageDrivenChannelAdapter reshardingChannelAdapter;

	@Autowired
	private KinesisAsyncClient amazonKinesisForResharding;

	@Autowired
	private Config config;

	@BeforeEach
	void setup() {
		this.kinesisChannel.purge(null);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void kinesisMessageDrivenChannelAdapter() {
		this.kinesisMessageDrivenChannelAdapter.start();
		final Set<KinesisShardOffset> shardOffsets = TestUtils.getPropertyValue(this.kinesisMessageDrivenChannelAdapter,
				"shardOffsets", Set.class);

		KinesisShardOffset testOffset1 = KinesisShardOffset.latest(STREAM1, "1");
		KinesisShardOffset testOffset2 = KinesisShardOffset.latest(STREAM1, "2");
		synchronized (shardOffsets) {
			assertThat(shardOffsets).contains(testOffset1, testOffset2);
			assertThat(shardOffsets).doesNotContain(KinesisShardOffset.latest(STREAM1, "3"));
		}

		Map<KinesisShardOffset, ?> shardConsumers = TestUtils.getPropertyValue(this.kinesisMessageDrivenChannelAdapter,
				"shardConsumers", Map.class);

		await().untilAsserted(() -> assertThat(shardConsumers).containsKeys(testOffset1, testOffset2));

		Message<?> message = this.kinesisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
		MessageHeaders headers = message.getHeaders();
		assertThat(headers.get(KinesisHeaders.RECEIVED_PARTITION_KEY)).isEqualTo("partition1");
		assertThat(headers.get(KinesisHeaders.SHARD)).isEqualTo("1");
		assertThat(headers.get(KinesisHeaders.RECEIVED_SEQUENCE_NUMBER)).isEqualTo("1");
		assertThat(headers.get(KinesisHeaders.RECEIVED_STREAM)).isEqualTo(STREAM1);
		Checkpointer checkpointer = headers.get(KinesisHeaders.CHECKPOINTER, Checkpointer.class);
		assertThat(checkpointer).isNotNull();

		message = this.kinesisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("bar");
		headers = message.getHeaders();
		assertThat(headers.get(KinesisHeaders.RECEIVED_PARTITION_KEY)).isEqualTo("partition1");
		assertThat(headers.get(KinesisHeaders.SHARD)).isEqualTo("1");
		assertThat(headers.get(KinesisHeaders.RECEIVED_SEQUENCE_NUMBER)).isEqualTo("2");
		assertThat(headers.get(KinesisHeaders.RECEIVED_STREAM)).isEqualTo(STREAM1);

		assertThat(this.kinesisChannel.receive(10)).isNull();

		checkpointer.checkpoint();

		assertThat(this.checkpointStore.get("SpringIntegration" + ":" + STREAM1 + ":" + "1")).isEqualTo("2");

		this.kinesisMessageDrivenChannelAdapter.stop();

		Map<?, ?> forLocking = TestUtils.getPropertyValue(this.kinesisMessageDrivenChannelAdapter,
				"shardConsumerManager.locks", Map.class);

		await().untilAsserted(() -> assertThat(forLocking).isEmpty());

		final List consumerInvokers = TestUtils.getPropertyValue(this.kinesisMessageDrivenChannelAdapter,
				"consumerInvokers", List.class);
		await().untilAsserted(() -> assertThat(consumerInvokers).isEmpty());

		this.kinesisMessageDrivenChannelAdapter.setListenerMode(ListenerMode.batch);
		this.kinesisMessageDrivenChannelAdapter.setCheckpointMode(CheckpointMode.record);
		this.checkpointStore.put("SpringIntegration" + ":" + STREAM1 + ":" + "1", "1");

		setup();

		this.kinesisMessageDrivenChannelAdapter.start();

		message = this.kinesisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(List.class);
		List<String> payload = (List<String>) message.getPayload();
		assertThat(payload).hasSize(1);
		String record = payload.get(0);
		assertThat(record).isEqualTo("bar");

		Object partitionKeyHeader = message.getHeaders().get(KinesisHeaders.RECEIVED_PARTITION_KEY);
		assertThat(partitionKeyHeader).isInstanceOf(List.class);
		assertThat((List<String>) partitionKeyHeader).contains("partition1");

		Object sequenceNumberHeader = message.getHeaders().get(KinesisHeaders.RECEIVED_SEQUENCE_NUMBER);
		assertThat(sequenceNumberHeader).isInstanceOf(List.class);
		assertThat((List<String>) sequenceNumberHeader).contains("2");

		await().untilAsserted(
				() -> assertThat(this.checkpointStore.get("SpringIntegration" + ":" + STREAM1 + ":" + "1"))
						.isEqualTo("2"));

		assertThat(TestUtils.getPropertyValue(this.kinesisMessageDrivenChannelAdapter, "consumerInvokers", List.class))
				.hasSize(2);

		this.kinesisMessageDrivenChannelAdapter.stop();

		this.kinesisMessageDrivenChannelAdapter.setListenerMode(ListenerMode.batch);
		this.kinesisMessageDrivenChannelAdapter.setCheckpointMode(CheckpointMode.manual);
		this.checkpointStore.put("SpringIntegration" + ":" + STREAM1 + ":" + "1", "2");

		setup();

		this.kinesisMessageDrivenChannelAdapter.start();

		message = this.kinesisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(List.class);
		List<String> messagePayload = (List<String>) message.getPayload();
		assertThat(messagePayload).hasSize(3);

		Object messageSequenceNumberHeader = message.getHeaders().get(KinesisHeaders.RECEIVED_SEQUENCE_NUMBER);
		assertThat(messageSequenceNumberHeader).isInstanceOf(List.class);
		assertThat((List<String>) messageSequenceNumberHeader).contains("3");
		// Set checkpoint to 3, this should prevent adapter from using next shard, since it's not the latest record
		// in the batch
		checkpointer.checkpoint("3");

		await().untilAsserted(
				() -> assertThat(this.checkpointStore.get("SpringIntegration" + ":" + STREAM1 + ":" + "1"))
						.isEqualTo("3"));
		message = this.kinesisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(List.class);
		messagePayload = (List<String>) message.getPayload();
		assertThat(messagePayload).containsExactly("bar", "foobar");

		this.kinesisMessageDrivenChannelAdapter.stop();
	}

	@Test
	@SuppressWarnings("rawtypes")
	void resharding() throws InterruptedException {
		this.reshardingChannelAdapter.start();

		assertThat(this.kinesisChannel.receive(10000)).isNotNull();

		Map shardConsumers = TestUtils.getPropertyValue(this.reshardingChannelAdapter, "shardConsumers", Map.class);

		int n = 0;
		while (shardConsumers.size() != 4 && n++ < 100) {
			Thread.sleep(100);
		}
		assertThat(n).isLessThan(100);

		// When resharding happens the describeStream() is performed again
		verify(this.amazonKinesisForResharding, atLeast(1)).listShards(any(ListShardsRequest.class));

		this.reshardingChannelAdapter.stop();

		assertThat(this.reshardingCheckpointStore.get("SpringIntegration:streamForResharding:closedEmptyShard5"))
				.isEqualTo("50");

		KinesisShardEndedEvent kinesisShardEndedEvent = this.config.shardEndedEventReference.get();

		assertThat(kinesisShardEndedEvent).isNotNull().extracting(KinesisShardEndedEvent::getShardKey)
				.isEqualTo("SpringIntegration:streamForResharding:closedEmptyShard5");
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		private final AtomicReference<KinesisShardEndedEvent> shardEndedEventReference = new AtomicReference<>();

		@Bean
		@SuppressWarnings("unchecked")
		public KinesisAsyncClient amazonKinesis() {
			KinesisAsyncClient amazonKinesis = mock(KinesisAsyncClient.class);

			given(amazonKinesis.listShards(any(ListShardsRequest.class))).willReturn(CompletableFuture.completedFuture(
					ListShardsResponse.builder().shards(Shard.builder().shardId("1").sequenceNumberRange(range -> {
					}).build(), Shard.builder().shardId("2").sequenceNumberRange(range -> {
					}).build(), Shard.builder().shardId("3")
							.sequenceNumberRange(range -> range.endingSequenceNumber("1")).build()).build()));

			String shard1Iterator1 = "shard1Iterator1";
			String shard1Iterator2 = "shard1Iterator2";

			given(amazonKinesis.getShardIterator(KinesisShardOffset.latest(STREAM1, "1").toShardIteratorRequest()))
					.willReturn(
							CompletableFuture.completedFuture(
									GetShardIteratorResponse.builder().shardIterator(shard1Iterator1).build()),
							CompletableFuture.completedFuture(
									GetShardIteratorResponse.builder().shardIterator(shard1Iterator2).build()));

			String shard2Iterator1 = "shard2Iterator1";

			given(amazonKinesis.getShardIterator(KinesisShardOffset.latest(STREAM1, "2").toShardIteratorRequest()))
					.willReturn(CompletableFuture.completedFuture(
							GetShardIteratorResponse.builder().shardIterator(shard2Iterator1).build()));

			given(amazonKinesis
					.getRecords(GetRecordsRequest.builder().shardIterator(shard1Iterator1).limit(25).build()))
					.willReturn(CompletableFuture.failedFuture(
							ProvisionedThroughputExceededException.builder().message("Iterator throttled").build()))
					.willReturn(CompletableFuture
							.failedFuture(ExpiredIteratorException.builder().message("Iterator expired").build()));

			SerializingConverter serializingConverter = new SerializingConverter();

			String shard1Iterator3 = "shard1Iterator3";

			given(amazonKinesis
					.getRecords(GetRecordsRequest.builder().shardIterator(shard1Iterator2).limit(25).build()))
					.willReturn(
							CompletableFuture
									.completedFuture(GetRecordsResponse.builder().nextShardIterator(shard1Iterator3)
											.records(
													Record.builder().partitionKey("partition1").sequenceNumber("1")
															.data(SdkBytes
																	.fromByteArray(serializingConverter.convert("foo")))
															.build(),
													Record.builder().partitionKey("partition1").sequenceNumber("2")
															.data(SdkBytes
																	.fromByteArray(serializingConverter.convert("bar")))
															.build())
											.build()));

			given(amazonKinesis
					.getRecords(GetRecordsRequest.builder().shardIterator(shard2Iterator1).limit(25).build()))
					.willReturn(CompletableFuture
							.completedFuture(GetRecordsResponse.builder().nextShardIterator(shard2Iterator1).build()));

			given(amazonKinesis
					.getRecords(GetRecordsRequest.builder().shardIterator(shard1Iterator3).limit(25).build()))
					.willReturn(CompletableFuture
							.completedFuture(GetRecordsResponse.builder().nextShardIterator(shard1Iterator3).build()));

			String shard1Iterator4 = "shard1Iterator4";

			given(amazonKinesis.getShardIterator(
					KinesisShardOffset.afterSequenceNumber(STREAM1, "1", "1").toShardIteratorRequest()))
					.willReturn(CompletableFuture.completedFuture(
							GetShardIteratorResponse.builder().shardIterator(shard1Iterator4).build()));

			given(amazonKinesis
					.getRecords(GetRecordsRequest.builder().shardIterator(shard1Iterator4).limit(25).build()))
					.willReturn(CompletableFuture
							.completedFuture(GetRecordsResponse.builder().nextShardIterator(shard1Iterator3)
									.records(Record.builder().partitionKey("partition1").sequenceNumber("2")
											.data(SdkBytes.fromByteArray(serializingConverter.convert("bar"))).build())
									.build()));

			String shard1Iterator5 = "shard1Iterator5";
			String shard1Iterator6 = "shard1Iterator6";

			given(amazonKinesis.getShardIterator(
					KinesisShardOffset.afterSequenceNumber(STREAM1, "1", "2").toShardIteratorRequest()))
					.willReturn(CompletableFuture.completedFuture(
							GetShardIteratorResponse.builder().shardIterator(shard1Iterator5).build()));

			given(amazonKinesis
					.getRecords(GetRecordsRequest.builder().shardIterator(shard1Iterator5).limit(25).build()))
					.willReturn(
							CompletableFuture
									.completedFuture(GetRecordsResponse.builder().nextShardIterator(shard1Iterator6)
											.records(
													Record.builder().partitionKey("partition1").sequenceNumber("3")
															.data(SdkBytes
																	.fromByteArray(serializingConverter.convert("foo")))
															.build(),
													Record.builder().partitionKey("partition1").sequenceNumber("4")
															.data(SdkBytes.fromByteArray(
																	serializingConverter.convert("bar")))
															.build(),
													Record.builder().partitionKey("partition1").sequenceNumber("5")
															.data(SdkBytes.fromByteArray(
																	serializingConverter.convert("foobar")))
															.build())
											.build()));

			given(amazonKinesis.getShardIterator(
					KinesisShardOffset.afterSequenceNumber(STREAM1, "1", "3").toShardIteratorRequest()))
					.willReturn(CompletableFuture.completedFuture(
							GetShardIteratorResponse.builder().shardIterator(shard1Iterator6).build()));

			given(amazonKinesis
					.getRecords(GetRecordsRequest.builder().shardIterator(shard1Iterator6).limit(25).build()))
					.willReturn(
							CompletableFuture
									.completedFuture(GetRecordsResponse.builder().nextShardIterator(shard1Iterator6)
											.records(
													Record.builder().partitionKey("partition1").sequenceNumber("4")
															.data(SdkBytes
																	.fromByteArray(serializingConverter.convert("bar")))
															.build(),
													Record.builder().partitionKey("partition1").sequenceNumber("5")
															.data(SdkBytes.fromByteArray(
																	serializingConverter.convert("foobar")))
															.build())
											.build()));

			return amazonKinesis;
		}

		@Bean
		public ConcurrentMetadataStore checkpointStore() {
			SimpleMetadataStore simpleMetadataStore = new SimpleMetadataStore();
			String testKey = "SpringIntegration" + ":" + STREAM1 + ":" + "3";
			simpleMetadataStore.put(testKey, "1");
			return simpleMetadataStore;
		}

		@Bean
		public KinesisMessageDrivenChannelAdapter kinesisMessageDrivenChannelAdapter() {
			KinesisMessageDrivenChannelAdapter adapter = new KinesisMessageDrivenChannelAdapter(amazonKinesis(),
					STREAM1);
			adapter.setAutoStartup(false);
			adapter.setOutputChannel(kinesisChannel());
			adapter.setCheckpointStore(checkpointStore());
			adapter.setCheckpointMode(CheckpointMode.manual);
			adapter.setLockRegistry(new DefaultLockRegistry());
			adapter.setStartTimeout(10000);
			adapter.setDescribeStreamRetries(1);
			adapter.setConcurrency(10);
			adapter.setRecordsLimit(25);
			adapter.setDescribeStreamBackoff(10);
			adapter.setConsumerBackoff(10);
			adapter.setIdleBetweenPolls(1);
			return adapter;
		}

		@Bean
		public PollableChannel kinesisChannel() {
			return new QueueChannel();
		}

		@Bean
		public KinesisAsyncClient amazonKinesisForResharding() {
			KinesisAsyncClient amazonKinesis = mock(KinesisAsyncClient.class);

			// kinesis handles adding a shard by closing a shard and opening 2 new instead, creating a scenario where it
			// happens couple times
			given(amazonKinesis.listShards(any(ListShardsRequest.class)))
					.willReturn(
							CompletableFuture.completedFuture(ListShardsResponse.builder()
									.shards(Shard.builder().shardId("closedShard1")
											.sequenceNumberRange(range -> range.endingSequenceNumber("10")).build())
									.build()))
					.willReturn(
							CompletableFuture
									.completedFuture(
											ListShardsResponse.builder().shards(
													Shard.builder().shardId("closedShard1")
															.sequenceNumberRange(
																	range -> range.endingSequenceNumber("10"))
															.build(),
													Shard.builder().shardId("newShard2").sequenceNumberRange(range -> {
													}).build(),
													Shard.builder().shardId("newShard3").sequenceNumberRange(range -> {
													}).build(),
													Shard.builder().shardId("closedShard4")
															.sequenceNumberRange(
																	range -> range.endingSequenceNumber("40"))
															.build(),
													Shard.builder().shardId("closedEmptyShard5")
															.sequenceNumberRange(
																	range -> range.endingSequenceNumber("50"))
															.build())
													.build()))
					.willReturn(
							CompletableFuture
									.completedFuture(
											ListShardsResponse.builder().shards(
													Shard.builder().shardId("closedShard1")
															.sequenceNumberRange(
																	range -> range.endingSequenceNumber("10"))
															.build(),
													Shard.builder().shardId("newShard2").sequenceNumberRange(range -> {
													}).build(),
													Shard.builder().shardId("newShard3").sequenceNumberRange(range -> {
													}).build(),
													Shard.builder().shardId("closedShard4")
															.sequenceNumberRange(
																	range -> range.endingSequenceNumber("40"))
															.build(),
													Shard.builder().shardId("closedEmptyShard5")
															.sequenceNumberRange(
																	range -> range.endingSequenceNumber("50"))
															.build(),
													Shard.builder().shardId("newShard6").sequenceNumberRange(range -> {
													}).build(),
													Shard.builder().shardId("newShard7").sequenceNumberRange(range -> {
													}).build()).build()));

			setClosedShard(amazonKinesis, "1");
			setNewShard(amazonKinesis, "2");
			setNewShard(amazonKinesis, "3");
			setClosedShard(amazonKinesis, "4");
			setClosedEmptyShard(amazonKinesis, "5");
			setNewShard(amazonKinesis, "6");
			setNewShard(amazonKinesis, "7");

			return amazonKinesis;
		}

		private static void setClosedShard(KinesisAsyncClient amazonKinesis, String shardIndex) {
			String shardIterator = String.format("shard%sIterator1", shardIndex);

			given(amazonKinesis.getShardIterator(KinesisShardOffset
					.latest(STREAM_FOR_RESHARDING, "closedShard" + shardIndex).toShardIteratorRequest()))
					.willReturn(CompletableFuture
							.completedFuture(GetShardIteratorResponse.builder().shardIterator(shardIterator).build()));

			given(amazonKinesis.getRecords(GetRecordsRequest.builder().shardIterator(shardIterator).limit(25).build()))
					.willReturn(CompletableFuture.completedFuture(GetRecordsResponse.builder().nextShardIterator(null)
							.records(Record.builder().partitionKey("partition1").sequenceNumber(shardIndex)
									.data(SdkBytes.fromUtf8String("foo")).build())
							.build()));
		}

		private static void setClosedEmptyShard(KinesisAsyncClient amazonKinesis, String shardIndex) {
			String shardIterator = String.format("shard%sIterator1", shardIndex);

			given(amazonKinesis.getShardIterator(KinesisShardOffset
					.latest(STREAM_FOR_RESHARDING, "closedEmptyShard" + shardIndex).toShardIteratorRequest()))
					.willReturn(CompletableFuture
							.completedFuture(GetShardIteratorResponse.builder().shardIterator(shardIterator).build()));

			given(amazonKinesis.getRecords(GetRecordsRequest.builder().shardIterator(shardIterator).limit(25).build()))
					.willReturn(CompletableFuture
							.completedFuture(GetRecordsResponse.builder().nextShardIterator(null).build()));
		}

		private static void setNewShard(KinesisAsyncClient amazonKinesis, String shardIndex) {
			String shardIterator1 = String.format("shard%sIterator1", shardIndex);
			String shardIterator2 = String.format("shard%sIterator2", shardIndex);

			given(amazonKinesis.getShardIterator(
					KinesisShardOffset.latest(STREAM_FOR_RESHARDING, "newShard" + shardIndex).toShardIteratorRequest()))
					.willReturn(CompletableFuture
							.completedFuture(GetShardIteratorResponse.builder().shardIterator(shardIterator1).build()));

			given(amazonKinesis.getRecords(GetRecordsRequest.builder().shardIterator(shardIterator2).limit(25).build()))
					.willReturn(CompletableFuture.completedFuture(GetRecordsResponse.builder()
							.nextShardIterator(shardIterator2).records(Record.builder().partitionKey("partition1")
									.sequenceNumber(shardIndex).data(SdkBytes.fromUtf8String("foo")).build())
							.build()));

			given(amazonKinesis.getShardIterator(
					KinesisShardOffset.latest(STREAM_FOR_RESHARDING, "newShard" + shardIndex).toShardIteratorRequest()))
					.willReturn(CompletableFuture
							.completedFuture(GetShardIteratorResponse.builder().shardIterator(shardIterator2).build()));
		}

		@Bean
		public ConcurrentMetadataStore reshardingCheckpointStore() {
			return new ExceptionReadyMetadataStore();
		}

		@Bean
		public KinesisMessageDrivenChannelAdapter reshardingChannelAdapter() {
			KinesisMessageDrivenChannelAdapter adapter = new KinesisMessageDrivenChannelAdapter(
					amazonKinesisForResharding(), STREAM_FOR_RESHARDING);
			adapter.setAutoStartup(false);
			adapter.setOutputChannel(kinesisChannel());
			adapter.setConverter(String::new);
			adapter.setStartTimeout(10000);
			adapter.setDescribeStreamRetries(1);
			adapter.setRecordsLimit(25);
			adapter.setConcurrency(1);
			adapter.setCheckpointStore(reshardingCheckpointStore());
			adapter.setDescribeStreamBackoff(10);
			adapter.setConsumerBackoff(10);
			adapter.setIdleBetweenPolls(1);
			return adapter;
		}

		@EventListener
		public void handleKinesisShardEndedEvent(KinesisShardEndedEvent event) {
			this.shardEndedEventReference.set(event);
		}

	}

	private static final class ExceptionReadyMetadataStore extends SimpleMetadataStore {

		@Override
		public boolean replace(String key, String oldValue, String newValue) {
			if ("SpringIntegration:streamForResharding:closedShard4".equals(key)) {
				throw ProvisionedThroughputExceededException.builder().message("Throughput exceeded").build();
			}

			return super.replace(key, oldValue, newValue);
		}

	}

}
