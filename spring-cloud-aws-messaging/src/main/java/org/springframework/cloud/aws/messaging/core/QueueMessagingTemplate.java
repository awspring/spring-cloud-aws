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

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.core.support.AbstractMessageChannelMessagingSendingTemplate;
import org.springframework.cloud.aws.messaging.support.destination.DynamicQueueUrlDestinationResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.core.DestinationResolvingMessageReceivingOperations;

/**
 * <b>IMPORTANT</b>: For the message conversion this class always tries to first use the
 * {@link StringMessageConverter} as it fits the underlying message channel type. If a
 * message converter is set through the constructor then it is added to a composite
 * converter already containing the {@link StringMessageConverter}. If
 * {@link QueueMessagingTemplate#setMessageConverter(MessageConverter)} is used, then the
 * {@link CompositeMessageConverter} containing the {@link StringMessageConverter} will
 * not be used anymore and the {@code String} payloads are also going to be converted with
 * the set converter.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class QueueMessagingTemplate
		extends AbstractMessageChannelMessagingSendingTemplate<QueueMessageChannel>
		implements DestinationResolvingMessageReceivingOperations<QueueMessageChannel> {

	private final AmazonSQSAsync amazonSqs;

	public QueueMessagingTemplate(AmazonSQSAsync amazonSqs) {
		this(amazonSqs, (ResourceIdResolver) null, null);
	}

	public QueueMessagingTemplate(AmazonSQSAsync amazonSqs,
			ResourceIdResolver resourceIdResolver) {
		this(amazonSqs, resourceIdResolver, null);
	}

	/**
	 * Initializes the messaging template by configuring the resource Id resolver as well
	 * as the message converter. Uses the {@link DynamicQueueUrlDestinationResolver} with
	 * the default configuration to resolve destination names.
	 * @param amazonSqs The {@link AmazonSQS} client, cannot be {@code null}.
	 * @param resourceIdResolver The {@link ResourceIdResolver} to be used for resolving
	 * logical queue names.
	 * @param messageConverter A {@link MessageConverter} that is going to be added to the
	 * composite converter.
	 */
	public QueueMessagingTemplate(AmazonSQSAsync amazonSqs,
			ResourceIdResolver resourceIdResolver, MessageConverter messageConverter) {
		this(amazonSqs,
				new DynamicQueueUrlDestinationResolver(amazonSqs, resourceIdResolver),
				messageConverter);
	}

	/**
	 * Initializes the messaging template by configuring the destination resolver as well
	 * as the message converter. Uses the {@link DynamicQueueUrlDestinationResolver} with
	 * the default configuration to resolve destination names.
	 * @param amazonSqs The {@link AmazonSQS} client, cannot be {@code null}.
	 * @param destinationResolver A destination resolver implementation to resolve queue
	 * names into queue urls. The destination resolver will be wrapped into a
	 * {@link org.springframework.messaging.core.CachingDestinationResolverProxy} to avoid
	 * duplicate queue url resolutions.
	 * @param messageConverter A {@link MessageConverter} that is going to be added to the
	 * composite converter.
	 */
	public QueueMessagingTemplate(AmazonSQSAsync amazonSqs,
			DestinationResolver<String> destinationResolver,
			MessageConverter messageConverter) {
		super(destinationResolver);
		this.amazonSqs = amazonSqs;
		initMessageConverter(messageConverter);
	}

	@Override
	protected QueueMessageChannel resolveMessageChannel(
			String physicalResourceIdentifier) {
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
	public <T> T receiveAndConvert(QueueMessageChannel destination, Class<T> targetClass)
			throws MessagingException {
		Message<?> message = destination.receive();
		if (message != null) {
			return (T) getMessageConverter().fromMessage(message, targetClass);
		}
		else {
			return null;
		}
	}

	@Override
	public Message<?> receive(String destinationName) throws MessagingException {
		return resolveMessageChannelByLogicalName(destinationName).receive();
	}

	@Override
	public <T> T receiveAndConvert(String destinationName, Class<T> targetClass)
			throws MessagingException {
		QueueMessageChannel channel = resolveMessageChannelByLogicalName(destinationName);
		return receiveAndConvert(channel, targetClass);
	}

}
