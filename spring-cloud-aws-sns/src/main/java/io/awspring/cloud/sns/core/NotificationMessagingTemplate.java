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

import static io.awspring.cloud.sns.core.MessageHeaderCodes.NOTIFICATION_SUBJECT_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Helper class that simplifies synchronous sending of notifications to SNS. The only mandatory fields are
 * {@link SnsClient} and AutoCreate boolean.
 *
 * @author Alain Sahli
 * @author Matej Nedic
 * @since 1.0
 */
public class NotificationMessagingTemplate extends AbstractMessageSendingTemplate<TopicMessageChannel>
		implements DestinationResolvingMessageSendingOperations<TopicMessageChannel> {

	private final SnsClient snsClient;

	private final AutoTopicCreator autoTopicCreator;

	public NotificationMessagingTemplate(SnsClient snsClient, boolean autoCreate, @Nullable ObjectMapper objectMapper) {
		this(snsClient, null, autoCreate, objectMapper);
	}

	public NotificationMessagingTemplate(SnsClient snsClient, MessageConverter messageConverter, boolean autoCreate,
			@Nullable ObjectMapper objectMapper) {
		Assert.notNull(snsClient, "SnsClient must not be null");
		Assert.notNull(snsClient, "AutoCreate must not be null");
		this.autoTopicCreator = new DefaultAutoTopicCreator(snsClient, autoCreate);
		this.snsClient = snsClient;
		setMessageConverter(initMessageConverter(messageConverter, objectMapper));
	}

	public NotificationMessagingTemplate(SnsClient snsClient, AutoTopicCreator autoTopicCreator,
			@Nullable MessageConverter messageConverter, @Nullable ObjectMapper objectMapper) {
		Assert.notNull(snsClient, "SnsClient must not be null");
		Assert.notNull(snsClient, "AutoCreate must not be null");
		this.autoTopicCreator = autoTopicCreator;
		this.snsClient = snsClient;
		setMessageConverter(initMessageConverter(messageConverter, objectMapper));
	}

	public void setDefaultDestinationName(String defaultDestination) {
		super.setDefaultDestination(resolveMessageChannelByLogicalName(defaultDestination));
	}

	@Override
	protected void doSend(TopicMessageChannel destination, Message<?> message) {
		destination.send(message);
	}

	@Override
	public void send(String destination, Message<?> message) throws MessagingException {
		TopicMessageChannel channel = resolveMessageChannelByLogicalName(destination);
		doSend(channel, message);
	}

	@Override
	public <T> void convertAndSend(String destination, T payload) throws MessagingException {
		TopicMessageChannel channel = resolveMessageChannelByLogicalName(destination);
		convertAndSend(channel, payload);
	}

	@Override
	public <T> void convertAndSend(String destination, T payload, @Nullable Map<String, Object> headers)
			throws MessagingException {
		TopicMessageChannel channel = resolveMessageChannelByLogicalName(destination);
		convertAndSend(channel, payload, headers);
	}

	@Override
	public <T> void convertAndSend(String destination, T payload, @Nullable MessagePostProcessor postProcessor)
			throws MessagingException {
		TopicMessageChannel channel = resolveMessageChannelByLogicalName(destination);
		convertAndSend(channel, payload, postProcessor);
	}

	@Override
	public <T> void convertAndSend(String destination, T payload, @Nullable Map<String, Object> headers,
			MessagePostProcessor postProcessor) throws MessagingException {
		TopicMessageChannel channel = resolveMessageChannelByLogicalName(destination);
		convertAndSend(channel, payload, headers, postProcessor);
	}

	protected TopicMessageChannel resolveMessageChannelByLogicalName(String destination) {
		String physicalResourceId = this.autoTopicCreator.createTopicBasedOnName(destination);
		return resolveMessageChannel(physicalResourceId);
	}

	protected CompositeMessageConverter initMessageConverter(MessageConverter messageConverter,
			ObjectMapper objectMapper) {
		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		stringMessageConverter.setSerializedPayloadClass(String.class);

		List<MessageConverter> messageConverters = new ArrayList<>();
		messageConverters.add(stringMessageConverter);

		if (messageConverter != null) {
			messageConverters.add(messageConverter);
		}
		else {
			MappingJackson2MessageConverter mappingJackson2MessageConverter = new MappingJackson2MessageConverter();
			mappingJackson2MessageConverter.setSerializedPayloadClass(String.class);
			if (objectMapper != null) {
				mappingJackson2MessageConverter.setObjectMapper(objectMapper);
			}
			messageConverters.add(mappingJackson2MessageConverter);
		}

		return new CompositeMessageConverter(messageConverters);
	}

	protected TopicMessageChannel resolveMessageChannel(String physicalResourceIdentifier) {
		return new TopicMessageChannel(this.snsClient, physicalResourceIdentifier);
	}

	/**
	 * Convenience method that sends a notification with the given {@literal message} and {@literal subject} to the
	 * {@literal destination}. The {@literal subject} is sent as header as defined in the
	 * <a href="https://docs.aws.amazon.com/sns/latest/dg/json-formats.html">SNS message JSON formats</a>.
	 * @param destinationName The logical name of the destination
	 * @param message The message to send
	 * @param subject The subject to send
	 */
	public void sendNotification(String destinationName, Object message, @Nullable String subject) {
		this.convertAndSend(destinationName, message, Collections.singletonMap(NOTIFICATION_SUBJECT_HEADER, subject));
	}

	/**
	 * Convenience method that sends a notification with the given {@literal message} and {@literal subject} to the
	 * {@literal destination}. The {@literal subject} is sent as header as defined in the
	 * <a href="https://docs.aws.amazon.com/sns/latest/dg/json-formats.html">SNS message JSON formats</a>. The
	 * configured default destination will be used.
	 * @param message The message to send
	 * @param subject The subject to send
	 */
	public void sendNotification(Object message, @Nullable String subject) {
		this.convertAndSend(getRequiredDefaultDestination(), message,
				Collections.singletonMap(NOTIFICATION_SUBJECT_HEADER, subject));
	}

}
