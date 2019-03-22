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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.NumberUtils;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class TopicMessageChannel extends AbstractMessageChannel {

	/**
	 * Header name.
	 */
	public static final String NOTIFICATION_SUBJECT_HEADER = "NOTIFICATION_SUBJECT_HEADER";

	private final AmazonSNS amazonSns;

	private final String topicArn;

	public TopicMessageChannel(AmazonSNS amazonSns, String topicArn) {
		this.amazonSns = amazonSns;
		this.topicArn = topicArn;
	}

	private static String findNotificationSubject(Message<?> message) {
		return message.getHeaders().containsKey(NOTIFICATION_SUBJECT_HEADER)
				? message.getHeaders().get(NOTIFICATION_SUBJECT_HEADER).toString() : null;
	}

	@Override
	protected boolean sendInternal(Message<?> message, long timeout) {
		PublishRequest publishRequest = new PublishRequest(this.topicArn,
				message.getPayload().toString(), findNotificationSubject(message));
		Map<String, MessageAttributeValue> messageAttributes = getMessageAttributes(
				message);
		if (!messageAttributes.isEmpty()) {
			publishRequest.withMessageAttributes(messageAttributes);
		}
		this.amazonSns.publish(publishRequest);

		return true;
	}

	private Map<String, MessageAttributeValue> getMessageAttributes(Message<?> message) {
		HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		for (Map.Entry<String, Object> messageHeader : message.getHeaders().entrySet()) {
			String messageHeaderName = messageHeader.getKey();
			Object messageHeaderValue = messageHeader.getValue();

			if (MessageHeaders.CONTENT_TYPE.equals(messageHeaderName)
					&& messageHeaderValue != null) {
				messageAttributes.put(messageHeaderName,
						getContentTypeMessageAttribute(messageHeaderValue));
			}
			else if (MessageHeaders.ID.equals(messageHeaderName)
					&& messageHeaderValue != null) {
				messageAttributes.put(messageHeaderName,
						getStringMessageAttribute(messageHeaderValue.toString()));
			}
			else if (messageHeaderValue instanceof String) {
				messageAttributes.put(messageHeaderName,
						getStringMessageAttribute((String) messageHeaderValue));
			}
			else if (messageHeaderValue instanceof Number) {
				messageAttributes.put(messageHeaderName,
						getNumberMessageAttribute(messageHeaderValue));
			}
			else if (messageHeaderValue instanceof ByteBuffer) {
				messageAttributes.put(messageHeaderName,
						getBinaryMessageAttribute((ByteBuffer) messageHeaderValue));
			}
			else {
				this.logger.warn(String.format(
						"Message header with name '%s' and type '%s' cannot be sent as"
								+ " message attribute because it is not supported by SNS.",
						messageHeaderName, messageHeaderValue != null
								? messageHeaderValue.getClass().getName() : ""));
			}
		}

		return messageAttributes;
	}

	private MessageAttributeValue getBinaryMessageAttribute(
			ByteBuffer messageHeaderValue) {
		return new MessageAttributeValue().withDataType(MessageAttributeDataTypes.BINARY)
				.withBinaryValue(messageHeaderValue);
	}

	private MessageAttributeValue getContentTypeMessageAttribute(
			Object messageHeaderValue) {
		if (messageHeaderValue instanceof MimeType) {
			return new MessageAttributeValue()
					.withDataType(MessageAttributeDataTypes.STRING)
					.withStringValue(messageHeaderValue.toString());
		}
		else if (messageHeaderValue instanceof String) {
			return new MessageAttributeValue()
					.withDataType(MessageAttributeDataTypes.STRING)
					.withStringValue((String) messageHeaderValue);
		}
		return null;
	}

	private MessageAttributeValue getStringMessageAttribute(String messageHeaderValue) {
		return new MessageAttributeValue().withDataType(MessageAttributeDataTypes.STRING)
				.withStringValue(messageHeaderValue);
	}

	private MessageAttributeValue getNumberMessageAttribute(Object messageHeaderValue) {
		Assert.isTrue(
				NumberUtils.STANDARD_NUMBER_TYPES.contains(messageHeaderValue.getClass()),
				"Only standard number types are accepted as message header.");

		return new MessageAttributeValue()
				.withDataType(MessageAttributeDataTypes.NUMBER + "."
						+ messageHeaderValue.getClass().getName())
				.withStringValue(messageHeaderValue.toString());
	}

}
