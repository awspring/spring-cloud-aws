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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.Consumer;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.metrics.MetricsLevel;

/**
 * @author Siddharth Jain
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
class KclMessageDrivenChannelAdapterMultiStreamTests implements LocalstackContainerTest {

	private static final String TEST_STREAM1 = "MultiStreamKcl1";

	private static final String TEST_STREAM2 = "MultiStreamKcl2";

	private static KinesisAsyncClient AMAZON_KINESIS;

	private static DynamoDbAsyncClient DYNAMO_DB;

	private static CloudWatchAsyncClient CLOUD_WATCH;

	@Autowired
	private PollableChannel kinesisReceiveChannel;

	@BeforeAll
	static void setup() {
		AMAZON_KINESIS = LocalstackContainerTest.kinesisClient();
		DYNAMO_DB = LocalstackContainerTest.dynamoDbClient();
		CLOUD_WATCH = LocalstackContainerTest.cloudWatchClient();

		CompletableFuture<?> completableFuture1 = AMAZON_KINESIS
				.createStream(request -> request.streamName(TEST_STREAM1).shardCount(1))
				.thenCompose(result -> AMAZON_KINESIS.waiter()
						.waitUntilStreamExists(request -> request.streamName(TEST_STREAM1)));

		CompletableFuture<?> completableFuture2 = AMAZON_KINESIS
				.createStream(request -> request.streamName(TEST_STREAM2).shardCount(1))
				.thenCompose(result -> AMAZON_KINESIS.waiter()
						.waitUntilStreamExists(request -> request.streamName(TEST_STREAM2)));

		CompletableFuture.allOf(completableFuture1, completableFuture2).join();
	}

	@Test
	void kclChannelAdapterMultiStream() {
		String testData = "test data";
		AMAZON_KINESIS.putRecord(request -> request.streamName(TEST_STREAM1).data(SdkBytes.fromUtf8String(testData))
				.partitionKey("test"));

		String testData2 = "test data 2";
		AMAZON_KINESIS.putRecord(request -> request.streamName(TEST_STREAM2).data(SdkBytes.fromUtf8String(testData2))
				.partitionKey("test"));

		// The below statement works but with a higher timeout. For 2 streams, this takes too long.
		Message<?> receive = this.kinesisReceiveChannel.receive(300_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isIn(testData, testData2);
		assertThat(receive.getHeaders().get(KinesisHeaders.RECEIVED_SEQUENCE_NUMBER, String.class)).isNotEmpty();

		receive = this.kinesisReceiveChannel.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isIn(testData, testData2);

		List<Consumer> stream1Consumers = AMAZON_KINESIS.describeStream(request -> request.streamName(TEST_STREAM1))
				.thenCompose(describeStreamResponse -> AMAZON_KINESIS.listStreamConsumers(
						request -> request.streamARN(describeStreamResponse.streamDescription().streamARN())))
				.join().consumers();

		List<Consumer> stream2Consumers = AMAZON_KINESIS.describeStream(request -> request.streamName(TEST_STREAM2))
				.thenCompose(describeStreamResponse -> AMAZON_KINESIS.listStreamConsumers(
						request -> request.streamARN(describeStreamResponse.streamDescription().streamARN())))
				.join().consumers();

		assertThat(stream1Consumers).hasSize(1);
		assertThat(stream2Consumers).hasSize(1);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class TestConfiguration {

		@Bean
		public KclMessageDrivenChannelAdapter kclMessageDrivenChannelAdapter(PollableChannel kinesisReceiveChannel) {
			KclMessageDrivenChannelAdapter adapter = new KclMessageDrivenChannelAdapter(AMAZON_KINESIS, CLOUD_WATCH,
					DYNAMO_DB, TEST_STREAM1, TEST_STREAM2);
			adapter.setOutputChannel(kinesisReceiveChannel);
			adapter.setStreamInitialSequence(
					InitialPositionInStreamExtended.newInitialPosition(InitialPositionInStream.TRIM_HORIZON));
			adapter.setConverter(String::new);
			adapter.setConsumerGroup("multi_stream_group");
			adapter.setMetricsLevel(MetricsLevel.NONE);
			adapter.setLeaseManagementConfigCustomizer(leaseManagementConfig -> leaseManagementConfig
					.workerUtilizationAwareAssignmentConfig().disableWorkerMetrics(true));
			return adapter;
		}

		@Bean
		public PollableChannel kinesisReceiveChannel() {
			return new QueueChannel();
		}

	}

}
