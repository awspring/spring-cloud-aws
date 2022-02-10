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

package io.awspring.cloud.v3.messaging.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.AdditionalAnswers.answersWithDelay;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class QueueMessageChannelTest {

	@Test
	void sendMessage_validTextMessage_returnsTrue() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
			.thenReturn(SendMessageResponse.builder().build());

		Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
		MessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		boolean sent = messageChannel.send(stringMessage);

		// Assert
		verify(amazonSqs, only()).sendMessage(any(SendMessageRequest.class));
		assertThat(sendMessageRequestArgumentCaptor.getValue().messageBody()).isEqualTo("message content");
		assertThat(sent).isTrue();
	}

	@Test
	void sendMessage_serviceThrowsError_throwsMessagingException() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);

		Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
		MessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		when(amazonSqs.sendMessage(
			SendMessageRequest.builder()
				.queueUrl("http://testQueue")
				.messageBody("message content")
				.delaySeconds(0)
				.messageAttributes(isNotNull())
				.build())).thenThrow(SdkClientException.create("wanted error"));

		// Assert
		assertThatThrownBy(() -> messageChannel.send(stringMessage)).isInstanceOf(MessagingException.class)
				.hasMessageContaining("wanted error");
	}

	@Test
	void sendMessage_withMimeTypeAsStringHeader_shouldPassItAsMessageAttribute() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		String mimeTypeAsString = new MimeType("test", "plain", Charset.forName("UTF-8")).toString();
		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader(MessageHeaders.CONTENT_TYPE, mimeTypeAsString).build();

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
			.thenReturn(SendMessageResponse.builder().build());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(sendMessageRequestArgumentCaptor.getValue().messageAttributes().get(MessageHeaders.CONTENT_TYPE)
				.stringValue()).isEqualTo(mimeTypeAsString);
	}

	@Test
	void sendMessage_withMimeTypeHeader_shouldPassItAsMessageAttribute() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		MimeType mimeType = new MimeType("test", "plain", Charset.forName("UTF-8"));
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(MessageHeaders.CONTENT_TYPE, mimeType)
				.build();

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
			.thenReturn(SendMessageResponse.builder().build());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(sendMessageRequestArgumentCaptor.getValue().messageAttributes().get(MessageHeaders.CONTENT_TYPE)
				.stringValue()).isEqualTo(mimeType.toString());
	}

	@Test
	void receiveMessage_withoutTimeout_returnsTextMessage() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		when(amazonSqs.receiveMessage(
			ReceiveMessageRequest.builder()
				.queueUrl("http://testQueue")
				.waitTimeSeconds(0)
				.maxNumberOfMessages(1)
				.attributeNames(QueueAttributeName.ALL)
				.messageAttributeNames("All")
				.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(Collections.singleton(software.amazon.awssdk.services.sqs.model.Message.builder()
						.body("content")
					.build()))
				.build());

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		assertThat(receivedMessage).isNotNull();
		assertThat(receivedMessage.getPayload()).isEqualTo("content");
	}

	@Test
	void receiveMessage_withSpecifiedTimeout_returnsTextMessage() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		when(amazonSqs.receiveMessage(
			ReceiveMessageRequest.builder()
				.queueUrl("http://testQueue")
				.maxNumberOfMessages(1)
				.waitTimeSeconds(2)
				.attributeNames(QueueAttributeName.ALL)
				.messageAttributeNames("All")
				.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(Collections.singleton(software.amazon.awssdk.services.sqs.model.Message.builder()
						.body("content")
					.build()))
				.build());

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive(2);

		// Assert
		assertThat(receivedMessage).isNotNull();
		assertThat(receivedMessage.getPayload()).isEqualTo("content");
	}

	@Test
	void receiveMessage_withSpecifiedTimeout_returnsNull() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		when(amazonSqs.receiveMessage(
				ReceiveMessageRequest.builder()
					.queueUrl("http://testQueue")
					.waitTimeSeconds(2)
					.maxNumberOfMessages(1)
					.attributeNames(QueueAttributeName.ALL)
					.messageAttributeNames("All")
				.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.build());

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive(2);

		// Assert
		assertThat(receivedMessage).isNull();
	}

	@Test
	void receiveMessage_withoutDefaultTimeout_returnsNull() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		when(amazonSqs.receiveMessage(
				ReceiveMessageRequest.builder()
					.queueUrl("http://testQueue")
					.waitTimeSeconds(0)
					.maxNumberOfMessages(1)
					.attributeNames(QueueAttributeName.ALL)
					.messageAttributeNames("All")
					.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.build());

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive(0);

		// Assert
		assertThat(receivedMessage).isNull();
	}

	@Test
	void receiveMessage_withMimeTypeMessageAttribute_shouldCopyToHeaders() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		MimeType mimeType = new MimeType("test", "plain", Charset.forName("UTF-8"));
		when(amazonSqs.receiveMessage(
			ReceiveMessageRequest.builder()
				.queueUrl("http://testQueue")
				.waitTimeSeconds(0)
				.maxNumberOfMessages(1)
				.attributeNames(QueueAttributeName.ALL)
				.messageAttributeNames("All")
				.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(Collections.singleton(software.amazon.awssdk.services.sqs.model.Message.builder()
					.body("Hello")
					.messageAttributes(Collections.singletonMap(MessageHeaders.CONTENT_TYPE, MessageAttributeValue.builder()
						.dataType(MessageAttributeDataTypes.STRING)
						.stringValue(mimeType.toString())
						.build()))
					.build()))
				.build());

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		assertThat(receivedMessage.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(mimeType);
	}

	@Test
	void sendMessage_withStringMessageHeader_shouldBeSentAsQueueMessageAttribute() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		String headerValue = "Header value";
		String headerName = "MyHeader";
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(headerName, headerValue).build();

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
			.thenReturn(SendMessageResponse.builder().build());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(sendMessageRequestArgumentCaptor.getValue().messageAttributes().get(headerName).stringValue())
				.isEqualTo(headerValue);
		assertThat(sendMessageRequestArgumentCaptor.getValue().messageAttributes().get(headerName).dataType())
				.isEqualTo(MessageAttributeDataTypes.STRING);
	}

	@Test
	void receiveMessage_withStringMessageHeader_shouldBeReceivedAsQueueMessageAttribute() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		String headerValue = "Header value";
		String headerName = "MyHeader";
		when(amazonSqs.receiveMessage(
				ReceiveMessageRequest.builder()
					.queueUrl("http://testQueue")
					.waitTimeSeconds(0)
					.maxNumberOfMessages(1)
					.attributeNames(QueueAttributeName.ALL)
					.messageAttributeNames("All")
					.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(software.amazon.awssdk.services.sqs.model.Message.builder()
					.body("Hello")
					.messageAttributes(Collections.singletonMap(headerName, MessageAttributeValue.builder()
							.dataType(MessageAttributeDataTypes.STRING)
							.stringValue(headerValue)
						.build()))
					.build())
				.build());

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		assertThat(receivedMessage.getHeaders().get(headerName)).isEqualTo(headerValue);
	}

	@Test
	void sendMessage_withNumericMessageHeaders_shouldBeSentAsQueueMessageAttributes() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		double doubleValue = 1234.56;
		long longValue = 1234L;
		int integerValue = 1234;
		byte byteValue = 2;
		short shortValue = 12;
		float floatValue = 1234.56f;
		BigInteger bigIntegerValue = new BigInteger("616416546156");
		BigDecimal bigDecimalValue = new BigDecimal("7834938");

		Message<String> message = MessageBuilder.withPayload("Hello").setHeader("double", doubleValue)
				.setHeader("long", longValue).setHeader("integer", integerValue).setHeader("byte", byteValue)
				.setHeader("short", shortValue).setHeader("float", floatValue).setHeader("bigInteger", bigIntegerValue)
				.setHeader("bigDecimal", bigDecimalValue).build();

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
			.thenReturn(SendMessageResponse.builder().build());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		Map<String, MessageAttributeValue> messageAttributes = sendMessageRequestArgumentCaptor.getValue()
				.messageAttributes();
		assertThat(messageAttributes.get("double").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Double");
		assertThat(messageAttributes.get("double").stringValue()).isEqualTo(String.valueOf(doubleValue));
		assertThat(messageAttributes.get("long").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Long");
		assertThat(messageAttributes.get("long").stringValue()).isEqualTo(String.valueOf(longValue));
		assertThat(messageAttributes.get("integer").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Integer");
		assertThat(messageAttributes.get("integer").stringValue()).isEqualTo(String.valueOf(integerValue));
		assertThat(messageAttributes.get("byte").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Byte");
		assertThat(messageAttributes.get("byte").stringValue()).isEqualTo(String.valueOf(byteValue));
		assertThat(messageAttributes.get("short").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Short");
		assertThat(messageAttributes.get("short").stringValue()).isEqualTo(String.valueOf(shortValue));
		assertThat(messageAttributes.get("float").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Float");
		assertThat(messageAttributes.get("float").stringValue()).isEqualTo(String.valueOf(floatValue));
		assertThat(messageAttributes.get("bigInteger").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.math.BigInteger");
		assertThat(messageAttributes.get("bigInteger").stringValue()).isEqualTo(String.valueOf(bigIntegerValue));
		assertThat(messageAttributes.get("bigDecimal").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.math.BigDecimal");
		assertThat(messageAttributes.get("bigDecimal").stringValue()).isEqualTo(String.valueOf(bigDecimalValue));
	}


	private static Stream<Arguments> numericMessageHeaders() {
		return Stream.of(
			Arguments.of("double", 1234.56d, MessageAttributeDataTypes.NUMBER + ".java.lang.Double")

		);
	}

	@Test
	void receiveMessage_withNumericMessageHeaders_shouldBeReceivedAsQueueMessageAttributes() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);

		HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		double doubleValue = 1234.56;
		messageAttributes.put("double",
				MessageAttributeValue.builder()
					.dataType(MessageAttributeDataTypes.NUMBER + ".java.lang.Double")
					.stringValue(String.valueOf(doubleValue))
					.build());

		long longValue = 1234L;
		messageAttributes.put("long",
			MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.NUMBER + ".java.lang.Long")
				.stringValue(String.valueOf(longValue))
				.build());

		int integerValue = 1234;
		messageAttributes.put("integer",
			MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.NUMBER + ".java.lang.Integer")
				.stringValue(String.valueOf(integerValue))
				.build());

		byte byteValue = 2;
		messageAttributes.put("byte",
			MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.NUMBER + ".java.lang.Byte")
				.stringValue(String.valueOf(byteValue))
				.build());

		short shortValue = 12;
		messageAttributes.put("short",
			MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.NUMBER + ".java.lang.Short")
				.stringValue(String.valueOf(shortValue))
				.build());

		float floatValue = 1234.56f;
		messageAttributes.put("float",
			MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.NUMBER + ".java.lang.Float")
				.stringValue(String.valueOf(floatValue))
				.build());

		BigInteger bigIntegerValue = new BigInteger("616416546156");
		messageAttributes.put("bigInteger",
			MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.NUMBER + ".java.math.BigInteger")
				.stringValue(String.valueOf(bigIntegerValue))
				.build());

		BigDecimal bigDecimalValue = new BigDecimal("7834938");
		messageAttributes.put("bigDecimal",
			MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.NUMBER + ".java.math.BigDecimal")
				.stringValue(String.valueOf(bigDecimalValue))
				.build());

		when(amazonSqs.receiveMessage(
				ReceiveMessageRequest.builder()
					.queueUrl("http://testQueue")
					.waitTimeSeconds(0)
					.maxNumberOfMessages(1)
					.attributeNames(QueueAttributeName.ALL)
					.messageAttributeNames("All")
					.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(software.amazon.awssdk.services.sqs.model.Message.builder()
					.body("Hello")
					.messageAttributes(messageAttributes)
					.build())
				.build());

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		assertThat(receivedMessage.getHeaders().get("double")).isEqualTo(doubleValue);
		assertThat(receivedMessage.getHeaders().get("long")).isEqualTo(longValue);
		assertThat(receivedMessage.getHeaders().get("integer")).isEqualTo(integerValue);
		assertThat(receivedMessage.getHeaders().get("byte")).isEqualTo(byteValue);
		assertThat(receivedMessage.getHeaders().get("short")).isEqualTo(shortValue);
		assertThat(receivedMessage.getHeaders().get("float")).isEqualTo(floatValue);
		assertThat(receivedMessage.getHeaders().get("bigInteger")).isEqualTo(bigIntegerValue);
		assertThat(receivedMessage.getHeaders().get("bigDecimal")).isEqualTo(bigDecimalValue);
	}

	@Test
	void receiveMessage_withIncompatibleNumericMessageHeader_shouldThrowAnException() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);

		HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		AtomicInteger atomicInteger = new AtomicInteger(17);
		messageAttributes.put("atomicInteger",
			MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.NUMBER + ".java.util.concurrent.atomic.AtomicInteger")
				.stringValue(String.valueOf(atomicInteger))
				.build());

		when(amazonSqs.receiveMessage(
				ReceiveMessageRequest.builder()
					.queueUrl("http://testQueue")
					.waitTimeSeconds(0)
					.maxNumberOfMessages(1)
					.attributeNames(QueueAttributeName.ALL)
					.messageAttributeNames("All")
					.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(software.amazon.awssdk.services.sqs.model.Message.builder()
					.body("Hello")
					.messageAttributes(messageAttributes)
					.build())
				.build());

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Assert
		assertThatThrownBy(messageChannel::receive).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
				"Cannot convert String [17] to target class [java.util.concurrent.atomic.AtomicInteger]");
	}

	@Test
	void receiveMessage_withMissingNumericMessageHeaderTargetClass_shouldThrowAnException() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);

		HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		messageAttributes.put("classNotFound", MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.NUMBER + ".class.not.Found")
				.stringValue("12")
			.build());

		when(amazonSqs.receiveMessage(
			ReceiveMessageRequest.builder()
					.queueUrl("http://testQueue")
					.waitTimeSeconds(0)
					.maxNumberOfMessages(1)
					.attributeNames(QueueAttributeName.ALL)
					.messageAttributeNames("All")
			.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(software.amazon.awssdk.services.sqs.model.Message.builder()
					.body("Hello")
					.messageAttributes(messageAttributes)
					.build())
				.build());

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Assert
		assertThatThrownBy(messageChannel::receive).isInstanceOf(MessagingException.class).hasMessageContaining(
				"Message attribute with value '12' and data type 'Number.class.not.Found' could not be converted"
						+ " into a Number because target class was not found.");
	}

	@Test
	void sendMessage_withBinaryMessageHeader_shouldBeSentAsBinaryMessageAttribute() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		ByteBuffer headerValue = ByteBuffer.wrap("My binary data!".getBytes());
		String headerName = "MyHeader";
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(headerName, headerValue).build();

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
			.thenReturn(SendMessageResponse.builder().build());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(sendMessageRequestArgumentCaptor.getValue().messageAttributes().get(headerName).binaryValue())
				.isEqualTo(SdkBytes.fromByteBuffer(headerValue));
		assertThat(sendMessageRequestArgumentCaptor.getValue().messageAttributes().get(headerName).dataType())
				.isEqualTo(MessageAttributeDataTypes.BINARY);
	}

	@Test
	void receiveMessage_withBinaryMessageHeader_shouldBeReceivedAsByteBufferMessageAttribute() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		ByteBuffer headerValue = ByteBuffer.wrap("My binary data!".getBytes());
		String headerName = "MyHeader";
		when(amazonSqs.receiveMessage(
				ReceiveMessageRequest.builder()
					.queueUrl("http://testQueue")
					.waitTimeSeconds(0)
					.maxNumberOfMessages(1)
					.attributeNames(QueueAttributeName.ALL)
					.messageAttributeNames("All")
					.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(software.amazon.awssdk.services.sqs.model.Message.builder()
					.body("Hello")
					.messageAttributes(Collections.singletonMap(headerName, MessageAttributeValue.builder()
							.dataType(MessageAttributeDataTypes.BINARY)
							.binaryValue(SdkBytes.fromByteBuffer(headerValue))
						.build()))
					.build())
				.build());

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		assertThat(receivedMessage.getHeaders().get(headerName)).isEqualTo(SdkBytes.fromByteBuffer(headerValue));
	}

	@Test
	void sendMessage_withUuidAsId_shouldConvertUuidToString() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		QueueMessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		Message<String> message = MessageBuilder.withPayload("Hello").build();
		UUID uuid = (UUID) message.getHeaders().get(MessageHeaders.ID);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture()))
			.thenReturn(SendMessageResponse.builder().build());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(sendMessageRequestArgumentCaptor.getValue().messageAttributes().get(MessageHeaders.ID)
				.stringValue()).isEqualTo(uuid.toString());
	}

	@Test
	void receiveMessage_withIdOfTypeString_IdShouldBeConvertedToUuid() throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);
		UUID uuid = UUID.randomUUID();
		when(amazonSqs.receiveMessage(
			ReceiveMessageRequest.builder()
					.queueUrl("http://testQueue")
					.waitTimeSeconds(0)
					.maxNumberOfMessages(1)
					.attributeNames(QueueAttributeName.ALL)
					.messageAttributeNames("All")
				.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(software.amazon.awssdk.services.sqs.model.Message.builder()
					.body("Hello")
					.messageAttributes(Collections.singletonMap(MessageHeaders.ID, MessageAttributeValue.builder()
							.dataType(MessageAttributeDataTypes.STRING)
							.stringValue(uuid.toString())
						.build()))
					.build())
				.build());

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		Message<?> receivedMessage = messageChannel.receive();

		// Assert
		Object idMessageHeader = receivedMessage.getHeaders().get(MessageHeaders.ID);
		assertThat(idMessageHeader).isInstanceOf(UUID.class).isEqualTo(uuid);
	}

    @Test
    @SuppressWarnings("unchecked")
    void sendMessage_withTimeout_sendsMessageAsyncAndReturnsTrueOnceFutureCompleted() throws Exception {
    	// Arrange
    	SendMessageResponse future = mock(SendMessageResponse.class);
    	SqsClient amazonSqs = mock(SqsClient.class);
    	when(amazonSqs.sendMessage(any(SendMessageRequest.class))).thenAnswer(answersWithDelay(1000, delegatesTo(future)));
    	QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

    	// Act
    	boolean result = queueMessageChannel.send(MessageBuilder.withPayload("Hello").build(), 1000);

    	// Assert
    	assertThat(result).isTrue();
    	verify(amazonSqs, only()).sendMessage(any(SendMessageRequest.class));
    }
//
//	@Test
//	@SuppressWarnings("unchecked")
//	void sendMessage_withSendMessageAsyncTakingMoreTimeThanSpecifiedTimeout_returnsFalse() throws Exception {
//		// Arrange
//		Future<SendMessageResult> future = mock(Future.class);
//		when(future.get(1000, TimeUnit.MILLISECONDS)).thenThrow(new TimeoutException());
//		SqsClient amazonSqs = mock(SqsClient.class);
//		when(amazonSqs.sendMessageAsync(any(SendMessageRequest.class))).thenReturn(future);
//		QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
//
//		// Act
//		boolean result = queueMessageChannel.send(MessageBuilder.withPayload("Hello").build(), 1000);
//
//		// Assert
//		assertThat(result).isFalse();
//	}

	// @Test
	// @SuppressWarnings("unchecked")
	// void sendMessage_withExecutionExceptionWhileSendingAsyncMessage_throwMessageDeliveryException() throws
	// Exception {
	// 	// Arrange
	// 	SendMessageResponse future = mock(SendMessageResponse.class);
	// 	when(future.get(1000, TimeUnit.MILLISECONDS)).thenThrow(new ExecutionException(new Exception()));
	// 	SqsClient amazonSqs = mock(SqsClient.class);
	// 	when(amazonSqs.sendMessage(any(SendMessageRequest.class))).thenReturn(future);
	// 	QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
// 
	// 	// Assert
	// 	assertThatThrownBy(() -> queueMessageChannel.send(MessageBuilder.withPayload("Hello").build(), 1000))
	// 			.isInstanceOf(MessageDeliveryException.class);
	// }

	@Test
	void sendMessage_withDelayHeader_shouldSetDelayOnSendMessageRequestAndNotSetItAsHeaderAsMessageAttribute()
			throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture())).thenReturn(SendMessageResponse.builder().build());

		QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(SqsMessageHeaders.SQS_DELAY_HEADER, 15)
				.build();

		// Act
		queueMessageChannel.send(message);

		// Assert
		SendMessageRequest sendMessageRequest = sendMessageRequestArgumentCaptor.getValue();
		assertThat(sendMessageRequest.delaySeconds()).isEqualTo(new Integer(15));
		assertThat(sendMessageRequest.messageAttributes().containsKey(SqsMessageHeaders.SQS_DELAY_HEADER)).isFalse();
	}

	@Test
	void sendMessage_withoutDelayHeader_shouldNotSetDelayOnSendMessageRequestAndNotSetHeaderAsMessageAttribute()
			throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture())).thenReturn(SendMessageResponse.builder().build());

		QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		Message<String> message = MessageBuilder.withPayload("Hello").build();

		// Act
		queueMessageChannel.send(message);

		// Assert
		SendMessageRequest sendMessageRequest = sendMessageRequestArgumentCaptor.getValue();
		assertThat(sendMessageRequest.delaySeconds()).isNull();
		assertThat(sendMessageRequest.messageAttributes().containsKey(SqsMessageHeaders.SQS_DELAY_HEADER)).isFalse();
	}

	@Test
	void sendMessage_withGroupIdHeader_shouldSetGroupIdOnSendMessageRequestAndNotSetItAsHeaderAsMessageAttribute()
			throws Exception {
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture())).thenReturn(SendMessageResponse.builder().build());

		QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader(SqsMessageHeaders.SQS_GROUP_ID_HEADER, "id-5").build();

		// Act
		queueMessageChannel.send(message);

		// Assert
		SendMessageRequest sendMessageRequest = sendMessageRequestArgumentCaptor.getValue();
		assertThat(sendMessageRequest.messageGroupId()).isEqualTo("id-5");
		assertThat(sendMessageRequest.messageAttributes().containsKey(SqsMessageHeaders.SQS_GROUP_ID_HEADER))
				.isFalse();
	}

	// @checkstyle:off
	@Test
	void sendMessage_withDeduplicationIdHeader_shouldSetDeduplicationIdOnSendMessageRequestAndNotSetItAsHeaderAsMessageAttribute()
			throws Exception {
		// @checkstyle:on
		// Arrange
		SqsClient amazonSqs = mock(SqsClient.class);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		when(amazonSqs.sendMessage(sendMessageRequestArgumentCaptor.capture())).thenReturn(SendMessageResponse.builder().build());

		QueueMessageChannel queueMessageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader(SqsMessageHeaders.SQS_DEDUPLICATION_ID_HEADER, "id-5").build();

		// Act
		queueMessageChannel.send(message);

		// Assert
		SendMessageRequest sendMessageRequest = sendMessageRequestArgumentCaptor.getValue();
		assertThat(sendMessageRequest.messageDeduplicationId()).isEqualTo("id-5");
		assertThat(sendMessageRequest.messageAttributes().containsKey(SqsMessageHeaders.SQS_DEDUPLICATION_ID_HEADER))
				.isFalse();
	}

}
