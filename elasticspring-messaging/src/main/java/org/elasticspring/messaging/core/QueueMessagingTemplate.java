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

package org.elasticspring.messaging.core;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.elasticspring.messaging.core.support.AbstractMessageChannelMessagingSendingTemplate;
import org.elasticspring.messaging.support.destination.DynamicQueueUrlDestinationResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.core.MessageReceivingOperations;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class QueueMessagingTemplate extends AbstractMessageChannelMessagingSendingTemplate implements MessageReceivingOperations<String> {

	private final AmazonSQS amazonSqs;

	public QueueMessagingTemplate(AmazonSQS amazonSqs) {
		super(new DynamicQueueUrlDestinationResolver(amazonSqs));
		this.amazonSqs = amazonSqs;
	}

	@Override
	protected PollableChannel resolveMessageChannel(String physicalResourceIdentifier) {
		return new QueueMessageChannel(this.amazonSqs, physicalResourceIdentifier);
	}

	@Override
	public Message<?> receive() throws MessagingException {
		return receive(getRequiredDefaultDestination());
	}

	@Override
	public Message<?> receive(String destination) throws MessagingException {
		return resolveMessageChannelByLogicalName(destination).receive();
	}

	@Override
	public <T> T receiveAndConvert(Class<T> targetClass) throws MessagingException {
		return receiveAndConvert(getRequiredDefaultDestination(), targetClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T receiveAndConvert(String destination, Class<T> targetClass) throws MessagingException {
		Message<?> message = receive(destination);
		if (message != null) {
			return (T) getMessageConverter().fromMessage(message, targetClass);
		}
		else {
			return null;
		}
	}

	public static class QueueMessageChannel extends AbstractMessageChannel implements PollableChannel {

		private static final String RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME = "ReceiptHandle";
		private static final String MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME = "MessageId";
		private final AmazonSQS amazonSqs;
		private final String queueUrl;
		static final String MESSAGE_RECEIVING_ATTRIBUTE_NAMES = "All";

		public QueueMessageChannel(AmazonSQS amazonSqs, String queueUrl) {
			this.amazonSqs = amazonSqs;
			this.queueUrl = queueUrl;
		}

		@Override
		protected boolean sendInternal(Message<?> message, long timeout) {
			try {
				this.amazonSqs.sendMessage(new SendMessageRequest(this.queueUrl, String.valueOf(message.getPayload())).
						withDelaySeconds(getDelaySeconds(timeout)));
			} catch (AmazonServiceException e) {
				throw new MessageDeliveryException(message, e.getMessage(), e);
			}

			return true;
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
							withAttributeNames(MESSAGE_RECEIVING_ATTRIBUTE_NAMES));
			if (receiveMessageResult.getMessages().isEmpty()) {
				return null;
			}
			com.amazonaws.services.sqs.model.Message amazonMessage = receiveMessageResult.getMessages().get(0);
			Message<String> message = createMessage(amazonMessage);
			this.amazonSqs.deleteMessage(new DeleteMessageRequest(this.queueUrl, amazonMessage.getReceiptHandle()));
			return message;
		}

		// returns 0 if there is a negative value for the delay seconds
		private static int getDelaySeconds(long timeout) {
			return Math.max(Long.valueOf(timeout).intValue(), 0);
		}

		private Message<String> createMessage(com.amazonaws.services.sqs.model.Message message) {
			MessageBuilder<String> builder = MessageBuilder.withPayload(message.getBody());
			builder.setHeader(MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME, message.getMessageId());
			builder.setHeader(RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME, message.getReceiptHandle());

			for (Map.Entry<String, String> attributeKeyValuePair : message.getAttributes().entrySet()) {
				builder.setHeader(attributeKeyValuePair.getKey(), attributeKeyValuePair.getValue());
			}

			return builder.build();
		}
	}
}
