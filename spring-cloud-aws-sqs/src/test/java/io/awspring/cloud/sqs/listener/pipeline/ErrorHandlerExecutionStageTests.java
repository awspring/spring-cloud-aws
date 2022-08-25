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
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementHandler;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class ErrorHandlerExecutionStageTests {

	@Test
	void shouldRecoverFromError() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message = mock(Message.class);
		given(message.getHeaders()).willReturn(headers);

		AsyncErrorHandler<Object> errorHandler = mock(AsyncErrorHandler.class);
		RuntimeException cause = new RuntimeException("Expected exception from shouldRecoverFromError");
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException(
				"Expected ListenerExecutionFailedException from shouldRecoverFromError", cause, message);
		CompletableFuture<Message<Object>> failedFuture = CompletableFutures.failedFuture(listenerException);

		given(errorHandler.handle(eq(message), any(Throwable.class)))
				.willReturn(CompletableFuture.completedFuture(null));

		MessageProcessingConfiguration<Object> configuration = MessageProcessingConfiguration.builder()
				.messageListener(mock(AsyncMessageListener.class)).ackHandler(mock(AcknowledgementHandler.class))
				.interceptors(Collections.emptyList()).errorHandler(errorHandler).build();
		MessageProcessingPipeline<Object> stage = new ErrorHandlerExecutionStage<>(configuration);
		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		CompletableFuture<Message<Object>> future = stage.process(failedFuture, context);

		assertThat(future).isCompletedWithValue(message);

	}

	@Test
	void shouldRecoverFromErrorBatch() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);

		AsyncErrorHandler<Object> errorHandler = mock(AsyncErrorHandler.class);
		RuntimeException cause = new RuntimeException("Expected exception from shouldRecoverFromErrorBatch");
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException(
				"Expected ListenerExecutionFailedException from shouldRecoverFromErrorBatch", cause, batch);
		CompletableFuture<Collection<Message<Object>>> failedFuture = CompletableFutures
				.failedFuture(listenerException);

		given(errorHandler.handle(eq(batch), any(Throwable.class))).willReturn(CompletableFuture.completedFuture(null));

		MessageProcessingConfiguration<Object> configuration = MessageProcessingConfiguration.builder()
				.messageListener(mock(AsyncMessageListener.class)).ackHandler(mock(AcknowledgementHandler.class))
				.interceptors(Collections.emptyList()).errorHandler(errorHandler).build();
		MessageProcessingPipeline<Object> stage = new ErrorHandlerExecutionStage<>(configuration);
		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		CompletableFuture<Collection<Message<Object>>> future = stage.processMany(failedFuture, context);

		assertThat(future).isCompletedWithValue(batch);

	}

	@Test
	void shouldWrapIfErrorNotListenerException() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message = mock(Message.class);
		given(message.getHeaders()).willReturn(headers);

		AsyncErrorHandler<Object> errorHandler = mock(AsyncErrorHandler.class);
		RuntimeException cause = new RuntimeException("Expected exception from shouldWrapIfErrorNotListenerException");
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException(
				"Expected ListenerExecutionFailedException from shouldWrapIfErrorNotListenerException", cause, message);
		CompletableFuture<Message<Object>> failedFuture = CompletableFutures.failedFuture(listenerException);

		RuntimeException resultException = new RuntimeException(
				"Expected result exception from shouldWrapIfErrorNotListenerException");
		given(errorHandler.handle(eq(message), any(Throwable.class)))
				.willReturn(CompletableFutures.failedFuture(resultException));

		MessageProcessingConfiguration<Object> configuration = MessageProcessingConfiguration.builder()
				.messageListener(mock(AsyncMessageListener.class)).ackHandler(mock(AcknowledgementHandler.class))
				.interceptors(Collections.emptyList()).errorHandler(errorHandler).build();
		MessageProcessingPipeline<Object> stage = new ErrorHandlerExecutionStage<>(configuration);
		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		CompletableFuture<Message<Object>> future = stage.process(failedFuture, context);

		assertThat(future).isCompletedExceptionally();
		assertThatThrownBy(future::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(ListenerExecutionFailedException.class)
				.asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessage).isEqualTo(message);

		assertThatThrownBy(future::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(ListenerExecutionFailedException.class).extracting(Throwable::getCause)
				.isInstanceOf(CompletionException.class).extracting(Throwable::getCause).isEqualTo(resultException);

	}

	@Test
	void shouldNotWrapIfErrorNotListenerException() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message = mock(Message.class);
		given(message.getHeaders()).willReturn(headers);

		AsyncErrorHandler<Object> errorHandler = mock(AsyncErrorHandler.class);
		RuntimeException cause = new RuntimeException(
				"Expected exception from shouldNotWrapIfErrorNotListenerException");
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException(
				"Expected ListenerExecutionFailedException from shouldNotWrapIfErrorNotListenerException", cause,
				message);
		CompletableFuture<Message<Object>> failedFuture = CompletableFutures.failedFuture(listenerException);

		RuntimeException resultException = new RuntimeException(
				"Expected result exception from shouldNotWrapIfErrorNotListenerException");
		given(errorHandler.handle(eq(message), any(Throwable.class)))
				.willReturn(CompletableFutures.failedFuture(resultException));

		MessageProcessingConfiguration<Object> configuration = MessageProcessingConfiguration.builder()
				.messageListener(mock(AsyncMessageListener.class)).ackHandler(mock(AcknowledgementHandler.class))
				.interceptors(Collections.emptyList()).errorHandler(errorHandler).build();
		MessageProcessingPipeline<Object> stage = new ErrorHandlerExecutionStage<>(configuration);
		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		CompletableFuture<Message<Object>> future = stage.process(failedFuture, context);

		assertThat(future).isCompletedExceptionally();
		assertThatThrownBy(future::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(ListenerExecutionFailedException.class)
				.asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessage).isEqualTo(message);

		assertThatThrownBy(future::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(ListenerExecutionFailedException.class).extracting(Throwable::getCause)
				.isInstanceOf(CompletionException.class).extracting(Throwable::getCause).isEqualTo(resultException);

	}

	@Test
	void shouldWrapIfErrorNotListenerExceptionBatch() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);

		AsyncErrorHandler<Object> errorHandler = mock(AsyncErrorHandler.class);
		RuntimeException cause = new RuntimeException(
				"Expected exception from shouldWrapIfErrorNotListenerExceptionBatch");
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException(
				"Expected ListenerExecutionFailedException from shouldWrapIfErrorNotListenerExceptionBatch", cause,
				batch);
		CompletableFuture<Collection<Message<Object>>> failedFuture = CompletableFutures
				.failedFuture(listenerException);

		RuntimeException resultException = new RuntimeException(
				"Expected result exception from shouldWrapIfErrorNotListenerExceptionBatch");
		given(errorHandler.handle(eq(batch), any(Throwable.class)))
				.willReturn(CompletableFutures.failedFuture(resultException));

		MessageProcessingConfiguration<Object> configuration = MessageProcessingConfiguration.builder()
				.messageListener(mock(AsyncMessageListener.class)).ackHandler(mock(AcknowledgementHandler.class))
				.interceptors(Collections.emptyList()).errorHandler(errorHandler).build();
		MessageProcessingPipeline<Object> stage = new ErrorHandlerExecutionStage<>(configuration);
		MessageProcessingContext<Object> context = MessageProcessingContext.create();

		CompletableFuture<Collection<Message<Object>>> future = stage.processMany(failedFuture, context);

		assertThat(future).isCompletedExceptionally();
		assertThatThrownBy(future::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(ListenerExecutionFailedException.class)
				.asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessages).isEqualTo(batch);

		assertThatThrownBy(future::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(ListenerExecutionFailedException.class).extracting(Throwable::getCause)
				.isInstanceOf(CompletionException.class).extracting(Throwable::getCause).isEqualTo(resultException);

	}

}
