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

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alain Sahli
 */
public class TopicMessageChannelTest {

    @Test
    public void sendMessage_validTextMessageAndSubject_returnsTrue() throws Exception {
        // Arrange
        AmazonSNS amazonSns = mock(AmazonSNS.class);

        Message<String> stringMessage = MessageBuilder.withPayload("Message content").setHeader(TopicMessageChannel.NOTIFICATION_SUBJECT_HEADER, "Subject").build();
        MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

        // Act
        boolean sent = messageChannel.send(stringMessage);

        // Assert
        verify(amazonSns, only()).publish(new PublishRequest("topicArn",
                "Message content", "Subject").withMessageAttributes(anyMapOf(String.class, MessageAttributeValue.class)));
        assertTrue(sent);
    }

    @Test
    public void sendMessage_validTextMessageWithoutSubject_returnsTrue() throws Exception {
        // Arrange
        AmazonSNS amazonSns = mock(AmazonSNS.class);

        Message<String> stringMessage = MessageBuilder.withPayload("Message content").build();
        MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

        // Act
        boolean sent = messageChannel.send(stringMessage);

        // Assert
        verify(amazonSns, only()).publish(new PublishRequest("topicArn",
                "Message content", null).withMessageAttributes(anyMapOf(String.class, MessageAttributeValue.class)));
        assertTrue(sent);
    }

    @Test
    public void sendMessage_validTextMessageAndTimeout_timeoutIsIgnored() throws Exception {
        // Arrange
        AmazonSNS amazonSns = mock(AmazonSNS.class);

        Message<String> stringMessage = MessageBuilder.withPayload("Message content").build();
        MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

        // Act
        boolean sent = messageChannel.send(stringMessage, 10);

        // Assert
        verify(amazonSns, only()).publish(new PublishRequest("topicArn",
                "Message content", null).withMessageAttributes(anyMapOf(String.class, MessageAttributeValue.class)));
        assertTrue(sent);
    }

    @Test
    public void sendMessage_withStringMessageHeader_shouldBeSentAsTopicMessageAttribute() throws Exception {
        // Arrange
        AmazonSNS amazonSns = mock(AmazonSNS.class);
        ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        when(amazonSns.publish(publishRequestArgumentCaptor.capture())).thenReturn(new PublishResult());

        String headerValue = "Header value";
        String headerName = "MyHeader";
        Message<String> message = MessageBuilder.withPayload("Hello").setHeader(headerName, headerValue).build();
        MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

        // Act
        boolean sent = messageChannel.send(message);

        // Assert
        assertTrue(sent);
        assertEquals(headerValue, publishRequestArgumentCaptor.getValue().getMessageAttributes().get(headerName).getStringValue());
        assertEquals(MessageAttributeDataTypes.STRING, publishRequestArgumentCaptor.getValue().getMessageAttributes().get(headerName).getDataType());
    }

    @Test
    public void sendMessage_withNumericMessageHeaders_shouldBeSentAsTopicMessageAttributes() throws Exception {
        // Arrange
        AmazonSNS amazonSns = mock(AmazonSNS.class);
        ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        when(amazonSns.publish(publishRequestArgumentCaptor.capture())).thenReturn(new PublishResult());

        double doubleValue = 1234.56;
        long longValue = 1234L;
        int integerValue = 1234;
        byte byteValue = 2;
        short shortValue = 12;
        float floatValue = 1234.56f;
        BigInteger bigIntegerValue = new BigInteger("616416546156");
        BigDecimal bigDecimalValue = new BigDecimal("7834938");

        Message<String> message = MessageBuilder.withPayload("Hello")
                .setHeader("double", doubleValue)
                .setHeader("long", longValue)
                .setHeader("integer", integerValue)
                .setHeader("byte", byteValue)
                .setHeader("short", shortValue)
                .setHeader("float", floatValue)
                .setHeader("bigInteger", bigIntegerValue)
                .setHeader("bigDecimal", bigDecimalValue)
                .build();
        MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

        // Act
        boolean sent = messageChannel.send(message);

        // Assert
        assertTrue(sent);
        Map<String, MessageAttributeValue> messageAttributes = publishRequestArgumentCaptor.getValue().getMessageAttributes();
        assertEquals(MessageAttributeDataTypes.NUMBER + ".java.lang.Double", messageAttributes.get("double").getDataType());
        assertEquals(String.valueOf(doubleValue), messageAttributes.get("double").getStringValue());
        assertEquals(MessageAttributeDataTypes.NUMBER + ".java.lang.Long", messageAttributes.get("long").getDataType());
        assertEquals(String.valueOf(longValue), messageAttributes.get("long").getStringValue());
        assertEquals(MessageAttributeDataTypes.NUMBER + ".java.lang.Integer", messageAttributes.get("integer").getDataType());
        assertEquals(String.valueOf(integerValue), messageAttributes.get("integer").getStringValue());
        assertEquals(MessageAttributeDataTypes.NUMBER + ".java.lang.Byte", messageAttributes.get("byte").getDataType());
        assertEquals(String.valueOf(byteValue), messageAttributes.get("byte").getStringValue());
        assertEquals(MessageAttributeDataTypes.NUMBER + ".java.lang.Short", messageAttributes.get("short").getDataType());
        assertEquals(String.valueOf(shortValue), messageAttributes.get("short").getStringValue());
        assertEquals(MessageAttributeDataTypes.NUMBER + ".java.lang.Float", messageAttributes.get("float").getDataType());
        assertEquals(String.valueOf(floatValue), messageAttributes.get("float").getStringValue());
        assertEquals(MessageAttributeDataTypes.NUMBER + ".java.math.BigInteger", messageAttributes.get("bigInteger").getDataType());
        assertEquals(String.valueOf(bigIntegerValue), messageAttributes.get("bigInteger").getStringValue());
        assertEquals(MessageAttributeDataTypes.NUMBER + ".java.math.BigDecimal", messageAttributes.get("bigDecimal").getDataType());
        assertEquals(String.valueOf(bigDecimalValue), messageAttributes.get("bigDecimal").getStringValue());
    }

    @Test
    public void sendMessage_withBinaryMessageHeader_shouldBeSentAsBinaryMessageAttribute() throws Exception {
        // Arrange
        AmazonSNS amazonSns = mock(AmazonSNS.class);
        ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        when(amazonSns.publish(publishRequestArgumentCaptor.capture())).thenReturn(new PublishResult());

        ByteBuffer headerValue = ByteBuffer.wrap("My binary data!".getBytes());
        String headerName = "MyHeader";
        Message<String> message = MessageBuilder.withPayload("Hello").setHeader(headerName, headerValue).build();
        MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

        // Act
        boolean sent = messageChannel.send(message);

        // Assert
        assertTrue(sent);
        assertEquals(headerValue, publishRequestArgumentCaptor.getValue().getMessageAttributes().get(headerName).getBinaryValue());
        assertEquals(MessageAttributeDataTypes.BINARY, publishRequestArgumentCaptor.getValue().getMessageAttributes().get(headerName).getDataType());
    }

    @Test
    public void sendMessage_withUuidAsId_shouldConvertUuidToString() throws Exception {
        // Arrange
        AmazonSNS amazonSns = mock(AmazonSNS.class);
        TopicMessageChannel messageChannel = new TopicMessageChannel(amazonSns, "http://testQueue");
        Message<String> message = MessageBuilder.withPayload("Hello").build();
        UUID uuid = (UUID) message.getHeaders().get(MessageHeaders.ID);

        ArgumentCaptor<PublishRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        when(amazonSns.publish(sendMessageRequestArgumentCaptor.capture())).thenReturn(new PublishResult());

        // Act
        boolean sent = messageChannel.send(message);

        // Assert
        assertTrue(sent);
        assertEquals(uuid.toString(), sendMessageRequestArgumentCaptor.getValue().getMessageAttributes().get(MessageHeaders.ID).getStringValue());
    }
}
