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
import io.awspring.cloud.sqs.support.converter.jackson2.LegacySqsMessagingMessageConverter;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

/**
 * Tests for {@link LegacySqsMessagingMessageConverter}.
 *
 * @author Tomaz Fernandes
 */
class LegacySqsMessagingMessageConverterTests {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void shouldUseProvidedTypeMapper() throws Exception {
		MyPojo myPojo = new MyPojo();
		String payload = new ObjectMapper().writeValueAsString(myPojo);
		Message message = Message.builder().body(payload).messageId(UUID.randomUUID().toString()).build();
		LegacySqsMessagingMessageConverter converter = new LegacySqsMessagingMessageConverter();
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
		LegacySqsMessagingMessageConverter converter = new LegacySqsMessagingMessageConverter();
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
		LegacySqsMessagingMessageConverter converter = new LegacySqsMessagingMessageConverter();
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
		LegacySqsMessagingMessageConverter converter = new LegacySqsMessagingMessageConverter();
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
		LegacySqsMessagingMessageConverter converter = new LegacySqsMessagingMessageConverter();
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

		LegacySqsMessagingMessageConverter converter = new LegacySqsMessagingMessageConverter();
		converter.setPayloadMessageConverter(payloadConverter);
		converter.setPayloadTypeMapper(msg -> MyPojo.class);

		org.springframework.messaging.Message<MyPojo> message = MessageBuilder.createMessage(new MyPojo(),
				new MessageHeaders(null));
		Message resultMessage = converter.fromMessagingMessage(message);

		assertThat(resultMessage.messageId()).isEqualTo(message.getHeaders().getId().toString());
		assertThat(resultMessage.messageAttributes()).containsEntry("contentType",
				MessageAttributeValue.builder().stringValue("application/json").dataType("String").build());
	}

	static class MyPojo {

		private String myProperty = "myValue";

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
