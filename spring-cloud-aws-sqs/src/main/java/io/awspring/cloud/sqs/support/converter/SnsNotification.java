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
import java.util.Map;
import java.util.Optional;
import org.springframework.lang.Nullable;

/**
 * Wrapper for SNS notifications that provides access to both the message payload and metadata.
 *
 * <p>
 * Example usage:
 * 
 * <pre>
 * {@code @SqsListener("my-queue")
 * public void handleMessage( @SnsNotification SnsNotification<String> notification) {
 * 	String messageId = notification.getMessageId();
 * 	String topicArn = notification.getTopicArn();
 * 	Optional<String> subject = notification.getSubject();
 * 	String message = notification.getMessage();
 * 	Instant timestamp = notification.getTimestamp();
 * 	Map<String, MessageAttribute> attributes = notification.getMessageAttributes();
 * 	// Process the notification...
 * }
 * }
 * </pre>
 *
 * @param <T> the type of the message payload
 * @author Damien Chomat
 * @see io.awspring.cloud.sqs.support.converter.SnsNotificationConverter
 * @since 3.4.1
 */
public class SnsNotification<T> {
	private final String type;
	private final String messageId;
	private final String topicArn;
	private final T message;
	private final String timestamp;
	private final Map<String, MessageAttribute> messageAttributes;
	@Nullable
	private final String sequenceNumber;
	@Nullable
	private final String subject;
	@Nullable
	private final String unsubscribeUrl;
	@Nullable
	private final String signature;
	@Nullable
	private final String signatureVersion;
	@Nullable
	private final String signingCertURL;

	@JsonCreator
	public SnsNotification(@JsonProperty("Type") String type, @JsonProperty("MessageId") String messageId,
			@JsonProperty("TopicArn") String topicArn, @JsonProperty("Message") T message,
			@JsonProperty("Timestamp") String timestamp,
			@JsonProperty("MessageAttributes") @Nullable Map<String, MessageAttribute> messageAttributes,
			@JsonProperty("SequenceNumber") @Nullable String sequenceNumber,
			@JsonProperty("Subject") @Nullable String subject,
			@JsonProperty("UnsubscribeURL") @Nullable String unsubscribeURL,
			@JsonProperty("Signature") @Nullable String signature,
			@JsonProperty("SignatureVersion") @Nullable String signatureVersion,
			@JsonProperty("SigningCertURL") @Nullable String signingCertURL) {
		this.type = type;
		this.messageId = messageId;
		this.topicArn = topicArn;
		this.sequenceNumber = sequenceNumber;
		this.unsubscribeUrl = unsubscribeURL;
		this.subject = subject;
		this.message = message;
		this.timestamp = timestamp;
		this.messageAttributes = Optional.ofNullable(messageAttributes).orElse(Map.of());
		this.signature = signature;
		this.signatureVersion = signatureVersion;
		this.signingCertURL = signingCertURL;
	}

	/**
	 * Gets the message ID.
	 *
	 * @return the message ID (required field, unique identifier assigned by SNS)
	 */
	public String getMessageId() {
		return messageId;
	}

	/**
	 * Gets the topic ARN.
	 *
	 * @return the topic ARN (required field, ARN of the topic that published the message)
	 */
	public String getTopicArn() {
		return topicArn;
	}

	/**
	 * Gets the message payload.
	 *
	 * @return the message payload (required field, the actual content of the notification)
	 */
	public T getMessage() {
		return message;
	}

	/**
	 * Gets the timestamp.
	 *
	 * @return the timestamp (required field, when the notification was published in ISO-8601 format)
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * Gets the message attributes.
	 *
	 * @return the message attributes (optional field, custom attributes attached to the message)
	 */
	public Map<String, MessageAttribute> getMessageAttributes() {
		return messageAttributes;
	}

	/**
	 * Gets the notification type.
	 *
	 * @return the notification type (required field, always "Notification" for standard SNS messages)
	 */
	public String getType() {
		return type;
	}

	/**
	 * Gets the subject.
	 *
	 * @return the subject (optional field, title/subject set when publishing), or empty if not present
	 */
	public Optional<String> getSubject() {
		return Optional.ofNullable(subject);
	}

	/**
	 * Gets the sequence number.
	 *
	 * @return the sequence number (optional field, present only for FIFO topics), or empty if not present
	 */
	public Optional<String> getSequenceNumber() {
		return Optional.ofNullable(sequenceNumber);
	}

	/**
	 * Gets the unsubscribe URL.
	 *
	 * @return the unsubscribe URL (optional field, URL to unsubscribe from the topic), or empty if not present
	 */
	public Optional<String> getUnsubscribeUrl() {
		return Optional.ofNullable(unsubscribeUrl);
	}

	/**
	 * Gets the signature version.
	 *
	 * @return the signature version (optional field, present when message signing is enabled), or empty if not present
	 */
	public Optional<String> getSignatureVersion() {
		return Optional.ofNullable(signatureVersion);
	}

	/**
	 * Gets the signature.
	 *
	 * @return the signature (optional field, present when message signing is enabled), or empty if not present
	 */
	public Optional<String> getSignature() {
		return Optional.ofNullable(signature);
	}

	/**
	 * Gets the signing certificate URL.
	 *
	 * @return the signing certificate URL (optional field, present when message signing is enabled), or empty if not
	 * present
	 */
	public Optional<String> getSigningCertURL() {
		return Optional.ofNullable(signingCertURL);
	}

	/**
	 * Represents an SNS message attribute.
	 */
	public static class MessageAttribute {
		private final String type;
		private final String value;

		/**
		 * Creates a new message attribute.
		 *
		 * @param type the attribute type
		 * @param value the attribute value
		 */
		@JsonCreator
		public MessageAttribute(@JsonProperty("Type") String type, @JsonProperty("Value") String value) {
			this.type = type;
			this.value = value;
		}

		/**
		 * Gets the attribute type.
		 *
		 * @return the attribute type
		 */
		public String getType() {
			return type;
		}

		/**
		 * Gets the attribute value.
		 *
		 * @return the attribute value
		 */
		public String getValue() {
			return value;
		}
	}
}
