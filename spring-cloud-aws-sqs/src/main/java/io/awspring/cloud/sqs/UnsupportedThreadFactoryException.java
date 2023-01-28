package io.awspring.cloud.sqs;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

/**
 * Exception thrown when an executor is provided with an unsupported {@link java.util.concurrent.ThreadFactory}.
 * Currently, for improved performance, a {@link MessageExecutionThreadFactory} is required.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class UnsupportedThreadFactoryException extends NestedRuntimeException {

	/**
	 * Create an instance with the provided error message.
	 * @param msg the error message.
	 */
	public UnsupportedThreadFactoryException(String msg) {
		super(msg);
	}

	/**
	 * Create an instance with the provided error message and cause.
	 * @param msg the error message.
	 * @param cause the cause.
	 */
	public UnsupportedThreadFactoryException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
