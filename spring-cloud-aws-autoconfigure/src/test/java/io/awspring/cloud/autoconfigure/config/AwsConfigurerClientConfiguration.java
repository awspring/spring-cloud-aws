/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.autoconfigure.config;

import io.awspring.cloud.autoconfigure.config.parameterstore.AwsParameterStoreClientConfigurer;
import io.awspring.cloud.autoconfigure.config.secretsmanager.AwsSecretsManagerClientConfigurer;
import java.time.Duration;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

public class AwsConfigurerClientConfiguration implements BootstrapRegistryInitializer {

	@Override
	public void initialize(BootstrapRegistry registry) {
		registry.register(AwsSecretsManagerClientConfigurer.class, context -> new AwsClientConfigurerSecrets());
		registry.register(AwsParameterStoreClientConfigurer.class, context -> new AwsClientConfigurerParameter());
	}

	public static class AwsClientConfigurerSecrets implements AwsSecretsManagerClientConfigurer {

		public ClientOverrideConfiguration overrideConfiguration() {
			return ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofMillis(1542)).build();
		}

		@Override
		public SdkHttpClient httpClient() {
			return ApacheHttpClient.builder().connectionTimeout(Duration.ofMillis(1542)).build();
		}
	}

	public static class AwsClientConfigurerParameter implements AwsParameterStoreClientConfigurer {

		public ClientOverrideConfiguration overrideConfiguration() {
			return ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofMillis(2828)).build();
		}

		@Override
		public SdkHttpClient httpClient() {
			return ApacheHttpClient.builder().connectionTimeout(Duration.ofMillis(1542)).build();
		}
	}
}
