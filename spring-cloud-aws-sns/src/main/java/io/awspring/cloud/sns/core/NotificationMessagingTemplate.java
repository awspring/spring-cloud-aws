/*
 * Copyright 2013-2022 the original author or authors.
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

package io.awspring.cloud.sns.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import software.amazon.awssdk.services.sns.SnsClient;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.core.MessagePostProcessor;

import static io.awspring.cloud.sns.core.MessageHeaderCodes.NOTIFICATION_SUBJECT_HEADER;

/**
 * @author Alain Sahli
 * @author Matej Nedic
 * @since 1.0
 */
public class NotificationMessagingTemplate extends AbstractMessageSendingTemplate<TopicMessageChannel>
		implements DestinationResolvingMessageSendingOperations<TopicMessageChannel> {

	private final SnsClient snsClient;

	private final DestinationResolver<String> destinationResolver;

	public NotificationMessagingTemplate(SnsClient snsClient) {
		this(snsClient, null);
	}

	public NotificationMessagingTemplate(SnsClient snsClient, MessageConverter messageConverter) {
		this.destinationResolver = new DynamicTopicDestinationResolver(snsClient);
		this.snsClient = snsClient;
		initMessageConverter(messageConverter);
	}

	public NotificationMessagingTemplate(SnsClient snsClient, DestinationResolver<String> destinationResolver,
			MessageConverter messageConverter) {
		this.destinationResolver = destinationResolver;
		this.snsClient = snsClient;
		initMessageConverter(messageConverter);
	}

	public void setDefaultDestinationName(String defaultDestination) {
		super.setDefaultDestination(resolveMessageChannelByLogicalName(defaultDestination));
	}

	@Override
	protected void doSend(TopicMessageChannel destination, Message<?> message) {
		destination.send(message);
	}

	@Override
	public void send(String destinationName, Message<?> message) throws MessagingException {
		TopicMessageChannel channel = resolveMessageChannelByLogicalName(destinationName);
		doSend(channel, message);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload) throws MessagingException {
		TopicMessageChannel channel = resolveMessageChannelByLogicalName(destinationName);
		convertAndSend(channel, payload);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers)
			throws MessagingException {
		TopicMessageChannel channel = resolveMessageChannelByLogicalName(destinationName);
		convertAndSend(channel, payload, headers);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, MessagePostProcessor postProcessor)
			throws MessagingException {
		TopicMessageChannel channel = resolveMessageChannelByLogicalName(destinationName);
		convertAndSend(channel, payload, postProcessor);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers,
			MessagePostProcessor postProcessor) throws MessagingException {
		TopicMessageChannel channel = resolveMessageChannelByLogicalName(destinationName);
		convertAndSend(channel, payload, headers, postProcessor);
	}

	protected TopicMessageChannel resolveMessageChannelByLogicalName(String destination) {
		String physicalResourceId = this.destinationResolver.resolveDestination(destination);
		return resolveMessageChannel(physicalResourceId);
	}

	protected void initMessageConverter(MessageConverter messageConverter) {
		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		stringMessageConverter.setSerializedPayloadClass(String.class);

		List<MessageConverter> messageConverters = new ArrayList<>();
		messageConverters.add(stringMessageConverter);

		if (messageConverter != null) {
			messageConverters.add(messageConverter);
		}
		setMessageConverter(new CompositeMessageConverter(messageConverters));
	}

	protected TopicMessageChannel resolveMessageChannel(String physicalResourceIdentifier) {
		return new TopicMessageChannel(this.snsClient, physicalResourceIdentifier);
	}

	/**
	 * Convenience method that sends a notification with the given {@literal message} and
	 * {@literal subject} to the {@literal destination}. The {@literal subject} is sent as
	 * header as defined in the
	 * <a href="https://docs.aws.amazon.com/sns/latest/dg/json-formats.html">SNS message
	 * JSON formats</a>.
	 * @param destinationName The logical name of the destination
	 * @param message The message to send
	 * @param subject The subject to send
	 */
	public void sendNotification(String destinationName, Object message, String subject) {
		this.convertAndSend(destinationName, message, Collections.singletonMap(NOTIFICATION_SUBJECT_HEADER, subject));
	}

	/**
	 * Convenience method that sends a notification with the given {@literal message} and
	 * {@literal subject} to the {@literal destination}. The {@literal subject} is sent as
	 * header as defined in the
	 * <a href="https://docs.aws.amazon.com/sns/latest/dg/json-formats.html">SNS message
	 * JSON formats</a>. The configured default destination will be used.
	 * @param message The message to send
	 * @param subject The subject to send
	 */
	public void sendNotification(Object message, String subject) {
		this.convertAndSend(getRequiredDefaultDestination(), message,
				Collections.singletonMap(NOTIFICATION_SUBJECT_HEADER, subject));
	}

}
