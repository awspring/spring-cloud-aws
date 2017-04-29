/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.messaging.core.support;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alain Sahli
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractMessageChannelMessagingSendingTemplateTest {

    @Mock
    private DestinationResolver<String> destinationResolver;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        reset(this.destinationResolver);
    }

    @Test
    public void send_WithDestinationNameAndMessage_shouldResolveTheDestinationAndSendTheMessage() throws Exception {
        // Arrange
        MessageSendingTemplateTest messageSendingTemplate = new MessageSendingTemplateTest(this.destinationResolver);
        when(this.destinationResolver.resolveDestination("destination")).thenReturn("resolvedDestination");

        Map<String, Object> headers = Collections.<String, Object>singletonMap("headerKey", "headerValue");
        String payload = "payload";

        // Act
        messageSendingTemplate.send("destination", MessageBuilder.createMessage(payload, new MessageHeaders(headers)));

        // Assert
        verify(this.destinationResolver).resolveDestination("destination");
        assertEquals(payload, messageSendingTemplate.getMessageChannel().getSentMessage().getPayload());
        assertEquals(headers.get("headerKey"), messageSendingTemplate.getMessageChannel().getSentMessage().getHeaders().get("headerKey"));
    }

    @Test
    public void convertAndSend_WithDestinationNameAndPayload_shouldResolveTheDestinationAndSendTheConvertedMessage() throws Exception {
        // Arrange
        MessageSendingTemplateTest messageSendingTemplate = new MessageSendingTemplateTest(this.destinationResolver);
        when(this.destinationResolver.resolveDestination("destination")).thenReturn("resolvedDestination");

        String payload = "payload";

        // Act
        messageSendingTemplate.convertAndSend("destination", payload);

        // Assert
        verify(this.destinationResolver).resolveDestination("destination");
        assertEquals(payload, messageSendingTemplate.getMessageChannel().getSentMessage().getPayload());
    }

    @Test
    public void convertAndSend_WithDestinationNamePayloadAndHeaders_shouldResolveTheDestinationAndSendTheConvertedMessage() throws Exception {
        // Arrange
        MessageSendingTemplateTest messageSendingTemplate = new MessageSendingTemplateTest(this.destinationResolver);
        when(this.destinationResolver.resolveDestination("destination")).thenReturn("resolvedDestination");

        Map<String, Object> headers = Collections.<String, Object>singletonMap("headerKey", "headerValue");
        String payload = "payload";

        // Act
        messageSendingTemplate.convertAndSend("destination", payload, headers);

        // Assert
        verify(this.destinationResolver).resolveDestination("destination");
        assertEquals(payload, messageSendingTemplate.getMessageChannel().getSentMessage().getPayload());
        assertEquals(headers.get("headerKey"), messageSendingTemplate.getMessageChannel().getSentMessage().getHeaders().get("headerKey"));
    }

    @Test
    public void convertAndSend_WithDestinationNamePayloadAndPostProcessor_shouldResolveTheDestinationSendTheConvertedMessageAndCallPostProcessor() throws Exception {
        // Arrange
        MessageSendingTemplateTest messageSendingTemplate = new MessageSendingTemplateTest(this.destinationResolver);
        when(this.destinationResolver.resolveDestination("destination")).thenReturn("resolvedDestination");
        MessagePostProcessor messagePostProcessor = mock(MessagePostProcessor.class);
        when(messagePostProcessor.postProcessMessage(Matchers.<Message<?>>any())).thenAnswer(new Answer<Message<?>>() {

            @Override
            public Message<?> answer(InvocationOnMock invocation) throws Throwable {
                return (Message<?>) invocation.getArguments()[0];
            }
        });

        String payload = "payload";

        // Act
        messageSendingTemplate.convertAndSend("destination", payload, messagePostProcessor);

        // Assert
        verify(this.destinationResolver).resolveDestination("destination");
        assertEquals(payload, messageSendingTemplate.getMessageChannel().getSentMessage().getPayload());
        verify(messagePostProcessor).postProcessMessage(messageSendingTemplate.getMessageChannel().getSentMessage());
    }

    @Test
    public void convertAndSend_WithDestinationNamePayloadHeadersAndPostProcessor_shouldResolveTheDestinationSendTheConvertedMessageAndCallPostProcessor() throws Exception {
        // Arrange
        MessageSendingTemplateTest messageSendingTemplate = new MessageSendingTemplateTest(this.destinationResolver);
        when(this.destinationResolver.resolveDestination("destination")).thenReturn("resolvedDestination");
        MessagePostProcessor messagePostProcessor = mock(MessagePostProcessor.class);
        when(messagePostProcessor.postProcessMessage(Matchers.<Message<?>>any())).thenAnswer(new Answer<Message<?>>() {

            @Override
            public Message<?> answer(InvocationOnMock invocation) throws Throwable {
                return (Message<?>) invocation.getArguments()[0];
            }
        });

        Map<String, Object> headers = Collections.<String, Object>singletonMap("headerKey", "headerValue");
        String payload = "payload";

        // Act
        messageSendingTemplate.convertAndSend("destination", payload, headers, messagePostProcessor);

        // Assert
        verify(this.destinationResolver).resolveDestination("destination");
        assertEquals(payload, messageSendingTemplate.getMessageChannel().getSentMessage().getPayload());
        assertEquals(headers.get("headerKey"), messageSendingTemplate.getMessageChannel().getSentMessage().getHeaders().get("headerKey"));
        verify(messagePostProcessor).postProcessMessage(messageSendingTemplate.getMessageChannel().getSentMessage());
    }

    @Test
    public void send_WithPayload_shouldUseDefaultDestination() throws Exception {
        // Arrange
        MessageSendingTemplateTest messageSendingTemplate = new MessageSendingTemplateTest(this.destinationResolver);
        when(this.destinationResolver.resolveDestination("defaultDestination")).thenReturn("resolvedDestination");
        messageSendingTemplate.setDefaultDestinationName("defaultDestination");

        Map<String, Object> headers = Collections.<String, Object>singletonMap("headerKey", "headerValue");
        String payload = "payload";

        // Act
        messageSendingTemplate.send(MessageBuilder.createMessage(payload, new MessageHeaders(headers)));

        // Assert
        verify(this.destinationResolver).resolveDestination("defaultDestination");
        assertEquals(payload, messageSendingTemplate.getMessageChannel().getSentMessage().getPayload());
        assertEquals(headers.get("headerKey"), messageSendingTemplate.getMessageChannel().getSentMessage().getHeaders().get("headerKey"));

    }

    private static class MessageSendingTemplateTest extends AbstractMessageChannelMessagingSendingTemplate<MessageChannel> {

        private MessageChannelTest messageChannel;

        protected MessageSendingTemplateTest(DestinationResolver<String> destinationResolver) {
            super(destinationResolver);
        }

        @Override
        protected MessageChannel resolveMessageChannel(String physicalResourceIdentifier) {
            this.messageChannel = new MessageChannelTest();
            return this.messageChannel;
        }

        public MessageChannelTest getMessageChannel() {
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

        public Message<?> getSentMessage() {
            return this.sentMessage;
        }

    }

}
