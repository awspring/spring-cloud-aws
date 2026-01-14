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
package io.awspring.cloud.sqs.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sqs.listener.AbstractMessageListenerContainer;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.support.converter.AbstractMessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * Tests for {@link AbstractEndpoint} focusing on payload type mapper configuration.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings("rawtypes")
class AbstractEndpointTest {

	private SqsEndpoint endpoint;

	private AbstractMessageListenerContainer<?, ?, ?> container;

	private ContainerOptions<?, ?> containerOptions;

	private AbstractMessagingMessageConverter<?> converter;

	private MethodPayloadTypeInferrer inferrer;

	private MessageHandlerMethodFactory handlerMethodFactory;

	private InvocableHandlerMethod invocableHandlerMethod;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setup() {
		endpoint = SqsEndpoint.builder().queueNames(Collections.singletonList("test-queue")).id("test-id").build();

		container = (AbstractMessageListenerContainer<?, ?, ?>) mock(AbstractMessageListenerContainer.class);
		containerOptions = (ContainerOptions<?, ?>) mock(ContainerOptions.class);
		converter = (AbstractMessagingMessageConverter<?>) mock(AbstractMessagingMessageConverter.class);
		inferrer = mock(MethodPayloadTypeInferrer.class);
		handlerMethodFactory = mock(MessageHandlerMethodFactory.class);
		invocableHandlerMethod = mock(InvocableHandlerMethod.class);

		when(container.getContainerOptions()).thenReturn((ContainerOptions) containerOptions);
		when(containerOptions.getMessageConverter()).thenReturn((MessagingMessageConverter) converter);
		when(handlerMethodFactory.createInvocableHandlerMethod(any(), any(Method.class)))
				.thenReturn(invocableHandlerMethod);

		// Mock the return type to be a non-CompletionStage
		MethodParameter returnType = mock(MethodParameter.class);
		when(invocableHandlerMethod.getReturnType()).thenReturn(returnType);
		when(returnType.getParameterType()).thenReturn((Class) String.class);

		endpoint.setBean(new TestListener());
		endpoint.setHandlerMethodFactory(handlerMethodFactory);
	}

	@Test
	void shouldNotConfigureMapperWhenInferrerIsNull() throws Exception {
		Method method = TestListener.class.getMethod("handleMessage", String.class);
		endpoint.setMethod(method);
		endpoint.setMethodPayloadTypeInferrer(null);

		endpoint.setupContainer(container);

		verify(converter, never()).setPayloadTypeMapper(any());
	}

	@Test
	void shouldNotConfigureMapperWhenInferredTypeIsNull() throws Exception {
		Method method = TestListener.class.getMethod("handleMessage", String.class);
		endpoint.setMethod(method);
		endpoint.setMethodPayloadTypeInferrer(inferrer);

		when(inferrer.inferPayloadType(any(Method.class), any())).thenReturn(null);

		endpoint.setupContainer(container);

		verify(converter, never()).setPayloadTypeMapper(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldConfigureMapperWhenUsingDefaultMapper() throws Exception {
		Method method = TestListener.class.getMethod("handleMessage", String.class);
		endpoint.setMethod(method);
		endpoint.setMethodPayloadTypeInferrer(inferrer);

		when(inferrer.inferPayloadType(any(Method.class), any())).thenReturn((Class) String.class);
		when(converter.isUsingDefaultPayloadTypeMapper()).thenReturn(true);

		endpoint.setupContainer(container);

		verify(converter).setPayloadTypeMapper(any(Function.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldNotOverrideCustomMapper() throws Exception {
		Method method = TestListener.class.getMethod("handleMessage", String.class);
		endpoint.setMethod(method);
		endpoint.setMethodPayloadTypeInferrer(inferrer);

		when(inferrer.inferPayloadType(any(Method.class), any())).thenReturn((Class) String.class);
		when(converter.isUsingDefaultPayloadTypeMapper()).thenReturn(false);

		endpoint.setupContainer(container);

		verify(converter, never()).setPayloadTypeMapper(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldNotConfigureWhenConverterIsNotAbstractMessagingMessageConverter() throws Exception {
		MessagingMessageConverter<?> nonAbstractConverter = (MessagingMessageConverter<?>) mock(
				MessagingMessageConverter.class);
		when(containerOptions.getMessageConverter()).thenReturn((MessagingMessageConverter) nonAbstractConverter);

		Method method = TestListener.class.getMethod("handleMessage", String.class);
		endpoint.setMethod(method);
		endpoint.setMethodPayloadTypeInferrer(inferrer);

		when(inferrer.inferPayloadType(any(Method.class), any())).thenReturn((Class) String.class);

		endpoint.setupContainer(container);

		verify(converter, never()).setPayloadTypeMapper(any());
	}

	static class TestListener {
		public void handleMessage(String message) {
			// test method
		}
	}

}
