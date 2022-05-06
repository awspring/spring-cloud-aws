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
package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.messaging.support.listener.AsyncMessageProducer;
import io.awspring.cloud.messaging.support.listener.MessageHeaders;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsMessageProducer implements AsyncMessageProducer<String>, SmartLifecycle {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageProducer.class);

	private final String logicalEndpointName;

	private final QueueAttributes queueAttributes;

	private final SqsAsyncClient sqsAsyncClient;

	private final String queueUrl;

	private volatile boolean running;

	public SqsMessageProducer(String logicalEndpointName, QueueAttributes queueAttributes, SqsAsyncClient sqsClient) {
		this.logicalEndpointName = logicalEndpointName;
		this.queueUrl = queueAttributes.getDestinationUrl();
		this.queueAttributes = queueAttributes;
		this.sqsAsyncClient = sqsClient;
	}

	@Override
	public CompletableFuture<Collection<Message<String>>> produce(int numberOfMessages, Duration timeout) {
		logger.trace("Polling for messages at {}", this.queueUrl);
		return sqsAsyncClient
				.receiveMessage(req -> req.queueUrl(this.queueUrl).maxNumberOfMessages(numberOfMessages)
						.waitTimeSeconds((int) timeout.getSeconds()))
				.thenApply(ReceiveMessageResponse::messages).thenApply(this::getMessagesForExecution)
				.exceptionally(handleException());
	}

	protected Function<Throwable, Collection<Message<String>>> handleException() {
		return t -> {
			logger.error("Error retrieving messages from SQS", t);
			return Collections.emptyList();
		};
	}

	private Collection<Message<String>> getMessagesForExecution(
			List<software.amazon.awssdk.services.sqs.model.Message> messages) {
		logger.trace("Poll returned {} messages", messages.size());
		return messages.stream().map(this::getMessageForExecution).collect(Collectors.toList());
	}

	protected org.springframework.messaging.Message<String> getMessageForExecution(
			final software.amazon.awssdk.services.sqs.model.Message message) {
		HashMap<String, Object> additionalHeaders = new HashMap<>();
		additionalHeaders.put(SqsMessageHeaders.SQS_LOGICAL_RESOURCE_ID, this.logicalEndpointName);
		additionalHeaders.put(SqsMessageHeaders.RECEIVED_AT, Instant.now());
		additionalHeaders.put(SqsMessageHeaders.QUEUE_VISIBILITY, this.queueAttributes.getVisibilityTimeout());
		additionalHeaders.put(SqsMessageHeaders.VISIBILITY,
				new QueueMessageVisibility(this.sqsAsyncClient, this.queueUrl, message.receiptHandle()));
		return createMessage(message, additionalHeaders);
	}

	protected org.springframework.messaging.Message<String> createMessage(
			software.amazon.awssdk.services.sqs.model.Message message, Map<String, Object> additionalHeaders) {

		HashMap<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put(SqsMessageHeaders.MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME, message.messageId());
		messageHeaders.put(SqsMessageHeaders.RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME, message.receiptHandle());
		messageHeaders.put(SqsMessageHeaders.SOURCE_DATA_HEADER, message);
		messageHeaders.put(MessageHeaders.ACKNOWLEDGMENT_HEADER,
				new SqsAcknowledge(this.sqsAsyncClient, this.queueUrl, message.receiptHandle()));
		messageHeaders.putAll(additionalHeaders);
		messageHeaders.putAll(getAttributesAsMessageHeaders(message));
		messageHeaders.putAll(getMessageAttributesAsMessageHeaders(message));
		return new GenericMessage<>(message.body(), new SqsMessageHeaders(messageHeaders));
	}

	private static Map<String, Object> getMessageAttributesAsMessageHeaders(
			software.amazon.awssdk.services.sqs.model.Message message) {

		Map<String, Object> messageHeaders = new HashMap<>();
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
		return messageHeaders;
	}

	private static Map<String, Object> getAttributesAsMessageHeaders(
			software.amazon.awssdk.services.sqs.model.Message message) {
		Map<String, Object> messageHeaders = new HashMap<>();
		for (Map.Entry<MessageSystemAttributeName, String> attributeKeyValuePair : message.attributes().entrySet()) {
			messageHeaders.put(attributeKeyValuePair.getKey().name(), attributeKeyValuePair.getValue());
		}
		return messageHeaders;
	}

	@Override
	public void start() {
		logger.debug("Starting SqsMessageProducer for {}", this.logicalEndpointName);
		this.running = true;
	}

	@Override
	public void stop() {
		logger.debug("Stopping SqsMessageProducer for {}", this.logicalEndpointName);
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}
}
