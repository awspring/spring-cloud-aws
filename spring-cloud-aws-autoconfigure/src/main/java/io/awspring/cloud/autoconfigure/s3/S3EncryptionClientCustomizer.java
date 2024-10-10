package io.awspring.cloud.autoconfigure.s3;

import io.awspring.cloud.autoconfigure.AwsClientCustomizer;
import software.amazon.encryption.s3.S3EncryptionClient;

/**
 * Callback interface that can be used to customize a {@link S3EncryptionClient.Builder}.
 *
 * @author Matej Nedic
 * @since 3.3.0
 */
@FunctionalInterface
public interface S3EncryptionClientCustomizer extends AwsClientCustomizer<S3EncryptionClient.Builder> {
}
