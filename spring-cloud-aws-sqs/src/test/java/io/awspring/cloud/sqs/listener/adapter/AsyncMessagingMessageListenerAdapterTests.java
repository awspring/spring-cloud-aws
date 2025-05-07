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
package io.awspring.cloud.sqs.listener.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * Tests for {@link AsyncMessagingMessageListenerAdapter}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings("unchecked")
class AsyncMessagingMessageListenerAdapterTests {

	@Test
	void shouldInvokeMessage() throws Exception {
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message = mock(Message.class);
		CompletableFuture<Void> expectedResult = CompletableFuture.completedFuture(null);
		given(handlerMethod.invoke(message)).willReturn(expectedResult);
		AsyncMessageListener<Object> adapter = new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
		CompletableFuture<Void> result = adapter.onMessage(message);
		verify(handlerMethod).invoke(message);
		assertThat(result).isNotCompletedExceptionally();
	}

	@Test
	void shouldReturnFailedFutureOnThrownException() throws Exception {
		MessageHeaders headers = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message = mock(Message.class);
		RuntimeException exception = new RuntimeException(
				"Expected exception from shouldReturnFailedFutureOnThrownException");
		given(message.getHeaders()).willReturn(headers);
		given(handlerMethod.invoke(message)).willThrow(exception);
		AsyncMessageListener<Object> adapter = new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
		CompletableFuture<Void> result = adapter.onMessage(message);
		assertThat(result).isCompletedExceptionally();
		assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(ListenerExecutionFailedException.class)
				.asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessage).isEqualTo(message);
	}

	@Test
	void shouldReturnFailedFutureOnThrownError() throws Exception {
		MessageHeaders headers = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message = mock(Message.class);
		Error error = new Error(
			"Expected exception from shouldReturnFailedFutureOnThrownError");
		given(message.getHeaders()).willReturn(headers);
		given(handlerMethod.invoke(message)).willThrow(error);
		AsyncMessageListener<Object> adapter = new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
		CompletableFuture<Void> result = adapter.onMessage(message);
		assertThat(result).isCompletedExceptionally();
		assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
			.isInstanceOf(ListenerExecutionFailedException.class)
			.asInstanceOf(type(ListenerExecutionFailedException.class))
			.extracting(ListenerExecutionFailedException::getFailedMessage).isEqualTo(message);
	}

	@Test
	void shouldWrapCompletionException() throws Exception {
		MessageHeaders headers = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message = mock(Message.class);
		RuntimeException exception = new RuntimeException("Expected exception from shouldWrapCompletionException");
		given(message.getHeaders()).willReturn(headers);
		given(handlerMethod.invoke(message)).willReturn(CompletableFutures.failedFuture(exception));
		AsyncMessageListener<Object> adapter = new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
		CompletableFuture<Void> result = adapter.onMessage(message);
		assertThat(result).isCompletedExceptionally();
		assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(ListenerExecutionFailedException.class)
				.asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessage).isEqualTo(message);
	}

	@Test
	void shouldWrapCompletionError() throws Exception {
		MessageHeaders headers = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message = mock(Message.class);
		Error error = new Error("Expected exception from shouldWrapCompletionError");
		given(message.getHeaders()).willReturn(headers);
		given(handlerMethod.invoke(message)).willReturn(CompletableFutures.failedFuture(error));
		AsyncMessageListener<Object> adapter = new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
		CompletableFuture<Void> result = adapter.onMessage(message);
		assertThat(result).isCompletedExceptionally();
		assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
			.isInstanceOf(ListenerExecutionFailedException.class)
			.asInstanceOf(type(ListenerExecutionFailedException.class))
			.extracting(ListenerExecutionFailedException::getFailedMessage).isEqualTo(message);
	}

	@Test
	void shouldHandleClassCastException() throws Exception {
		MessageHeaders headers = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message = mock(Message.class);
		given(message.getHeaders()).willReturn(headers);
		Object invocationResult = new Object();
		given(handlerMethod.invoke(message)).willReturn(invocationResult);
		AsyncMessageListener<Object> adapter = new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
		CompletableFuture<Void> result = adapter.onMessage(message);
		assertThat(result).isCompletedExceptionally();
		assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(IllegalArgumentException.class).extracting(Throwable::getCause)
				.isInstanceOf(ClassCastException.class);
	}

	@Test
	void shouldInvokeMessageBatch() throws Exception {
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message1, message2, message3);
		CompletableFuture<Void> expectedResult = CompletableFuture.completedFuture(null);
		given(handlerMethod.invoke(any(Message.class))).willReturn(expectedResult);
		AsyncMessageListener<Object> adapter = new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
		CompletableFuture<Void> result = adapter.onMessage(messages);
		ArgumentCaptor<Message<Object>> captor = ArgumentCaptor.forClass(Message.class);
		verify(handlerMethod).invoke(captor.capture());
		assertThat(captor.getValue().getPayload()).asInstanceOf(list(Message.class))
				.containsExactlyElementsOf(messages);
		assertThat(result).isNotCompletedExceptionally();
	}

	@Test
	void shouldReturnFailedFutureOnExceptionBatch() throws Exception {
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		RuntimeException exception = new RuntimeException(
				"Expected exception from shouldReturnFailedFutureOnExceptionBatch");
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(handlerMethod.invoke(any(Message.class))).willThrow(exception);
		AsyncMessageListener<Object> adapter = new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
		CompletableFuture<Void> result = adapter.onMessage(messages);
		assertThat(result).isCompletedExceptionally();
		assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(ListenerExecutionFailedException.class)
				.asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessages).isEqualTo(messages);
	}

	@Test
	void shouldReturnFailedFutureOnErrorBatch() throws Exception {
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Error error = new Error(
			"Expected exception from shouldReturnFailedFutureOnErrorBatch");
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(handlerMethod.invoke(any(Message.class))).willThrow(error);
		AsyncMessageListener<Object> adapter = new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
		CompletableFuture<Void> result = adapter.onMessage(messages);
		assertThat(result).isCompletedExceptionally();
		assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
			.isInstanceOf(ListenerExecutionFailedException.class)
			.asInstanceOf(type(ListenerExecutionFailedException.class))
			.extracting(ListenerExecutionFailedException::getFailedMessages).isEqualTo(messages);
	}

	@Test
	void shouldWrapCompletionExceptionBatch() throws Exception {
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		RuntimeException exception = new RuntimeException("Expected exception from shouldWrapCompletionExceptionBatch");
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(handlerMethod.invoke(any(Message.class))).willReturn(CompletableFutures.failedFuture(exception));
		AsyncMessageListener<Object> adapter = new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
		CompletableFuture<Void> result = adapter.onMessage(messages);
		assertThat(result).isCompletedExceptionally();
		assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(ListenerExecutionFailedException.class)
				.asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessages).isEqualTo(messages);
	}

	@Test
	void shouldWrapCompletionErrorBatch() throws Exception {
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Error error = new Error("Expected exception from shouldWrapCompletionErrorBatch");
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(handlerMethod.invoke(any(Message.class))).willReturn(CompletableFutures.failedFuture(error));
		AsyncMessageListener<Object> adapter = new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
		CompletableFuture<Void> result = adapter.onMessage(messages);
		assertThat(result).isCompletedExceptionally();
		assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
			.isInstanceOf(ListenerExecutionFailedException.class)
			.asInstanceOf(type(ListenerExecutionFailedException.class))
			.extracting(ListenerExecutionFailedException::getFailedMessages).isEqualTo(messages);
	}

	@Test
	void shouldHandleClassCastExceptionBatch() throws Exception {
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		Object invocationResult = new Object();
		given(handlerMethod.invoke(any(Message.class))).willReturn(invocationResult);
		AsyncMessageListener<Object> adapter = new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
		CompletableFuture<Void> result = adapter.onMessage(messages);
		assertThat(result).isCompletedExceptionally();
		assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(IllegalArgumentException.class).extracting(Throwable::getCause)
				.isInstanceOf(ClassCastException.class);
	}

}
