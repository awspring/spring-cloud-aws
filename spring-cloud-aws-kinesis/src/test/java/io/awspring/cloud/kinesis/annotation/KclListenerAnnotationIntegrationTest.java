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
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointer;
import io.awspring.cloud.kinesis.operations.KinesisTemplate;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
class KclListenerAnnotationIntegrationTest implements LocalstackContainerTest {

	private static final String SINGLE_STREAM = "kcl-annotation-stream";

	private static final String BATCH_STREAM = "kcl-annotation-batch-stream";

	private static final List<String> RECEIVED = new CopyOnWriteArrayList<>();

	private static final List<String> RECEIVED_BATCH = new CopyOnWriteArrayList<>();

	private static final AtomicBoolean BATCH_CHECKPOINTED = new AtomicBoolean(false);

	@Test
	void annotatedListenersReceiveProducedRecords() {
		KinesisAsyncClient kinesisClient = LocalstackContainerTest.kinesisClient();
		LocalstackContainerTest.createStream(kinesisClient, SINGLE_STREAM, 1).join();
		LocalstackContainerTest.createStream(kinesisClient, BATCH_STREAM, 1).join();

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			KinesisTemplate template = new KinesisTemplate(kinesisClient, new JsonMapper());
			template.send(SINGLE_STREAM, "partition-key", "hello-annotation");
			template.send(BATCH_STREAM, "partition-key", "batch-a");
			template.send(BATCH_STREAM, "partition-key", "batch-b");

			Awaitility.await().atMost(Duration.ofMinutes(3)).until(() -> RECEIVED.contains("hello-annotation")
					&& RECEIVED_BATCH.containsAll(List.of("batch-a", "batch-b")) && BATCH_CHECKPOINTED.get());

			assertThat(RECEIVED).contains("hello-annotation");
			assertThat(RECEIVED_BATCH).contains("batch-a", "batch-b");
			assertThat(BATCH_CHECKPOINTED).isTrue();
		}
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
		SingleRecordListener singleRecordListener() {
			return new SingleRecordListener();
		}

		@Bean
		BatchListener batchListener() {
			return new BatchListener();
		}

	}

	static class SingleRecordListener {

		@KclListener(id = "single-listener", streamNames = SINGLE_STREAM, applicationName = "kcl-annotation-single-application")
		void handle(String payload) {
			RECEIVED.add(payload);
		}

	}

	static class BatchListener {

		@KclListener(id = "batch-listener", streamNames = BATCH_STREAM, applicationName = "kcl-annotation-batch-application", checkpointMode = "MANUAL")
		void handle(List<String> payloads, KclCheckpointer checkpointer) {
			RECEIVED_BATCH.addAll(payloads);
			checkpointer.checkpoint();
			BATCH_CHECKPOINTED.set(true);
		}

	}

}
