/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.support.converter;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;

import java.io.Serializable;
import java.nio.charset.UnsupportedCharsetException;

/**
 *
 */
public class ObjectMessageConverterTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testToMessageAndFromMessage() throws Exception {
		String content = "stringwithspecialcharsöäü€a8";
		MySerializableClass sourceMessage = new MySerializableClass(content);
		MessageConverter messageConverter = new ObjectMessageConverter();
		Message<?> message = messageConverter.toMessage(sourceMessage, null);
		Assert.assertTrue(Base64.isBase64(message.getPayload().toString().getBytes("UTF-8")));
		MySerializableClass result = (MySerializableClass) messageConverter.fromMessage(message,MySerializableClass.class);
		Assert.assertEquals(content, result.getContent());
	}

	@Test
	public void testToMessageAndFromMessageWithCustomEncoding() throws Exception {
		String content = "stringwithspecialcharsöäü€a8";
		MySerializableClass sourceMessage = new MySerializableClass(content);
		MessageConverter messageConverter = new ObjectMessageConverter("ISO-8859-1");
		Message<?> message = messageConverter.toMessage(sourceMessage, null);
		Assert.assertTrue(Base64.isBase64(message.getPayload().toString().getBytes("ISO-8859-1")));
		MySerializableClass result = (MySerializableClass) messageConverter.fromMessage(message,MySerializableClass.class);
		Assert.assertEquals(content, result.getContent());
	}

	@Test(expected = UnsupportedCharsetException.class)
	public void testWithWrongCharset() throws Exception {
		//noinspection ResultOfObjectAllocationIgnored
		new ObjectMessageConverter("someUnsupportedEncoding");
	}

	@Test
	public void testPayloadIsNotAValidBase64Payload() throws Exception {
		this.expectedException.expect(MessageConversionException.class);
		this.expectedException.expectMessage("not a valid base64 encoded stream");

		ObjectMessageConverter messageConverter = new ObjectMessageConverter();
		messageConverter.fromMessage(MessageBuilder.withPayload("test€").build(),null);
	}

	@Test
	public void testPayloadIsNotAValidObjectStream() throws Exception {
		this.expectedException.expect(MessageConversionException.class);
		this.expectedException.expectMessage("Error reading payload");

		ObjectMessageConverter messageConverter = new ObjectMessageConverter();
		messageConverter.fromMessage(MessageBuilder.withPayload("someStream").build(),null);
	}

	private static class MySerializableClass implements Serializable {

		private final String content;

		private MySerializableClass(String content) {
			this.content = content;
		}

		public String getContent() {
			return this.content;
		}
	}
}
