package io.awspring.cloud.sqs.operations;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Options for sending messages to SQS queues, with a method chaining API.
 * @param <T> the payload type.
 * @param <O> the implementation class to be returned by the chained methods.
 */
public interface SqsSendOptions<T, O extends SqsSendOptions<T, O>> {

	/**
	 * Set the queue to send the message to.
	 * @param queue the queue name.
	 * @return the options instance.
	 */
	O queue(String queue);

	/**
	 * The payload to send in the message. The payload will be serialized if necessary.
	 * @param payload the payload.
	 * @return the options instance.
	 */
	O payload(T payload);

	/**
	 * Add a header to be sent in the message. The header will be sent
	 * as a MessageAttribute.
	 *
	 * @param headerName the header name.
	 * @param headerValue the header value.
	 * @return the options instance.
	 */
	O header(String headerName, Object headerValue);

	/**
	 * Add headers to be sent in the message. The headers will be sent
	 * as MessageAttributes.
	 *
	 * @param headers the headers to add.
	 * @return the options instance.
	 */
	O headers(Map<String, Object> headers);

	/**
	 * Set a delay for the message.
	 * @param delay the delay.
	 * @return the options instance.
	 */
	O delay(Duration delay);

	/**
	 * Specific options for Standard Sqs queues.
	 * @param <T> the payload type.
	 */
	interface Standard<T> extends SqsSendOptions<T, Standard<T>> {
	}

	/**
	 * Specific options for Fifo Sqs queues.
	 * @param <T> the payload type.
	 */
	interface Fifo<T> extends SqsSendOptions<T, Fifo<T>> {

		/**
		 * Set the messageGroupId for the message. If none is provided,
		 * a random one is added.
		 * @param messageGroupId the id.
		 * @return the options instance.
		 */
		Fifo<T> messageGroupId(UUID messageGroupId);

		/**
		 * Set the messageDeduplicationId for the message. If none is provided,
		 * a random on is added.
		 * @param messageDeduplicationId the id.
		 * @return the options instance.
		 */
		Fifo<T> messageDeduplicationId(UUID messageDeduplicationId);

	}

}
