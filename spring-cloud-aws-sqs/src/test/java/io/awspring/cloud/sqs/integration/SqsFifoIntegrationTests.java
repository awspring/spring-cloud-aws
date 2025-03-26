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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.FifoBatchGroupingStrategy;
import io.awspring.cloud.sqs.listener.FifoSqsComponentFactory;
import io.awspring.cloud.sqs.listener.ListenerMode;
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.OnSuccessAcknowledgementHandler;
import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.observation.AbstractListenerObservation;
import io.awspring.cloud.sqs.support.observation.AbstractTemplateObservation;
import io.awspring.cloud.sqs.support.observation.SqsListenerObservation;
import io.awspring.cloud.sqs.support.observation.SqsTemplateObservation;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.ObservationContextAssert;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Integration tests for handling SQS FIFO queues.
 *
 * @author Tomaz Fernandes
 * @author Mikhail Strokov
 */
@SpringBootTest
class SqsFifoIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsFifoIntegrationTests.class);

	static final String FIFO_RECEIVES_MESSAGES_IN_ORDER_QUEUE_NAME = "fifo_receives_messages_in_order.fifo";

	static final String FIFO_RECEIVES_MESSAGE_IN_ORDER_MANY_GROUPS_QUEUE_NAME = "fifo_receives_messages_in_order_many_groups.fifo";

	static final String FIFO_STOPS_PROCESSING_ON_ERROR_QUEUE_NAME = "fifo_stops_processing_on_error.fifo";

	static final String FIFO_STOPS_PROCESSING_ON_ACK_ERROR_ERROR_QUEUE_NAME = "fifo_stops_processing_on_ack_error.fifo";

	static final String FIFO_RECEIVES_BATCHES_MANY_GROUPS_QUEUE_NAME = "fifo_receives_batches_many_groups.fifo";

	static final String FIFO_RECEIVES_BATCH_GROUPING_STRATEGY_MULTIPLE_GROUPS_IN_SAME_BATCH_QUEUE_NAME = "fifo_receives_batch_grouping_strategy_multiple_groups_in_same_batch.fifo";

	static final String FIFO_MANUALLY_CREATE_CONTAINER_QUEUE_NAME = "fifo_manually_create_container_test_queue.fifo";

	static final String FIFO_MANUALLY_CREATE_FACTORY_QUEUE_NAME = "fifo_manually_create_factory_test_queue.fifo";

	static final String FIFO_MANUALLY_CREATE_BATCH_CONTAINER_QUEUE_NAME = "fifo_manually_create_batch_container_test_queue.fifo";

	static final String FIFO_MANUALLY_CREATE_BATCH_FACTORY_QUEUE_NAME = "fifo_manually_create_batch_factory_test_queue.fifo";

	static final String OBSERVES_MESSAGE_FIFO_QUEUE_NAME = "observes_fifo_message_test_queue.fifo";

	private static final String ERROR_ON_ACK_FACTORY = "errorOnAckFactory";

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	SqsTemplate sqsTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired(required = false)
	ReceivesMessageInOrderListener receivesMessageInOrderListener;

	@Autowired(required = false)
	ReceivesMessageInOrderManyGroupsListener receivesMessageInOrderManyGroupsListener;

	@Autowired(required = false)
	StopsOnErrorListener stopsOnErrorListener;

	@Autowired(required = false)
	ReceivesBatchesFromManyGroupsListener receivesBatchesFromManyGroupsListener;

	@Autowired
	LoadSimulator loadSimulator;

	@Autowired
	TestObservationRegistry observationRegistry;

	@Autowired
	Settings settings;

	@Autowired
	MessagesContainer messagesContainer;

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(
				createFifoQueue(client, FIFO_RECEIVES_MESSAGES_IN_ORDER_QUEUE_NAME, getVisibilityAttribute("20")),
				createFifoQueue(client, FIFO_RECEIVES_MESSAGE_IN_ORDER_MANY_GROUPS_QUEUE_NAME),
				createFifoQueue(client, FIFO_STOPS_PROCESSING_ON_ERROR_QUEUE_NAME, getVisibilityAttribute("2")),
				createFifoQueue(client, FIFO_STOPS_PROCESSING_ON_ACK_ERROR_ERROR_QUEUE_NAME),
				createFifoQueue(client, FIFO_RECEIVES_BATCHES_MANY_GROUPS_QUEUE_NAME),
				createFifoQueue(client, FIFO_RECEIVES_BATCH_GROUPING_STRATEGY_MULTIPLE_GROUPS_IN_SAME_BATCH_QUEUE_NAME),
				createFifoQueue(client, FIFO_MANUALLY_CREATE_CONTAINER_QUEUE_NAME),
				createFifoQueue(client, FIFO_MANUALLY_CREATE_FACTORY_QUEUE_NAME),
				createFifoQueue(client, FIFO_MANUALLY_CREATE_BATCH_CONTAINER_QUEUE_NAME),
				createFifoQueue(client, OBSERVES_MESSAGE_FIFO_QUEUE_NAME),
				createFifoQueue(client, FIFO_MANUALLY_CREATE_BATCH_FACTORY_QUEUE_NAME)).join();
	}

	private static Map<QueueAttributeName, String> getVisibilityAttribute(String value) {
		return Collections.singletonMap(QueueAttributeName.VISIBILITY_TIMEOUT, value);
	}

	private static class Settings implements SmartInitializingSingleton {

		@Autowired
		LoadSimulator loadSimulator;

		public boolean receiveMessages = true;

		public boolean sendMessages = true;

		private final int messagesPerTest = 5;

		private final int messagesPerMessageGroup = 10;

		private final int latchTimeoutSeconds = messagesPerTest * 10;

		@Override
		public void afterSingletonsInstantiated() {
			loadSimulator.setLoadEnabled(false);
			loadSimulator.setBound(1000);
			loadSimulator.setRandom(true);
		}

	}

	@Test
	void receivesMessagesInOrder() throws Exception {
		latchContainer.receivesMessageLatch = new CountDownLatch(this.settings.messagesPerTest);
		String messageGroupId = UUID.randomUUID().toString();
		List<String> values = IntStream.range(0, this.settings.messagesPerTest).mapToObj(String::valueOf)
				.collect(toList());
		sqsTemplate.sendMany(FIFO_RECEIVES_MESSAGES_IN_ORDER_QUEUE_NAME,
				createMessagesFromValues(messageGroupId, values));
		assertThat(latchContainer.receivesMessageLatch.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS)).isTrue();
		assertThat(receivesMessageInOrderListener.receivedMessages).containsExactlyElementsOf(values);
	}

	@Test
	void receivesMessagesInOrderFromManyMessageGroups() throws Exception {
		int messagesPerTest = Math.max(this.settings.messagesPerTest, 30);
		int numberOfMessageGroups = messagesPerTest / Math.max(this.settings.messagesPerMessageGroup, 10);
		int messagesPerMessageGroup = Math.max(messagesPerTest / numberOfMessageGroups, 1);
		latchContainer.receivesMessageManyGroupsLatch = new CountDownLatch(messagesPerTest);
		latchContainer.manyGroupsAcks = new CountDownLatch(messagesPerTest);
		List<String> values = IntStream.range(0, messagesPerMessageGroup).mapToObj(String::valueOf).collect(toList());
		List<String> messageGroups = IntStream.range(0, numberOfMessageGroups)
				.mapToObj(index -> UUID.randomUUID().toString()).collect(toList());
		StopWatch watch = new StopWatch();
		watch.start();
		LoadSimulator loadSimulator = new LoadSimulator().setLoadEnabled(true).setRandom(true).setBound(20);
		IntStream.range(0, messageGroups.size()).forEach(index -> {
			if (this.settings.sendMessages) {
				try {
					if (useLocalStackClient) {
						sqsTemplate.sendMany(FIFO_RECEIVES_MESSAGE_IN_ORDER_MANY_GROUPS_QUEUE_NAME,
								createMessagesFromValues(messageGroups.get(index), values));
					}
					else {
						sqsTemplate.sendManyAsync(FIFO_RECEIVES_MESSAGE_IN_ORDER_MANY_GROUPS_QUEUE_NAME,
								createMessagesFromValues(messageGroups.get(index), values));
					}
				}
				catch (Exception e) {
					logger.error("Error sending messages to queue {}",
							FIFO_RECEIVES_MESSAGE_IN_ORDER_MANY_GROUPS_QUEUE_NAME, e);
					throw (RuntimeException) e;
				}
			}
			if (index % 10 == 0) {
				loadSimulator.runLoad();
			}
		});
		assertThat(latchContainer.receivesMessageManyGroupsLatch.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS))
				.isTrue();
		assertThat(latchContainer.manyGroupsAcks.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS)).isTrue();
		watch.stop();
		messageGroups.forEach(group -> {
			assertThat(receivesMessageInOrderManyGroupsListener.receivedMessages.get(group))
					.containsExactlyElementsOf(values);
			assertThat(messagesContainer.acknowledgesFromManyGroups.get(group)).containsExactlyElementsOf(values);
		});
		double totalTimeSeconds = watch.getTotalTimeSeconds();
		logger.debug("{}s for processing {} messages in {} message groups. Messages / seconds: {}", totalTimeSeconds,
				messagesPerTest, numberOfMessageGroups, messagesPerTest / totalTimeSeconds);
	}

	@Test
	void observesMessageFifo() throws Exception {
		String messageBody = "observesMessage-payload";
		SendResult<Object> sendResult = sqsTemplate
				.send(to -> to.queue(OBSERVES_MESSAGE_FIFO_QUEUE_NAME).payload(messageBody));
		String messageGroupId = MessageHeaderUtils.getHeaderAsString(sendResult.message(),
				SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER);
		String messageDeduplicationId = MessageHeaderUtils.getHeaderAsString(sendResult.message(),
				SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER);
		logger.debug("Sent message to queue {} with messageBody {}", OBSERVES_MESSAGE_FIFO_QUEUE_NAME, messageBody);
		assertThat(latchContainer.observesFifoMessageLatch.await(10, TimeUnit.MINUTES)).isTrue();
		await()
			.atMost(10, TimeUnit.SECONDS)
			.untilAsserted(() ->
				TestObservationRegistryAssert.then(observationRegistry).hasNumberOfObservationsEqualTo(3)
				.hasHandledContextsThatSatisfy(contexts -> {
					ObservationContextAssert.then(contexts.get(0)).hasNameEqualTo("spring.aws.sqs.template")
							.isInstanceOf(SqsTemplateObservation.Context.class)
							.hasContextualNameEqualTo(OBSERVES_MESSAGE_FIFO_QUEUE_NAME + " send")
							.hasLowCardinalityKeyValue(
									AbstractTemplateObservation.Documentation.LowCardinalityTags.MESSAGING_OPERATION
											.asString(),
									"publish")
							.hasLowCardinalityKeyValue(
									AbstractTemplateObservation.Documentation.LowCardinalityTags.MESSAGING_DESTINATION_NAME
											.asString(),
									OBSERVES_MESSAGE_FIFO_QUEUE_NAME)
							.hasLowCardinalityKeyValue(
									AbstractTemplateObservation.Documentation.LowCardinalityTags.MESSAGING_DESTINATION_KIND
											.asString(),
									"queue")
							.hasLowCardinalityKeyValue(
									AbstractTemplateObservation.Documentation.LowCardinalityTags.MESSAGING_SYSTEM
											.asString(),
									"sqs")
							.hasHighCardinalityKeyValue(
									AbstractTemplateObservation.Documentation.HighCardinalityTags.MESSAGE_ID.asString(),
									sendResult.messageId().toString())
							.hasHighCardinalityKeyValue(
									SqsTemplateObservation.Documentation.HighCardinalityTags.MESSAGE_GROUP_ID
											.asString(),
									messageGroupId)
							.hasHighCardinalityKeyValue(
									SqsTemplateObservation.Documentation.HighCardinalityTags.MESSAGE_DEDUPLICATION_ID
											.asString(),
									messageDeduplicationId)
							.doesNotHaveParentObservation();
					ObservationContextAssert.then(contexts.get(1)).hasNameEqualTo("spring.aws.sqs.listener")
							.isInstanceOf(SqsListenerObservation.Context.class)
							.hasContextualNameEqualTo(OBSERVES_MESSAGE_FIFO_QUEUE_NAME + " receive")
							.hasLowCardinalityKeyValue(
									AbstractListenerObservation.Documentation.LowCardinalityTags.MESSAGING_OPERATION
											.asString(),
									"receive")
							.hasLowCardinalityKeyValue(
									AbstractListenerObservation.Documentation.LowCardinalityTags.MESSAGING_SOURCE_NAME
											.asString(),
									OBSERVES_MESSAGE_FIFO_QUEUE_NAME)
							.hasLowCardinalityKeyValue(
									AbstractListenerObservation.Documentation.LowCardinalityTags.MESSAGING_SOURCE_KIND
											.asString(),
									"queue")
							.hasLowCardinalityKeyValue(
									AbstractListenerObservation.Documentation.LowCardinalityTags.MESSAGING_SYSTEM
											.asString(),
									"sqs")
							.hasHighCardinalityKeyValue(
									AbstractListenerObservation.Documentation.HighCardinalityTags.MESSAGE_ID.asString(),
									sendResult.messageId().toString())
							.hasHighCardinalityKeyValue(
									SqsListenerObservation.Documentation.HighCardinalityTags.MESSAGE_GROUP_ID
											.asString(),
									messageGroupId)
							.hasHighCardinalityKeyValue(
									SqsListenerObservation.Documentation.HighCardinalityTags.MESSAGE_DEDUPLICATION_ID
											.asString(),
									messageDeduplicationId)
							.doesNotHaveParentObservation();
					ObservationContextAssert.then(contexts.get(2)).hasNameEqualTo("listener.process")
							.hasParentObservationContextMatching(
									contextView -> contextView.getName().equals("spring.aws.sqs.listener"));
				})
			);
	}

	@Test
	void stopsProcessingAfterException() throws Exception {
		latchContainer.stopsProcessingOnErrorLatch1 = new CountDownLatch(4);
		latchContainer.stopsProcessingOnErrorLatch2 = new CountDownLatch(this.settings.messagesPerTest + 1);
		List<String> values = IntStream.range(0, this.settings.messagesPerTest).mapToObj(String::valueOf)
				.collect(toList());
		String messageGroupId = UUID.randomUUID().toString();
		sqsTemplate.sendMany(FIFO_STOPS_PROCESSING_ON_ERROR_QUEUE_NAME,
				createMessagesFromValues(messageGroupId, values));
		assertThat(latchContainer.stopsProcessingOnErrorLatch1.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS))
				.isTrue();
		logger.debug("receivedMessagesBeforeException: {}", stopsOnErrorListener.receivedMessagesBeforeException);
		assertThat(stopsOnErrorListener.receivedMessagesBeforeException)
				.containsExactlyElementsOf(values.stream().limit(4).collect(toList()));
		assertThat(latchContainer.stopsProcessingOnErrorLatch2.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS))
				.isTrue();
		logger.debug("receivedMessagesBeforeException: {}", stopsOnErrorListener.receivedMessagesBeforeException);
		logger.debug("receivedMessagesAfterException: {}", stopsOnErrorListener.receivedMessagesAfterException);
		assertThat(stopsOnErrorListener.receivedMessagesBeforeException)
				.containsExactlyElementsOf(values.stream().limit(4).collect(toList()));
		assertThat(stopsOnErrorListener.receivedMessagesAfterException)
				.containsExactlyElementsOf(values.subList(3, this.settings.messagesPerTest));
	}

	final AtomicBoolean stopsProcessingOnAckErrorHasThrown = new AtomicBoolean(false);

	@Test
	void stopsProcessingAfterAckException() throws Exception {
		latchContainer.stopsProcessingOnAckErrorLatch1 = new CountDownLatch(4);
		latchContainer.stopsProcessingOnAckErrorLatch2 = new CountDownLatch(this.settings.messagesPerTest - 3);
		latchContainer.stopsProcessingOnAckErrorHasThrown = new CountDownLatch(1);
		messagesContainer.stopsProcessingOnAckErrorBeforeThrown.clear();
		messagesContainer.stopsProcessingOnAckErrorAfterThrown.clear();
		stopsProcessingOnAckErrorHasThrown.set(false);
		List<String> values = IntStream.range(0, this.settings.messagesPerTest).mapToObj(String::valueOf)
				.collect(toList());
		String messageGroupId = UUID.randomUUID().toString();
		sqsTemplate.sendMany(FIFO_STOPS_PROCESSING_ON_ACK_ERROR_ERROR_QUEUE_NAME,
				createMessagesFromValues(messageGroupId, values));
		assertThat(latchContainer.stopsProcessingOnAckErrorLatch1.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS))
				.isTrue();
		logger.debug("Messages consumed before error: {}", messagesContainer.stopsProcessingOnAckErrorBeforeThrown);
		assertThat(messagesContainer.stopsProcessingOnAckErrorBeforeThrown)
				.containsExactlyElementsOf(values.stream().limit(4).collect(toList()));
		assertThat(latchContainer.stopsProcessingOnAckErrorLatch2.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS))
				.isTrue();
		logger.debug("Messages consumed before error second latch: {}",
				messagesContainer.stopsProcessingOnAckErrorBeforeThrown);
		assertThat(messagesContainer.stopsProcessingOnAckErrorBeforeThrown)
				.containsExactlyElementsOf(values.stream().limit(4).collect(toList()));
		logger.debug("Messages consumed after error: {}", messagesContainer.stopsProcessingOnAckErrorAfterThrown);
		assertThat(messagesContainer.stopsProcessingOnAckErrorAfterThrown)
				.containsExactlyElementsOf(values.subList(3, this.settings.messagesPerTest));
	}

	@Test
	void receivesBatchesManyGroups() throws Exception {
		latchContainer.receivesBatchManyGroupsLatch = new CountDownLatch(this.settings.messagesPerTest * 3);
		List<String> values = IntStream.range(0, this.settings.messagesPerTest).mapToObj(String::valueOf)
				.collect(toList());
		String messageGroupId1 = UUID.randomUUID().toString();
		String messageGroupId2 = UUID.randomUUID().toString();
		String messageGroupId3 = UUID.randomUUID().toString();
		sqsTemplate.sendMany(FIFO_RECEIVES_BATCHES_MANY_GROUPS_QUEUE_NAME,
				createMessagesFromValues(messageGroupId1, values));
		sqsTemplate.sendMany(FIFO_RECEIVES_BATCHES_MANY_GROUPS_QUEUE_NAME,
				createMessagesFromValues(messageGroupId2, values));
		sqsTemplate.sendMany(FIFO_RECEIVES_BATCHES_MANY_GROUPS_QUEUE_NAME,
				createMessagesFromValues(messageGroupId3, values));
		assertThat(latchContainer.receivesBatchManyGroupsLatch.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS))
				.isTrue();
		assertThat(receivesBatchesFromManyGroupsListener.receivedMessages.get(messageGroupId1))
				.containsExactlyElementsOf(values);
		assertThat(receivesBatchesFromManyGroupsListener.receivedMessages.get(messageGroupId2))
				.containsExactlyElementsOf(values);
		assertThat(receivesBatchesFromManyGroupsListener.receivedMessages.get(messageGroupId3))
				.containsExactlyElementsOf(values);
	}

	@Test
	void receivesFifoBatchGroupingStrategyMultipleGroupsInSameBatch() throws Exception {
		List<String> values = IntStream.range(0, 2).mapToObj(String::valueOf).collect(toList());
		String messageGroupId1 = UUID.randomUUID().toString();
		String messageGroupId2 = UUID.randomUUID().toString();
		List<Message<String>> messages = new ArrayList<>();
		messages.addAll(createMessagesFromValues(messageGroupId1, values));
		messages.addAll(createMessagesFromValues(messageGroupId2, values));
		sqsTemplate.sendMany(FIFO_RECEIVES_BATCH_GROUPING_STRATEGY_MULTIPLE_GROUPS_IN_SAME_BATCH_QUEUE_NAME, messages);

		SqsMessageListenerContainer<String> container = SqsMessageListenerContainer.<String> builder()
				.queueNames(FIFO_RECEIVES_BATCH_GROUPING_STRATEGY_MULTIPLE_GROUPS_IN_SAME_BATCH_QUEUE_NAME)
				.messageListener(new MessageListener<>() {
					@Override
					public void onMessage(Message<String> message) {
						throw new UnsupportedOperationException("Batch listener");
					}

					@Override
					public void onMessage(Collection<Message<String>> messages) {
						assertThat(MessageHeaderUtils.getHeader(messages,
								SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, String.class).stream()
								.distinct().count()).isEqualTo(2);
						latchContainer.receivesFifoBatchGroupingStrategyMultipleGroupsInSameBatchLatch.countDown();
					}
				})
				.configure(options -> options.maxConcurrentMessages(10).pollTimeout(Duration.ofSeconds(10))
						.maxMessagesPerPoll(10).maxDelayBetweenPolls(Duration.ofSeconds(1))
						.fifoBatchGroupingStrategy(FifoBatchGroupingStrategy.PROCESS_MULTIPLE_GROUPS_IN_SAME_BATCH)
						.listenerMode(ListenerMode.BATCH))
				.sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient()).build();

		try {
			container.start();
			assertThat(latchContainer.receivesFifoBatchGroupingStrategyMultipleGroupsInSameBatchLatch
					.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS)).isTrue();
		}
		finally {
			container.stop();
		}

	}

	@Test
	void manuallyCreatesContainer() throws Exception {
		List<String> values = IntStream.range(0, this.settings.messagesPerTest).mapToObj(String::valueOf)
				.collect(toList());
		sqsTemplate.sendMany(FIFO_MANUALLY_CREATE_CONTAINER_QUEUE_NAME,
				createMessagesFromValues(UUID.randomUUID().toString(), values));
		assertThat(latchContainer.manuallyCreatedContainerLatch.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS))
				.isTrue();
		assertThat(messagesContainer.manuallyCreatedContainerMessages).containsExactlyElementsOf(values);
	}

	@Test
	void manuallyCreatesBatchContainer() throws Exception {
		List<String> values = IntStream.range(0, this.settings.messagesPerTest).mapToObj(String::valueOf)
				.collect(toList());
		sqsTemplate.sendMany(FIFO_MANUALLY_CREATE_BATCH_CONTAINER_QUEUE_NAME,
				createMessagesFromValues(UUID.randomUUID().toString(), values));
		assertThat(
				latchContainer.manuallyCreatedBatchContainerLatch.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS))
				.isTrue();
		assertThat(messagesContainer.manuallyCreatedBatchContainerMessages).containsExactlyElementsOf(values);
	}

	@Test
	void manuallyCreatesFactory() throws Exception {
		List<String> values = IntStream.range(0, this.settings.messagesPerTest).mapToObj(String::valueOf)
				.collect(toList());
		sqsTemplate.sendMany(FIFO_MANUALLY_CREATE_FACTORY_QUEUE_NAME,
				createMessagesFromValues(UUID.randomUUID().toString(), values));
		assertThat(latchContainer.manuallyCreatedFactoryLatch.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS))
				.isTrue();
		assertThat(messagesContainer.manuallyCreatedFactoryMessages).containsExactlyElementsOf(values);
	}

	@Test
	void manuallyCreatesBatchFactory() throws Exception {
		List<String> values = IntStream.range(0, this.settings.messagesPerTest).mapToObj(String::valueOf)
				.collect(toList());
		sqsTemplate.sendMany(FIFO_MANUALLY_CREATE_BATCH_FACTORY_QUEUE_NAME,
				createMessagesFromValues(UUID.randomUUID().toString(), values));
		assertThat(
				latchContainer.manuallyCreatedBatchFactoryLatch.await(settings.latchTimeoutSeconds, TimeUnit.SECONDS))
				.isTrue();
		assertThat(messagesContainer.manuallyCreatedBatchFactoryMessages).containsExactlyElementsOf(values);
	}

	private Message<String> createMessage(String body, String messageGroupId) {
		return MessageBuilder.withPayload(body)
				.setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, messageGroupId)
				.setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER,
						UUID.randomUUID().toString())
				.build();
	}

	private List<Message<String>> createMessagesFromValues(String messageGroupId, List<String> values) {
		return values.stream().map(value -> createMessage(value, messageGroupId)).toList();
	}

	static class ReceivesMessageInOrderListener {

		List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		LoadSimulator loadSimulator;

		@SqsListener(queueNames = FIFO_RECEIVES_MESSAGES_IN_ORDER_QUEUE_NAME)
		void listen(Message<String> message) {
			logger.debug("Received message with id {} and payload {} from ReceivesMessageInOrderListener",
					MessageHeaderUtils.getId(message), message.getPayload());
			loadSimulator.runLoad();
			receivedMessages.add(message.getPayload());
			latchContainer.receivesMessageLatch.countDown();
		}
	}

	static class ObservesFifoMessageListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		ObservationRegistry observationRegistry;

		@SqsListener(queueNames = OBSERVES_MESSAGE_FIFO_QUEUE_NAME, id = "observes-fifo-message-container", factory = "observationSqsListenerContainerFactory")
		void listen(String message) {
			Observation.createNotStarted("listener.process", observationRegistry).observe(() -> {
				logger.debug("Observed message in Listener Method: {}", message);
				latchContainer.observesFifoMessageLatch.countDown();
			});

			logger.debug("Received observed message in Listener Method: " + message);
		}
	}

	static class ReceivesMessageInOrderManyGroupsListener {

		private final AtomicInteger totalMessagesReceived = new AtomicInteger();

		private final Map<String, List<String>> receivedMessages = new ConcurrentHashMap<>();

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		LoadSimulator loadSimulator;

		@SqsListener(queueNames = FIFO_RECEIVES_MESSAGE_IN_ORDER_MANY_GROUPS_QUEUE_NAME, id = "receives-in-order-many-groups")
		void listen(Message<String> message,
				@Header(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER) String groupId) {
			logger.trace("Received message {} in listener method from groupId {}", message.getPayload(), groupId);
			loadSimulator.runLoad();
			List<String> messageList = receivedMessages.computeIfAbsent(groupId, newGroupId -> new ArrayList<>());
			if (messageList.contains(message.getPayload())) {
				logger.warn("Message {} with id {} already present for group id {}", message.getPayload(),
						MessageHeaderUtils.getId(message), groupId);
			}
			messageList.add(message.getPayload());
			latchContainer.receivesMessageManyGroupsLatch.countDown();
			int received = totalMessagesReceived.incrementAndGet();
			if (received % 1000 == 0) {
				logger.debug("{} messages received from {} message groups", received, receivedMessages.size());
			}
			// logger.debug("Message {} processed.", message);
		}
	}

	static class StopsOnErrorListener {

		List<String> receivedMessagesBeforeException = Collections.synchronizedList(new ArrayList<>());

		List<String> receivedMessagesAfterException = Collections.synchronizedList(new ArrayList<>());

		AtomicBoolean hasThrown = new AtomicBoolean(false);

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		LoadSimulator loadSimulator;

		@SqsListener(queueNames = FIFO_STOPS_PROCESSING_ON_ERROR_QUEUE_NAME, messageVisibilitySeconds = "3", id = "stops-processing-on-error")
		void listen(String message) {
			logger.debug("Received message in listener method: " + message);
			loadSimulator.runLoad(500);
			if (!hasThrown.get()) {
				this.receivedMessagesBeforeException.add(message);
			}
			else {
				this.receivedMessagesAfterException.add(message);
			}
			latchContainer.stopsProcessingOnErrorLatch1.countDown();
			latchContainer.stopsProcessingOnErrorLatch2.countDown();
			if ("3".equals(message) && this.hasThrown.compareAndSet(false, true)) {
				throw new RuntimeException("Expected exception from stops-processing-on-error");
			}
		}
	}

	static class StopsOnAckErrorListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = FIFO_STOPS_PROCESSING_ON_ACK_ERROR_ERROR_QUEUE_NAME, factory = ERROR_ON_ACK_FACTORY, messageVisibilitySeconds = "2", id = "stops-on-ack-error")
		void listen(String message) {
			logger.debug("Received message in listener method: " + message);
		}
	}

	static class ReceivesBatchesFromManyGroupsListener {

		Map<String, List<String>> receivedMessages = new ConcurrentHashMap<>();

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = FIFO_RECEIVES_BATCHES_MANY_GROUPS_QUEUE_NAME, messageVisibilitySeconds = "20")
		void listen(List<Message<String>> messages) {
			String firstMessage = messages.iterator().next().getPayload();// Make sure we got the right type
			Assert.isTrue(MessageHeaderUtils
					.getHeader(messages, SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, String.class)
					.stream().distinct().count() == 1, "More than one message group returned in the same batch");
			String messageGroupId = messages.iterator().next().getHeaders()
					.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, String.class);
			List<String> values = messages.stream().map(Message::getPayload).collect(toList());
			logger.trace("Started processing messages {} for group id {}", values, messageGroupId);
			receivedMessages.computeIfAbsent(messageGroupId, groupId -> Collections.synchronizedList(new ArrayList<>()))
					.addAll(values);
			messages.forEach(msg -> latchContainer.receivesBatchManyGroupsLatch.countDown());
			logger.trace("Finished processing messages {} for group id {}", values, messageGroupId);
		}

	}

	static class LatchContainer {

		Settings settings;

		final CountDownLatch manuallyCreatedContainerLatch;
		final CountDownLatch manuallyCreatedFactoryLatch;
		final CountDownLatch manuallyCreatedBatchContainerLatch;
		final CountDownLatch manuallyCreatedBatchFactoryLatch;

		// Lazily initialized
		CountDownLatch receivesMessageLatch;
		CountDownLatch receivesMessageManyGroupsLatch;
		CountDownLatch manyGroupsAcks;
		CountDownLatch observesFifoMessageLatch;
		CountDownLatch stopsProcessingOnErrorLatch1;
		CountDownLatch stopsProcessingOnErrorLatch2;
		CountDownLatch stopsProcessingOnAckErrorLatch1;
		CountDownLatch stopsProcessingOnAckErrorLatch2;
		CountDownLatch stopsProcessingOnAckErrorHasThrown;
		CountDownLatch receivesBatchManyGroupsLatch;
		CountDownLatch receivesFifoBatchGroupingStrategyMultipleGroupsInSameBatchLatch;

		LatchContainer(Settings settings) {
			this.settings = settings;
			this.manuallyCreatedContainerLatch = new CountDownLatch(this.settings.messagesPerTest);
			this.manuallyCreatedFactoryLatch = new CountDownLatch(this.settings.messagesPerTest);
			this.manuallyCreatedBatchContainerLatch = new CountDownLatch(this.settings.messagesPerTest);
			this.manuallyCreatedBatchFactoryLatch = new CountDownLatch(this.settings.messagesPerTest);

			// Lazily initialized
			this.receivesMessageLatch = new CountDownLatch(1);
			this.receivesMessageManyGroupsLatch = new CountDownLatch(1);
			this.manyGroupsAcks = new CountDownLatch(1);
			this.stopsProcessingOnErrorLatch1 = new CountDownLatch(3);
			this.stopsProcessingOnErrorLatch2 = new CountDownLatch(1);
			this.stopsProcessingOnAckErrorLatch1 = new CountDownLatch(1);
			this.stopsProcessingOnAckErrorLatch2 = new CountDownLatch(1);
			this.receivesBatchManyGroupsLatch = new CountDownLatch(1);
			this.receivesFifoBatchGroupingStrategyMultipleGroupsInSameBatchLatch = new CountDownLatch(1);
			this.stopsProcessingOnAckErrorHasThrown = new CountDownLatch(1);
			this.observesFifoMessageLatch = new CountDownLatch(1);
		}

	}

	static class MessagesContainer {

		Map<String, List<String>> acknowledgesFromManyGroups = Collections.synchronizedMap(new HashMap<>());
		List<String> manuallyCreatedContainerMessages = Collections.synchronizedList(new ArrayList<>());
		List<String> manuallyCreatedBatchContainerMessages = Collections.synchronizedList(new ArrayList<>());
		List<String> manuallyCreatedFactoryMessages = Collections.synchronizedList(new ArrayList<>());
		List<String> manuallyCreatedBatchFactoryMessages = Collections.synchronizedList(new ArrayList<>());
		List<String> stopsProcessingOnAckErrorBeforeThrown = Collections.synchronizedList(new ArrayList<>());
		List<String> stopsProcessingOnAckErrorAfterThrown = Collections.synchronizedList(new ArrayList<>());

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
		public SqsMessageListenerContainerFactory<String> observationSqsListenerContainerFactory(ObservationRegistry observationRegistry) {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options -> options
				.maxConcurrentMessages(10)
				.acknowledgementThreshold(10)
				.acknowledgementOrdering(AcknowledgementOrdering.ORDERED_BY_GROUP)
				.acknowledgementInterval(Duration.ofSeconds(1))
				.observationRegistry(observationRegistry)
			);
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createHighThroughputAsyncClient);
			return factory;
		}

			// @formatter:off
		@Bean
		public SqsMessageListenerContainerFactory<String> defaultSqsListenerContainerFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options -> options
				.maxConcurrentMessages(10)
				.acknowledgementThreshold(10)
				.acknowledgementOrdering(AcknowledgementOrdering.ORDERED_BY_GROUP)
				.acknowledgementInterval(Duration.ofSeconds(1))
			);
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createHighThroughputAsyncClient);
			factory.setAcknowledgementResultCallback(new AcknowledgementResultCallback<String>() {

				final AtomicInteger ackedMessages = new AtomicInteger();

				@Override
				public void onSuccess(Collection<Message<String>> messages) {
					if (FIFO_RECEIVES_MESSAGE_IN_ORDER_MANY_GROUPS_QUEUE_NAME.equals(MessageHeaderUtils.getHeaderAsString(messages.iterator().next(), SqsHeaders.SQS_QUEUE_NAME_HEADER))) {
						messages.stream()
							.collect(groupingBy(msg -> MessageHeaderUtils.getHeaderAsString(msg, SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER)))
							.forEach((key, value) -> messagesContainer.acknowledgesFromManyGroups.computeIfAbsent(key,
								newGroup -> Collections.synchronizedList(new ArrayList<>())).addAll(value.stream().map(Message::getPayload).collect(toList())));
						messages.forEach(msg -> {
							int acked = ackedMessages.incrementAndGet();
							if (acked % 1000 == 0) {
								logger.debug("Acknowledged {} messages", acked);
							}
							latchContainer.manyGroupsAcks.countDown();
						});
					}
				}
			});
			return factory;
		}

		@Bean(ERROR_ON_ACK_FACTORY)
		public SqsMessageListenerContainerFactory<String> errorOnAckSqsListenerContainerFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options -> options
				.maxConcurrentMessages(10)
				.acknowledgementThreshold(10)
				.acknowledgementInterval(Duration.ofMillis(200))
				.maxDelayBetweenPolls(Duration.ofSeconds(1))
				.pollTimeout(Duration.ofSeconds(3)));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			factory.setContainerComponentFactories(Collections.singletonList(new FifoSqsComponentFactory<String>() {
				@Override
				public AcknowledgementHandler<String> createAcknowledgementHandler(SqsContainerOptions options) {
					return new OnSuccessAcknowledgementHandler<>() {

						@Override
						public CompletableFuture<Void> onSuccess(Message<String> message,
																 AcknowledgementCallback<String> callback) {
							if (message.getPayload().equals("3") && latchContainer.stopsProcessingOnAckErrorHasThrown.getCount() == 1) {
								latchContainer.stopsProcessingOnAckErrorHasThrown.countDown();
								messagesContainer.stopsProcessingOnAckErrorBeforeThrown.add(message.getPayload());
								latchContainer.stopsProcessingOnAckErrorLatch1.countDown();
								logger.debug("stopsProcessingOnAckErrorLatch1 countdown. Remaining: {}",
									latchContainer.stopsProcessingOnAckErrorLatch1.getCount());
								return CompletableFutures.failedFuture(new RuntimeException("Expected acking error"));
							}
							return super.onSuccess(message, callback).whenComplete((v, t) -> handleResult(message));
						}

						private void handleResult(Message<String> message) {
							if (latchContainer.stopsProcessingOnAckErrorHasThrown.getCount() == 1) {
								messagesContainer.stopsProcessingOnAckErrorBeforeThrown.add(message.getPayload());
								latchContainer.stopsProcessingOnAckErrorLatch1.countDown();
								logger.debug("stopsProcessingOnAckErrorLatch1 countdown. Remaining: {}",
									latchContainer.stopsProcessingOnAckErrorLatch1.getCount());
							}
							else {
								messagesContainer.stopsProcessingOnAckErrorAfterThrown.add(message.getPayload());
								latchContainer.stopsProcessingOnAckErrorLatch2.countDown();
								logger.debug("stopsProcessingOnAckErrorLatch2 countdown. Remaining: {}",
									latchContainer.stopsProcessingOnAckErrorLatch2.getCount());
							}
						}
					};
				}
			}));
			return factory;
		}

		@Bean
		public MessageListenerContainer<String> manuallyCreatedContainer() {
			SqsMessageListenerContainer<String> container = new SqsMessageListenerContainer<>(createAsyncClient());
			container.configure(options -> options
				.maxDelayBetweenPolls(Duration.ofSeconds(1))
				.pollTimeout(Duration.ofSeconds(1)));
			container.setQueueNames(FIFO_MANUALLY_CREATE_CONTAINER_QUEUE_NAME);
			container.setId("fifo-manually-created-container");
			container.setMessageListener(msg -> {
				messagesContainer.manuallyCreatedContainerMessages.add(msg.getPayload());
				latchContainer.manuallyCreatedContainerLatch.countDown();
			});
			return container;
		}

		@Bean
		public MessageListenerContainer<String> manuallyCreatedBatchContainer() {
			SqsMessageListenerContainer<String> container = new SqsMessageListenerContainer<>(createAsyncClient());
			container.configure(options -> options
				.maxDelayBetweenPolls(Duration.ofSeconds(1))
				.pollTimeout(Duration.ofSeconds(1))
				.listenerMode(ListenerMode.BATCH));
			container.setQueueNames(FIFO_MANUALLY_CREATE_BATCH_CONTAINER_QUEUE_NAME);
			container.setMessageListener(new MessageListener<String>() {
				@Override
				public void onMessage(Message<String> message) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void onMessage(Collection<Message<String>> messages) {
					messagesContainer.manuallyCreatedBatchContainerMessages
						.addAll(messages.stream().map(Message::getPayload).collect(toList()));
					messages.forEach(msg -> latchContainer.manuallyCreatedBatchContainerLatch.countDown());
				}
			});
			return container;
		}

		@Bean
		public SqsMessageListenerContainer<String> manuallyCreatedFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options ->
				options.maxConcurrentMessages(10)
					.pollTimeout(Duration.ofSeconds(1))
					.maxMessagesPerPoll(10)
					.maxDelayBetweenPolls(Duration.ofSeconds(1)));
			factory.setSqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient());
			factory.setMessageListener(msg -> {
				logger.debug("Processed message {}", msg.getPayload());
				messagesContainer.manuallyCreatedFactoryMessages.add(msg.getPayload());
				latchContainer.manuallyCreatedFactoryLatch.countDown();
			});
			return factory.createContainer(FIFO_MANUALLY_CREATE_FACTORY_QUEUE_NAME);
		}

		@Bean
		public MessageListenerContainer<String> manuallyCreatedBatchFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options -> options
				.maxConcurrentMessages(10)
				.pollTimeout(Duration.ofSeconds(1))
				.maxMessagesPerPoll(10)
				.maxDelayBetweenPolls(Duration.ofSeconds(1))
				.listenerMode(ListenerMode.BATCH));
			factory.setSqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient());
			factory.setMessageListener(new MessageListener<String>() {
				@Override
				public void onMessage(Message<String> message) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void onMessage(Collection<Message<String>> messages) {
					messagesContainer.manuallyCreatedBatchFactoryMessages
						.addAll(messages.stream().map(Message::getPayload).collect(toList()));
					messages.forEach(msg -> latchContainer.manuallyCreatedBatchFactoryLatch.countDown());
				}
			});
			return factory.createContainer(FIFO_MANUALLY_CREATE_BATCH_FACTORY_QUEUE_NAME);
		}
		// @formatter:on

		@Bean
		ReceivesMessageInOrderListener receivesMessageInOrderListener() {
			return new ReceivesMessageInOrderListener();
		}

		@Bean
		ReceivesMessageInOrderManyGroupsListener receivesMessageInOrderManyGroupsListener() {
			if (settings.receiveMessages) {
				return new ReceivesMessageInOrderManyGroupsListener();
			}
			return null;
		}

		@Bean
		StopsOnErrorListener stopsOnErrorListener() {
			return new StopsOnErrorListener();
		}

		@Bean
		StopsOnAckErrorListener stopsOnAckErrorListener() {
			return new StopsOnAckErrorListener();
		}

		@Bean
		ReceivesBatchesFromManyGroupsListener receiveBatchesFromManyGroupsListener() {
			return new ReceivesBatchesFromManyGroupsListener();
		}

		@Bean
		ObservesFifoMessageListener observesFifoMessageListener() {
			return new ObservesFifoMessageListener();
		}

		@Bean
		ObservationRegistry observationRegistry() {
			return TestObservationRegistry.create();
		}

		Settings settings = new Settings();
		LatchContainer latchContainer = new LatchContainer(settings);

		@Bean
		LatchContainer latchContainer() {
			return this.latchContainer;
		}

		@Bean
		LoadSimulator loadSimulator() {
			return new LoadSimulator();
		}

		@Bean
		Settings settings() {
			return settings;
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		SqsTemplate sqsTemplate(ObservationRegistry observationRegistry) {
			return SqsTemplate.builder().configure(options -> options.observationRegistry(observationRegistry))
					.sqsAsyncClient(BaseSqsIntegrationTest.createHighThroughputAsyncClient()).build();
		}

	}

}
