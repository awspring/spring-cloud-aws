/*
 * Copyright 2013-2025 the original author or authors.
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * Tests for {@link CompositeInvocableHandler}.
 *
 * @author José Iêdo
 */
@SuppressWarnings("unchecked")
class CompositeInvocableHandlerTest {

	static class Listener {
		public String handle(String payload) {
			return "Handled: " + payload;
		}

		public String handle(Integer payload) {
			return "Handled Integer: " + payload;
		}

		public String handle(Object payload) {
			return "Handled object: " + payload;
		}
	}

	@Test
	void invokesHandlerMatchingPayloadType() throws Exception {
		Message<String> message = mock(Message.class);
		when(message.getPayload()).thenReturn("testPayload");

		InvocableHandlerMethod stringHandler = mockHandler(String.class, "handlerResult");
		InvocableHandlerMethod intHandler = mockHandler(Integer.class, "shouldNotBeCalled");

		CompositeInvocableHandler handler = new CompositeInvocableHandler(List.of(stringHandler, intHandler), null);

		assertThat(handler.invoke(message)).isEqualTo("handlerResult");
		verify(stringHandler).invoke(message);
		verify(intHandler, never()).invoke(message);
	}

	@Test
	void throwsIfNoHandlerAndNoDefault() {
		Message<Integer> message = mock(Message.class);
		when(message.getPayload()).thenReturn(42);

		CompositeInvocableHandler handler = new CompositeInvocableHandler(List.of(), null);

		assertThatThrownBy(() -> handler.invoke(message)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("No handler found");
	}

	@Test
	void usesDefaultHandlerWhenNoSpecificMatch() throws Exception {
		Message<Long> message = mock(Message.class);
		when(message.getPayload()).thenReturn(1L);

		InvocableHandlerMethod stringHandler = mockHandler(String.class, "shouldNotBeCalled");
		InvocableHandlerMethod defaultHandler = mockHandler(Integer.class, "defaultHandlerResult");

		CompositeInvocableHandler handler = new CompositeInvocableHandler(List.of(stringHandler), defaultHandler);

		assertThat(handler.invoke(message)).isEqualTo("defaultHandlerResult");
		verify(stringHandler, never()).invoke(message);
		verify(defaultHandler).invoke(message);
	}

	@Test
	void throwsIfMultipleHandlersMatch() throws Exception {
		Message<String> message = mock(Message.class);
		when(message.getPayload()).thenReturn("duplicatePayload");

		InvocableHandlerMethod handler1 = mockHandler(String.class, "result1");
		InvocableHandlerMethod handler2 = mockHandler(String.class, "result2");

		var handlerGroup = new CompositeInvocableHandler(List.of(handler1, handler2), null);

		assertThatThrownBy(() -> handlerGroup.invoke(message)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Ambiguous handler method for payload type");
	}

	private InvocableHandlerMethod mockHandler(Class<?> paramType, Object returnValue) throws Exception {
		Method method = Listener.class.getDeclaredMethod("handle", paramType);
		InvocableHandlerMethod handler = mock(InvocableHandlerMethod.class);
		when(handler.getMethod()).thenReturn(method);
		when(handler.invoke(any())).thenReturn(returnValue);
		return handler;
	}
}
