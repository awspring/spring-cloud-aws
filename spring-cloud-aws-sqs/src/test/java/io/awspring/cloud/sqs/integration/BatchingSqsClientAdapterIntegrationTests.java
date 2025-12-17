/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sqs.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sqs.operations.BatchingSqsClientAdapter;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.*;

/**
 * Integration tests for the Sqs Batching Client Adapter.
 *
 * @author Heechul Kang
 */
@SpringBootTest
public class BatchingSqsClientAdapterIntegrationTests extends BaseSqsIntegrationTest {

	private static final String BASE_QUEUE_NAME = "batching-test-queue";

	@Autowired
	private SqsAsyncClient asyncClient;

	@Test
	void shouldReturnCorrectServiceName() {
		try (BatchingSqsClientAdapter adapter = createBatchingAdapter()) {
			String serviceName = adapter.serviceName();
			assertThat(serviceName).isEqualTo(SqsAsyncClient.SERVICE_NAME);
		}
	}

	@Test
	void shouldSendMessageThroughBatchManager() {
		String queueName = createUniqueQueueName();
		createQueue(this.asyncClient, queueName).join();

		try (BatchingSqsClientAdapter adapter = createBatchingAdapter()) {
			String messageBody = "Test message for batching";

			SendMessageResponse response = adapter
					.sendMessage(SendMessageRequest.builder().queueUrl(queueName).messageBody(messageBody).build())
					.join();

			assertThat(response.messageId()).isNotNull();

			ReceiveMessageResponse received = this.asyncClient
					.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueName).maxNumberOfMessages(1).build())
					.join();

			assertThat(received.messages()).hasSize(1);
			assertThat(received.messages().get(0).body()).isEqualTo(messageBody);
		}
	}

	@Test
	void shouldSendMessageWithConsumer() {
		String queueName = createUniqueQueueName();
		createQueue(this.asyncClient, queueName).join();

		try (BatchingSqsClientAdapter adapter = createBatchingAdapter()) {
			String messageBody = "Test message with consumer";

			SendMessageResponse response = adapter
					.sendMessage(builder -> builder.queueUrl(queueName).messageBody(messageBody)).join();

			assertThat(response.messageId()).isNotNull();

			ReceiveMessageResponse received = this.asyncClient
					.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueName).maxNumberOfMessages(1).build())
					.join();

			assertThat(received.messages()).hasSize(1);
			assertThat(received.messages().get(0).body()).isEqualTo(messageBody);
		}
	}

	@Test
	void shouldReceiveMessageThroughBatchManager() {
		String queueName = createUniqueQueueName();
		createQueue(this.asyncClient, queueName).join();

		try (BatchingSqsClientAdapter adapter = createBatchingAdapter()) {
			String messageBody = "Test message for receiving";
			this.asyncClient
					.sendMessage(SendMessageRequest.builder().queueUrl(queueName).messageBody(messageBody).build())
					.join();

			ReceiveMessageResponse response = adapter.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueName)
					.maxNumberOfMessages(1).waitTimeSeconds(10).build()).join();

			assertThat(response.messages()).hasSize(1);
			assertThat(response.messages().get(0).body()).isEqualTo(messageBody);
		}
	}

	@Test
	void shouldReceiveMessageWithConsumer() {
		String queueName = createUniqueQueueName();
		createQueue(this.asyncClient, queueName).join();

		try (BatchingSqsClientAdapter adapter = createBatchingAdapter()) {
			String messageBody = "Test message for receiving with consumer";
			this.asyncClient
					.sendMessage(SendMessageRequest.builder().queueUrl(queueName).messageBody(messageBody).build())
					.join();

			ReceiveMessageResponse response = adapter
					.receiveMessage(builder -> builder.queueUrl(queueName).maxNumberOfMessages(1).waitTimeSeconds(10))
					.join();

			assertThat(response.messages()).hasSize(1);
			assertThat(response.messages().get(0).body()).isEqualTo(messageBody);
		}
	}

	@Test
	void shouldDeleteMessageThroughBatchManager() {
		String queueName = createUniqueQueueName();
		createQueue(this.asyncClient, queueName).join();

		try (BatchingSqsClientAdapter adapter = createBatchingAdapter()) {
			String messageBody = "Test message for deletion";
			this.asyncClient
					.sendMessage(SendMessageRequest.builder().queueUrl(queueName).messageBody(messageBody).build())
					.join();

			ReceiveMessageResponse received = this.asyncClient.receiveMessage(ReceiveMessageRequest.builder()
					.queueUrl(queueName).maxNumberOfMessages(1).waitTimeSeconds(10).build()).join();

			assertThat(received.messages()).hasSize(1);
			String receiptHandle = received.messages().get(0).receiptHandle();

			DeleteMessageResponse deleteResponse = adapter
					.deleteMessage(
							DeleteMessageRequest.builder().queueUrl(queueName).receiptHandle(receiptHandle).build())
					.join();

			assertThat(deleteResponse).isNotNull();

			ReceiveMessageResponse afterDelete = this.asyncClient.receiveMessage(ReceiveMessageRequest.builder()
					.queueUrl(queueName).maxNumberOfMessages(1).waitTimeSeconds(1).build()).join();

			assertThat(afterDelete.messages()).isEmpty();
		}
	}

	@Test
	void shouldDeleteMessageWithConsumer() {
		String queueName = createUniqueQueueName();
		createQueue(this.asyncClient, queueName).join();

		try (BatchingSqsClientAdapter adapter = createBatchingAdapter()) {
			String messageBody = "Test message for deletion with consumer";
			this.asyncClient
					.sendMessage(SendMessageRequest.builder().queueUrl(queueName).messageBody(messageBody).build())
					.join();

			ReceiveMessageResponse received = this.asyncClient.receiveMessage(ReceiveMessageRequest.builder()
					.queueUrl(queueName).maxNumberOfMessages(1).waitTimeSeconds(10).build()).join();

			String receiptHandle = received.messages().get(0).receiptHandle();

			DeleteMessageResponse deleteResponse = adapter
					.deleteMessage(builder -> builder.queueUrl(queueName).receiptHandle(receiptHandle)).join();

			assertThat(deleteResponse).isNotNull();
		}
	}

	@Test
	void shouldChangeMessageVisibilityThroughBatchManager() {
		String queueName = createUniqueQueueName();
		createQueue(this.asyncClient, queueName).join();

		try (BatchingSqsClientAdapter adapter = createBatchingAdapter()) {
			String messageBody = "Test message for visibility change";
			this.asyncClient
					.sendMessage(SendMessageRequest.builder().queueUrl(queueName).messageBody(messageBody).build())
					.join();

			ReceiveMessageResponse received = this.asyncClient.receiveMessage(ReceiveMessageRequest.builder()
					.queueUrl(queueName).maxNumberOfMessages(1).visibilityTimeout(5).waitTimeSeconds(10).build())
					.join();

			String receiptHandle = received.messages().get(0).receiptHandle();

			ChangeMessageVisibilityResponse response = adapter.changeMessageVisibility(ChangeMessageVisibilityRequest
					.builder().queueUrl(queueName).receiptHandle(receiptHandle).visibilityTimeout(30).build()).join();

			assertThat(response).isNotNull();
		}
	}

	@Test
	void shouldChangeMessageVisibilityWithConsumer() {
		String queueName = createUniqueQueueName();
		createQueue(this.asyncClient, queueName).join();

		try (BatchingSqsClientAdapter adapter = createBatchingAdapter()) {
			String messageBody = "Test message for visibility change with consumer";
			this.asyncClient
					.sendMessage(SendMessageRequest.builder().queueUrl(queueName).messageBody(messageBody).build())
					.join();

			ReceiveMessageResponse received = this.asyncClient.receiveMessage(ReceiveMessageRequest.builder()
					.queueUrl(queueName).maxNumberOfMessages(1).visibilityTimeout(5).waitTimeSeconds(10).build())
					.join();

			String receiptHandle = received.messages().get(0).receiptHandle();

			ChangeMessageVisibilityResponse response = adapter
					.changeMessageVisibility(
							builder -> builder.queueUrl(queueName).receiptHandle(receiptHandle).visibilityTimeout(30))
					.join();

			assertThat(response).isNotNull();
		}
	}

	@Test
	void shouldHandleBatchingEfficiently() {
		String queueName = createUniqueQueueName();
		createQueue(this.asyncClient, queueName).join();

		try (BatchingSqsClientAdapter adapter = createBatchingAdapter()) {
			int messageCount = 5;
			String messageBodyPrefix = "Batch test message ";

			CompletableFuture<SendMessageResponse>[] futures = new CompletableFuture[messageCount];

			for (int i = 0; i < messageCount; i++) {
				futures[i] = adapter.sendMessage(
						SendMessageRequest.builder().queueUrl(queueName).messageBody(messageBodyPrefix + i).build());
			}

			CompletableFuture.allOf(futures).join();

			for (CompletableFuture<SendMessageResponse> future : futures) {
				assertThat(future.join().messageId()).isNotNull();
			}

			ReceiveMessageResponse received = this.asyncClient.receiveMessage(ReceiveMessageRequest.builder()
					.queueUrl(queueName).maxNumberOfMessages(10).waitTimeSeconds(10).build()).join();

			assertThat(received.messages()).hasSize(messageCount);
		}
	}

	@Test
	void shouldSendMessageJoinCompletesAfterFrequency() {
		String queueName = createUniqueQueueName();
		createQueue(this.asyncClient, queueName).join();

		try (BatchingSqsClientAdapter adapter = createBatchingAdapterWithFrequency(Duration.ofSeconds(3))) {
			long startTime = System.nanoTime();

			String messageBody = "Test message for join frequency";

			adapter.sendMessage(SendMessageRequest.builder().queueUrl(queueName).messageBody(messageBody).build())
					.join();

			long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;

			assertThat(elapsedMillis).isGreaterThanOrEqualTo(3000L);
		}
	}

	private String createUniqueQueueName() {
		return BASE_QUEUE_NAME + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	private BatchingSqsClientAdapter createBatchingAdapter() {
		return createBatchingAdapterWithFrequency(Duration.ofMillis(100));
	}

	private BatchingSqsClientAdapter createBatchingAdapterWithFrequency(Duration sendRequestFrequency) {
		SqsAsyncBatchManager batchManager = SqsAsyncBatchManager.builder().client(this.asyncClient)
				.scheduledExecutor(Executors.newScheduledThreadPool(2))
				.overrideConfiguration(builder -> builder.maxBatchSize(10).sendRequestFrequency(sendRequestFrequency))
				.build();

		return new BatchingSqsClientAdapter(batchManager);
	}

	@Configuration
	static class SQSConfiguration {

		@Bean
		SqsAsyncClient client() {
			return createAsyncClient();
		}
	}
}