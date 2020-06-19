/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.messaging.core;

import java.math.BigDecimal;
import java.util.stream.Stream;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link QueueMessageUtils}.
 *
 * @author Maciej Walkowiak
 */
class QueueMessageUtilsTest {

	@ParameterizedTest
	@MethodSource("validArguments")
	void createsMessageWithNumberHeader(String value, String type, Number expected) {
		Message message = new Message().withBody("some body").addMessageAttributesEntry(
				"number-attribute",
				new MessageAttributeValue().withStringValue(value).withDataType(type));

		org.springframework.messaging.Message<String> result = QueueMessageUtils
				.createMessage(message);

		assertThat(result.getHeaders().get("number-attribute")).isEqualTo(expected);
	}

	private static Stream<Arguments> validArguments() {
		return Stream.of(Arguments.of("10", "Number", BigDecimal.valueOf(10)),
				Arguments.of("3", "Number.byte", (byte) 3),
				Arguments.of("3", "Number.Byte", (byte) 3),
				Arguments.of("3", "Number.java.lang.Byte", (byte) 3),
				Arguments.of("10", "Number.long", 10L),
				Arguments.of("10", "Number.Long", 10L),
				Arguments.of("10", "Number.java.lang.Long", 10L),
				Arguments.of("10", "Number.short", (short) 10),
				Arguments.of("10", "Number.Short", (short) 10),
				Arguments.of("10", "Number.java.lang.Short", (short) 10),
				Arguments.of("10", "Number.int", 10),
				Arguments.of("10", "Number.Int", 10),
				Arguments.of("10", "Number.java.lang.Integer", 10),
				Arguments.of("3.4", "Number.float", 3.4f),
				Arguments.of("3.4", "Number.Float", 3.4f),
				Arguments.of("3.4", "Number.java.lang.Float", 3.4f),
				Arguments.of("3.4", "Number.double", 3.4d),
				Arguments.of("3.4", "Number.Double", 3.4d),
				Arguments.of("3.4", "Number.java.lang.Double", 3.4d));
	}

}
