package org.springframework.cloud.aws.cloudmap.exceptions;

// Thrown in case maximum retry for polling has exceeded.
public class MaxRetryExceededException extends RuntimeException {

	public MaxRetryExceededException(String message) {
		super(message);
	}

}
