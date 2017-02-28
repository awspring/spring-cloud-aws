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

import com.amazonaws.services.sns.AmazonSNS;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.core.support.AbstractMessageChannelMessagingSendingTemplate;
import org.springframework.cloud.aws.messaging.support.destination.DynamicTopicDestinationResolver;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class NotificationMessagingTemplate extends AbstractMessageChannelMessagingSendingTemplate<TopicMessageChannel> {

	private final AmazonSNS amazonSns;

	private static final boolean JACKSON_2_PRESENT = ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", NotificationMessagingTemplate.class.getClassLoader());

	public NotificationMessagingTemplate(AmazonSNS amazonSns) {
		this(amazonSns, (ResourceIdResolver) null, null);
	}

	public NotificationMessagingTemplate(AmazonSNS amazonSns, ResourceIdResolver resourceIdResolver) {
		this(amazonSns, resourceIdResolver, null);
	}

	public NotificationMessagingTemplate(AmazonSNS amazonSns, ResourceIdResolver resourceIdResolver, MessageConverter messageConverter) {
		super(new DynamicTopicDestinationResolver(amazonSns, resourceIdResolver));
		this.amazonSns = amazonSns;
		initMessageConverter(messageConverter);
	}

	public NotificationMessagingTemplate(AmazonSNS amazonSns, DestinationResolver<String> destinationResolver, MessageConverter messageConverter) {
		super(destinationResolver);
		this.amazonSns = amazonSns;
		initMessageConverter(messageConverter);
	}

	private void initMessageConverter(MessageConverter messageConverter) {

		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		stringMessageConverter.setSerializedPayloadClass(String.class);

		List<MessageConverter> messageConverters = new ArrayList<>();
		messageConverters.add(stringMessageConverter);

		if (messageConverter != null) {
			messageConverters.add(messageConverter);
		} else if (JACKSON_2_PRESENT) {
			MappingJackson2MessageConverter mappingJackson2MessageConverter = new MappingJackson2MessageConverter();
			mappingJackson2MessageConverter.setSerializedPayloadClass(String.class);
			messageConverters.add(mappingJackson2MessageConverter);
		}

		setMessageConverter(new CompositeMessageConverter(messageConverters));
	}

	@Override
	protected TopicMessageChannel resolveMessageChannel(String physicalResourceIdentifier) {
		return new TopicMessageChannel(this.amazonSns, physicalResourceIdentifier);
	}

	/**
	 * Convenience method that sends a notification with the given {@literal message} and {@literal subject} to the {@literal destination}.
	 * The {@literal subject} is sent as header as defined in the <a href="http://docs.aws.amazon.com/sns/latest/dg/json-formats.html">SNS message JSON formats</a>.
	 *
	 * @param destinationName
	 * 		The logical name of the destination
	 * @param message
	 * 		The message to send
	 * @param subject
	 * 		The subject to send
	 */
	public void sendNotification(String destinationName, Object message, String subject) {
		this.convertAndSend(destinationName, message, Collections.<String, Object>singletonMap(TopicMessageChannel.NOTIFICATION_SUBJECT_HEADER, subject));
	}

	/**
	 * Convenience method that sends a notification with the given {@literal message} and {@literal subject} to the {@literal destination}.
	 * The {@literal subject} is sent as header as defined in the <a href="http://docs.aws.amazon.com/sns/latest/dg/json-formats.html">SNS message JSON formats</a>.
	 * The configured default destination will be used.
	 *
	 * @param message
	 * 		The message to send
	 * @param subject
	 * 		The subject to send
	 */
	public void sendNotification(Object message, String subject) {
		this.convertAndSend(getRequiredDefaultDestination(), message, Collections.<String, Object>singletonMap(TopicMessageChannel.NOTIFICATION_SUBJECT_HEADER, subject));
	}
}
