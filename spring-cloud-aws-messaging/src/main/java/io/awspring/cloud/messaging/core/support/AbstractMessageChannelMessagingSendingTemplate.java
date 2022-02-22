/*
 * Copyright 2013-2020 the original author or authors.
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

package io.awspring.cloud.messaging.core.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.CachingDestinationResolverProxy;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.core.MessagePostProcessor;

/**
 * @param <D> message channel type
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public abstract class AbstractMessageChannelMessagingSendingTemplate<D extends MessageChannel>
		extends AbstractMessageSendingTemplate<D> implements DestinationResolvingMessageSendingOperations<D> {

	private final DestinationResolver<String> destinationResolver;

	protected AbstractMessageChannelMessagingSendingTemplate(DestinationResolver<String> destinationResolver) {
		this.destinationResolver = new CachingDestinationResolverProxy<>(destinationResolver);
	}

	public void setDefaultDestinationName(String defaultDestination) {
		super.setDefaultDestination(resolveMessageChannelByLogicalName(defaultDestination));
	}

	@Override
	protected void doSend(D destination, Message<?> message) {
		destination.send(message);
	}

	@Override
	public void send(String destinationName, Message<?> message) throws MessagingException {
		D channel = resolveMessageChannelByLogicalName(destinationName);
		doSend(channel, message);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload) throws MessagingException {
		D channel = resolveMessageChannelByLogicalName(destinationName);
		convertAndSend(channel, payload);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers)
			throws MessagingException {
		D channel = resolveMessageChannelByLogicalName(destinationName);
		convertAndSend(channel, payload, headers);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, MessagePostProcessor postProcessor)
			throws MessagingException {
		D channel = resolveMessageChannelByLogicalName(destinationName);
		convertAndSend(channel, payload, postProcessor);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers,
			MessagePostProcessor postProcessor) throws MessagingException {
		D channel = resolveMessageChannelByLogicalName(destinationName);
		convertAndSend(channel, payload, headers, postProcessor);
	}

	protected D resolveMessageChannelByLogicalName(String destination) {
		String physicalResourceId = this.destinationResolver.resolveDestination(destination);
		return resolveMessageChannel(physicalResourceId);
	}

	protected void initMessageConverter(MessageConverter messageConverter) {
		this.initMessageConverter(messageConverter, null);
	}

	protected void initMessageConverter(MessageConverter messageConverter, ObjectMapper objectMapper) {
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

		setMessageConverter(new CompositeMessageConverter(messageConverters));
	}

	protected abstract D resolveMessageChannel(String physicalResourceIdentifier);

}
