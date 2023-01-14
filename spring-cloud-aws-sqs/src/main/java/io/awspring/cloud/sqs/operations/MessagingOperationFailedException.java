package io.awspring.cloud.sqs.operations;

import org.springframework.lang.Nullable;

/**
 * Exception to represent the failure of a Messaging Operation.
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessagingOperationFailedException extends RuntimeException {

	/**
	 * Create an instance with the provided error message.
	 * @param msg the message.
	 */
	public MessagingOperationFailedException(String msg) {
		super(msg);
	}

	/**
	 * Create an instance with the provided error message and cause, if any.
	 * @param msg the error message.
	 * @param cause the cause.
	 */
	public MessagingOperationFailedException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
