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
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import io.awspring.cloud.sqs.listener.StandardSqsComponentFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.AckHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.OnSuccessAckHandler;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StopWatch;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@SpringBootTest
class SqsIntegrationLoadTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsIntegrationLoadTests.class);

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

	final int outerBatchSize = 1;
	final int innerBatchSize = 1;
	final int totalMessages = 2 * outerBatchSize * innerBatchSize;

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
				latchContainer.batchesAckLatch);
	}

	private void testWithLoad(String queue1, String queue2, CountDownLatch countDownLatch, CountDownLatch secondLatch)
		throws InterruptedException, ExecutionException {
		String queueUrl1 = fetchQueueUrl(queue1);
		String queueUrl2 = fetchQueueUrl(queue2);
		StopWatch watch = new StopWatch();
		watch.start();
		IntStream.range(0, outerBatchSize).forEach(index -> {
			sendMessageBatchAsync(queueUrl1, index, innerBatchSize);
			sendMessageBatchAsync(queueUrl2, index + outerBatchSize, innerBatchSize);
		});
		assertThat(countDownLatch.await(600, TimeUnit.SECONDS)).isTrue();
		assertThat(secondLatch.await(600, TimeUnit.SECONDS)).isTrue();
		watch.stop();
		double totalTimeSeconds = watch.getTotalTimeSeconds();
		logger.info("{} seconds for sending and consuming {} messages. Messages / second: {}", totalTimeSeconds,
				totalMessages, totalMessages / totalTimeSeconds);
	}

	private void sendMessageBatchAsync(String queueUrl, int parentIndex, int batchSize) {
		sqsAsyncClient.sendMessageBatch(req -> req.entries(getBatchEntries(batchSize, parentIndex))
				.queueUrl(queueUrl).build())
			.thenRun(() -> logSend(queueUrl, parentIndex, batchSize))
			.exceptionally(t -> {
				logger.error("Error sending messages", t);
				return null;
			});
	}

	private void logSend(String queueUrl, int parentIndex, int batchSize) {
		if (parentIndex % 5 == 0) {
			logger.debug("Sent " + parentIndex * batchSize + " messages to queue {}", queueUrl);
		}
		logger.trace("Sending {} messages from parent index {}", batchSize, parentIndex);
	}

	private Collection<SendMessageBatchRequestEntry> getBatchEntries(int batchSize, int parentIndex) {
		return IntStream.range(0, batchSize)
				.mapToObj(index -> SendMessageBatchRequestEntry.builder().id(UUID.randomUUID().toString())
						.messageBody(getBody(parentIndex, index)).build())
				.collect(Collectors.toList());
	}

	private String getBody(int parent, int index) {
		try {
			return objectMapper
				.writeValueAsString(new MyPojo("MyPojo - " + ((parent * 10) + index), "MyPojo - secondValue"));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private String fetchQueueUrl(String receivesMessageQueueName) throws InterruptedException, ExecutionException {
		return sqsAsyncClient.getQueueUrl(req -> req.queueName(receivesMessageQueueName)).get().queueUrl();
	}

	static class ReceiveManyFromTwoQueuesListener {

		Set<String> messagesReceivedPayload = Collections.synchronizedSet(new HashSet<>());

		@Autowired
		LatchContainer latchContainer;

		AtomicInteger messagesReceived = new AtomicInteger();

		@Autowired
		Sleeper sleeper;

		@SqsListener(queueNames = { RECEIVE_FROM_MANY_1_QUEUE_NAME, RECEIVE_FROM_MANY_2_QUEUE_NAME},
			factory = HIGH_THROUGHPUT_FACTORY_NAME, id = "many-from-two-queues")
		void listen(Message<String> message) {
			logger.trace("Started processing {}", MessageHeaderUtils.getId(message));
			if (this.messagesReceivedPayload.contains(message.getPayload())) {
				logger.warn("Received duplicated message: {}", message);
			}
			this.messagesReceivedPayload.add(message.getPayload());
			sleeper.sleep();
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

		@Autowired
		Sleeper sleeper;

		@SqsListener(queueNames = { RECEIVE_BATCH_1_QUEUE_NAME,
				RECEIVE_BATCH_2_QUEUE_NAME }, factory = HIGH_THROUGHPUT_FACTORY_NAME, id = "batch-from-two-queues")
		void listen(List<Message<MyPojo>> messages) {
			logger.trace("Started processing messages {}", MessageHeaderUtils.getId(messages));
			String firstField = messages.get(0).getPayload().firstField;// Make sure we got the right type
			sleeper.sleep();
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
				.maxInflightMessagesPerQueue(10)
				.pollTimeout(Duration.ofSeconds(3))
				.messagesPerPoll(10)
				.permitAcquireTimeout(Duration.ofSeconds(1))
				.backPressureMode(BackPressureMode.HIGH_THROUGHPUT)
				.shutDownTimeout(Duration.ofSeconds(40));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createHighThroughputAsyncClient);
			factory.setContainerComponentFactory(getTestAckHandlerComponentFactory());
			return factory;
		}

		LatchContainer latchContainer = new LatchContainer();

		@Bean
		ReceiveManyFromTwoQueuesListener receiveManyFromTwoQueuesListener() {
			return new ReceiveManyFromTwoQueuesListener();
		}

		@Bean
		ReceiveBatchesFromTwoQueuesListener receiveBatchesFromTwoQueuesListener() {
			return new ReceiveBatchesFromTwoQueuesListener();
		}

		@Bean
		LatchContainer latchContainer() {
			return this.latchContainer;
		}

		@Bean
		Sleeper sleeper() {
			return new Sleeper();
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean(name = TEST_SQS_ASYNC_CLIENT_BEAN_NAME)
		SqsAsyncClient sqsAsyncClientProducer() {
			return BaseSqsIntegrationTest.createHighThroughputAsyncClient();
		}

		private StandardSqsComponentFactory<String> getTestAckHandlerComponentFactory() {
			return new StandardSqsComponentFactory<String>() {
				@Override
				public AckHandler<String> createAckHandler(ContainerOptions options) {
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
