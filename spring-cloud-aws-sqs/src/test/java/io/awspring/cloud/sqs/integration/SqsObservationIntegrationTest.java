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
import static org.awaitility.Awaitility.await;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.converter.MessagingMessageHeaders;
import io.awspring.cloud.sqs.support.observation.MessageHeaderContextAccessor;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.observation.tck.ObservationContextAssert;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
 * @author Tomaz Fernandes
 *
 */
@SpringBootTest
class SqsObservationIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsObservationIntegrationTests.class);

	static final String RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME = "observability_on_components_test_queue";

	static final String SYNC_CONTEXT_PROPAGATION_QUEUE_NAME = "sync_context_propagation_test_queue";

	static final String ASYNC_CONTEXT_PROPAGATION_QUEUE_NAME = "async_context_propagation_test_queue";

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
				createQueue(client, SYNC_CONTEXT_PROPAGATION_QUEUE_NAME),
				createQueue(client, ASYNC_CONTEXT_PROPAGATION_QUEUE_NAME)).join();
	}

	@Test
	@DisplayName("Should correctly instrument and propagate observations across multiple components")
	void shouldInstrumentObservationsAcrossThreads() throws Exception {
		sqsTemplate.send(SYNC_CONTEXT_PROPAGATION_QUEUE_NAME, SHOULD_CHANGE_PAYLOAD);
		assertThat(latchContainer.receivesChangedMessageOnErrorLatch.await(20, TimeUnit.SECONDS)).isTrue();
		assertThat(receivesChangedPayloadOnErrorListener.receivedMessages).containsExactly(CHANGED_PAYLOAD);
		assertThat(latchContainer.receivesChangedMessageLatch.await(10, TimeUnit.SECONDS)).isTrue();
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> TestObservationRegistryAssert.then(observationRegistry)
				.hasHandledContextsThatSatisfy(contexts -> {

					assertThat(contexts).hasSizeGreaterThanOrEqualTo(8);

					Optional<Observation.Context> templateWithoutParent = contexts.stream()
							.filter(ctx -> ctx.getName().equals("spring.aws.sqs.template")
									&& ctx.getParentObservation() == null)
							.findFirst();
					assertThat(templateWithoutParent).isPresent();
					ObservationContextAssert.then(templateWithoutParent.get()).hasNameEqualTo("spring.aws.sqs.template")
							.doesNotHaveParentObservation();

					Observation.Context listenerContext = getContextWithNameAndQueueName(contexts,
							"spring.aws.sqs.listener", SYNC_CONTEXT_PROPAGATION_QUEUE_NAME);

					ObservationContextAssert.then(listenerContext).hasNameEqualTo("spring.aws.sqs.listener")
							.doesNotHaveParentObservation();

					List<String> childObservationNames = contexts.stream()
							.filter(ctx -> ctx.getParentObservation() != null && ctx.getParentObservation()
									.getContextView().getName().equals("spring.aws.sqs.listener"))
							.peek(childCtx -> ObservationContextAssert.then(childCtx)
									.hasParentObservationContextMatching(
											parentCtx -> parentCtx.getName().equals("spring.aws.sqs.listener")))
							.map(Observation.Context::getName).collect(Collectors.toList());

					List<String> expectedChildObservations = Arrays.asList("blockingInterceptor.intercept",
							"listener.listen", "spring.aws.sqs.template", "errorHandler.handle",
							"interceptor1.afterProcessing", "interceptor2.afterProcessing");

					assertThat(childObservationNames).containsAll(expectedChildObservations);
				}));
	}

	@Test
	@DisplayName("Should propagate context in asynchronous listener with scope")
	void shouldPropagateContextInAsyncListener() throws Exception {
		// When
		String payload = "test-async-context-propagation";
		sqsTemplate.send(ASYNC_CONTEXT_PROPAGATION_QUEUE_NAME, payload);

		// Then
		assertThat(latchContainer.asyncContextPropagationLatch.await(10, TimeUnit.SECONDS)).isTrue();

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> TestObservationRegistryAssert.then(observationRegistry)
				.hasHandledContextsThatSatisfy(contexts -> {

					assertThat(getContextWithNameAndQueueName(contexts, "spring.aws.sqs.listener",
							ASYNC_CONTEXT_PROPAGATION_QUEUE_NAME)).isNotNull();

					Optional<Observation.Context> childContext = contexts.stream()
							.filter(ctx -> ctx.getName().equals("async.child.operation")).findFirst();

					assertThat(childContext).isPresent();

					// Verify the child has the correct parent
					ObservationContextAssert.then(childContext.get()).hasNameEqualTo("async.child.operation")
							.hasParentObservationContextMatching(
									parentCtx -> parentCtx.getName().equals("spring.aws.sqs.listener")
											&& parentCtx.getContextualName() != null && parentCtx.getContextualName()
													.contains(ASYNC_CONTEXT_PROPAGATION_QUEUE_NAME));
				}));
	}

	static class ReceivesChangedPayloadOnErrorListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		SqsTemplate sqsTemplate;

		@Autowired
		ObservationRegistry observationRegistry;

		Collection<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

		@SqsListener(queueNames = SYNC_CONTEXT_PROPAGATION_QUEUE_NAME, id = "receives-changed-payload-on-error")
		void listen(Message<String> message, @Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			Observation.createNotStarted("listener.listen", observationRegistry).observe(() -> {
				logger.debug("Received message {} with id {} from queue {}", message.getPayload(),
						MessageHeaderUtils.getId(message), queueName);
				if (isChangedPayload(message)) {
					receivedMessages.add(message.getPayload());
					latchContainer.receivesChangedMessageOnErrorLatch.countDown();
				}
			});
			SendResult<String> sendResult = sqsTemplate.send(RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME,
					message.getPayload());
			logger.debug("Sent message by SqsTemplate#sendAsync: {}", sendResult.messageId());
			throw new RuntimeException("Expected exception from receives-changed-payload-on-error");
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

	static class ReceivesChangedMessageInterceptor implements MessageInterceptor<String> {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		SqsTemplate sqsTemplate;

		@Autowired
		ObservationRegistry observationRegistry;

		@Override
		public void afterProcessing(Message<String> message, Throwable t) {
			Observation.createNotStarted("interceptor1.afterProcessing", observationRegistry).observe(() -> {
				logger.debug("ReceivesChangedMessageInterceptor - afterProcessing: {}",
						MessageHeaderUtils.getId(message));
				Optional<Message<String>> result = sqsTemplate
						.receive(RECEIVES_CHANGED_MESSAGE_ON_COMPONENTS_QUEUE_NAME, String.class);
				logger.debug("Received message with SqsTemplate#receive: {}",
						result.map(MessageHeaderUtils::getId).orElse("empty"));
				result.ifPresent(msg -> latchContainer.receivesChangedMessageLatch.countDown());
			});
		}
	}

	static class ReceivesChangedPayloadOnErrorInterceptor implements MessageInterceptor<String> {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		ObservationRegistry observationRegistry;

		@Override
		public void afterProcessing(Message<String> message, Throwable t) {
			Observation.createNotStarted("interceptor2.afterProcessing", observationRegistry).observe(() -> {
				logger.debug("ReceivesChangedPayloadOnErrorInterceptor - afterProcessing: {}",
						MessageHeaderUtils.getId(message));
				if (isChangedPayload(message)) {
					latchContainer.receivesChangedMessageOnErrorLatch.countDown();
				}
			});
		}
	}

	static class AsyncContextPropagationListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		ObservationRegistry observationRegistry;

		private final ContextSnapshotFactory snapshotFactory;

		AsyncContextPropagationListener() {
			ContextRegistry contextRegistry = new ContextRegistry();
			contextRegistry.registerContextAccessor(new MessageHeaderContextAccessor());
			contextRegistry.registerThreadLocalAccessor(new ObservationThreadLocalAccessor());
			this.snapshotFactory = ContextSnapshotFactory.builder().contextRegistry(contextRegistry).build();
		}

		@SqsListener(queueNames = ASYNC_CONTEXT_PROPAGATION_QUEUE_NAME, id = "async-context-propagation", factory = "asyncSqsListenerContainerFactory")
		CompletableFuture<Void> listen(Message<String> message) {
			logger.debug("Received message in async listener: {}", message.getPayload());

			try (ContextSnapshot.Scope scope = snapshotFactory.captureFrom(message.getHeaders()).setThreadLocals()) {

				Observation childObservation = Observation.createNotStarted("async.child.operation",
						observationRegistry);
				childObservation
						.observe(() -> logger.debug("Executing task in child observation with propagated context"));
				latchContainer.asyncContextPropagationLatch.countDown();
			}

			return CompletableFuture.completedFuture(null);
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

		final CountDownLatch asyncContextPropagationLatch = new CountDownLatch(1);

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
			factory.configure(options -> options.observationRegistry(observationRegistry()));
			factory.addMessageInterceptor(interceptor1());
			factory.addMessageInterceptor(interceptor2());
			factory.addMessageInterceptor(blockingInterceptor());
			factory.setErrorHandler(blockErrorHandler());
			return factory;
		}
		
		// @formatter:on

		// @formatter:off
		@Bean
		SqsMessageListenerContainerFactory<String> asyncSqsListenerContainerFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options -> options
				.maxDelayBetweenPolls(Duration.ofSeconds(1))
				.pollTimeout(Duration.ofSeconds(3)));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			factory.configure(options -> options.observationRegistry(observationRegistry()));
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
			return SqsTemplate.builder().configure(options -> options.observationRegistry(observationRegistry()))
					.sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient()).build();
		}

		@Bean
		ReceivesChangedMessageInterceptor interceptor1() {
			return new ReceivesChangedMessageInterceptor();
		}

		@Bean
		ReceivesChangedPayloadOnErrorInterceptor interceptor2() {
			return new ReceivesChangedPayloadOnErrorInterceptor();
		}

		@Bean
		BlockingInterceptor blockingInterceptor() {
			return new BlockingInterceptor();
		}

		@Bean
		AsyncContextPropagationListener asyncContextPropagationListener() {
			return new AsyncContextPropagationListener();
		}

	}

	private static boolean isChangedPayload(Message<?> message) {
		return message != null && message.getPayload().equals(CHANGED_PAYLOAD)
				&& MessageHeaderUtils.getId(message).equals(CHANGED_ID.toString());
	}

	private Observation.Context getContextWithNameAndQueueName(List<Observation.Context> contexts, String name,
			String queueName) {
		return contexts.stream()
				.filter(ctx -> ctx.getName().equals(name) && ctx.getContextualName() != null
						&& ctx.getContextualName().contains(queueName))
				.findFirst().orElseThrow(() -> new AssertionError(
						"Could not find context with name " + name + " and queue " + queueName));
	}

}
