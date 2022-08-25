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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException;
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
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class AfterProcessingInterceptorExecutionStageTests {

	@Test
	void shouldExecuteInterceptorsInOrder() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message = mock(Message.class);

		AsyncMessageInterceptor<Object> interceptor1 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor2 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor3 = mock(AsyncMessageInterceptor.class);

		when(interceptor1.afterProcessing(message, null)).thenReturn(CompletableFuture.completedFuture(null));
		when(interceptor2.afterProcessing(message, null)).thenReturn(CompletableFuture.completedFuture(null));
		when(interceptor3.afterProcessing(message, null)).thenReturn(CompletableFuture.completedFuture(null));
		when(message.getHeaders()).thenReturn(headers);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new AfterProcessingInterceptorExecutionStage<>(
				createConfiguration(interceptor1, interceptor2, interceptor3));
		CompletableFuture<Message<Object>> future = stage.process(CompletableFuture.completedFuture(message), context);
		assertThat(future.join()).isEqualTo(message);

		InOrder inOrder = inOrder(interceptor1, interceptor2, interceptor3);
		inOrder.verify(interceptor1).afterProcessing(message, null);
		inOrder.verify(interceptor2).afterProcessing(message, null);
		inOrder.verify(interceptor3).afterProcessing(message, null);

	}

	@Test
	void shouldExecuteInterceptorsInOrderBatch() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);

		AsyncMessageInterceptor<Object> interceptor1 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor2 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor3 = mock(AsyncMessageInterceptor.class);

		when(interceptor1.afterProcessing(batch, null)).thenReturn(CompletableFuture.completedFuture(null));
		when(interceptor2.afterProcessing(batch, null)).thenReturn(CompletableFuture.completedFuture(null));
		when(interceptor3.afterProcessing(batch, null)).thenReturn(CompletableFuture.completedFuture(null));
		when(message1.getHeaders()).thenReturn(headers);
		when(message2.getHeaders()).thenReturn(headers);
		when(message3.getHeaders()).thenReturn(headers);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new AfterProcessingInterceptorExecutionStage<>(
				createConfiguration(interceptor1, interceptor2, interceptor3));
		CompletableFuture<Collection<Message<Object>>> future = stage
				.processMany(CompletableFuture.completedFuture(batch), context);
		assertThat(future.join()).isEqualTo(batch);

		InOrder inOrder = inOrder(interceptor1, interceptor2, interceptor3);
		inOrder.verify(interceptor1).afterProcessing(batch, null);
		inOrder.verify(interceptor2).afterProcessing(batch, null);
		inOrder.verify(interceptor3).afterProcessing(batch, null);

	}

	@Test
	void shouldPassErrorToInterceptors() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message = mock(Message.class);

		AsyncMessageInterceptor<Object> interceptor1 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor2 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor3 = mock(AsyncMessageInterceptor.class);

		RuntimeException exception = new RuntimeException(
				"Expected error from AfterProcessingInterceptorExecutionStageTests#shouldPassErrorToInterceptors");
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException(
				"Expected ListenerExecutionFailedException from AfterProcessingInterceptorExecutionStageTests#shouldPassErrorToInterceptors",
				exception, message);
		CompletableFuture<Message<Object>> failedFuture = CompletableFutures.failedFuture(listenerException);

		when(interceptor1.afterProcessing(message, listenerException))
				.thenReturn(CompletableFuture.completedFuture(null));
		when(interceptor2.afterProcessing(message, listenerException))
				.thenReturn(CompletableFuture.completedFuture(null));
		when(interceptor3.afterProcessing(message, listenerException))
				.thenReturn(CompletableFuture.completedFuture(null));
		when(message.getHeaders()).thenReturn(headers);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new AfterProcessingInterceptorExecutionStage<>(
				createConfiguration(interceptor1, interceptor2, interceptor3));
		CompletableFuture<Message<Object>> future = stage.process(failedFuture, context);
		assertThat(future).isCompletedExceptionally();
		assertThatThrownBy(future::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isEqualTo(listenerException);

		verify(interceptor1).afterProcessing(message, listenerException);
		verify(interceptor2).afterProcessing(message, listenerException);
		verify(interceptor3).afterProcessing(message, listenerException);

	}

	@Test
	void shouldPassErrorToInterceptorsBatch() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);

		AsyncMessageInterceptor<Object> interceptor1 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor2 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor3 = mock(AsyncMessageInterceptor.class);

		RuntimeException exception = new RuntimeException(
				"Expected exception from AfterProcessingInterceptorExecutionStageTests#shouldPassErrorToInterceptorsBatch");
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException(
				"Expected ListenerExecutionFailedException from AfterProcessingInterceptorExecutionStageTests#shouldPassErrorToInterceptorsBatch",
				exception, batch);
		CompletableFuture<Collection<Message<Object>>> failedFuture = CompletableFutures
				.failedFuture(listenerException);

		when(interceptor1.afterProcessing(batch, listenerException))
				.thenReturn(CompletableFuture.completedFuture(null));
		when(interceptor2.afterProcessing(batch, listenerException))
				.thenReturn(CompletableFuture.completedFuture(null));
		when(interceptor3.afterProcessing(batch, listenerException))
				.thenReturn(CompletableFuture.completedFuture(null));
		when(message1.getHeaders()).thenReturn(headers);
		when(message2.getHeaders()).thenReturn(headers);
		when(message3.getHeaders()).thenReturn(headers);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new AfterProcessingInterceptorExecutionStage<>(
				createConfiguration(interceptor1, interceptor2, interceptor3));
		CompletableFuture<Collection<Message<Object>>> future = stage.processMany(failedFuture, context);
		assertThat(future).isCompletedExceptionally();
		assertThatThrownBy(future::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isEqualTo(listenerException);

		verify(interceptor1).afterProcessing(batch, listenerException);
		verify(interceptor2).afterProcessing(batch, listenerException);
		verify(interceptor3).afterProcessing(batch, listenerException);

	}

	@Test
	void shouldForwardMessageForEmptyInterceptors() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message = mock(Message.class);
		when(message.getHeaders()).thenReturn(headers);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new AfterProcessingInterceptorExecutionStage<>(
				createEmptyConfiguration());
		CompletableFuture<Message<Object>> future = stage.process(CompletableFuture.completedFuture(message), context);
		assertThat(future.join()).isEqualTo(message);

	}

	@Test
	void shouldForwardMessageForEmptyInterceptorsBatch() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		when(message1.getHeaders()).thenReturn(headers);
		when(message2.getHeaders()).thenReturn(headers);
		when(message3.getHeaders()).thenReturn(headers);

		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);

		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		MessageProcessingPipeline<Object> stage = new AfterProcessingInterceptorExecutionStage<>(
				createEmptyConfiguration());
		CompletableFuture<Collection<Message<Object>>> future = stage
				.processMany(CompletableFuture.completedFuture(batch), context);
		assertThat(future.join()).isEqualTo(batch);

	}

	private MessageProcessingConfiguration<Object> createConfiguration(AsyncMessageInterceptor<Object> interceptor1,
			AsyncMessageInterceptor<Object> interceptor2, AsyncMessageInterceptor<Object> interceptor3) {
		return MessageProcessingConfiguration.builder().ackHandler(mock(AcknowledgementHandler.class))
				.errorHandler(mock(AsyncErrorHandler.class)).messageListener(mock(AsyncMessageListener.class))
				.interceptors(Arrays.asList(interceptor1, interceptor2, interceptor3)).build();
	}

	@SuppressWarnings("unchecked")
	private MessageProcessingConfiguration<Object> createEmptyConfiguration() {
		return MessageProcessingConfiguration.builder().ackHandler(mock(AcknowledgementHandler.class))
				.errorHandler(mock(AsyncErrorHandler.class)).messageListener(mock(AsyncMessageListener.class))
				.interceptors(Collections.emptyList()).build();
	}

}
