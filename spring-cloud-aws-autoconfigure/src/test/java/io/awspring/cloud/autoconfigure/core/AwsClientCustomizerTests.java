/*
 * Copyright 2013-2023 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

class AwsClientCustomizerTests {
	AwsClientBuilder<?, ?> syncClientBuilder = mock(AwsClientBuilder.class,
			withSettings().extraInterfaces(AwsSyncClientBuilder.class));
	AwsClientBuilder<?, ?> asyncClientBuilder = mock(AwsClientBuilder.class,
			withSettings().extraInterfaces(AwsAsyncClientBuilder.class));

	@Test
	void applyOverrideConfigurationCustomizer() {
		var customizer = new AwsClientCustomizer<AwsClientBuilder<?, ?>>() {
			@Override
			public ClientOverrideConfiguration overrideConfiguration() {
				return ClientOverrideConfiguration.builder().build();
			}
		};

		AwsClientCustomizer.apply(customizer, syncClientBuilder);
		assertAll(() -> verify(syncClientBuilder).overrideConfiguration(any(ClientOverrideConfiguration.class)),
				() -> verify((AwsSyncClientBuilder<?, ?>) syncClientBuilder, never())
						.httpClient(any(SdkHttpClient.class)),
				() -> verify((AwsSyncClientBuilder<?, ?>) syncClientBuilder, never())
						.httpClientBuilder(any(SdkHttpClient.Builder.class)));
	}

	@Test
	void applySyncClientCustomizer() {
		var customizer = new AwsClientCustomizer<AwsClientBuilder<?, ?>>() {
			@Override
			public SdkHttpClient httpClient() {
				return mock(SdkHttpClient.class);
			}

			@Override
			public SdkHttpClient.Builder<?> httpClientBuilder() {
				return mock(SdkHttpClient.Builder.class);
			}
		};

		AwsClientCustomizer.apply(customizer, syncClientBuilder);
		assertAll(
				() -> verify(syncClientBuilder, never()).overrideConfiguration(any(ClientOverrideConfiguration.class)),
				() -> verify((AwsSyncClientBuilder<?, ?>) syncClientBuilder).httpClient(any(SdkHttpClient.class)),
				() -> verify((AwsSyncClientBuilder<?, ?>) syncClientBuilder)
						.httpClientBuilder(any(SdkHttpClient.Builder.class)));
	}

	@Test
	void applyAsyncClientBuilderCustomizer() {
		var customizer = new AwsClientCustomizer<AwsClientBuilder<?, ?>>() {
			@Override
			public SdkAsyncHttpClient asyncHttpClient() {
				return mock(SdkAsyncHttpClient.class);
			}

			@Override
			public SdkAsyncHttpClient.Builder<?> asyncHttpClientBuilder() {
				return mock(SdkAsyncHttpClient.Builder.class);
			}
		};

		AwsClientCustomizer.apply(customizer, asyncClientBuilder);
		assertAll(
				() -> verify(asyncClientBuilder, never()).overrideConfiguration(any(ClientOverrideConfiguration.class)),
				() -> verify((AwsAsyncClientBuilder<?, ?>) asyncClientBuilder)
						.httpClient(any(SdkAsyncHttpClient.class)),
				() -> verify((AwsAsyncClientBuilder<?, ?>) asyncClientBuilder)
						.httpClientBuilder(any(SdkAsyncHttpClient.Builder.class)));
	}

	@Test
	void applyEmptyCustomizer() {
		var customizer = new AwsClientCustomizer<AwsClientBuilder<?, ?>>() {
		};
		AwsClientCustomizer.apply(customizer, asyncClientBuilder);
		assertAll(
				() -> verify(asyncClientBuilder, never()).overrideConfiguration(any(ClientOverrideConfiguration.class)),
				() -> verify((AwsAsyncClientBuilder<?, ?>) asyncClientBuilder, never())
						.httpClient(any(SdkAsyncHttpClient.class)),
				() -> verify((AwsAsyncClientBuilder<?, ?>) asyncClientBuilder, never())
						.httpClientBuilder(any(SdkAsyncHttpClient.Builder.class)));
	}
}
