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
package io.awspring.cloud.sqs.listener.acknowledgement;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@ExtendWith(MockitoExtension.class)
class BatchingAcknowledgementProcessorTests {

	private static final Logger logger = LoggerFactory.getLogger(BatchingAcknowledgementProcessorTests.class);

	private static final Duration ACK_INTERVAL_HUNDRED_MILLIS = Duration.ofMillis(100);

	private static final Duration ACK_INTERVAL_ZERO = Duration.ZERO;

	private static final int MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN = 10;

	private static final Integer ACK_THRESHOLD_TEN = 10;

	private static final Integer ACK_THRESHOLD_ZERO = 0;

	private static final String ID = "batchingAcknowledgementProcessorTestsAckProcessor";

	private final static UUID MESSAGE_ID = UUID.randomUUID();

	@Mock
	private AcknowledgementExecutor<String> ackExecutor;

	@Mock
	Message<String> message;

	MessageHeaders messageHeaders = new MessageHeaders(null);

	@Test
	void shouldAckAfterBatch() throws Exception {
		given(message.getHeaders()).willReturn(messageHeaders);
		TaskExecutor executor = new SimpleAsyncTaskExecutor();
		List<Message<String>> messages = IntStream.range(0, ACK_THRESHOLD_TEN).mapToObj(index -> message)
				.collect(Collectors.toList());
		given(ackExecutor.execute(messages)).willReturn(CompletableFuture.completedFuture(null));
		CountDownLatch ackLatch = new CountDownLatch(1);
		BatchingAcknowledgementProcessor<String> processor = new BatchingAcknowledgementProcessor<String>() {
			@Override
			protected CompletableFuture<Void> sendToExecutor(Collection<Message<String>> messagesToAck) {
				return super.sendToExecutor(messagesToAck).thenRun(ackLatch::countDown);
			}
		};
		ContainerOptions options = ContainerOptions.builder().acknowledgementInterval(ACK_INTERVAL_ZERO)
				.acknowledgementThreshold(ACK_THRESHOLD_TEN).acknowledgementOrdering(AcknowledgementOrdering.PARALLEL)
				.build();
		processor.configure(options);
		processor.setTaskExecutor(executor);
		processor.setAcknowledgementExecutor(ackExecutor);
		processor.setMaxAcknowledgementsPerBatch(MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN);
		processor.setId(ID);
		processor.start();
		processor.doOnAcknowledge(messages);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		processor.stop();

		then(ackExecutor).should().execute(messages);
	}

	@Test
	void shouldAckAfterTime() throws Exception {
		given(message.getHeaders()).willReturn(messageHeaders);
		TaskExecutor executor = new SimpleAsyncTaskExecutor();
		List<Message<String>> messages = IntStream.range(0, 5).mapToObj(index -> message).collect(Collectors.toList());
		given(ackExecutor.execute(messages)).willReturn(CompletableFuture.completedFuture(null));
		CountDownLatch ackLatch = new CountDownLatch(1);
		BatchingAcknowledgementProcessor<String> processor = new BatchingAcknowledgementProcessor<String>() {
			@Override
			protected CompletableFuture<Void> sendToExecutor(Collection<Message<String>> messagesToAck) {
				return super.sendToExecutor(messagesToAck).thenRun(ackLatch::countDown);
			}
		};
		ContainerOptions options = ContainerOptions.builder().acknowledgementInterval(ACK_INTERVAL_HUNDRED_MILLIS)
				.acknowledgementThreshold(ACK_THRESHOLD_ZERO).acknowledgementOrdering(AcknowledgementOrdering.PARALLEL)
				.build();
		processor.configure(options);
		processor.setTaskExecutor(executor);
		processor.setAcknowledgementExecutor(ackExecutor);
		processor.setMaxAcknowledgementsPerBatch(MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN);
		processor.setId(ID);

		processor.start();
		processor.doOnAcknowledge(messages);
		assertThat(ackLatch.await(20, TimeUnit.SECONDS)).isTrue();
		processor.stop();
		then(ackExecutor).should().execute(messages);
	}

	@Test
	void shouldPartitionMessages() throws Exception {
		given(message.getHeaders()).willReturn(messageHeaders);
		TaskExecutor executor = new SimpleAsyncTaskExecutor();
		List<Message<String>> messages = IntStream.range(0, 15).mapToObj(index -> message).collect(Collectors.toList());
		given(ackExecutor.execute(any())).willReturn(CompletableFuture.completedFuture(null));
		CountDownLatch ackLatch = new CountDownLatch(1);
		BatchingAcknowledgementProcessor<String> processor = new BatchingAcknowledgementProcessor<String>() {
			@Override
			protected CompletableFuture<Void> sendToExecutor(Collection<Message<String>> messagesToAck) {
				return super.sendToExecutor(messagesToAck).thenRun(ackLatch::countDown);
			}
		};
		ContainerOptions options = ContainerOptions.builder().acknowledgementInterval(ACK_INTERVAL_HUNDRED_MILLIS)
				.acknowledgementThreshold(ACK_THRESHOLD_ZERO).acknowledgementOrdering(AcknowledgementOrdering.PARALLEL)
				.build();
		processor.configure(options);
		processor.setTaskExecutor(executor);
		processor.setAcknowledgementExecutor(ackExecutor);
		processor.setMaxAcknowledgementsPerBatch(MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN);
		processor.setId(ID);
		processor.start();
		processor.doOnAcknowledge(messages);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		processor.stop();
		then(ackExecutor).should().execute(messages.subList(0, 10));
		then(ackExecutor).should().execute(messages.subList(10, 15));
	}

	@Test
	void shouldAcknowledgeInOrder() throws Exception {
		TaskExecutor executor = new SimpleAsyncTaskExecutor();
		int numberOfMessages = 100;
		int maxMessagesPerBatch = MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN;
		List<Message<String>> messages = IntStream.range(0, numberOfMessages).mapToObj(this::createMessage)
				.collect(Collectors.toList());
		CountDownLatch ackLatch = new CountDownLatch(10);
		List<Collection<Message<String>>> acknowledgedMessages = Collections.synchronizedList(new ArrayList<>());
		AcknowledgementExecutor<String> acknowledgementExecutor = messagesToAck -> {
			ackLatch.countDown();
			acknowledgedMessages.add(messagesToAck);
			return CompletableFuture.completedFuture(null);
		};
		BatchingAcknowledgementProcessor<String> processor = new BatchingAcknowledgementProcessor<>();
		ContainerOptions options = ContainerOptions.builder().acknowledgementInterval(ACK_INTERVAL_HUNDRED_MILLIS)
				.acknowledgementThreshold(ACK_THRESHOLD_ZERO).acknowledgementOrdering(AcknowledgementOrdering.PARALLEL)
				.build();
		processor.configure(options);
		processor.setTaskExecutor(executor);
		processor.setAcknowledgementExecutor(acknowledgementExecutor);
		processor.setMaxAcknowledgementsPerBatch(MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN);
		processor.setId(ID);

		processor.start();
		processor.doOnAcknowledge(messages);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		processor.stop();

		IntStream.range(0, Math.min(messages.size() / maxMessagesPerBatch, 1))
				.forEach(index -> assertThat(acknowledgedMessages.get(index))
						.containsExactlyElementsOf(messages.subList(index * maxMessagesPerBatch,
								Math.min((index + 1) * maxMessagesPerBatch, messages.size()))));
	}

	@Test
	void shouldAcknowledgeOrderedByGroupFromManyMessageGroups() throws Exception {
		testAcknowledgementOrdering(AcknowledgementOrdering.ORDERED_BY_GROUP, msg -> MessageHeaderUtils
				.getHeaderAsString(msg, SqsHeaders.MessageSystemAttribute.SQS_MESSAGE_GROUP_ID_HEADER));
	}

	@Test
	void shouldAcknowledgeOrderedFromManyMessageGroups() throws Exception {
		testAcknowledgementOrdering(AcknowledgementOrdering.ORDERED, null);
	}

	private void testAcknowledgementOrdering(AcknowledgementOrdering acknowledgementOrdering,
			Function<Message<String>, String> groupingFunction) throws InterruptedException {
		TaskExecutor executor = new SimpleAsyncTaskExecutor();
		int numberOfMessages = 100;
		int numberOfMessageGroups = 10;
		int messagesPerGroup = numberOfMessages / numberOfMessageGroups;
		List<String> messageGroups = IntStream.range(0, numberOfMessageGroups)
				.mapToObj(index -> UUID.randomUUID().toString()).collect(Collectors.toList());
		Map<String, List<Message<String>>> messages = messageGroups
				.stream().map(
						group -> createFifoMessages(messagesPerGroup, group))
				.collect(Collectors.toMap(msgs -> MessageHeaderUtils.getHeaderAsString(msgs.iterator().next(),
						SqsHeaders.MessageSystemAttribute.SQS_MESSAGE_GROUP_ID_HEADER), msgs -> msgs));
		CountDownLatch ackLatch = new CountDownLatch(numberOfMessages);
		Map<String, Collection<String>> acknowledgedMessages = new ConcurrentHashMap<>();
		AcknowledgementExecutor<String> acknowledgementExecutor = messagesToAck -> {
			logger.info("Acknowledging {} messages", messagesToAck.size());
			return CompletableFuture.runAsync(this::sleep, executor).whenComplete((v, t) -> {
				logger.info("Done acknowledging {} messages", messagesToAck.size());
				messagesToAck.stream()
						.collect(
								groupingBy(msg -> MessageHeaderUtils.getHeaderAsString(msg,
										SqsHeaders.MessageSystemAttribute.SQS_MESSAGE_GROUP_ID_HEADER)))
						.forEach((key, value) -> acknowledgedMessages
								.computeIfAbsent(key, newGroup -> Collections.synchronizedList(new ArrayList<>()))
								.addAll(value.stream().map(Message::getPayload).collect(toList())));
				messagesToAck.forEach(msg -> ackLatch.countDown());
			});
		};
		BatchingAcknowledgementProcessor<String> processor = new BatchingAcknowledgementProcessor<>();
		ContainerOptions options = ContainerOptions.builder()// .acknowledgementInterval(ACK_INTERVAL_HUNDRED_MILLIS)
				.acknowledgementInterval(Duration.ZERO).acknowledgementThreshold(10)
				.acknowledgementOrdering(acknowledgementOrdering).build();
		if (groupingFunction != null) {
			processor.setMessageGroupingFunction(groupingFunction);
		}
		processor.configure(options);
		processor.setTaskExecutor(executor);
		processor.setAcknowledgementExecutor(acknowledgementExecutor);
		processor.setMaxAcknowledgementsPerBatch(MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN);
		processor.setId(ID);
		processor.start();
		messages.forEach(
				(group, theMessages) -> CompletableFuture.runAsync(() -> processor.doOnAcknowledge(theMessages)));
		logger.info("Sent all messages for acknowledgement");
		assertThat(ackLatch.await(1000, TimeUnit.SECONDS)).isTrue();
		processor.stop();
		messages.forEach((group, messagesFromGroup) -> assertThat(acknowledgedMessages.get(group))
				.containsExactlyElementsOf(messagesFromGroup.stream().map(Message::getPayload).collect(toList())));
	}

	private void sleep() {
		try {
			Thread.sleep(new Random().nextInt(100));
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldRecoverFromErrorInOrder() throws Exception {
		TaskExecutor executor = new SimpleAsyncTaskExecutor();
		List<List<Message<String>>> batches = IntStream.range(0, 10).mapToObj(this::createTenMessages)
				.collect(Collectors.toList());

		given(ackExecutor.execute(any())).willReturn(CompletableFuture.completedFuture(null));
		given(ackExecutor.execute(batches.get(3))).willReturn(CompletableFutures
				.failedFuture(new RuntimeException("Expected exception from shouldRecoverFromErrorInOrder")));

		CountDownLatch ackLatch = new CountDownLatch(1);
		BatchingAcknowledgementProcessor<String> processor = new BatchingAcknowledgementProcessor<String>() {
			@Override
			protected CompletableFuture<Void> sendToExecutor(Collection<Message<String>> messagesToAck) {
				return super.sendToExecutor(messagesToAck).whenComplete((v, t) -> ackLatch.countDown());
			}
		};
		ContainerOptions options = ContainerOptions.builder().acknowledgementInterval(ACK_INTERVAL_HUNDRED_MILLIS)
				.acknowledgementThreshold(ACK_THRESHOLD_ZERO).acknowledgementOrdering(AcknowledgementOrdering.ORDERED)
				.build();
		processor.configure(options);
		processor.setTaskExecutor(executor);
		processor.setAcknowledgementExecutor(ackExecutor);
		processor.setMaxAcknowledgementsPerBatch(MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN);
		processor.setId(ID);

		processor.start();
		processor.doOnAcknowledge(batches.stream().flatMap(Collection::stream).collect(Collectors.toList()));
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		processor.stop();

		ArgumentCaptor<Collection<Message<String>>> messagesCaptor = ArgumentCaptor.forClass(Collection.class);

		then(ackExecutor).should(times(10)).execute(messagesCaptor.capture());

		List<Collection<Message<String>>> executedBatches = messagesCaptor.getAllValues();

		IntStream.range(0, 10)
				.forEach(index -> assertThat(executedBatches.get(index)).containsExactlyElementsOf(batches.get(index)));
	}

	private Message<String> createMessage(int index) {
		return MessageBuilder.withPayload(String.valueOf(index)).build();
	}

	private List<Message<String>> createTenMessages(int index) {
		return IntStream.range(0, 10).mapToObj(subIndex -> createMessage((index * 10) + subIndex))
				.collect(Collectors.toList());
	}

	private Message<String> createFifoMessage(int index, String messageGroup) {
		return MessageBuilder.withPayload(String.valueOf(index))
				.setHeader(SqsHeaders.MessageSystemAttribute.SQS_MESSAGE_GROUP_ID_HEADER, messageGroup).build();
	}

	private List<Message<String>> createFifoMessages(int numberOfMessages, String messageGroup) {
		return IntStream.range(0, numberOfMessages).mapToObj(subIndex -> createFifoMessage(subIndex, messageGroup))
				.collect(Collectors.toList());
	}

}
