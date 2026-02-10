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

import io.awspring.cloud.sns.Person;
import io.awspring.cloud.sns.core.SnsHeaders;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests for {@link DefaultSnsMessageConverter}.
 *
 * @author Matej Nedic
 */
class DefaultSnsMessageConverterTest {

	private final DefaultSnsMessageConverter converter = new DefaultSnsMessageConverter();

	@Test
	void convertsStringPayload() {
		Message<String> message = MessageBuilder.withPayload("hello").build();

		PublishBatchRequestEntry entry = converter.convertMessage(message);

		assertThat(entry.message()).isEqualTo("hello");
	}

	@Test
	void convertsJsonPayloadWithCustomConverter() {
		JacksonJsonMessageConverter jacksonConverter = new JacksonJsonMessageConverter(new JsonMapper());
		jacksonConverter.setSerializedPayloadClass(String.class);
		DefaultSnsMessageConverter jsonConverter = new DefaultSnsMessageConverter(jacksonConverter);

		Message<Person> message = MessageBuilder.withPayload(new Person("John")).build();

		PublishBatchRequestEntry entry = jsonConverter.convertMessage(message);

		assertThat(entry.message()).contains("John");
	}

	@Test
	void setsMessageGroupId() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(SnsHeaders.MESSAGE_GROUP_ID_HEADER, "group-1").build();

		PublishBatchRequestEntry entry = converter.convertMessage(message);

		assertThat(entry.messageGroupId()).isEqualTo("group-1");
	}

	@Test
	void setsDeduplicationId() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER, "dedup-1").build();

		PublishBatchRequestEntry entry = converter.convertMessage(message);

		assertThat(entry.messageDeduplicationId()).isEqualTo("dedup-1");
	}

	@Test
	void setsAllFifoHeaders() {
		Message<String> message = MessageBuilder.withPayload("fifo payload")
				.setHeader(SnsHeaders.MESSAGE_GROUP_ID_HEADER, "grp")
				.setHeader(SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER, "ded")
				.setHeader("custom", "val").build();

		PublishBatchRequestEntry entry = converter.convertMessage(message);

		assertThat(entry.message()).isEqualTo("fifo payload");
		assertThat(entry.messageGroupId()).isEqualTo("grp");
		assertThat(entry.messageDeduplicationId()).isEqualTo("ded");
		//Test DataType as well
		assertThat(entry.messageAttributes().get("custom")).isEqualTo(MessageAttributeValue.builder().stringValue("val").dataType("String").build());
	}

	@Test
	void leavesOptionalHeadersNullWhenAbsent() {
		Message<String> message = MessageBuilder.withPayload("plain").build();

		PublishBatchRequestEntry entry = converter.convertMessage(message);

		assertThat(entry.messageGroupId()).isNull();
		assertThat(entry.messageDeduplicationId()).isNull();
	}

	@Test
	void convertsCustomHeadersToMessageAttributes() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("priority", "high").build();

		PublishBatchRequestEntry entry = converter.convertMessage(message);

		assertThat(entry.messageAttributes()).containsKey("priority");
		//Test plain String value\
		assertThat(entry.messageAttributes().get("priority").stringValue()).isEqualTo("high");
	}

	@Test
	void usesCustomMessageIdHeaderAsBatchEntryId() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(SnsHeaders.MESSAGE_ID_HEADER, "custom-id-123").build();

		PublishBatchRequestEntry entry = converter.convertMessage(message);

		assertThat(entry.id()).isEqualTo("custom-id-123");
	}

	@Test
	void generatesUuidWhenNoMessageIdHeader() {
		Message<String> message = MessageBuilder.withPayload("test").build();

		PublishBatchRequestEntry entry = converter.convertMessage(message);

		assertThat(entry.id()).isNotBlank();
		assertThat(UUID.fromString(entry.id())).isNotNull();
	}
}
