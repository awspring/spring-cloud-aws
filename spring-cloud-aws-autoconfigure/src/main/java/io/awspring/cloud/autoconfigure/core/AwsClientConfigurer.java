package io.awspring.cloud.autoconfigure.core;

import org.springframework.lang.Nullable;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;

public interface AwsClientConfigurer<AwsClientBuilder>  {

	@Nullable
	default ClientOverrideConfiguration overrideConfiguration() {
		return null;
	}

	@Nullable
	default <T extends SdkHttpClient.Builder<T>> SdkHttpClient.Builder<T> httpClientBuilder() {
		return null;
	}

	static <T extends software.amazon.awssdk.awscore.client.builder.AwsClientBuilder<?, ?> & AwsSyncClientBuilder<?, ?>>  void apply(
		T builder, @Nullable AwsClientConfigurer<T> configurer) {
		if (configurer != null) {
			if (configurer.overrideConfiguration() != null) {
				builder.overrideConfiguration(configurer.overrideConfiguration());
			}
			if (configurer.httpClientBuilder() != null) {
				builder.httpClientBuilder(configurer.httpClientBuilder());
			}
		}
	}
}
