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
package io.awspring.cloud.autoconfigure.s3;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import java.net.URI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.endpoints.S3ClientContextParams;
import software.amazon.awssdk.services.s3.internal.crt.S3NativeClientConfiguration;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Tests for {@link S3CrtAsyncClientAutoConfiguration}.
 *
 * @author Maciej Walkowiak
 */
class S3CrtAsyncClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, S3CrtAsyncClientAutoConfiguration.class));

	@Nested
	class S3CrtAsyncClientEndpointConfigurationTests {
		@Test
		void withCustomEndpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.s3.endpoint:http://localhost:8090").run(context -> {
				S3AsyncClient client = context.getBean(S3AsyncClient.class);
				ConfiguredAwsClient configuredClient = new ConfiguredAwsClient(client);
				assertThat(configuredClient.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
				assertThat(configuredClient.isEndpointOverridden()).isTrue();
			});
		}

		@Test
		void withCustomGlobalEndpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090").run(context -> {
				S3AsyncClient client = context.getBean(S3AsyncClient.class);
				ConfiguredAwsClient configuredClient = new ConfiguredAwsClient(client);
				assertThat(configuredClient.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
				assertThat(configuredClient.isEndpointOverridden()).isTrue();
			});
		}

		@Test
		void withCustomGlobalEndpointAndS3Endpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090",
					"spring.cloud.aws.s3.endpoint:http://localhost:9999").run(context -> {
						S3AsyncClient client = context.getBean(S3AsyncClient.class);
						ConfiguredAwsClient configuredClient = new ConfiguredAwsClient(client);
						assertThat(configuredClient.getEndpoint()).isEqualTo(URI.create("http://localhost:9999"));
						assertThat(configuredClient.isEndpointOverridden()).isTrue();
					});
		}
	}

	@Test
	void withPathStyleAccessEnabled() {
		contextRunner.withPropertyValues("spring.cloud.aws.s3.path-style-access-enabled:true").run(context -> {
			S3AsyncClient client = context.getBean(S3AsyncClient.class);
			S3AsyncClient delegate = (S3AsyncClient) ReflectionTestUtils.getField(client, "delegate");
			SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(delegate,
					"clientConfiguration");
			AttributeMap contextParams = clientConfiguration.option(SdkClientOption.CLIENT_CONTEXT_PARAMS);
			assertThat(contextParams.get(S3ClientContextParams.FORCE_PATH_STYLE)).isTrue();
		});
	}

	@Test
	void setsPropertiesOnClient() {
		contextRunner.withPropertyValues("spring.cloud.aws.s3.crt.minimum-part-size-in-bytes=50",
				"spring.cloud.aws.s3.crt.initial-read-buffer-size-in-bytes=150",
				"spring.cloud.aws.s3.crt.max-concurrency=20", "spring.cloud.aws.s3.crt.target-throughput-in-gbps=100")
				.run(context -> {
					S3AsyncClient bean = context.getBean(S3AsyncClient.class);
					S3NativeClientConfiguration s3NativeClientConfiguration = s3NativeClientConfiguration(bean);
					assertThat(s3NativeClientConfiguration.partSizeBytes()).isEqualTo(50);
					assertThat(s3NativeClientConfiguration.readBufferSizeInBytes()).isEqualTo(150);
					assertThat(s3NativeClientConfiguration.targetThroughputInGbps()).isEqualTo(100);
					assertThat(s3NativeClientConfiguration.maxConcurrency()).isEqualTo(20);
				});
	}

	@Test
	void handlesMissingS3AsyncClient() {
		contextRunner.withClassLoader(new FilteredClassLoader(S3AsyncClient.class)).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(S3AsyncClient.class);
		});
	}

	@Test
	void handlesMissingS3CrtLibrary() {
		contextRunner.withClassLoader(new FilteredClassLoader(AwsCrtHttpClient.class)).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(S3AsyncClient.class);
		});
	}

	private static S3NativeClientConfiguration s3NativeClientConfiguration(S3AsyncClient client) {
		ConfiguredAwsClient configuredClient = new ConfiguredAwsClient(client);
		SdkAsyncHttpClient sdkAsyncHttpClient = configuredClient.getAsyncHttpClient();
		return (S3NativeClientConfiguration) ReflectionTestUtils.getField(sdkAsyncHttpClient,
				"s3NativeClientConfiguration");
	}

}
