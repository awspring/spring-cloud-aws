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

package org.springframework.cloud.aws.messaging.core;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alain Sahli
 */
public class QueueMessagingTemplateTest {

    @Test(expected = IllegalStateException.class)
    public void send_withoutDefaultDestination_throwAnException() throws Exception {
        AmazonSQSAsync amazonSqs = createAmazonSqs();
        QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);

        Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
        queueMessagingTemplate.send(stringMessage);
    }

    @Test
    public void send_withDefaultDestination_usesDefaultDestination() throws Exception {
        AmazonSQSAsync amazonSqs = createAmazonSqs();
        QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);
        queueMessagingTemplate.setDefaultDestinationName("my-queue");

        Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
        queueMessagingTemplate.send(stringMessage);

        ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(amazonSqs).sendMessage(sendMessageRequestArgumentCaptor.capture());
        assertEquals("http://queue-url.com", sendMessageRequestArgumentCaptor.getValue().getQueueUrl());
    }

    @Test
    public void send_withDestination_usesDestination() throws Exception {
        AmazonSQSAsync amazonSqs = createAmazonSqs();
        QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);

        Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
        queueMessagingTemplate.send("my-queue", stringMessage);

        ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(amazonSqs).sendMessage(sendMessageRequestArgumentCaptor.capture());
        assertEquals("http://queue-url.com", sendMessageRequestArgumentCaptor.getValue().getQueueUrl());
    }


    @Test
    public void send_withCustomDestinationResolveAndDestination_usesDestination() throws Exception {
        AmazonSQSAsync amazonSqs = createAmazonSqs();
        QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs, new DestinationResolver<String>() {

            @Override
            public String resolveDestination(String name) throws DestinationResolutionException {
                return name.toUpperCase(Locale.ENGLISH);
            }
        }, null);

        Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
        queueMessagingTemplate.send("myqueue", stringMessage);

        ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(amazonSqs).sendMessage(sendMessageRequestArgumentCaptor.capture());
        assertEquals("MYQUEUE", sendMessageRequestArgumentCaptor.getValue().getQueueUrl());
    }

    @Test(expected = IllegalStateException.class)
    public void receive_withoutDefaultDestination_throwsAnException() throws Exception {
        AmazonSQSAsync amazonSqs = createAmazonSqs();
        QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);

        queueMessagingTemplate.receive();
    }

    @Test
    public void receive_withDefaultDestination_useDefaultDestination() throws Exception {
        AmazonSQSAsync amazonSqs = createAmazonSqs();
        QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);
        queueMessagingTemplate.setDefaultDestinationName("my-queue");

        queueMessagingTemplate.receive();

        ArgumentCaptor<ReceiveMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(amazonSqs).receiveMessage(sendMessageRequestArgumentCaptor.capture());
        assertEquals("http://queue-url.com", sendMessageRequestArgumentCaptor.getValue().getQueueUrl());
    }

    @Test
    public void receive_withDestination_usesDestination() throws Exception {
        AmazonSQSAsync amazonSqs = createAmazonSqs();
        QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);

        queueMessagingTemplate.receive("my-queue");

        ArgumentCaptor<ReceiveMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(amazonSqs).receiveMessage(sendMessageRequestArgumentCaptor.capture());
        assertEquals("http://queue-url.com", sendMessageRequestArgumentCaptor.getValue().getQueueUrl());
    }

    @Test(expected = IllegalStateException.class)
    public void receiveAndConvert_withoutDefaultDestination_throwsAnException() throws Exception {
        AmazonSQSAsync amazonSqs = createAmazonSqs();
        QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);

        queueMessagingTemplate.receiveAndConvert(String.class);
    }

    @Test
    public void receiveAndConvert_withDefaultDestination_usesDefaultDestinationAndConvertsMessage() throws Exception {
        AmazonSQSAsync amazonSqs = createAmazonSqs();
        QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);
        queueMessagingTemplate.setDefaultDestinationName("my-queue");

        String message = queueMessagingTemplate.receiveAndConvert(String.class);

        assertEquals("My message", message);
    }

    @Test
    public void receiveAndConvert_withDestination_usesDestinationAndConvertsMessage() throws Exception {
        AmazonSQSAsync amazonSqs = createAmazonSqs();
        QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);

        String message = queueMessagingTemplate.receiveAndConvert("my-queue", String.class);

        assertEquals("My message", message);
    }

    @Test
    public void instantiation_withConverter_shouldAddItToTheCompositeConverter() throws Exception {
        // Arrange
        SimpleMessageConverter simpleMessageConverter = new SimpleMessageConverter();

        // Act
        QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(createAmazonSqs(), (ResourceIdResolver) null, simpleMessageConverter);

        // Assert
        assertEquals(2, ((CompositeMessageConverter) queueMessagingTemplate.getMessageConverter()).getConverters().size());
        assertEquals(simpleMessageConverter, ((CompositeMessageConverter) queueMessagingTemplate.getMessageConverter()).getConverters().get(1));
    }

    private AmazonSQSAsync createAmazonSqs() {
        AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);

        GetQueueUrlResult queueUrl = new GetQueueUrlResult();
        queueUrl.setQueueUrl("http://queue-url.com");
        when(amazonSqs.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrl);

        ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult();
        com.amazonaws.services.sqs.model.Message message = new com.amazonaws.services.sqs.model.Message();
        message.setBody("My message");
        receiveMessageResult.withMessages(message);
        when(amazonSqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(receiveMessageResult);

        return amazonSqs;
    }
}
