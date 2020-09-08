/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.context;

import java.io.IOException;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import org.apache.http.client.CredentialsProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.aws.core.credentials.CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME;

/**
 * Tests for {@link ContextCredentialsAutoConfiguration}.
 *
 * @author Agim Emruli
 * @author Maciej Walkowiak
 */
class ContextCredentialsAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(ContextCredentialsAutoConfiguration.class));

	// @checkstyle:off
	@Test
	void credentialsProvider_noExplicitCredentialsProviderConfigured_configuresDefaultAwsCredentialsProviderChain() {
		// @checkstyle:on
		this.contextRunner.run((context) -> {
			AWSCredentialsProvider awsCredentialsProvider = context.getBean(
					AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
					AWSCredentialsProvider.class);
			assertThat(awsCredentialsProvider).isNotNull()
					.isInstanceOf(DefaultAWSCredentialsProviderChain.class);
		});

	}

	@Test
	void credentialsProvider_propertyToUseDefaultIsSet_configuresDefaultAwsCredentialsProvider() {
		this.contextRunner
				.withPropertyValues(
						"cloud.aws.credentials.use-default-aws-credentials-chain:true")
				.run((context -> {
					AWSCredentialsProvider awsCredentialsProvider = context.getBean(
							AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
							AWSCredentialsProvider.class);
					assertThat(awsCredentialsProvider).isNotNull();

					assertThat(awsCredentialsProvider)
							.isInstanceOf(DefaultAWSCredentialsProviderChain.class);
				}));
	}

	// @checkstyle:off
	@Test
	void credentialsProvider_accessKeyAndSecretKeyConfigured_configuresStaticCredentialsProviderWithAccessAndSecretKey() {
		// @checkstyle:on
		this.contextRunner.withPropertyValues(
				"cloud.aws.credentials.use-default-aws-credentials-chain:false",
				"cloud.aws.credentials.accessKey:foo",
				"cloud.aws.credentials.secretKey:bar").run((context) -> {
					AWSCredentialsProvider awsCredentialsProvider = context.getBean(
							AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
							AWSCredentialsProviderChain.class);
					assertThat(awsCredentialsProvider).isNotNull();
					assertThat(
							awsCredentialsProvider.getCredentials().getAWSAccessKeyId())
									.isEqualTo("foo");
					assertThat(awsCredentialsProvider.getCredentials().getAWSSecretKey())
							.isEqualTo("bar");

					@SuppressWarnings("unchecked")
					List<CredentialsProvider> credentialsProviders = (List<CredentialsProvider>) ReflectionTestUtils
							.getField(awsCredentialsProvider, "credentialsProviders");
					assertThat(credentialsProviders).hasSize(1)
							.hasOnlyElementsOfType(AWSStaticCredentialsProvider.class);
				});
	}

	@Test
	void credentialsProvider_instanceProfileConfigured_configuresInstanceProfileCredentialsProvider() {
		this.contextRunner.withPropertyValues(
				"cloud.aws.credentials.use-default-aws-credentials-chain:false",
				"cloud.aws.credentials.instance-profile:true").run((context) -> {
					AWSCredentialsProvider awsCredentialsProvider = context.getBean(
							AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
							AWSCredentialsProvider.class);
					assertThat(awsCredentialsProvider).isNotNull();

					@SuppressWarnings("unchecked")
					List<CredentialsProvider> credentialsProviders = (List<CredentialsProvider>) ReflectionTestUtils
							.getField(awsCredentialsProvider, "credentialsProviders");
					assertThat(credentialsProviders).hasSize(1).hasOnlyElementsOfType(
							EC2ContainerCredentialsProviderWrapper.class);
				});
	}

	@Test
	void credentialsProvider_profileNameConfigured_configuresProfileCredentialsProvider() {
		this.contextRunner.withPropertyValues(
				"cloud.aws.credentials.use-default-aws-credentials-chain:false",
				"cloud.aws.credentials.profile-name:test").run((context) -> {
					AWSCredentialsProvider awsCredentialsProvider = context.getBean(
							AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
							AWSCredentialsProvider.class);
					assertThat(awsCredentialsProvider).isNotNull();

					@SuppressWarnings("unchecked")
					List<CredentialsProvider> credentialsProviders = (List<CredentialsProvider>) ReflectionTestUtils
							.getField(awsCredentialsProvider, "credentialsProviders");
					assertThat(credentialsProviders).hasSize(1)
							.hasOnlyElementsOfType(ProfileCredentialsProvider.class);
					assertThat(ReflectionTestUtils.getField(credentialsProviders.get(0),
							"profileName")).isEqualTo("test");
				});
	}

	@Test
	void credentialsProvider_profileNameAndPathConfigured_configuresProfileCredentialsProvider()
			throws IOException {
		this.contextRunner
				.withPropertyValues(
						"cloud.aws.credentials.use-default-aws-credentials-chain:false",
						"cloud.aws.credentials.profileName:customProfile",
						"cloud.aws.credentials.profilePath:" + new ClassPathResource(
								getClass().getSimpleName() + "-profile", getClass())
										.getFile().getAbsolutePath())
				.run((context) -> {
					AWSCredentialsProvider awsCredentialsProvider = context.getBean(
							AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
							AWSCredentialsProvider.class);
					assertThat(awsCredentialsProvider).isNotNull();

					@SuppressWarnings("unchecked")
					List<CredentialsProvider> credentialsProviders = (List<CredentialsProvider>) ReflectionTestUtils
							.getField(awsCredentialsProvider, "credentialsProviders");
					assertThat(credentialsProviders).hasSize(1)
							.hasOnlyElementsOfType(ProfileCredentialsProvider.class);

					ProfileCredentialsProvider provider = (ProfileCredentialsProvider) credentialsProviders
							.get(0);
					assertThat(provider.getCredentials().getAWSAccessKeyId())
							.isEqualTo("testAccessKey");
					assertThat(provider.getCredentials().getAWSSecretKey())
							.isEqualTo("testSecretKey");
				});
	}

	@Test
	void credentialsProvider_customCredentialsConfigured_customCredentialsAreUsed() {
		// @checkstyle:on
		this.contextRunner
				.withUserConfiguration(CustomCredentialsProviderConfiguration.class)
				.run((context) -> {
					AWSCredentialsProvider awsCredentialsProvider = context
							.getBean(AWSCredentialsProvider.class);
					assertThat(awsCredentialsProvider).isNotNull()
							.isInstanceOf(CustomAWSCredentialsProvider.class);
				});

	}

	@Configuration
	static class CustomCredentialsProviderConfiguration {

		@Bean(name = CREDENTIALS_PROVIDER_BEAN_NAME)
		public AWSCredentialsProvider customAwsCredentialsProvider() {
			return new CustomAWSCredentialsProvider();
		}

	}

	static class CustomAWSCredentialsProvider implements AWSCredentialsProvider {

		@Override
		public AWSCredentials getCredentials() {
			return null;
		}

		@Override
		public void refresh() {

		}

	}

}
