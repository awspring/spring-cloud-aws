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
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class QueueMessageChannel extends AbstractMessageChannel implements PollableChannel {

	static final String MESSAGE_RECEIVING_ATTRIBUTE_NAMES = "All";
	private static final String RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME = "ReceiptHandle";
	private static final String MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME = "MessageId";
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
			Map<String, MessageAttributeValue> messageAttributes = getContentTypeMessageAttributes(message);
			if (!messageAttributes.isEmpty()) {
				sendMessageRequest.withMessageAttributes(messageAttributes);
			}
			this.amazonSqs.sendMessage(sendMessageRequest);
		} catch (AmazonServiceException e) {
			throw new MessageDeliveryException(message, e.getMessage(), e);
		}

		return true;
	}

	private Map<String, MessageAttributeValue> getContentTypeMessageAttributes(Message<?> message) {
		Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>(1);
		Object mimeType = message.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		if (mimeType != null) {
			if (mimeType instanceof MimeType) {
				messageAttributes.put(MessageHeaders.CONTENT_TYPE, new MessageAttributeValue().withDataType("String").withStringValue(mimeType.toString()));
			} else if (mimeType instanceof String) {
				messageAttributes.put(MessageHeaders.CONTENT_TYPE, new MessageAttributeValue().withDataType("String").withStringValue((String) mimeType));
			}
		}

		return messageAttributes;
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
						withAttributeNames(MESSAGE_RECEIVING_ATTRIBUTE_NAMES).
						withMessageAttributeNames(MessageHeaders.CONTENT_TYPE));
		if (receiveMessageResult.getMessages().isEmpty()) {
			return null;
		}
		com.amazonaws.services.sqs.model.Message amazonMessage = receiveMessageResult.getMessages().get(0);
		Message<String> message = createMessage(amazonMessage);
		this.amazonSqs.deleteMessage(new DeleteMessageRequest(this.queueUrl, amazonMessage.getReceiptHandle()));
		return message;
	}

	private Message<String> createMessage(com.amazonaws.services.sqs.model.Message message) {
		MessageBuilder<String> messageBuilder = MessageBuilder.withPayload(message.getBody());
		messageBuilder.setHeader(MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME, message.getMessageId());
		messageBuilder.setHeader(RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME, message.getReceiptHandle());

		for (Map.Entry<String, String> attributeKeyValuePair : message.getAttributes().entrySet()) {
			messageBuilder.setHeader(attributeKeyValuePair.getKey(), attributeKeyValuePair.getValue());
		}

		if (message.getMessageAttributes().containsKey(MessageHeaders.CONTENT_TYPE)) {
			messageBuilder.setHeader(MessageHeaders.CONTENT_TYPE,
					MimeType.valueOf(message.getMessageAttributes().get(MessageHeaders.CONTENT_TYPE).getStringValue()));
		}

		return messageBuilder.build();
	}

	// returns 0 if there is a negative value for the delay seconds
	private static int getDelaySeconds(long timeout) {
		return Math.max(Long.valueOf(timeout).intValue(), 0);
	}
}
