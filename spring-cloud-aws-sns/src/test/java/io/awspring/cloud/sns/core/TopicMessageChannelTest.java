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
package io.awspring.cloud.sns.core;

import static io.awspring.cloud.sns.core.MessageHeaderCodes.MESSAGE_DEDUPLICATION_ID_HEADER;
import static io.awspring.cloud.sns.core.MessageHeaderCodes.MESSAGE_GROUP_ID_HEADER;
import static io.awspring.cloud.sns.core.MessageHeaderCodes.NOTIFICATION_SUBJECT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * @author Alain Sahli
 */
public class TopicMessageChannelTest {

	@Test
	void sendMessage_validTextMessageAndSubject_returnsTrue() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);

		Message<String> stringMessage = MessageBuilder.withPayload("Message content")
				.setHeader(NOTIFICATION_SUBJECT_HEADER, "Subject").build();
		MessageChannel messageChannel = new TopicMessageChannel(snsClient, "topicArn");

		// Act
		boolean sent = messageChannel.send(stringMessage);

		// Assert
		verify(snsClient, only()).publish(PublishRequest.builder().topicArn("topicArn").message("Message content")
				.subject("Subject").messageAttributes(isNotNull()).build());
		assertThat(sent).isTrue();
	}

	@Test
	void sendMessage_validTextMessageWithoutSubject_returnsTrue() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);

		Message<String> stringMessage = MessageBuilder.withPayload("Message content").build();
		MessageChannel messageChannel = new TopicMessageChannel(snsClient, "topicArn");

		// Act
		boolean sent = messageChannel.send(stringMessage);

		// Assert
		verify(snsClient, only()).publish(PublishRequest.builder().topicArn("topicArn").message("Message content")
				.subject(null).messageAttributes(isNotNull()).build());
		assertThat(sent).isTrue();
	}

	@Test
	void sendMessage_validTextMessageAndTimeout_timeoutIsIgnored() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);

		Message<String> stringMessage = MessageBuilder.withPayload("Message content").build();
		MessageChannel messageChannel = new TopicMessageChannel(snsClient, "topicArn");

		// Act
		boolean sent = messageChannel.send(stringMessage, 10);

		// Assert
		verify(snsClient, only()).publish(PublishRequest.builder().topicArn("topicArn").message("Message content")
				.subject(null).messageAttributes(isNotNull()).build());
		assertThat(sent).isTrue();
	}

	@Test
	void sendMessage_withStringMessageHeader_shouldBeSentAsTopicMessageAttribute() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
		when(snsClient.publish(publishRequestArgumentCaptor.capture())).thenReturn(PublishResponse.builder().build());

		String headerValue = "Header value";
		String headerName = "MyHeader";
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(headerName, headerValue).build();
		MessageChannel messageChannel = new TopicMessageChannel(snsClient, "topicArn");

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(publishRequestArgumentCaptor.getValue().messageAttributes().get(headerName).stringValue())
				.isEqualTo(headerValue);
		assertThat(publishRequestArgumentCaptor.getValue().messageAttributes().get(headerName).dataType())
				.isEqualTo(MessageAttributeDataTypes.STRING);
	}

	@Test
	void sendMessage_withNumericMessageHeaders_shouldBeSentAsTopicMessageAttributes() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
		when(snsClient.publish(publishRequestArgumentCaptor.capture())).thenReturn(PublishResponse.builder().build());

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
		MessageChannel messageChannel = new TopicMessageChannel(snsClient, "topicArn");

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		Map<String, MessageAttributeValue> messageAttributes = publishRequestArgumentCaptor.getValue()
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

	@Test
	void sendMessage_withBinaryMessageHeader_shouldBeSentAsBinaryMessageAttribute() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
		when(snsClient.publish(publishRequestArgumentCaptor.capture())).thenReturn(PublishResponse.builder().build());

		ByteBuffer headerValue = ByteBuffer.wrap("My binary data!".getBytes());
		String headerName = "MyHeader";
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(headerName, headerValue).build();
		MessageChannel messageChannel = new TopicMessageChannel(snsClient, "topicArn");

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(publishRequestArgumentCaptor.getValue().messageAttributes().get(headerName).binaryValue()
				.asByteBuffer()).isEqualTo(headerValue);
		assertThat(publishRequestArgumentCaptor.getValue().messageAttributes().get(headerName).dataType())
				.isEqualTo(MessageAttributeDataTypes.BINARY);
	}

	@Test
	void sendMessage_withUuidAsId_shouldConvertUuidToString() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		TopicMessageChannel messageChannel = new TopicMessageChannel(snsClient, "http://testQueue");
		Message<String> message = MessageBuilder.withPayload("Hello").build();
		UUID uuid = (UUID) message.getHeaders().get(MessageHeaders.ID);

		ArgumentCaptor<PublishRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
		when(snsClient.publish(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(PublishResponse.builder().build());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(sendMessageRequestArgumentCaptor.getValue().messageAttributes().get(MessageHeaders.ID).stringValue())
				.isEqualTo(uuid.toString());
	}

	@Test
	public void sendMessage_withStringArrayMessageHeader_shouldBeSentAsTopicMessageAttribute() {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
		when(snsClient.publish(publishRequestArgumentCaptor.capture())).thenReturn(PublishResponse.builder().build());

		List<String> headerValue = Arrays.asList("List", "\"of\"", "header", "values");
		String headerName = "MyHeader";
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(headerName, headerValue).build();
		MessageChannel messageChannel = new TopicMessageChannel(snsClient, "topicArn");

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(publishRequestArgumentCaptor.getValue().messageAttributes().get(headerName).stringValue())
				.isEqualTo("[\"List\", \"\\\"of\\\"\", \"header\", \"values\"]");
		assertThat(publishRequestArgumentCaptor.getValue().messageAttributes().get(headerName).dataType())
				.isEqualTo(MessageAttributeDataTypes.STRING_ARRAY);
	}

	@Test
	public void sendMessage_withMessageGroupIdHeader_shouldSetMessageGroupIdOnPublishRequestAndNotSetItAsMessageAttribute() {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
		when(snsClient.publish(publishRequestArgumentCaptor.capture())).thenReturn(PublishResponse.builder().build());

		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(MESSAGE_GROUP_ID_HEADER, "id-5")
				.build();
		MessageChannel messageChannel = new TopicMessageChannel(snsClient, "topicArn");

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(publishRequestArgumentCaptor.getValue().messageAttributes().containsKey(MESSAGE_GROUP_ID_HEADER))
				.isFalse();
		assertThat(publishRequestArgumentCaptor.getValue().messageGroupId()).isEqualTo("id-5");
	}

	@Test
	public void sendMessage_withMessageDeduplicationIdHeader_shouldSetMessageDeduplicationIdOnPublishRequestAndNotSetItAsMessageAttribute() {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
		when(snsClient.publish(publishRequestArgumentCaptor.capture())).thenReturn(PublishResponse.builder().build());

		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(MESSAGE_DEDUPLICATION_ID_HEADER, "id-5")
				.build();
		MessageChannel messageChannel = new TopicMessageChannel(snsClient, "topicArn");

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(publishRequestArgumentCaptor.getValue().messageAttributes()
				.containsKey(MESSAGE_DEDUPLICATION_ID_HEADER)).isFalse();
		assertThat(publishRequestArgumentCaptor.getValue().messageDeduplicationId()).isEqualTo("id-5");
	}

}
