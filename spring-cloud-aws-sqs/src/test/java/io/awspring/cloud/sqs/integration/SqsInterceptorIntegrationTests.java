/*
 * Copyright 2013-2022 the original author or authors.
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

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.StandardSqsComponentFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.BatchingAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.converter.MessagingMessageHeaders;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Integration tests for SQS interceptors.
 *
 * @author Tomaz Fernandes
 */
@SpringBootTest
class SqsInterceptorIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsInterceptorIntegrationTests.class);

	private static final String TEST_SQS_ASYNC_CLIENT_BEAN_NAME = "testSqsAsyncClient";

	private static final String TEST_SQS_TEMPLATE_BEAN_NAME = "testSqsTemplate";

	static final String RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME = "receives_changed_message_on_components_test_queue";

	static final String RECEIVES_CHANGED_MESSAGE_ON_ERROR_QUEUE_NAME = "receives_changed_message_on_error_test_queue";

	static final String SHOULD_CHANGE_PAYLOAD = "should-change-payload";

	private static final String CHANGED_PAYLOAD = "Changed payload";

	private static final UUID CHANGED_ID = UUID.fromString("0009f2c8-1dc4-e211-cc99-9f9c62b5e66b");

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(createQueue(client, RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME),
				createQueue(client, RECEIVES_CHANGED_MESSAGE_ON_ERROR_QUEUE_NAME)).join();
	}

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	@Qualifier(TEST_SQS_TEMPLATE_BEAN_NAME)
	SqsTemplate sqsTemplate;

	@Autowired(required = false)
	ReceivesChangedPayloadListener receivesChangedPayloadListener;

	@Test
	void shouldReceiveChangedMessageOnComponents() throws Exception {
		sqsTemplate.send(RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME, SHOULD_CHANGE_PAYLOAD);
		logger.debug("Sent message to queue {} with messageBody {}", RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME,
				SHOULD_CHANGE_PAYLOAD);
		assertThat(latchContainer.receivesChangedMessageLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(receivesChangedPayloadListener.receivedMessages).containsExactly(CHANGED_PAYLOAD);
	}

	@Test
	void shouldReceiveChangedMessageOnComponentsWhenError() throws Exception {
		sqsTemplate.send(RECEIVES_CHANGED_MESSAGE_ON_ERROR_QUEUE_NAME, SHOULD_CHANGE_PAYLOAD);
		logger.debug("Sent message to queue {} with messageBody {}", RECEIVES_CHANGED_MESSAGE_ON_ERROR_QUEUE_NAME,
				SHOULD_CHANGE_PAYLOAD);
		assertThat(latchContainer.receivesChangedMessageOnErrorLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	static class ReceivesChangedPayloadListener {

		@Autowired
		LatchContainer latchContainer;

		Collection<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

		@SqsListener(queueNames = RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME, id = "receives-changed-payload-on-success")
		void listen(Message<String> message, @Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			logger.debug("Received message {} with id {} from queue {}", message.getPayload(),
					MessageHeaderUtils.getId(message), queueName);
			if (isChangedPayload(message)) {
				receivedMessages.add(message.getPayload());
				latchContainer.receivesChangedMessageLatch.countDown();
			}
		}
	}

	static class ReceivesChangedPayloadOnErrorListener {

		@Autowired
		LatchContainer latchContainer;

		Collection<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

		@SqsListener(queueNames = RECEIVES_CHANGED_MESSAGE_ON_ERROR_QUEUE_NAME, id = "receives-changed-payload-on-error")
		void listen(Message<String> message, @Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			logger.debug("Received message {} with id {} from queue {}", message.getPayload(),
					MessageHeaderUtils.getId(message), queueName);
			if (isChangedPayload(message)) {
				receivedMessages.add(message.getPayload());
				latchContainer.receivesChangedMessageOnErrorLatch.countDown();
			}
			throw new RuntimeException("Expected exception from receives-changed-payload-on-error");
		}
	}

	static class LatchContainer {

		final CountDownLatch receivesChangedMessageLatch = new CountDownLatch(3);

		final CountDownLatch receivesChangedMessageOnErrorLatch = new CountDownLatch(3);

	}

	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class SQSConfiguration {

		// @formatter:off
		@Bean
		public SqsMessageListenerContainerFactory<String> defaultSqsListenerContainerFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options -> options
				.maxDelayBetweenPolls(Duration.ofSeconds(1))
				.queueAttributeNames(Collections.singletonList(QueueAttributeName.QUEUE_ARN))
				.acknowledgementMode(AcknowledgementMode.ALWAYS)
				.pollTimeout(Duration.ofSeconds(3)));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			factory.addMessageInterceptor(getMessageInterceptor());
			factory.setErrorHandler(getErrorHandler());
			factory.setContainerComponentFactories(Collections.singletonList(getContainerComponentFactory()));
			return factory;
		}
		// @formatter:on

		@Bean
		ReceivesChangedPayloadListener receivesChangedPayloadListener() {
			return new ReceivesChangedPayloadListener();
		}

		@Bean
		ReceivesChangedPayloadOnErrorListener receivesChangedPayloadOnErrorListener() {
			return new ReceivesChangedPayloadOnErrorListener();
		}

		LatchContainer latchContainer = new LatchContainer();

		@Bean
		LatchContainer latchContainer() {
			return this.latchContainer;
		}

		@Bean(name = TEST_SQS_ASYNC_CLIENT_BEAN_NAME)
		SqsAsyncClient sqsAsyncClientProducer() {
			return BaseSqsIntegrationTest.createAsyncClient();
		}

		@Bean(name = TEST_SQS_TEMPLATE_BEAN_NAME)
		SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
			return SqsTemplate.builder().sqsAsyncClient(sqsAsyncClient).build();
		}

		private AsyncMessageInterceptor<String> getMessageInterceptor() {
			return new AsyncMessageInterceptor<String>() {
				@Override
				public CompletableFuture<Message<String>> intercept(Message<String> message) {
					logger.debug("Received message in interceptor: {}", MessageHeaderUtils.getId(message));
					if (message.getPayload().equals(SHOULD_CHANGE_PAYLOAD)) {
						MessagingMessageHeaders headers = new MessagingMessageHeaders(message.getHeaders(), CHANGED_ID);
						return CompletableFuture
								.completedFuture(MessageBuilder.createMessage(CHANGED_PAYLOAD, headers));
					}
					return CompletableFuture.completedFuture(message);
				}

				@Override
				public CompletableFuture<Void> afterProcessing(Message<String> message, Throwable t) {
					logger.debug("Received message in afterProcessing: {}", MessageHeaderUtils.getId(message));
					if (isChangedPayload(message)) {
						latchContainer.receivesChangedMessageLatch.countDown();
						latchContainer.receivesChangedMessageOnErrorLatch.countDown();
					}
					return CompletableFuture.completedFuture(null);
				}
			};
		}

		private AsyncErrorHandler<String> getErrorHandler() {
			return new AsyncErrorHandler<String>() {
				@Override
				public CompletableFuture<Void> handle(Message<String> message, Throwable t) {
					logger.debug("Received message in error handler: {}", MessageHeaderUtils.getId(message));
					if (isChangedPayload(message)) {
						latchContainer.receivesChangedMessageOnErrorLatch.countDown();
					}
					return CompletableFutures.failedFuture(t);
				}
			};
		}

		private StandardSqsComponentFactory<String> getContainerComponentFactory() {
			return new StandardSqsComponentFactory<String>() {
				@Override
				protected BatchingAcknowledgementProcessor<String> createBatchingProcessorInstance() {
					return new BatchingAcknowledgementProcessor<String>() {
						@Override
						protected CompletableFuture<Void> sendToExecutor(Collection<Message<String>> messagesToAck) {
							return super.sendToExecutor(messagesToAck).whenComplete((v, t) -> {
								if (messagesToAck.stream().allMatch(SqsInterceptorIntegrationTests::isChangedPayload)) {
									latchContainer.receivesChangedMessageLatch.countDown();
									latchContainer.receivesChangedMessageOnErrorLatch.countDown();
								}
							});
						}
					};
				}
			};
		}

	}

	private static boolean isChangedPayload(Message<?> message) {
		return message != null && message.getPayload().equals(CHANGED_PAYLOAD)
				&& MessageHeaderUtils.getId(message).equals(CHANGED_ID.toString());
	}

}
