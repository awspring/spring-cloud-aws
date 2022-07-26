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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.MessageDeliveryStrategy;
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import io.awspring.cloud.sqs.listener.SqsMessageHeaders;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@Execution(CONCURRENT)
@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = { "spring.cloud.aws.credentials.access-key=noop",
		"spring.cloud.aws.credentials.secret-key=noop", "spring.cloud.aws.region.static=us-east-2" })
class SqsFifoIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsFifoIntegrationTests.class);

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

	@Autowired(required = false)
	ReceivesMessageInOrderListener receivesMessageInOrderListener;

	@Autowired(required = false)
	ReceivesMessageInOrderManyGroupsListener receivesMessageInOrderManyGroupsListener;

	@Autowired(required = false)
	StopsOnErrorListener stopsOnErrorListener;

	@Autowired(required = false)
	ReceiveBatchesFromManyGroupsListener receiveBatchesFromManyGroupsListener;

	@Test
	void receivesMessagesInOrder() throws Exception {
		int numberOfMessages = 10;
		latchContainer.receivesMessageLatch = new CountDownLatch(numberOfMessages);
		String messageGroupId = UUID.randomUUID().toString();
		List<String> values = IntStream.range(0, numberOfMessages).mapToObj(String::valueOf).collect(toList());
		String queueUrl = fetchQueueUrl(FIFO_RECEIVES_MESSAGE_IN_ORDER_QUEUE_NAME);
		sendMessageTo(queueUrl, values, messageGroupId);
		assertThat(latchContainer.receivesMessageLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(receivesMessageInOrderListener.receivedMessages).containsExactlyElementsOf(values);
	}

	@Test
	void receivesMessagesInOrderFromManyMessageGroups() throws Exception {
		int numberOfMessages = 10;
		latchContainer.receivesMessageManyGroupsLatch = new CountDownLatch(numberOfMessages * 3);
		List<String> values = IntStream.range(0, numberOfMessages).mapToObj(String::valueOf).collect(toList());
		String messageGroupId1 = UUID.randomUUID().toString();
		String messageGroupId2 = UUID.randomUUID().toString();
		String messageGroupId3 = UUID.randomUUID().toString();
		String queueUrl = fetchQueueUrl(FIFO_RECEIVES_MESSAGE_IN_ORDER_MANY_GROUPS_QUEUE_NAME);
		sendMessageTo(queueUrl, values, messageGroupId1);
		sendMessageTo(queueUrl, values, messageGroupId2);
		sendMessageTo(queueUrl, values, messageGroupId3);
		assertThat(latchContainer.receivesMessageManyGroupsLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(receivesMessageInOrderManyGroupsListener.receivedMessages.get(messageGroupId1)).containsExactlyElementsOf(values);
		assertThat(receivesMessageInOrderManyGroupsListener.receivedMessages.get(messageGroupId2)).containsExactlyElementsOf(values);
		assertThat(receivesMessageInOrderManyGroupsListener.receivedMessages.get(messageGroupId3)).containsExactlyElementsOf(values);
	}

	//@Test
	void stopsProcessingAfterException() throws Exception {
		int numberOfMessages = 10;
		latchContainer.stopsProcessingOnErrorLatch1 = new CountDownLatch(4);
		latchContainer.stopsProcessingOnErrorLatch1 = new CountDownLatch(11);
		List<String> values = IntStream.range(0, numberOfMessages).mapToObj(String::valueOf).collect(toList());
		String messageGroupId = UUID.randomUUID().toString();
		String queueUrl = fetchQueueUrl(FIFO_STOPS_PROCESSING_ON_ERROR_QUEUE_NAME);
		sendMessageTo(queueUrl, values, messageGroupId);
		assertThat(latchContainer.stopsProcessingOnErrorLatch1.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(stopsOnErrorListener.receivedMessagesBeforeException).containsExactlyElementsOf(values.stream().limit(4).collect(toList()));
		assertThat(latchContainer.stopsProcessingOnErrorLatch2.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(stopsOnErrorListener.receivedMessagesBeforeException).containsExactlyElementsOf(values.stream().limit(4).collect(toList()));
		assertThat(stopsOnErrorListener.receivedMessagesAfterException).containsExactlyElementsOf(values.subList(3, numberOfMessages));
	}

	//@Test
	void receivesBatchesManyGroups() throws Exception {
		int numberOfMessages = 10;
		latchContainer.receivesBatchManyGroupsLatch = new CountDownLatch(numberOfMessages * 3);
		List<String> values = IntStream.range(0, numberOfMessages).mapToObj(String::valueOf).collect(toList());
		String messageGroupId1 = UUID.randomUUID().toString();
		String messageGroupId2 = UUID.randomUUID().toString();
		String messageGroupId3 = UUID.randomUUID().toString();
		String queueUrl = fetchQueueUrl(FIFO_RECEIVES_BATCHES_MANY_GROUPS_QUEUE_NAME);
		sendMessageTo(queueUrl, values, messageGroupId1);
		sendMessageTo(queueUrl, values, messageGroupId2);
		sendMessageTo(queueUrl, values, messageGroupId3);
		assertThat(latchContainer.receivesBatchManyGroupsLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(receiveBatchesFromManyGroupsListener.receivedMessages.get(messageGroupId1)).containsExactlyElementsOf(values);
		assertThat(receiveBatchesFromManyGroupsListener.receivedMessages.get(messageGroupId2)).containsExactlyElementsOf(values);
		assertThat(receiveBatchesFromManyGroupsListener.receivedMessages.get(messageGroupId3)).containsExactlyElementsOf(values);
	}

	@Autowired
	MessagesContainer messagesContainer;

	@Test
	void manuallyCreatesContainer() throws Exception {
		String queueUrl = fetchQueueUrl(FIFO_MANUALLY_CREATE_CONTAINER_QUEUE_NAME);
		int numberOfMessages = 10;
		List<String> values = IntStream.range(0, numberOfMessages).mapToObj(String::valueOf).collect(toList());
		sendMessageTo(queueUrl, values, UUID.randomUUID().toString());
		assertThat(latchContainer.manuallyCreatedContainerLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(messagesContainer.manuallyCreatedContainerMessages).containsExactlyElementsOf(values);
	}

	@Test
	void manuallyCreatesBatchContainer() throws Exception {
		String queueUrl = fetchQueueUrl(FIFO_MANUALLY_CREATE_BATCH_CONTAINER_QUEUE_NAME);
		int numberOfMessages = 10;
		List<String> values = IntStream.range(0, numberOfMessages).mapToObj(String::valueOf).collect(toList());
		sendMessageTo(queueUrl, values, UUID.randomUUID().toString());
		assertThat(latchContainer.manuallyCreatedBatchContainerLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(messagesContainer.manuallyCreatedBatchContainerMessages).containsExactlyElementsOf(values);
	}

	@Test
	void manuallyCreatesFactory() throws Exception {
		String queueUrl = fetchQueueUrl(FIFO_MANUALLY_CREATE_FACTORY_QUEUE_NAME);
		int numberOfMessages = 10;
		List<String> values = IntStream.range(0, numberOfMessages).mapToObj(String::valueOf).collect(toList());
		sendMessageTo(queueUrl, values, UUID.randomUUID().toString());
		assertThat(latchContainer.manuallyCreatedFactoryLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(messagesContainer.manuallyCreatedFactoryMessages).containsExactlyElementsOf(values);
	}

	@Test
	void manuallyCreatesBatchFactory() throws Exception {
		String queueUrl = fetchQueueUrl(FIFO_MANUALLY_CREATE_BATCH_FACTORY_QUEUE_NAME);
		int numberOfMessages = 10;
		List<String> values = IntStream.range(0, numberOfMessages).mapToObj(String::valueOf).collect(toList());
		sendMessageTo(queueUrl, values, UUID.randomUUID().toString());
		assertThat(latchContainer.manuallyCreatedBatchFactoryLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(messagesContainer.manuallyCreatedBatchFactoryMessages).containsExactlyElementsOf(values);
	}

	static class ReceivesMessageInOrderListener {

		List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = FIFO_RECEIVES_MESSAGE_IN_ORDER_QUEUE_NAME, messageVisibilitySeconds = "5")
		void listen(String message) throws Exception {
			logger.debug("Received message in listener method: " + message);
//			int sleep = new Random().nextInt(1000);
//			logger.debug("Sleeping for {}ms for message {}", sleep, message);
//			Thread.sleep(sleep);
			receivedMessages.add(message);
			latchContainer.receivesMessageLatch.countDown();
		}
	}

	static class ReceivesMessageInOrderManyGroupsListener {

		Map<String, List<String>> receivedMessages = new ConcurrentHashMap<>();

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = FIFO_RECEIVES_MESSAGE_IN_ORDER_MANY_GROUPS_QUEUE_NAME)
		void listen(String message, @Header(SqsMessageHeaders.SQS_GROUP_ID_HEADER) String groupId) {
			logger.debug("Received message in listener method: " + message);
//			int sleep = new Random().nextInt(1000);
//			logger.debug("Sleeping for {}ms for message {}", sleep, message);
//			Thread.sleep(sleep);
			receivedMessages.computeIfAbsent(groupId, newGroupId -> new ArrayList<>()).add(message);
			latchContainer.receivesMessageManyGroupsLatch.countDown();
			logger.debug("Message {} processed.", message);
		}
	}

	static class StopsOnErrorListener {

		List<String> receivedMessagesBeforeException = Collections.synchronizedList(new ArrayList<>());

		List<String> receivedMessagesAfterException = Collections.synchronizedList(new ArrayList<>());

		AtomicBoolean hasThrown = new AtomicBoolean(false);

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = FIFO_STOPS_PROCESSING_ON_ERROR_QUEUE_NAME, messageVisibilitySeconds = "2")
		void listen(String message) throws Exception {
			logger.debug("Received message in listener method: " + message);
//			int sleep = new Random().nextInt(1000);
//			logger.debug("Sleeping for {}ms for message {}", sleep, message);
//			Thread.sleep(sleep);
			if (!hasThrown.get()) {
				this.receivedMessagesBeforeException.add(message);
			}
			else {
				this.receivedMessagesAfterException.add(message);
			}
			latchContainer.stopsProcessingOnErrorLatch1.countDown();
			latchContainer.stopsProcessingOnErrorLatch2.countDown();
			if (!hasThrown.get() && "3".equals(message)) {
				this.hasThrown.compareAndSet(false, true);
				throw new RuntimeException("Expected exception");
			}
		}
	}

	static class ReceiveBatchesFromManyGroupsListener {

		Map<String, List<String>> receivedMessages = new ConcurrentHashMap<>();

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = FIFO_RECEIVES_BATCHES_MANY_GROUPS_QUEUE_NAME, messageVisibilitySeconds = "20")
		void listen(Collection<Message<String>> messages) {
			String firstMessage = messages.iterator().next().getPayload();// Make sure we got the right type
			Assert.isTrue(MessageHeaderUtils.getHeader(messages, SqsMessageHeaders.SQS_GROUP_ID_HEADER, String.class)
					.stream().distinct().count() == 1,
				"More than one message group returned in the same batch");
			String messageGroupId = MessageHeaderUtils.getHeader(messages.iterator().next(), SqsMessageHeaders.SQS_GROUP_ID_HEADER, String.class);
			logger.debug("Started processing {} messages for group id {}", messages.size(), messageGroupId);
			receivedMessages.put(messageGroupId, messages.stream().map(Message::getPayload).collect(toList()));
			messages.forEach(msg -> latchContainer.receivesBatchManyGroupsLatch.countDown());
			logger.debug("Finished processing {} messages for group id {}", messages.size(), messageGroupId);
		}
	}

	private void sendMessageTo(String queueUrl, Collection<String> messageBodies, String messageGroupId) {
		try {
			sqsAsyncClient.sendMessageBatch(req -> req.entries(messageBodies.stream().map(body -> createEntry(body, messageGroupId)).collect(toList())).queueUrl(queueUrl)
				.build()).get();
			logger.debug("Sent message to queue {} with messageBody {}", queueUrl, messageBodies);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private SendMessageBatchRequestEntry createEntry(String body, String messageGroupId) {
		return SendMessageBatchRequestEntry.builder().messageBody(body).id(UUID.randomUUID().toString())
			.messageGroupId(messageGroupId).messageDeduplicationId(UUID.randomUUID().toString()).build();
	}

	private String fetchQueueUrl(String receivesMessageQueueName) throws InterruptedException, ExecutionException {
		return sqsAsyncClient.getQueueUrl(req -> req.queueName(receivesMessageQueueName)).get().queueUrl();
	}

	static class LatchContainer {

		final CountDownLatch manuallyCreatedContainerLatch = new CountDownLatch(10);
		final CountDownLatch manuallyCreatedFactoryLatch = new CountDownLatch(10);
		final CountDownLatch manuallyCreatedBatchContainerLatch = new CountDownLatch(10);
		final CountDownLatch manuallyCreatedBatchFactoryLatch = new CountDownLatch(10);

		// Lazily initialized
		CountDownLatch receivesMessageLatch = new CountDownLatch(1);
		CountDownLatch receivesMessageManyGroupsLatch = new CountDownLatch(1);
		CountDownLatch stopsProcessingOnErrorLatch1 = new CountDownLatch(1);
		CountDownLatch stopsProcessingOnErrorLatch2 = new CountDownLatch(1);
		CountDownLatch receivesBatchManyGroupsLatch = new CountDownLatch(1);

	}

	static class MessagesContainer {

		List<String> manuallyCreatedContainerMessages = Collections.synchronizedList(new ArrayList<>());
		List<String> manuallyCreatedBatchContainerMessages = Collections.synchronizedList(new ArrayList<>());
		List<String> manuallyCreatedFactoryMessages = Collections.synchronizedList(new ArrayList<>());
		List<String> manuallyCreatedBatchFactoryMessages = Collections.synchronizedList(new ArrayList<>());

	}

	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class SQSConfiguration {

		MessagesContainer messagesContainer = new MessagesContainer();

		@Bean
		public MessagesContainer messagesContainer() {
			return this.messagesContainer;
		}

		@Bean
		public SqsMessageListenerContainerFactory<String> defaultSqsListenerContainerFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.getContainerOptions().permitAcquireTimeout(Duration.ofSeconds(1)).maxInflightMessagesPerQueue(30)
					.pollTimeout(Duration.ofSeconds(1));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			return factory;
		}

		@Bean
		public SqsMessageListenerContainerFactory<String> lowResourceFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.getContainerOptions().maxInflightMessagesPerQueue(10).pollTimeout(Duration.ofSeconds(1))
					.messagesPerPoll(1).permitAcquireTimeout(Duration.ofSeconds(1));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			return factory;
		}

		@Bean
		public MessageListenerContainer<String> manuallyCreatedContainer() {
			SqsMessageListenerContainer<String> container = new SqsMessageListenerContainer<>(createAsyncClient(),
					ContainerOptions.create().permitAcquireTimeout(Duration.ofSeconds(1))
							.pollTimeout(Duration.ofSeconds(1)));
			container.setQueueNames(FIFO_MANUALLY_CREATE_CONTAINER_QUEUE_NAME);
			container.setMessageListener(msg -> {
				messagesContainer.manuallyCreatedContainerMessages.add(msg.getPayload());
				latchContainer.manuallyCreatedContainerLatch.countDown();
			});
			return container;
		}

		@Bean
		public MessageListenerContainer<String> manuallyCreatedBatchContainer() {
			SqsMessageListenerContainer<String> container = new SqsMessageListenerContainer<>(createAsyncClient(),
					ContainerOptions.create().permitAcquireTimeout(Duration.ofSeconds(1))
							.pollTimeout(Duration.ofSeconds(1)).messageDeliveryStrategy(MessageDeliveryStrategy.BATCH));
			container.setQueueNames(FIFO_MANUALLY_CREATE_BATCH_CONTAINER_QUEUE_NAME);
			container.setMessageListener(new MessageListener<String>() {
				@Override
				public void onMessage(Message<String> message) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void onMessage(Collection<Message<String>> messages) {
					messagesContainer.manuallyCreatedBatchContainerMessages.addAll(messages.stream().map(Message::getPayload).collect(toList()));
					messages.forEach(msg -> latchContainer.manuallyCreatedBatchContainerLatch.countDown());
				}
			});
			return container;
		}

		@Bean
		public SqsMessageListenerContainer<String> manuallyCreatedFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.getContainerOptions().maxInflightMessagesPerQueue(10).pollTimeout(Duration.ofSeconds(2))
					.messagesPerPoll(10).permitAcquireTimeout(Duration.ofSeconds(1))
					.pollTimeout(Duration.ofSeconds(1));
			factory.setSqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient());
			Random random = new Random();
			factory.setMessageListener(msg -> {
//				try {
//					Thread.sleep(random.nextInt(1000));
//				} catch (InterruptedException e) {
//					Thread.currentThread().interrupt();
//					// ignore
//				}
				logger.debug("Processed message {}", msg.getPayload());
				messagesContainer.manuallyCreatedFactoryMessages.add(msg.getPayload());
				latchContainer.manuallyCreatedFactoryLatch.countDown();
			});
			return factory.createContainer(FIFO_MANUALLY_CREATE_FACTORY_QUEUE_NAME);
		}

		@Bean
		public SqsMessageListenerContainer<String> manuallyCreatedBatchFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.getContainerOptions().maxInflightMessagesPerQueue(10).pollTimeout(Duration.ofSeconds(2))
					.messagesPerPoll(10).permitAcquireTimeout(Duration.ofSeconds(1))
					.pollTimeout(Duration.ofSeconds(1)).messageDeliveryStrategy(MessageDeliveryStrategy.BATCH);
			factory.setSqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient());
			factory.setMessageListener(new MessageListener<String>() {
				@Override
				public void onMessage(Message<String> message) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void onMessage(Collection<Message<String>> messages) {
					messagesContainer.manuallyCreatedBatchFactoryMessages.addAll(messages.stream().map(Message::getPayload).collect(toList()));
					messages.forEach(msg -> latchContainer.manuallyCreatedBatchFactoryLatch.countDown());
				}
			});
			return factory.createContainer(FIFO_MANUALLY_CREATE_BATCH_FACTORY_QUEUE_NAME);
		}

		@Bean
		ReceivesMessageInOrderListener receivesMessageInOrderListener() {
			return new ReceivesMessageInOrderListener();
		}

		@Bean
		ReceivesMessageInOrderManyGroupsListener receivesMessageInOrderManyGroupsListener() {
			return new ReceivesMessageInOrderManyGroupsListener();
		}

		@Bean
		StopsOnErrorListener stopsOnErrorListener() {
			return new StopsOnErrorListener();
		}

		@Bean
		ReceiveBatchesFromManyGroupsListener receiveBatchesFromManyGroupsListener() {
			return new ReceiveBatchesFromManyGroupsListener();
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

		@Bean(name = TEST_SQS_ASYNC_CLIENT_BEAN_NAME)
		SqsAsyncClient sqsAsyncClientProducer() {
			return BaseSqsIntegrationTest.createAsyncClient();
		}

	}

}
