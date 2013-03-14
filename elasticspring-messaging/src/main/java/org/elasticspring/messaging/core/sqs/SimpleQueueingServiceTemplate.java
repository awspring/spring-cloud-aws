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

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.elasticspring.messaging.StringMessage;
import org.elasticspring.messaging.core.QueueingOperations;
import org.elasticspring.messaging.support.converter.MessageConverter;
import org.elasticspring.messaging.support.converter.StringMessageConverter;
import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.elasticspring.messaging.support.destination.DestinationResolver;
import org.elasticspring.messaging.support.destination.DynamicQueueDestinationResolver;
import org.springframework.util.Assert;

/**
 *
 */
public class SimpleQueueingServiceTemplate implements QueueingOperations {

	private final AmazonSQS amazonSQS;
	private final DestinationResolver destinationResolver;
	private String defaultDestinationName;
	private MessageConverter messageConverter = new StringMessageConverter();

	public SimpleQueueingServiceTemplate(AmazonSQS amazonSQS) {
		this.amazonSQS = amazonSQS;
		this.destinationResolver = new CachingDestinationResolver(new DynamicQueueDestinationResolver(this.amazonSQS));
	}

	public void setDefaultDestinationName(String defaultDestinationName) {
		this.defaultDestinationName = defaultDestinationName;
	}

	@Override
	public void convertAndSend(Object payLoad) {
		Assert.isTrue(this.defaultDestinationName != null, "No default destination name configured for this template.");
		this.convertAndSend(this.defaultDestinationName, payLoad);
	}

	@Override
	public void convertAndSend(String destinationName, Object payLoad) {
		Assert.notNull(destinationName, "destinationName must not be null.");
		String destinationUrl = this.destinationResolver.resolveDestinationName(destinationName);
		org.elasticspring.messaging.Message<String> message = this.getMessageConverter().toMessage(payLoad);
		SendMessageRequest request = new SendMessageRequest(destinationUrl, message.getPayload());
		this.amazonSQS.sendMessage(request);
	}

	@Override
	public Object receiveAndConvert() {
		Assert.isTrue(this.defaultDestinationName != null, "No default destination name configured for this template.");
		return this.receiveAndConvert(this.defaultDestinationName);
	}

	@Override
	public Object receiveAndConvert(String destinationName) {
		Assert.notNull(destinationName, "destinationName must not be null.");
		String destinationUrl = this.destinationResolver.resolveDestinationName(destinationName);
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(destinationUrl).withMaxNumberOfMessages(1);
		ReceiveMessageResult receiveMessageResult = this.amazonSQS.receiveMessage(receiveMessageRequest);
		if (receiveMessageResult.getMessages().isEmpty()) {
			return null;
		}

		Message message = receiveMessageResult.getMessages().get(0);

		org.elasticspring.messaging.Message<String> msg = new StringMessage(message.getBody());
		Object result = this.getMessageConverter().fromMessage(msg);

		this.amazonSQS.deleteMessage(new DeleteMessageRequest(destinationUrl, message.getReceiptHandle()));

		return result;
	}

	// TODO create a method for receive with expected type

	protected MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

}