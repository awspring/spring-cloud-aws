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
package io.awspring.cloud.sqs.support.converter;

import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.HeaderMapper;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * {@link MessagingMessageConverter} implementation for converting SQS
 * {@link software.amazon.awssdk.services.sqs.model.Message} instances to Spring Messaging {@link Message} instances.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see SqsHeaderMapper
 * @see SqsMessageConversionContext
 */
public abstract class AbstractMessagingMessageConverter<S> implements ContextAwareMessagingMessageConverter<S> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMessagingMessageConverter.class);

	private static final MessageConverter DEFAULT_MESSAGE_CONVERTER = new MappingJackson2MessageConverter();

	private String typeHeader = SqsHeaders.SQS_MA_HEADER_PREFIX + SqsHeaders.SQS_DEFAULT_TYPE_HEADER;

	private MessageConverter payloadMessageConverter = DEFAULT_MESSAGE_CONVERTER;

	private HeaderMapper<S> headerMapper = getDefaultHeaderMapper();

	private Function<Message<?>, Class<?>> payloadTypeMapper = this::defaultHeaderTypeMapping;

	/**
	 * Set the payload type mapper to be used by this converter. {@link Message} payloads will be converted to the
	 * {@link Class} returned by this function. The {@link #defaultHeaderTypeMapping} uses the {@link #typeHeader}
	 * property to retrieve the payload class' FQCN. This method replaces the default type mapping for this converter
	 * instance.
	 * @param payloadTypeMapper the type mapping function.
	 */
	public void setPayloadTypeMapper(Function<Message<?>, Class<?>> payloadTypeMapper) {
		Assert.notNull(payloadTypeMapper, "payloadTypeMapper cannot be null");
		this.payloadTypeMapper = payloadTypeMapper;
	}

	/**
	 * Set the {@link MessageConverter} to be used for converting the {@link Message} instances payloads. The default is
	 * {@link #DEFAULT_MESSAGE_CONVERTER}.
	 * @param messageConverter the converter instances.
	 */
	public void setPayloadMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "messageConverter cannot be null");
		this.payloadMessageConverter = messageConverter;
	}

	/**
	 * Set the name of the header to be looked up in a {@link Message} instance by the
	 * {@link #defaultHeaderTypeMapping(Message)}.
	 * @param typeHeader the header name.
	 */
	public void setPayloadTypeHeader(String typeHeader) {
		Assert.notNull(typeHeader, "typeHeader cannot be null");
		this.typeHeader = SqsHeaders.SQS_MA_HEADER_PREFIX + typeHeader;
	}

	/**
	 * Set the {@link HeaderMapper} to used to convert headers for
	 * {@link software.amazon.awssdk.services.sqs.model.Message} instances.
	 * @param headerMapper the header mapper instance.
	 */
	public void setHeaderMapper(HeaderMapper<S> headerMapper) {
		Assert.notNull(headerMapper, "headerMapper cannot be null");
		this.headerMapper = headerMapper;
	}

	protected abstract HeaderMapper<S> getDefaultHeaderMapper();

	@Override
	public Message<?> toMessagingMessage(S message, @Nullable MessageConversionContext context) {
		MessageHeaders messageHeaders = createMessageHeaders(message, context);
		return MessageBuilder.createMessage(convertPayload(message, messageHeaders), messageHeaders);
	}

	private MessageHeaders createMessageHeaders(S message, MessageConversionContext context) {
		MessageHeaders messageHeaders = this.headerMapper.toHeaders(message);
		return context != null && this.headerMapper instanceof ContextAwareHeaderMapper
				? addContextHeaders(message, context, messageHeaders)
				: messageHeaders;
	}

	private MessageHeaders addContextHeaders(S message, MessageConversionContext context,
			MessageHeaders messageHeaders) {
		MessageHeaders contextHeaders = getContextHeaders(message, context);
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.copyHeaders(messageHeaders);
		accessor.copyHeaders(contextHeaders);
		return accessor.getMessageHeaders();
	}

	private MessageHeaders getContextHeaders(S message, MessageConversionContext context) {
		return ((ContextAwareHeaderMapper<S>) this.headerMapper).createContextHeaders(message, context);
	}

	private Object convertPayload(S message, MessageHeaders messageHeaders) {
		Message<?> messagingMessage = MessageBuilder.createMessage(getPayloadToConvert(message), messageHeaders);
		Class<?> targetType = this.payloadTypeMapper.apply(messagingMessage);
		return targetType != null ? this.payloadMessageConverter.fromMessage(messagingMessage, targetType)
				: getPayloadToConvert(message);
	}

	protected abstract Object getPayloadToConvert(S message);

	@Nullable
	private Class<?> defaultHeaderTypeMapping(Message<?> message) {
		String header = message.getHeaders().get(this.typeHeader, String.class);
		if (header == null) {
			return null;
		}
		try {
			return Class.forName(header);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("No class found with name " + header);
		}
	}

	@Override
	public MessageConversionContext createMessageConversionContext() {
		return new MessageConversionContext() {
		};
	}

	@Override
	public S fromMessagingMessage(Message<?> message) {
		// To be implemented for `SqsTemplate`
		throw new UnsupportedOperationException("fromMessagingMessage not implemented");
	}

}
