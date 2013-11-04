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

package org.elasticspring.messaging.core.sns;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import org.elasticspring.messaging.core.NotificationOperations;
import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.elasticspring.messaging.support.destination.DestinationResolver;
import org.elasticspring.messaging.support.destination.DynamicTopicDestinationResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.converter.MessageConverter;
import org.springframework.messaging.support.converter.SimpleMessageConverter;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link NotificationOperations} interface to send notification to the Amazon Notification
 * Service. This implementation uses the {@link MessageConverter} to convert the messages into the particular platform
 * specific format and a {@link DestinationResolver} to retrieve the physical destination name based on a logical
 * destination name.
 * <p/>
 * This implementation used the {@link AmazonSNS} implementation of the Amazon Webservices SDK to make the service API
 * calls.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class SimpleNotificationServiceTemplate implements NotificationOperations {

	/**
	 * {@link AmazonSNS} client used by the instance
	 */
	private final AmazonSNS amazonSNS;

	/**
	 * {@link MessageConverter} used by the instance. By default a {@link SimpleMessageConverter}
	 */
	private MessageConverter messageConverter = new SimpleMessageConverter();

	/**
	 * {@link DestinationResolver} used by the instance. By default a {@link DynamicTopicDestinationResolver}
	 */
	private DestinationResolver destinationResolver;

	/**
	 * The default destination name for operations that don't provide a destination name
	 */
	private String defaultDestinationName;

	/**
	 * Constructs an instance of this class with the mandatory {@link AmazonSNS} instance. This constructor will also
	 * create {@link DynamicTopicDestinationResolver} which is wrapped with a {@link CachingDestinationResolver} to
	 * improve
	 * the performance of the destination resolving process.
	 *
	 * @param amazonSNS
	 * 		- the AmazonSNS client used by the instance, must not be null.
	 */
	public SimpleNotificationServiceTemplate(AmazonSNS amazonSNS) {
		Assert.notNull(amazonSNS, "amazonSNS must not be null");
		this.amazonSNS = amazonSNS;
		this.destinationResolver = new CachingDestinationResolver(new DynamicTopicDestinationResolver(amazonSNS));
	}

	/**
	 * Configures the default destination name for this instance. This name will be used by operations that do not receive
	 * a destination name for the client (e.g. {@link #convertAndSend(Object)}. The value which might be a logical or
	 * physical name (topic arn) will be resolved during the method called to the physical name.
	 *
	 * @param defaultDestinationName
	 * 		- the default destination name, either a topic arn or logical topic name.
	 */
	public void setDefaultDestinationName(String defaultDestinationName) {
		this.defaultDestinationName = defaultDestinationName;
	}

	/**
	 * Configures a {@link DestinationResolver} overriding the default {@link DynamicTopicDestinationResolver} for this
	 * instance. The destination resolver could add additional logic to retrieve physical destination names.
	 *
	 * @param destinationResolver
	 * 		- the destination resolver. Must not be null
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Configures the {@link MessageConverter} used by this instance to convert the payload into notification messages.
	 * Overrides the default {@link SimpleMessageConverter} that convert String objects into notification messages.
	 *
	 * @param messageConverter
	 * 		- the message converter to be used. Must not be null
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Converts and sends the payload.
	 *
	 * @param payload
	 * 		- the payload that will be converted and sent (e.g. a String in combination with a {@link SimpleMessageConverter}
	 * @throws java.lang.IllegalStateException
	 * 		if the default destination name is not set
	 */
	@Override
	public void convertAndSend(Object payload) {
		Assert.state(this.defaultDestinationName != null, "No default destination name configured for this template.");
		this.convertAndSend(this.defaultDestinationName, payload);
	}

	/**
	 * Convert and send the payload with the additional subject.
	 *
	 * @param payload
	 * 		- the payload that will be converted and sent (e.g. a String in combination with a {@link SimpleMessageConverter}
	 * @throws java.lang.IllegalStateException
	 * 		if the default destination name is not set
	 */
	@Override
	public void convertAndSendWithSubject(Object payload, String subject) {
		Assert.state(this.defaultDestinationName != null, "No default destination name configured for this template.");
		this.convertAndSendWithSubject(this.defaultDestinationName, payload, subject);
	}

	/**
	 * Convert and send the payload with the additional subject.
	 *
	 * @param destinationName
	 * 		- the destination name, must not be null
	 * @param payload
	 * 		- the payload that will be converted and sent (e.g. a String in combination with a {@link SimpleMessageConverter}
	 * @throws java.lang.IllegalStateException
	 * 		if the default destination name is not set
	 */
	@Override
	public void convertAndSend(String destinationName, Object payload) {
		this.convertAndSendWithSubject(destinationName, payload, null);
	}

	/**
	 * Convert and send the payload with the additional subject.
	 *
	 * @param destinationName
	 * 		- the destination name, must not be null
	 * @param payload
	 * 		- the payload that will be converted and sent (e.g. a String in combination with a {@link SimpleMessageConverter}
	 * @param subject
	 * 		- the optional subject
	 * @throws java.lang.IllegalStateException
	 * 		if the default destination name is not set
	 */
	@Override
	public void convertAndSendWithSubject(String destinationName, Object payload, String subject) {
		Assert.notNull("destinationName must not be null");
		String topicArn = this.destinationResolver.resolveDestinationName(destinationName);
		Message<?> message = this.messageConverter.toMessage(payload, null);

		if (subject == null) {
			this.amazonSNS.publish(new PublishRequest(topicArn, message.getPayload().toString()));
		} else {
			this.amazonSNS.publish(new PublishRequest(topicArn, message.getPayload().toString(), subject));
		}
	}
}
