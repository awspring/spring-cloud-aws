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
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.converter.MessagingMessageHeaders;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.ObservationContextAssert;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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
 * Integration tests for the SQS Observation API.
 *
 * @author Mariusz Sondecki
 */
@SpringBootTest
class SqsObservationIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsObservationIntegrationTests.class);

	static final String RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME = "observability_on_components_test_queue";

	static final String RECEIVES_CHANGED_MESSAGE_ON_ERROR_QUEUE_NAME = "observability_on_error_test_queue";

	static final String SHOULD_CHANGE_PAYLOAD = "should-change-payload";

	private static final String CHANGED_PAYLOAD = "Changed payload";

	private static final UUID CHANGED_ID = UUID.fromString("0009f2c8-1dc4-e211-cc99-9f9c62b5e66b");

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	SqsTemplate sqsTemplate;

	@Autowired(required = false)
	ReceivesChangedPayloadOnErrorListener receivesChangedPayloadOnErrorListener;

	@Autowired
	TestObservationRegistry observationRegistry;

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(createQueue(client, RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME),
				createQueue(client, RECEIVES_CHANGED_MESSAGE_ON_ERROR_QUEUE_NAME)).join();
	}

	@Test
	@DisplayName("Should correctly instrument and propagate observations across different threads")
	void shouldInstrumentObservationsAcrossThreads() throws Exception {
		sqsTemplate.send(RECEIVES_CHANGED_MESSAGE_ON_ERROR_QUEUE_NAME, SHOULD_CHANGE_PAYLOAD);
		assertThat(latchContainer.receivesChangedMessageOnErrorLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(receivesChangedPayloadOnErrorListener.receivedMessages).containsExactly(CHANGED_PAYLOAD);
		assertThat(latchContainer.receivesChangedMessageLatch.await(10, TimeUnit.SECONDS)).isTrue();
		TestObservationRegistryAssert.then(observationRegistry).hasNumberOfObservationsEqualTo(9)
				.hasHandledContextsThatSatisfy(contexts -> {
					ObservationContextAssert.then(contexts.get(0)).hasNameEqualTo("sqs.single.message.publish")
							.doesNotHaveParentObservation();

					ObservationContextAssert.then(contexts.get(1)).hasNameEqualTo("sqs.single.message.polling.process")
							.doesNotHaveParentObservation();
					ObservationContextAssert.then(contexts.get(2)).hasNameEqualTo("blockingInterceptor.intercept")
							.hasParentObservationContextMatching(
									contextView -> contextView.getName().equals("sqs.single.message.polling.process"));
					ObservationContextAssert.then(contexts.get(3)).hasNameEqualTo("listener.listen")
							.hasParentObservationContextMatching(
									contextView -> contextView.getName().equals("sqs.single.message.polling.process"));
					ObservationContextAssert.then(contexts.get(4)).hasNameEqualTo("sqs.single.message.publish")
							.hasParentObservationContextMatching(
									contextView -> contextView.getName().equals("sqs.single.message.polling.process"));
					ObservationContextAssert.then(contexts.get(5)).hasNameEqualTo("errorHandler.handle")
							.hasParentObservationContextMatching(
									contextView -> contextView.getName().equals("sqs.single.message.polling.process"));

					ObservationContextAssert.then(contexts.get(6)).hasNameEqualTo("asyncInterceptor1.afterProcessing")
							.hasParentObservationContextMatching(
									contextView -> contextView.getName().equals("sqs.single.message.polling.process"));
					ObservationContextAssert.then(contexts.get(7)).hasNameEqualTo("sqs.single.message.manual.process")
							.hasParentObservationContextMatching(
									contextView -> contextView.getName().equals("asyncInterceptor1.afterProcessing"));
					ObservationContextAssert.then(contexts.get(8)).hasNameEqualTo("asyncInterceptor2.afterProcessing")
							.hasParentObservationContextMatching(
									contextView -> contextView.getName().equals("asyncInterceptor1.afterProcessing"));
				});
	}

	static class ReceivesChangedPayloadOnErrorListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		SqsTemplate sqsTemplate;

		@Autowired
		ObservationRegistry observationRegistry;

		Collection<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

		@SqsListener(queueNames = RECEIVES_CHANGED_MESSAGE_ON_ERROR_QUEUE_NAME, id = "receives-changed-payload-on-error")
		CompletableFuture<Void> listen(Message<String> message,
				@Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			Observation.createNotStarted("listener.listen", observationRegistry).observe(() -> {
				logger.debug("Received message {} with id {} from queue {}", message.getPayload(),
						MessageHeaderUtils.getId(message), queueName);
				if (isChangedPayload(message)) {
					receivedMessages.add(message.getPayload());
					latchContainer.receivesChangedMessageOnErrorLatch.countDown();
				}
			});
			return sqsTemplate.sendAsync(RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME, message.getPayload())
					.thenAccept(result -> logger.debug("Sent message by SqsTemplate#sendAsync: {}", result.messageId()))
					.thenCompose(result -> CompletableFutures.failedFuture(
							new RuntimeException("Expected exception from receives-changed-payload-on-error")));
		}
	}

	static class BlockingErrorHandler implements ErrorHandler<String> {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		ObservationRegistry observationRegistry;

		@Override
		public void handle(Message<String> message, Throwable t) {
			Observation.createNotStarted("errorHandler.handle", observationRegistry).observe(() -> {
				logger.debug("BlockingErrorHandler - handle: {}", MessageHeaderUtils.getId(message));
				if (isChangedPayload(message)) {
					latchContainer.receivesChangedMessageOnErrorLatch.countDown();
				}
			});
		}
	}

	static class ReceivesChangedMessageInterceptor implements AsyncMessageInterceptor<String> {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		SqsTemplate sqsTemplate;

		@Autowired
		ObservationRegistry observationRegistry;

		@Override
		public CompletableFuture<Void> afterProcessing(Message<String> message, Throwable t) {
			return Observation.createNotStarted("asyncInterceptor1.afterProcessing", observationRegistry)
					.observe(() -> {
						logger.debug("ReceivesChangedMessageInterceptor - afterProcessing: {}",
								MessageHeaderUtils.getId(message));
						return sqsTemplate.receiveAsync(RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME, String.class)
								.thenAccept(result -> {
									logger.debug("Received message with SqsTemplate#receiveAsync: {}",
											result.map(MessageHeaderUtils::getId).orElse("empty"));
									result.ifPresent(msg -> latchContainer.receivesChangedMessageLatch.countDown());
								});
					});
		}
	}

	static class ReceivesChangedPayloadOnErrorInterceptor implements AsyncMessageInterceptor<String> {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		ObservationRegistry observationRegistry;

		@Override
		public CompletableFuture<Void> afterProcessing(Message<String> message, Throwable t) {
			return Observation.createNotStarted("asyncInterceptor2.afterProcessing", observationRegistry)
					.observe(() -> {
						logger.debug("ReceivesChangedPayloadOnErrorInterceptor - afterProcessing: {}",
								MessageHeaderUtils.getId(message));
						if (isChangedPayload(message)) {
							latchContainer.receivesChangedMessageOnErrorLatch.countDown();
						}
						return CompletableFuture.completedFuture(null);
					});
		}
	}

	static class BlockingInterceptor implements MessageInterceptor<String> {

		@Autowired
		ObservationRegistry observationRegistry;

		@Override
		public Message<String> intercept(Message<String> message) {
			return Observation.createNotStarted("blockingInterceptor.intercept", observationRegistry).observe(() -> {
				logger.debug("BlockingInterceptor - intercept: {}", MessageHeaderUtils.getId(message));
				if (message.getPayload().equals(SHOULD_CHANGE_PAYLOAD)) {
					MessagingMessageHeaders headers = new MessagingMessageHeaders(message.getHeaders(), CHANGED_ID);
					return MessageBuilder.createMessage(CHANGED_PAYLOAD, headers);
				}
				return message;
			});
		}
	}

	static class LatchContainer {

		final CountDownLatch receivesChangedMessageLatch = new CountDownLatch(1);

		final CountDownLatch receivesChangedMessageOnErrorLatch = new CountDownLatch(3);

	}

	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class SQSConfiguration {

		LatchContainer latchContainer = new LatchContainer();

		// @formatter:off
		@Bean
		SqsMessageListenerContainerFactory<String> defaultSqsListenerContainerFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options -> options
				.maxDelayBetweenPolls(Duration.ofSeconds(1))
				.queueAttributeNames(Collections.singletonList(QueueAttributeName.QUEUE_ARN))
				.acknowledgementMode(AcknowledgementMode.ALWAYS)
				.pollTimeout(Duration.ofSeconds(3)));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			factory.setObservationRegistry(observationRegistry());
			factory.addMessageInterceptor(asyncInterceptor1());
			factory.addMessageInterceptor(asyncInterceptor2());
			factory.addMessageInterceptor(blockingInterceptor());
			factory.setErrorHandler(blockErrorHandler());
			factory.setContainerComponentFactories(Collections.singletonList(getContainerComponentFactory()));
			return factory;
		}
		// @formatter:on

		@Bean
		BlockingErrorHandler blockErrorHandler() {
			return new BlockingErrorHandler();
		}

		@Bean
		ReceivesChangedPayloadOnErrorListener receivesChangedPayloadOnErrorListener() {
			return new ReceivesChangedPayloadOnErrorListener();
		}

		@Bean
		LatchContainer latchContainer() {
			return this.latchContainer;
		}

		@Bean
		TestObservationRegistry observationRegistry() {
			return TestObservationRegistry.create();
		}

		@Bean
		SqsTemplate sqsTemplate() {
			return SqsTemplate.builder().observationRegistry(observationRegistry())
					.sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient()).build();
		}

		@Bean
		ReceivesChangedMessageInterceptor asyncInterceptor1() {
			return new ReceivesChangedMessageInterceptor();
		}

		@Bean
		ReceivesChangedPayloadOnErrorInterceptor asyncInterceptor2() {
			return new ReceivesChangedPayloadOnErrorInterceptor();
		}

		@Bean
		BlockingInterceptor blockingInterceptor() {
			return new BlockingInterceptor();
		}

		private StandardSqsComponentFactory<String> getContainerComponentFactory() {
			return new StandardSqsComponentFactory<>() {
				@Override
				protected BatchingAcknowledgementProcessor<String> createBatchingProcessorInstance() {
					return new BatchingAcknowledgementProcessor<>() {
						@Override
						protected CompletableFuture<Void> sendToExecutor(Collection<Message<String>> messagesToAck) {
							return super.sendToExecutor(messagesToAck).whenComplete((v, t) -> {
								if (messagesToAck.stream().allMatch(SqsObservationIntegrationTests::isChangedPayload)) {
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
