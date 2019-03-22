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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class NotificationRequestConverterTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testWriteMessageNotSupported() throws Exception {
		this.expectedException.expect(UnsupportedOperationException.class);
		new NotificationRequestConverter(new StringMessageConverter()).toMessage("test",
				null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void fromMessage_withoutMessage_shouldThrowAnException() throws Exception {
		new NotificationRequestConverter(new StringMessageConverter()).fromMessage(null,
				String.class);
	}

	@Test
	public void fromMessage_withMessageAndSubject_shouldReturnMessage() throws Exception {
		// Arrange
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Subject", "Hello");
		jsonObject.put("Message", "World");
		String payload = jsonObject.toString();

		// Act
		Object notificationRequest = new NotificationRequestConverter(
				new StringMessageConverter()).fromMessage(
						MessageBuilder.withPayload(payload).build(), String.class);

		// Assert
		assertThat(NotificationRequestConverter.NotificationRequest.class
				.isInstance(notificationRequest)).isTrue();
		assertThat(
				((NotificationRequestConverter.NotificationRequest) notificationRequest)
						.getSubject()).isEqualTo("Hello");
		assertThat(
				((NotificationRequestConverter.NotificationRequest) notificationRequest)
						.getMessage()).isEqualTo("World");
	}

	@Test
	public void fromMessage_withMessageOnly_shouldReturnMessage() throws Exception {
		// Arrange
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Message", "World");
		String payload = jsonObject.toString();

		// Act
		Object notificationRequest = new NotificationRequestConverter(
				new StringMessageConverter()).fromMessage(
						MessageBuilder.withPayload(payload).build(), String.class);

		// Assert
		assertThat(NotificationRequestConverter.NotificationRequest.class
				.isInstance(notificationRequest)).isTrue();
		assertThat(
				((NotificationRequestConverter.NotificationRequest) notificationRequest)
						.getMessage()).isEqualTo("World");
	}

	@Test
	public void testNoTypeSupplied() throws Exception {
		this.expectedException.expect(MessageConversionException.class);
		this.expectedException.expectMessage("does not contain a Type attribute");
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Message", "Hello World!");
		String payload = jsonObject.toString();
		new NotificationRequestConverter(new StringMessageConverter())
				.fromMessage(MessageBuilder.withPayload(payload).build(), String.class);
	}

	@Test
	public void testWrongTypeSupplied() throws Exception {
		this.expectedException.expect(MessageConversionException.class);
		this.expectedException.expectMessage("is not a valid notification");
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Subscription");
		jsonObject.put("Message", "Hello World!");
		String payload = jsonObject.toString();
		new NotificationRequestConverter(new StringMessageConverter())
				.fromMessage(MessageBuilder.withPayload(payload).build(), String.class);
	}

	@Test
	public void testNoMessageAvailableSupplied() throws Exception {
		this.expectedException.expect(MessageConversionException.class);
		this.expectedException.expectMessage("does not contain a message");
		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Subject", "Hello World!");
		String payload = jsonObject.toString();
		new NotificationRequestConverter(new StringMessageConverter())
				.fromMessage(MessageBuilder.withPayload(payload).build(), String.class);
	}

	@Test
	public void testNoValidJson() throws Exception {
		this.expectedException.expect(MessageConversionException.class);
		this.expectedException.expectMessage("Could not read JSON");
		String message = "foo";
		new NotificationRequestConverter(new StringMessageConverter())
				.fromMessage(MessageBuilder.withPayload(message).build(), String.class);
	}

}
