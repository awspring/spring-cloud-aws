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
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

/**
 * @author Matej Nedić
 * @since 3.0.0
 */
@Deprecated(since = "3.3.0", forRemoval = true)
public interface AwsClientCustomizer<T> {

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

	@Nullable
	default SdkAsyncHttpClient asyncHttpClient() {
		return null;
	}

	@Nullable
	default SdkAsyncHttpClient.Builder<?> asyncHttpClientBuilder() {
		return null;
	}

	static <V extends AwsClientBuilder<?, ?>> void apply(AwsClientCustomizer<V> configurer, V builder) {
		if (configurer.overrideConfiguration() != null) {
			builder.overrideConfiguration(configurer.overrideConfiguration());
		}

		if (builder instanceof AwsSyncClientBuilder) {
			AwsSyncClientBuilder syncClientBuilder = (AwsSyncClientBuilder) builder;
			if (configurer.httpClient() != null) {
				syncClientBuilder.httpClient(configurer.httpClient());
			}
			if (configurer.httpClientBuilder() != null) {
				syncClientBuilder.httpClientBuilder(configurer.httpClientBuilder());
			}
		}
		else if (builder instanceof AwsAsyncClientBuilder) {
			AwsAsyncClientBuilder asyncClientBuilder = (AwsAsyncClientBuilder) builder;
			if (configurer.asyncHttpClient() != null) {
				asyncClientBuilder.httpClient(configurer.asyncHttpClient());
			}
			if (configurer.asyncHttpClientBuilder() != null) {
				asyncClientBuilder.httpClientBuilder(configurer.asyncHttpClientBuilder());
			}
		}
	}
}
