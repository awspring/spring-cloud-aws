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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class JsonMessageConverter implements MessageConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageConverter.class);

	private final ObjectMapper converter = new ObjectMapper();

	@Override
	public Message<String> toMessage(Object payload, MessageHeaders header) {
		if (payload == null) {
			return null;
		}

		try {
			String messagePayload = this.converter.writeValueAsString(payload);
			return MessageBuilder.withPayload(messagePayload).copyHeaders(header).build();
		} catch (Exception e) {
			LOGGER.debug("Payload ({}) couldn't be converted to JSON", payload.getClass().getSimpleName(), e);
			return null;
		}
	}

	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		if (message == null || message.getPayload() == null || targetClass == null) {
			return null;
		}

		try {
			return this.converter.readValue(message.getPayload().toString(), targetClass);
		} catch (Exception e) {
			LOGGER.debug("Message payload couldn't be converted to the targetClass {}", targetClass.getSimpleName(), e);
			return null;
		}
	}
}