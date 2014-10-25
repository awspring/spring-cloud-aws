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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;

import java.nio.charset.Charset;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class QueueMessageChannelTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void sendMessage_validTextMessage_returnsTrue() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = mock(AmazonSQS.class);

		Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
		MessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		boolean sent = messageChannel.send(stringMessage);

		// Assert
		verify(amazonSqs, only()).
				sendMessage(new SendMessageRequest("http://testQueue", "message content").withDelaySeconds(0));
		assertTrue(sent);
	}

	@Test
	public void sendMessage_serviceThrowsError_throwsMessagingException() throws Exception {
		//Arrange
		AmazonSQS amazonSqs = mock(AmazonSQS.class);

		Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
		MessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		when(amazonSqs.sendMessage(new SendMessageRequest("http://testQueue", "message content").withDelaySeconds(0))).
				thenThrow(new AmazonServiceException("wanted error"));

		//Assert
		this.expectedException.expect(MessagingException.class);
		this.expectedException.expectMessage("wanted error");

		//Act
		messageChannel.send(stringMessage);
	}

	@Test
	public void sendMessage_withMimeTypeAsStringHeader_shouldPassItAsMessageAttribute() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		String mimeTypeAsString = new MimeType("test", "plain", Charset.forName("UTF-8")).toString();
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(MessageHeaders.CONTENT_TYPE, mimeTypeAsString).build();

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture())).thenReturn(new SendMessageResult());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertTrue(sent);
		assertEquals(mimeTypeAsString, sendMessageRequestArgumentCaptor.getValue().getMessageAttributes().get(MessageHeaders.CONTENT_TYPE).getStringValue());
	}

	@Test
	public void sendMessage_withMimeTypeHeader_shouldPassItAsMessageAttribute() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		MimeType mimeType = new MimeType("test", "plain", Charset.forName("UTF-8"));
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(MessageHeaders.CONTENT_TYPE, mimeType).build();

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture())).thenReturn(new SendMessageResult());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertTrue(sent);
		assertEquals(mimeType.toString(), sendMessageRequestArgumentCaptor.getValue().getMessageAttributes().get(MessageHeaders.CONTENT_TYPE).getStringValue());
	}

	@Test
	public void receiveMessage_withoutTimeout_returnsTextMessage() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue").
				withWaitTimeSeconds(0).
				withMaxNumberOfMessages(1).
				withAttributeNames(QueueMessageChannel.MESSAGE_RECEIVING_ATTRIBUTE_NAMES).
				withMessageAttributeNames(MessageHeaders.CONTENT_TYPE))).
				thenReturn(new ReceiveMessageResult().withMessages(
						Collections.singleton(new com.amazonaws.services.sqs.model.Message().withBody("content"))));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		//Act
		Message<?> receivedMessage = messageChannel.receive();

		//Assert
		assertNotNull(receivedMessage);
		assertEquals("content", receivedMessage.getPayload());
	}

	@Test
	public void receiveMessage_withSpecifiedTimeout_returnsTextMessage() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue").
				withWaitTimeSeconds(2).
				withMaxNumberOfMessages(1).
				withAttributeNames(QueueMessageChannel.MESSAGE_RECEIVING_ATTRIBUTE_NAMES).
				withMessageAttributeNames(MessageHeaders.CONTENT_TYPE))).
				thenReturn(new ReceiveMessageResult().withMessages(
						Collections.singleton(new com.amazonaws.services.sqs.model.Message().withBody("content"))));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		//Act
		Message<?> receivedMessage = messageChannel.receive(2);

		//Assert
		assertNotNull(receivedMessage);
		assertEquals("content", receivedMessage.getPayload());
	}

	@Test
	public void receiveMessage_withSpecifiedTimeout_returnsNull() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue").
				withWaitTimeSeconds(2).
				withMaxNumberOfMessages(1).
				withAttributeNames(QueueMessageChannel.MESSAGE_RECEIVING_ATTRIBUTE_NAMES).
				withMessageAttributeNames(MessageHeaders.CONTENT_TYPE))).
				thenReturn(new ReceiveMessageResult().withMessages(
						Collections.<com.amazonaws.services.sqs.model.Message>emptyList()));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		//Act
		Message<?> receivedMessage = messageChannel.receive(2);

		//Assert
		assertNull(receivedMessage);
	}

	@Test
	public void receiveMessage_withoutDefaultTimeout_returnsNull() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue").
				withWaitTimeSeconds(0).
				withMaxNumberOfMessages(1).
				withAttributeNames(QueueMessageChannel.MESSAGE_RECEIVING_ATTRIBUTE_NAMES).
				withMessageAttributeNames(MessageHeaders.CONTENT_TYPE))).
				thenReturn(new ReceiveMessageResult().withMessages(
						Collections.<com.amazonaws.services.sqs.model.Message>emptyList()));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		//Act
		Message<?> receivedMessage = messageChannel.receive(0);

		//Assert
		assertNull(receivedMessage);
	}

	@Test
	public void receiveMessage_withMimeTypeMessageAttribute_shouldCopyToHeaders() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		MimeType mimeType = new MimeType("test", "plain", Charset.forName("UTF-8"));
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue").
				withWaitTimeSeconds(0).
				withMaxNumberOfMessages(1).
				withAttributeNames(QueueMessageChannel.MESSAGE_RECEIVING_ATTRIBUTE_NAMES).
				withMessageAttributeNames(MessageHeaders.CONTENT_TYPE))).
				thenReturn(new ReceiveMessageResult().withMessages(new com.amazonaws.services.sqs.model.Message().withBody("Hello").
						withMessageAttributes(Collections.singletonMap(MessageHeaders.CONTENT_TYPE,
								new MessageAttributeValue().withDataType("String").withStringValue(mimeType.toString())))));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		assertEquals(mimeType, receivedMessage.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}
}