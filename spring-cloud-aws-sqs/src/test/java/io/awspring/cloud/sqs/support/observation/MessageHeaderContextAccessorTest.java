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
package io.awspring.cloud.sqs.support.observation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Tests for {@link MessageHeaderContextAccessor}.
 *
 * @author Tomaz Fernandes
 */
class MessageHeaderContextAccessorTest {

	private MessageHeaderContextAccessor accessor;
	private MessageHeaders headers;
	private String testKey;
	private String testValue;

	@BeforeEach
	void setUp() {
		accessor = new MessageHeaderContextAccessor();
		testKey = "test-key";
		testValue = "test-value";
		headers = MessageBuilder.withPayload("test-payload").setHeader(testKey, testValue).build().getHeaders();
	}

	@Test
	void shouldReturnMessageHeadersAsReadableType() {
		// when
		Class<?> readableType = accessor.readableType();

		// then
		assertThat(readableType).isEqualTo(MessageHeaders.class);
	}

	@Test
	void shouldReturnMessageHeadersAsWriteableType() {
		// when
		Class<?> writeableType = accessor.writeableType();

		// then
		assertThat(writeableType).isEqualTo(MessageHeaders.class);
	}

	@Test
	void shouldReadValuesBasedOnPredicate() {
		// given
		Map<Object, Object> target = new HashMap<>();
		Predicate<Object> keyPredicate = key -> key.equals(testKey);

		// when
		accessor.readValues(headers, keyPredicate, target);

		// then
		assertThat(target).hasSize(1).containsEntry(testKey, testValue);
	}

	@Test
	void shouldNotReadValuesWhenPredicateDoesNotMatch() {
		// given
		Map<Object, Object> target = new HashMap<>();
		Predicate<Object> keyPredicate = key -> key.equals("non-existent-key");

		// when
		accessor.readValues(headers, keyPredicate, target);

		// then
		assertThat(target).isEmpty();
	}

	@Test
	void shouldReadSingleValue() {
		// when
		String value = accessor.readValue(headers, testKey);

		// then
		assertThat(value).isEqualTo(testValue);
	}

	@Test
	void shouldReturnNullForMissingKey() {
		// when
		String value = accessor.readValue(headers, "non-existent-key");

		// then
		assertThat(value).isNull();
	}

	@Test
	void shouldThrowExceptionWhenWritingValues() {
		// given
		Map<Object, Object> valuesToWrite = new HashMap<>();
		valuesToWrite.put("key", "value");

		// when/then
		assertThatThrownBy(() -> accessor.writeValues(valuesToWrite, headers))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessageContaining("Should not write values to the MessageHeaders");
	}
}