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

import static io.awspring.cloud.sns.core.Matchers.requestMatches;
import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER;
import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_GROUP_ID_HEADER;
import static io.awspring.cloud.sns.core.SnsHeaders.NOTIFICATION_SUBJECT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

/**
 * @author Alain Sahli
 */
class TopicMessageChannelTest {
	private static final String TOPIC_ARN = "arn:aws:sns:eu-west:123456789012:test";

	private final SnsClient snsClient = mock(SnsClient.class);
	private final MessageChannel messageChannel = new TopicMessageChannel(snsClient, Arn.fromString(TOPIC_ARN));

	@Test
	void sendMessage_validTextMessageAndSubject_returnsTrue() throws Exception {
		// Arrange
		Message<String> stringMessage = MessageBuilder.withPayload("Message content")
				.setHeader(NOTIFICATION_SUBJECT_HEADER, "Subject").build();

		// Act
		boolean sent = messageChannel.send(stringMessage);

		// Assert
		verify(snsClient, only()).publish(requestMatches(it -> {
			assertThat(it.subject()).isEqualTo("Subject");
			assertThat(it.topicArn()).isEqualTo(TOPIC_ARN);
			assertThat(it.message()).isEqualTo("Message content");
			assertThat(it.messageAttributes()).containsKeys(MessageHeaders.ID, MessageHeaders.TIMESTAMP);
			assertThat(it.messageAttributes()).doesNotContainKey(NOTIFICATION_SUBJECT_HEADER);
		}));
		assertThat(sent).isTrue();
	}

	@Test
	void sendMessage_validTextMessageWithoutSubject_returnsTrue() {
		// Arrange
		Message<String> stringMessage = MessageBuilder.withPayload("Message content").build();

		// Act
		boolean sent = messageChannel.send(stringMessage);

		// Assert
		verify(snsClient, only()).publish(requestMatches(it -> {
			assertThat(it.subject()).isNull();
			assertThat(it.topicArn()).isEqualTo(TOPIC_ARN);
			assertThat(it.message()).isEqualTo("Message content");
			assertThat(it.messageAttributes().keySet()).contains(MessageHeaders.ID, MessageHeaders.TIMESTAMP);
		}));
		assertThat(sent).isTrue();
	}

	@Test
	void sendMessage_withStringMessageHeader_shouldBeSentAsTopicMessageAttribute() {
		// Arrange
		String headerValue = "Header value";
		String headerName = "MyHeader";
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(headerName, headerValue).build();

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		verify(snsClient).publish(requestMatches(it -> {
			assertThat(it.messageAttributes().get(headerName).stringValue()).isEqualTo(headerValue);
			assertThat(it.messageAttributes().get(headerName).dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		}));
		assertThat(sent).isTrue();
	}

	@Test
	void sendMessage_withNumericMessageHeaders_shouldBeSentAsTopicMessageAttributes() {
		// Arrange
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

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		verify(snsClient).publish(requestMatches(it -> {
			Map<String, MessageAttributeValue> attrs = it.messageAttributes();
			assertThat(attrs.get("double").dataType())
					.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Double");
			assertThat(attrs.get("double").stringValue()).isEqualTo(String.valueOf(doubleValue));
			assertThat(attrs.get("long").dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Long");
			assertThat(attrs.get("long").stringValue()).isEqualTo(String.valueOf(longValue));
			assertThat(attrs.get("integer").dataType())
					.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Integer");
			assertThat(attrs.get("integer").stringValue()).isEqualTo(String.valueOf(integerValue));
			assertThat(attrs.get("byte").dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Byte");
			assertThat(attrs.get("byte").stringValue()).isEqualTo(String.valueOf(byteValue));
			assertThat(attrs.get("short").dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Short");
			assertThat(attrs.get("short").stringValue()).isEqualTo(String.valueOf(shortValue));
			assertThat(attrs.get("float").dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Float");
			assertThat(attrs.get("float").stringValue()).isEqualTo(String.valueOf(floatValue));
			assertThat(attrs.get("bigInteger").dataType())
					.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.math.BigInteger");
			assertThat(attrs.get("bigInteger").stringValue()).isEqualTo(String.valueOf(bigIntegerValue));
			assertThat(attrs.get("bigDecimal").dataType())
					.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.math.BigDecimal");
			assertThat(attrs.get("bigDecimal").stringValue()).isEqualTo(String.valueOf(bigDecimalValue));
		}));
		assertThat(sent).isTrue();
	}

	@Test
	void sendMessage_withBinaryMessageHeader_shouldBeSentAsBinaryMessageAttribute() {
		// Arrange
		ByteBuffer headerValue = ByteBuffer.wrap("My binary data!".getBytes());
		String headerName = "MyHeader";
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(headerName, headerValue).build();

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		verify(snsClient).publish(requestMatches(it -> {
			assertThat(it.messageAttributes().get(headerName).binaryValue().asByteBuffer()).isEqualTo(headerValue);
			assertThat(it.messageAttributes().get(headerName).dataType()).isEqualTo(MessageAttributeDataTypes.BINARY);
		}));
		assertThat(sent).isTrue();

	}

	@Test
	void sendMessage_withUuidAsId_shouldConvertUuidToString() {
		// Arrange
		Message<String> message = MessageBuilder.withPayload("Hello").build();
		UUID uuid = (UUID) message.getHeaders().get(MessageHeaders.ID);

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		verify(snsClient).publish(requestMatches(it -> {
			assertThat(it.messageAttributes().get(MessageHeaders.ID).stringValue()).isEqualTo(uuid.toString());
		}));
	}

	@Test
	void sendMessage_withStringArrayMessageHeader_shouldBeSentAsTopicMessageAttribute() {
		// Arrange
		List<String> headerValue = Arrays.asList("List", "\"of\"", "header", "values");
		String headerName = "MyHeader";
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(headerName, headerValue).build();

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		verify(snsClient).publish(requestMatches(it -> {
			assertThat(it.messageAttributes().get(headerName).stringValue())
					.isEqualTo("[\"List\", \"\\\"of\\\"\", \"header\", \"values\"]");
			assertThat(it.messageAttributes().get(headerName).dataType())
					.isEqualTo(MessageAttributeDataTypes.STRING_ARRAY);
		}));
	}

	@Test
	void sendMessage_withMessageGroupIdHeader_shouldSetMessageGroupIdOnPublishRequestAndNotSetItAsMessageAttribute() {
		// Arrange
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(MESSAGE_GROUP_ID_HEADER, "id-5")
				.build();

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		verify(snsClient).publish(requestMatches(it -> {
			assertThat(it.messageAttributes()).doesNotContainKey(MESSAGE_GROUP_ID_HEADER);
			assertThat(it.messageGroupId()).isEqualTo("id-5");
		}));
	}

	@Test
	void sendMessage_withMessageDeduplicationIdHeader_shouldSetMessageDeduplicationIdOnPublishRequestAndNotSetItAsMessageAttribute() {
		// Arrange
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(MESSAGE_DEDUPLICATION_ID_HEADER, "id-5")
				.build();

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		verify(snsClient).publish(requestMatches(it -> {
			assertThat(it.messageAttributes()).doesNotContainKey(MESSAGE_DEDUPLICATION_ID_HEADER);
			assertThat(it.messageDeduplicationId()).isEqualTo("id-5");
		}));
	}
}
