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

import io.awspring.cloud.kinesis.LocalstackContainerTest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.Consumer;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.kinesis.metrics.MetricsFactory;
import software.amazon.kinesis.metrics.MetricsLevel;
import software.amazon.kinesis.metrics.NullMetricsFactory;

/**
 * @author Artem Bilan
 * @author Siddharth Jain
 * @author Minkyu Moon
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
public class KclMessageDrivenChannelAdapterTests implements LocalstackContainerTest {

	private static final String TEST_STREAM = "TestStreamKcl";

	public static final String LEASE_TABLE_NAME = "test_table";

	public static final String TEST_DATA = "test data";

	private static KinesisAsyncClient AMAZON_KINESIS;

	private static DynamoDbAsyncClient DYNAMO_DB;

	private static CloudWatchAsyncClient CLOUD_WATCH;

	@Autowired
	private PollableChannel kinesisReceiveChannel;

	@Autowired
	private KclMessageDrivenChannelAdapter kclMessageDrivenChannelAdapter;

	@BeforeAll
	static void setup() {
		AMAZON_KINESIS = LocalstackContainerTest.kinesisClient();
		DYNAMO_DB = LocalstackContainerTest.dynamoDbClient();
		CLOUD_WATCH = LocalstackContainerTest.cloudWatchClient();

		CompletableFuture.allOf(initializeStream(TEST_STREAM), initializeLeaseTableFor(LEASE_TABLE_NAME)).join();
	}

	@Test
	void kclChannelAdapterReceivesBatchedRecords() {
		this.kclMessageDrivenChannelAdapter.setListenerMode(ListenerMode.batch);
		this.kclMessageDrivenChannelAdapter.setCheckpointMode(CheckpointMode.batch);

		Message<?> received = verifyRecordReceived(TEST_DATA);
		assertThat(received.getPayload()).isEqualTo(Collections.singletonList(TEST_DATA));
		List<?> receivedSequences = received.getHeaders().get(KinesisHeaders.RECEIVED_SEQUENCE_NUMBER, List.class);
		assertThat(receivedSequences).isNotEmpty();
	}

	@Test
	void kclChannelAdapterReceivesSingleRecord() {

		this.kclMessageDrivenChannelAdapter.setListenerMode(ListenerMode.record);
		this.kclMessageDrivenChannelAdapter.setCheckpointMode(CheckpointMode.record);

		Message<?> receive = verifyRecordReceived(TEST_DATA);
		assertThat(receive.getPayload()).isEqualTo(TEST_DATA);
		assertThat(receive.getHeaders()).containsKey(IntegrationMessageHeaderAccessor.SOURCE_DATA);
		assertThat(receive.getHeaders().get(KinesisHeaders.RECEIVED_SEQUENCE_NUMBER, String.class)).isNotEmpty();
	}

	private Message<?> verifyRecordReceived(String testData) {
		AMAZON_KINESIS.putRecord(request -> request.streamName(TEST_STREAM).data(SdkBytes.fromUtf8String(testData))
				.partitionKey("test"));

		Message<?> receive = this.kinesisReceiveChannel.receive(60_000);
		assertThat(receive).isNotNull();

		List<Consumer> streamConsumers = AMAZON_KINESIS.describeStream(r -> r.streamName(TEST_STREAM))
				.thenCompose(describeStreamResponse -> AMAZON_KINESIS
						.listStreamConsumers(r -> r.streamARN(describeStreamResponse.streamDescription().streamARN())))
				.join().consumers();

		// Because FanOut is false, there would be no Stream Consumers.
		assertThat(streamConsumers).isEmpty();

		List<String> tableNames = DYNAMO_DB.listTables().join().tableNames();
		assertThat(tableNames).contains(LEASE_TABLE_NAME);
		return receive;
	}

	@Test
	void metricsLevelOfMetricsConfigShouldBeSetToMetricsLevelOfAdapter() {
		MetricsLevel metricsLevel = TestUtils.getPropertyValue(this.kclMessageDrivenChannelAdapter,
				"scheduler.metricsConfig.metricsLevel", MetricsLevel.class);
		assertThat(metricsLevel).isEqualTo(MetricsLevel.NONE);
	}

	@Test
	void metricsFactoryOfSchedulerShouldBeSetNullMetricsFactoryIfMetricsLevelIsNone() {
		MetricsFactory metricsFactory = TestUtils.getPropertyValue(this.kclMessageDrivenChannelAdapter,
				"scheduler.metricsFactory", MetricsFactory.class);
		assertThat(metricsFactory).isInstanceOf(NullMetricsFactory.class);
	}

	@Test
	void maxLeasesForWorkerOverriddenByCustomizer() {
		Integer maxLeasesForWorker = TestUtils.getPropertyValue(this.kclMessageDrivenChannelAdapter,
				"scheduler.leaseCoordinator.leaseTaker.maxLeasesForWorker", Integer.class);
		assertThat(maxLeasesForWorker).isEqualTo(10);
	}

	@Test
	void shardConsumerDispatchPollIntervalMillisOverriddenByCustomizer() {
		Long shardConsumerDispatchPollIntervalMillis = TestUtils.getPropertyValue(this.kclMessageDrivenChannelAdapter,
				"scheduler.shardConsumerDispatchPollIntervalMillis", Long.class);
		assertThat(shardConsumerDispatchPollIntervalMillis).isEqualTo(500L);
	}

	@Test
	void pollingMaxRecordsIsPropagated() {
		Integer maxRecords = TestUtils.getPropertyValue(this.kclMessageDrivenChannelAdapter,
				"scheduler.retrievalConfig.retrievalSpecificConfig.maxRecords", Integer.class);
		assertThat(maxRecords).isEqualTo(99);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class TestConfiguration {

		@Bean
		public KclMessageDrivenChannelAdapter kclMessageDrivenChannelAdapter(PollableChannel kinesisReceiveChannel) {
			KclMessageDrivenChannelAdapter adapter = new KclMessageDrivenChannelAdapter(AMAZON_KINESIS, CLOUD_WATCH,
					DYNAMO_DB, TEST_STREAM);
			adapter.setOutputChannel(kinesisReceiveChannel);
			adapter.setConverter(String::new);
			adapter.setConsumerGroup("single_stream_group");
			adapter.setLeaseTableName("test_table");
			adapter.setFanOut(false);
			adapter.setMetricsLevel(MetricsLevel.NONE);
			adapter.setLeaseManagementConfigCustomizer(leaseManagementConfig -> leaseManagementConfig
					.maxLeasesForWorker(10).workerUtilizationAwareAssignmentConfig().disableWorkerMetrics(true));
			adapter.setCoordinatorConfigCustomizer(
					coordinatorConfig -> coordinatorConfig.shardConsumerDispatchPollIntervalMillis(500L));
			adapter.setBindSourceRecord(true);
			adapter.setEmptyRecordList(true);
			adapter.setPollingMaxRecords(99);
			adapter.setGracefulShutdownTimeout(100);
			return adapter;
		}

		@Bean
		public PollableChannel kinesisReceiveChannel() {
			return new QueueChannel();
		}

	}

	private static CompletableFuture<WaiterResponse<DescribeStreamResponse>> initializeStream(String streamName) {
		return AMAZON_KINESIS.createStream(request -> request.streamName(streamName).shardCount(1)).thenCompose(
				result -> AMAZON_KINESIS.waiter().waitUntilStreamExists(request -> request.streamName(streamName)));
	}

	/**
	 * Initialize the lease table to improve KCL initialisation time
	 */
	private static CompletableFuture<PutItemResponse> initializeLeaseTableFor(String leaseTableName) {
		return DYNAMO_DB
				.createTable(CreateTableRequest.builder().tableName(leaseTableName)
						.attributeDefinitions(AttributeDefinition.builder().attributeName("leaseKey")
								.attributeType(ScalarAttributeType.S).build())
						.keySchema(KeySchemaElement.builder().attributeName("leaseKey").keyType(KeyType.HASH).build())
						.provisionedThroughput(
								ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build())
						.build())
				.thenCompose(
						result -> DYNAMO_DB.waiter().waitUntilTableExists(request -> request.tableName(leaseTableName)))
				.thenCompose(describeTableResponseWaiterResponse -> DYNAMO_DB.putItem(PutItemRequest.builder()
						.tableName(leaseTableName)
						.item(Map.of("leaseKey", AttributeValue.fromS("shardId-000000000000"), "checkpoint",
								AttributeValue.fromS("TRIM_HORIZON"), "leaseCounter", AttributeValue.fromN("1"),
								"startingHashKey", AttributeValue.fromS("0"), "ownerSwitchesSinceCheckpoint",
								AttributeValue.fromN("0"), "checkpointSubSequenceNumber", AttributeValue.fromN("0")))
						.build()));
	}

}
