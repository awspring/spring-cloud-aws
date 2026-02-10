package io.awspring.cloud.sns.core.async;

import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Pair containing a PublishRequest and the original Spring Message.
 *
 * @param publishRequest the AWS SNS publish request
 * @param originalMessage the original Spring message
 * @param <T> the message payload type
 * @author Matej Nedic
 * @since 4.1.0
 */
public record PublishRequestMessagePair<T>(
	PublishRequest publishRequest,
	Message<T> originalMessage
) {
}
