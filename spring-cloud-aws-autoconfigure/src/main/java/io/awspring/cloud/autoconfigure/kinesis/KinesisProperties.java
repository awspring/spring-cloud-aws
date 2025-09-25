package io.awspring.cloud.autoconfigure.kinesis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static io.awspring.cloud.autoconfigure.kinesis.KinesisProperties.PREFIX;

/**
 * Properties related to KinesisClient
 *
 * @author Matej Nedic
 * @since 4.0.0
 */
@ConfigurationProperties(prefix = PREFIX)
public class KinesisProperties {
	/**
	 * The prefix used for AWS Kinesis configuration.
	 */
	public static final String PREFIX = "spring.cloud.aws.kinesis";
}
