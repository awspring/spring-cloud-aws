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
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.Visibility;
import io.awspring.cloud.sqs.listener.errorhandler.*;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
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

	static final String SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_QUEUE = "success_exponential_full_jitter_backoff_error_handler";

	static final String SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_FACTORY = "success_exponential_full_jitter_backoff_error_handler_factory";

	static final String SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_QUEUE = "success_exponential_half_jitter_backoff_error_handler";
	static final String SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_FACTORY = "success_exponential_half_jitter_backoff_error_handler_factory";
	static final String SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_BATCH_QUEUE = "success_exponential_full_jitter_backoff_error_handler_batch";

	static final String SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_BATCH_QUEUE = "success_exponential_half_jitter_backoff_error_handler_batch";

	static final String SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_BATCH_QUEUE = "success_linear_backoff_error_handler_batch";
	static final String SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_QUEUE = "success_linear_backoff_error_handler";

	static final String SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_FACTORY = "success_linear_backoff_error_handler_factory";

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(
				createQueue(client, SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_QUEUE_NAME,
						singletonMap(QueueAttributeName.VISIBILITY_TIMEOUT, "500")),
				createQueue(client, SUCCESS_VISIBILITY_TIMEOUT_TO_ZERO_BATCH_QUEUE_NAME,
						singletonMap(QueueAttributeName.VISIBILITY_TIMEOUT, "500")),
				createQueue(client, SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_QUEUE_NAME),
				createQueue(client, SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_BATCH_QUEUE_NAME),
				createQueue(client, SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_QUEUE),
				createQueue(client, SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_QUEUE),
				createQueue(client, SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_BATCH_QUEUE),
				createQueue(client, SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_BATCH_QUEUE),
				createQueue(client, SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_BATCH_QUEUE)).join();
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
	void receivesMessageLinearBackOffErrorHandler() throws Exception {
		String messageBody = UUID.randomUUID().toString();
		sqsTemplate.send(SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_QUEUE, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}", SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_QUEUE,
				messageBody);

		await().atLeast(20, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS)
				.until(() -> latchContainer.receivesRetryMessageLinearlyLatch.getCount() == 0);

	}

	@Test
	void receivesMessageExponentialBackOffErrorHandler() throws Exception {
		String messageBody = UUID.randomUUID().toString();
		sqsTemplate.send(SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_QUEUE_NAME, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}",
				SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_QUEUE_NAME, messageBody);
		await().atLeast(10, TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS)
				.until(() -> latchContainer.receivesRetryMessageExponentiallyLatch.getCount() == 0);
	}

	@Test
	void receivesMessageExponentialFullJitterBackOffErrorHandler() throws Exception {
		String messageBody = UUID.randomUUID().toString();
		sqsTemplate.send(SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_QUEUE, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}",
				SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_QUEUE, messageBody);

		await().atLeast(64, TimeUnit.SECONDS).atMost(72, TimeUnit.SECONDS)
				.until(() -> latchContainer.receivesRetryMessageFullJitterLatch.getCount() == 0);
	}

	@Test
	void receivesMessageExponentialHalfJitterBackOffErrorHandler() throws Exception {
		String messageBody = UUID.randomUUID().toString();
		sqsTemplate.send(SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_QUEUE, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}",
				SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_QUEUE, messageBody);

		assertThat(latchContainer.receivesRetryMessageHalfJitterLatch.await(64, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void receivesBatchMessageExponentialBackOffErrorHandler() throws Exception {
		List<Message<String>> messages = create10Messages("receivesMessageVisibilityTimeoutBatch");

		sqsTemplate.sendManyAsync(SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_BATCH_QUEUE_NAME, messages);
		logger.debug("Sent message to queue {} with messageBody {}",
				SUCCESS_EXPONENTIAL_BACKOFF_ERROR_HANDLER_BATCH_QUEUE_NAME, messages);

		assertThat(latchContainer.receivesRetryBatchMessageExponentiallyLatch.await(35, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void receivesBatchMessageExponentialFullJitterBackOffErrorHandler() throws Exception {
		List<Message<String>> messages = create10Messages(
				"receivesBatchMessageExponentialFullJitterBackOffErrorHandler");

		sqsTemplate.sendManyAsync(SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_BATCH_QUEUE, messages);
		logger.debug("Sent message to queue {} with messageBody {}",
				SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_BATCH_QUEUE, messages);
		await().atLeast(64, TimeUnit.SECONDS).atMost(72, TimeUnit.SECONDS)
				.until(() -> latchContainer.receivesRetryBatchMessageFullJitterLatch.getCount() == 0);
	}

	@Test
	void receivesBatchMessageExponentialHalfJitterBackOffErrorHandler() throws Exception {
		List<Message<String>> messages = create10Messages(
				"receivesBatchMessageExponentialHalfJitterBackOffErrorHandler");

		sqsTemplate.sendManyAsync(SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_BATCH_QUEUE, messages);
		logger.debug("Sent message to queue {} with messageBody {}",
				SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_BATCH_QUEUE, messages);
		await().atLeast(32, TimeUnit.SECONDS).atMost(64, TimeUnit.SECONDS)
				.until(() -> latchContainer.receivesRetryBatchMessageHalfJitterLatch.getCount() == 0);
	}

	@Test
	void receivesBatchMessageLinearBackOffErrorHandler() throws Exception {
		List<Message<String>> messages = create10Messages("receivesBatchMessageLinearBackOffErrorHandler");

		sqsTemplate.sendManyAsync(SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_BATCH_QUEUE, messages);
		logger.debug("Sent message to queue {} with messageBody {}", SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_BATCH_QUEUE,
				messages);
		await().atLeast(20, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS)
				.until(() -> latchContainer.receivesRetryBatchMessageLinearlyLatch.getCount() == 0);
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

	static class ExponentialBackOffJitterErrorHandlerListener {
		@Autowired
		LatchContainer latchContainer;
		static Double multiplier = 2.0;
		static Integer initialValueSeconds = 2;
		long firstReceiveTimestamp;

		@SqsListener(queueNames = SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_QUEUE, factory = SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_FACTORY, id = "visibilityExponentialFullJitterErrorHandler")
		CompletableFuture<Void> listen(Message<String> message,
				@Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			logger.info("Received message {} from queue {}", message, queueName);

			long receiveCount = Long.parseLong(MessageHeaderUtils.getHeader(message,
					SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class));

			if (receiveCount < 7) {
				return CompletableFuture.failedFuture(
						new RuntimeException("Expected exception from visibilityExponentialErrorHandler"));
			}

			firstReceiveTimestamp = Long.parseLong(MessageHeaderUtils.getHeader(message,
					SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP, String.class));
			long currentTimestamp = MessageHeaderUtils.getHeader(message, MessageHeaders.TIMESTAMP, Long.class);

			long elapsedTimeBetweenMessageReceivesInSeconds = (currentTimestamp - firstReceiveTimestamp) / 1000;
			long expectedElapsedTime = calculateJitterTotalElapsedExpectedTime(receiveCount, false);
			if (elapsedTimeBetweenMessageReceivesInSeconds >= expectedElapsedTime) {
				latchContainer.receivesRetryMessageFullJitterLatch.countDown();
			}

			return CompletableFuture.completedFuture(null);
		}

		@SqsListener(queueNames = SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_QUEUE, factory = SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_FACTORY, id = "visibilityExponentialHalfJitterErrorHandler")
		CompletableFuture<Void> listenHalf(Message<String> message,
				@Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			logger.info("Received message {} from queue {}", message, queueName);

			long receiveCount = Long.parseLong(MessageHeaderUtils.getHeader(message,
					SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class));

			if (receiveCount < 6) {
				return CompletableFuture.failedFuture(
						new RuntimeException("Expected exception from visibilityExponentialErrorHandler"));
			}

			firstReceiveTimestamp = Long.parseLong(MessageHeaderUtils.getHeader(message,
					SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP, String.class));
			long currentTimestamp = MessageHeaderUtils.getHeader(message, MessageHeaders.TIMESTAMP, Long.class);

			long elapsedTimeBetweenMessageReceivesInSeconds = (currentTimestamp - firstReceiveTimestamp) / 1000;
			long expectedElapsedTime = calculateJitterTotalElapsedExpectedTime(receiveCount, true);
			if (elapsedTimeBetweenMessageReceivesInSeconds >= expectedElapsedTime) {
				latchContainer.receivesRetryMessageHalfJitterLatch.countDown();
			}

			return CompletableFuture.completedFuture(null);
		}
	}

	static class LinearBackOffErrorHandlerListener {
		@Autowired
		LatchContainer latchContainer;
		static int increment = 2;
		static int initialValueSeconds = 2;
		long firstReceiveTimestamp;

		@SqsListener(queueNames = SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_QUEUE, factory = SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_FACTORY, id = "visibilityLinearErrorHandler")
		CompletableFuture<Void> listen(Message<String> message,
				@Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			logger.info("Received message {} from queue {}", message, queueName);

			long receiveCount = Long.parseLong(MessageHeaderUtils.getHeader(message,
					SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class));

			if (receiveCount < 5) {
				return CompletableFuture
						.failedFuture(new RuntimeException("Expected exception from visibilityLinearErrorHandler"));
			}

			firstReceiveTimestamp = Long.parseLong(MessageHeaderUtils.getHeader(message,
					SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP, String.class));
			long currentTimestamp = MessageHeaderUtils.getHeader(message, MessageHeaders.TIMESTAMP, Long.class);

			long elapsedTimeBetweenMessageReceivesInSeconds = (currentTimestamp - firstReceiveTimestamp) / 1000;
			long expectedElapsedTime = calculateLinearTotalElapsedExpectedTime(receiveCount);
			if (elapsedTimeBetweenMessageReceivesInSeconds >= expectedElapsedTime) {
				latchContainer.receivesRetryMessageLinearlyLatch.countDown();
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

	static class ExponentialBackOffFullJitterBatchErrorHandlerListener {
		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_BATCH_QUEUE, messageVisibilitySeconds = "10", factory = SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_FACTORY, id = "visibilityExponentialFullJitterBatchErrorHandler")
		CompletableFuture<Void> listen(List<Message<String>> messages) {
			logger.debug("Received messages {} from queue {}", MessageHeaderUtils.getId(messages),
					messages.get(0).getHeaders().get(SqsHeaders.SQS_QUEUE_NAME_HEADER));

			Collection<String> messagesReceiveCount = MessageHeaderUtils.getHeader(messages,
					SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class);

			boolean anyIsUnderRetryCount = messagesReceiveCount.stream()
					.anyMatch(messageReceiveCount -> Long.parseLong(messageReceiveCount) < 7);

			if (anyIsUnderRetryCount) {
				return CompletableFuture.failedFuture(new RuntimeException(
						"Expected exception from visibilityExponentialFullJitterBatchErrorHandler"));
			}

			for (Message<String> message : messages) {
				long receiveCount = Long.parseLong(MessageHeaderUtils.getHeader(message,
						SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class));
				long firstReceiveTimestamp = Long.parseLong(MessageHeaderUtils.getHeader(message,
						SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP, String.class));
				long currentTimestamp = MessageHeaderUtils.getHeader(message, MessageHeaders.TIMESTAMP, Long.class);
				long elapsedTimeBetweenMessageReceivesInSeconds = (currentTimestamp - firstReceiveTimestamp) / 1000;
				long expectedElapsedTime = calculateJitterTotalElapsedExpectedTime(receiveCount, false);
				if (elapsedTimeBetweenMessageReceivesInSeconds >= expectedElapsedTime) {
					latchContainer.receivesRetryBatchMessageFullJitterLatch.countDown();
				}
			}

			return CompletableFuture.completedFuture(null);
		}
	}

	static class ExponentialBackOffHalfJitterBatchErrorHandlerListener {
		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_BATCH_QUEUE, messageVisibilitySeconds = "10", factory = SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_FACTORY, id = "visibilityExponentialHalfJitterBatchErrorHandler")
		CompletableFuture<Void> listen(List<Message<String>> messages) {
			logger.debug("Received messages {} from queue {}", MessageHeaderUtils.getId(messages),
					messages.get(0).getHeaders().get(SqsHeaders.SQS_QUEUE_NAME_HEADER));

			Collection<String> messagesReceiveCount = MessageHeaderUtils.getHeader(messages,
					SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class);

			boolean anyIsUnderRetryCount = messagesReceiveCount.stream()
					.anyMatch(messageReceiveCount -> Long.parseLong(messageReceiveCount) < 6);

			if (anyIsUnderRetryCount) {
				return CompletableFuture.failedFuture(new RuntimeException(
						"Expected exception from visibilityExponentialHalfJitterBatchErrorHandler"));
			}

			for (Message<String> message : messages) {
				long receiveCount = Long.parseLong(MessageHeaderUtils.getHeader(message,
						SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class));
				long firstReceiveTimestamp = Long.parseLong(MessageHeaderUtils.getHeader(message,
						SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP, String.class));
				long currentTimestamp = MessageHeaderUtils.getHeader(message, MessageHeaders.TIMESTAMP, Long.class);
				long elapsedTimeBetweenMessageReceivesInSeconds = (currentTimestamp - firstReceiveTimestamp) / 1000;
				long expectedElapsedTime = calculateJitterTotalElapsedExpectedTime(receiveCount, true);
				if (elapsedTimeBetweenMessageReceivesInSeconds >= expectedElapsedTime) {
					latchContainer.receivesRetryBatchMessageHalfJitterLatch.countDown();
				}
			}

			return CompletableFuture.completedFuture(null);
		}
	}

	static class LinearBackOffBatchErrorHandlerListener {
		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_BATCH_QUEUE, factory = SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_FACTORY, id = "visibilityLinearBatchErrorHandler")
		CompletableFuture<Void> listen(List<Message<String>> messages) {
			logger.debug("Received messages {} from queue {}", MessageHeaderUtils.getId(messages),
					messages.get(0).getHeaders().get(SqsHeaders.SQS_QUEUE_NAME_HEADER));

			Collection<String> messagesReceiveCount = MessageHeaderUtils.getHeader(messages,
					SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class);

			boolean anyIsUnderRetryCount = messagesReceiveCount.stream()
					.anyMatch(messageReceiveCount -> Long.parseLong(messageReceiveCount) < 5);

			if (anyIsUnderRetryCount) {
				return CompletableFuture.failedFuture(
						new RuntimeException("Expected exception from visibilityLinearBatchErrorHandler"));
			}

			for (Message<String> message : messages) {
				long receiveCount = Long.parseLong(MessageHeaderUtils.getHeader(message,
						SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class));
				long firstReceiveTimestamp = Long.parseLong(MessageHeaderUtils.getHeader(message,
						SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP, String.class));
				long currentTimestamp = MessageHeaderUtils.getHeader(message, MessageHeaders.TIMESTAMP, Long.class);
				long elapsedTimeBetweenMessageReceivesInSeconds = (currentTimestamp - firstReceiveTimestamp) / 1000;
				long expectedElapsedTime = calculateLinearTotalElapsedExpectedTime(receiveCount);
				if (elapsedTimeBetweenMessageReceivesInSeconds >= expectedElapsedTime) {
					latchContainer.receivesRetryBatchMessageLinearlyLatch.countDown();
				}
			}

			return CompletableFuture.completedFuture(null);
		}
	}

	static class LatchContainer {
		final CountDownLatch receivesRetryMessageQuicklyLatch = new CountDownLatch(1);
		final CountDownLatch receivesRetryBatchMessageQuicklyLatch = new CountDownLatch(10);
		final CountDownLatch receivesRetryMessageExponentiallyLatch = new CountDownLatch(1);
		final CountDownLatch receivesRetryBatchMessageExponentiallyLatch = new CountDownLatch(10);
		final CountDownLatch receivesRetryMessageFullJitterLatch = new CountDownLatch(1);
		final CountDownLatch receivesRetryMessageHalfJitterLatch = new CountDownLatch(1);
		final CountDownLatch receivesRetryMessageLinearlyLatch = new CountDownLatch(1);

		final CountDownLatch receivesRetryBatchMessageFullJitterLatch = new CountDownLatch(10);
		final CountDownLatch receivesRetryBatchMessageHalfJitterLatch = new CountDownLatch(10);
		final CountDownLatch receivesRetryBatchMessageLinearlyLatch = new CountDownLatch(10);
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

		// @formatter:off
		@Bean(name = SUCCESS_EXPONENTIAL_FULL_JITTER_BACKOFF_ERROR_HANDLER_FACTORY)
		public SqsMessageListenerContainerFactory<Object> exponentialBackOffFullJitterErrorHandler() {
			return SqsMessageListenerContainerFactory
				.builder()
				.configure(options -> options
					.maxConcurrentMessages(10)
					.pollTimeout(Duration.ofSeconds(15))
					.maxMessagesPerPoll(10)
					.queueAttributeNames(Collections.singletonList(QueueAttributeName.QUEUE_ARN))
					.maxDelayBetweenPolls(Duration.ofSeconds(15)))
				.errorHandler(ExponentialBackoffErrorHandler.builder()
					.initialVisibilityTimeoutSeconds(ExponentialBackOffJitterErrorHandlerListener.initialValueSeconds)
					.multiplier(ExponentialBackOffJitterErrorHandlerListener.multiplier)
					.randomSupplier(() -> new MockedRandomNextInt(getRandomFunction()))
					.jitter(Jitter.FULL)
					.build())
				.sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient)
				.build();
		}
		// @formatter:on

		//@formatter:off
		@Bean(name = SUCCESS_EXPONENTIAL_HALF_JITTER_BACKOFF_ERROR_HANDLER_FACTORY)
		public SqsMessageListenerContainerFactory<Object> exponentialBackOffHalfJitterErrorHandler() {
			return SqsMessageListenerContainerFactory
				.builder()
				.configure(options -> options
					.maxConcurrentMessages(10)
					.pollTimeout(Duration.ofSeconds(15))
					.maxMessagesPerPoll(10)
					.queueAttributeNames(Collections.singletonList(QueueAttributeName.QUEUE_ARN))
					.maxDelayBetweenPolls(Duration.ofSeconds(15)))
				.errorHandler(ExponentialBackoffErrorHandler.builder()
					.initialVisibilityTimeoutSeconds(ExponentialBackOffJitterErrorHandlerListener.initialValueSeconds)
					.multiplier(ExponentialBackOffJitterErrorHandlerListener.multiplier)
					.randomSupplier(() -> new MockedRandomNextInt(getRandomFunction()))
					.jitter(Jitter.HALF)
					.build())
				.sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient)
				.build();
		}
		// @formatter:on

		//@formatter:off
		@Bean(name = SUCCESS_LINEAR_BACKOFF_ERROR_HANDLER_FACTORY)
		public SqsMessageListenerContainerFactory<Object> linearBackOffErrorHandler() {
			return SqsMessageListenerContainerFactory
				.builder()
				.configure(options -> options
					.maxConcurrentMessages(10)
					.pollTimeout(Duration.ofSeconds(15))
					.maxMessagesPerPoll(10)
					.queueAttributeNames(Collections.singletonList(QueueAttributeName.QUEUE_ARN))
					.maxDelayBetweenPolls(Duration.ofSeconds(15)))
				.errorHandler(LinearBackoffErrorHandler.builder()
					.initialVisibilityTimeoutSeconds(LinearBackOffErrorHandlerListener.initialValueSeconds)
					.increment(LinearBackOffErrorHandlerListener.increment)
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

		@Bean
		ExponentialBackOffJitterErrorHandlerListener exponentialBackOffJitterErrorHandlerListener() {
			return new ExponentialBackOffJitterErrorHandlerListener();
		}

		@Bean
		ExponentialBackOffFullJitterBatchErrorHandlerListener exponentialBackOffFullJitterBatchErrorHandlerListener() {
			return new ExponentialBackOffFullJitterBatchErrorHandlerListener();
		}

		@Bean
		ExponentialBackOffHalfJitterBatchErrorHandlerListener exponentialBackOffHalfJitterBatchErrorHandlerListener() {
			return new ExponentialBackOffHalfJitterBatchErrorHandlerListener();
		}

		@Bean
		LinearBackOffBatchErrorHandlerListener linearBackOffBatchErrorHandlerListener() {
			return new LinearBackOffBatchErrorHandlerListener();
		}

		@Bean
		LinearBackOffErrorHandlerListener linearBackOffErrorHandlerListener() {
			return new LinearBackOffErrorHandlerListener();
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

	private static @NotNull Function<Integer, Integer> getRandomFunction() {
		return timeout -> timeout / 2;
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

	private static long calculateJitterTotalElapsedExpectedTime(long receiveCount, boolean halfJitter) {
		long sum = 0;
		for (int i = 0; i < receiveCount - 1; i++) {
			long timeout = (long) (ExponentialBackOffJitterErrorHandlerListener.initialValueSeconds
					* Math.pow(ExponentialBackOffJitterErrorHandlerListener.multiplier, i));
			sum += timeout / 2;
		}
		return sum;
	}

	private static long calculateLinearTotalElapsedExpectedTime(long receiveCount) {
		return ErrorHandlerVisibilityHelper.calculateVisibilityTimeoutLinearly(receiveCount,
				LinearBackOffErrorHandlerListener.initialValueSeconds, LinearBackOffErrorHandlerListener.increment,
				Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS);
	}

	static class MockedRandomNextInt extends Random {
		final Function<Integer, Integer> nextInt;

		MockedRandomNextInt(Function<Integer, Integer> nextInt) {
			this.nextInt = nextInt;
		}

		@Override
		public int nextInt(int bound) {
			return nextInt.apply(bound);
		}

		@Override
		public int nextInt(int origin, int bound) {
			return nextInt.apply(bound);
		}
	}
}
