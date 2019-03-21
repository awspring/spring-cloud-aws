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

package org.springframework.cloud.aws.messaging.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQSAsync;
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
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNotNull;
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
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(new SendMessageResult());

		Message<String> stringMessage = MessageBuilder.withPayload("message content")
				.build();
		MessageChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		boolean sent = messageChannel.send(stringMessage);

		// Assert
		verify(amazonSqs, only()).sendMessage(any(SendMessageRequest.class));
		assertThat(sendMessageRequestArgumentCaptor.getValue().getMessageBody())
				.isEqualTo("message content");
		assertThat(sent).isTrue();
	}

	@Test
	public void sendMessage_serviceThrowsError_throwsMessagingException()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);

		Message<String> stringMessage = MessageBuilder.withPayload("message content")
				.build();
		MessageChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");
		when(amazonSqs
				.sendMessage(new SendMessageRequest("http://testQueue", "message content")
						.withDelaySeconds(0).withMessageAttributes(isNotNull())))
								.thenThrow(new AmazonServiceException("wanted error"));

		// Assert
		this.expectedException.expect(MessagingException.class);
		this.expectedException.expectMessage("wanted error");

		// Act
		messageChannel.send(stringMessage);
	}

	@Test
	public void sendMessage_withMimeTypeAsStringHeader_shouldPassItAsMessageAttribute()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");
		String mimeTypeAsString = new MimeType("test", "plain", Charset.forName("UTF-8"))
				.toString();
		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader(MessageHeaders.CONTENT_TYPE, mimeTypeAsString).build();

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(new SendMessageResult());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(sendMessageRequestArgumentCaptor.getValue().getMessageAttributes()
				.get(MessageHeaders.CONTENT_TYPE).getStringValue())
						.isEqualTo(mimeTypeAsString);
	}

	@Test
	public void sendMessage_withMimeTypeHeader_shouldPassItAsMessageAttribute()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");
		MimeType mimeType = new MimeType("test", "plain", Charset.forName("UTF-8"));
		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader(MessageHeaders.CONTENT_TYPE, mimeType).build();

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(new SendMessageResult());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(sendMessageRequestArgumentCaptor.getValue().getMessageAttributes()
				.get(MessageHeaders.CONTENT_TYPE).getStringValue())
						.isEqualTo(mimeType.toString());
	}

	@Test
	public void receiveMessage_withoutTimeout_returnsTextMessage() throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue")
				.withWaitTimeSeconds(0).withMaxNumberOfMessages(1)
				.withAttributeNames(QueueMessageChannel.ATTRIBUTE_NAMES)
				.withMessageAttributeNames("All")))
						.thenReturn(new ReceiveMessageResult().withMessages(Collections
								.singleton(new com.amazonaws.services.sqs.model.Message()
										.withBody("content"))));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		assertThat(receivedMessage).isNotNull();
		assertThat(receivedMessage.getPayload()).isEqualTo("content");
	}

	@Test
	public void receiveMessage_withSpecifiedTimeout_returnsTextMessage()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue")
				.withWaitTimeSeconds(2).withMaxNumberOfMessages(1)
				.withAttributeNames(QueueMessageChannel.ATTRIBUTE_NAMES)
				.withMessageAttributeNames("All")))
						.thenReturn(new ReceiveMessageResult().withMessages(Collections
								.singleton(new com.amazonaws.services.sqs.model.Message()
										.withBody("content"))));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive(2);

		// Assert
		assertThat(receivedMessage).isNotNull();
		assertThat(receivedMessage.getPayload()).isEqualTo("content");
	}

	@Test
	public void receiveMessage_withSpecifiedTimeout_returnsNull() throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue")
				.withWaitTimeSeconds(2).withMaxNumberOfMessages(1)
				.withAttributeNames(QueueMessageChannel.ATTRIBUTE_NAMES)
				.withMessageAttributeNames("All"))).thenReturn(
						new ReceiveMessageResult().withMessages(Collections.emptyList()));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive(2);

		// Assert
		assertThat(receivedMessage).isNull();
	}

	@Test
	public void receiveMessage_withoutDefaultTimeout_returnsNull() throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue")
				.withWaitTimeSeconds(0).withMaxNumberOfMessages(1)
				.withAttributeNames(QueueMessageChannel.ATTRIBUTE_NAMES)
				.withMessageAttributeNames("All"))).thenReturn(
						new ReceiveMessageResult().withMessages(Collections.emptyList()));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive(0);

		// Assert
		assertThat(receivedMessage).isNull();
	}

	@Test
	public void receiveMessage_withMimeTypeMessageAttribute_shouldCopyToHeaders()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		MimeType mimeType = new MimeType("test", "plain", Charset.forName("UTF-8"));
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue")
				.withWaitTimeSeconds(0).withMaxNumberOfMessages(1)
				.withAttributeNames(QueueMessageChannel.ATTRIBUTE_NAMES)
				.withMessageAttributeNames("All"))).thenReturn(new ReceiveMessageResult()
						.withMessages(new com.amazonaws.services.sqs.model.Message()
								.withBody("Hello")
								.withMessageAttributes(Collections.singletonMap(
										MessageHeaders.CONTENT_TYPE,
										new MessageAttributeValue()
												.withDataType(
														MessageAttributeDataTypes.STRING)
												.withStringValue(mimeType.toString())))));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		assertThat(receivedMessage.getHeaders().get(MessageHeaders.CONTENT_TYPE))
				.isEqualTo(mimeType);
	}

	@Test
	public void sendMessage_withStringMessageHeader_shouldBeSentAsQueueMessageAttribute()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");
		String headerValue = "Header value";
		String headerName = "MyHeader";
		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader(headerName, headerValue).build();

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(new SendMessageResult());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(sendMessageRequestArgumentCaptor.getValue().getMessageAttributes()
				.get(headerName).getStringValue()).isEqualTo(headerValue);
		assertThat(sendMessageRequestArgumentCaptor.getValue().getMessageAttributes()
				.get(headerName).getDataType())
						.isEqualTo(MessageAttributeDataTypes.STRING);
	}

	@Test
	public void receiveMessage_withStringMessageHeader_shouldBeReceivedAsQueueMessageAttribute()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		String headerValue = "Header value";
		String headerName = "MyHeader";
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue")
				.withWaitTimeSeconds(0).withMaxNumberOfMessages(1)
				.withAttributeNames(QueueMessageChannel.ATTRIBUTE_NAMES)
				.withMessageAttributeNames("All")))
						.thenReturn(new ReceiveMessageResult().withMessages(
								new com.amazonaws.services.sqs.model.Message()
										.withBody("Hello")
										.withMessageAttributes(Collections.singletonMap(
												headerName,
												new MessageAttributeValue().withDataType(
														MessageAttributeDataTypes.STRING)
														.withStringValue(headerValue)))));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		assertThat(receivedMessage.getHeaders().get(headerName)).isEqualTo(headerValue);
	}

	@Test
	public void sendMessage_withNumericMessageHeaders_shouldBeSentAsQueueMessageAttributes()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");
		double doubleValue = 1234.56;
		long longValue = 1234L;
		int integerValue = 1234;
		byte byteValue = 2;
		short shortValue = 12;
		float floatValue = 1234.56f;
		BigInteger bigIntegerValue = new BigInteger("616416546156");
		BigDecimal bigDecimalValue = new BigDecimal("7834938");

		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader("double", doubleValue).setHeader("long", longValue)
				.setHeader("integer", integerValue).setHeader("byte", byteValue)
				.setHeader("short", shortValue).setHeader("float", floatValue)
				.setHeader("bigInteger", bigIntegerValue)
				.setHeader("bigDecimal", bigDecimalValue).build();

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(new SendMessageResult());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		Map<String, MessageAttributeValue> messageAttributes = sendMessageRequestArgumentCaptor
				.getValue().getMessageAttributes();
		assertThat(messageAttributes.get("double").getDataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Double");
		assertThat(messageAttributes.get("double").getStringValue())
				.isEqualTo(String.valueOf(doubleValue));
		assertThat(messageAttributes.get("long").getDataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Long");
		assertThat(messageAttributes.get("long").getStringValue())
				.isEqualTo(String.valueOf(longValue));
		assertThat(messageAttributes.get("integer").getDataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Integer");
		assertThat(messageAttributes.get("integer").getStringValue())
				.isEqualTo(String.valueOf(integerValue));
		assertThat(messageAttributes.get("byte").getDataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Byte");
		assertThat(messageAttributes.get("byte").getStringValue())
				.isEqualTo(String.valueOf(byteValue));
		assertThat(messageAttributes.get("short").getDataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Short");
		assertThat(messageAttributes.get("short").getStringValue())
				.isEqualTo(String.valueOf(shortValue));
		assertThat(messageAttributes.get("float").getDataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Float");
		assertThat(messageAttributes.get("float").getStringValue())
				.isEqualTo(String.valueOf(floatValue));
		assertThat(messageAttributes.get("bigInteger").getDataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.math.BigInteger");
		assertThat(messageAttributes.get("bigInteger").getStringValue())
				.isEqualTo(String.valueOf(bigIntegerValue));
		assertThat(messageAttributes.get("bigDecimal").getDataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.math.BigDecimal");
		assertThat(messageAttributes.get("bigDecimal").getStringValue())
				.isEqualTo(String.valueOf(bigDecimalValue));
	}

	@Test
	public void receiveMessage_withNumericMessageHeaders_shouldBeReceivedAsQueueMessageAttributes()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);

		HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		double doubleValue = 1234.56;
		messageAttributes.put("double",
				new MessageAttributeValue()
						.withDataType(
								MessageAttributeDataTypes.NUMBER + ".java.lang.Double")
						.withStringValue(String.valueOf(doubleValue)));
		long longValue = 1234L;
		messageAttributes.put("long",
				new MessageAttributeValue()
						.withDataType(
								MessageAttributeDataTypes.NUMBER + ".java.lang.Long")
						.withStringValue(String.valueOf(longValue)));
		int integerValue = 1234;
		messageAttributes.put("integer",
				new MessageAttributeValue()
						.withDataType(
								MessageAttributeDataTypes.NUMBER + ".java.lang.Integer")
						.withStringValue(String.valueOf(integerValue)));
		byte byteValue = 2;
		messageAttributes.put("byte",
				new MessageAttributeValue()
						.withDataType(
								MessageAttributeDataTypes.NUMBER + ".java.lang.Byte")
						.withStringValue(String.valueOf(byteValue)));
		short shortValue = 12;
		messageAttributes.put("short",
				new MessageAttributeValue()
						.withDataType(
								MessageAttributeDataTypes.NUMBER + ".java.lang.Short")
						.withStringValue(String.valueOf(shortValue)));
		float floatValue = 1234.56f;
		messageAttributes.put("float",
				new MessageAttributeValue()
						.withDataType(
								MessageAttributeDataTypes.NUMBER + ".java.lang.Float")
						.withStringValue(String.valueOf(floatValue)));
		BigInteger bigIntegerValue = new BigInteger("616416546156");
		messageAttributes.put("bigInteger", new MessageAttributeValue()
				.withDataType(MessageAttributeDataTypes.NUMBER + ".java.math.BigInteger")
				.withStringValue(String.valueOf(bigIntegerValue)));
		BigDecimal bigDecimalValue = new BigDecimal("7834938");
		messageAttributes.put("bigDecimal", new MessageAttributeValue()
				.withDataType(MessageAttributeDataTypes.NUMBER + ".java.math.BigDecimal")
				.withStringValue(String.valueOf(bigDecimalValue)));

		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue")
				.withWaitTimeSeconds(0).withMaxNumberOfMessages(1)
				.withAttributeNames(QueueMessageChannel.ATTRIBUTE_NAMES)
				.withMessageAttributeNames("All")))
						.thenReturn(new ReceiveMessageResult().withMessages(
								new com.amazonaws.services.sqs.model.Message()
										.withBody("Hello")
										.withMessageAttributes(messageAttributes)));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		assertThat(receivedMessage.getHeaders().get("double")).isEqualTo(doubleValue);
		assertThat(receivedMessage.getHeaders().get("long")).isEqualTo(longValue);
		assertThat(receivedMessage.getHeaders().get("integer")).isEqualTo(integerValue);
		assertThat(receivedMessage.getHeaders().get("byte")).isEqualTo(byteValue);
		assertThat(receivedMessage.getHeaders().get("short")).isEqualTo(shortValue);
		assertThat(receivedMessage.getHeaders().get("float")).isEqualTo(floatValue);
		assertThat(receivedMessage.getHeaders().get("bigInteger"))
				.isEqualTo(bigIntegerValue);
		assertThat(receivedMessage.getHeaders().get("bigDecimal"))
				.isEqualTo(bigDecimalValue);
	}

	@Test
	public void receiveMessage_withIncompatibleNumericMessageHeader_shouldThrowAnException()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(
				"Cannot convert String [17] to target class [java.util.concurrent.atomic.AtomicInteger]");

		HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		AtomicInteger atomicInteger = new AtomicInteger(17);
		messageAttributes.put("atomicInteger",
				new MessageAttributeValue()
						.withDataType(MessageAttributeDataTypes.NUMBER
								+ ".java.util.concurrent.atomic.AtomicInteger")
						.withStringValue(String.valueOf(atomicInteger)));

		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue")
				.withWaitTimeSeconds(0).withMaxNumberOfMessages(1)
				.withAttributeNames(QueueMessageChannel.ATTRIBUTE_NAMES)
				.withMessageAttributeNames("All")))
						.thenReturn(new ReceiveMessageResult().withMessages(
								new com.amazonaws.services.sqs.model.Message()
										.withBody("Hello")
										.withMessageAttributes(messageAttributes)));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		messageChannel.receive();
	}

	@Test
	public void receiveMessage_withMissingNumericMessageHeaderTargetClass_shouldThrowAnException()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		this.expectedException.expect(MessagingException.class);
		this.expectedException.expectMessage(
				"Message attribute with value '12' and data type 'Number.class.not.Found' could not be converted"
						+ " into a Number because target class was not found.");

		HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		messageAttributes.put("classNotFound",
				new MessageAttributeValue()
						.withDataType(
								MessageAttributeDataTypes.NUMBER + ".class.not.Found")
						.withStringValue("12"));

		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue")
				.withWaitTimeSeconds(0).withMaxNumberOfMessages(1)
				.withAttributeNames(QueueMessageChannel.ATTRIBUTE_NAMES)
				.withMessageAttributeNames("All")))
						.thenReturn(new ReceiveMessageResult().withMessages(
								new com.amazonaws.services.sqs.model.Message()
										.withBody("Hello")
										.withMessageAttributes(messageAttributes)));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		messageChannel.receive();
	}

	@Test
	public void sendMessage_withBinaryMessageHeader_shouldBeSentAsBinaryMessageAttribute()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");
		ByteBuffer headerValue = ByteBuffer.wrap("My binary data!".getBytes());
		String headerName = "MyHeader";
		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader(headerName, headerValue).build();

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(new SendMessageResult());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(sendMessageRequestArgumentCaptor.getValue().getMessageAttributes()
				.get(headerName).getBinaryValue()).isEqualTo(headerValue);
		assertThat(sendMessageRequestArgumentCaptor.getValue().getMessageAttributes()
				.get(headerName).getDataType())
						.isEqualTo(MessageAttributeDataTypes.BINARY);
	}

	@Test
	public void receiveMessage_withBinaryMessageHeader_shouldBeReceivedAsByteBufferMessageAttribute()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		ByteBuffer headerValue = ByteBuffer.wrap("My binary data!".getBytes());
		String headerName = "MyHeader";
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue")
				.withWaitTimeSeconds(0).withMaxNumberOfMessages(1)
				.withAttributeNames(QueueMessageChannel.ATTRIBUTE_NAMES)
				.withMessageAttributeNames("All")))
						.thenReturn(new ReceiveMessageResult().withMessages(
								new com.amazonaws.services.sqs.model.Message()
										.withBody("Hello")
										.withMessageAttributes(Collections.singletonMap(
												headerName,
												new MessageAttributeValue().withDataType(
														MessageAttributeDataTypes.BINARY)
														.withBinaryValue(headerValue)))));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		assertThat(receivedMessage.getHeaders().get(headerName)).isEqualTo(headerValue);
	}

	@Test
	public void sendMessage_withUuidAsId_shouldConvertUuidToString() throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");
		Message<String> message = MessageBuilder.withPayload("Hello").build();
		UUID uuid = (UUID) message.getHeaders().get(MessageHeaders.ID);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(new SendMessageResult());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(sendMessageRequestArgumentCaptor.getValue().getMessageAttributes()
				.get(MessageHeaders.ID).getStringValue()).isEqualTo(uuid.toString());
	}

	@Test
	public void receiveMessage_withIdOfTypeString_IdShouldBeConvertedToUuid()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		UUID uuid = UUID.randomUUID();
		when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue")
				.withWaitTimeSeconds(0).withMaxNumberOfMessages(1)
				.withAttributeNames(QueueMessageChannel.ATTRIBUTE_NAMES)
				.withMessageAttributeNames("All"))).thenReturn(new ReceiveMessageResult()
						.withMessages(new com.amazonaws.services.sqs.model.Message()
								.withBody("Hello")
								.withMessageAttributes(Collections.singletonMap(
										MessageHeaders.ID,
										new MessageAttributeValue()
												.withDataType(
														MessageAttributeDataTypes.STRING)
												.withStringValue(uuid.toString())))));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		Object idMessageHeader = receivedMessage.getHeaders().get(MessageHeaders.ID);
		assertThat(UUID.class.isInstance(idMessageHeader)).isTrue();
		assertThat(idMessageHeader).isEqualTo(uuid);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void sendMessage_withTimeout_sendsMessageAsyncAndReturnsTrueOnceFutureCompleted()
			throws Exception {
		// Arrange
		Future<SendMessageResult> future = mock(Future.class);
		when(future.get(1000, TimeUnit.MILLISECONDS)).thenReturn(new SendMessageResult());
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		when(amazonSqs.sendMessageAsync(any(SendMessageRequest.class)))
				.thenReturn(future);
		QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		boolean result = queueMessageChannel
				.send(MessageBuilder.withPayload("Hello").build(), 1000);

		// Assert
		assertThat(result).isTrue();
		verify(amazonSqs, only()).sendMessageAsync(any(SendMessageRequest.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void sendMessage_withSendMessageAsyncTakingMoreTimeThanSpecifiedTimeout_returnsFalse()
			throws Exception {
		// Arrange
		Future<SendMessageResult> future = mock(Future.class);
		when(future.get(1000, TimeUnit.MILLISECONDS)).thenThrow(new TimeoutException());
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		when(amazonSqs.sendMessageAsync(any(SendMessageRequest.class)))
				.thenReturn(future);
		QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Act
		boolean result = queueMessageChannel
				.send(MessageBuilder.withPayload("Hello").build(), 1000);

		// Assert
		assertThat(result).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void sendMessage_withExecutionExceptionWhileSendingAsyncMessage_throwMessageDeliveryException()
			throws Exception {
		// Arrange
		Future<SendMessageResult> future = mock(Future.class);
		when(future.get(1000, TimeUnit.MILLISECONDS))
				.thenThrow(new ExecutionException(new Exception()));
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		when(amazonSqs.sendMessageAsync(any(SendMessageRequest.class)))
				.thenReturn(future);
		QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");

		// Assert
		this.expectedException.expect(MessageDeliveryException.class);

		// Act
		queueMessageChannel.send(MessageBuilder.withPayload("Hello").build(), 1000);

	}

	@Test
	public void sendMessage_withDelayHeader_shouldSetDelayOnSendMessageRequestAndNotSetItAsHeaderAsMessageAttribute()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(new SendMessageResult());

		QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");
		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader(SqsMessageHeaders.SQS_DELAY_HEADER, 15).build();

		// Act
		queueMessageChannel.send(message);

		// Assert
		SendMessageRequest sendMessageRequest = sendMessageRequestArgumentCaptor
				.getValue();
		assertThat(sendMessageRequest.getDelaySeconds()).isEqualTo(new Integer(15));
		assertThat(sendMessageRequest.getMessageAttributes()
				.containsKey(SqsMessageHeaders.SQS_DELAY_HEADER)).isFalse();
	}

	@Test
	public void sendMessage_withoutDelayHeader_shouldNotSetDelayOnSendMessageRequestAndNotSetHeaderAsMessageAttribute()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(new SendMessageResult());

		QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");
		Message<String> message = MessageBuilder.withPayload("Hello").build();

		// Act
		queueMessageChannel.send(message);

		// Assert
		SendMessageRequest sendMessageRequest = sendMessageRequestArgumentCaptor
				.getValue();
		assertThat(sendMessageRequest.getDelaySeconds()).isNull();
		assertThat(sendMessageRequest.getMessageAttributes()
				.containsKey(SqsMessageHeaders.SQS_DELAY_HEADER)).isFalse();
	}

	@Test
	public void sendMessage_withGroupIdHeader_shouldSetGroupIdOnSendMessageRequestAndNotSetItAsHeaderAsMessageAttribute()
			throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(new SendMessageResult());

		QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");
		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader(SqsMessageHeaders.SQS_GROUP_ID_HEADER, "id-5").build();

		// Act
		queueMessageChannel.send(message);

		// Assert
		SendMessageRequest sendMessageRequest = sendMessageRequestArgumentCaptor
				.getValue();
		assertThat(sendMessageRequest.getMessageGroupId()).isEqualTo("id-5");
		assertThat(sendMessageRequest.getMessageAttributes()
				.containsKey(SqsMessageHeaders.SQS_GROUP_ID_HEADER)).isFalse();
	}

	// @checkstyle:off
	@Test
	public void sendMessage_withDeduplicationIdHeader_shouldSetDeduplicationIdOnSendMessageRequestAndNotSetItAsHeaderAsMessageAttribute()
			throws Exception {
		// @checkstyle:on
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(new SendMessageResult());

		QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs,
				"http://testQueue");
		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader(SqsMessageHeaders.SQS_DEDUPLICATION_ID_HEADER, "id-5").build();

		// Act
		queueMessageChannel.send(message);

		// Assert
		SendMessageRequest sendMessageRequest = sendMessageRequestArgumentCaptor
				.getValue();
		assertThat(sendMessageRequest.getMessageDeduplicationId()).isEqualTo("id-5");
		assertThat(sendMessageRequest.getMessageAttributes()
				.containsKey(SqsMessageHeaders.SQS_DEDUPLICATION_ID_HEADER)).isFalse();
	}

}
