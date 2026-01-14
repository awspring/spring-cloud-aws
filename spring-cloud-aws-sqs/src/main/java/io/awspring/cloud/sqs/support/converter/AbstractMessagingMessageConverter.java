/*
 * Copyright 2013-2024 the original author or authors.
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

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.util.Objects;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Base {@link MessagingMessageConverter} implementation.
 *
 * @author Tomaz Fernandes
 * @author Dongha Kim
 *
 * @since 3.0
 * @see SqsHeaderMapper
 * @see SqsMessageConversionContext
 */
public abstract class AbstractMessagingMessageConverter<S> implements ContextAwareMessagingMessageConverter<S> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMessagingMessageConverter.class);

	private String typeHeader = SqsHeaders.SQS_DEFAULT_TYPE_HEADER;

	private MessageConverter payloadMessageConverter;

	private HeaderMapper<S> headerMapper;

	private static final Function<Message<?>, Class<?>> DEFAULT_PAYLOAD_TYPE_MAPPER = msg -> headerTypeMapping(msg,
			SqsHeaders.SQS_DEFAULT_TYPE_HEADER);

	private Function<Message<?>, Class<?>> payloadTypeMapper = DEFAULT_PAYLOAD_TYPE_MAPPER;

	private Function<Message<?>, String> payloadTypeHeaderFunction = message -> message.getPayload().getClass()
			.getName();

	protected AbstractMessagingMessageConverter(MessageConverter messageConverter) {
		this.headerMapper = createDefaultHeaderMapper();
		this.payloadMessageConverter = messageConverter;
	}

	/**
	 * Check if this converter is using the default payload type mapper (header-based).
	 * @return true if using the default mapper, false if a custom mapper has been configured
	 */
	public boolean isUsingDefaultPayloadTypeMapper() {
		return this.payloadTypeMapper == DEFAULT_PAYLOAD_TYPE_MAPPER;
	}

	/**
	 * Set the payload type mapper to be used by this converter. {@link Message} payloads will be converted to the
	 * {@link Class} returned by this function. The default header-based type mapping uses the {@link #typeHeader}
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
	 * Get the {@link MessageConverter} to be used for converting the {@link Message} instances payloads.
	 * @return the instance.
	 */
	public MessageConverter getPayloadMessageConverter() {
		return this.payloadMessageConverter;
	}

	/**
	 * Set the name of the header to be looked up in a {@link Message} instance for payload type mapping. When used,
	 * type header mapping takes precedence over automatic type inferrence.
	 * @param typeHeader the header name.
	 */
	public void setPayloadTypeHeader(String typeHeader) {
		Assert.notNull(typeHeader, "typeHeader cannot be null");
		this.typeHeader = typeHeader;
		this.payloadTypeMapper = msg -> headerTypeMapping(msg, typeHeader);
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

	/**
	 * Configure the converter to not include payload type information in the
	 * {@link software.amazon.awssdk.services.sqs.model.Message} headers.
	 */
	public void doNotSendPayloadTypeHeader() {
		this.payloadTypeHeaderFunction = message -> null;
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
				? MessageHeaderUtils.addHeadersIfAbsent(messageHeaders, getContextHeaders(message, context))
				: messageHeaders;
	}

	private MessageHeaders getContextHeaders(S message, MessageConversionContext context) {
		return ((ContextAwareHeaderMapper<S>) this.headerMapper).createContextHeaders(message, context);
	}

	private Object convertPayload(S message, MessageHeaders messageHeaders,
			@Nullable MessageConversionContext context) {
		Message<?> messagingMessage = MessageBuilder.createMessage(getPayloadToDeserialize(message), messageHeaders);
		Class<?> targetType = getTargetType(messagingMessage, context);
		return targetType != null
				? Objects.requireNonNull(this.payloadMessageConverter.fromMessage(messagingMessage, targetType),
						"payloadMessageConverter returned null payload")
				: messagingMessage.getPayload();
	}

	@Nullable
	private Class<?> getTargetType(Message<?> messagingMessage, @Nullable MessageConversionContext context) {
		Class<?> classFromTypeMapper = this.payloadTypeMapper.apply(messagingMessage);
		return classFromTypeMapper == null && context != null && context.getPayloadClass() != null
				? context.getPayloadClass()
				: classFromTypeMapper;
	}

	protected abstract Object getPayloadToDeserialize(S message);

	@Nullable
	private static Class<?> headerTypeMapping(Message<?> message, String typeHeader) {
		String header = message.getHeaders().get(typeHeader, String.class);
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
	public S fromMessagingMessage(Message<?> message, @Nullable MessageConversionContext context) {
		// We must make sure the message id stays consistent throughout this process
		MessageHeaders headers = getMessageHeaders(message);
		Message<?> convertedMessage = convertPayload(message, message.getPayload());
		MessageHeaders completeHeaders = MessageHeaderUtils.addHeadersIfAbsent(headers, convertedMessage.getHeaders());
		S messageWithHeaders = this.headerMapper.fromHeaders(completeHeaders);
		return doConvertMessage(messageWithHeaders, convertedMessage.getPayload());
	}

	private Message<?> convertPayload(Message<?> message, Object payload) {
		return Objects.requireNonNull(this.payloadMessageConverter.toMessage(payload, message.getHeaders()),
				() -> "payloadMessageConverter returned null message for message " + message);
	}

	private MessageHeaders getMessageHeaders(Message<?> message) {
		String typeHeaderName = this.payloadTypeHeaderFunction.apply(message);
		return typeHeaderName != null
				? MessageHeaderUtils.addHeaderIfAbsent(message.getHeaders(), this.typeHeader, typeHeaderName)
				: message.getHeaders();
	}

	protected abstract S doConvertMessage(S messageWithHeaders, Object payload);

	private static SimpleClassMatchingMessageConverter createClassMatchingMessageConverter() {
		SimpleClassMatchingMessageConverter matchingMessageConverter = new SimpleClassMatchingMessageConverter();
		matchingMessageConverter.setSerializedPayloadClass(String.class);
		return matchingMessageConverter;
	}

	private static StringMessageConverter createStringMessageConverter() {
		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		stringMessageConverter.setSerializedPayloadClass(String.class);
		return stringMessageConverter;
	}

}
