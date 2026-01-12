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
package io.awspring.cloud.kinesis.stream.binder.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient;
import io.awspring.cloud.kinesis.stream.binder.LocalstackContainerTest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.StreamViewType;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.cloud.stream.kinesis.bindings.dynamoDbConsumer-in-0.consumer.dynamoDbStreams=true")
@DirtiesContext
public class DynamoDbStreamsIntegrationTests implements LocalstackContainerTest {

	static final String TEST_TABLE = "StreamsBinderDemoTable";

	static final String TABLE_KEY = "id";

	static DynamoDbAsyncClient DYNAMODB;

	static String DYNAMODB_STREAM_ARN;

	@Autowired
	private BindingService bindingService;

	@Autowired
	private CountDownLatch messageBarrier;

	@Autowired
	private AtomicReference<String> messageHolder;

	@BeforeAll
	static void setup() {
		DYNAMODB = LocalstackContainerTest.dynamoDbClient();
		DYNAMODB_STREAM_ARN = createDemoTable();
		AmazonDynamoDBStreamsAdapterClient streamsAdapterClient = new AmazonDynamoDBStreamsAdapterClient(
				LocalstackContainerTest.dynamoDbStreamsClient(), null);

		await().atMost(Duration.ofMinutes(2))
				.untilAsserted(() -> assertThat(
						streamsAdapterClient.describeStream(builder -> builder.streamName(DYNAMODB_STREAM_ARN)))
						.succeedsWithin(Duration.ofSeconds(60))
						.extracting(describeStreamResponse -> describeStreamResponse.streamDescription()
								.streamStatusAsString())
						.isEqualTo("ENABLED"));

		System.setProperty("spring.cloud.stream.bindings.dynamoDbConsumer-in-0.destination", DYNAMODB_STREAM_ARN);
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
	void fromDynamoDbStreamToKinesisBinder() throws InterruptedException {
		List<Binding<?>> consumerBindings = this.bindingService.getConsumerBindings("dynamoDbConsumer-in-0");

		assertThat(consumerBindings).hasSize(1);

		Binding<?> binding = consumerBindings.get(0);

		Map<?, ?> shardConsumers = TestUtils.getPropertyValue(binding, "lifecycle.shardConsumers", Map.class);
		assertThat(shardConsumers).hasSize(1);

		Object shardConsumer = shardConsumers.values().iterator().next();

		await().untilAsserted(
				() -> assertThat(TestUtils.getPropertyValue(shardConsumer, "state").toString()).isEqualTo("CONSUME"));

		DYNAMODB.putItem(builder -> builder.tableName(TEST_TABLE).item(
				Map.of(TABLE_KEY, AttributeValue.fromS("some_id"), "Message", AttributeValue.fromS("some value"))))
				.join();

		assertThat(this.messageBarrier.await(60, TimeUnit.SECONDS)).isTrue();
		assertThat(this.messageHolder.get()).contains("some_id", "some value", "\"eventName\":\"INSERT\"",
				"\"eventSource\":\"aws:dynamodb\"");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	static class TestConfiguration {

		@Bean
		public LockRegistry<?> lockRegistry() {
			return new DefaultLockRegistry();
		}

		@Bean
		public ConcurrentMetadataStore checkpointStore() {
			return new SimpleMetadataStore();
		}

		@Bean
		public AtomicReference<String> messageHolder() {
			return new AtomicReference<>();
		}

		@Bean
		public CountDownLatch messageBarrier() {
			return new CountDownLatch(1);
		}

		@Bean
		public Consumer<String> dynamoDbConsumer(AtomicReference<String> messageHolder, CountDownLatch messageBarrier) {
			return eventMessages -> {
				messageHolder.set(eventMessages);
				messageBarrier.countDown();
			};
		}

	}

}
