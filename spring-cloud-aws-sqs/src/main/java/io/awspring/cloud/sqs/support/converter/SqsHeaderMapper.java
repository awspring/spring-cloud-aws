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
import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * A {@link org.springframework.messaging.support.HeaderMapper} implementation for SQS {@link Message}s. Enables
 * creating additional SQS related headers from a {@link SqsMessageConversionContext}.
 * @author Tomaz Fernandes
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
	public void fromHeaders(MessageHeaders headers, Message target) {
		// We'll probably use this for SqsTemplate later
	}

	@Override
	public MessageHeaders toHeaders(Message source) {
		logger.trace("Mapping headers for message {}", source.messageId());
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.copyHeadersIfAbsent(getMessageSystemAttributesAsHeaders(source));
		accessor.copyHeadersIfAbsent(getMessageAttributesAsHeaders(source));
		accessor.copyHeadersIfAbsent(createDefaultHeaders(source));
		accessor.copyHeadersIfAbsent(createAdditionalHeaders(source, new MessageHeaderAccessor()));
		MessageHeaders messageHeaders = accessor.toMessageHeaders();
		logger.trace("Mapped headers {} for message {}", messageHeaders, source.messageId());
		return new MessagingMessageHeaders(messageHeaders, UUID.fromString(source.messageId()));
	}

	private MessageHeaders createAdditionalHeaders(Message source, MessageHeaderAccessor accessor) {
		return this.additionalHeadersFunction.apply(source, accessor);
	}

	private MessageHeaders createDefaultHeaders(Message source) {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setHeader(SqsHeaders.SQS_RECEIPT_HANDLE_HEADER, source.receiptHandle());
		accessor.setHeader(SqsHeaders.SQS_SOURCE_DATA_HEADER, source);
		accessor.setHeader(SqsHeaders.SQS_RECEIVED_AT_HEADER, Instant.now());
		return accessor.toMessageHeaders();
	}

	// @formatter:off
	private Map<String, String> getMessageAttributesAsHeaders(Message source) {
		return source
			.messageAttributes()
			.entrySet()
			.stream()
			.collect(Collectors.toMap(entry -> SqsHeaders.SQS_MA_HEADER_PREFIX + entry.getKey(), entry -> entry.getValue().stringValue()));
	}

	private Map<String, String> getMessageSystemAttributesAsHeaders(Message source) {
		return source
			.attributes()
			.entrySet()
			.stream()
			.collect(Collectors.toMap(entry -> SqsHeaders.MessageSystemAttribute.SQS_MSA_HEADER_PREFIX + entry.getKey(), Map.Entry::getValue));
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
		SqsAsyncClient sqsAsyncClient = sqsContext.getSqsAsyncClient();
		accessor.setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, queueAttributes.getQueueName());
		accessor.setHeader(SqsHeaders.SQS_QUEUE_URL_HEADER, queueAttributes.getQueueUrl());
		accessor.setHeader(SqsHeaders.SQS_QUEUE_ATTRIBUTES_HEADER, queueAttributes);
		accessor.setHeader(SqsHeaders.SQS_VISIBILITY_HEADER,
				new QueueMessageVisibility(sqsAsyncClient, queueAttributes.getQueueUrl(), source.receiptHandle()));
	}

	private void maybeAddAcknowledgementHeader(AcknowledgementAwareMessageConversionContext sqsContext,
			MessageHeaderAccessor accessor) {
		ConfigUtils.INSTANCE.acceptIfNotNull(sqsContext.getAcknowledgementCallback(),
				callback -> accessor.setHeader(SqsHeaders.SQS_ACKNOWLEDGMENT_CALLBACK_HEADER, callback));
	}

}
