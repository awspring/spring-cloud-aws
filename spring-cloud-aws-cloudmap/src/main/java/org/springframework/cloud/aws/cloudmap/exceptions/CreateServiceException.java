package org.springframework.cloud.aws.cloudmap.exceptions;

// Throw in case of cloudmap service exception.
public class CreateServiceException extends RuntimeException {

	public CreateServiceException(Throwable cause) {
		super(cause);
	}

}
