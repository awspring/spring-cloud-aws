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
import static org.mockito.Mockito.when;

import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * Tests for {@link AbstractMethodInvokingListenerAdapter}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings("unchecked")
class AbstractMethodInvokingListenerAdapterTests {

	@Test
	void shouldInvokeHandler() throws Exception {
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message = mock(Message.class);
		Object expectedResult = new Object();
		when(handlerMethod.invoke(message)).thenReturn(expectedResult);
		AbstractMethodInvokingListenerAdapter<Object> adapter = new AbstractMethodInvokingListenerAdapter<Object>(
				handlerMethod) {
		};
		Object actualResult = adapter.invokeHandler(message);
		assertThat(actualResult).isEqualTo(expectedResult);
	}

	@Test
	void shouldWrapException() throws Exception {
		MessageHeaders headers = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message = mock(Message.class);
		given(message.getHeaders()).willReturn(headers);
		RuntimeException exception = new RuntimeException("Expected exception from shouldWrapException");
		when(handlerMethod.invoke(message)).thenThrow(exception);
		AbstractMethodInvokingListenerAdapter<Object> adapter = new AbstractMethodInvokingListenerAdapter<Object>(
				handlerMethod) {
		};
		assertThatThrownBy(() -> adapter.invokeHandler(message)).isInstanceOf(ListenerExecutionFailedException.class)
				.asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessage).isEqualTo(message);
	}

	@Test
	void shouldWrapError() throws Exception {
		MessageHeaders headers = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message = mock(Message.class);
		given(message.getHeaders()).willReturn(headers);
		Error error = new Error("Expected exception from shouldWrapError");
		when(handlerMethod.invoke(message)).thenThrow(error);
		AbstractMethodInvokingListenerAdapter<Object> adapter = new AbstractMethodInvokingListenerAdapter<Object>(
				handlerMethod) {
		};
		assertThatThrownBy(() -> adapter.invokeHandler(message)).isInstanceOf(ListenerExecutionFailedException.class)
				.asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessage).isEqualTo(message);
	}

	@Test
	void shouldInvokeHandlerBatch() throws Exception {
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message1, message2, message3);
		Object expectedResult = new Object();

		when(handlerMethod.invoke(any(Message.class))).thenReturn(expectedResult);
		AbstractMethodInvokingListenerAdapter<Object> adapter = new AbstractMethodInvokingListenerAdapter<Object>(
				handlerMethod) {
		};
		Object actualResult = adapter.invokeHandler(messages);
		ArgumentCaptor<Message<Object>> messageCaptor = ArgumentCaptor.forClass(Message.class);
		verify(handlerMethod).invoke(messageCaptor.capture());
		Message<Object> messageValue = messageCaptor.getValue();
		assertThat(messageValue).extracting(Message::getPayload).asInstanceOf(list(Message.class))
				.containsExactlyElementsOf(messages);
		assertThat(actualResult).isEqualTo(expectedResult);
	}

	@Test
	void shouldWrapExceptionBatch() throws Exception {
		MessageHeaders messageHeaders = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		given(message1.getHeaders()).willReturn(messageHeaders);
		given(message2.getHeaders()).willReturn(messageHeaders);
		given(message3.getHeaders()).willReturn(messageHeaders);
		RuntimeException exception = new RuntimeException("Expected exception from shouldWrapExceptionBatch");
		when(handlerMethod.invoke(any(Message.class))).thenThrow(exception);
		AbstractMethodInvokingListenerAdapter<Object> adapter = new AbstractMethodInvokingListenerAdapter<Object>(
				handlerMethod) {
		};
		assertThatThrownBy(() -> adapter.invokeHandler(batch)).isInstanceOf(ListenerExecutionFailedException.class)
				.asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessages).isEqualTo(batch);
	}

	@Test
	void shouldWrapErrorBatch() throws Exception {
		MessageHeaders messageHeaders = new MessageHeaders(null);
		InvocableHandlerMethod handlerMethod = mock(InvocableHandlerMethod.class);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		given(message1.getHeaders()).willReturn(messageHeaders);
		given(message2.getHeaders()).willReturn(messageHeaders);
		given(message3.getHeaders()).willReturn(messageHeaders);
		Error error = new Error("Expected exception from shouldWrapErrorBatch");
		when(handlerMethod.invoke(any(Message.class))).thenThrow(error);
		AbstractMethodInvokingListenerAdapter<Object> adapter = new AbstractMethodInvokingListenerAdapter<Object>(
				handlerMethod) {
		};
		assertThatThrownBy(() -> adapter.invokeHandler(batch)).isInstanceOf(ListenerExecutionFailedException.class)
				.asInstanceOf(type(ListenerExecutionFailedException.class))
				.extracting(ListenerExecutionFailedException::getFailedMessages).isEqualTo(batch);
	}

}
