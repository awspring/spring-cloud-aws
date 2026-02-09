package io.awspring.cloud.autoconfigure.config.appconfig;


/**
 * An exception thrown if there is failure when calling AppConfig.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public class AwsAppConfigPropertySourceNotFoundException extends RuntimeException {
	public AwsAppConfigPropertySourceNotFoundException(Exception e) {
		super(e);
	}
}
