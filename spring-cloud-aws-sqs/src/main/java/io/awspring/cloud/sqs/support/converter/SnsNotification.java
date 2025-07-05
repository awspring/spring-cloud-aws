/*
 * Copyright 2013-2025 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Wrapper for SNS notifications that provides access to both the message payload and metadata.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @SqsListener("my-queue")
 * public void handleMessage(@SnsNotification SnsNotification<String> notification) {
 *     String messageId = notification.getMessageId();
 *     String topicArn = notification.getTopicArn();
 *     Optional<String> subject = notification.getSubject();
 *     String message = notification.getMessage();
 *     Instant timestamp = notification.getTimestamp();
 *     Map<String, MessageAttribute> attributes = notification.getMessageAttributes();
 *     // Process the notification...
 * }
 * }
 * </pre>
 *
 * @param <T> the type of the message payload
 * @author Damien Chomat
 * @since 3.4.1
 * @see io.awspring.cloud.sqs.support.converter.SnsNotificationConverter
 */
public class SnsNotification<T> {

	private final String type;
	private final String messageId;
	private final String sequenceNumber;
    private final String topicArn;
	private final String subject;
	private final T message;
    private final Instant timestamp;
	private final String unsubscribeUrl;
    private final Map<String, MessageAttribute> messageAttributes;

    /**
     * Creates a new SNS notification.
     * @param messageId the message ID
     * @param topicArn the topic ARN
     * @param subject the subject (optional)
     * @param message the message payload
     * @param timestamp the timestamp
     * @param messageAttributes the message attributes
     */
	@JsonCreator
	public SnsNotification(
            @JsonProperty("Type") String type,
            @JsonProperty("MessageId") String messageId,
            @JsonProperty("TopicArn") String topicArn,
            @JsonProperty("Subject") String subject,
            @JsonProperty("SequenceNumber") String sequenceNumber,
            @JsonProperty("Message") T message,
            @JsonProperty("Timestamp") Instant timestamp,
            @JsonProperty("UnsubscribeURL") String unsubscribeUrl,
            @JsonProperty("MessageAttributes") Map<String, MessageAttribute> messageAttributes) {
        this.type = type;
        this.messageId = messageId;
        this.topicArn = topicArn;
        this.sequenceNumber = sequenceNumber;
        this.unsubscribeUrl = unsubscribeUrl;
        this.subject = subject;
        this.message = message;
        this.timestamp = timestamp;
        this.messageAttributes = messageAttributes;
    }
    
    

    /**
     * Gets the message ID.
     * @return the message ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Gets the topic ARN.
     * @return the topic ARN
     */
    public String getTopicArn() {
        return topicArn;
    }

    /**
     * Gets the subject.
     * @return the subject, or empty if not present
     */
    public Optional<String> getSubject() {
        return Optional.ofNullable(subject);
    }

    /**
     * Gets the message payload.
     * @return the message payload
     */
    public T getMessage() {
        return message;
    }

    /**
     * Gets the timestamp.
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the message attributes.
     * @return the message attributes
     */
    public Map<String, MessageAttribute> getMessageAttributes() {
        return messageAttributes;
    }

    /**
     * Gets the notification type.
     *
     * @return the notification type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the sequence number.
     *
     * @return the sequence number
     */
    public String getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Gets the unsubscribe URL.
     *
     * @return the unsubscribe URL
     */
    public String getUnsubscribeUrl() {
        return unsubscribeUrl;
    }

    /**
     * Represents an SNS message attribute.
     */
    public static class MessageAttribute {
        private final String type;
        private final String value;

        /**
         * Creates a new message attribute.
         * @param type the attribute type
         * @param value the attribute value
         */
        @JsonCreator
        public MessageAttribute(
                @JsonProperty("Type") String type,
                @JsonProperty("Value") String value) {
            this.type = type;
            this.value = value;
        }

        /**
         * Gets the attribute type.
         * @return the attribute type
         */
        public String getType() {
            return type;
        }

        /**
         * Gets the attribute value.
         * @return the attribute value
         */
        public String getValue() {
            return value;
        }
    }
}
