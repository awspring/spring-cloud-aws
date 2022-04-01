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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

class CredentialsProviderAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CredentialsProviderAutoConfiguration.class));

	// @checkstyle:off
	@Test
	void credentialsProvider_noExplicitCredentialsProviderConfigured_configuresDefaultAwsCredentialsProviderChain() {
		// @checkstyle:on
		this.contextRunner.run((context) -> {
			AwsCredentialsProvider awsCredentialsProvider = context.getBean("credentialsProvider",
					AwsCredentialsProvider.class);
			assertThat(awsCredentialsProvider).isNotNull().isInstanceOf(DefaultCredentialsProvider.class);
		});

	}

	// @checkstyle:off
	@Test
	void credentialsProvider_accessKeyAndSecretKeyConfigured_configuresStaticCredentialsProviderWithAccessAndSecretKey() {
		// @checkstyle:on
		this.contextRunner.withPropertyValues("spring.cloud.aws.credentials.accessKey:foo",
				"spring.cloud.aws.credentials.secretKey:bar").run((context) -> {
					AwsCredentialsProvider awsCredentialsProvider = context.getBean("credentialsProvider",
							AwsCredentialsProvider.class);
					assertThat(awsCredentialsProvider).isNotNull();
					assertThat(awsCredentialsProvider.resolveCredentials().accessKeyId()).isEqualTo("foo");
					assertThat(awsCredentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("bar");

					@SuppressWarnings("unchecked")
					List<AwsCredentialsProvider> credentialsProviders = (List<AwsCredentialsProvider>) ReflectionTestUtils
							.getField(awsCredentialsProvider, "credentialsProviders");
					assertThat(credentialsProviders).hasSize(1).hasOnlyElementsOfType(StaticCredentialsProvider.class);
				});
	}

	@Test
	void credentialsProvider_instanceProfileConfigured_configuresInstanceProfileCredentialsProvider() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.credentials.instance-profile:true").run((context) -> {
			AwsCredentialsProvider awsCredentialsProvider = context.getBean("credentialsProvider",
					AwsCredentialsProvider.class);
			assertThat(awsCredentialsProvider).isNotNull();

			@SuppressWarnings("unchecked")
			List<AwsCredentialsProvider> credentialsProviders = (List<AwsCredentialsProvider>) ReflectionTestUtils
					.getField(awsCredentialsProvider, "credentialsProviders");
			assertThat(credentialsProviders).hasSize(1).hasOnlyElementsOfType(InstanceProfileCredentialsProvider.class);
		});
	}

	@Test
	void credentialsProvider_profileNameAndPathConfigured_configuresProfileCredentialsProvider() throws IOException {
		this.contextRunner.withPropertyValues("spring.cloud.aws.credentials.profile.name:customProfile",
				"spring.cloud.aws.credentials.profile.path:"
						+ new ClassPathResource(getClass().getSimpleName() + "-profile", getClass()).getFile()
								.getAbsolutePath())
				.run((context) -> {
					AwsCredentialsProvider awsCredentialsProvider = context.getBean("credentialsProvider",
							AwsCredentialsProvider.class);
					assertThat(awsCredentialsProvider).isNotNull();

					@SuppressWarnings("unchecked")
					List<AwsCredentialsProvider> credentialsProviders = (List<AwsCredentialsProvider>) ReflectionTestUtils
							.getField(awsCredentialsProvider, "credentialsProviders");
					assertThat(credentialsProviders).hasSize(1).hasOnlyElementsOfType(ProfileCredentialsProvider.class);

					ProfileCredentialsProvider provider = (ProfileCredentialsProvider) credentialsProviders.get(0);
					assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("testAccessKey");
					assertThat(provider.resolveCredentials().secretAccessKey()).isEqualTo("testSecretKey");
				});
	}

	@Test
	void credentialsProvider_customCredentialsConfigured_customCredentialsAreUsed() {
		// @checkstyle:on
		this.contextRunner.withUserConfiguration(CustomCredentialsProviderConfiguration.class).run((context) -> {
			AwsCredentialsProvider awsCredentialsProvider = context.getBean(AwsCredentialsProvider.class);
			assertThat(awsCredentialsProvider).isNotNull().isInstanceOf(CustomAWSCredentialsProvider.class);
		});

	}

	@Configuration
	static class CustomCredentialsProviderConfiguration {

		@Bean(name = "credentialsProvider")
		public AwsCredentialsProvider customAwsCredentialsProvider() {
			return new CustomAWSCredentialsProvider();
		}

	}

	static class CustomAWSCredentialsProvider implements AwsCredentialsProvider {

		@Override
		public AwsCredentials resolveCredentials() {
			return null;
		}

	}

}
