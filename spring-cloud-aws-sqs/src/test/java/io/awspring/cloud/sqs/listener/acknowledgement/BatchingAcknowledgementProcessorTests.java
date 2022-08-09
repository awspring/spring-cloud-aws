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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

	private static final Duration ACK_INTERVAL_HUNDRED_MILLIS = Duration.ofMillis(100);

	private static final Duration ACK_INTERVAL_ZERO = Duration.ZERO;

	private static final int MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN = 10;

	private static final Integer ACK_THRESHOLD_TEN = 10;

	private static final Integer ACK_THRESHOLD_ZERO = 0;

	private static final String ID = "ackProcessor";

	private final static UUID MESSAGE_ID = UUID.randomUUID();

	@Mock
	private AcknowledgementExecutor<String> ackExecutor;

	@Mock
	Message<String> message;

	@Mock
	MessageHeaders messageHeaders;

	@Test
	void shouldAckAfterBatch() throws Exception {
		given(message.getHeaders()).willReturn(messageHeaders);
		given(messageHeaders.get(SqsHeaders.SQS_MESSAGE_ID_HEADER, UUID.class)).willReturn(MESSAGE_ID);
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
		processor.setAcknowledgementInterval(ACK_INTERVAL_ZERO);
		processor.setAcknowledgementThreshold(ACK_THRESHOLD_TEN);
		processor.setTaskExecutor(executor);
		processor.setAcknowledgementOrdering(AcknowledgementOrdering.PARALLEL);
		processor.setAcknowledgementExecutor(ackExecutor);
		processor.setMaxAcknowledgementsPerBatch(MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN);
		processor.setId(ID);

		processor.start();
		processor.doOnAcknowledge(messages);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		processor.stop();

		verify(ackExecutor).execute(messages);
	}

	@Test
	void shouldAckAfterTime() throws Exception {
		given(message.getHeaders()).willReturn(messageHeaders);
		given(messageHeaders.get(SqsHeaders.SQS_MESSAGE_ID_HEADER, UUID.class)).willReturn(MESSAGE_ID);
		TaskExecutor executor = new SimpleAsyncTaskExecutor();
		List<Message<String>> messages = IntStream.range(0, 5).mapToObj(index -> message).collect(Collectors.toList());
		given(ackExecutor.execute(messages)).willReturn(CompletableFuture.completedFuture(null));
		CountDownLatch ackLatch = new CountDownLatch(1);
		BatchingAcknowledgementProcessor<String> processor = new BatchingAcknowledgementProcessor<String>() {
			@Override
			protected CompletableFuture<Void> sendToExecutor(Collection<Message<String>> messagesToAck) {
				ackLatch.countDown();
				return super.sendToExecutor(messagesToAck);
			}
		};
		processor.setAcknowledgementInterval(ACK_INTERVAL_HUNDRED_MILLIS);
		processor.setAcknowledgementThreshold(ACK_THRESHOLD_ZERO);
		processor.setTaskExecutor(executor);
		processor.setAcknowledgementOrdering(AcknowledgementOrdering.PARALLEL);
		processor.setAcknowledgementExecutor(ackExecutor);
		processor.setMaxAcknowledgementsPerBatch(MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN);
		processor.setId(ID);

		processor.start();
		processor.doOnAcknowledge(messages);
		assertThat(ackLatch.await(20, TimeUnit.SECONDS)).isTrue();
		processor.stop();

		verify(ackExecutor).execute(messages);
	}

	@Test
	void shouldPartitionMessages() throws Exception {
		given(message.getHeaders()).willReturn(messageHeaders);
		given(messageHeaders.get(SqsHeaders.SQS_MESSAGE_ID_HEADER, UUID.class)).willReturn(MESSAGE_ID);
		TaskExecutor executor = new SimpleAsyncTaskExecutor();
		List<Message<String>> messages = IntStream.range(0, 15).mapToObj(index -> message).collect(Collectors.toList());
		given(ackExecutor.execute(any())).willReturn(CompletableFuture.completedFuture(null));
		CountDownLatch ackLatch = new CountDownLatch(1);
		BatchingAcknowledgementProcessor<String> processor = new BatchingAcknowledgementProcessor<String>() {
			@Override
			protected CompletableFuture<Void> sendToExecutor(Collection<Message<String>> messagesToAck) {
				ackLatch.countDown();
				return super.sendToExecutor(messagesToAck);
			}
		};
		processor.setAcknowledgementInterval(ACK_INTERVAL_HUNDRED_MILLIS);
		processor.setAcknowledgementThreshold(ACK_THRESHOLD_ZERO);
		processor.setTaskExecutor(executor);
		processor.setAcknowledgementOrdering(AcknowledgementOrdering.PARALLEL);
		processor.setAcknowledgementExecutor(ackExecutor);
		processor.setMaxAcknowledgementsPerBatch(MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN);
		processor.setId(ID);

		processor.start();
		processor.doOnAcknowledge(messages);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		processor.stop();

		verify(ackExecutor).execute(messages.subList(0, 10));
		verify(ackExecutor).execute(messages.subList(10, 15));
	}

	@Test
	void shouldAcknowledgeInOrder() throws Exception {
		TaskExecutor executor = new SimpleAsyncTaskExecutor();
		int nummberOfMessages = 100;
		List<Message<String>> messages = IntStream.range(0, nummberOfMessages).mapToObj(this::createMessage)
				.collect(Collectors.toList());
		given(ackExecutor.execute(any())).willReturn(CompletableFuture.completedFuture(null));
		CountDownLatch ackLatch = new CountDownLatch(1);
		BatchingAcknowledgementProcessor<String> processor = new BatchingAcknowledgementProcessor<String>() {
			@Override
			protected CompletableFuture<Void> sendToExecutor(Collection<Message<String>> messagesToAck) {
				sleepFor(100);
				return super.sendToExecutor(messagesToAck).whenComplete((v, t) -> ackLatch.countDown());
			}
		};
		processor.setAcknowledgementInterval(ACK_INTERVAL_HUNDRED_MILLIS);
		processor.setAcknowledgementThreshold(ACK_THRESHOLD_ZERO);
		processor.setTaskExecutor(executor);
		processor.setAcknowledgementOrdering(AcknowledgementOrdering.ORDERED);
		processor.setAcknowledgementExecutor(ackExecutor);
		processor.setMaxAcknowledgementsPerBatch(MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN);
		processor.setId(ID);

		processor.start();
		processor.doOnAcknowledge(messages);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		processor.stop();

		ArgumentCaptor<Collection<Message<String>>> messagesCaptor = ArgumentCaptor.forClass(Collection.class);

		verify(ackExecutor, times(10)).execute(messagesCaptor.capture());

		List<Collection<Message<String>>> executedBatches = messagesCaptor.getAllValues();

		IntStream.range(0, Math.min(messages.size() / MAX_ACKNOWLEDGEMENTS_PER_BATCH_TEN, 1))
				.forEach(index -> assertThat(executedBatches.get(index)).containsExactlyElementsOf(
						messages.subList(index * 10, Math.max((index + 1) * 10, messages.size()))));
	}

	private Message<String> createMessage(int index) {
		return MessageBuilder.withPayload(String.valueOf(index))
				.setHeader(SqsHeaders.SQS_MESSAGE_ID_HEADER, UUID.randomUUID()).build();
	}

	private void sleepFor(int amount) {
		try {
			Thread.sleep(new Random().nextInt(amount));
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
