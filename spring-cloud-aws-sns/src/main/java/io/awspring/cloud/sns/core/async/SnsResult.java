package io.awspring.cloud.sns.core.async;

import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;

/**
 * Result of an SNS operation.
 *
 * @param message the original message that was sent
 * @param messageId the SNS message ID returned by AWS
 * @param sequenceNumber the sequence number for FIFO topics (null for standard topics)
 * @param <T> the message payload type
 * @author Matej Nedic
 * @since 4.0.1
 */
public record SnsResult<T>(
	Message<T> message,
	String messageId,
	@Nullable String sequenceNumber
) {
}
