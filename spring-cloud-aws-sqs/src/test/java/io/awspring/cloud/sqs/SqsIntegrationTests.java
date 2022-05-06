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

import static io.awspring.cloud.sqs.config.SqsFactoryOptions.withOptions;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.support.config.MessageListenerContainerFactory;
import io.awspring.cloud.messaging.support.listener.AsyncErrorHandler;
import io.awspring.cloud.messaging.support.listener.AsyncMessageInterceptor;
import io.awspring.cloud.messaging.support.listener.AsyncMessageListener;
import io.awspring.cloud.messaging.support.listener.MessageHeaders;
import io.awspring.cloud.messaging.support.listener.MessageListenerContainerRegistry;
import io.awspring.cloud.messaging.support.listener.acknowledgement.AsyncAckHandler;
import io.awspring.cloud.messaging.support.listener.acknowledgement.AsyncAcknowledgement;
import io.awspring.cloud.sqs.annotation.EnableSqs;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsConfigUtils;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.AsyncMessageHandlerMessageListener;
import io.awspring.cloud.sqs.listener.SqsMessageHeaders;
import io.awspring.cloud.sqs.listener.Visibility;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = { "cloud.aws.credentials.access-key=noop", "cloud.aws.credentials.secret-key=noop",
		"cloud.aws.region.static=us-east-2" })
class SqsIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsIntegrationTests.class);

	private static final String TEST_SQS_ASYNC_CLIENT_BEAN_NAME = "testSqsAsyncClient";

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
		assertThat(latchContainer.interceptorLatch.await(10, TimeUnit.SECONDS)).isTrue();
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
	}

	@Test
	void resolvesPojoParameterTypes() throws Exception {
		sendMessageTo(RESOLVES_POJO_TYPES_QUEUE_NAME,
				objectMapper.writeValueAsString(new MyPojo("firstValue", "secondValue")));
		assertThat(latchContainer.resolvesPojoLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	final int outerBatchSize = 1; //20;
	final int innerBatchSize = 1; //10;
	final int totalMessages = 2 * outerBatchSize * innerBatchSize;

	// These tests are really only for us to have some indication on how the system performs under load.
	// We can probably remove them later, or adapt to only make sure it handles more than one queue.

	@Test
	void receivesManyFromTwoQueuesWithLoad() throws Exception {
		latchContainer.manyMessagesTwoQueuesLatch = new CountDownLatch(totalMessages);
		latchContainer.messageAckLatch = new CountDownLatch(totalMessages);
		testWithLoad(RECEIVE_FROM_MANY_1_QUEUE_NAME, RECEIVE_FROM_MANY_2_QUEUE_NAME,
				latchContainer.manyMessagesTwoQueuesLatch, latchContainer.messageAckLatch);
		assertThat(latchContainer.messageAckLatch.await(60, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void asyncReceivesManyFromTwoQueuesWithLoad() throws Exception {
		latchContainer.asyncManyMessagesTwoQueuesLatch = new CountDownLatch(totalMessages);
		latchContainer.messageAckLatchAsync = new CountDownLatch(totalMessages);
		testWithLoad(ASYNC_RECEIVE_FROM_MANY_1_QUEUE_NAME, ASYNC_RECEIVE_FROM_MANY_2_QUEUE_NAME,
				latchContainer.asyncManyMessagesTwoQueuesLatch, latchContainer.messageAckLatchAsync);
		assertThat(latchContainer.messageAckLatchAsync.await(60, TimeUnit.SECONDS)).isTrue();
	}

	private void testWithLoad(String queue1, String queue2, CountDownLatch countDownLatch, CountDownLatch secondLatch)
			throws InterruptedException, ExecutionException {
		StopWatch watch = new StopWatch();
		watch.start();
		String queueUrl1 = fetchQueueUrl(queue1);
		String queueUrl2 = fetchQueueUrl(queue2);
		IntStream.range(0, outerBatchSize).forEach(index -> sendMessageBatch(queueUrl1, index, innerBatchSize));
		IntStream.range(outerBatchSize, outerBatchSize * 2)
				.forEach(index -> sendMessageBatch(queueUrl2, index, innerBatchSize));
		assertThat(countDownLatch.await(60, TimeUnit.SECONDS)).isTrue();
		assertThat(secondLatch.await(60, TimeUnit.SECONDS)).isTrue();
		watch.stop();
		double totalTimeSeconds = watch.getTotalTimeSeconds();
		logger.info("{} seconds for sending and consuming {} messages. Messages / second: {}", totalTimeSeconds,
				totalMessages, totalMessages / totalTimeSeconds);
	}

	private void sendMessageBatch(String queueUrl, int parentIndex, int batchSize) {
		try {
			sqsAsyncClient
					.sendMessageBatch(
							req -> req.entries(getBatchEntries(batchSize, parentIndex)).queueUrl(queueUrl).build())
					.get();
			if (parentIndex % 5 == 0) {
				logger.debug("Sent " + parentIndex * batchSize + " messages.");
			}
		}
		catch (Exception e) {
			logger.error("Error sending messages: ", e);
			throw new RuntimeException(e);
		}
	}

	private SendMessageBatchRequestEntry[] getBatchEntries(int batchSize, int parentIndex) {
		return IntStream.range(0, batchSize)
				.mapToObj(index -> SendMessageBatchRequestEntry.builder().id(UUID.randomUUID().toString())
						.messageBody(TEST_PAYLOAD + " - " + ((parentIndex * 10) + index)).build())
				.toArray(SendMessageBatchRequestEntry[]::new);
	}

	private void sendMessageTo(String receivesMessageQueueName) throws InterruptedException, ExecutionException {
		String queueUrl = fetchQueueUrl(receivesMessageQueueName);
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

		@SqsListener(queueNames = RECEIVES_MESSAGE_QUEUE_NAME)
		void listen(String message) {
			logger.debug("Received message in Listener Method: " + message);
			latchContainer.receivesMessageLatch.countDown();
		}
	}

	static class DoesNotAckOnErrorListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = DOES_NOT_ACK_ON_ERROR_QUEUE_NAME, factory = "lowResourceFactory")
		void listen(String message) {
			logger.debug("Received message in Listener Method: " + message);
			latchContainer.doesNotAckLatch.countDown();
			throw new RuntimeException("Expected exception");
		}
	}

	static class ResolvesParameterTypesListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = RESOLVES_PARAMETER_TYPES_QUEUE_NAME, minSecondsToProcess = "20")
		void listen(String message, SqsMessageHeaders headers, AsyncAcknowledgement ack, Visibility visibility,
				software.amazon.awssdk.services.sqs.model.Message originalMessage) {
			Assert.notNull(headers, "Received null SqsMessageHeaders");
			Assert.notNull(ack, "Received null AsyncAcknowledgement");
			Assert.notNull(visibility, "Received null Visibility");
			Assert.notNull(originalMessage, "Received null software.amazon.awssdk.services.sqs.model.Message");
			Assert.notNull(message, "Received null message");
			logger.debug("Received message in Listener Method: " + message);
			latchContainer.manyParameterTypesLatch.countDown();
		}
	}

	static class ResolvesPojoListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = RESOLVES_POJO_TYPES_QUEUE_NAME, concurrentPollsPerContainer = "1")
		void listen(MyPojo pojo) {
			Assert.notNull(pojo, "Received null message");
			logger.debug("Received message in Listener Method: " + pojo);
			latchContainer.resolvesPojoLatch.countDown();
		}
	}

	static class ReceiveManyFromTwoQueuesListener {

		@Autowired
		LatchContainer latchContainer;

		AtomicInteger messagesReceived = new AtomicInteger();

		@SqsListener(queueNames = { RECEIVE_FROM_MANY_1_QUEUE_NAME,
				RECEIVE_FROM_MANY_2_QUEUE_NAME }, factory = "highThroughputFactory")
		void listen(String message) throws Exception {
			logger.debug("Started processing " + message);
			Thread.sleep(1000);
			int count;
			if ((count = messagesReceived.incrementAndGet()) % 50 == 0) {
				logger.debug("Listener processed {} messages", count);
			}
			latchContainer.manyMessagesTwoQueuesLatch.countDown();
			logger.debug("Finished processing " + message);
		}
	}

	static class AsyncReceiveManyFromTwoQueuesListener {

		@Autowired
		LatchContainer latchContainer;

		AtomicInteger messagesReceived = new AtomicInteger();

		ThreadPoolTaskExecutor taskExecutor = getTaskExecutor();

		private ThreadPoolTaskExecutor getTaskExecutor() {
			ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
			taskExecutor.initialize();
			taskExecutor.setCorePoolSize(25);
			taskExecutor.setMaxPoolSize(25);
			return taskExecutor;
		}

		@SqsListener(queueNames = { ASYNC_RECEIVE_FROM_MANY_1_QUEUE_NAME,
				ASYNC_RECEIVE_FROM_MANY_2_QUEUE_NAME }, factory = "lowResourceFactory")
		CompletableFuture<Void> listenAsync(String message) {
			logger.debug("Received message {}", message);
			return CompletableFuture.supplyAsync(() -> processMessage(message), this.taskExecutor);
		}

		@Nullable
		private Void processMessage(String message) {
			try {
				logger.debug("Started processing " + message);
				Thread.sleep(1000);
				logEvery50messages();
				latchContainer.asyncManyMessagesTwoQueuesLatch.countDown();
				logger.debug("Finished processing " + message);
				return null;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.error("Interrupted while sleeping", e);
				throw new RuntimeException(e);
			}
			catch (Exception e) {
				logger.error("Error in listener", e);
				throw new RuntimeException(e);
			}
		}

		private void logEvery50messages() {
			int count;
			if ((count = messagesReceived.incrementAndGet()) % 50 == 0) {
				logger.debug("Listener processed {} messages", count);
			}
		}
	}

	static class LatchContainer {

		final CountDownLatch receivesMessageLatch = new CountDownLatch(1);
		final CountDownLatch doesNotAckLatch = new CountDownLatch(2);
		final CountDownLatch errorHandlerLatch = new CountDownLatch(2);
		final CountDownLatch interceptorLatch = new CountDownLatch(1);
		final CountDownLatch manyParameterTypesLatch = new CountDownLatch(1);
		final CountDownLatch resolvesPojoLatch = new CountDownLatch(1);
		// Lazily initialized
		CountDownLatch manyMessagesTwoQueuesLatch = new CountDownLatch(1);
		CountDownLatch asyncManyMessagesTwoQueuesLatch = new CountDownLatch(1);
		CountDownLatch messageAckLatch = new CountDownLatch(1);
		CountDownLatch messageAckLatchAsync = new CountDownLatch(1);

	}

	@EnableSqs
	@Configuration
	static class SQSConfiguration {

		// TODO: Probably move some of this to auto configuration with @ConditionalOnMissingBean
		@Bean(name = SqsConfigUtils.SQS_ASYNC_CLIENT_BEAN_NAME)
		SqsAsyncClient sqsAsyncClientConsumer() throws Exception {
			SqsAsyncClient asyncClient = SqsAsyncClient.builder().endpointOverride(localstack.getEndpointOverride(SQS))
					.build();
			createQueues(asyncClient);
			return asyncClient;
		}

		@Bean
		public MessageListenerContainerFactory<?, ?> defaultListenerContainerFactory() {
			return new SqsMessageListenerContainerFactory();
		}

		@Bean
		public MessageListenerContainerFactory<?, ?> highThroughputFactory() {
			return new SqsMessageListenerContainerFactory(
					withOptions().concurrentWorkersPerContainer(25).concurrentPollsPerContainer(10).messagesPerPoll(10)
							.pollingTimeoutSeconds(1).errorHandler(testErrorHandler()).ackHandler(testAckHandler()));
		}

		@Bean
		public MessageListenerContainerFactory<?, ?> lowResourceFactory() {
			return new SqsMessageListenerContainerFactory(withOptions().concurrentWorkersPerContainer(3)
					.concurrentPollsPerContainer(10).interceptor(testInterceptor()).messagesPerPoll(10)
					.ackHandler(testAckHandler()).errorHandler(testErrorHandler()).pollingTimeoutSeconds(3));
		}

		@Bean(name = SqsConfigUtils.SQS_ASYNC_LISTENER_BEAN_NAME)
		AsyncMessageListener<String> asyncMessageListener(MessageHandler messageHandler) {
			return new AsyncMessageHandlerMessageListener<>(messageHandler);
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
		AsyncReceiveManyFromTwoQueuesListener asyncReceiveManyFromTwoQueuesListener() {
			return new AsyncReceiveManyFromTwoQueuesListener();
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
			return SqsAsyncClient.builder().endpointOverride(localstack.getEndpointOverride(SQS)).build();
		}

		private void createQueues(SqsAsyncClient client) throws InterruptedException, ExecutionException {
			CompletableFuture.allOf(
					client.createQueue(req -> req.queueName(RECEIVES_MESSAGE_QUEUE_NAME)
							.attributes(singletonMap(QueueAttributeName.VISIBILITY_TIMEOUT, "1")).build()),
					client.createQueue(req -> req.queueName(DOES_NOT_ACK_ON_ERROR_QUEUE_NAME)
							.attributes(singletonMap(QueueAttributeName.VISIBILITY_TIMEOUT, "1")).build()),
					client.createQueue(req -> req.queueName(RECEIVE_FROM_MANY_1_QUEUE_NAME)
							.attributes(singletonMap(QueueAttributeName.VISIBILITY_TIMEOUT, "1")).build()),
					client.createQueue(req -> req.queueName(RECEIVE_FROM_MANY_2_QUEUE_NAME)
							.attributes(singletonMap(QueueAttributeName.VISIBILITY_TIMEOUT, "1")).build()),
					client.createQueue(req -> req.queueName(RESOLVES_PARAMETER_TYPES_QUEUE_NAME).build()),
					client.createQueue(req -> req.queueName(RESOLVES_POJO_TYPES_QUEUE_NAME).build()),
					client.createQueue(req -> req.queueName(ASYNC_RECEIVE_FROM_MANY_1_QUEUE_NAME).build()),
					client.createQueue(req -> req.queueName(ASYNC_RECEIVE_FROM_MANY_2_QUEUE_NAME).build())).get();
		}

		private AsyncMessageInterceptor<String> testInterceptor() {
			return msg -> {
				latchContainer.interceptorLatch.countDown();
				return CompletableFuture.completedFuture(msg);
			};
		}

		private AsyncErrorHandler<String> testErrorHandler() {
			return (msg, t) -> {
				logger.error("Error processing msg {}", msg, t);
				latchContainer.errorHandlerLatch.countDown();
				// Eventually ack to not interfere with other tests.
				if (latchContainer.errorHandlerLatch.getCount() == 0) {
					Objects.requireNonNull(
							(AsyncAcknowledgement) msg.getHeaders().get(MessageHeaders.ACKNOWLEDGMENT_HEADER),
							"No acknowledgement present").acknowledge();
				}
				return CompletableFuture.completedFuture(null);
			};
		}

		private AsyncAckHandler<String> testAckHandler() {
			return msg -> {
				latchContainer.messageAckLatch.countDown();
				latchContainer.messageAckLatchAsync.countDown();
				return Objects
						.requireNonNull(
								(AsyncAcknowledgement) msg.getHeaders().get(MessageHeaders.ACKNOWLEDGMENT_HEADER))
						.acknowledge();
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
