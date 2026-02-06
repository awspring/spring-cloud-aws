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
package io.awspring.cloud.sns.core.batch.converter;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sns.core.SnsHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Matej Nedic
 */
class DefaultSnsMessageConverterTest {

	@Test
	void convertsSimpleStringMessage() {
		DefaultSnsMessageConverter converter = new DefaultSnsMessageConverter();
		Message<String> message = MessageBuilder.withPayload("test message").build();

		PublishBatchRequestEntry entry = converter.covertMessage(message);

		assertThat(entry.message()).isEqualTo("test message");
	}

	@Test
	void convertsMessageWithCustomConverter() {
		JacksonJsonMessageConverter jacksonConverter = new JacksonJsonMessageConverter(new JsonMapper());
		jacksonConverter.setSerializedPayloadClass(String.class);
		DefaultSnsMessageConverter converter = new DefaultSnsMessageConverter(jacksonConverter);

		TestPayload payload = new TestPayload("John", 30);
		Message<TestPayload> message = MessageBuilder.withPayload(payload).build();

		PublishBatchRequestEntry entry = converter.covertMessage(message);

		assertThat(entry.message()).contains("John").contains("30");
	}

	@Test
	void convertsMessageWithMessageGroupId() {
		DefaultSnsMessageConverter converter = new DefaultSnsMessageConverter();
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(SnsHeaders.MESSAGE_GROUP_ID_HEADER, "group-123").build();

		PublishBatchRequestEntry entry = converter.covertMessage(message);

		assertThat(entry.messageGroupId()).isEqualTo("group-123");
	}

	@Test
	void convertsMessageWithDeduplicationId() {
		DefaultSnsMessageConverter converter = new DefaultSnsMessageConverter();
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER, "dedup-456").build();

		PublishBatchRequestEntry entry = converter.covertMessage(message);

		assertThat(entry.messageDeduplicationId()).isEqualTo("dedup-456");
	}

	@Test
	void convertsMessageWithMessageAttributes() {
		DefaultSnsMessageConverter converter = new DefaultSnsMessageConverter();
		Message<String> message = MessageBuilder.withPayload("test").setHeader("customHeader", "customValue").build();

		PublishBatchRequestEntry entry = converter.covertMessage(message);

		assertThat(entry.messageAttributes()).isNotEmpty();
	}

	@Test
	void convertsMessageWithAllHeaders() {
		DefaultSnsMessageConverter converter = new DefaultSnsMessageConverter();
		Message<String> message = MessageBuilder.withPayload("test message")
				.setHeader(SnsHeaders.MESSAGE_GROUP_ID_HEADER, "group-123")
				.setHeader(SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER, "dedup-456")
				.setHeader("customHeader", "customValue").build();

		PublishBatchRequestEntry entry = converter.covertMessage(message);

		assertThat(entry.message()).isEqualTo("test message");
		assertThat(entry.messageGroupId()).isEqualTo("group-123");
		assertThat(entry.messageDeduplicationId()).isEqualTo("dedup-456");
		assertThat(entry.messageAttributes()).isNotEmpty();
	}

	@Test
	void convertsMessageWithoutOptionalHeaders() {
		DefaultSnsMessageConverter converter = new DefaultSnsMessageConverter();
		Message<String> message = MessageBuilder.withPayload("simple message").build();

		PublishBatchRequestEntry entry = converter.covertMessage(message);

		assertThat(entry.message()).isEqualTo("simple message");
		assertThat(entry.messageGroupId()).isNull();
		assertThat(entry.messageDeduplicationId()).isNull();
	}
}
