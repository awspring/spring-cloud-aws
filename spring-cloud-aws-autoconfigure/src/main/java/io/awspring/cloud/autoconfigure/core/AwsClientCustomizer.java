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
package io.awspring.cloud.autoconfigure.core;

import org.springframework.lang.Nullable;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;

/**
 * @author Matej Nedić
 * @since 3.0.0
 */
public interface AwsClientCustomizer<T extends AwsClientBuilder<?, ?> & AwsSyncClientBuilder<?, ?>> {

	@Nullable
	default ClientOverrideConfiguration overrideConfiguration() {
		return null;
	}

	@Nullable
	default SdkHttpClient httpClient() {
		return null;
	}

	@Nullable
	default SdkHttpClient.Builder<?> httpClientBuilder() {
		return null;
	}

	static <V extends software.amazon.awssdk.awscore.client.builder.AwsClientBuilder<?, ?> & AwsSyncClientBuilder<?, ?>> void apply(
			AwsClientCustomizer<V> configurer, V builder) {
		if (configurer.overrideConfiguration() != null) {
			builder.overrideConfiguration(configurer.overrideConfiguration());
		}
		if (configurer.httpClient() != null) {
			builder.httpClient(configurer.httpClient());
		}
		if (configurer.httpClientBuilder() != null) {
			builder.httpClientBuilder(configurer.httpClientBuilder());
		}
	}
}
