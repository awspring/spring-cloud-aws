package io.awspring.cloud.v3.autoconfigure.s3;

import io.awspring.cloud.v3.autoconfigure.AwsClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(S3Properties.PREFIX)
public class S3Properties extends AwsClientProperties {
	/**
	 * The prefix used for AWS credentials related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.s3";
}
