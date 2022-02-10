package io.awspring.cloud.v3.autoconfigure.sns;

import io.awspring.cloud.v3.autoconfigure.AwsClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AWS Simple Notification Service.
 *
 * @author Eddú Meléndez
 */
@ConfigurationProperties(prefix = SnsProperties.PREFIX)
public class SnsProperties extends AwsClientProperties {
	/**
	 * The prefix used for AWS credentials related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.sns";
}
