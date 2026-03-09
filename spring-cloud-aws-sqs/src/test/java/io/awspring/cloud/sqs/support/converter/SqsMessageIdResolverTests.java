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

import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * Tests for {@link SqsMessageIdResolver}.
 *
 * @author Jeongmin Kim
 */
class SqsMessageIdResolverTests {

	@Test
	void shouldReturnTrueForValidUuid() {
		assertThat(SqsMessageIdResolver.isValidUuid("550e8400-e29b-41d4-a716-446655440000")).isTrue();
		assertThat(SqsMessageIdResolver.isValidUuid(UUID.randomUUID().toString())).isTrue();
	}

	@Test
	void shouldReturnFalseForInvalidUuid() {
		assertThat(SqsMessageIdResolver.isValidUuid("92898073-7bd6a160-5797b060-54a7e539")).isFalse();
		assertThat(SqsMessageIdResolver.isValidUuid("not-a-uuid")).isFalse();
	}

	@Test
	void shouldResolveValidUuidDirectly() {
		String uuidString = "550e8400-e29b-41d4-a716-446655440000";
		UUID result = SqsMessageIdResolver.resolveUuid(uuidString);
		assertThat(result).isEqualTo(UUID.fromString(uuidString));
	}

	@Test
	void shouldResolveDeterministicUuidForNonUuidString() {
		String nonUuid = "92898073-7bd6a160-5797b060-54a7e539";
		UUID result = SqsMessageIdResolver.resolveUuid(nonUuid);
		assertThat(result).isEqualTo(UUID.nameUUIDFromBytes(nonUuid.getBytes(StandardCharsets.UTF_8)));
		// Verify deterministic
		assertThat(SqsMessageIdResolver.resolveUuid(nonUuid)).isEqualTo(result);
	}

	@Test
	void shouldResolveAndAddMessageIdWithValidUuid() {
		String uuidMessageId = "550e8400-e29b-41d4-a716-446655440000";
		MessageHeaders inputHeaders = new MessageHeaderAccessor().toMessageHeaders();
		MessageHeaders result = SqsMessageIdResolver.resolveAndAddMessageId(uuidMessageId, inputHeaders, true);
		assertThat(result.getId()).isEqualTo(UUID.fromString(uuidMessageId));
		assertThat(result.get(SqsHeaders.SQS_RAW_MESSAGE_ID_HEADER)).isEqualTo(uuidMessageId);
	}

	@Test
	void shouldResolveAndAddMessageIdWithValidUuidWhenConvertMessageIdToUuidIsFalse() {
		String uuidMessageId = "550e8400-e29b-41d4-a716-446655440000";
		MessageHeaders inputHeaders = new MessageHeaderAccessor().toMessageHeaders();
		MessageHeaders result = SqsMessageIdResolver.resolveAndAddMessageId(uuidMessageId, inputHeaders, false);
		assertThat(result.getId()).isEqualTo(UUID.fromString(uuidMessageId));
		assertThat(result.get(SqsHeaders.SQS_RAW_MESSAGE_ID_HEADER)).isEqualTo(uuidMessageId);
	}

	@Test
	void shouldThrowWhenConvertMessageIdToUuidIsTrueAndIdIsNotValidUuid() {
		String nonUuid = "92898073-7bd6a160-5797b060-54a7e539";
		MessageHeaders inputHeaders = new MessageHeaderAccessor().toMessageHeaders();
		assertThatThrownBy(() -> SqsMessageIdResolver.resolveAndAddMessageId(nonUuid, inputHeaders, true))
				.isInstanceOf(MessagingException.class).hasMessageContaining("not a valid UUID")
				.hasMessageContaining("convert-message-id-to-uuid");
	}

	@Test
	void shouldGenerateDeterministicUuidWhenConvertMessageIdToUuidIsFalse() {
		String nonUuid = "92898073-7bd6a160-5797b060-54a7e539";
		MessageHeaders inputHeaders = new MessageHeaderAccessor().toMessageHeaders();
		MessageHeaders result = SqsMessageIdResolver.resolveAndAddMessageId(nonUuid, inputHeaders, false);
		assertThat(result.getId()).isEqualTo(UUID.nameUUIDFromBytes(nonUuid.getBytes(StandardCharsets.UTF_8)));
		assertThat(result.get(SqsHeaders.SQS_RAW_MESSAGE_ID_HEADER)).isEqualTo(nonUuid);
	}

	@Test
	void shouldPreserveExistingHeadersWhenResolvingMessageId() {
		String uuidMessageId = UUID.randomUUID().toString();
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setHeader("customHeader", "customValue");
		MessageHeaders inputHeaders = accessor.toMessageHeaders();
		MessageHeaders result = SqsMessageIdResolver.resolveAndAddMessageId(uuidMessageId, inputHeaders, true);
		assertThat(result.get("customHeader")).isEqualTo("customValue");
	}

}