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
package io.awspring.cloud.kinesis.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.kinesis.LocalstackContainerTest;
import io.awspring.cloud.kinesis.config.KclBootstrapConfiguration;
import io.awspring.cloud.kinesis.config.KclMessageListenerContainerFactory;
import io.awspring.cloud.kinesis.config.MessageListenerContainerFactory;
import io.awspring.cloud.kinesis.listener.MessageListenerContainerRegistry;
import io.awspring.cloud.kinesis.operations.KinesisTemplate;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.handler.annotation.Header;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Matej Nedic
 * @since 4.2.0
 */
class KclListenerMultiStreamIntegrationTest implements LocalstackContainerTest {

	private static final String ORDERS_STREAM = "kcl-multi-orders";

	private static final String SHIPMENTS_STREAM = "kcl-multi-shipments";

	private static final String APPLICATION_NAME = "kcl-multi-stream-application";

	private static final List<String> RECEIVED = new CopyOnWriteArrayList<>();

	@Test
	@DisplayName("one @KclListener consumes several streams and checkpoints each of them")
	void multiStreamListenerConsumesAndCheckpointsEveryStream() {
		KinesisAsyncClient kinesisClient = LocalstackContainerTest.kinesisClient();
		DynamoDbAsyncClient dynamoDbClient = LocalstackContainerTest.dynamoDbClient();
		LocalstackContainerTest.createStream(kinesisClient, ORDERS_STREAM, 1).join();
		LocalstackContainerTest.createStream(kinesisClient, SHIPMENTS_STREAM, 1).join();

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			assertThat(context.getBean(MessageListenerContainerRegistry.class).getListenerContainers()).hasSize(1);

			KinesisTemplate template = new KinesisTemplate(kinesisClient, new JsonMapper());
			template.send(ORDERS_STREAM, "partition-key", "order-1");
			template.send(SHIPMENTS_STREAM, "partition-key", "shipment-1");

			Awaitility.await().atMost(Duration.ofMinutes(3)).until(
					() -> RECEIVED.containsAll(List.of(ORDERS_STREAM + ":order-1", SHIPMENTS_STREAM + ":shipment-1")));

			assertThat(RECEIVED).contains(ORDERS_STREAM + ":order-1", SHIPMENTS_STREAM + ":shipment-1");

			Awaitility.await().atMost(Duration.ofMinutes(3)).until(() -> isCheckpointed(dynamoDbClient, ORDERS_STREAM)
					&& isCheckpointed(dynamoDbClient, SHIPMENTS_STREAM));

			assertThat(isCheckpointed(dynamoDbClient, ORDERS_STREAM)).isTrue();
			assertThat(isCheckpointed(dynamoDbClient, SHIPMENTS_STREAM)).isTrue();
		}
	}

	private static boolean isCheckpointed(DynamoDbAsyncClient dynamoDbClient, String streamName) {
		return leaseItems(dynamoDbClient).stream().filter(item -> leaseKeyOf(item).contains(streamName))
				.anyMatch(item -> isSequenceNumber(valueOf(item, "checkpoint")));
	}

	private static List<Map<String, AttributeValue>> leaseItems(DynamoDbAsyncClient dynamoDbClient) {
		try {
			return dynamoDbClient.scan(request -> request.tableName(APPLICATION_NAME)).join().items();
		}
		catch (Exception ex) {
			return List.of();
		}
	}

	private static String leaseKeyOf(Map<String, AttributeValue> item) {
		return valueOf(item, "leaseKey");
	}

	private static String valueOf(Map<String, AttributeValue> item, String attribute) {
		AttributeValue value = item.get(attribute);
		return value != null && value.s() != null ? value.s() : "";
	}

	private static boolean isSequenceNumber(String checkpoint) {
		return checkpoint.chars().allMatch(Character::isDigit) && !checkpoint.isEmpty();
	}

	@Configuration
	@Import(KclBootstrapConfiguration.class)
	static class TestConfig {

		@Bean
		KinesisAsyncClient kinesisClient() {
			return LocalstackContainerTest.kinesisClient();
		}

		@Bean
		DynamoDbAsyncClient dynamoDbClient() {
			return LocalstackContainerTest.dynamoDbClient();
		}

		@Bean
		CloudWatchAsyncClient cloudWatchClient() {
			return LocalstackContainerTest.cloudWatchClient();
		}

		@Bean
		MessageListenerContainerFactory kclContainerFactory(KinesisAsyncClient kinesisClient,
				DynamoDbAsyncClient dynamoDbClient, CloudWatchAsyncClient cloudWatchClient) {
			KclMessageListenerContainerFactory factory = new KclMessageListenerContainerFactory(kinesisClient,
					dynamoDbClient, cloudWatchClient);
			factory.configure(options -> options.idleTimeBetweenReadsInMillis(500).maxRecords(100)
					.gracefulShutdownTimeout(Duration.ofSeconds(10)));
			return factory;
		}

		@Bean
		MultiStreamListener multiStreamListener() {
			return new MultiStreamListener();
		}

	}

	static class MultiStreamListener {

		@KclListener(id = "multi-stream-listener", streamNames = { ORDERS_STREAM,
				SHIPMENTS_STREAM }, applicationName = APPLICATION_NAME)
		void handle(String payload, @Header(KinesisMessageHeaders.STREAM_NAME) String streamName) {
			RECEIVED.add(streamName + ":" + payload);
		}

	}

}
