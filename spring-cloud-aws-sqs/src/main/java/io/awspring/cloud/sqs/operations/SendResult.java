package io.awspring.cloud.sqs.operations;

import org.springframework.messaging.Message;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * The result of a send operation.
 * @param messageId the message id as returned by the endpoint if successful, the id from the original {@link Message} if failed.
 * @param message the message that was sent, with any additional headers added by the framework.
 * @param additionalInformation additional information on the send operation.
 * @param <T> the message payload type.
 */
public record SendResult<T>(UUID messageId, String endpoint, Message<T> message, Map<String, Object> additionalInformation) {

	/**
	 * The result of a batch send operation.
	 * @param successful the {@link SendResult} for messages successfully sent.
	 * @param failed the {@link SendResult} for messages that failed to be sent.
	 * @param <T> the message payload type.
	 */
	public record Batch<T>(Collection<SendResult<T>> successful, Collection<SendResult.Failed<T>> failed) {}

	/**
	 * The result of a failed send operation.
	 * @param errorMessage a message with information on the error.
	 * @param <T> the message payload type.
	 */
	public record Failed<T> (String errorMessage, String endpoint, Message<T> message, Map<String, Object> additionalInformation) {}

}
