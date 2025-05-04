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
package io.awspring.cloud.sqs.integration;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.errorhandler.ExponentialBackoffErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ImmediateRetryAsyncErrorHandler;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Integration tests for SQS ErrorHandler integration.
 *
 * @author Bruno Garcia
 * @author Rafael Pavarini
 */

@SpringBootTest
public class SqsErrorHandlerIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsErrorHandlerIntegrationTests.class);

	static final String SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_QUEUE_NAME = "success_visibility_timeout_to_zero_test_queue";

	static final String SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_BATCH_QUEUE_NAME = "success_visibility_batch_timeout_to_zero_test_queue";

	static final String SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_FACTORY = "receivesMessageErrorFactory";

	static final String SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_QUEUE_NAME = "success_exponential_backoff_error_handler";

	static final String SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_BATCH_QUEUE_NAME = "success_exponential_batch_timeout_backoff_error_handler";

	static final String SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_FACTORY = "receivesMessageExponentialErrorFactory";

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(
				createQueue(client, SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_QUEUE_NAME,
						singletonMap(QueueAttributeName.VISIBILITY_TIMEOUT, "500")),
				createQueue(client, SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_BATCH_QUEUE_NAME,
						singletonMap(QueueAttributeName.VISIBILITY_TIMEOUT, "500")),
				createQueue(client, SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_QUEUE_NAME),
				createQueue(client, SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_BATCH_QUEUE_NAME)).join();
	}

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	SqsTemplate sqsTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Test
	void receivesMessageVisibilityTimeout() throws Exception {
		String messageBody = UUID.randomUUID().toString();
		sqsTemplate.send(SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_QUEUE_NAME, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}", SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_QUEUE_NAME,
				messageBody);

		assertThat(latchContainer.receivesRetryMessageQuicklyLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void receivesMessageVisibilityTimeoutBatch() throws Exception {
		List<Message<String>> messages = create10Messages("receivesMessageVisibilityTimeoutBatch");

		sqsTemplate.sendManyAsync(SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_BATCH_QUEUE_NAME, messages);
		logger.debug("Sent message to queue {} with messageBody {}",
				SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_BATCH_QUEUE_NAME, messages);

		assertThat(latchContainer.receivesRetryBatchMessageQuicklyLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void receivesMessageExponentialBackOffErrorHandler() throws Exception {
		String messageBody = UUID.randomUUID().toString();
		sqsTemplate.send(SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_QUEUE_NAME, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}",
				SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_QUEUE_NAME, messageBody);

		assertThat(latchContainer.receivesRetryMessageExponentiallyLatch.await(20, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void receivesBatchMessageExponentialBackOffErrorHandler() throws Exception {
		List<Message<String>> messages = create10Messages("receivesMessageVisibilityTimeoutBatch");

		sqsTemplate.sendManyAsync(SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_BATCH_QUEUE_NAME, messages);
		logger.debug("Sent message to queue {} with messageBody {}",
				SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_BATCH_QUEUE_NAME, messages);

		assertThat(latchContainer.receivesRetryBatchMessageExponentiallyLatch.await(35, TimeUnit.SECONDS)).isTrue();
	}

	static class ImmediateRetryAsyncErrorHandlerListener {

		@Autowired
		LatchContainer latchContainer;

		private static final Map<String, Long> previousReceivedMessageTimestamps = new ConcurrentHashMap<>();

		private static final int MAX_EXPECTED_ELAPSED_TIME_BETWEEN_MSG_RECEIVES_IN_MS = 5000;

		@SqsListener(queueNames = SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_QUEUE_NAME, messageVisibilitySeconds = "500", factory = SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_FACTORY, id = "visibilityErrorHandler")
		CompletableFuture<Void> listen(Message<String> message,
				@Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			logger.info("Received message {} from queue {}", message, queueName);
			String msgId = MessageHeaderUtils.getHeader(message, "id", UUID.class).toString();
			Long prevReceivedMessageTimestamp = previousReceivedMessageTimestamps.get(msgId);
			if (prevReceivedMessageTimestamp == null) {
				previousReceivedMessageTimestamps.put(msgId, System.currentTimeMillis());
				return CompletableFuture
						.failedFuture(new RuntimeException("Expected exception from visibilityErrorHandler"));
			}

			long elapsedTimeBetweenMessageReceivesInMs = System.currentTimeMillis() - prevReceivedMessageTimestamp;
			if (elapsedTimeBetweenMessageReceivesInMs < MAX_EXPECTED_ELAPSED_TIME_BETWEEN_MSG_RECEIVES_IN_MS) {
				latchContainer.receivesRetryMessageQuicklyLatch.countDown();
			}

			return CompletableFuture.completedFuture(null);
		}
	}

	static class ImmediateRetryAsyncBatchErrorHandlerListener {

		@Autowired
		LatchContainer latchContainer;

		private static final Map<String, Long> previousReceivedMessageTimestamps = new ConcurrentHashMap<>();

		private static final int MAX_EXPECTED_ELAPSED_TIME_BETWEEN_BATCH_MSG_RECEIVES_IN_MS = 5000;

		@SqsListener(queueNames = SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_BATCH_QUEUE_NAME, messageVisibilitySeconds = "500", factory = SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_FACTORY, id = "visibilityBatchErrorHandler")
		CompletableFuture<Void> listen(List<Message<String>> messages) {
			logger.info("Received messages {} from queue {}", MessageHeaderUtils.getId(messages),
					messages.get(0).getHeaders().get(SqsHeaders.SQS_QUEUE_NAME_HEADER));

			for (Message<String> message : messages) {
				String msgId = MessageHeaderUtils.getHeader(message, "id", UUID.class).toString();
				if (!previousReceivedMessageTimestamps.containsKey(msgId)) {
					previousReceivedMessageTimestamps.put(msgId, System.currentTimeMillis());
					return CompletableFuture
							.failedFuture(new RuntimeException("Expected exception from visibilityBatchErrorHandler"));
				}
				else {
					long timediff = System.currentTimeMillis() - previousReceivedMessageTimestamps.get(msgId);
					if (MAX_EXPECTED_ELAPSED_TIME_BETWEEN_BATCH_MSG_RECEIVES_IN_MS > timediff) {
						latchContainer.receivesRetryBatchMessageQuicklyLatch.countDown();
					}
				}
			}

			return CompletableFuture.completedFuture(null);
		}
	}

	static class ExponentialBackOffErrorHandlerListener {
		@Autowired
		LatchContainer latchContainer;
		static Double multiplier = 2.0;
		static Integer initialValueSeconds = 2;
		long firstReceiveTimestamp;

		@SqsListener(queueNames = SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_QUEUE_NAME, messageVisibilitySeconds = "10", factory = SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_FACTORY, id = "visibilityExponentialErrorHandler")
		CompletableFuture<Void> listen(Message<String> message,
				@Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			logger.info("Received message {} from queue {}", message, queueName);

			long receiveCount = Long.parseLong(MessageHeaderUtils.getHeader(message,
					SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class));

			if (receiveCount < 4) {
				return CompletableFuture.failedFuture(
						new RuntimeException("Expected exception from visibilityExponentialErrorHandler"));
			}

			firstReceiveTimestamp = Long.parseLong(MessageHeaderUtils.getHeader(message,
					SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP, String.class));
			long currentTimestamp = MessageHeaderUtils.getHeader(message, MessageHeaders.TIMESTAMP, Long.class);

			long elapsedTimeBetweenMessageReceivesInSeconds = (currentTimestamp - firstReceiveTimestamp) / 1000;
			long expectedElapsedTime = calculateTotalElapsedExpectedTime(receiveCount);
			if (elapsedTimeBetweenMessageReceivesInSeconds >= expectedElapsedTime) {
				latchContainer.receivesRetryMessageExponentiallyLatch.countDown();
			}

			return CompletableFuture.completedFuture(null);
		}
	}

	static class ExponentialBackOffBatchErrorHandlerListener {
		@Autowired
		LatchContainer latchContainer;
		Map<String, Long> counter = new ConcurrentHashMap<>();

		@SqsListener(queueNames = SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_BATCH_QUEUE_NAME, messageVisibilitySeconds = "10", factory = SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_FACTORY, id = "visibilityExponentialBatchErrorHandler")
		CompletableFuture<Void> listen(List<Message<String>> messages) {
			logger.debug("Received messages {} from queue {}", MessageHeaderUtils.getId(messages),
					messages.get(0).getHeaders().get(SqsHeaders.SQS_QUEUE_NAME_HEADER));

			Collection<String> messagesReceiveCount = MessageHeaderUtils.getHeader(messages,
					SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class);

			boolean anyIsUnderRetryCount = messagesReceiveCount.stream()
					.anyMatch(messageReceiveCount -> Long.parseLong(messageReceiveCount) < 5);

			if (anyIsUnderRetryCount) {
				return CompletableFuture.failedFuture(
						new RuntimeException("Expected exception from visibilityExponentialBatchErrorHandler"));
			}

			for (Message<String> message : messages) {
				long receiveCount = Long.parseLong(MessageHeaderUtils.getHeader(message,
						SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class));
				long firstReceiveTimestamp = Long.parseLong(MessageHeaderUtils.getHeader(message,
						SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP, String.class));
				long currentTimestamp = MessageHeaderUtils.getHeader(message, MessageHeaders.TIMESTAMP, Long.class);
				long elapsedTimeBetweenMessageReceivesInSeconds = (currentTimestamp - firstReceiveTimestamp) / 1000;
				long expectedElapsedTime = calculateTotalElapsedExpectedTime(receiveCount);
				if (elapsedTimeBetweenMessageReceivesInSeconds >= expectedElapsedTime) {
					latchContainer.receivesRetryBatchMessageExponentiallyLatch.countDown();
				}
				String msgId = MessageHeaderUtils.getHeader(message, "id", UUID.class).toString();
				counter.put(msgId, elapsedTimeBetweenMessageReceivesInSeconds);
			}
			logger.info("Time elapsed by message {}", counter.toString());

			return CompletableFuture.completedFuture(null);
		}
	}

	static class LatchContainer {
		final CountDownLatch receivesRetryMessageQuicklyLatch = new CountDownLatch(1);
		final CountDownLatch receivesRetryBatchMessageQuicklyLatch = new CountDownLatch(10);
		final CountDownLatch receivesRetryMessageExponentiallyLatch = new CountDownLatch(1);
		final CountDownLatch receivesRetryBatchMessageExponentiallyLatch = new CountDownLatch(10);
	}

	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class SQSConfiguration {

		// @formatter:off
		@Bean(name = SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_FACTORY)
		public SqsMessageListenerContainerFactory<Object> errorHandlerVisibility() {
			return SqsMessageListenerContainerFactory
				.builder()
				.configure(options -> options
					.maxConcurrentMessages(10)
					.pollTimeout(Duration.ofSeconds(10))
					.maxMessagesPerPoll(10)
					.queueAttributeNames(Collections.singletonList(QueueAttributeName.QUEUE_ARN))
					.maxDelayBetweenPolls(Duration.ofSeconds(10)))
				.errorHandler(new ImmediateRetryAsyncErrorHandler<>())
				.sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient)
				.build();
		}
		// @formatter:on

		// @formatter:off
		@Bean(name = SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_FACTORY)
		public SqsMessageListenerContainerFactory<Object> exponentialBackOffErrorHandler() {
			return SqsMessageListenerContainerFactory
				.builder()
				.configure(options -> options
					.maxConcurrentMessages(10)
					.pollTimeout(Duration.ofSeconds(15))
					.maxMessagesPerPoll(10)
					.queueAttributeNames(Collections.singletonList(QueueAttributeName.QUEUE_ARN))
					.maxDelayBetweenPolls(Duration.ofSeconds(15)))
				.errorHandler(ExponentialBackoffErrorHandler.builder()
					.initialVisibilityTimeoutSeconds(ExponentialBackOffErrorHandlerListener.initialValueSeconds)
					.multiplier(ExponentialBackOffErrorHandlerListener.multiplier)
					.build())
				.sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient)
				.build();
		}
		// @formatter:on

		@Bean
		ImmediateRetryAsyncErrorHandlerListener immediateRetryAsyncErrorHandlerListener() {
			return new ImmediateRetryAsyncErrorHandlerListener();
		}

		@Bean
		ImmediateRetryAsyncBatchErrorHandlerListener immediateRetryAsyncBatchErrorHandlerListener() {
			return new ImmediateRetryAsyncBatchErrorHandlerListener();
		}

		@Bean
		ExponentialBackOffErrorHandlerListener exponentialBackOffErrorHandlerListener() {
			return new ExponentialBackOffErrorHandlerListener();
		}

		@Bean
		ExponentialBackOffBatchErrorHandlerListener exponentialBackOffBatchErrorHandlerListener() {
			return new ExponentialBackOffBatchErrorHandlerListener();
		}

		LatchContainer latchContainer = new LatchContainer();

		@Bean
		LatchContainer latchContainer() {
			return this.latchContainer;
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		SqsTemplate sqsTemplate() {
			return SqsTemplate.builder().sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient()).build();
		}
	}

	private List<Message<String>> create10Messages(String testName) {
		return IntStream.range(0, 10).mapToObj(index -> testName + "-payload-" + index)
				.map(payload -> MessageBuilder.withPayload(payload).build()).collect(Collectors.toList());
	}

	private static long calculateTotalElapsedExpectedTime(long receiveCount) {
		double sum = 0;
		for (int i = 0; i < receiveCount - 1; i++) {
			sum += Math.pow(ExponentialBackOffErrorHandlerListener.multiplier, i);
		}
		return (long) (ExponentialBackOffErrorHandlerListener.initialValueSeconds * sum);
	}
}
