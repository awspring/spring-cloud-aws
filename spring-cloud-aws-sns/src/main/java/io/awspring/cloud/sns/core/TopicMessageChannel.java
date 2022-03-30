/*
 * Copyright 2013-2022 the original author or authors.
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

package io.awspring.cloud.sns.core;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.NumberUtils;

import static io.awspring.cloud.sns.core.MessageHeaderCodes.MESSAGE_DEDUPLICATION_ID_HEADER;
import static io.awspring.cloud.sns.core.MessageHeaderCodes.MESSAGE_GROUP_ID_HEADER;
import static io.awspring.cloud.sns.core.MessageHeaderCodes.NOTIFICATION_SUBJECT_HEADER;

/**
 * Implementation of {@link AbstractMessageChannel} which is used for converting and
 * sending messages via {@link SnsClient} to SNS.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Gyozo Papp
 * @since 1.0
 */
public class TopicMessageChannel extends AbstractMessageChannel {

	private final JsonStringEncoder jsonStringEncoder = JsonStringEncoder.getInstance();

	private final SnsClient snsClient;

	private final String topicArn;

	public TopicMessageChannel(SnsClient snsClient, String topicArn) {
		this.snsClient = snsClient;
		this.topicArn = topicArn;
	}

	private static String findNotificationSubject(Message<?> message) {
		return message.getHeaders().containsKey(NOTIFICATION_SUBJECT_HEADER)
				? message.getHeaders().get(NOTIFICATION_SUBJECT_HEADER).toString() : null;
	}

	@Override
	protected boolean sendInternal(Message<?> message, long timeout) {
		PublishRequest.Builder publishRequestBuilder = PublishRequest.builder();
		publishRequestBuilder.topicArn(this.topicArn).message(message.getPayload().toString())
				.subject(findNotificationSubject(message));
		Map<String, MessageAttributeValue> messageAttributes = getMessageAttributes(message);

		Optional.of(messageAttributes).ifPresent(publishRequestBuilder::messageAttributes);
		Optional.ofNullable(message.getHeaders().get(MESSAGE_GROUP_ID_HEADER, String.class))
				.ifPresent(publishRequestBuilder::messageGroupId);
		Optional.ofNullable(message.getHeaders().get(MESSAGE_DEDUPLICATION_ID_HEADER, String.class))
				.ifPresent(publishRequestBuilder::messageDeduplicationId);

		this.snsClient.publish(publishRequestBuilder.build());
		return true;
	}

	private Map<String, MessageAttributeValue> getMessageAttributes(Message<?> message) {
		HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		for (Map.Entry<String, Object> messageHeader : message.getHeaders().entrySet()) {
			String messageHeaderName = messageHeader.getKey();
			Object messageHeaderValue = messageHeader.getValue();

			if (isSkipHeader(messageHeaderName)) {
				continue;
			}

			if (MessageHeaders.CONTENT_TYPE.equals(messageHeaderName) && messageHeaderValue != null) {
				messageAttributes.put(messageHeaderName, getContentTypeMessageAttribute(messageHeaderValue));
			}
			else if (MessageHeaders.ID.equals(messageHeaderName) && messageHeaderValue != null) {
				messageAttributes.put(messageHeaderName, getStringMessageAttribute(messageHeaderValue.toString()));
			}
			else if (messageHeaderValue instanceof String) {
				messageAttributes.put(messageHeaderName, getStringMessageAttribute((String) messageHeaderValue));
			}
			else if (messageHeaderValue instanceof Number) {
				messageAttributes.put(messageHeaderName, getNumberMessageAttribute(messageHeaderValue));
			}
			else if (messageHeaderValue instanceof ByteBuffer) {
				messageAttributes.put(messageHeaderName, getBinaryMessageAttribute((ByteBuffer) messageHeaderValue));
			}
			else if (messageHeaderValue instanceof List) {
				messageAttributes.put(messageHeaderName,
						getStringArrayMessageAttribute((List<Object>) messageHeaderValue));
			}
			else {
				this.logger.warn(String.format(
						"Message header with name '%s' and type '%s' cannot be sent as"
								+ " message attribute because it is not supported by SNS.",
						messageHeaderName, messageHeaderValue != null ? messageHeaderValue.getClass().getName() : ""));
			}
		}

		return messageAttributes;
	}

	private boolean isSkipHeader(String headerName) {
		return MESSAGE_GROUP_ID_HEADER.equals(headerName) || MESSAGE_DEDUPLICATION_ID_HEADER.equals(headerName);
	}

	private MessageAttributeValue getStringArrayMessageAttribute(List<Object> messageHeaderValue) {

		String stringValue = "[" + messageHeaderValue.stream()
				.map(item -> "\"" + String.valueOf(jsonStringEncoder.quoteAsString(item.toString())) + "\"")
				.collect(Collectors.joining(", ")) + "]";

		return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING_ARRAY).stringValue(stringValue)
				.build();
	}

	private MessageAttributeValue getBinaryMessageAttribute(ByteBuffer messageHeaderValue) {
		return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.BINARY)
				.binaryValue(SdkBytes.fromByteBuffer(messageHeaderValue)).build();
	}

	private MessageAttributeValue getContentTypeMessageAttribute(Object messageHeaderValue) {
		if (messageHeaderValue instanceof MimeType) {
			return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING)
					.stringValue(messageHeaderValue.toString()).build();
		}
		else if (messageHeaderValue instanceof String) {
			return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING)
					.stringValue((String) messageHeaderValue).build();
		}
		return null;
	}

	private MessageAttributeValue getStringMessageAttribute(String messageHeaderValue) {
		return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING)
				.stringValue(messageHeaderValue).build();
	}

	private MessageAttributeValue getNumberMessageAttribute(Object messageHeaderValue) {
		Assert.isTrue(NumberUtils.STANDARD_NUMBER_TYPES.contains(messageHeaderValue.getClass()),
				"Only standard number types are accepted as message header.");

		return MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.NUMBER + "." + messageHeaderValue.getClass().getName())
				.stringValue(messageHeaderValue.toString()).build();
	}

}
