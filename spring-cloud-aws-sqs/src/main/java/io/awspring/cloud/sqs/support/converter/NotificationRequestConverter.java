/*
 * Copyright 2013-2023 the original author or authors.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * @author Michael Sosa
 */
public class NotificationRequestConverter implements MessageConverter {

	private final ObjectMapper jsonMapper = new ObjectMapper();

	private final MessageConverter payloadConverter;

	public NotificationRequestConverter(MessageConverter payloadConverter) {
		this.payloadConverter = payloadConverter;
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
			throw new MessageConversionException(
				"Payload: '" + message.getPayload() + "' does not contain a Type attribute", null);
		}

		if (!"Notification".equals(jsonNode.get("Type").asText())) {
			throw new MessageConversionException("Payload: '" + message.getPayload() + "' is not a valid notification",
				null);
		}

		if (!jsonNode.has("Message")) {
			throw new MessageConversionException("Payload: '" + message.getPayload() + "' does not contain a message",
				null);
		}

		String messagePayload = jsonNode.get("Message").asText();
		GenericMessage<String> genericMessage = new GenericMessage<>(messagePayload);
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
