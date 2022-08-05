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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.StandardSqsComponentFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.BatchingAcknowledgementProcessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.util.StopWatch;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@SpringBootTest
class SqsLoadIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsLoadIntegrationTests.class);

	private static final String RECEIVE_FROM_MANY_1_QUEUE_NAME = "receive_many_test_queue_1";

	private static final String RECEIVE_FROM_MANY_2_QUEUE_NAME = "receive_many_test_queue_2";

	private static final String RECEIVE_BATCH_1_QUEUE_NAME = "receive_batch_test_queue_1";

	private static final String RECEIVE_BATCH_2_QUEUE_NAME = "receive_batch_test_queue_2";

	private static final String TEST_SQS_ASYNC_CLIENT_BEAN_NAME = "testSqsAsyncClient";

	private static final String HIGH_THROUGHPUT_FACTORY_NAME = "highThroughputFactory";

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	@Qualifier(TEST_SQS_ASYNC_CLIENT_BEAN_NAME)
	SqsAsyncClient sqsAsyncClient;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	Settings settings;

	@Autowired
	LoadSimulator loadSimulator;

	@Autowired
	MessageContainer messageContainer;

	static class Settings implements SmartInitializingSingleton {

		@Autowired
		LoadSimulator loadSimulator;

		final int totalMessages = 20;
		final boolean sendMessages = true;
		final boolean receiveMessages = true;
		final boolean receivesManyTestEnabled = true;
		final boolean receivesBatchesTestEnabled = true;
		final int maxInflight = 10;
		final int latchAwaitSeconds = 10;

		@Override
		public void afterSingletonsInstantiated() {
			loadSimulator.setLoadEnabled(false);
			loadSimulator.setBound(1000);
			loadSimulator.setRandom(false);
		}
	}

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(
			createQueue(client, RECEIVE_FROM_MANY_1_QUEUE_NAME),
			createQueue(client, RECEIVE_FROM_MANY_2_QUEUE_NAME),
			createQueue(client, RECEIVE_BATCH_1_QUEUE_NAME, singletonMap(QueueAttributeName.VISIBILITY_TIMEOUT, "10")),
			createQueue(client, RECEIVE_BATCH_2_QUEUE_NAME, singletonMap(QueueAttributeName.VISIBILITY_TIMEOUT, "10"))
		).join();
	}

	@Test
	void receivesManyFromTwoQueuesWithLoad() throws Exception {
		latchContainer.manyMessagesTwoQueuesLatch = new CountDownLatch(settings.totalMessages);
		latchContainer.messageAckLatch = new CountDownLatch(settings.totalMessages);
		testWithLoad(RECEIVE_FROM_MANY_1_QUEUE_NAME, RECEIVE_FROM_MANY_2_QUEUE_NAME, messageContainer.receivedByListener);
	}

	@Test
	void receivesBatchesFromTwoQueuesWithLoad() throws Exception {
		latchContainer.batchesTwoQueuesLatch = new CountDownLatch(settings.totalMessages);
		latchContainer.batchesAckLatch = new CountDownLatch(settings.totalMessages);
		testWithLoad(RECEIVE_BATCH_1_QUEUE_NAME, RECEIVE_BATCH_2_QUEUE_NAME, messageContainer.receivedByBatchListener);
	}

	private void testWithLoad(String queue1, String queue2, Collection<String> receivedCollection)
		throws InterruptedException, ExecutionException {
		String queueUrl1 = fetchQueueUrl(queue1);
		String queueUrl2 = fetchQueueUrl(queue2);
		StopWatch watch = new StopWatch();
		watch.start();
		LoadSimulator sendLoadSimulator = new LoadSimulator();
		sendLoadSimulator.setLoadEnabled(false);
		IntStream.range(0, Math.max(settings.totalMessages / 20, 1)).forEach(index -> {
			sendMessageBatchAsync(queueUrl1);
			sendMessageBatchAsync(queueUrl2);
			if (index % 10 == 0) {
				sendLoadSimulator.runLoad(50);
			}
		});
		await()
			.atMost(Duration.ofSeconds(settings.latchAwaitSeconds))
			.pollDelay(Duration.ofMillis(100))
			.until(() -> receivedCollection.size() >= settings.totalMessages);
		logger.debug("Received all messages");
		await()
			.atMost(Duration.ofSeconds(settings.latchAwaitSeconds))
			.pollDelay(Duration.ofMillis(100))
			.until(() -> messageContainer.successfullyAcked.size() + messageContainer.errorAcking.size()
				>= ((settings.receivesManyTestEnabled && settings.receivesBatchesTestEnabled)
					? settings.totalMessages * 2 : settings.totalMessages));
		logger.debug("Acked all messages");
		logger.info("Messages received by listener: {}", receivedCollection.size());
		logger.info("messageContainer.successfullyAcked: {}", messageContainer.successfullyAcked.size());
		logger.info("messageContainer.errorAcking: {}", messageContainer.errorAcking.size());
		HashSet<String> acked = new HashSet<>(messageContainer.successfullyAcked);
		acked.addAll(messageContainer.errorAcking);
		assertThat(acked.containsAll(receivedCollection)).isTrue();
		if (!messageContainer.errorAcking.isEmpty()) {
			logger.warn("Some messages got an error acking: {}", messageContainer.errorAcking);
		}
		watch.stop();
		double totalTimeSeconds = watch.getTotalTimeSeconds();
		logger.info("{} seconds for {}consuming {} messages with {}. Messages / second: {}",
			totalTimeSeconds, this.settings.sendMessages ? "sending and " : "",
			settings.totalMessages, loadSimulator, settings.totalMessages / totalTimeSeconds);
	}

	AtomicInteger sentMessages = new AtomicInteger();

	AtomicInteger bodyInteger = new AtomicInteger();

	private void sendMessageBatchAsync(String queueUrl) {
		if (!settings.sendMessages) {
			return;
		}
		Collection<SendMessageBatchRequestEntry> batchEntries = getBatchEntries();
		doSendMessageBatch(queueUrl, batchEntries);
	}

	private void doSendMessageBatch(String queueUrl, Collection<SendMessageBatchRequestEntry> batchEntries) {
		sqsAsyncClient.sendMessageBatch(req -> req.entries(batchEntries)
						.queueUrl(queueUrl).build())
			.thenRun(this::logSend)
			.exceptionally(t -> {
				logger.error("Error sending messages - retrying", t);
				doSendMessageBatch(queueUrl, batchEntries);
				return null;
			});
	}

	private void logSend() {
		int sent = sentMessages.addAndGet(10);
		if (sent % 1000 == 0) {
			logger.debug("Sent {} messages", sent);
		}
	}

	private Collection<SendMessageBatchRequestEntry> getBatchEntries() {
		return IntStream.range(0, 10)
				.mapToObj(index ->SendMessageBatchRequestEntry.builder().id(UUID.randomUUID().toString())
						.messageBody(getBody()).build())
				.collect(Collectors.toList());
	}

	private String getBody() {
		try {
			return this.objectMapper
				.writeValueAsString(new MyPojo("MyPojo - " + bodyInteger.incrementAndGet(), "MyPojo - secondValue"));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private String fetchQueueUrl(String receivesMessageQueueName) throws InterruptedException, ExecutionException {
		return sqsAsyncClient.getQueueUrl(req -> req.queueName(receivesMessageQueueName)).get().queueUrl();
	}

	static class MessageContainer {

		Collection<String> receivedByListener = Collections.synchronizedSet(new HashSet<>());

		Collection<String> receivedByBatchListener = Collections.synchronizedSet(new HashSet<>());

		Collection<String> successfullyAcked = Collections.synchronizedSet(new HashSet<>());

		Collection<String> errorAcking = Collections.synchronizedSet(new HashSet<>());

	}

	static class ReceiveManyFromTwoQueuesListener {

		@Autowired
		MessageContainer messageContainer;

		@Autowired
		LatchContainer latchContainer;

		AtomicInteger messagesReceived = new AtomicInteger();

		@Autowired
		LoadSimulator loadSimulator;

		@SqsListener(queueNames = { RECEIVE_FROM_MANY_1_QUEUE_NAME, RECEIVE_FROM_MANY_2_QUEUE_NAME},
			factory = HIGH_THROUGHPUT_FACTORY_NAME, id = "many-from-two-queues")
		void listen(Message<String> message) {
			logger.trace("Started processing {}", MessageHeaderUtils.getId(message));
			if (this.messageContainer.receivedByListener.contains(MessageHeaderUtils.getId(message))) {
				logger.warn("Received duplicated message: {}", message);
			}
			loadSimulator.runLoad();
			this.messageContainer.receivedByListener.add(MessageHeaderUtils.getId(message));
			latchContainer.manyMessagesTwoQueuesLatch.countDown();
			int count;
			if ((count = messagesReceived.incrementAndGet()) % 500 == 0) {
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

		@Autowired
		LoadSimulator loadSimulator;

		@Autowired
		MessageContainer messageContainer;

		@SqsListener(queueNames = { RECEIVE_BATCH_1_QUEUE_NAME,
				RECEIVE_BATCH_2_QUEUE_NAME }, factory = HIGH_THROUGHPUT_FACTORY_NAME, id = "batch-from-two-queues")
		void listen(List<Message<MyPojo>> messages) {
			logger.trace("Started processing messages {}", MessageHeaderUtils.getId(messages));
			String firstField = messages.get(0).getPayload().firstField;// Make sure we got the right type
			loadSimulator.runLoad();
			messages.forEach(msg -> latchContainer.batchesTwoQueuesLatch.countDown());
			int messagesCount = this.messagesReceived.addAndGet(messages.size());
			this.messageContainer.receivedByBatchListener.addAll(messages.stream().map(MessageHeaderUtils::getId).collect(Collectors.toList()));
			int batches;
			if ((batches = batchesReceived.incrementAndGet()) % 5 == 0) {
				logger.debug("Listener processed {} batches and {} messages", batches, messagesCount);
			}
			logger.trace("Finished processing {} messages", messages.size());
		}
	}

	static class LatchContainer {

		CountDownLatch manyMessagesTwoQueuesLatch = new CountDownLatch(1);
		CountDownLatch messageAckLatch = new CountDownLatch(1);
		CountDownLatch batchesAckLatch = new CountDownLatch(1);
		CountDownLatch batchesTwoQueuesLatch = new CountDownLatch(1);

	}

	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class SQSConfiguration {

		@Bean
		public SqsMessageListenerContainerFactory<String> highThroughputFactory() {
			// For load tests, set maxInflightMessagesPerQueue to a higher value - e.g. 600
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.getContainerOptions()
				.maxInflightMessagesPerQueue(settings.maxInflight)
				.pollTimeout(Duration.ofSeconds(3))
				.messagesPerPoll(10)
				.permitAcquireTimeout(Duration.ofSeconds(1))
				.backPressureMode(BackPressureMode.HIGH_THROUGHPUT)
				.sourceShutdownTimeout(Duration.ofSeconds(40));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createHighThroughputAsyncClient);
			factory.setContainerComponentFactory(getTestAckHandlerComponentFactory());
			return factory;
		}

		LatchContainer latchContainer = new LatchContainer();

		Settings settings = new Settings();

		@Bean
		Settings settings() {
			return this.settings;
		}

		@Bean
		ReceiveManyFromTwoQueuesListener receiveManyFromTwoQueuesListener() {
			return settings.receiveMessages && settings.receivesManyTestEnabled ? new ReceiveManyFromTwoQueuesListener() : null;
		}

		@Bean
		ReceiveBatchesFromTwoQueuesListener receiveBatchesFromTwoQueuesListener() {
			return settings.receiveMessages && settings.receivesBatchesTestEnabled ? new ReceiveBatchesFromTwoQueuesListener() : null;
		}

		MessageContainer messageContainer = new MessageContainer();

		@Bean
		MessageContainer messageContainer() {
			return messageContainer;
		}

		@Bean
		LatchContainer latchContainer() {
			return this.latchContainer;
		}

		@Bean
		LoadSimulator sleeper() {
			return new LoadSimulator();
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean(name = TEST_SQS_ASYNC_CLIENT_BEAN_NAME)
		SqsAsyncClient sqsAsyncClientProducer() {
			return BaseSqsIntegrationTest.createHighThroughputAsyncClient();
		}

		private final AtomicInteger acks = new AtomicInteger();

		private StandardSqsComponentFactory<String> getTestAckHandlerComponentFactory() {
			return new StandardSqsComponentFactory<String>() {
				@Override
				public AcknowledgementProcessor<String> createAcknowledgementProcessor(ContainerOptions options) {
					BatchingAcknowledgementProcessor<String> processor = new BatchingAcknowledgementProcessor<String>() {

						@Override
						protected CompletableFuture<Void> execute(Collection<Message<String>> messagesToAck) {
							return super.execute(messagesToAck).whenComplete((v, t) -> {
								if (t != null) {
									logger.error("Error acknowledging messages", t);
									messageContainer.errorAcking.addAll(messagesToAck.stream().map(MessageHeaderUtils::getId).collect(Collectors.toList()));
									return;
								}
								messagesToAck.forEach(msg -> {
									int acked = acks.incrementAndGet();
									if (acked % 1000 == 0) {
										logger.debug("Acked {} messages", acked);
									}
									latchContainer.messageAckLatch.countDown();
									latchContainer.batchesAckLatch.countDown();
								});
								messageContainer.successfullyAcked.addAll(messagesToAck.stream().map(MessageHeaderUtils::getId).collect(Collectors.toList()));
							});
						}
					};
					ConfigUtils.INSTANCE
						.acceptIfNotNullOrElse(processor::setAcknowledgementInterval, options.getAcknowledgementInterval(), Duration.ofSeconds(1))
						.acceptIfNotNullOrElse(processor::setAcknowledgementThreshold, options.getAcknowledgementThreshold(), 10)
						.acceptIfNotNullOrElse(processor::setAcknowledgementOrdering, options.getAcknowledgementOrdering(), AcknowledgementOrdering.PARALLEL);
					return processor;
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
