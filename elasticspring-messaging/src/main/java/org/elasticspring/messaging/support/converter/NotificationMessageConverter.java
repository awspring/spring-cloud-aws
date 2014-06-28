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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class NotificationMessageConverter implements MessageConverter {

	private final ObjectMapper objectMapper = new ObjectMapper();


	@Override
	public Message<?> toMessage(Object payload, MessageHeaders header) {
		throw new UnsupportedOperationException("This converter only supports reading a SNS notification and not writing them");
	}

	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		Assert.notNull(message, "message must not be null");

		try {
			JsonNode jsonNode = this.objectMapper.readValue(message.getPayload().toString(), JsonNode.class);
			if (!jsonNode.has("Type")) {
				throw new MessageConversionException("Payload: '" + message.getPayload() + "' does not contain a Type attribute",null);
			}

			if (!"Notification".equals(jsonNode.get("Type").asText())) {
				throw new MessageConversionException("Payload: '" + message.getPayload() + "' is not a valid notification",null);
			}

			if (!jsonNode.has("Message")) {
				throw new MessageConversionException("Payload: '" + message.getPayload() + "' does not contain a message",null);
			}

			return new NotificationRequest(nullSafeGetTextValue(jsonNode, "Subject"), nullSafeGetTextValue(jsonNode, "Message"));
		} catch (IOException e) {
			throw new MessageConversionException("Error reading payload :'" + message.getPayload() + "' from message", e);
		}
	}

	private static String nullSafeGetTextValue(JsonNode jsonNode, String attribute) {
		return jsonNode.has(attribute) ? jsonNode.get(attribute).asText() : null;
	}

	public static class NotificationRequest {
		private final String subject;
		private final String message;

		public NotificationRequest(String subject, String message) {
			this.subject = subject;
			this.message = message;
		}

		public String getSubject() {
			return this.subject;
		}

		public String getMessage() {
			return this.message;
		}
	}

}