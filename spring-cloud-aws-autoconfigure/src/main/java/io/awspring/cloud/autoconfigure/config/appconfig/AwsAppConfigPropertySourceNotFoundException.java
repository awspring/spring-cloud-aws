package io.awspring.cloud.autoconfigure.config.appconfig;

public class AwsAppConfigPropertySourceNotFoundException extends RuntimeException {
	public AwsAppConfigPropertySourceNotFoundException(Exception e) {
		super(e);
	}
}
