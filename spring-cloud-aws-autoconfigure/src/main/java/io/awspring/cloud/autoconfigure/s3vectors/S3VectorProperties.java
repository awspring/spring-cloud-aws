package io.awspring.cloud.autoconfigure.s3vectors;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * @author Matej Nedic
 */
@ConfigurationProperties(prefix = S3VectorProperties.PREFIX)
public class S3VectorProperties extends AwsClientProperties {

	/**
	 * The prefix used for S3 related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.s3.vector";


}
