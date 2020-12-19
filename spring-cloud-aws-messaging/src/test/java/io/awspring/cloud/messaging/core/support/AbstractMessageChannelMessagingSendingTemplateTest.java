/*
 * Copyright 2013-2019 the original author or authors.
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

package io.awspring.cloud.messaging.core.support;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alain Sahli
 */
@ExtendWith(MockitoExtension.class)
class AbstractMessageChannelMessagingSendingTemplateTest {

	@Mock
	private DestinationResolver<String> destinationResolver;

	@SuppressWarnings("unchecked")
	@BeforeEach
	void setUp() throws Exception {
		reset(this.destinationResolver);
	}

	@Test
	void send_WithDestinationNameAndMessage_shouldResolveTheDestinationAndSendTheMessage() throws Exception {
		// Arrange
		MessageSendingTemplateTest messageSendingTemplate = new MessageSendingTemplateTest(this.destinationResolver);
		when(this.destinationResolver.resolveDestination("destination")).thenReturn("resolvedDestination");

		Map<String, Object> headers = Collections.singletonMap("headerKey", "headerValue");
		String payload = "payload";

		// Act
		messageSendingTemplate.send("destination", MessageBuilder.createMessage(payload, new MessageHeaders(headers)));

		// Assert
		verify(this.destinationResolver).resolveDestination("destination");
		assertThat(messageSendingTemplate.getMessageChannel().getSentMessage().getPayload()).isEqualTo(payload);
		assertThat(messageSendingTemplate.getMessageChannel().getSentMessage().getHeaders().get("headerKey"))
				.isEqualTo(headers.get("headerKey"));
	}

	@Test
	void convertAndSend_WithDestinationNameAndPayload_shouldResolveTheDestinationAndSendTheConvertedMessage()
			throws Exception {
		// Arrange
		MessageSendingTemplateTest messageSendingTemplate = new MessageSendingTemplateTest(this.destinationResolver);
		when(this.destinationResolver.resolveDestination("destination")).thenReturn("resolvedDestination");

		String payload = "payload";

		// Act
		messageSendingTemplate.convertAndSend("destination", payload);

		// Assert
		verify(this.destinationResolver).resolveDestination("destination");
		assertThat(messageSendingTemplate.getMessageChannel().getSentMessage().getPayload()).isEqualTo(payload);
	}

	@Test
	void convertAndSend_WithDestinationNamePayloadAndHeaders_shouldResolveTheDestinationAndSendTheConvertedMessage()
			throws Exception {
		// Arrange
		MessageSendingTemplateTest messageSendingTemplate = new MessageSendingTemplateTest(this.destinationResolver);
		when(this.destinationResolver.resolveDestination("destination")).thenReturn("resolvedDestination");

		Map<String, Object> headers = Collections.singletonMap("headerKey", "headerValue");
		String payload = "payload";

		// Act
		messageSendingTemplate.convertAndSend("destination", payload, headers);

		// Assert
		verify(this.destinationResolver).resolveDestination("destination");
		assertThat(messageSendingTemplate.getMessageChannel().getSentMessage().getPayload()).isEqualTo(payload);
		assertThat(messageSendingTemplate.getMessageChannel().getSentMessage().getHeaders().get("headerKey"))
				.isEqualTo(headers.get("headerKey"));
	}

	// @checkstyle:off
	@Test
	void convertAndSend_WithDestinationNamePayloadAndPostProcessor_shouldResolveTheDestinationSendTheConvertedMessageAndCallPostProcessor()
			throws Exception {
		// @checkstyle:on
		// Arrange
		MessageSendingTemplateTest messageSendingTemplate = new MessageSendingTemplateTest(this.destinationResolver);
		when(this.destinationResolver.resolveDestination("destination")).thenReturn("resolvedDestination");
		MessagePostProcessor messagePostProcessor = mock(MessagePostProcessor.class);
		when(messagePostProcessor.postProcessMessage(ArgumentMatchers.any()))
				.thenAnswer((Answer<Message<?>>) invocation -> (Message<?>) invocation.getArguments()[0]);

		String payload = "payload";

		// Act
		messageSendingTemplate.convertAndSend("destination", payload, messagePostProcessor);

		// Assert
		verify(this.destinationResolver).resolveDestination("destination");
		assertThat(messageSendingTemplate.getMessageChannel().getSentMessage().getPayload()).isEqualTo(payload);
		verify(messagePostProcessor).postProcessMessage(messageSendingTemplate.getMessageChannel().getSentMessage());
	}

	// @checkstyle:off
	@Test
	void convertAndSend_WithDestinationNamePayloadHeadersAndPostProcessor_shouldResolveTheDestinationSendTheConvertedMessageAndCallPostProcessor()
			// @checkstyle:on
			throws Exception {
		// Arrange
		MessageSendingTemplateTest messageSendingTemplate = new MessageSendingTemplateTest(this.destinationResolver);
		when(this.destinationResolver.resolveDestination("destination")).thenReturn("resolvedDestination");
		MessagePostProcessor messagePostProcessor = mock(MessagePostProcessor.class);
		when(messagePostProcessor.postProcessMessage(ArgumentMatchers.any()))
				.thenAnswer((Answer<Message<?>>) invocation -> (Message<?>) invocation.getArguments()[0]);

		Map<String, Object> headers = Collections.singletonMap("headerKey", "headerValue");
		String payload = "payload";

		// Act
		messageSendingTemplate.convertAndSend("destination", payload, headers, messagePostProcessor);

		// Assert
		verify(this.destinationResolver).resolveDestination("destination");
		assertThat(messageSendingTemplate.getMessageChannel().getSentMessage().getPayload()).isEqualTo(payload);
		assertThat(messageSendingTemplate.getMessageChannel().getSentMessage().getHeaders().get("headerKey"))
				.isEqualTo(headers.get("headerKey"));
		verify(messagePostProcessor).postProcessMessage(messageSendingTemplate.getMessageChannel().getSentMessage());
	}

	@Test
	void send_WithPayload_shouldUseDefaultDestination() throws Exception {
		// Arrange
		MessageSendingTemplateTest messageSendingTemplate = new MessageSendingTemplateTest(this.destinationResolver);
		when(this.destinationResolver.resolveDestination("defaultDestination")).thenReturn("resolvedDestination");
		messageSendingTemplate.setDefaultDestinationName("defaultDestination");

		Map<String, Object> headers = Collections.singletonMap("headerKey", "headerValue");
		String payload = "payload";

		// Act
		messageSendingTemplate.send(MessageBuilder.createMessage(payload, new MessageHeaders(headers)));

		// Assert
		verify(this.destinationResolver).resolveDestination("defaultDestination");
		assertThat(messageSendingTemplate.getMessageChannel().getSentMessage().getPayload()).isEqualTo(payload);
		assertThat(messageSendingTemplate.getMessageChannel().getSentMessage().getHeaders().get("headerKey"))
				.isEqualTo(headers.get("headerKey"));

	}

	private static class MessageSendingTemplateTest
			extends AbstractMessageChannelMessagingSendingTemplate<MessageChannel> {

		private MessageChannelTest messageChannel;

		protected MessageSendingTemplateTest(DestinationResolver<String> destinationResolver) {
			super(destinationResolver);
		}

		@Override
		protected MessageChannel resolveMessageChannel(String physicalResourceIdentifier) {
			this.messageChannel = new MessageChannelTest();
			return this.messageChannel;
		}

		MessageChannelTest getMessageChannel() {
			return this.messageChannel;
		}

	}

	private static class MessageChannelTest implements MessageChannel {

		private Message<?> sentMessage;

		@Override
		public boolean send(Message<?> message) {
			this.sentMessage = message;
			return true;
		}

		@Override
		public boolean send(Message<?> message, long timeout) {
			this.sentMessage = message;
			return false;
		}

		Message<?> getSentMessage() {
			return this.sentMessage;
		}

	}

}
