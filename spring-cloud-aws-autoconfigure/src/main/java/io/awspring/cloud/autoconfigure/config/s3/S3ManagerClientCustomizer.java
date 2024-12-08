package io.awspring.cloud.autoconfigure.config.s3;

import io.awspring.cloud.autoconfigure.AwsClientCustomizer;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Callback interface that can be used to customize a {@link S3ClientBuilder}.
 *
 * @author Matej Nedic
 * @since 3.4.0
 */
@FunctionalInterface
public interface S3ManagerClientCustomizer  extends AwsClientCustomizer<S3ClientBuilder> {
}

