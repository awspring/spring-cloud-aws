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
package io.awspring.cloud.sns.core.async;

import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER;
import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_GROUP_ID_HEADER;
import static io.awspring.cloud.sns.core.SnsHeaders.NOTIFICATION_SUBJECT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sns.Person;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

/**
 * Unit tests for {@link DefaultSnsPublishMessageConverter}.
 *
 * @author Matej Nedic
 * @since 4.0.1
 */
class DefaultSnsPublishMessageConverterTest {

	private DefaultSnsPublishMessageConverter converter;

	@BeforeEach
	void setUp() {
		JacksonJsonMessageConverter messageConverter = new JacksonJsonMessageConverter();
		messageConverter.setSerializedPayloadClass(String.class);
		converter = new DefaultSnsPublishMessageConverter(messageConverter);
	}

	@Test
	void convert_withMessage_returnsPublishRequestAndOriginalMessage() {
		Message<String> message = MessageBuilder
			.withPayload("test payload")
			.setHeader("custom-header", "custom-value")
			.build();

		PublishRequestMessagePair<String> result = converter.convert(message);

		assertThat(result.publishRequest()).isNotNull();
		assertThat(result.publishRequest().message()).isEqualTo("test payload");
		assertThat(result.originalMessage()).isEqualTo(message);
		assertThat(result.originalMessage().getPayload()).isEqualTo("test payload");
	}

	@Test
	void convert_withPayloadAndHeaders_buildsMessageAndReturnsIt() {
		Map<String, Object> headers = new HashMap<>();
		headers.put("custom-header", "custom-value");
		headers.put(NOTIFICATION_SUBJECT_HEADER, "Test Subject");

		PublishRequestMessagePair<String> result = converter.convert("test payload", headers);

		assertThat(result.publishRequest()).isNotNull();
		assertThat(result.publishRequest().message()).isEqualTo("test payload");
		assertThat(result.publishRequest().subject()).isEqualTo("Test Subject");
		assertThat(result.originalMessage()).isNotNull();
		assertThat(result.originalMessage().getPayload()).isEqualTo("test payload");
		assertThat(result.originalMessage().getHeaders().get("custom-header")).isEqualTo("custom-value");
		assertThat(result.originalMessage().getHeaders().get(NOTIFICATION_SUBJECT_HEADER)).isEqualTo("Test Subject");
	}

	@Test
	void convert_withSubjectHeader_setsSubjectInPublishRequest() {
		Message<String> message = MessageBuilder
			.withPayload("test payload")
			.setHeader(NOTIFICATION_SUBJECT_HEADER, "My Subject")
			.build();

		PublishRequestMessagePair<String> result = converter.convert(message);

		assertThat(result.publishRequest().subject()).isEqualTo("My Subject");
	}

	@Test
	void convert_withFifoHeaders_setsFifoAttributesInPublishRequest() {
		Message<String> message = MessageBuilder
			.withPayload("test payload")
			.setHeader(MESSAGE_GROUP_ID_HEADER, "group-123")
			.setHeader(MESSAGE_DEDUPLICATION_ID_HEADER, "dedup-456")
			.build();

		PublishRequestMessagePair<String> result = converter.convert(message);

		assertThat(result.publishRequest().messageGroupId()).isEqualTo("group-123");
		assertThat(result.publishRequest().messageDeduplicationId()).isEqualTo("dedup-456");
	}

	@Test
	void convert_withMessageAttributes_setsMessageAttributesInPublishRequest() {
		Message<String> message = MessageBuilder
			.withPayload("test payload")
			.setHeader("string-attr", "value")
			.setHeader("number-attr", 42)
			.build();

		PublishRequestMessagePair<String> result = converter.convert(message);

		assertThat(result.publishRequest().messageAttributes()).isNotEmpty();
		assertThat(result.publishRequest().messageAttributes()).containsKey("string-attr");
		assertThat(result.publishRequest().messageAttributes()).containsKey("number-attr");
	}

	@Test
	void convert_withComplexObject_serializesPayload() {
		Person testObject = new Person("test");
		Message<Person> message = MessageBuilder
			.withPayload(testObject)
			.build();

		PublishRequestMessagePair<Person> result = converter.convert(message);

		assertThat(result.publishRequest().message()).contains("test");
		assertThat(result.originalMessage().getPayload()).isEqualTo(testObject);
	}

	@Test
	void convert_withEmptyHeaders_createsValidPublishRequest() {
		PublishRequestMessagePair<String> result = converter.convert("payload", Map.of());

		assertThat(result.publishRequest()).isNotNull();
		assertThat(result.publishRequest().message()).isEqualTo("payload");
		assertThat(result.originalMessage()).isNotNull();
		assertThat(result.originalMessage().getPayload()).isEqualTo("payload");
	}

}
