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

package io.awspring.cloud.messaging.support.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.sns.message.SnsMessageManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.core.MessageAttributeDataTypes;
import io.awspring.cloud.messaging.core.QueueMessageUtils;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Manuel Wessner
 * @since 1.0
 */
public class NotificationRequestConverter implements MessageConverter {

	private final ObjectMapper jsonMapper = new ObjectMapper();

	private final MessageConverter payloadConverter;

	private final SnsMessageManager snsMessageManager;

	public NotificationRequestConverter(MessageConverter payloadConverter, SnsMessageManager snsMessageManager) {
		this.payloadConverter = payloadConverter;
		this.snsMessageManager = snsMessageManager;
	}

	private static Map<String, Object> getMessageAttributesAsMessageHeaders(JsonNode message) {
		Map<String, Object> messageHeaders = new HashMap<>();
		Iterator<String> fieldNames = message.fieldNames();
		while (fieldNames.hasNext()) {
			String attributeName = fieldNames.next();
			String attributeValue = message.get(attributeName).get("Value").asText();
			String attributeType = message.get(attributeName).get("Type").asText();
			if (MessageHeaders.CONTENT_TYPE.equals(attributeName)) {
				messageHeaders.put(MessageHeaders.CONTENT_TYPE, MimeType.valueOf(attributeValue));
			}
			else if (MessageHeaders.ID.equals(attributeName)) {
				messageHeaders.put(MessageHeaders.ID, UUID.fromString(attributeValue));
			}
			else {
				if (MessageAttributeDataTypes.STRING.equals(attributeType)) {
					messageHeaders.put(attributeName, attributeValue);
				}
				else if (attributeType.startsWith(MessageAttributeDataTypes.NUMBER)) {
					messageHeaders.put(attributeName, QueueMessageUtils.getNumberValue(attributeValue, attributeType));
				}
				else if (MessageAttributeDataTypes.BINARY.equals(attributeName)) {
					messageHeaders.put(attributeName, ByteBuffer.wrap(attributeType.getBytes()));
				}
			}
		}

		return messageHeaders;
	}

	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		Assert.notNull(message, "message must not be null");
		Assert.notNull(targetClass, "target class must not be null");

		String payload = message.getPayload().toString();
		JsonNode jsonNode;
		try {
			jsonNode = this.jsonMapper.readTree(payload);
		}
		catch (Exception e) {
			throw new MessageConversionException("Could not read JSON", e);
		}
		if (!jsonNode.has("Type")) {
			throw new MessageConversionException("Payload: '" + payload + "' does not contain a Type attribute", null);
		}

		if (!"Notification".equals(jsonNode.get("Type").asText())) {
			throw new MessageConversionException("Payload: '" + payload + "' is not a valid notification", null);
		}

		if (!jsonNode.has("Message")) {
			throw new MessageConversionException("Payload: '" + payload + "' does not contain a message", null);
		}

		if (jsonNode.has("SignatureVersion")) {
			verifySignature(payload);
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

	private void verifySignature(String payload) {
		try (InputStream messageStream = new ByteArrayInputStream(payload.getBytes())) {
			// Unmarshalling the message is not needed, but also done here
			snsMessageManager.parseMessage(messageStream);
		}
		catch (IOException e) {
			throw new MessageConversionException("Issue while verifying signature of Payload: '" + payload + "'", e);
		}
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
