package io.awspring.cloud.autoconfigure.s3vectors;

import io.awspring.cloud.autoconfigure.AwsClientCustomizer;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3vectors.S3VectorsClientBuilder;

/**
 * Callback interface that can be used to customize a {@link S3ClientBuilder}.
 *
 * @author Matej Nedic
 * @since 3.5.0
 */
@FunctionalInterface
public interface S3VectorClientCustomizer extends AwsClientCustomizer<S3VectorsClientBuilder> {
}
