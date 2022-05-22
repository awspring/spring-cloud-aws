package io.awspring.cloud.autoconfigure.config.secretsmanager;

import io.awspring.cloud.autoconfigure.config.parameterstore.AwsClientConfigurerParameterStore;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;


public class SecretsManagerStoreBootstrapConfiguration implements BootstrapRegistryInitializer {

	@Override
	public void initialize(BootstrapRegistry registry) {
		registry.register(AwsClientConfigurerSecretsManager.class, context -> new AwsClientConfigurerSecrets<SecretsManagerClientBuilder>());
		registry.register(AwsClientConfigurerParameterStore.class, context -> new AwsClientConfigurerParameter<SsmClientBuilder>());
	}

	public class AwsClientConfigurerSecrets<SecretsManagerClientBuilder> implements AwsClientConfigurerSecretsManager<SecretsManagerClientBuilder> {

		public ClientOverrideConfiguration overrideConfiguration() {
			return ClientOverrideConfiguration.builder().build();
		}

		public <T extends SdkHttpClient.Builder<T>> SdkHttpClient.Builder<T> httpClientBuilder() {
			return null;
		}
	}

	public class AwsClientConfigurerParameter<SsmClientBuilder> implements AwsClientConfigurerParameterStore<SsmClientBuilder> {

		public ClientOverrideConfiguration overrideConfiguration() {
			return ClientOverrideConfiguration.builder().build();
		}

		public <T extends SdkHttpClient.Builder<T>> SdkHttpClient.Builder<T> httpClientBuilder() {
			return null;
		}

	}
}
