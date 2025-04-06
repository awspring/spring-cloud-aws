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
package io.awspring.cloud.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Tests for {@link MessageHeaderUtils}.
 *
 * @author Tomaz Fernandes
 */
class MessageHeaderUtilsTest {

	@Test
	void shouldRemoveHeaderWhenPresent() {
		// given
		String headerKey = "test-header";
		String headerValue = "test-value";
		Message<String> message = MessageBuilder.withPayload("test-payload").setHeader(headerKey, headerValue).build();

		// when
		Message<String> result = MessageHeaderUtils.removeHeaderIfPresent(message, headerKey);

		// then
		assertThat(result.getPayload()).isEqualTo(message.getPayload());
		assertThat(result.getHeaders().containsKey(headerKey)).isFalse();

		// Original message should remain unchanged
		assertThat(message.getHeaders().containsKey(headerKey)).isTrue();
		assertThat(message.getHeaders().get(headerKey)).isEqualTo(headerValue);
	}

	@Test
	void shouldReturnOriginalMessageWhenHeaderNotPresent() {
		// given
		String headerKey = "non-existent-header";
		Message<String> message = MessageBuilder.withPayload("test-payload").build();

		// when
		Message<String> result = MessageHeaderUtils.removeHeaderIfPresent(message, headerKey);

		// then
		assertThat(result).isSameAs(message);
	}

	@Test
	void shouldPreserveMessageIdAndTimestamp() {
		// given
		String headerKey = "test-header";
		String headerValue = "test-value";
		Message<String> message = MessageBuilder.withPayload("test-payload").setHeader(headerKey, headerValue).build();

		// when
		Message<String> result = MessageHeaderUtils.removeHeaderIfPresent(message, headerKey);

		// then
		assertThat(result.getHeaders().getId()).isEqualTo(message.getHeaders().getId());
		assertThat(result.getHeaders().getTimestamp()).isEqualTo(message.getHeaders().getTimestamp());
		assertThat(MessageHeaderUtils.getId(result)).isEqualTo(message.getHeaders().getId().toString());
	}

	@Test
	void shouldPreserveOtherHeaders() {
		// given
		String headerToRemove = "header-to-remove";
		String headerToKeep = "header-to-keep";
		Message<String> message = MessageBuilder.withPayload("test-payload").setHeader(headerToRemove, "remove-value")
				.setHeader(headerToKeep, "keep-value").setHeader("another-header", "another-value").build();

		// when
		Message<String> result = MessageHeaderUtils.removeHeaderIfPresent(message, headerToRemove);

		// then
		assertThat(result.getHeaders().containsKey(headerToRemove)).isFalse();
		assertThat(result.getHeaders().get(headerToKeep)).isEqualTo("keep-value");
		assertThat(result.getHeaders().get("another-header")).isEqualTo("another-value");
		assertThat(result.getHeaders().size()).isEqualTo(message.getHeaders().size() - 1);
	}
}
