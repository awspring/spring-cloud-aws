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

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.MessagingHeaders;
import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.NumberUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

/**
 * A {@link HeaderMapper} implementation for SQS {@link Message}s. Enables creating additional SQS related headers from
 * a {@link SqsMessageConversionContext}.
 *
 * @author Tomaz Fernandes
 * @author Alain Sahli
 * @author Maciej Walkowiak
 *
 * @since 3.0
 * @see SqsMessagingMessageConverter
 */
public class SqsHeaderMapper implements ContextAwareHeaderMapper<Message> {

	private static final Logger logger = LoggerFactory.getLogger(SqsHeaderMapper.class);

	private BiFunction<Message, MessageHeaderAccessor, MessageHeaders> additionalHeadersFunction = ((message,
			accessor) -> accessor.toMessageHeaders());

	public void setAdditionalHeadersFunction(
			BiFunction<Message, MessageHeaderAccessor, MessageHeaders> headerFunction) {
		Assert.notNull(headerFunction, "headerFunction cannot be null");
		this.additionalHeadersFunction = headerFunction;
	}

	@Override
	public Message fromHeaders(MessageHeaders headers) {
		Message.Builder builder = Message.builder();
		Map<MessageSystemAttributeName, String> attributes = new HashMap<>();
		if (headers.containsKey(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER)) {
			attributes.put(MessageSystemAttributeName.MESSAGE_GROUP_ID,
					headers.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, String.class));
		}
		if (headers.containsKey(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER)) {
			attributes.put(MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID,
					headers.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, String.class));
		}
		if (headers.containsKey(SqsHeaders.MessageSystemAttributes.SQS_AWS_TRACE_HEADER)) {
			attributes.put(MessageSystemAttributeName.AWS_TRACE_HEADER,
				headers.get(SqsHeaders.MessageSystemAttributes.SQS_AWS_TRACE_HEADER, String.class));
		}
		Map<String, MessageAttributeValue> messageAttributes = headers.entrySet().stream()
				.filter(entry -> !isSkipHeader(entry.getKey())).collect(Collectors.toMap(Map.Entry::getKey,
						entry -> getMessageAttributeValue(entry.getKey(), entry.getValue())));
		if (headers.containsKey(SqsHeaders.SQS_DELAY_HEADER)) {
			messageAttributes.put(SqsHeaders.SQS_DELAY_HEADER, getNumberMessageAttribute(Objects.requireNonNull(
					headers.get(SqsHeaders.SQS_DELAY_HEADER, Integer.class), "Delay header value must not be null")));
		}
		String messageId = Objects.requireNonNull(headers.getId(), "No ID found for message").toString();
		return builder.attributes(attributes).messageId(messageId).messageAttributes(messageAttributes).build();
	}

	private MessageAttributeValue getMessageAttributeValue(String messageHeaderName,
			@Nullable Object messageHeaderValue) {
		if (MessageHeaders.CONTENT_TYPE.equals(messageHeaderName) && messageHeaderValue != null) {
			return getContentTypeMessageAttribute(messageHeaderValue);
		}
		else if (messageHeaderValue instanceof String) {
			return getStringMessageAttribute((String) messageHeaderValue);
		}
		else if (messageHeaderValue instanceof Number) {
			return getNumberMessageAttribute(messageHeaderValue);
		}
		else if (messageHeaderValue instanceof ByteBuffer) {
			return getBinaryMessageAttribute((ByteBuffer) messageHeaderValue);
		}
		return getStringMessageAttribute(messageHeaderValue != null ? messageHeaderValue.toString() : "");
	}

	private boolean isSkipHeader(String headerName) {
		return SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER.equals(headerName)
				|| SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER.equals(headerName)
				|| SqsHeaders.MessageSystemAttributes.SQS_AWS_TRACE_HEADER.equals(headerName)
				|| SqsHeaders.SQS_DELAY_HEADER.equals(headerName) || MessageHeaders.ID.equals(headerName)
				|| MessageHeaders.TIMESTAMP.equals(headerName);
	}

	private MessageAttributeValue getBinaryMessageAttribute(ByteBuffer messageHeaderValue) {
		return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.BINARY)
				.binaryValue(SdkBytes.fromByteBuffer(messageHeaderValue)).build();
	}

	private MessageAttributeValue getContentTypeMessageAttribute(Object messageHeaderValue) {
		if (messageHeaderValue instanceof MimeType) {
			return getStringMessageAttribute(messageHeaderValue.toString());
		}
		else if (messageHeaderValue instanceof String stringValue) {
			return getStringMessageAttribute(stringValue);
		}
		return getStringMessageAttribute("");
	}

	private MessageAttributeValue getStringMessageAttribute(String messageHeaderValue) {
		return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING)
				.stringValue(messageHeaderValue).build();
	}

	private MessageAttributeValue getNumberMessageAttribute(Object messageHeaderValue) {
		Assert.isTrue(NumberUtils.STANDARD_NUMBER_TYPES.contains(messageHeaderValue.getClass()),
				"Only standard number types are accepted as message header.");
		return MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.NUMBER + "." + messageHeaderValue.getClass().getName())
				.stringValue(messageHeaderValue.toString()).build();
	}

	@Override
	public MessageHeaders toHeaders(Message source) {
		Assert.notNull(source.messageId(), "messageId must not be null");
		logger.trace("Mapping headers for message {}", source.messageId());
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.copyHeadersIfAbsent(getMessageSystemAttributesAsHeaders(source));
		accessor.copyHeadersIfAbsent(getMessageAttributesAsHeaders(source));
		accessor.copyHeadersIfAbsent(createDefaultHeaders(source));
		accessor.copyHeadersIfAbsent(createAdditionalHeaders(source));
		MessageHeaders messageHeaders = accessor.toMessageHeaders();
		logger.trace("Mapped headers {} for message {}", messageHeaders, source.messageId());
		return new MessagingMessageHeaders(messageHeaders, UUID.fromString(source.messageId()));
	}

	private MessageHeaders createAdditionalHeaders(Message source) {
		return this.additionalHeadersFunction.apply(source, new MessageHeaderAccessor());
	}

	private MessageHeaders createDefaultHeaders(Message source) {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setHeader(SqsHeaders.SQS_RECEIPT_HANDLE_HEADER, source.receiptHandle());
		accessor.setHeader(SqsHeaders.SQS_SOURCE_DATA_HEADER, source);
		accessor.setHeader(SqsHeaders.SQS_RECEIVED_AT_HEADER, Instant.now());
		return accessor.toMessageHeaders();
	}

	// @formatter:off
	private Map<String, Object> getMessageAttributesAsHeaders(Message source) {
		return source
			.messageAttributes()
			.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, this::getValue));
	}

	private Object getValue(Map.Entry<String, MessageAttributeValue> entry) {
		MessageAttributeValue value = entry.getValue();
		String dataType = value.dataType();
		Assert.notNull(dataType, "dataType must not be null");

		if (dataType.contains(".")) {
			dataType = dataType.substring(0, dataType.indexOf('.'));
		}

		return switch (dataType) {
			case MessageAttributeDataTypes.NUMBER -> getNumberValue(value);
			case MessageAttributeDataTypes.BINARY -> value.binaryValue();
			default -> value.stringValue();
		};
	}

	private Map<String, String> getMessageSystemAttributesAsHeaders(Message source) {
		return source
			.attributes()
			.entrySet()
			.stream()
			.collect(Collectors.toMap(entry -> SqsHeaders.MessageSystemAttributes.SQS_MSA_HEADER_PREFIX + entry.getKey(),
				Map.Entry::getValue));
	}
	// @formatter:on

	@Override
	public MessageHeaders createContextHeaders(Message source, MessageConversionContext context) {
		logger.trace("Creating context headers for message {}", source.messageId());
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		ConfigUtils.INSTANCE.acceptIfInstance(context, SqsMessageConversionContext.class,
				sqsContext -> addSqsContextHeaders(source, sqsContext, accessor)).acceptIfInstance(context,
						SqsMessageConversionContext.class, smcc -> maybeAddAcknowledgementHeader(smcc, accessor));
		MessageHeaders messageHeaders = accessor.toMessageHeaders();
		logger.trace("Context headers {} created for message {}", messageHeaders, source.messageId());
		return messageHeaders;
	}

	private void addSqsContextHeaders(Message source, SqsMessageConversionContext sqsContext,
			MessageHeaderAccessor accessor) {
		QueueAttributes queueAttributes = sqsContext.getQueueAttributes();
		if (queueAttributes != null) {
			accessor.setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, queueAttributes.getQueueName());
			accessor.setHeader(SqsHeaders.SQS_QUEUE_URL_HEADER, queueAttributes.getQueueUrl());
			accessor.setHeader(SqsHeaders.SQS_QUEUE_ATTRIBUTES_HEADER, queueAttributes);
		}
		SqsAsyncClient sqsAsyncClient = sqsContext.getSqsAsyncClient();
		if (sqsAsyncClient != null && queueAttributes != null) {
			accessor.setHeader(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER,
					new QueueMessageVisibility(sqsAsyncClient, queueAttributes.getQueueUrl(), source.receiptHandle()));
		}
	}

	private void maybeAddAcknowledgementHeader(AcknowledgementAwareMessageConversionContext sqsContext,
			MessageHeaderAccessor accessor) {
		ConfigUtils.INSTANCE.acceptIfNotNull(sqsContext.getAcknowledgementCallback(),
				callback -> accessor.setHeader(MessagingHeaders.ACKNOWLEDGMENT_CALLBACK_HEADER, callback));
	}

	private Object getNumberValue(String attributeValue, String attributeType) {
		try {
			return NumberParser.parseNumber(attributeValue, attributeType);
		}
		catch (ClassNotFoundException e) {
			throw new MessagingException(
					String.format(
							"Message attribute with value '%s' and data type '%s' could not be converted "
									+ "into a Number because target class was not found.",
							attributeValue, attributeType),
					e);
		}
	}

	private Object getNumberValue(MessageAttributeValue value) {
		return getNumberValue(value.stringValue(), value.dataType());
	}

	private static class NumberParser {

		private static final Map<String, Class<? extends Number>> PRIMITIVE_TO_WRAPPED = Map.of(byte.class.getName(),
				Byte.class, short.class.getName(), Short.class, int.class.getName(), Integer.class,
				long.class.getName(), Long.class, float.class.getName(), Float.class, double.class.getName(),
				Double.class);

		private static Object parseNumber(String value, String type) throws ClassNotFoundException {
			if (MessageAttributeDataTypes.NUMBER.equals(type)) {
				return NumberUtils.parseNumber(value, Number.class);
			}
			else {
				String javaType = type.substring(MessageAttributeDataTypes.NUMBER.length() + 1);
				if (PRIMITIVE_TO_WRAPPED.containsKey(javaType.toLowerCase())) {
					return NumberUtils.parseNumber(value, PRIMITIVE_TO_WRAPPED.get(javaType.toLowerCase()));
				}
				else {
					Class<? extends Number> numberTypeClass = Class.forName(javaType).asSubclass(Number.class);
					return NumberUtils.parseNumber(value, numberTypeClass);
				}
			}
		}

	}

}
