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
import static org.mockito.Mockito.mock;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.s3.TransferManagerS3OutputStreamProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.DefaultS3CrtAsyncClient;
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
					CredentialsProviderAutoConfiguration.class, S3CrtAsyncClientAutoConfiguration.class, S3TransferManagerAutoConfiguration.class));

	@Nested
	class TransferManagerTests {
		@Test
		void createsS3TransferManagerBeanWithCrtClientWhenInClassPath() {
			contextRunner.run(context -> {
				assertThat(context).hasSingleBean(S3TransferManager.class);
				ConfiguredTransferManager configuredTransferManager = new ConfiguredTransferManager(
						context.getBean(S3TransferManager.class));
				assertThat(configuredTransferManager.getClient()).isInstanceOf(DefaultS3CrtAsyncClient.class);
			});
		}

		@Test
		void createsS3TransferManagerBeanWithCustomClient() {
			contextRunner.withBean(S3AsyncClient.class, () -> mock(S3AsyncClient.class)).run(context -> {
				assertThat(context).hasSingleBean(S3TransferManager.class);
				ConfiguredTransferManager configuredTransferManager = new ConfiguredTransferManager(
						context.getBean(S3TransferManager.class));
				assertThat(Mockito.mockingDetails(configuredTransferManager.getClient()).isMock()).isTrue();
			});
		}

		@Test
		void usesExistingS3TransferManagerBeanWhenExists() {
			S3TransferManager customDefinedS3TransferManager = mock(S3TransferManager.class);
			contextRunner.withBean("s3transferManager", S3TransferManager.class, () -> customDefinedS3TransferManager)
					.run(context -> assertThat(context.getBean(S3TransferManager.class))
							.isEqualTo(customDefinedS3TransferManager));
		}

		@Test
		void setsPropertiesOnTransferManager() {
			contextRunner.withBean(S3AsyncClient.class, () -> mock(S3AsyncClient.class))
					.withPropertyValues("spring.cloud.aws.s3.transfer-manager.follow-symbolic-links:true",
							"spring.cloud.aws.s3.transfer-manager.max-depth:12")
					.run(context -> {
						ConfiguredTransferManager transferManager = new ConfiguredTransferManager(
								context.getBean(S3TransferManager.class));
						assertThat(transferManager.getUploadDirectoryFileVisitOption()).isTrue();
						assertThat(transferManager.getMaxDepth()).isEqualTo(12);
					});
		}

		@Test
		void doesNotCreateS3TransferManagerBeanWhenNotInClassPath() {
			contextRunner.withClassLoader(new FilteredClassLoader(S3TransferManager.class)).run(context -> {
				assertThat(context).doesNotHaveBean(S3TransferManager.class);
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

}
