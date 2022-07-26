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
package io.awspring.cloud.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsListenerCustomizer;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.StandardSqsComponentFactory;
import io.awspring.cloud.sqs.listener.Visibility;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import io.awspring.cloud.sqs.listener.SqsMessageHeaders;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.awspring.cloud.sqs.listener.acknowledgement.AckHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import io.awspring.cloud.sqs.listener.acknowledgement.OnSuccessAckHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.source.MessageSource;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
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
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@Execution(CONCURRENT)
@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = { "spring.cloud.aws.credentials.access-key=noop",
		"spring.cloud.aws.credentials.secret-key=noop", "spring.cloud.aws.region.static=us-east-2" })
class SqsIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsIntegrationTests.class);

	private static final String TEST_SQS_ASYNC_CLIENT_BEAN_NAME = "testSqsAsyncClient";

	private static final String HIGH_THROUGHPUT_FACTORY_NAME = "highThroughputFactory";

	private static final String LOW_RESOURCE_FACTORY_NAME = "lowResourceFactory";

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	@Qualifier(TEST_SQS_ASYNC_CLIENT_BEAN_NAME)
	SqsAsyncClient sqsAsyncClient;

	@Autowired
	MessageListenerContainerRegistry registry;

	@Autowired
	ObjectMapper objectMapper;

	private static final String TEST_PAYLOAD = "My test";

	@Test
	void contextLoads() throws Exception {
		sqsAsyncClient.createQueue(req -> req.queueName("myQueue").build()).get();
		sendMessageTo("myQueue");
		String queueUrl = fetchQueueUrl("myQueue");
		ReceiveMessageResponse response = sqsAsyncClient
				.receiveMessage(rec -> rec.queueUrl(queueUrl).maxNumberOfMessages(10)).get();
		assertThat(response.messages().get(0).body()).isEqualTo(TEST_PAYLOAD);
	}

	@Test
	void receivesMessage() throws Exception {
		sendMessageTo(RECEIVES_MESSAGE_QUEUE_NAME);
		assertThat(latchContainer.receivesMessageLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.invocableHandlerMethodLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void doesNotAckOnError() throws Exception {
		sendMessageTo(DOES_NOT_ACK_ON_ERROR_QUEUE_NAME);
		assertThat(latchContainer.doesNotAckLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.errorHandlerLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void resolvesManyParameterTypes() throws Exception {
		sendMessageTo(RESOLVES_PARAMETER_TYPES_QUEUE_NAME);
		assertThat(latchContainer.manyParameterTypesLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.manyParameterTypesSecondLatch.await(1, TimeUnit.SECONDS)).isFalse();
	}

	@Test
	void resolvesPojoParameterTypes() throws Exception {
		sendMessageTo(RESOLVES_POJO_TYPES_QUEUE_NAME,
				objectMapper.writeValueAsString(new MyPojo("firstValue", "secondValue")));
		assertThat(latchContainer.resolvesPojoLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.interceptorLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void manuallyCreatesContainer() throws Exception {
		sendMessageTo(MANUALLY_CREATE_CONTAINER_QUEUE_NAME, "MyTest");
		assertThat(latchContainer.manuallyCreatedContainerLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void manuallyStartsContainerAndChangesComponent() throws Exception {
		SqsMessageListenerContainer<String> container = new SqsMessageListenerContainer<>(createAsyncClient(),
				ContainerOptions.create().permitAcquireTimeout(Duration.ofSeconds(1))
						.pollTimeout(Duration.ofSeconds(1)));
		container.setQueueNames(MANUALLY_START_CONTAINER);
		container.setMessageListener(msg -> latchContainer.manuallyStartedContainerLatch.countDown());
		container.start();
		sendMessageTo(MANUALLY_START_CONTAINER, "MyTest");
		assertThat(latchContainer.manuallyStartedContainerLatch.await(2, TimeUnit.SECONDS)).isTrue();
		container.stop();
		container.setMessageListener(msg -> latchContainer.manuallyStartedContainerLatch2.countDown());
		container.start();
		sendMessageTo(MANUALLY_START_CONTAINER, "MyTest2");
		assertThat(latchContainer.manuallyStartedContainerLatch2.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void manuallyCreatesFactory() throws Exception {
		sendMessageTo(MANUALLY_CREATE_FACTORY_QUEUE_NAME, "MyTest");
		assertThat(latchContainer.manuallyCreatedFactoryLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.manuallyCreatedFactorySourceFactoryLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.manuallyCreatedFactorySinkLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.manuallyCreatedFactoryAckHandlerLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	final int outerBatchSize = 1; // 50;
	final int innerBatchSize = 1; // 10;
	final int totalMessages = 2 * outerBatchSize * innerBatchSize;

	// These tests are really only for us to have some indication on how the system performs under load.
	// We can probably remove them later, or adapt to only make sure it handles more than one queue.

	@Test
	void receivesManyFromTwoQueuesWithLoad() throws Exception {
		latchContainer.manyMessagesTwoQueuesLatch = new CountDownLatch(totalMessages);
		latchContainer.messageAckLatch = new CountDownLatch(totalMessages);
		testWithLoad(RECEIVE_FROM_MANY_1_QUEUE_NAME, RECEIVE_FROM_MANY_2_QUEUE_NAME,
				latchContainer.manyMessagesTwoQueuesLatch, latchContainer.messageAckLatch);
	}

	@Test
	void receivesBatchesFromTwoQueuesWithLoad() throws Exception {
		latchContainer.batchesTwoQueuesLatch = new CountDownLatch(totalMessages);
		latchContainer.batchesAckLatch = new CountDownLatch(totalMessages);
		testWithLoad(RECEIVE_BATCH_1_QUEUE_NAME, RECEIVE_BATCH_2_QUEUE_NAME, latchContainer.batchesTwoQueuesLatch,
				latchContainer.batchesAckLatch, pojoBodyFunction());
	}

	private BiFunction<Integer, Integer, String> pojoBodyFunction() {
		return (parent, index) -> {
			try {
				return objectMapper
						.writeValueAsString(new MyPojo(TEST_PAYLOAD + " - " + ((parent * 10) + index), "secondValue"));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	private void testWithLoad(String queue1, String queue2, CountDownLatch countDownLatch, CountDownLatch secondLatch)
			throws InterruptedException, ExecutionException {
		testWithLoad(queue1, queue2, countDownLatch, secondLatch,
				(parent, index) -> TEST_PAYLOAD + " - " + ((parent * 10) + index));
	}

	private void testWithLoad(String queue1, String queue2, CountDownLatch countDownLatch, CountDownLatch secondLatch,
			BiFunction<Integer, Integer, String> bodyFunction) throws InterruptedException, ExecutionException {
		String queueUrl1 = fetchQueueUrl(queue1);
		String queueUrl2 = fetchQueueUrl(queue2);
		StopWatch watch = new StopWatch();
		watch.start();
		IntStream.range(0, outerBatchSize).forEach(index -> {
			sendMessageBatch(queueUrl1, index, innerBatchSize, bodyFunction);
			sendMessageBatch(queueUrl2, index + outerBatchSize, innerBatchSize, bodyFunction);
		});
		assertThat(countDownLatch.await(600, TimeUnit.SECONDS)).isTrue();
		assertThat(secondLatch.await(600, TimeUnit.SECONDS)).isTrue();
		watch.stop();
		double totalTimeSeconds = watch.getTotalTimeSeconds();
		logger.info("{} seconds for sending and consuming {} messages. Messages / second: {}", totalTimeSeconds,
				totalMessages, totalMessages / totalTimeSeconds);
	}

	private void sendMessageBatch(String queueUrl, int parentIndex, int batchSize,
			BiFunction<Integer, Integer, String> bodyFunction) {

		sqsAsyncClient.sendMessageBatch(req -> req.entries(getBatchEntries(batchSize, parentIndex, bodyFunction))
					.queueUrl(queueUrl).build()).thenRun(() -> {
						if (parentIndex % 5 == 0) {
							logger.debug("Sent " + parentIndex * batchSize + " messages to queue {}", queueUrl);
						}
						logger.trace("Sending {} messages from parent index {}", batchSize, parentIndex);
					}).exceptionally(t -> {
						logger.error("Error sending messages", t);
						return null;
					});
	}

	private SendMessageBatchRequestEntry[] getBatchEntries(int batchSize, int parentIndex,
			BiFunction<Integer, Integer, String> bodyFunction) {
		return IntStream.range(0, batchSize)
				.mapToObj(index -> SendMessageBatchRequestEntry.builder().id(UUID.randomUUID().toString())
						.messageBody(bodyFunction.apply(parentIndex, index)).build())
				.toArray(SendMessageBatchRequestEntry[]::new);
	}

	private void sendMessageTo(String receivesMessageQueueName) throws InterruptedException, ExecutionException {
		String queueUrl = fetchQueueUrl(receivesMessageQueueName);
		logger.trace("Sending message {} to {}", receivesMessageQueueName, queueUrl);
		sqsAsyncClient.sendMessage(req -> req.messageBody(TEST_PAYLOAD).queueUrl(queueUrl).build()).get();
	}

	private void sendMessageTo(String queueName, String messageBody) throws InterruptedException, ExecutionException {
		String queueUrl = fetchQueueUrl(queueName);
		sqsAsyncClient.sendMessage(req -> req.messageBody(messageBody).queueUrl(queueUrl).build()).get();
		logger.debug("Sent message to queue {} with messageBody {}", queueName, messageBody);
	}

	private String fetchQueueUrl(String receivesMessageQueueName) throws InterruptedException, ExecutionException {
		return sqsAsyncClient.getQueueUrl(req -> req.queueName(receivesMessageQueueName)).get().queueUrl();
	}

	static class ReceivesMessageListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = RECEIVES_MESSAGE_QUEUE_NAME, id = "receivesMessageContainer")
		void listen(String message) {
			logger.debug("Received message in Listener Method: " + message);
			latchContainer.receivesMessageLatch.countDown();
		}
	}

	static class DoesNotAckOnErrorListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = DOES_NOT_ACK_ON_ERROR_QUEUE_NAME, factory = LOW_RESOURCE_FACTORY_NAME, id = "does-not-ack")
		void listen(String message, @Header(SqsMessageHeaders.SQS_LOGICAL_RESOURCE_ID) String queueName) {
			logger.debug("Received message {} from queue {}", message, queueName);
			latchContainer.doesNotAckLatch.countDown();
			throw new RuntimeException("Expected exception");
		}
	}

	static class ResolvesParameterTypesListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = RESOLVES_PARAMETER_TYPES_QUEUE_NAME, factory = LOW_RESOURCE_FACTORY_NAME, messageVisibilitySeconds = "1", id = "resolves-parameter")
		void listen(Message<String> message, SqsMessageHeaders headers, Acknowledgement ack, Visibility visibility,
				software.amazon.awssdk.services.sqs.model.Message originalMessage) throws Exception {
			Assert.notNull(headers, "Received null SqsMessageHeaders");
			Assert.notNull(ack, "Received null AsyncAcknowledgement");
			Assert.notNull(visibility, "Received null Visibility");
			Assert.notNull(originalMessage, "Received null software.amazon.awssdk.services.sqs.model.Message");
			Assert.notNull(message, "Received null message");
			logger.debug("Received message in Listener Method: " + message);

			// Verify VisibilityTimeout extension
			Thread.sleep(1000);
			latchContainer.manyParameterTypesLatch.countDown();
			latchContainer.manyParameterTypesSecondLatch.countDown();
		}
	}

	static class ResolvesPojoListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = RESOLVES_POJO_TYPES_QUEUE_NAME, factory = LOW_RESOURCE_FACTORY_NAME, id = "resolves-pojo")
		void listen(MyPojo pojo, @Header(SqsMessageHeaders.SQS_LOGICAL_RESOURCE_ID) String queueName) {
			Assert.notNull(pojo, "Received null message");
			logger.debug("Received message {} from queue {}", pojo, queueName);
			latchContainer.resolvesPojoLatch.countDown();
		}
	}

	static class ReceiveManyFromTwoQueuesListener {

		Set<String> messagesReceivedPayload = Collections.synchronizedSet(new HashSet<>());

		@Autowired
		LatchContainer latchContainer;

		AtomicInteger messagesReceived = new AtomicInteger();

		@SqsListener(queueNames = { RECEIVE_FROM_MANY_1_QUEUE_NAME, RECEIVE_FROM_MANY_2_QUEUE_NAME},
			factory = HIGH_THROUGHPUT_FACTORY_NAME)
		void listen(Message<String> message) {
			logger.trace("Started processing {}", MessageHeaderUtils.getId(message));
			if (this.messagesReceivedPayload.contains(message.getPayload())) {
				logger.warn("Received duplicated message: {}", message);
			}
			this.messagesReceivedPayload.add(message.getPayload());
			try {
				int sleepTime = 1000;//new Random().nextInt(1000);
				Thread.sleep(sleepTime);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				// do nothing
			}
			latchContainer.manyMessagesTwoQueuesLatch.countDown();
			int count;
			if ((count = messagesReceived.incrementAndGet()) % 50 == 0) {
				logger.debug("Listener processed {} messages", count);
			}
			logger.trace("Finished processing {}", MessageHeaderUtils.getId(message));
		}
	}

	static class ReceiveBatchesFromTwoQueuesListener {

		@Autowired
		LatchContainer latchContainer;

		AtomicInteger messagesReceived = new AtomicInteger();

		AtomicInteger batchesReceived = new AtomicInteger();

		@SqsListener(queueNames = { RECEIVE_BATCH_1_QUEUE_NAME,
				RECEIVE_BATCH_2_QUEUE_NAME }, factory = HIGH_THROUGHPUT_FACTORY_NAME)
		void listen(Collection<Message<MyPojo>> messages) {
			logger.trace("Started processing messages {}", MessageHeaderUtils.getId(messages));
			String firstField = messages.iterator().next().getPayload().firstField;// Make sure we got the right type
			try {
				Thread.sleep(2000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				// do nothing
			}
			messages.forEach(msg -> latchContainer.batchesTwoQueuesLatch.countDown());
			int messagesCount = this.messagesReceived.addAndGet(messages.size());
			int batches;
			if ((batches = batchesReceived.incrementAndGet()) % 5 == 0) {
				logger.debug("Listener processed {} batches and {} messages", batches, messagesCount);
			}
			logger.trace("Finished processing {} messages", messages.size());
		}
	}

	static class LatchContainer {

		final CountDownLatch receivesMessageLatch = new CountDownLatch(1);
		final CountDownLatch doesNotAckLatch = new CountDownLatch(2);
		final CountDownLatch errorHandlerLatch = new CountDownLatch(2);
		final CountDownLatch interceptorLatch = new CountDownLatch(2);
		final CountDownLatch manyParameterTypesLatch = new CountDownLatch(1);
		final CountDownLatch manyParameterTypesSecondLatch = new CountDownLatch(2);
		final CountDownLatch resolvesPojoLatch = new CountDownLatch(1);
		final CountDownLatch manuallyCreatedContainerLatch = new CountDownLatch(1);
		final CountDownLatch manuallyStartedContainerLatch = new CountDownLatch(1);
		final CountDownLatch manuallyStartedContainerLatch2 = new CountDownLatch(1);
		final CountDownLatch manuallyCreatedFactorySourceFactoryLatch = new CountDownLatch(1);
		final CountDownLatch manuallyCreatedFactorySinkLatch = new CountDownLatch(1);
		final CountDownLatch manuallyCreatedFactoryAckHandlerLatch = new CountDownLatch(1);
		final CountDownLatch manuallyCreatedFactoryLatch = new CountDownLatch(1);
		final CountDownLatch invocableHandlerMethodLatch = new CountDownLatch(1);

		// Lazily initialized
		CountDownLatch orderedLoadLatch = new CountDownLatch(1);
		CountDownLatch manyMessagesTwoQueuesLatch = new CountDownLatch(1);
		CountDownLatch messageAckLatch = new CountDownLatch(1);
		CountDownLatch batchesAckLatch = new CountDownLatch(1);
		CountDownLatch batchesTwoQueuesLatch = new CountDownLatch(1);

	}

	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class SQSConfiguration {

		@Bean
		public SqsMessageListenerContainerFactory<String> defaultSqsListenerContainerFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.getContainerOptions().permitAcquireTimeout(Duration.ofSeconds(1))
					.pollTimeout(Duration.ofSeconds(1));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			return factory;
		}

		@Bean
		public SqsMessageListenerContainerFactory<String> highThroughputFactory() {
			// For load tests, set maxInflightMessagesPerQueue to a higher value - e.g. 600
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.getContainerOptions()
				.maxInflightMessagesPerQueue(10)
				.pollTimeout(Duration.ofSeconds(1))
				.messagesPerPoll(10)
				.permitAcquireTimeout(Duration.ofSeconds(1))
				.backPressureMode(BackPressureMode.HIGH_THROUGHPUT)
				.shutDownTimeout(Duration.ofSeconds(40));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createHighThroughputAsyncClient);
			factory.setContainerComponentFactory(getTestAckHandlerComponentFactory());
			return factory;
		}

		@Bean
		public SqsMessageListenerContainerFactory<String> lowResourceFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.getContainerOptions()
				.maxInflightMessagesPerQueue(1)
				.pollTimeout(Duration.ofSeconds(1))
				.messagesPerPoll(1)
				.permitAcquireTimeout(Duration.ofSeconds(1));
			factory.setErrorHandler(testErrorHandler());
			factory.addMessageInterceptor(testInterceptor());
			factory.addMessageInterceptor(testInterceptor());
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			return factory;
		}

		@Bean
		public MessageListenerContainer<String> manuallyCreatedContainer() {
			SqsMessageListenerContainer<String> container = new SqsMessageListenerContainer<>(createAsyncClient(),
					ContainerOptions.create().permitAcquireTimeout(Duration.ofSeconds(1))
							.pollTimeout(Duration.ofSeconds(1)));
			container.setQueueNames(MANUALLY_CREATE_CONTAINER_QUEUE_NAME);
			container.setMessageListener(msg -> latchContainer.manuallyCreatedContainerLatch.countDown());
			return container;
		}

		@Bean
		public SqsMessageListenerContainer<String> manuallyCreatedFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.getContainerOptions().maxInflightMessagesPerQueue(1).pollTimeout(Duration.ofSeconds(2))
					.messagesPerPoll(1).permitAcquireTimeout(Duration.ofSeconds(1))
					.pollTimeout(Duration.ofSeconds(1));
			factory.setContainerComponentFactory(new StandardSqsComponentFactory<String>() {
				@Override
				public MessageSource<String> createMessageSource() {
					latchContainer.manuallyCreatedFactorySourceFactoryLatch.countDown();
					return super.createMessageSource();
				}

				@Override
				public MessageSink<String> createMessageSink() {
					latchContainer.manuallyCreatedFactorySinkLatch.countDown();
					return super.createMessageSink();
				}

				@Override
				public AckHandler<String> createAckHandler() {
					latchContainer.manuallyCreatedFactoryAckHandlerLatch.countDown();
					return super.createAckHandler();
				}
			});
			factory.setSqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient());
			factory.setMessageListener(msg -> latchContainer.manuallyCreatedFactoryLatch.countDown());
			return factory.createContainer(MANUALLY_CREATE_FACTORY_QUEUE_NAME);
		}

		LatchContainer latchContainer = new LatchContainer();

		@Bean
		ReceivesMessageListener receivesMessageListener() {
			return new ReceivesMessageListener();
		}

		@Bean
		DoesNotAckOnErrorListener doesNotAckOnErrorListener() {
			return new DoesNotAckOnErrorListener();
		}

		@Bean
		ResolvesParameterTypesListener resolvesParameterTypesListener() {
			return new ResolvesParameterTypesListener();
		}

		@Bean
		ResolvesPojoListener resolvesPojoListener() {
			return new ResolvesPojoListener();
		}

		@Bean
		ReceiveManyFromTwoQueuesListener receiveManyFromTwoQueuesListener() {
			return new ReceiveManyFromTwoQueuesListener();
		}

		@Bean
		ReceiveBatchesFromTwoQueuesListener receiveBatchesFromTwoQueuesListener() {
			return new ReceiveBatchesFromTwoQueuesListener();
		}

		@Bean
		SqsListenerCustomizer customizer() {
			return registrar -> registrar.setMessageHandlerMethodFactory(new DefaultMessageHandlerMethodFactory() {
				@Override
				public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
					latchContainer.invocableHandlerMethodLatch.countDown();
					return super.createInvocableHandlerMethod(bean, method);
				}
			});
		}

		@Bean
		LatchContainer latchContainer() {
			return this.latchContainer;
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean(name = TEST_SQS_ASYNC_CLIENT_BEAN_NAME)
		SqsAsyncClient sqsAsyncClientProducer() {
			return BaseSqsIntegrationTest.createHighThroughputAsyncClient();
		}

		private MessageInterceptor<String> testInterceptor() {
			return msg -> {
				latchContainer.interceptorLatch.countDown();
				return msg;
			};
		}

		private ErrorHandler<String> testErrorHandler() {
			return (msg, t) -> {
				logger.error("Error processing msg {}", msg, t);
				latchContainer.errorHandlerLatch.countDown();
				// Eventually ack to not interfere with other tests.
				if (latchContainer.errorHandlerLatch.getCount() == 0) {
					Objects.requireNonNull(
							(Acknowledgement) msg.getHeaders().get(SqsMessageHeaders.ACKNOWLEDGMENT_HEADER),
							"No acknowledgement present").acknowledge();
				}
				throw new RuntimeException("Propagating error from test error handler", t);
			};
		}

		private StandardSqsComponentFactory<String> getTestAckHandlerComponentFactory() {
			return new StandardSqsComponentFactory<String>() {
				@Override
				public AckHandler<String> createAckHandler() {
					return testAckHandler();
				}
			};
		}

		private AckHandler<String> testAckHandler() {
			return new OnSuccessAckHandler<String>() {
				@Override
				public CompletableFuture<Void> onSuccess(Message<String> message) {
					return super.onSuccess(message).thenRun(() -> latchContainer.messageAckLatch.countDown());
				}

				@Override
				public CompletableFuture<Void> onSuccess(Collection<Message<String>> messages) {
					return super.onSuccess(messages).exceptionally(t -> {
						logger.error("Error acknowledging test batch", t);
						return null;
					}).thenRun(() -> messages.forEach(msg -> latchContainer.batchesAckLatch.countDown()));
				}
			};
		}
	}

	static class MyPojo {

		String firstField;
		String secondField;

		MyPojo(String firstField, String secondField) {
			this.firstField = firstField;
			this.secondField = secondField;
		}

		MyPojo() {
		}

		public String getFirstField() {
			return firstField;
		}

		public void setFirstField(String firstField) {
			this.firstField = firstField;
		}

		public String getSecondField() {
			return secondField;
		}

		public void setSecondField(String secondField) {
			this.secondField = secondField;
		}
	}

}
