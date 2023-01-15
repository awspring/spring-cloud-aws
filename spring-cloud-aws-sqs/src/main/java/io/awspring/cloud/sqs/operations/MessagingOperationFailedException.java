package io.awspring.cloud.sqs.operations;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Exception to represent the failure of a Messaging Operation.
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessagingOperationFailedException extends NestedRuntimeException {

	private final String endpoint;

	private final Collection<Message<?>> failedMessages;

	/**
	 * Create an instance with the provided error message.
	 * @param msg the message.
	 */
	public MessagingOperationFailedException(String msg, String endpoint) {
		this(msg, endpoint, null);
	}

	/**
	 * Create an instance with the provided error message and cause, if any.
	 * @param msg the error message.
	 * @param cause the cause.
	 */
	public MessagingOperationFailedException(String msg, String endpoint, @Nullable Throwable cause) {
		this(msg, endpoint, Collections.emptyList(), cause);
	}

	/**
	 * Create an instance with the provided parameters and a single message.
	 * @param msg the error message.
	 * @param endpoint the endpoint with which the operation failed.
	 * @param message the message with which the operation failed.
	 * @param cause the cause.
	 */
	public MessagingOperationFailedException(String msg, String endpoint, Message<?> message, @Nullable Throwable cause) {
		this(msg, endpoint, Collections.singletonList(message), cause);
	}

	/**
	 * Create an instance with the provided parameters and a batch of messages.
	 * @param msg the error message.
	 * @param endpoint the endpoint with which the operation failed.
	 * @param messages the messages with which the operation failed.
	 * @param cause the cause.
	 */
	public <T> MessagingOperationFailedException(String msg, String endpoint, Collection<Message<T>> messages,
												 @Nullable Throwable cause) {
		super(msg, cause);
		this.endpoint = endpoint;
		this.failedMessages = Collections.unmodifiableCollection(messages);
	}

	/**
	 * Get the endpoint which the operation failed.
	 * @return the endpoint.
	 */
	public String getEndpoint() {
		return this.endpoint;
	}

	/**
	 * The failed messages.
	 * @return the messages.
	 */
	public Collection<Message<?>> getFailedMessages() {
		return this.failedMessages;
	}

	/**
	 * A single failed message, if present.
	 * @return the message if present.
	 */
	public Optional<Message<?>> getFailedMessage() {
		Assert.isTrue(this.failedMessages.size() < 2, "More than one message found");
		return this.failedMessages.stream().findFirst();
	}

}
