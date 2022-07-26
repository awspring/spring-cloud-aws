package io.awspring.cloud.sqs.support;

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsMessageHeaders;
import io.awspring.cloud.sqs.listener.acknowledgement.SqsAcknowledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsMessageConverter<T> {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageConverter.class);

	private final SqsAsyncClient sqsAsyncClient;

	private final QueueAttributes queueAttributes;

	public SqsMessageConverter(QueueAttributes queueAttributes, SqsAsyncClient sqsAsyncClient) {
		this.sqsAsyncClient = sqsAsyncClient;
		this.queueAttributes = queueAttributes;
	}

	public Collection<Message<T>> toMessagingMessages(List<software.amazon.awssdk.services.sqs.model.Message> messages) {
		logger.trace("Converting  {} messages from queue {}", messages.size(), this.queueAttributes.getQueueName());
		return messages.stream().map(this::toMessagingMessage).collect(Collectors.toList());
	}

	public Message<T> toMessagingMessage(software.amazon.awssdk.services.sqs.model.Message message) {
		logger.trace("Converting message {} to messaging message", message.messageId());
		HashMap<String, Object> additionalHeaders = new HashMap<>();
		additionalHeaders.put(SqsMessageHeaders.SQS_LOGICAL_RESOURCE_ID, this.queueAttributes.getQueueName());
		additionalHeaders.put(SqsMessageHeaders.SQS_QUEUE_URL, this.queueAttributes.getQueueUrl());
		additionalHeaders.put(SqsMessageHeaders.RECEIVED_AT, Instant.now()); // TODO: Is this necessary?
		if (this.queueAttributes.getVisibilityTimeout() != null) {
			additionalHeaders.put(SqsMessageHeaders.QUEUE_VISIBILITY, this.queueAttributes.getVisibilityTimeout());
		}
		additionalHeaders.put(SqsMessageHeaders.VISIBILITY,
			new QueueMessageVisibility(this.sqsAsyncClient, this.queueAttributes.getQueueUrl(), message.receiptHandle()));
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
			new SqsAcknowledge(this.sqsAsyncClient, this.queueAttributes.getQueueUrl(), message.receiptHandle()));
		messageHeaders.putAll(additionalHeaders);
		messageHeaders.putAll(getAttributesAsMessageHeaders(message));
		messageHeaders.putAll(getMessageAttributesAsMessageHeaders(message));
		return new GenericMessage<>((T) message.body(), new SqsMessageHeaders(messageHeaders));
	}

	// TODO: Review this logic using streams
	private static Map<String, Object> getMessageAttributesAsMessageHeaders(
		software.amazon.awssdk.services.sqs.model.Message message) {

		Map<String, Object> messageHeaders = new HashMap<>();
		if (message.attributes().get(MessageSystemAttributeName.MESSAGE_GROUP_ID) != null) {
			messageHeaders.put(SqsMessageHeaders.SQS_GROUP_ID_HEADER,
				message.attributes().get(MessageSystemAttributeName.MESSAGE_GROUP_ID));
		}
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
