package io.awspring.cloud.autoconfigure.s3vectors;

import io.awspring.cloud.autoconfigure.AwsClientCustomizer;
import software.amazon.awssdk.services.s3vectors.S3VectorsClientBuilder;

/**
 * Callback interface that can be used to customize a {@link S3VectorsClientBuilder}.
 *
 * @author Matej Nedic
 * @since 4.0.0
 */
@FunctionalInterface
public interface S3VectorClientCustomizer extends AwsClientCustomizer<S3VectorsClientBuilder> {
}
