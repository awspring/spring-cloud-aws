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

package org.springframework.cloud.aws.messaging.support.converter;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 *
 */
class ObjectMessageConverterTest {

	private static MessageHeaders getMessageHeaders(String charsetName) {
		return new MessageHeaders(Collections.singletonMap(MessageHeaders.CONTENT_TYPE,
				new MimeType("application", "x-java-serialized-object", Charset.forName(charsetName))));
	}

	@Test
	void testToMessageAndFromMessage() throws Exception {
		String content = "stringwithspecialcharsöäü€a8";
		MySerializableClass sourceMessage = new MySerializableClass(content);
		MessageConverter messageConverter = new ObjectMessageConverter();
		Message<?> message = messageConverter.toMessage(sourceMessage, getMessageHeaders("UTF-8"));
		assertThat(Base64.isBase64(message.getPayload().toString().getBytes("UTF-8"))).isTrue();
		MySerializableClass result = (MySerializableClass) messageConverter.fromMessage(message,
				MySerializableClass.class);
		assertThat(result.getContent()).isEqualTo(content);
	}

	@Test
	void testToMessageAndFromMessageWithCustomEncoding() throws Exception {
		String content = "stringwithspecialcharsöäü€a8";
		MySerializableClass sourceMessage = new MySerializableClass(content);
		MessageConverter messageConverter = new ObjectMessageConverter("ISO-8859-1");
		Message<?> message = messageConverter.toMessage(sourceMessage, getMessageHeaders("ISO-8859-1"));
		assertThat(Base64.isBase64(message.getPayload().toString().getBytes("ISO-8859-1"))).isTrue();
		MySerializableClass result = (MySerializableClass) messageConverter.fromMessage(message,
				MySerializableClass.class);
		assertThat(result.getContent()).isEqualTo(content);
	}

	@Test
	void testWithWrongCharset() throws Exception {
		assertThatThrownBy(() -> new ObjectMessageConverter("someUnsupportedEncoding"))
				.isInstanceOf(UnsupportedCharsetException.class);
	}

	@Test
	void testPayloadIsNotAValidBase64Payload() throws Exception {

		ObjectMessageConverter messageConverter = new ObjectMessageConverter();
		assertThatThrownBy(() -> messageConverter.fromMessage(MessageBuilder.withPayload("test€").build(), null))
				.isInstanceOf(MessageConversionException.class)
				.hasMessageContaining("not a valid base64 encoded stream");
	}

	@Test
	void testPayloadIsNotAValidObjectStream() throws Exception {
		ObjectMessageConverter messageConverter = new ObjectMessageConverter();
		assertThatThrownBy(() -> messageConverter.fromMessage(MessageBuilder.withPayload("someStream").build(), null))
				.isInstanceOf(MessageConversionException.class).hasMessageContaining("Error reading payload");
	}

	private static final class MySerializableClass implements Serializable {

		private final String content;

		private MySerializableClass(String content) {
			this.content = content;
		}

		String getContent() {
			return this.content;
		}

	}

}
