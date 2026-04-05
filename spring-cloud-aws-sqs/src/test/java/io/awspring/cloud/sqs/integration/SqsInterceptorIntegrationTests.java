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
import io.awspring.cloud.sqs.listener.InterceptorExecutionFailedException;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.StandardSqsComponentFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Integration tests for SQS interceptors.
 *
 * @author Tomaz Fernandes
 * @author Mikhail Strokov
 */
@SpringBootTest
class SqsInterceptorIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsInterceptorIntegrationTests.class);

	static final String RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME = "receives_changed_message_on_components_test_queue";

	static final String RECEIVES_CHANGED_MESSAGE_ON_ERROR_QUEUE_NAME = "receives_changed_message_on_error_test_queue";

	static final String INTERCEPTOR_THROWS_QUEUE_NAME = "interceptor_throws_test_queue";

	static final String INTERCEPTOR_THROWS_BATCH_QUEUE_NAME = "interceptor_throws_batch_test_queue";

	static final String INTERCEPTOR_THROWS_RECOVERS_QUEUE_NAME = "interceptor_throws_recovers_test_queue";

	static final String SHOULD_CHANGE_PAYLOAD = "should-change-payload";

	private static final String CHANGED_PAYLOAD = "Changed payload";

	private static final UUID CHANGED_ID = UUID.fromString("0009f2c8-1dc4-e211-cc99-9f9c62b5e66b");

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(createQueue(client, RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME),
				createQueue(client, RECEIVES_CHANGED_MESSAGE_ON_ERROR_QUEUE_NAME),
				createQueue(client, INTERCEPTOR_THROWS_QUEUE_NAME),
				createQueue(client, INTERCEPTOR_THROWS_BATCH_QUEUE_NAME),
				createQueue(client, INTERCEPTOR_THROWS_RECOVERS_QUEUE_NAME)).join();
	}

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	SqsTemplate sqsTemplate;

	@Autowired(required = false)
	ReceivesChangedPayloadListener receivesChangedPayloadListener;

	@Test
	void shouldInvokeErrorHandlerAndAfterProcessingInterceptorWhenInterceptorThrows() throws Exception {
		sqsTemplate.send(INTERCEPTOR_THROWS_QUEUE_NAME, "any-payload");
		assertThat(latchContainer.interceptorThrowsErrorHandlerLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.interceptorThrowsAfterProcessingLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.interceptorThrowsException.get().getCause())
				.isInstanceOf(InterceptorExecutionFailedException.class);
		assertThat(latchContainer.interceptorThrowsAfterProcessingException.get().getCause())
				.isInstanceOf(InterceptorExecutionFailedException.class);
	}

	@Test
	void shouldInvokeErrorHandlerAndAfterProcessingInterceptorWhenInterceptorThrowsBatch() throws Exception {
		sqsTemplate.send(INTERCEPTOR_THROWS_BATCH_QUEUE_NAME, "any-payload");
		assertThat(latchContainer.interceptorThrowsBatchErrorHandlerLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.interceptorThrowsBatchAfterProcessingLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.interceptorThrowsBatchException.get().getCause())
				.isInstanceOf(InterceptorExecutionFailedException.class);
		assertThat(latchContainer.interceptorThrowsBatchAfterProcessingException.get().getCause())
				.isInstanceOf(InterceptorExecutionFailedException.class);
	}

	@Test
	void shouldAcknowledgeAndNotProcessWhenErrorHandlerRecoversInterceptorException() throws Exception {
		sqsTemplate.send(INTERCEPTOR_THROWS_RECOVERS_QUEUE_NAME, "any-payload");
		assertThat(latchContainer.interceptorThrowsRecoversAckLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.interceptorThrowsRecoversAfterProcessingLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.interceptorThrowsRecoversAfterProcessingException.get()).isNull();
		assertThat(latchContainer.interceptorThrowsRecoversListenerCalled).isFalse();
	}

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

	static class InterceptorThrowsRecoversListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = INTERCEPTOR_THROWS_RECOVERS_QUEUE_NAME, id = "interceptor-throws-recovers", factory = "interceptorThrowsRecoversFactory")
		void listen(Message<String> message) {
			latchContainer.interceptorThrowsRecoversListenerCalled = true;
		}

	}

	static class InterceptorThrowsListener {

		@SqsListener(queueNames = INTERCEPTOR_THROWS_QUEUE_NAME, id = "interceptor-throws", factory = "interceptorThrowsFactory")
		void listen(Message<String> message) {
			throw new RuntimeException("Listener should not be called when interceptor throws");
		}

	}

	static class InterceptorThrowsBatchListener {

		@SqsListener(queueNames = INTERCEPTOR_THROWS_BATCH_QUEUE_NAME, id = "interceptor-throws-batch", factory = "interceptorThrowsBatchFactory")
		void listen(List<Message<String>> messages) {
			throw new RuntimeException("Listener should not be called when interceptor throws");
		}

	}

	static class LatchContainer {

		final CountDownLatch receivesChangedMessageLatch = new CountDownLatch(3);

		final CountDownLatch receivesChangedMessageOnErrorLatch = new CountDownLatch(3);

		final CountDownLatch interceptorThrowsErrorHandlerLatch = new CountDownLatch(1);

		final CountDownLatch interceptorThrowsAfterProcessingLatch = new CountDownLatch(1);

		final AtomicReference<Throwable> interceptorThrowsException = new AtomicReference<>();

		final AtomicReference<Throwable> interceptorThrowsAfterProcessingException = new AtomicReference<>();

		final CountDownLatch interceptorThrowsBatchErrorHandlerLatch = new CountDownLatch(1);

		final CountDownLatch interceptorThrowsBatchAfterProcessingLatch = new CountDownLatch(1);

		final AtomicReference<Throwable> interceptorThrowsBatchException = new AtomicReference<>();

		final AtomicReference<Throwable> interceptorThrowsBatchAfterProcessingException = new AtomicReference<>();

		final CountDownLatch interceptorThrowsRecoversAckLatch = new CountDownLatch(1);

		final CountDownLatch interceptorThrowsRecoversAfterProcessingLatch = new CountDownLatch(1);

		final AtomicReference<Throwable> interceptorThrowsRecoversAfterProcessingException = new AtomicReference<>();

		volatile boolean interceptorThrowsRecoversListenerCalled = false;

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
		public SqsMessageListenerContainerFactory<String> interceptorThrowsFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options -> options.maxDelayBetweenPolls(Duration.ofSeconds(1))
					.acknowledgementMode(AcknowledgementMode.ALWAYS).pollTimeout(Duration.ofSeconds(3)));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			factory.addMessageInterceptor(new AsyncMessageInterceptor<String>() {
				@Override
				public CompletableFuture<Message<String>> intercept(Message<String> message) {
					throw new RuntimeException("Expected interceptor exception");
				}

				@Override
				public CompletableFuture<Void> afterProcessing(Message<String> message, Throwable t) {
					latchContainer.interceptorThrowsAfterProcessingException.set(t);
					latchContainer.interceptorThrowsAfterProcessingLatch.countDown();
					return CompletableFuture.completedFuture(null);
				}
			});
			factory.setErrorHandler(new AsyncErrorHandler<String>() {
				@Override
				public CompletableFuture<Void> handle(Message<String> message, Throwable t) {
					latchContainer.interceptorThrowsException.set(t);
					latchContainer.interceptorThrowsErrorHandlerLatch.countDown();
					return CompletableFutures.failedFuture(t);
				}
			});
			return factory;
		}

		@Bean
		public SqsMessageListenerContainerFactory<String> interceptorThrowsBatchFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options -> options.maxDelayBetweenPolls(Duration.ofSeconds(1))
					.acknowledgementMode(AcknowledgementMode.ALWAYS).pollTimeout(Duration.ofSeconds(3)));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			factory.addMessageInterceptor(new AsyncMessageInterceptor<String>() {
				@Override
				public CompletableFuture<Collection<Message<String>>> intercept(Collection<Message<String>> messages) {
					throw new RuntimeException("Expected batch interceptor exception");
				}

				@Override
				public CompletableFuture<Void> afterProcessing(Collection<Message<String>> messages, Throwable t) {
					latchContainer.interceptorThrowsBatchAfterProcessingException.set(t);
					latchContainer.interceptorThrowsBatchAfterProcessingLatch.countDown();
					return CompletableFuture.completedFuture(null);
				}
			});
			factory.setErrorHandler(new AsyncErrorHandler<String>() {
				@Override
				public CompletableFuture<Void> handle(Collection<Message<String>> messages, Throwable t) {
					latchContainer.interceptorThrowsBatchException.set(t);
					latchContainer.interceptorThrowsBatchErrorHandlerLatch.countDown();
					return CompletableFutures.failedFuture(t);
				}
			});
			return factory;
		}

		@Bean
		public SqsMessageListenerContainerFactory<String> interceptorThrowsRecoversFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options -> options.maxDelayBetweenPolls(Duration.ofSeconds(1))
					.acknowledgementMode(AcknowledgementMode.ON_SUCCESS).pollTimeout(Duration.ofSeconds(3)));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			factory.addMessageInterceptor(new AsyncMessageInterceptor<String>() {
				@Override
				public CompletableFuture<Message<String>> intercept(Message<String> message) {
					throw new RuntimeException("Simulated duplicate — interceptor rejects message");
				}

				@Override
				public CompletableFuture<Void> afterProcessing(Message<String> message, Throwable t) {
					latchContainer.interceptorThrowsRecoversAfterProcessingException.set(t);
					latchContainer.interceptorThrowsRecoversAfterProcessingLatch.countDown();
					return CompletableFuture.completedFuture(null);
				}
			});
			factory.setErrorHandler(new AsyncErrorHandler<String>() {
				@Override
				public CompletableFuture<Void> handle(Message<String> message, Throwable t) {
					// swallow — message is considered handled (e.g. duplicate, skip it)
					return CompletableFuture.completedFuture(null);
				}
			});
			factory.setAcknowledgementResultCallback(new AcknowledgementResultCallback<String>() {
				@Override
				public void onSuccess(Collection<Message<String>> messages) {
					latchContainer.interceptorThrowsRecoversAckLatch.countDown();
				}
			});
			return factory;
		}

		@Bean
		InterceptorThrowsRecoversListener interceptorThrowsRecoversListener() {
			return new InterceptorThrowsRecoversListener();
		}

		@Bean
		InterceptorThrowsListener interceptorThrowsListener() {
			return new InterceptorThrowsListener();
		}

		@Bean
		InterceptorThrowsBatchListener interceptorThrowsBatchListener() {
			return new InterceptorThrowsBatchListener();
		}

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

		@Bean
		SqsTemplate sqsTemplate() {
			return SqsTemplate.builder().sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient()).build();
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
