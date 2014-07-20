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

import com.amazonaws.services.sns.AmazonSNS;
import org.elasticspring.core.env.ResourceIdResolver;
import org.elasticspring.messaging.core.support.AbstractMessageChannelMessagingSendingTemplate;
import org.elasticspring.messaging.support.destination.DynamicTopicDestinationResolver;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Map;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class NotificationMessagingTemplate extends AbstractMessageChannelMessagingSendingTemplate<TopicMessageChannel> {

	private final AmazonSNS amazonSns;

	public NotificationMessagingTemplate(AmazonSNS amazonSns, ResourceIdResolver resourceIdResolver) {
		super(new DynamicTopicDestinationResolver(amazonSns, resourceIdResolver));
		this.amazonSns = amazonSns;
	}

	public NotificationMessagingTemplate(AmazonSNS amazonSns) {
		this(amazonSns, null);
	}

	@Override
	protected TopicMessageChannel resolveMessageChannel(String physicalResourceIdentifier) {
		return new TopicMessageChannel(this.amazonSns, physicalResourceIdentifier);
	}

	/**
	 * <b>IMPORTANT</b>: the underlying message channel {@link org.elasticspring.messaging.core.TopicMessageChannel} only
	 * supports {@code String} as payload. Therefore only {@code String} payloads are accepted.
	 *
	 * @see org.springframework.messaging.core.MessageSendingOperations#convertAndSend(Object, Object, java.util.Map)
	 */
	@Override
	public <T> void convertAndSend(String destinationName, T payload) throws MessagingException {
		Assert.isInstanceOf(String.class, payload, "Payload must be of type string");
		super.convertAndSend(destinationName, payload);
	}

	/**
	 * <b>IMPORTANT</b>: the underlying message channel {@link org.elasticspring.messaging.core.TopicMessageChannel} only
	 * supports {@code String} as payload. Therefore only {@code String} payloads are accepted.
	 *
	 * @see org.springframework.messaging.core.MessageSendingOperations#convertAndSend(Object, Object, java.util.Map)
	 */
	@Override
	public <T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers) throws MessagingException {
		Assert.isInstanceOf(String.class, payload, "Payload must be of type string");
		super.convertAndSend(destinationName, payload, headers);
	}

	/**
	 * <b>IMPORTANT</b>: the underlying message channel {@link org.elasticspring.messaging.core.TopicMessageChannel} only
	 * supports {@code String} as payload. Therefore only {@code String} payloads are accepted.
	 *
	 * @see org.springframework.messaging.core.MessageSendingOperations#convertAndSend(Object, Object, java.util.Map)
	 */
	@Override
	public <T> void convertAndSend(String destinationName, T payload, MessagePostProcessor postProcessor) throws MessagingException {
		Assert.isInstanceOf(String.class, payload, "Payload must be of type string");
		super.convertAndSend(destinationName, payload, postProcessor);
	}

	/**
	 * <b>IMPORTANT</b>: the underlying message channel {@link org.elasticspring.messaging.core.TopicMessageChannel} only
	 * supports {@code String} as payload. Therefore only {@code String} payloads are accepted.
	 *
	 * @see org.springframework.messaging.core.MessageSendingOperations#convertAndSend(Object, Object, java.util.Map)
	 */
	@Override
	public <T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers, MessagePostProcessor postProcessor) throws MessagingException {
		Assert.isInstanceOf(String.class, payload, "Payload must be of type string");
		super.convertAndSend(destinationName, payload, headers, postProcessor);
	}

	public void sendNotification(String destinationName, String payload, String subject) {
		this.convertAndSend(destinationName, payload, Collections.<String, Object>singletonMap(TopicMessageChannel.NOTIFICATION_SUBJECT_HEADER, subject));
	}

	public void sendNotification(String payload, String subject) {
		this.convertAndSend(getRequiredDefaultDestination(), payload, Collections.<String, Object>singletonMap(TopicMessageChannel.NOTIFICATION_SUBJECT_HEADER, subject));
	}
}
