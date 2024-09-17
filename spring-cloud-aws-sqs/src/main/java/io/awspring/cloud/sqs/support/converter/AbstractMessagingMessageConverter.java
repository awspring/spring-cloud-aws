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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
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

	private Function<Message<?>, Class<?>> payloadTypeMapper;

	private Function<Message<?>, String> payloadTypeHeaderFunction = message -> message.getPayload().getClass()
			.getName();

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
		MappingJackson2MessageConverter converter = getMappingJackson2MessageConverter().orElseThrow(
				() -> new IllegalStateException("%s can only be set in %s instances, or %s containing one.".formatted(
						ObjectMapper.class.getSimpleName(), MappingJackson2MessageConverter.class.getSimpleName(),
						CompositeMessageConverter.class.getSimpleName())));
		converter.setObjectMapper(objectMapper);
	}

	private Optional<MappingJackson2MessageConverter> getMappingJackson2MessageConverter() {
		return this.payloadMessageConverter instanceof CompositeMessageConverter compositeConverter
				? compositeConverter.getConverters().stream()
						.filter(converter -> converter instanceof MappingJackson2MessageConverter)
						.map(MappingJackson2MessageConverter.class::cast).findFirst()
				: this.payloadMessageConverter instanceof MappingJackson2MessageConverter converter
						? Optional.of(converter)
						: Optional.empty();
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

	private CompositeMessageConverter createDefaultCompositeMessageConverter() {
		List<MessageConverter> messageConverters = new ArrayList<>();
		messageConverters.add(createClassMatchingMessageConverter());
		messageConverters.add(createStringMessageConverter());
		messageConverters.add(createDefaultMappingJackson2MessageConverter());
		return new CompositeMessageConverter(messageConverters);
	}

	private SimpleClassMatchingMessageConverter createClassMatchingMessageConverter() {
		SimpleClassMatchingMessageConverter matchingMessageConverter = new SimpleClassMatchingMessageConverter();
		matchingMessageConverter.setSerializedPayloadClass(String.class);
		return matchingMessageConverter;
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
