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

package org.elasticspring.messaging.core;

import com.amazonaws.services.sqs.AmazonSQS;
import org.elasticspring.core.env.ResourceIdResolver;
import org.elasticspring.messaging.core.support.AbstractMessageChannelMessagingSendingTemplate;
import org.elasticspring.messaging.support.destination.DynamicQueueUrlDestinationResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolvingMessageReceivingOperations;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class QueueMessagingTemplate extends AbstractMessageChannelMessagingSendingTemplate<QueueMessageChannel> implements DestinationResolvingMessageReceivingOperations<QueueMessageChannel> {

	private final AmazonSQS amazonSqs;

	public QueueMessagingTemplate(AmazonSQS amazonSqs, ResourceIdResolver resourceIdResolver) {
		super(new DynamicQueueUrlDestinationResolver(amazonSqs, resourceIdResolver));
		this.amazonSqs = amazonSqs;
	}

	public QueueMessagingTemplate(AmazonSQS amazonSqs) {
		this(amazonSqs, null);
	}

	@Override
	protected QueueMessageChannel resolveMessageChannel(String physicalResourceIdentifier) {
		return new QueueMessageChannel(this.amazonSqs, physicalResourceIdentifier);
	}

	@Override
	public Message<?> receive() throws MessagingException {
		return receive(getRequiredDefaultDestination());
	}

	@Override
	public Message<?> receive(QueueMessageChannel destination) throws MessagingException {
		return destination.receive();
	}

	@Override
	public <T> T receiveAndConvert(Class<T> targetClass) throws MessagingException {
		return receiveAndConvert(getRequiredDefaultDestination(), targetClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T receiveAndConvert(QueueMessageChannel destination, Class<T> targetClass) throws MessagingException {
		Message<?> message = destination.receive();
		if (message != null) {
			return (T) getMessageConverter().fromMessage(message, targetClass);
		} else {
			return null;
		}
	}

	@Override
	public Message<?> receive(String destinationName) throws MessagingException {
		return resolveMessageChannelByLogicalName(destinationName).receive();
	}

	@Override
	public <T> T receiveAndConvert(String destinationName, Class<T> targetClass) throws MessagingException {
		QueueMessageChannel channel = resolveMessageChannelByLogicalName(destinationName);
		return receiveAndConvert(channel, targetClass);
	}
}
