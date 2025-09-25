package io.awspring.cloud.autoconfigure.kinesis;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static io.awspring.cloud.autoconfigure.kinesis.KinesisProperties.PREFIX;

/**
 * Properties related to KinesisClient
 *
 * @author Matej Nedic
 * @since 4.0.0
 */
@ConfigurationProperties(prefix = PREFIX)
public class KinesisProperties extends AwsClientProperties {
	/**
	 * The prefix used for AWS Kinesis configuration.
	 */
	public static final String PREFIX = "spring.cloud.aws.kinesis";
}
