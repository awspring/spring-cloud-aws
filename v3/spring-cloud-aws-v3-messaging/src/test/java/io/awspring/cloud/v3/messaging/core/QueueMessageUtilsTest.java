/*
 * Copyright 2013-2020 the original author or authors.
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

package io.awspring.cloud.v3.messaging.core;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link QueueMessageUtils}.
 *
 * @author Maciej Walkowiak
 * @author Wojciech Mąka
 */
class QueueMessageUtilsTest {

	public static Charset charset = StandardCharsets.UTF_8;

	public static CharsetEncoder encoder = charset.newEncoder();

	@ParameterizedTest
	@MethodSource("validArguments")
	void createsMessageWithNumberHeader(String value, String type, Number expected) {
		Message message = Message.builder()
			.body("some body")
			.messageAttributes(Collections.singletonMap("number-attribute",
				MessageAttributeValue.builder()
					.stringValue(value)
					.dataType(type)
					.build()))
			.build();

		org.springframework.messaging.Message<String> result = QueueMessageUtils.createMessage(message);

		assertThat(result.getHeaders().get("number-attribute")).isEqualTo(expected);
	}

	private static Stream<Arguments> validArguments() {
		return Stream.of(Arguments.of("10", "Number", BigDecimal.valueOf(10)),
				Arguments.of("3", "Number.byte", (byte) 3), Arguments.of("3", "Number.Byte", (byte) 3),
				Arguments.of("3", "Number.java.lang.Byte", (byte) 3), Arguments.of("10", "Number.long", 10L),
				Arguments.of("10", "Number.Long", 10L), Arguments.of("10", "Number.java.lang.Long", 10L),
				Arguments.of("10", "Number.short", (short) 10), Arguments.of("10", "Number.Short", (short) 10),
				Arguments.of("10", "Number.java.lang.Short", (short) 10), Arguments.of("10", "Number.int", 10),
				Arguments.of("10", "Number.Int", 10), Arguments.of("10", "Number.java.lang.Integer", 10),
				Arguments.of("3.4", "Number.float", 3.4f), Arguments.of("3.4", "Number.Float", 3.4f),
				Arguments.of("3.4", "Number.java.lang.Float", 3.4f), Arguments.of("3.4", "Number.double", 3.4d),
				Arguments.of("3.4", "Number.Double", 3.4d), Arguments.of("3.4", "Number.java.lang.Double", 3.4d));
	}

	@ParameterizedTest
	@MethodSource("binaryMessageAttributes")
	void createsMessageWithBinaryMessageAttributes(String extendedType, String value, ByteBuffer expected)
			throws CharacterCodingException {
		final MessageAttributeValue messageAttributeValue = MessageAttributeValue.builder()
				.binaryValue(SdkBytes.fromByteBuffer(encoder.encode(CharBuffer.wrap(value))))
				.dataType(extendedType)
			.build();

		Message message = Message.builder()
			.body("some body")
			.messageAttributes(Collections.singletonMap("binary-attribute",
				messageAttributeValue))
			.build();

		org.springframework.messaging.Message<String> result = QueueMessageUtils.createMessage(message);

		assertThat(result.getHeaders().get("binary-attribute")).isEqualTo(SdkBytes.fromByteBuffer(expected));
	}

	private static Stream<Arguments> binaryMessageAttributes() throws CharacterCodingException {
		return Stream.of(Arguments.of("Binary.png", "cmFuZG9t", encoder.encode(CharBuffer.wrap("cmFuZG9t"))),
				Arguments.of("Binary.png.800x600", "cmFuZG9t", encoder.encode(CharBuffer.wrap("cmFuZG9t"))));
	}

	@ParameterizedTest
	@MethodSource("stringMessageAttributes")
	void createsMessageWithStringMessageAttributes(String extendedType, String value, String expected) {
		final MessageAttributeValue messageAttributeValue = MessageAttributeValue.builder()
			.stringValue(value)
			.dataType(extendedType)
			.build();

		Message message = Message.builder()
			.body("some body")
			.messageAttributes(Collections.singletonMap("string-type-attribute",
				messageAttributeValue))
			.build();

		org.springframework.messaging.Message<String> result = QueueMessageUtils.createMessage(message);

		assertThat(result.getHeaders().get("string-type-attribute")).isEqualTo(expected);
	}

	private static Stream<Arguments> stringMessageAttributes() {
		return Stream.of(Arguments.of("String.description", "A message", "A message"),
				Arguments.of("String.description.moreInfo", "In the bottle", "In the bottle"));
	}

}
