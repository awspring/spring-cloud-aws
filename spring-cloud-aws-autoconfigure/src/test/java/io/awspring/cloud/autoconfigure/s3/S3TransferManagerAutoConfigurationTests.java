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
package io.awspring.cloud.autoconfigure.s3;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.s3.TransferManagerS3OutputStreamProvider;
import java.net.URI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.lang.NonNull;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * Tests for {@link S3TransferManagerAutoConfigurationTests}.
 *
 * @author Maciej Walkowiak
 * @author Anton Perez
 */
class S3TransferManagerAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, S3TransferManagerAutoConfiguration.class));

	@Nested
	class TransferManagerTests {
		@Test
		void createsS3TransferManagerBeanWhenInClassPath() {
			contextRunner.run(context -> assertThat(context).hasSingleBean(S3TransferManager.class));
		}

		@Test
		void usesExistingS3TransferManagerBeanWhenExists() {
			S3TransferManager customDefinedS3TransferManager = Mockito.mock(S3TransferManager.class);
			contextRunner.withBean("s3transferManager", S3TransferManager.class, () -> customDefinedS3TransferManager)
					.run(context -> assertThat(context.getBean(S3TransferManager.class))
							.isEqualTo(customDefinedS3TransferManager));
		}

		@Test
		void doesNotCreateS3TransferManagerBeanWhenNotInClassPath() {
			contextRunner.withClassLoader(new FilteredClassLoader(S3TransferManager.class)).run(context -> {
				assertThat(context).doesNotHaveBean(S3TransferManager.class);
			});
		}
	}

	@Nested
	class TransferManagerEndpointConfigurationTests {
		@Test
		void withCustomEndpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.s3.endpoint:http://localhost:8090").run(context -> {
				S3TransferManager transferManager = context.getBean(S3TransferManager.class);
				ConfiguredAwsClient client = new ConfiguredAwsClient(resolveS3Client(transferManager));
				assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
				assertThat(client.isEndpointOverridden()).isTrue();
			});
		}

		@Test
		void withCustomGlobalEndpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090").run(context -> {
				S3TransferManager transferManager = context.getBean(S3TransferManager.class);
				ConfiguredAwsClient client = new ConfiguredAwsClient(resolveS3Client(transferManager));
				assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
				assertThat(client.isEndpointOverridden()).isTrue();
			});
		}

		@Test
		void withCustomGlobalEndpointAndS3Endpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090",
					"spring.cloud.aws.s3.endpoint:http://localhost:9999").run(context -> {
						S3TransferManager transferManager = context.getBean(S3TransferManager.class);
						ConfiguredAwsClient client = new ConfiguredAwsClient(resolveS3Client(transferManager));
						assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:9999"));
						assertThat(client.isEndpointOverridden()).isTrue();
					});
		}
	}

	@Nested
	class OutputStreamProviderTests {

		@Test
		void whenS3TransferManagerInClassPathCreatesTransferManagerSS3OutputStreamProvider() {
			contextRunner
					.run(context -> assertThat(context).hasSingleBean(TransferManagerS3OutputStreamProvider.class));
		}

		@Test
		void customS3OutputStreamProviderCanBeConfigured() {
			contextRunner
					.withUserConfiguration(S3AutoConfigurationTests.CustomS3OutputStreamProviderConfiguration.class)
					.run(context -> assertThat(context)
							.hasSingleBean(S3AutoConfigurationTests.CustomS3OutputStreamProvider.class));
		}
	}

	@NonNull
	private static S3AsyncClient resolveS3Client(S3TransferManager builder) {
		return (S3AsyncClient) ReflectionTestUtils.getField(ReflectionTestUtils.getField(builder, "s3CrtAsyncClient"),
				"delegate");
	}

}
