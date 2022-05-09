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
package io.awspring.cloud.sqs.listener.source;

import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.QueueAttributesResolver;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsMessageHeaders;
import io.awspring.cloud.sqs.listener.acknowledgement.SqsAcknowledge;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * {@link MessageSource} implementation for polling messages from a SQS queue and converting them to messaging
 * {@link Message}.
 *
 * <p>
 * A {@link io.awspring.cloud.sqs.listener.MessageListenerContainer} can contain many sources, and each source polls
 * from a single queue.
 * </p>
 *
 * <p>
 * Note that currently the payload is not converted here and is returned as String. The actual conversion to the
 * {@link io.awspring.cloud.sqs.annotation.SqsListener} argument type happens on
 * {@link org.springframework.messaging.handler.invocation.InvocableHandlerMethod} invocation.
 * </p>
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsMessageSource<T> extends AbstractPollingMessageSource<T> {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageSource.class);

	private SqsAsyncClient sqsAsyncClient;

	private QueueAttributes queueAttributes;

	private String queueUrl;

	public void setSqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
		Assert.notNull(sqsAsyncClient, "sqsAsyncClient cannot be null.");
		this.sqsAsyncClient = sqsAsyncClient;
	}

	@Override
	protected void doStart() {
		Assert.state(this.sqsAsyncClient != null, "sqsAsyncClient not set");
		this.queueAttributes = QueueAttributesResolver.resolveAttributes(getPollingEndpointName(),
				this.sqsAsyncClient);
		this.queueUrl = queueAttributes.getQueueUrl();
		super.doStart();
	}

	@Override
	protected CompletableFuture<Collection<Message<T>>> doPollForMessages(int messagesToRequest) {
		logger.trace("Polling queue {} for {} messages.", this.queueUrl, getMessagesPerPoll());
		return sqsAsyncClient
				.receiveMessage(req -> req.queueUrl(this.queueUrl).maxNumberOfMessages(messagesToRequest)
					.attributeNames(QueueAttributeName.ALL).waitTimeSeconds((int) getPollTimeout().getSeconds()))
				.thenApply(ReceiveMessageResponse::messages).thenApply(this::convertMessages);
	}

	private Collection<Message<T>> convertMessages(List<software.amazon.awssdk.services.sqs.model.Message> messages) {
		logger.trace("Received {} messages from queue {}", messages.size(), this.queueUrl);
		return messages.stream().map(this::convertMessage).collect(Collectors.toList());
	}

	private Message<T> convertMessage(final software.amazon.awssdk.services.sqs.model.Message message) {
		logger.trace("Converting message {} to messaging message", message.messageId());
		HashMap<String, Object> additionalHeaders = new HashMap<>();
		additionalHeaders.put(SqsMessageHeaders.SQS_LOGICAL_RESOURCE_ID, getPollingEndpointName());
		additionalHeaders.put(SqsMessageHeaders.RECEIVED_AT, Instant.now());
		additionalHeaders.put(SqsMessageHeaders.SQS_CLIENT_HEADER, this.sqsAsyncClient);
		additionalHeaders.put(SqsMessageHeaders.QUEUE_VISIBILITY, this.queueAttributes.getVisibilityTimeout());
		additionalHeaders.put(SqsMessageHeaders.VISIBILITY,
			new QueueMessageVisibility(this.sqsAsyncClient, this.queueUrl, message.receiptHandle()));
		return createMessage(message, Collections.unmodifiableMap(additionalHeaders));
	}

	// TODO: Convert the message payload to type T
	@SuppressWarnings("unchecked")
	private Message<T> createMessage(software.amazon.awssdk.services.sqs.model.Message message,
			Map<String, Object> additionalHeaders) {

		HashMap<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put(SqsMessageHeaders.MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME, message.messageId());
		messageHeaders.put(SqsMessageHeaders.RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME, message.receiptHandle());
		messageHeaders.put(SqsMessageHeaders.SOURCE_DATA_HEADER, message);
		messageHeaders.put(SqsMessageHeaders.ACKNOWLEDGMENT_HEADER,
				new SqsAcknowledge(this.sqsAsyncClient, this.queueUrl, message.receiptHandle()));
		messageHeaders.putAll(additionalHeaders);
		messageHeaders.putAll(getAttributesAsMessageHeaders(message));
		messageHeaders.putAll(getMessageAttributesAsMessageHeaders(message));
		return new GenericMessage<>((T) message.body(), new SqsMessageHeaders(messageHeaders));
	}

	// TODO: Review this logic using streams
	private static Map<String, Object> getMessageAttributesAsMessageHeaders(
			software.amazon.awssdk.services.sqs.model.Message message) {

		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put(SqsMessageHeaders.SQS_GROUP_ID_HEADER,
			message.attributes().get(MessageSystemAttributeName.MESSAGE_GROUP_ID));
		for (Map.Entry<MessageSystemAttributeName, String> messageAttribute : message.attributes().entrySet()) {
			if (org.springframework.messaging.MessageHeaders.CONTENT_TYPE.equals(messageAttribute.getKey().name())) {
				messageHeaders.put(org.springframework.messaging.MessageHeaders.CONTENT_TYPE,
						MimeType.valueOf(messageAttribute.getValue()));
			}
			else if (org.springframework.messaging.MessageHeaders.ID.equals(messageAttribute.getKey().name())) {
				messageHeaders.put(org.springframework.messaging.MessageHeaders.ID,
						UUID.fromString(messageAttribute.getValue()));
			}
			else {
				messageHeaders.put(messageAttribute.getKey().name(), messageAttribute.getValue());
			}
		}
		return Collections.unmodifiableMap(messageHeaders);
	}

	private static Map<String, Object> getAttributesAsMessageHeaders(
			software.amazon.awssdk.services.sqs.model.Message message) {
		Map<String, Object> messageHeaders = new HashMap<>();
		for (Map.Entry<MessageSystemAttributeName, String> attributeKeyValuePair : message.attributes().entrySet()) {
			messageHeaders.put(attributeKeyValuePair.getKey().name(), attributeKeyValuePair.getValue());
		}
		return messageHeaders;
	}
}
