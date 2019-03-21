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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cloud.aws.messaging.core.MessageAttributeDataTypes;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.NumberUtils;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class NotificationRequestConverter implements MessageConverter {

	private final ObjectMapper jsonMapper = new ObjectMapper();

	private final MessageConverter payloadConverter;

	public NotificationRequestConverter(MessageConverter payloadConverter) {
		this.payloadConverter = payloadConverter;
	}

	private static Map<String, Object> getMessageAttributesAsMessageHeaders(
			JsonNode message) {
		Map<String, Object> messageHeaders = new HashMap<>();
		Iterator<String> fieldNames = message.fieldNames();
		while (fieldNames.hasNext()) {
			String attributeName = fieldNames.next();
			String attributeValue = message.get(attributeName).get("Value").asText();
			String attributeType = message.get(attributeName).get("Type").asText();
			if (MessageHeaders.CONTENT_TYPE.equals(attributeName)) {
				messageHeaders.put(MessageHeaders.CONTENT_TYPE,
						MimeType.valueOf(attributeValue));
			}
			else if (MessageHeaders.ID.equals(attributeName)) {
				messageHeaders.put(MessageHeaders.ID, UUID.fromString(attributeValue));
			}
			else {
				if (MessageAttributeDataTypes.STRING.equals(attributeType)) {
					messageHeaders.put(attributeName, attributeValue);
				}
				else if (attributeType.startsWith(MessageAttributeDataTypes.NUMBER)) {
					Object numberValue = getNumberValue(attributeType, attributeValue);
					if (numberValue != null) {
						messageHeaders.put(attributeName, numberValue);
					}
				}
				else if (MessageAttributeDataTypes.BINARY.equals(attributeName)) {
					messageHeaders.put(attributeName,
							ByteBuffer.wrap(attributeType.getBytes()));
				}
			}
		}

		return messageHeaders;
	}

	private static Object getNumberValue(String attributeType, String attributeValue) {
		String numberType = attributeType
				.substring(MessageAttributeDataTypes.NUMBER.length() + 1);
		try {
			Class<? extends Number> numberTypeClass = Class.forName(numberType)
					.asSubclass(Number.class);
			return NumberUtils.parseNumber(attributeValue, numberTypeClass);
		}
		catch (ClassNotFoundException e) {
			throw new MessagingException(String.format(
					"Message attribute with value '%s' and data type '%s' could not be converted "
							+ "into a Number because target class was not found.",
					attributeValue, attributeType), e);
		}
	}

	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		Assert.notNull(message, "message must not be null");
		Assert.notNull(targetClass, "target class must not be null");

		JsonNode jsonNode;
		try {
			jsonNode = this.jsonMapper.readTree(message.getPayload().toString());
		}
		catch (Exception e) {
			throw new MessageConversionException("Could not read JSON", e);
		}
		if (!jsonNode.has("Type")) {
			throw new MessageConversionException("Payload: '" + message.getPayload()
					+ "' does not contain a Type attribute", null);
		}

		if (!"Notification".equals(jsonNode.get("Type").asText())) {
			throw new MessageConversionException(
					"Payload: '" + message.getPayload() + "' is not a valid notification",
					null);
		}

		if (!jsonNode.has("Message")) {
			throw new MessageConversionException(
					"Payload: '" + message.getPayload() + "' does not contain a message",
					null);
		}

		String messagePayload = jsonNode.get("Message").asText();
		GenericMessage<String> genericMessage = new GenericMessage<>(messagePayload,
				getMessageAttributesAsMessageHeaders(jsonNode.path("MessageAttributes")));
		return new NotificationRequest(jsonNode.path("Subject").asText(),
				this.payloadConverter.fromMessage(genericMessage, targetClass));
	}

	@Override
	public Message<?> toMessage(Object payload, MessageHeaders headers) {
		throw new UnsupportedOperationException(
				"This converter only supports reading a SNS notification and not writing them");
	}

	/**
	 * Notification request wrapper.
	 */
	public static class NotificationRequest {

		private final String subject;

		private final Object message;

		public NotificationRequest(String subject, Object message) {
			this.subject = subject;
			this.message = message;
		}

		public String getSubject() {
			return this.subject;
		}

		public Object getMessage() {
			return this.message;
		}

	}

}
