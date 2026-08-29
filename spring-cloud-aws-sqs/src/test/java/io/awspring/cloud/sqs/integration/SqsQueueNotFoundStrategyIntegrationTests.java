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
package io.awspring.cloud.sqs.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.awspring.cloud.sqs.QueueAttributesResolvingException;
import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

/**
 * Integration tests for {@link QueueNotFoundStrategy}.
 *
 * @author Bill Kim
 */
class SqsQueueNotFoundStrategyIntegrationTests extends BaseSqsIntegrationTest {

	@Test
	void shouldStartAndStopWithoutPollingWhenIgnoredQueueIsMissing() throws Exception {
		SqsAsyncClient client = createAsyncClient();
		SqsTemplate template = SqsTemplate.newTemplate(client);
		String missingQueueName = uniqueQueueName();
		CountDownLatch messagesReceived = new CountDownLatch(1);
		SqsMessageListenerContainer<String> container = createContainer(client, QueueNotFoundStrategy.IGNORE,
				messagesReceived, missingQueueName);

		assertDoesNotThrow(container::start);
		assertThat(container.isRunning()).isTrue();

		createQueue(client, missingQueueName).join();
		template.send(missingQueueName, "should-not-be-consumed-before-restart");

		assertThat(messagesReceived.await(2, TimeUnit.SECONDS)).isFalse();

		assertDoesNotThrow(() -> container.stop());
		assertThat(container.isRunning()).isFalse();
	}

	@Test
	void shouldPollAfterIgnoredQueueIsCreatedAndContainerIsRestarted() throws Exception {
		SqsAsyncClient client = createAsyncClient();
		SqsTemplate template = SqsTemplate.newTemplate(client);
		String missingQueueName = uniqueQueueName();
		CountDownLatch messagesReceived = new CountDownLatch(1);
		SqsMessageListenerContainer<String> container = createContainer(client, QueueNotFoundStrategy.IGNORE,
				messagesReceived, missingQueueName);

		container.start();
		createQueue(client, missingQueueName).join();
		template.send(missingQueueName, "should-be-consumed-after-restart");

		assertThat(messagesReceived.await(2, TimeUnit.SECONDS)).isFalse();

		container.stop();
		container.start();

		assertThat(messagesReceived.await(10, TimeUnit.SECONDS)).isTrue();

		container.stop();
	}

	@Test
	void shouldConsumeFromExistingQueueWhenAnotherQueueIsIgnored() throws Exception {
		SqsAsyncClient client = createAsyncClient();
		SqsTemplate template = SqsTemplate.newTemplate(client);
		String existingQueueName = uniqueQueueName();
		String missingQueueName = uniqueQueueName();
		CountDownLatch messagesReceived = new CountDownLatch(1);
		SqsMessageListenerContainer<String> container = createContainer(client, QueueNotFoundStrategy.IGNORE,
				messagesReceived, missingQueueName, existingQueueName);

		createQueue(client, existingQueueName).join();
		container.start();
		template.send(existingQueueName, "should-be-consumed-from-existing-queue");

		assertThat(messagesReceived.await(10, TimeUnit.SECONDS)).isTrue();

		container.stop();
	}

	@Test
	void shouldThrowWhenQueueIsMissingAndStrategyIsFail() {
		SqsAsyncClient client = createAsyncClient();
		CountDownLatch messagesReceived = new CountDownLatch(1);
		SqsMessageListenerContainer<String> container = createContainer(client, QueueNotFoundStrategy.FAIL,
				messagesReceived, uniqueQueueName());

		assertThatThrownBy(container::start).isInstanceOf(CompletionException.class).cause()
				.isInstanceOfSatisfying(QueueAttributesResolvingException.class,
						qare -> assertThat(qare.isQueueIgnored()).isFalse())
				.cause().isInstanceOf(QueueDoesNotExistException.class);

		container.stop();
	}

	private SqsMessageListenerContainer<String> createContainer(SqsAsyncClient client,
			QueueNotFoundStrategy queueNotFoundStrategy, CountDownLatch messagesReceived, String... queueNames) {
		return SqsMessageListenerContainer.<String> builder().sqsAsyncClient(client).queueNames(queueNames)
				.configure(options -> options.queueNotFoundStrategy(queueNotFoundStrategy)
						.pollTimeout(Duration.ofMillis(200)).maxMessagesPerPoll(1).maxConcurrentMessages(1))
				.messageListener(message -> messagesReceived.countDown()).build();
	}

	private String uniqueQueueName() {
		return "queue-not-found-strategy-" + UUID.randomUUID();
	}

}
