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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.listener.SqsHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * Base {@link MessagingMessageConverter} implementation.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see SqsHeaderMapper
 * @see SqsMessageConversionContext
 */
public abstract class AbstractMessagingMessageConverter<S> implements ContextAwareMessagingMessageConverter<S> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMessagingMessageConverter.class);

	private String typeHeader = SqsHeaders.SQS_DEFAULT_TYPE_HEADER;

	private MessageConverter payloadMessageConverter;

	private HeaderMapper<S> headerMapper;

	private Function<Message<?>, Class<?>> payloadTypeMapper;

	private Function<Message<?>, String> payloadTypeHeaderFunction = message -> message.getPayload().getClass().getName();

	protected AbstractMessagingMessageConverter() {
		this.payloadMessageConverter = createDefaultCompositeMessageConverter();
		this.headerMapper = createDefaultHeaderMapper();
		this.payloadTypeMapper = this::defaultHeaderTypeMapping;
	}

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
	 * Set the {@link MessageConverter} to be used for converting the {@link Message} instances payloads.
	 * @param messageConverter the converter instance.
	 */
	public void setPayloadMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "messageConverter cannot be null");
		this.payloadMessageConverter = messageConverter;
	}

	/**
	 * Set the {@link ObjectMapper} instance to be used for converting the {@link Message} instances payloads.
	 * @param objectMapper the object mapper instance.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "messageConverter cannot be null");
		Assert.isInstanceOf(MappingJackson2MessageConverter.class, this.payloadMessageConverter,
			"ObjectMapper can only be set in MappingJackson2MessageConverter instances");
		((MappingJackson2MessageConverter) this.payloadMessageConverter).setObjectMapper(objectMapper);
	}

	/**
	 * Get the {@link MessageConverter} to be used for converting the {@link Message} instances payloads.
	 * @return the instance.
	 */
	public MessageConverter getPayloadMessageConverter() {
		return this.payloadMessageConverter;
	}

	/**
	 * Set the name of the header to be looked up in a {@link Message} instance by the
	 * {@link #defaultHeaderTypeMapping(Message)}.
	 * @param typeHeader the header name.
	 */
	public void setPayloadTypeHeader(String typeHeader) {
		Assert.notNull(typeHeader, "typeHeader cannot be null");
		this.typeHeader = typeHeader;
	}

	/**
	 * Set the function to create the payload type header value from.
	 * @param payloadTypeHeaderFunction the function.
	 */
	public void setPayloadTypeHeaderValueFunction(Function<Message<?>, String> payloadTypeHeaderFunction) {
		Assert.notNull(payloadTypeHeaderFunction, "payloadTypeHeaderFunction cannot be null");
		this.payloadTypeHeaderFunction = payloadTypeHeaderFunction;
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

	protected abstract HeaderMapper<S> createDefaultHeaderMapper();

	@Override
	public Message<?> toMessagingMessage(S message, @Nullable MessageConversionContext context) {
		MessageHeaders messageHeaders = createMessageHeaders(message, context);
		return MessageBuilder.createMessage(convertPayload(message, messageHeaders, context), messageHeaders);
	}

	private MessageHeaders createMessageHeaders(S message, @Nullable MessageConversionContext context) {
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

	private Object convertPayload(S message, MessageHeaders messageHeaders, @Nullable MessageConversionContext context) {
		Message<?> messagingMessage = MessageBuilder.createMessage(getPayloadToDeserialize(message), messageHeaders);
		Class<?> targetType = getTargetType(messagingMessage, context);
		return targetType != null ? Objects.requireNonNull(this.payloadMessageConverter.fromMessage(messagingMessage, targetType),
			"payloadMessageConverter returned null payload")
				: messagingMessage.getPayload();
	}

	@Nullable
	private Class<?> getTargetType(Message<?> messagingMessage, @Nullable MessageConversionContext context) {
		return context != null && context.getPayloadClass() != null
			? context.getPayloadClass() : this.payloadTypeMapper.apply(messagingMessage);
	}

	protected abstract Object getPayloadToDeserialize(S message);

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
		return null;
	}

	@Override
	public S fromMessagingMessage(Message<?> message) {
		MessageHeaders headers = getMessageHeaders(message);
		S messageWithHeaders = this.headerMapper.fromHeaders(headers);
		Object payload = Objects.requireNonNull(this.payloadMessageConverter.toMessage(message.getPayload(), message.getHeaders()),
			() -> "payloadMessageConverter returned null message for message " + message).getPayload();
		return doConvertMessage(messageWithHeaders, payload);
	}

	private MessageHeaders getMessageHeaders(Message<?> message) {
		String typeHeaderName = this.payloadTypeHeaderFunction.apply(message);
		return typeHeaderName != null ? addTypeInfo(message, typeHeaderName) : message.getHeaders();
	}

	private MessageHeaders addTypeInfo(Message<?> message, String typeHeaderName) {
		MessageHeaders headers;
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.copyHeaders(message.getHeaders());
		accessor.setHeader(this.typeHeader, typeHeaderName);
		headers = accessor.getMessageHeaders();
		return headers;
	}

	protected abstract S doConvertMessage(S messageWithHeaders, Object payload);

	private CompositeMessageConverter createDefaultCompositeMessageConverter() {
		List<MessageConverter> messageConverters = new ArrayList<>();
		messageConverters.add(createStringMessageConverter());
		messageConverters.add(createDefaultMappingJackson2MessageConverter());
		return new CompositeMessageConverter(messageConverters);
	}

	private StringMessageConverter createStringMessageConverter() {
		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		stringMessageConverter.setSerializedPayloadClass(String.class);
		return stringMessageConverter;
	}

	private MappingJackson2MessageConverter createDefaultMappingJackson2MessageConverter() {
		MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
		messageConverter.setSerializedPayloadClass(String.class);
		messageConverter.setStrictContentTypeMatch(false);
		return messageConverter;
	}

}
