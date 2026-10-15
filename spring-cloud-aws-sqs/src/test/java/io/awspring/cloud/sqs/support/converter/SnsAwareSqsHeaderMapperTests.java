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
package io.awspring.cloud.sqs.support.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.MessageHeaders;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests for {@link SnsAwareSqsHeaderMapper}.
 *
 * @author WooSeong Kim
 */
class SnsAwareSqsHeaderMapperTests {

	@Test
	void shouldRejectNullJsonMapper() {
		assertThatThrownBy(() -> new SnsAwareSqsHeaderMapper(null)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("jsonMapper cannot be null");
	}

	@Test
	void shouldUseConfiguredJsonMapper() {
		JsonMapper jsonMapper = JsonMapper.builder().enable(JsonReadFeature.ALLOW_JAVA_COMMENTS).build();
		Message message = Message.builder().messageId(UUID.randomUUID().toString()).body("""
				{
				  // Accepted only by the application's configured mapper.
				  "Type": "Notification", "Message": "payload",
				  "MessageAttributes": {"attribute": {"Type": "String", "Value": "snsValue"}}
				}
				""").build();

		assertThat(new SnsAwareSqsHeaderMapper(new JsonMapper()).toHeaders(message)).doesNotContainKey("attribute");
		assertThat(new SnsAwareSqsHeaderMapper(jsonMapper).toHeaders(message)).containsEntry("attribute", "snsValue");
	}

	@Test
	void shouldAddSnsMessageAttributes() {
		SnsAwareSqsHeaderMapper mapper = new SnsAwareSqsHeaderMapper(new JsonMapper());
		Message message = Message.builder().body("""
				{
				  "Type": "Notification",
				  "Message": "payload",
				  "MessageAttributes": {
				    "stringAttribute": { "Type": "String", "Value": "myString" },
				    "numberAttribute": { "Type": "Number.java.lang.Integer", "Value": "10" },
				    "binaryAttribute": { "Type": "Binary", "Value": "bXlCaW5hcnk=" }
				  }
				}
				""").messageId(UUID.randomUUID().toString()).build();

		MessageHeaders headers = mapper.toHeaders(message);

		assertThat(headers.get("stringAttribute")).isEqualTo("myString");
		assertThat(headers.get("numberAttribute")).isEqualTo(10);
		assertThat(headers.get("binaryAttribute")).isEqualTo(SdkBytes.fromUtf8String("myBinary"));
	}

	@Test
	void shouldPreferSqsMessageAttributesOverSnsMessageAttributes() {
		SnsAwareSqsHeaderMapper mapper = new SnsAwareSqsHeaderMapper(new JsonMapper());
		Message message = Message.builder().body("""
				{
				  "Type": "Notification",
				  "Message": "payload",
				  "MessageAttributes": {
				    "attribute": { "Type": "String", "Value": "snsValue" }
				  }
				}
				""")
				.messageAttributes(
						Map.of("attribute",
								MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING)
										.stringValue("sqsValue").build()))
				.messageId(UUID.randomUUID().toString()).build();

		MessageHeaders headers = mapper.toHeaders(message);

		assertThat(headers.get("attribute")).isEqualTo("sqsValue");
	}

	@Test
	void shouldIgnoreMessageAttributesInNonSnsPayload() {
		SnsAwareSqsHeaderMapper mapper = new SnsAwareSqsHeaderMapper(new JsonMapper());
		Message message = Message.builder().body("""
				{
				  "Type": "ApplicationEvent",
				  "Message": "payload",
				  "MessageAttributes": {
				    "attribute": { "Type": "String", "Value": "value" }
				  }
				}
				""").messageId(UUID.randomUUID().toString()).build();

		MessageHeaders headers = mapper.toHeaders(message);

		assertThat(headers).doesNotContainKey("attribute");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "payload", "{}", "null", "{\"MessageAttributes\":",
			"{\"Type\":\"Notification\",\"Message\":\"payload\",\"MessageAttributes\":[]}" })
	void shouldLeaveNonNotificationAndMalformedBodiesUnchanged(String body) {
		Message message = Message.builder().messageId(UUID.randomUUID().toString()).body(body).build();
		MessageHeaders headers = new SnsAwareSqsHeaderMapper(new JsonMapper()).toHeaders(message);

		assertThat(headers.getId()).isEqualTo(UUID.fromString(message.messageId()));
		assertThat(headers.get(SqsHeaders.SQS_SOURCE_DATA_HEADER)).isSameAs(message);
	}

	@Test
	void shouldPreserveAdditionalHeadersAndNonUuidMessageIds() {
		SnsAwareSqsHeaderMapper mapper = new SnsAwareSqsHeaderMapper(new JsonMapper());
		mapper.setConvertMessageIdToUuid(false);
		mapper.setAdditionalHeadersFunction((message, accessor) -> {
			accessor.setHeader("attribute", "custom");
			return accessor.toMessageHeaders();
		});
		Message message = Message.builder().messageId("custom-id").body("""
				{"Type":"Notification","Message":"payload",
				 "MessageAttributes":{"attribute":{"Type":"String","Value":"snsValue"}}}
				""").build();
		MessageHeaders headers = mapper.toHeaders(message);

		assertThat(headers).containsEntry("attribute", "custom");
		assertThat(headers).containsEntry(SqsHeaders.SQS_RAW_MESSAGE_ID_HEADER, "custom-id");
		assertThat(headers.get(SqsHeaders.SQS_SOURCE_DATA_HEADER)).isSameAs(message);
		SqsHeaderMapper defaultMapper = new SqsHeaderMapper();
		defaultMapper.setConvertMessageIdToUuid(false);
		assertThat(headers.getId()).isEqualTo(defaultMapper.toHeaders(message).getId());
	}

	@Test
	void shouldUseMapperOnlyWhenConfiguredOnConverter() {
		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
		Message message = Message.builder().messageId(UUID.randomUUID().toString()).body("""
				{"Type":"Notification","Message":"payload",
				 "MessageAttributes":{"attribute":{"Type":"String","Value":"snsValue"}}}
				""").build();

		assertThat(converter.toMessagingMessage(message, null).getHeaders()).doesNotContainKey("attribute");
		converter.setHeaderMapper(new SnsAwareSqsHeaderMapper(new JsonMapper()));
		assertThat(converter.toMessagingMessage(message, null).getHeaders()).containsEntry("attribute", "snsValue");
	}
}
