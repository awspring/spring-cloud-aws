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
package io.awspring.cloud.sqs.listener.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementHandler;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class BeforeProcessingInterceptorExecutionStageTests {

	@Test
	void shouldExecuteInterceptorsInOrder() {
		MessageHeaders headers = getMessageHeaders();
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		Message<Object> message4 = mock(Message.class);
		CompletableFuture<Message<Object>> messageFuture2 = CompletableFuture.completedFuture(message2);
		CompletableFuture<Message<Object>> messageFuture3 = CompletableFuture.completedFuture(message3);
		CompletableFuture<Message<Object>> messageFuture4 = CompletableFuture.completedFuture(message4);

		AsyncMessageInterceptor<Object> interceptor1 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor2 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor3 = mock(AsyncMessageInterceptor.class);

		when(interceptor1.intercept(message1)).thenReturn(messageFuture2);
		when(interceptor2.intercept(message2)).thenReturn(messageFuture3);
		when(interceptor3.intercept(message3)).thenReturn(messageFuture4);
		when(message1.getHeaders()).thenReturn(headers);
		when(message2.getHeaders()).thenReturn(headers);
		when(message3.getHeaders()).thenReturn(headers);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new BeforeProcessingInterceptorExecutionStage<>(
				createConfiguration(Arrays.asList(interceptor1, interceptor2, interceptor3)));
		CompletableFuture<Message<Object>> future = stage.process(message1, context);
		assertThat(future.join()).isEqualTo(message4);

		InOrder inOrder = inOrder(interceptor1, interceptor2, interceptor3);
		inOrder.verify(interceptor1).intercept(message1);
		inOrder.verify(interceptor2).intercept(message2);
		inOrder.verify(interceptor3).intercept(message3);

	}

	@Test
	void shouldThrowIfInterceptorReturnsNull() {
		MessageHeaders headers = getMessageHeaders();
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		CompletableFuture<Message<Object>> messageFuture2 = CompletableFuture.completedFuture(message2);

		AsyncMessageInterceptor<Object> interceptor1 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor2 = mock(AsyncMessageInterceptor.class);

		when(interceptor1.intercept(message1)).thenReturn(messageFuture2);
		when(interceptor2.intercept(message2)).thenReturn(CompletableFuture.completedFuture(null));
		when(message1.getHeaders()).thenReturn(headers);
		when(message2.getHeaders()).thenReturn(headers);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();
		MessageProcessingPipeline<Object> stage = new BeforeProcessingInterceptorExecutionStage<>(
				createConfiguration(Arrays.asList(interceptor1, interceptor2)));
		CompletableFuture<Message<Object>> future = stage.process(message1, context);
		assertThatThrownBy(future::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(IllegalArgumentException.class);

		InOrder inOrder = inOrder(interceptor1, interceptor2);
		inOrder.verify(interceptor1).intercept(message1);
		inOrder.verify(interceptor2).intercept(message2);

	}

	@Test
	void shouldExecuteInterceptorsInOrderBatch() {
		MessageHeaders headers = getMessageHeaders();
		Message<Object> message11 = mock(Message.class);
		Message<Object> message12 = mock(Message.class);
		Message<Object> message13 = mock(Message.class);
		Message<Object> message14 = mock(Message.class);

		Message<Object> message21 = mock(Message.class);
		Message<Object> message22 = mock(Message.class);
		Message<Object> message23 = mock(Message.class);
		Message<Object> message24 = mock(Message.class);

		Message<Object> message31 = mock(Message.class);
		Message<Object> message32 = mock(Message.class);
		Message<Object> message33 = mock(Message.class);
		Message<Object> message34 = mock(Message.class);

		List<Message<Object>> firstBatch = Arrays.asList(message11, message21, message31);
		List<Message<Object>> secondBatch = Arrays.asList(message12, message22, message32);
		List<Message<Object>> thirdBatch = Arrays.asList(message13, message23, message33);
		List<Message<Object>> fourthBatch = Arrays.asList(message14, message24, message34);

		CompletableFuture<Collection<Message<Object>>> messageFuture1 = CompletableFuture.completedFuture(secondBatch);
		CompletableFuture<Collection<Message<Object>>> messageFuture2 = CompletableFuture.completedFuture(thirdBatch);
		CompletableFuture<Collection<Message<Object>>> messageFuture3 = CompletableFuture.completedFuture(fourthBatch);

		AsyncMessageInterceptor<Object> interceptor1 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor2 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor3 = mock(AsyncMessageInterceptor.class);

		when(interceptor1.intercept(firstBatch)).thenReturn(messageFuture1);
		when(interceptor2.intercept(secondBatch)).thenReturn(messageFuture2);
		when(interceptor3.intercept(thirdBatch)).thenReturn(messageFuture3);
		when(message11.getHeaders()).thenReturn(headers);
		when(message21.getHeaders()).thenReturn(headers);
		when(message31.getHeaders()).thenReturn(headers);
		when(message12.getHeaders()).thenReturn(headers);
		when(message22.getHeaders()).thenReturn(headers);
		when(message32.getHeaders()).thenReturn(headers);
		when(message13.getHeaders()).thenReturn(headers);
		when(message23.getHeaders()).thenReturn(headers);
		when(message33.getHeaders()).thenReturn(headers);
		when(message14.getHeaders()).thenReturn(headers);
		when(message24.getHeaders()).thenReturn(headers);
		when(message34.getHeaders()).thenReturn(headers);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new BeforeProcessingInterceptorExecutionStage<>(
				createConfiguration(Arrays.asList(interceptor1, interceptor2, interceptor3)));
		CompletableFuture<Collection<Message<Object>>> result = stage.process(firstBatch, context);
		assertThat(result.join()).isEqualTo(fourthBatch);

		InOrder inOrder = inOrder(interceptor1, interceptor2, interceptor3);
		inOrder.verify(interceptor1).intercept(firstBatch);
		inOrder.verify(interceptor2).intercept(secondBatch);
		inOrder.verify(interceptor3).intercept(thirdBatch);

	}

	@NotNull
	private MessageHeaders getMessageHeaders() {
		return new MessageHeaders(null);
	}

	@Test
	void shouldThrowIfEmptyCollection() {
		shouldThrowIfNullOrEmptyCollection(Collections.emptyList());
	}

	@Test
	void shouldThrowIfNullCollection() {
		shouldThrowIfNullOrEmptyCollection(null);
	}

	private void shouldThrowIfNullOrEmptyCollection(Collection<Message<Object>> messages) {
		MessageHeaders headers = getMessageHeaders();
		Message<Object> message11 = mock(Message.class);
		Message<Object> message12 = mock(Message.class);

		Message<Object> message21 = mock(Message.class);
		Message<Object> message22 = mock(Message.class);

		Message<Object> message31 = mock(Message.class);
		Message<Object> message32 = mock(Message.class);

		List<Message<Object>> firstBatch = Arrays.asList(message11, message21, message31);
		List<Message<Object>> secondBatch = Arrays.asList(message12, message22, message32);

		CompletableFuture<Collection<Message<Object>>> messageFuture1 = CompletableFuture.completedFuture(secondBatch);

		AsyncMessageInterceptor<Object> interceptor1 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor2 = mock(AsyncMessageInterceptor.class);

		when(interceptor1.intercept(firstBatch)).thenReturn(messageFuture1);
		when(interceptor2.intercept(secondBatch)).thenReturn(CompletableFuture.completedFuture(messages));
		when(message11.getHeaders()).thenReturn(headers);
		when(message21.getHeaders()).thenReturn(headers);
		when(message31.getHeaders()).thenReturn(headers);
		when(message12.getHeaders()).thenReturn(headers);
		when(message22.getHeaders()).thenReturn(headers);
		when(message32.getHeaders()).thenReturn(headers);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new BeforeProcessingInterceptorExecutionStage<>(
				createConfiguration(Arrays.asList(interceptor1, interceptor2)));
		CompletableFuture<Collection<Message<Object>>> result = stage.process(firstBatch, context);
		assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(IllegalArgumentException.class);

		InOrder inOrder = inOrder(interceptor1, interceptor2);
		inOrder.verify(interceptor1).intercept(firstBatch);
		inOrder.verify(interceptor2).intercept(secondBatch);
	}

	@Test
	void shouldPropagateError() {
		MessageHeaders headers = getMessageHeaders();
		Message<Object> message1 = mock(Message.class);

		AsyncMessageInterceptor<Object> interceptor1 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor2 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor3 = mock(AsyncMessageInterceptor.class);

		RuntimeException exception = new RuntimeException("Expected error");
		when(interceptor1.intercept(message1)).thenThrow(exception);
		when(message1.getHeaders()).thenReturn(headers);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new BeforeProcessingInterceptorExecutionStage<>(
				createConfiguration(Arrays.asList(interceptor1, interceptor2, interceptor3)));
		CompletableFuture<Message<Object>> future = stage.process(message1, context);
		assertThat(future).isCompletedExceptionally();
		assertThatThrownBy(future::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isEqualTo(exception);

		verify(interceptor1).intercept(message1);
		verify(interceptor2, never()).intercept(any(Message.class));
		verify(interceptor3, never()).intercept(any(Message.class));

	}

	@Test
	void shouldPropagateErrorBatch() {
		MessageHeaders headers = getMessageHeaders();
		Message<Object> message11 = mock(Message.class);
		Message<Object> message12 = mock(Message.class);
		Message<Object> message13 = mock(Message.class);

		List<Message<Object>> firstBatch = Arrays.asList(message11, message12, message13);

		AsyncMessageInterceptor<Object> interceptor1 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor2 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor3 = mock(AsyncMessageInterceptor.class);

		RuntimeException exception = new RuntimeException("Expected error");
		when(interceptor1.intercept(firstBatch)).thenThrow(exception);
		when(message11.getHeaders()).thenReturn(headers);
		when(message12.getHeaders()).thenReturn(headers);
		when(message13.getHeaders()).thenReturn(headers);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new BeforeProcessingInterceptorExecutionStage<>(
				createConfiguration(Arrays.asList(interceptor1, interceptor2, interceptor3)));
		CompletableFuture<Collection<Message<Object>>> future = stage.process(firstBatch, context);
		assertThat(future).isCompletedExceptionally();
		assertThatThrownBy(future::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isEqualTo(exception);

		verify(interceptor1).intercept(firstBatch);
		verify(interceptor2, never()).intercept(any(Message.class));
		verify(interceptor3, never()).intercept(any(Message.class));

	}

	@Test
	void shouldPassMessageForEmptyInterceptors() {
		MessageHeaders headers = getMessageHeaders();
		Message<Object> message1 = mock(Message.class);
		when(message1.getHeaders()).thenReturn(headers);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new BeforeProcessingInterceptorExecutionStage<>(
				createEmptyConfiguration());
		CompletableFuture<Message<Object>> future = stage.process(message1, context);
		assertThat(future.join()).isEqualTo(message1);

	}

	@Test
	void shouldPassMessageForEmptyInterceptorsBatch() {
		MessageHeaders headers = getMessageHeaders();
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		when(message1.getHeaders()).thenReturn(headers);
		when(message2.getHeaders()).thenReturn(headers);
		when(message3.getHeaders()).thenReturn(headers);

		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new BeforeProcessingInterceptorExecutionStage<>(
				createEmptyConfiguration());
		CompletableFuture<Collection<Message<Object>>> future = stage.process(batch, context);
		assertThat(future.join()).isEqualTo(batch);

	}

	// @formatter:off
	private MessageProcessingConfiguration<Object> createConfiguration(Collection<AsyncMessageInterceptor<Object>> interceptors) {
		return MessageProcessingConfiguration
			.builder()
			.ackHandler(mock(AcknowledgementHandler.class))
			.errorHandler(mock(AsyncErrorHandler.class))
			.messageListener(mock(AsyncMessageListener.class))
			.interceptors(interceptors)
			.build();
	}

	@SuppressWarnings("unchecked")
	private MessageProcessingConfiguration<Object> createEmptyConfiguration() {
		return MessageProcessingConfiguration
			.builder()
			.ackHandler(mock(AcknowledgementHandler.class))
			.errorHandler(mock(AsyncErrorHandler.class))
			.messageListener(mock(AsyncMessageListener.class))
			.interceptors(Collections.emptyList())
			.build();
	// @formatter:on
	}

}
