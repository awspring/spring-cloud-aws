package io.awspring.cloud.autoconfigure.config.secretsmanager;

import io.awspring.cloud.autoconfigure.core.AwsClientConfigurer;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

public interface AwsClientConfigurerSecretsManager<SecretsManagerClientBuilder> extends AwsClientConfigurer<SecretsManagerClientBuilder> {
}
