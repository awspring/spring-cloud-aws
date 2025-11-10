package io.awspring.cloud.secretsmanager;

class SecretParseException extends RuntimeException {
	SecretParseException(Exception cause) {
		super(cause);
	}
}
