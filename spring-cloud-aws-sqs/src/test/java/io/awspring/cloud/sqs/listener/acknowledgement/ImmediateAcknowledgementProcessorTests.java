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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Tests for {@link ImmediateAcknowledgementProcessor}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings("unchecked")
class ImmediateAcknowledgementProcessorTests {

	@Test
	void shouldAcknowledge() throws Exception {
		AcknowledgementExecutor<Object> executor = mock(AcknowledgementExecutor.class);
		Message<Object> message = mock(Message.class);
		MessageHeaders headers = new MessageHeaders(null);
		given(message.getHeaders()).willReturn(headers);
		CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
		given(executor.execute(Collections.singletonList(message))).willReturn(result);
		CountDownLatch countDownLatch = new CountDownLatch(1);
		ImmediateAcknowledgementProcessor<Object> processor = new ImmediateAcknowledgementProcessor<Object>() {
			@Override
			protected CompletableFuture<Void> sendToExecutor(Collection<Message<Object>> messagesToAck) {
				return super.sendToExecutor(messagesToAck).thenRun(countDownLatch::countDown);
			}
		};
		processor.setMaxAcknowledgementsPerBatch(10);
		processor.setId("id");
		processor.setAcknowledgementExecutor(executor);
		processor.configure(
				SqsContainerOptions.builder().acknowledgementOrdering(AcknowledgementOrdering.PARALLEL).build());
		processor.start();
		processor.doOnAcknowledge(message);
		assertThat(countDownLatch.await(10, TimeUnit.SECONDS)).isTrue();
		processor.stop();
	}

	@Test
	void shouldPropagateErrorForOrdered() {
		testErrorPropagation(AcknowledgementOrdering.ORDERED);
	}

	@Test
	void shouldPropagateErrorForOrderedByGroup() {
		testErrorPropagation(AcknowledgementOrdering.ORDERED_BY_GROUP);
	}

	private void testErrorPropagation(AcknowledgementOrdering ordering) {
		AcknowledgementExecutor<Object> executor = mock(AcknowledgementExecutor.class);
		Message<Object> message = mock(Message.class);
		MessageHeaders messageHeaders = new MessageHeaders(Collections.singletonMap(
				SqsHeaders.MessageSystemAttribute.SQS_MESSAGE_GROUP_ID_HEADER, UUID.randomUUID().toString()));
		given(message.getHeaders()).willReturn(messageHeaders);
		RuntimeException exception = new RuntimeException("Expected exception from shouldPropagateErrorForOrdered");
		given(executor.execute(Collections.singletonList(message)))
				.willReturn(CompletableFutures.failedFuture(exception));
		ImmediateAcknowledgementProcessor<Object> processor = new ImmediateAcknowledgementProcessor<>();
		processor.setMaxAcknowledgementsPerBatch(10);
		processor.setId("id");
		processor.setAcknowledgementExecutor(executor);
		processor.configure(SqsContainerOptions.builder().acknowledgementOrdering(ordering).build());
		if (AcknowledgementOrdering.ORDERED_BY_GROUP.equals(ordering)) {
			processor.setMessageGroupingFunction(msg -> MessageHeaderUtils.getHeaderAsString(msg,
					SqsHeaders.MessageSystemAttribute.SQS_MESSAGE_GROUP_ID_HEADER));
		}
		processor.start();
		CompletableFuture<Void> ackResult = processor.doOnAcknowledge(message);
		processor.stop();
		assertThatThrownBy(ackResult::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isEqualTo(exception);
	}

	@Test
	void shouldExecuteCallbackSuccessfully() throws Exception {
		AcknowledgementExecutor<Object> executor = mock(AcknowledgementExecutor.class);
		Message<Object> message = mock(Message.class);
		MessageHeaders messageHeaders = new MessageHeaders(null);
		given(message.getHeaders()).willReturn(messageHeaders);
		CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
		given(executor.execute(Collections.singletonList(message))).willReturn(result);
		CountDownLatch countDownLatch = new CountDownLatch(1);
		ImmediateAcknowledgementProcessor<Object> processor = new ImmediateAcknowledgementProcessor<>();
		processor.setMaxAcknowledgementsPerBatch(10);
		processor.setId("id");
		processor.setAcknowledgementExecutor(executor);
		processor.setAcknowledgementResultCallback(new AsyncAcknowledgementResultCallback<Object>() {
			@Override
			public CompletableFuture<Void> onSuccess(Collection<Message<Object>> messages) {
				countDownLatch.countDown();
				return CompletableFuture.completedFuture(null);
			}
		});
		processor.configure(
				SqsContainerOptions.builder().acknowledgementOrdering(AcknowledgementOrdering.PARALLEL).build());
		processor.start();
		processor.doOnAcknowledge(message);
		assertThat(countDownLatch.await(10, TimeUnit.SECONDS)).isTrue();
		processor.stop();
	}

	@Test
	void shouldExecuteCallbackOnError() throws Exception {
		AcknowledgementExecutor<Object> executor = mock(AcknowledgementExecutor.class);
		Message<Object> message = mock(Message.class);
		MessageHeaders messageHeaders = new MessageHeaders(null);
		given(message.getHeaders()).willReturn(messageHeaders);
		RuntimeException acknowledgementException = new RuntimeException(
				"Expected exception from shouldExecuteCallbackOnError");
		CompletableFuture<Void> result = CompletableFutures.failedFuture(acknowledgementException);
		given(executor.execute(Collections.singletonList(message))).willReturn(result);
		CountDownLatch countDownLatch = new CountDownLatch(1);
		ImmediateAcknowledgementProcessor<Object> processor = new ImmediateAcknowledgementProcessor<>();
		processor.setMaxAcknowledgementsPerBatch(10);
		processor.setId("id");
		processor.setAcknowledgementExecutor(executor);
		processor.setAcknowledgementResultCallback(new AsyncAcknowledgementResultCallback<Object>() {
			@Override
			public CompletableFuture<Void> onFailure(Collection<Message<Object>> messages, Throwable t) {
				countDownLatch.countDown();
				return CompletableFuture.completedFuture(null);
			}
		});
		processor.configure(
				SqsContainerOptions.builder().acknowledgementOrdering(AcknowledgementOrdering.PARALLEL).build());
		processor.start();
		processor.doOnAcknowledge(message);
		assertThat(countDownLatch.await(10, TimeUnit.SECONDS)).isTrue();
		processor.stop();
	}

	@Test
	void shouldPropagateExecuteCallbackException() throws Exception {
		AcknowledgementExecutor<Object> executor = mock(AcknowledgementExecutor.class);
		Message<Object> message = mock(Message.class);
		MessageHeaders messageHeaders = new MessageHeaders(null);
		CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
		given(message.getHeaders()).willReturn(messageHeaders);
		given(executor.execute(Collections.singletonList(message))).willReturn(result);
		RuntimeException exception = new RuntimeException(
				"Expected exception from shouldPropagateExecuteCallbackException");
		CountDownLatch countDownLatch = new CountDownLatch(1);
		ImmediateAcknowledgementProcessor<Object> processor = new ImmediateAcknowledgementProcessor<>();
		processor.setMaxAcknowledgementsPerBatch(10);
		processor.setId("id");
		processor.setAcknowledgementExecutor(executor);
		processor.setAcknowledgementResultCallback(new AsyncAcknowledgementResultCallback<Object>() {
			@Override
			public CompletableFuture<Void> onSuccess(Collection<Message<Object>> messages) {
				countDownLatch.countDown();
				return CompletableFutures.failedFuture(exception);
			}
		});
		processor.configure(
				SqsContainerOptions.builder().acknowledgementOrdering(AcknowledgementOrdering.PARALLEL).build());
		processor.start();
		CompletableFuture<Void> resultFuture = processor.doOnAcknowledge(message);
		assertThat(countDownLatch.await(10, TimeUnit.SECONDS)).isTrue();
		processor.stop();
		assertThatThrownBy(resultFuture::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(AcknowledgementResultCallbackException.class).extracting(Throwable::getCause)
				.isInstanceOf(CompletionException.class).extracting(Throwable::getCause).isEqualTo(exception);
	}

}
