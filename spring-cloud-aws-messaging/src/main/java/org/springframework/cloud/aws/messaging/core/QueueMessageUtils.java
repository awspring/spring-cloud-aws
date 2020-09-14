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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.sqs.model.MessageAttributeValue;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;
import org.springframework.util.NumberUtils;

/**
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @since 1.0
 */
public final class QueueMessageUtils {

	private static final String RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME = "ReceiptHandle";

	private static final String MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME = "MessageId";

	private static final String SOURCE_DATA_HEADER = "sourceData";

	private QueueMessageUtils() {
		// Avoid instantiation
	}

	public static Message<String> createMessage(
			com.amazonaws.services.sqs.model.Message message) {
		return createMessage(message, Collections.emptyMap());
	}

	public static Message<String> createMessage(
			com.amazonaws.services.sqs.model.Message message,
			Map<String, Object> additionalHeaders) {
		HashMap<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put(MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME, message.getMessageId());
		messageHeaders.put(RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME,
				message.getReceiptHandle());
		messageHeaders.put(SOURCE_DATA_HEADER, message);

		messageHeaders.putAll(additionalHeaders);
		messageHeaders.putAll(getAttributesAsMessageHeaders(message));
		messageHeaders.putAll(getMessageAttributesAsMessageHeaders(message));

		return new GenericMessage<>(message.getBody(),
				new SqsMessageHeaders(messageHeaders));
	}

	public static Object getNumberValue(String attributeValue, String attributeType) {
		try {
			return NumberParser.parseNumber(attributeValue, attributeType);
		}
		catch (ClassNotFoundException e) {
			throw new MessagingException(String.format(
					"Message attribute with value '%s' and data type '%s' could not be converted "
							+ "into a Number because target class was not found.",
					attributeValue, attributeType), e);
		}
	}

	public static com.amazonaws.services.sqs.model.Message getSourceData(
			Message<?> message) {
		return (com.amazonaws.services.sqs.model.Message) message.getHeaders()
				.get(SOURCE_DATA_HEADER);
	}

	private static Map<String, Object> getAttributesAsMessageHeaders(
			com.amazonaws.services.sqs.model.Message message) {
		Map<String, Object> messageHeaders = new HashMap<>();
		for (Map.Entry<String, String> attributeKeyValuePair : message.getAttributes()
				.entrySet()) {
			messageHeaders.put(attributeKeyValuePair.getKey(),
					attributeKeyValuePair.getValue());
		}

		return messageHeaders;
	}

	private static Map<String, Object> getMessageAttributesAsMessageHeaders(
			com.amazonaws.services.sqs.model.Message message) {
		Map<String, Object> messageHeaders = new HashMap<>();
		for (Map.Entry<String, MessageAttributeValue> messageAttribute : message
				.getMessageAttributes().entrySet()) {
			if (MessageHeaders.CONTENT_TYPE.equals(messageAttribute.getKey())) {
				messageHeaders.put(MessageHeaders.CONTENT_TYPE,
						MimeType.valueOf(messageAttribute.getValue().getStringValue()));
			}
			else if (MessageHeaders.ID.equals(messageAttribute.getKey())) {
				messageHeaders.put(MessageHeaders.ID,
						UUID.fromString(messageAttribute.getValue().getStringValue()));
			}
			else if (messageAttribute.getValue().getDataType()
					.startsWith(MessageAttributeDataTypes.STRING)) {
				messageHeaders.put(messageAttribute.getKey(),
						messageAttribute.getValue().getStringValue());
			}
			else if (messageAttribute.getValue().getDataType()
					.startsWith(MessageAttributeDataTypes.NUMBER)) {
				messageHeaders.put(messageAttribute.getKey(),
						getNumberValue(messageAttribute.getValue()));
			}
			else if (messageAttribute.getValue().getDataType()
					.startsWith(MessageAttributeDataTypes.BINARY)) {
				messageHeaders.put(messageAttribute.getKey(),
						messageAttribute.getValue().getBinaryValue());
			}
		}

		return messageHeaders;
	}

	private static Object getNumberValue(MessageAttributeValue value) {
		return getNumberValue(value.getStringValue(), value.getDataType());
	}

	private static class NumberParser {

		private static final Map<String, Class<? extends Number>> PRIMITIVE_TO_WRAPPED = new HashMap<>();

		static {
			PRIMITIVE_TO_WRAPPED.put(byte.class.getName(), Byte.class);
			PRIMITIVE_TO_WRAPPED.put(short.class.getName(), Short.class);
			PRIMITIVE_TO_WRAPPED.put(int.class.getName(), Integer.class);
			PRIMITIVE_TO_WRAPPED.put(long.class.getName(), Long.class);
			PRIMITIVE_TO_WRAPPED.put(float.class.getName(), Float.class);
			PRIMITIVE_TO_WRAPPED.put(double.class.getName(), Double.class);
		}

		public static Object parseNumber(String value, String type)
				throws ClassNotFoundException {
			if (MessageAttributeDataTypes.NUMBER.equals(type)) {
				return NumberUtils.parseNumber(value, Number.class);
			}
			else {
				String javaType = type
						.substring(MessageAttributeDataTypes.NUMBER.length() + 1);

				if (PRIMITIVE_TO_WRAPPED.containsKey(javaType.toLowerCase())) {
					return NumberUtils.parseNumber(value,
							PRIMITIVE_TO_WRAPPED.get(javaType.toLowerCase()));
				}
				else {
					Class<? extends Number> numberTypeClass = Class.forName(javaType)
							.asSubclass(Number.class);
					return NumberUtils.parseNumber(value, numberTypeClass);
				}
			}
		}

	}

}
