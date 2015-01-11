/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.messaging.core;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.NumberUtils;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class QueueMessageChannel extends AbstractMessageChannel implements PollableChannel {

	static final String ATTRIBUTE_NAMES = "All";
	private static final String RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME = "ReceiptHandle";
	private static final String MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME = "MessageId";
	public static final String MESSAGE_ATTRIBUTE_NAMES = "All";
	private final AmazonSQS amazonSqs;
	private final String queueUrl;

	public QueueMessageChannel(AmazonSQS amazonSqs, String queueUrl) {
		this.amazonSqs = amazonSqs;
		this.queueUrl = queueUrl;
	}

	@Override
	protected boolean sendInternal(Message<?> message, long timeout) {
		try {
			SendMessageRequest sendMessageRequest = new SendMessageRequest(this.queueUrl, String.valueOf(message.getPayload())).withDelaySeconds(getDelaySeconds(timeout));
			Map<String, MessageAttributeValue> messageAttributes = getMessageAttributes(message);
			if (!messageAttributes.isEmpty()) {
				sendMessageRequest.withMessageAttributes(messageAttributes);
			}
			this.amazonSqs.sendMessage(sendMessageRequest);
		} catch (AmazonServiceException e) {
			throw new MessageDeliveryException(message, e.getMessage(), e);
		}

		return true;
	}

	private Map<String, MessageAttributeValue> getMessageAttributes(Message<?> message) {
		HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		for (Map.Entry<String, Object> messageHeader : message.getHeaders().entrySet()) {
			String messageHeaderName = messageHeader.getKey();
			Object messageHeaderValue = messageHeader.getValue();

			if (MessageHeaders.CONTENT_TYPE.equals(messageHeaderName) && messageHeaderValue != null) {
				messageAttributes.put(messageHeaderName, getContentTypeMessageAttribute(messageHeaderValue));
			} else if (messageHeaderValue instanceof String) {
				messageAttributes.put(messageHeaderName, getStringMessageAttribute((String) messageHeaderValue));
			} else if (messageHeaderValue instanceof Number) {
				messageAttributes.put(messageHeaderName, getNumberMessageAttribute(messageHeaderValue));
			} else if (messageHeaderValue instanceof ByteBuffer) {
				messageAttributes.put(messageHeaderName, getBinaryMessageAttribute((ByteBuffer) messageHeaderValue));
			} else {
				this.logger.warn(String.format("Message header with name '%s' and type '%s' cannot be sent as" +
								" message attribute because it is not supported by SQS.", messageHeaderName,
						messageHeaderValue != null ? messageHeaderValue.getClass().getName() : ""));
			}
		}

		return messageAttributes;
	}

	private MessageAttributeValue getBinaryMessageAttribute(ByteBuffer messageHeaderValue) {
		return new MessageAttributeValue().withDataType(MessageAttributeDataTypes.BINARY).withBinaryValue(messageHeaderValue);
	}

	private MessageAttributeValue getContentTypeMessageAttribute(Object messageHeaderValue) {
		if (messageHeaderValue instanceof MimeType) {
			return new MessageAttributeValue().withDataType(MessageAttributeDataTypes.STRING).withStringValue(messageHeaderValue.toString());
		} else if (messageHeaderValue instanceof String) {
			return new MessageAttributeValue().withDataType(MessageAttributeDataTypes.STRING).withStringValue((String) messageHeaderValue);
		}
		return null;
	}

	private MessageAttributeValue getStringMessageAttribute(String messageHeaderValue) {
		return new MessageAttributeValue().withDataType(MessageAttributeDataTypes.STRING).withStringValue(messageHeaderValue);
	}

	private MessageAttributeValue getNumberMessageAttribute(Object messageHeaderValue) {
		Assert.isTrue(NumberUtils.STANDARD_NUMBER_TYPES.contains(messageHeaderValue.getClass()), "Only standard number types are accepted as message header.");

		return new MessageAttributeValue().withDataType(MessageAttributeDataTypes.NUMBER + "." + messageHeaderValue.getClass().getName()).withStringValue(messageHeaderValue.toString());
	}

	@Override
	public Message<String> receive() {
		return this.receive(0);
	}

	@Override
	public Message<String> receive(long timeout) {
		ReceiveMessageResult receiveMessageResult = this.amazonSqs.receiveMessage(
				new ReceiveMessageRequest(this.queueUrl).
						withMaxNumberOfMessages(1).
						withWaitTimeSeconds(Long.valueOf(timeout).intValue()).
						withAttributeNames(ATTRIBUTE_NAMES).
						withMessageAttributeNames(MESSAGE_ATTRIBUTE_NAMES));
		if (receiveMessageResult.getMessages().isEmpty()) {
			return null;
		}
		com.amazonaws.services.sqs.model.Message amazonMessage = receiveMessageResult.getMessages().get(0);
		Message<String> message = createMessage(amazonMessage);
		this.amazonSqs.deleteMessage(new DeleteMessageRequest(this.queueUrl, amazonMessage.getReceiptHandle()));
		return message;
	}

	private Message<String> createMessage(com.amazonaws.services.sqs.model.Message message) {
		HashMap<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put(MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME, message.getMessageId());
		messageHeaders.put(RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME, message.getReceiptHandle());

		messageHeaders.putAll(getAttributesAsMessageHeaders(message));
		messageHeaders.putAll(getMessageAttributesAsMessageHeaders(message));

		return new GenericMessage<>(message.getBody(), messageHeaders);
	}

	private Map<String, Object> getAttributesAsMessageHeaders(com.amazonaws.services.sqs.model.Message message) {
		Map<String, Object> messageHeaders = new HashMap<>();
		for (Map.Entry<String, String> attributeKeyValuePair : message.getAttributes().entrySet()) {
			messageHeaders.put(attributeKeyValuePair.getKey(), attributeKeyValuePair.getValue());
		}

		return messageHeaders;
	}

	private Map<String, Object> getMessageAttributesAsMessageHeaders(com.amazonaws.services.sqs.model.Message message) {
		Map<String, Object> messageHeaders = new HashMap<>();
		for (Map.Entry<String, MessageAttributeValue> messageAttribute : message.getMessageAttributes().entrySet()) {
			if (MessageHeaders.CONTENT_TYPE.equals(messageAttribute.getKey())) {
				messageHeaders.put(MessageHeaders.CONTENT_TYPE,
						MimeType.valueOf(message.getMessageAttributes().get(MessageHeaders.CONTENT_TYPE).getStringValue()));
			} else if (MessageAttributeDataTypes.STRING.equals(messageAttribute.getValue().getDataType())) {
				messageHeaders.put(messageAttribute.getKey(), messageAttribute.getValue().getStringValue());
			} else if (messageAttribute.getValue().getDataType().startsWith(MessageAttributeDataTypes.NUMBER)) {
				Object numberValue = getNumberValue(messageAttribute.getValue());
				if (numberValue != null) {
					messageHeaders.put(messageAttribute.getKey(), numberValue);
				}
			} else if (MessageAttributeDataTypes.BINARY.equals(messageAttribute.getValue().getDataType())) {
				messageHeaders.put(messageAttribute.getKey(), messageAttribute.getValue().getBinaryValue());
			}
		}

		return messageHeaders;
	}

	private Object getNumberValue(MessageAttributeValue value) {
		String numberType = value.getDataType().substring(MessageAttributeDataTypes.NUMBER.length() + 1);
		try {
			Class<? extends Number> numberTypeClass = Class.forName(numberType).asSubclass(Number.class);
			return NumberUtils.parseNumber(value.getStringValue(), numberTypeClass);
		} catch (ClassNotFoundException e) {
			throw new MessagingException(String.format("Message attribute with value '%s' and data type '%s' could not be converted " +
					"into a Number because target class was not found.", value.getStringValue(), value.getDataType()), e);
		}
	}

	// returns 0 if there is a negative value for the delay seconds
	private static int getDelaySeconds(long timeout) {
		return Math.max(Long.valueOf(timeout).intValue(), 0);
	}
}
