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

package org.elasticspring.messaging.core.sqs;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.channel.AbstractMessageChannel;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueMessageChannel extends AbstractMessageChannel implements PollableChannel {

	private final AmazonSQS amazonSqs;
	private final String queueUrl;

	public QueueMessageChannel(AmazonSQS amazonSqs, String queueUrl) {
		this.amazonSqs = amazonSqs;
		this.queueUrl = queueUrl;
	}

	//TODO: Error handling
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

	// returns 0 if there is a negative value for the delay seconds
	private static int getDelaySeconds(long timeout) {
		return Math.max(Long.valueOf(timeout).intValue(), 0);
	}

	@Override
	public Message<?> receive() {
		return this.receive(0);
	}

	@Override
	public Message<?> receive(long timeout) {
		ReceiveMessageResult receiveMessageResult = this.amazonSqs.receiveMessage(new ReceiveMessageRequest(this.queueUrl).withMaxNumberOfMessages(1).withWaitTimeSeconds(Long.valueOf(timeout).intValue()));
		if (receiveMessageResult.getMessages().isEmpty()) {
			throw new MessagingException("Error receiving message within specified timeout");
		}
		Message<String> message = MessageBuilder.withPayload(receiveMessageResult.getMessages().get(0).getBody()).build();
		this.amazonSqs.deleteMessage(new DeleteMessageRequest(this.queueUrl,receiveMessageResult.getMessages().get(0).getReceiptHandle()));
		return message;
	}
}