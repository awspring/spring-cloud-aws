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

import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient;
import io.awspring.cloud.kinesis.LocalstackContainerTest;
import java.time.Duration;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.StreamViewType;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
class DynamoDbStreamToKinesisIntegrationTests implements LocalstackContainerTest {

	static final Log LOGGER = LogFactory.getLog(DynamoDbStreamToKinesisIntegrationTests.class);

	static final String TEST_TABLE = "StreamsDemoTable";

	static final String TABLE_KEY = "id";

	static DynamoDbAsyncClient DYNAMODB;

	static DynamoDbStreamsClient DYNAMODB_STREAMS;

	static String DYNAMODB_STREAM_ARN;

	@Autowired
	KinesisMessageDrivenChannelAdapter kinesisMessageDrivenChannelAdapter;

	@Autowired
	PollableChannel kinesisReceiveChannel;

	@BeforeAll
	static void setup() {
		DYNAMODB = LocalstackContainerTest.dynamoDbClient();
		DYNAMODB_STREAMS = LocalstackContainerTest.dynamoDbStreamsClient();
		DYNAMODB_STREAM_ARN = createDemoTable();

		AmazonDynamoDBStreamsAdapterClient streamsAdapterClient = new AmazonDynamoDBStreamsAdapterClient(
				DYNAMODB_STREAMS, null);

		await().atMost(Duration.ofMinutes(2))
				.untilAsserted(() -> assertThat(
						streamsAdapterClient.describeStream(builder -> builder.streamName(DYNAMODB_STREAM_ARN)))
						.succeedsWithin(Duration.ofSeconds(60))
						.extracting(describeStreamResponse -> describeStreamResponse.streamDescription()
								.streamStatusAsString())
						.isEqualTo("ENABLED"));
	}

	private static String createDemoTable() {
		CreateTableRequest createTableRequest = CreateTableRequest.builder()
				.attributeDefinitions(AttributeDefinition.builder().attributeName(TABLE_KEY)
						.attributeType(ScalarAttributeType.S).build())
				.keySchema(KeySchemaElement.builder().attributeName(TABLE_KEY).keyType(KeyType.HASH).build())
				.billingMode(BillingMode.PAY_PER_REQUEST).tableName(TEST_TABLE)
				.streamSpecification(
						builder -> builder.streamEnabled(true).streamViewType(StreamViewType.NEW_AND_OLD_IMAGES))
				.build();

		return DYNAMODB.createTable(createTableRequest)
				.thenCompose(result -> DYNAMODB.waiter().waitUntilTableExists(request -> request.tableName(TEST_TABLE),
						waiter -> waiter.maxAttempts(10)
								.backoffStrategyV2(BackoffStrategy.fixedDelay(Duration.ofSeconds(1)))))
				.thenApply(waiterResponse -> waiterResponse.matched().response()
						.map(describeTableResponse -> describeTableResponse.table().latestStreamArn()))
				.join().get();
	}

	@Test
	void fromDynamoDbStreamToKinesis() {
		Map<?, ?> shardConsumers = TestUtils.getPropertyValue(this.kinesisMessageDrivenChannelAdapter, "shardConsumers",
				Map.class);

		await().untilAsserted(() -> assertThat(shardConsumers).hasSize(1));

		Object shardConsumer = shardConsumers.values().iterator().next();

		await().untilAsserted(
				() -> assertThat(TestUtils.getPropertyValue(shardConsumer, "state").toString()).isEqualTo("CONSUME"));

		DYNAMODB.putItem(builder -> builder.tableName(TEST_TABLE).item(
				Map.of(TABLE_KEY, AttributeValue.fromS("some_id"), "Message", AttributeValue.fromS("some value"))))
				.join();

		Message<?> receive = this.kinesisReceiveChannel.receive(120_000);
		assertThat(receive).satisfies(LOGGER::warn).extracting(Message::getPayload).asString().contains("some_id",
				"some value", "\"eventName\":\"INSERT\"", "\"eventSource\":\"aws:dynamodb\"");

		DYNAMODB.updateItem(
				builder -> builder.tableName(TEST_TABLE).key(Map.of(TABLE_KEY, AttributeValue.fromS("some_id")))
						.attributeUpdates(Map.of("Message", AttributeValueUpdate.builder().action(AttributeAction.PUT)
								.value(AttributeValue.fromS("updated value")).build())))
				.join();

		receive = this.kinesisReceiveChannel.receive(30_000);
		assertThat(receive).satisfies(LOGGER::warn).extracting(Message::getPayload).asString().contains("some_id",
				"some value", "updated value", "\"eventName\":\"MODIFY\"");

		DYNAMODB.deleteItem(
				builder -> builder.tableName(TEST_TABLE).key(Map.of(TABLE_KEY, AttributeValue.fromS("some_id"))))
				.join();

		receive = this.kinesisReceiveChannel.receive(30_000);
		assertThat(receive).satisfies(LOGGER::warn).extracting(Message::getPayload).asString().contains("some_id",
				"\"eventName\":\"REMOVE\"");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class TestConfiguration {

		@Bean
		PollableChannel kinesisReceiveChannel() {
			return new QueueChannel();
		}

		@Bean
		KinesisMessageDrivenChannelAdapter kinesisMessageDrivenChannelAdapter(PollableChannel kinesisReceiveChannel) {
			KinesisMessageDrivenChannelAdapter adapter = new KinesisMessageDrivenChannelAdapter(
					new SpringDynamoDBStreamsAdapterClient(DYNAMODB_STREAMS), DYNAMODB_STREAM_ARN);
			adapter.setStreamInitialSequence(KinesisShardOffset.trimHorizon());
			adapter.setOutputChannel(kinesisReceiveChannel);
			adapter.setConverter(String::new);
			DirectFieldAccessor dfa = new DirectFieldAccessor(adapter);
			dfa.setPropertyValue("describeStreamBackoff", 10);
			dfa.setPropertyValue("consumerBackoff", 10);
			dfa.setPropertyValue("idleBetweenPolls", 1);
			return adapter;
		}

	}

}
