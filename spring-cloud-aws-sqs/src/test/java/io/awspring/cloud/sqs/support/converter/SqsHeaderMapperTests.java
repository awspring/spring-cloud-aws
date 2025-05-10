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

import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.messaging.MessageHeaders;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

/**
 * Tests for {@link SqsHeaderMapper}.
 *
 * @author Tomaz Fernandes
 * @author Maciej Walkowiak
 */
class SqsHeaderMapperTests {

	@Test
	void shouldAddExtraHeader() {
		SqsHeaderMapper headerMapper = new SqsHeaderMapper();
		String myHeader = "myHeader";
		String myValue = "myValue";
		headerMapper.setAdditionalHeadersFunction((message, accessor) -> {
			accessor.setHeader(myHeader, myValue);
			return accessor.toMessageHeaders();
		});
		Message message = Message.builder().body("payload").messageId(UUID.randomUUID().toString()).build();
		MessageHeaders headers = headerMapper.toHeaders(message);
		assertThat(headers.get(myHeader)).isEqualTo(myValue);
	}

	@Test
	void shouldAddStringMessageAttributes() {
		SqsHeaderMapper mapper = new SqsHeaderMapper();
		String headerName = "stringAttribute";
		String headerValue = "myString";
		Message message = Message.builder().body("payload")
				.messageAttributes(
						Map.of(headerName,
								MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING)
										.stringValue(headerValue).build()))
				.messageId(UUID.randomUUID().toString()).build();
		MessageHeaders headers = mapper.toHeaders(message);
		assertThat(headers.get(headerName)).isEqualTo(headerValue);
	}

	@Test
	void shouldAddStringCustomMessageAttributes() {
		SqsHeaderMapper mapper = new SqsHeaderMapper();
		String headerName = "stringAttribute";
		String headerValue = "myString";
		Message message = Message.builder().body("payload")
				.messageAttributes(
						Map.of(headerName,
								MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING + ".Array")
										.stringValue(headerValue).build()))
				.messageId(UUID.randomUUID().toString()).build();
		MessageHeaders headers = mapper.toHeaders(message);
		assertThat(headers.get(headerName)).isEqualTo(headerValue);
	}

	@Test
	void shouldDefaultToStringIfDataTypeUnknownMessageAttributes() {
		SqsHeaderMapper mapper = new SqsHeaderMapper();
		String headerName = "stringAttribute";
		String headerValue = "myString";
		Message message = Message.builder().body("payload")
				.messageAttributes(Map.of(headerName,
						MessageAttributeValue.builder().dataType("invalid data type").stringValue(headerValue).build()))
				.messageId(UUID.randomUUID().toString()).build();
		MessageHeaders headers = mapper.toHeaders(message);
		assertThat(headers.get(headerName)).isEqualTo(headerValue);
	}

	@Test
	void shouldAddBinaryMessageAttributes() {
		SqsHeaderMapper mapper = new SqsHeaderMapper();
		String headerName = "stringAttribute";
		SdkBytes headerValue = SdkBytes.fromUtf8String("myString");
		Message message = Message.builder().body("payload")
				.messageAttributes(
						Map.of(headerName,
								MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.BINARY)
										.binaryValue(headerValue).build()))
				.messageId(UUID.randomUUID().toString()).build();
		MessageHeaders headers = mapper.toHeaders(message);
		assertThat(headers.get(headerName)).isEqualTo(headerValue);
	}

	@Test
	void shouldAddBinaryCustomMessageAttributes() {
		SqsHeaderMapper mapper = new SqsHeaderMapper();
		String headerName = "stringAttribute";
		SdkBytes headerValue = SdkBytes.fromUtf8String("myString");
		Message message = Message.builder().body("payload")
				.messageAttributes(
						Map.of(headerName,
								MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.BINARY + ".protobuf")
										.binaryValue(headerValue).build()))
				.messageId(UUID.randomUUID().toString()).build();
		MessageHeaders headers = mapper.toHeaders(message);
		assertThat(headers.get(headerName)).isEqualTo(headerValue);
	}

	@Test
	void shouldAddNumberMessageAttributes() {
		SqsHeaderMapper mapper = new SqsHeaderMapper();
		String headerName = "stringAttribute";
		int headerValue = 10;
		Message message = Message.builder().body("payload")
				.messageAttributes(Map.of(headerName,
						MessageAttributeValue.builder().stringValue(String.valueOf(headerValue))
								.dataType(MessageAttributeDataTypes.NUMBER + ".int").build()))
				.messageId(UUID.randomUUID().toString()).build();
		MessageHeaders headers = mapper.toHeaders(message);
		assertThat(headers.get(headerName)).isEqualTo(headerValue);
	}

	@Test
	void shouldCreateMessageWithSystemAttributesFromHeaders() {
		MessageHeaders headers = new MessageHeaders(
				Map.of(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, "value1",
						SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, "value2",
						SqsHeaders.MessageSystemAttributes.SQS_AWS_TRACE_HEADER, "value3", "customHeaderString",
						"customValueString", "customHeaderNumber", 42));

		SqsHeaderMapper mapper = new SqsHeaderMapper();
		Message message = mapper.fromHeaders(headers);

		assertThat(message.attributes()).hasSize(3)
				.containsExactlyInAnyOrderEntriesOf(Map.of(MessageSystemAttributeName.MESSAGE_GROUP_ID, "value1",
						MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID, "value2",
						MessageSystemAttributeName.AWS_TRACE_HEADER, "value3"));
		assertThat(message.messageAttributes()).hasSize(2)
				.containsExactlyInAnyOrderEntriesOf(Map.of("customHeaderString", MessageAttributeValue.builder()
						.dataType(MessageAttributeDataTypes.STRING).stringValue("customValueString").build(),
						"customHeaderNumber",
						MessageAttributeValue.builder()
								.dataType(MessageAttributeDataTypes.NUMBER + ".java.lang.Integer").stringValue("42")
								.build()));
	}

	@ParameterizedTest
	@MethodSource("validArguments")
	void createsMessageWithNumberHeader(String value, String type, Number expected) {
		String headerName = "number-attribute";
		Message message = Message.builder().body("some body").messageId(UUID.randomUUID().toString())
				.messageAttributes(
						Map.of(headerName, MessageAttributeValue.builder().stringValue(value).dataType(type).build()))
				.build();
		MessageHeaders headers = new SqsHeaderMapper().toHeaders(message);
		assertThat(headers.get(headerName)).isEqualTo(expected);
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

}
