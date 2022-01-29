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

package io.awspring.cloud.messaging.support.converter;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.sns.message.SnsMessage;
import com.amazonaws.services.sns.message.SnsMessageManager;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Manuel Wessner
 * @since 1.0
 */
class NotificationRequestConverterTest {

	@Test
	void testWriteMessageNotSupported() throws Exception {
		assertThatThrownBy(
				() -> new NotificationRequestConverter(new StringMessageConverter(), mock(SnsMessageManager.class))
						.toMessage("test", null)).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void fromMessage_withoutMessage_shouldThrowAnException() throws Exception {
		assertThatThrownBy(
				() -> new NotificationRequestConverter(new StringMessageConverter(), mock(SnsMessageManager.class))
						.fromMessage(null, String.class)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void fromMessage_withMessageAndSubject_shouldReturnMessage() throws Exception {
		// Arrange
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Subject", "Hello");
		jsonObject.put("Message", "World");
		String payload = jsonObject.toString();

		// Act
		Object notificationRequest = new NotificationRequestConverter(new StringMessageConverter(),
				mock(SnsMessageManager.class)).fromMessage(MessageBuilder.withPayload(payload).build(), String.class);

		// Assert
		assertThat(notificationRequest).isInstanceOf(NotificationRequestConverter.NotificationRequest.class);
		assertThat(((NotificationRequestConverter.NotificationRequest) notificationRequest).getSubject())
				.isEqualTo("Hello");
		assertThat(((NotificationRequestConverter.NotificationRequest) notificationRequest).getMessage())
				.isEqualTo("World");
	}

	@Test
	void fromMessage_withMessageOnly_shouldReturnMessage() throws Exception {
		// Arrange
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Message", "World");
		String payload = jsonObject.toString();

		// Act
		Object notificationRequest = new NotificationRequestConverter(new StringMessageConverter(),
				mock(SnsMessageManager.class)).fromMessage(MessageBuilder.withPayload(payload).build(), String.class);

		// Assert
		assertThat(notificationRequest).isInstanceOf(NotificationRequestConverter.NotificationRequest.class);
		assertThat(((NotificationRequestConverter.NotificationRequest) notificationRequest).getMessage())
				.isEqualTo("World");
	}

	@Test
	void fromMessage_withNumberAttribute_shouldReturnMessage() throws Exception {
		// Arrange
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Message", "World");
		ObjectNode messageAttributes = JsonNodeFactory.instance.objectNode();
		messageAttributes.set("number-attribute",
				JsonNodeFactory.instance.objectNode().put("Value", "30").put("Type", "Number.long"));
		jsonObject.set("MessageAttributes", messageAttributes);
		String payload = jsonObject.toString();

		// Act
		Object notificationRequest = new NotificationRequestConverter(new StringMessageConverter(),
				mock(SnsMessageManager.class)).fromMessage(MessageBuilder.withPayload(payload).build(), String.class);

		// Assert
		assertThat(notificationRequest).isNotNull();
	}

	@Test
	void fromMessage_withValidSignature_shouldReturnMessage() {
		// Arrange
		SnsMessageManager messageManagerMock = mock(SnsMessageManager.class);
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Message", "World");
		jsonObject.put("Subject", "A subject");
		jsonObject.put("SignatureVersion", "1");
		jsonObject.put("Signature", "aValidSignature");
		jsonObject.put("SigningCertURL",
				"https://sns.eu-central-1.amazonaws.com/SimpleNotificationService-732423r4fdsfasdf69ffabfda.pem");
		String payload = jsonObject.toString();
		when(messageManagerMock.parseMessage(any())).thenReturn(mock(SnsMessage.class));

		// Act
		Object notificationRequest = new NotificationRequestConverter(new StringMessageConverter(), messageManagerMock)
				.fromMessage(MessageBuilder.withPayload(payload).build(), String.class);

		// Assert
		assertThat(((NotificationRequestConverter.NotificationRequest) notificationRequest).getMessage())
				.isEqualTo("World");
	}

	@Test
	void fromMessage_withInvalidSignature_shouldThrowAnException() {
		// Arrange
		SnsMessageManager messageManagerMock = mock(SnsMessageManager.class);
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Message", "World");
		jsonObject.put("Subject", "A subject");
		jsonObject.put("SignatureVersion", "1");
		jsonObject.put("Signature", "invalidSignature");
		jsonObject.put("SigningCertURL",
				"https://sns.eu-central-1.amazonaws.com/SimpleNotificationService-732423r4fdsfasdf69abfda.pem");
		String payload = jsonObject.toString();
		when(messageManagerMock.parseMessage(any()))
				.thenThrow(new SdkClientException("Signature in SNS message was invalid"));

		// Act + Assert
		assertThatThrownBy(() -> new NotificationRequestConverter(new StringMessageConverter(), messageManagerMock)
				.fromMessage(MessageBuilder.withPayload(payload).build(), String.class))
						.isInstanceOf(SdkClientException.class);
	}

	@Test
	void testNoTypeSupplied() throws Exception {
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Message", "Hello World!");
		String payload = jsonObject.toString();

		assertThatThrownBy(
				() -> new NotificationRequestConverter(new StringMessageConverter(), mock(SnsMessageManager.class))
						.fromMessage(MessageBuilder.withPayload(payload).build(), String.class))
								.isInstanceOf(MessageConversionException.class)
								.hasMessageContaining("does not contain a Type attribute");

	}

	@Test
	void testWrongTypeSupplied() throws Exception {
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Subscription");
		jsonObject.put("Message", "Hello World!");

		String payload = jsonObject.toString();

		assertThatThrownBy(
				() -> new NotificationRequestConverter(new StringMessageConverter(), mock(SnsMessageManager.class))
						.fromMessage(MessageBuilder.withPayload(payload).build(), String.class))
								.isInstanceOf(MessageConversionException.class)
								.hasMessageContaining("is not a valid notification");
	}

	@Test
	void testNoMessageAvailableSupplied() throws Exception {
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Subject", "Hello World!");
		String payload = jsonObject.toString();

		assertThatThrownBy(
				() -> new NotificationRequestConverter(new StringMessageConverter(), mock(SnsMessageManager.class))
						.fromMessage(MessageBuilder.withPayload(payload).build(), String.class))
								.isInstanceOf(MessageConversionException.class)
								.hasMessageContaining("does not contain a message");
	}

	@Test
	void testNoValidJson() throws Exception {
		String message = "foo";
		assertThatThrownBy(
				() -> new NotificationRequestConverter(new StringMessageConverter(), mock(SnsMessageManager.class))
						.fromMessage(MessageBuilder.withPayload(message).build(), String.class))
								.isInstanceOf(MessageConversionException.class)
								.hasMessageContaining("Could not read JSON");
	}

}
