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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
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

/**
 * Tests for {@link AcknowledgementHandlerExecutionStage}.
 *
 * @author Tomaz Fernandes
 */
class AcknowledgementHandlerExecutionStageTests {

	@SuppressWarnings("unchecked")
	@Test
	void shouldAckOnSuccess() {
		AcknowledgementHandler<Object> handler = mock(AcknowledgementHandler.class);
		MessageProcessingConfiguration<Object> configuration = createConfiguration(handler);
		Message<Object> message = mock(Message.class);
		CompletableFuture<Message<Object>> future = CompletableFuture.completedFuture(message);
		AcknowledgementCallback<Object> callback = mock(AcknowledgementCallback.class);
		MessageProcessingContext<Object> context = mock(MessageProcessingContext.class);
		when(context.getAcknowledgmentCallback()).thenReturn(callback);
		when(handler.onSuccess(message, callback)).thenReturn(CompletableFuture.completedFuture(null));

		AcknowledgementHandlerExecutionStage<Object> stage = new AcknowledgementHandlerExecutionStage<>(configuration);
		CompletableFuture<Message<Object>> resultFuture = stage.process(future, context);

		verify(handler).onSuccess(message, callback);
		assertThat(resultFuture).isCompletedWithValue(message);

	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldAckOnError() {
		AcknowledgementHandler<Object> handler = mock(AcknowledgementHandler.class);
		MessageProcessingConfiguration<Object> configuration = createConfiguration(handler);
		Message<Object> message = mock(Message.class);
		RuntimeException exception = new ListenerExecutionFailedException("Expected error", new RuntimeException(),
				message);
		CompletableFuture<Message<Object>> failedFuture = CompletableFutures.failedFuture(exception);
		AcknowledgementCallback<Object> callback = mock(AcknowledgementCallback.class);
		MessageProcessingContext<Object> context = mock(MessageProcessingContext.class);
		when(context.getAcknowledgmentCallback()).thenReturn(callback);
		when(handler.onError(message, exception, callback)).thenReturn(CompletableFuture.completedFuture(null));

		AcknowledgementHandlerExecutionStage<Object> stage = new AcknowledgementHandlerExecutionStage<>(configuration);
		CompletableFuture<Message<Object>> resultFuture = stage.process(failedFuture, context);

		verify(handler).onError(message, exception, callback);
		assertThat(resultFuture).isCompletedExceptionally();
		assertThatThrownBy(resultFuture::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isEqualTo(exception).asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessage).isEqualTo(message);

	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldAckOnSuccessBatch() {
		AcknowledgementHandler<Object> handler = mock(AcknowledgementHandler.class);
		MessageProcessingConfiguration<Object> configuration = createConfiguration(handler);
		Message<Object> message = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message, message, message);
		CompletableFuture<Collection<Message<Object>>> messagesFuture = CompletableFuture.completedFuture(messages);
		AcknowledgementCallback<Object> callback = mock(AcknowledgementCallback.class);
		MessageProcessingContext<Object> context = mock(MessageProcessingContext.class);
		when(context.getAcknowledgmentCallback()).thenReturn(callback);
		when(handler.onSuccess(messages, callback)).thenReturn(CompletableFuture.completedFuture(null));

		AcknowledgementHandlerExecutionStage<Object> stage = new AcknowledgementHandlerExecutionStage<>(configuration);
		CompletableFuture<Collection<Message<Object>>> resultFuture = stage.processMany(messagesFuture, context);

		verify(handler).onSuccess(messages, callback);
		assertThat(resultFuture).isCompletedWithValue(messages);

	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldAckOnErrorBatch() {
		AcknowledgementHandler<Object> handler = mock(AcknowledgementHandler.class);
		MessageProcessingConfiguration<Object> configuration = createConfiguration(handler);
		Message<Object> message = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message, message, message);
		RuntimeException exception = new ListenerExecutionFailedException("Expected error", new RuntimeException(),
				messages);
		CompletableFuture<Collection<Message<Object>>> failedFuture = CompletableFutures.failedFuture(exception);
		AcknowledgementCallback<Object> callback = mock(AcknowledgementCallback.class);
		MessageProcessingContext<Object> context = mock(MessageProcessingContext.class);
		when(context.getAcknowledgmentCallback()).thenReturn(callback);
		when(handler.onError(messages, exception, callback)).thenReturn(CompletableFuture.completedFuture(null));

		AcknowledgementHandlerExecutionStage<Object> stage = new AcknowledgementHandlerExecutionStage<>(configuration);
		CompletableFuture<Collection<Message<Object>>> resultFuture = stage.processMany(failedFuture, context);

		verify(handler).onError(messages, exception, callback);
		assertThat(resultFuture).isCompletedExceptionally();
		assertThatThrownBy(resultFuture::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isEqualTo(exception).asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessages).isEqualTo(messages);

	}

	@SuppressWarnings("unchecked")
	private MessageProcessingConfiguration<Object> createConfiguration(AcknowledgementHandler<Object> handler) {
		return MessageProcessingConfiguration.builder().ackHandler(handler).errorHandler(mock(AsyncErrorHandler.class))
				.messageListener(mock(AsyncMessageListener.class)).interceptors(Collections.emptyList()).build();
	}

}
