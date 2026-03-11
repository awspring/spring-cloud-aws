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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests for {@link DefaultSnsPublishMessageConverter}.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
class DefaultSnsPublishMessageConverterTest {

	private final DefaultSnsPublishMessageConverter converter = createConverter();

	private static DefaultSnsPublishMessageConverter createConverter() {
		JacksonJsonMessageConverter jackson = new JacksonJsonMessageConverter(new JsonMapper());
		jackson.setSerializedPayloadClass(String.class);
		return new DefaultSnsPublishMessageConverter(jackson);
	}

	@Test
	void convertsStringPayload() {
		Message<String> message = MessageBuilder.withPayload("hello").build();

		PublishRequestMessagePair<String> result = converter.convert(message);

		assertThat(result.publishRequest().message()).isEqualTo("hello");
		assertThat(result.originalMessage()).isEqualTo(message);
	}

	@Test
	void convertsPayloadWithHeaders() {
		PublishRequestMessagePair<String> result = converter.convert("payload",
				Map.of("custom", "value", NOTIFICATION_SUBJECT_HEADER, "Subject"));

		assertThat(result.publishRequest().message()).isEqualTo("payload");
		assertThat(result.publishRequest().subject()).isEqualTo("Subject");
		assertThat(result.originalMessage().getPayload()).isEqualTo("payload");
		assertThat(result.originalMessage().getHeaders()).containsEntry("custom", "value");
	}

	@Test
	void setsSubjectFromHeader() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(NOTIFICATION_SUBJECT_HEADER, "My Subject").build();

		PublishRequestMessagePair<String> result = converter.convert(message);

		assertThat(result.publishRequest().subject()).isEqualTo("My Subject");
	}

	@Test
	void setsFifoHeaders() {
		Message<String> message = MessageBuilder.withPayload("fifo")
				.setHeader(MESSAGE_GROUP_ID_HEADER, "grp-1")
				.setHeader(MESSAGE_DEDUPLICATION_ID_HEADER, "ded-1").build();

		PublishRequestMessagePair<String> result = converter.convert(message);

		assertThat(result.publishRequest().messageGroupId()).isEqualTo("grp-1");
		assertThat(result.publishRequest().messageDeduplicationId()).isEqualTo("ded-1");
	}

	@Test
	void convertsCustomHeadersToMessageAttributes() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("priority", "high")
				.setHeader("count", 42).build();

		PublishRequestMessagePair<String> result = converter.convert(message);

		assertThat(result.publishRequest().messageAttributes()).containsKey("priority");
		assertThat(result.publishRequest().messageAttributes()).containsKey("count");
	}

	@Test
	void serializesComplexPayload() {
		Person person = new Person("John");
		Message<Person> message = MessageBuilder.withPayload(person).build();

		PublishRequestMessagePair<Person> result = converter.convert(message);

		assertThat(result.publishRequest().message()).contains("John");
		assertThat(result.originalMessage().getPayload()).isEqualTo(person);
	}

	@Test
	void handlesEmptyHeaders() {
		PublishRequestMessagePair<String> result = converter.convert("payload", Map.of());

		assertThat(result.publishRequest().message()).isEqualTo("payload");
		assertThat(result.originalMessage().getPayload()).isEqualTo("payload");
	}
}
