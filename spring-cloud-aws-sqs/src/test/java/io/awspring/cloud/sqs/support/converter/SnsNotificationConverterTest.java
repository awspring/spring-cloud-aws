/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.support.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;

/**
 * Tests for {@link SnsNotificationConverter}.
 *
 * @author Damien Chomat
 */
class SnsNotificationConverterTest {

	private SnsNotificationConverter converter;

	@Mock
	private MessageConverter payloadConverter;

	@Mock
	private MethodParameter methodParameter;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		converter = new SnsNotificationConverter(payloadConverter, objectMapper);
	}

	@Test
	void shouldConvertSnsNotification() {
		String snsJson = "{" + "\"Type\": \"Notification\"," + "\"MessageId\": \"message-id\","
				+ "\"TopicArn\": \"topic-arn\"," + "\"Subject\": \"subject\"," + "\"Message\": \"message\","
				+ "\"Timestamp\": \"2023-01-01T00:00:00Z\"," + "\"SequenceNumber\": \"123456789\","
				+ "\"UnsubscribeURL\": \"https://sns.us-east-1.amazonaws.com/unsubscribe\","
				+ "\"Signature\": \"signature-value\"," + "\"SignatureVersion\": \"1\","
				+ "\"SigningCertURL\": \"https://sns.us-east-1.amazonaws.com/cert\"," + "\"MessageAttributes\": {"
				+ "  \"key\": {" + "    \"Type\": \"String\"," + "    \"Value\": \"value\"" + "  }" + "}" + "}";
		Message<String> message = new GenericMessage<>(snsJson);

		when(payloadConverter.fromMessage(any(), eq(String.class))).thenReturn("message");

		Object result = converter.fromMessage(message, SnsNotification.class);

		assertThat(result).isInstanceOf(SnsNotification.class);
		SnsNotification<String> notification = (SnsNotification<String>) result;
		assertThat(notification.getMessageId()).isEqualTo("message-id");
		assertThat(notification.getTopicArn()).isEqualTo("topic-arn");
		assertThat(notification.getSubject()).isEqualTo(Optional.of("subject"));
		assertThat(notification.getMessage()).isEqualTo("message");
		assertThat(notification.getTimestamp()).isEqualTo("2023-01-01T00:00:00Z");
		assertThat(notification.getSequenceNumber()).isEqualTo(Optional.of("123456789"));
		assertThat(notification.getUnsubscribeUrl())
				.isEqualTo(Optional.of("https://sns.us-east-1.amazonaws.com/unsubscribe"));
		assertThat(notification.getSignature()).isEqualTo(Optional.of("signature-value"));
		assertThat(notification.getSignatureVersion()).isEqualTo(Optional.of("1"));
		assertThat(notification.getSigningCertURL()).isEqualTo(Optional.of("https://sns.us-east-1.amazonaws.com/cert"));
		assertThat(notification.getMessageAttributes()).hasSize(1);
		assertThat(notification.getMessageAttributes().get("key").getType()).isEqualTo("String");
		assertThat(notification.getMessageAttributes().get("key").getValue()).isEqualTo("value");
	}

	@Test
	void shouldConvertSnsNotificationWithoutMessageAttributes() {
		String snsJson = "{" + "\"Type\": \"Notification\"," + "\"MessageId\": \"message-id\","
				+ "\"TopicArn\": \"topic-arn\"," + "\"Subject\": \"subject\"," + "\"Message\": \"message\","
				+ "\"Timestamp\": \"2023-01-01T00:00:00Z\"" + "}";
		Message<String> message = new GenericMessage<>(snsJson);

		when(payloadConverter.fromMessage(any(), eq(String.class))).thenReturn("message");

		Object result = converter.fromMessage(message, SnsNotification.class);

		assertThat(result).isInstanceOf(SnsNotification.class);
		SnsNotification<String> notification = (SnsNotification<String>) result;
		assertThat(notification.getMessageId()).isEqualTo("message-id");
		assertThat(notification.getTopicArn()).isEqualTo("topic-arn");
		assertThat(notification.getSubject()).isEqualTo(Optional.of("subject"));
		assertThat(notification.getMessage()).isEqualTo("message");
		assertThat(notification.getTimestamp()).isEqualTo("2023-01-01T00:00:00Z");
		assertThat(notification.getMessageAttributes()).isEmpty();
	}

	@Test
	void shouldThrowExceptionWhenToMessageIsCalled() {
		assertThatThrownBy(() -> converter.toMessage("payload", null)).isInstanceOf(UnsupportedOperationException.class)
				.hasMessageContaining("This converter only supports reading SNS notifications");
	}

	@Test
	void shouldThrowExceptionWhenToMessageWithConversionHintIsCalled() {
		assertThatThrownBy(() -> converter.toMessage("payload", null, null))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessageContaining("This converter only supports reading SNS notifications");
	}
}
