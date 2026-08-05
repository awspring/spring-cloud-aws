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
package io.awspring.cloud.sqs.support.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.support.converter.legacy.LegacyJackson2SqsMessagingMessageConverter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

/**
 * Tests for {@link LegacyJackson2SqsMessagingMessageConverter}.
 *
 * @author Tomaz Fernandes
 */
class LegacyJackson2SqsMessagingMessageConverterTests {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void shouldUseProvidedTypeMapper() throws Exception {
		MyPojo myPojo = new MyPojo();
		String payload = new ObjectMapper().writeValueAsString(myPojo);
		Message message = Message.builder().body(payload).messageId(UUID.randomUUID().toString()).build();
		LegacyJackson2SqsMessagingMessageConverter converter = new LegacyJackson2SqsMessagingMessageConverter();
		converter.setPayloadTypeMapper(msg -> MyPojo.class);
		org.springframework.messaging.Message<?> resultMessage = converter.toMessagingMessage(message);
		assertThat(resultMessage.getPayload()).isEqualTo(myPojo);
	}

	@Test
	void shouldUseProvidedTypeHeader() throws Exception {
		String typeHeader = "myHeader";
		MyPojo myPojo = new MyPojo();
		String payload = this.objectMapper.writeValueAsString(myPojo);
		Message message = Message.builder()
				.messageAttributes(Collections.singletonMap(typeHeader,
						MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING)
								.stringValue(MyPojo.class.getName()).build()))
				.body(payload).messageId(UUID.randomUUID().toString()).build();
		LegacyJackson2SqsMessagingMessageConverter converter = new LegacyJackson2SqsMessagingMessageConverter();
		converter.setPayloadTypeHeader(typeHeader);
		org.springframework.messaging.Message<?> resultMessage = converter.toMessagingMessage(message);
		assertThat(resultMessage.getPayload()).isEqualTo(myPojo);
	}

	@Test
	void shouldUseHeaderOverPayloadClass() throws Exception {
		String typeHeader = "myHeader";
		MyPojo myPojo = new MyPojo();
		String payload = this.objectMapper.writeValueAsString(myPojo);
		Message message = Message.builder()
				.messageAttributes(Collections.singletonMap(typeHeader,
						MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING)
								.stringValue(MyPojo.class.getName()).build()))
				.body(payload).messageId(UUID.randomUUID().toString()).build();
		LegacyJackson2SqsMessagingMessageConverter converter = new LegacyJackson2SqsMessagingMessageConverter();
		SqsMessageConversionContext context = new SqsMessageConversionContext();
		context.setPayloadClass(String.class);
		converter.setPayloadTypeHeader(typeHeader);
		org.springframework.messaging.Message<?> resultMessage = converter.toMessagingMessage(message, context);
		assertThat(resultMessage.getPayload()).isEqualTo(myPojo);
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldUseProvidedHeaderMapper() {
		Message message = Message.builder().body("test-payload").messageId(UUID.randomUUID().toString()).build();
		LegacyJackson2SqsMessagingMessageConverter converter = new LegacyJackson2SqsMessagingMessageConverter();
		HeaderMapper<software.amazon.awssdk.services.sqs.model.Message> mapper = mock(HeaderMapper.class);
		MessageHeaders messageHeaders = new MessageHeaders(Collections.singletonMap("testHeader", "testHeaderValue"));
		given(mapper.toHeaders(message)).willReturn(messageHeaders);
		converter.setHeaderMapper(mapper);
		org.springframework.messaging.Message<?> resultMessage = converter.toMessagingMessage(message);
		assertThat(resultMessage.getHeaders()).isEqualTo(messageHeaders);
	}

	@Test
	void shouldUseProvidedPayloadConverter() throws Exception {
		MyPojo myPojo = new MyPojo();
		String payload = new ObjectMapper().writeValueAsString(myPojo);
		Message message = Message.builder().body(payload).messageId(UUID.randomUUID().toString()).build();
		MessageConverter payloadConverter = mock(MessageConverter.class);
		when(payloadConverter.fromMessage(any(org.springframework.messaging.Message.class), eq(MyPojo.class)))
				.thenReturn(myPojo);
		LegacyJackson2SqsMessagingMessageConverter converter = new LegacyJackson2SqsMessagingMessageConverter();
		converter.setPayloadMessageConverter(payloadConverter);
		converter.setPayloadTypeMapper(msg -> MyPojo.class);
		org.springframework.messaging.Message<?> resultMessage = converter.toMessagingMessage(message);
		assertThat(resultMessage.getPayload()).isEqualTo(myPojo);
	}

	@Test
	void shouldUseHeadersFromPayloadConverter() {
		MessageConverter payloadConverter = mock(MessageConverter.class);
		org.springframework.messaging.Message convertedMessageWithContentType = MessageBuilder.withPayload("example")
				.setHeader("contentType", "application/json").build();
		when(payloadConverter.toMessage(any(MyPojo.class), any())).thenReturn(convertedMessageWithContentType);

		LegacyJackson2SqsMessagingMessageConverter converter = new LegacyJackson2SqsMessagingMessageConverter();
		converter.setPayloadMessageConverter(payloadConverter);
		converter.setPayloadTypeMapper(msg -> MyPojo.class);

		org.springframework.messaging.Message<MyPojo> message = MessageBuilder.createMessage(new MyPojo(),
				new MessageHeaders(null));
		Message resultMessage = converter.fromMessagingMessage(message);

		assertThat(resultMessage.messageId()).isEqualTo(message.getHeaders().getId().toString());
		assertThat(resultMessage.messageAttributes()).containsEntry("contentType",
				MessageAttributeValue.builder().stringValue("application/json").dataType("String").build());
	}

	@Test
	void shouldReturnTrueForDefaultPayloadTypeMapper() {
		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
		assertThat(converter.isUsingDefaultPayloadTypeMapper()).isTrue();
	}

	@Test
	void shouldReturnFalseAfterSettingCustomPayloadTypeMapper() {
		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
		converter.setPayloadTypeMapper(msg -> MyPojo.class);
		assertThat(converter.isUsingDefaultPayloadTypeMapper()).isFalse();
	}

	@Test
	void shouldReturnFalseAfterSettingPayloadTypeHeader() {
		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
		converter.setPayloadTypeHeader("myTypeHeader");
		assertThat(converter.isUsingDefaultPayloadTypeMapper()).isFalse();
	}

	@Test
	void shouldDeserializeGenericWrapperUsingListenerMethodConversionHint() throws Exception {
		Method listenerMethod = GenericListener.class.getDeclaredMethod("listen", GenericWrapper.class);
		MethodParameter conversionHint = new MethodParameter(listenerMethod, 0);
		LegacyJackson2SqsMessagingMessageConverter converter = new LegacyJackson2SqsMessagingMessageConverter();
		SqsMessageConversionContext context = new SqsMessageConversionContext();
		context.setPayloadClass(GenericWrapper.class);
		context.setConversionHint(conversionHint);
		Message message = Message.builder().body("""
				{"value":{"myProperty":"nested-value"}}
				""").messageId(UUID.randomUUID().toString()).build();

		org.springframework.messaging.Message<?> result = converter.toMessagingMessage(message, context);

		assertThat(result.getPayload()).isInstanceOfSatisfying(GenericWrapper.class,
				wrapper -> assertThat(wrapper.getValue()).isInstanceOfSatisfying(MyPojo.class,
						pojo -> assertThat(pojo.getMyProperty()).isEqualTo("nested-value")));
	}

	@Test
	void shouldDeserializeNestedGenericWrapperUsingListenerMethodConversionHint() throws Exception {
		Method listenerMethod = GenericListener.class.getDeclaredMethod("listenToNestedWrapper", GenericWrapper.class);
		MethodParameter conversionHint = new MethodParameter(listenerMethod, 0);
		LegacyJackson2SqsMessagingMessageConverter converter = new LegacyJackson2SqsMessagingMessageConverter();
		SqsMessageConversionContext context = new SqsMessageConversionContext();
		context.setPayloadClass(GenericWrapper.class);
		context.setConversionHint(conversionHint);
		Message message = Message.builder().body("""
				{"value":[{"myProperty":"first"},{"myProperty":"second"}]}
				""").messageId(UUID.randomUUID().toString()).build();

		org.springframework.messaging.Message<?> result = converter.toMessagingMessage(message, context);

		assertThat(result.getPayload()).isInstanceOfSatisfying(GenericWrapper.class,
				wrapper -> assertThat(wrapper.getValue())
						.isEqualTo(List.of(new MyPojo("first"), new MyPojo("second"))));
	}

	static class GenericListener {

		void listen(GenericWrapper<MyPojo> payload) {
		}

		void listenToNestedWrapper(GenericWrapper<List<MyPojo>> payload) {
		}

	}

	static class GenericWrapper<T> {

		private T value;

		public T getValue() {
			return this.value;
		}

		public void setValue(T value) {
			this.value = value;
		}

	}

	static class MyPojo {

		private String myProperty = "myValue";

		MyPojo() {
		}

		MyPojo(String myProperty) {
			this.myProperty = myProperty;
		}

		public String getMyProperty() {
			return this.myProperty;
		}

		public void setMyProperty(String myProperty) {
			this.myProperty = myProperty;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			MyPojo myPojo = (MyPojo) o;
			return Objects.equals(myProperty, myPojo.myProperty);
		}

		@Override
		public int hashCode() {
			return myProperty != null ? myProperty.hashCode() : 0;
		}
	}

}
