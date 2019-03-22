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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import org.apache.http.client.CredentialsProvider;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Agim Emruli
 */
public class ContextCredentialsAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	// @checkstyle:off
	@Test
	public void credentialsProvider_noExplicitCredentialsProviderConfigured_configuresDefaultAwsCredentialsProviderChainWithInstanceProfile()
			throws Exception {
		// @checkstyle:on
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextCredentialsAutoConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(
				AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
				AWSCredentialsProvider.class);
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<CredentialsProvider> credentialsProviders = (List<CredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(2);
		assertThat(EC2ContainerCredentialsProviderWrapper.class
				.isInstance(credentialsProviders.get(0))).isTrue();
		assertThat(
				ProfileCredentialsProvider.class.isInstance(credentialsProviders.get(1)))
						.isTrue();
	}

	// @checkstyle:off
	@Test
	public void credentialsProvider_propertyToUseDefaultIsSet_configuresDefaultAwsCredentialsProvider_existAccessKeyAndSecretKey() {
		// @checkstyle:on
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextCredentialsAutoConfiguration.class);
		TestPropertyValues
				.of("cloud.aws.credentials.accessKey:testAccessKey",
						"cloud.aws.credentials.secretKey:testSecretKey",
						"cloud.aws.credentials.useDefaultAwsCredentialsChain:true")
				.applyTo(this.context);
		this.context.refresh();

		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(
				AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
				AWSCredentialsProvider.class);
		assertThat(awsCredentialsProvider).isNotNull();

		assertThat(awsCredentialsProvider.getCredentials().getAWSAccessKeyId())
				.isEqualTo("testAccessKey");
		assertThat(awsCredentialsProvider.getCredentials().getAWSSecretKey())
				.isEqualTo("testSecretKey");
	}

	@Test
	public void credentialsProvider_propertyToUseDefaultIsSet_configuresDefaultAwsCredentialsProvider() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextCredentialsAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.credentials.useDefaultAwsCredentialsChain:true")
				.applyTo(this.context);
		this.context.refresh();

		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(
				AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
				AWSCredentialsProvider.class);
		assertThat(awsCredentialsProvider).isNotNull();

		assertThat(awsCredentialsProvider.getClass()
				.isAssignableFrom(DefaultAWSCredentialsProviderChain.class)).isTrue();
	}

	// @checkstyle:off
	@Test
	public void credentialsProvider_accessKeyAndSecretKeyConfigured_configuresStaticCredentialsProviderWithAccessAndSecretKey() {
		// @checkstyle:on
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextCredentialsAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.credentials.accessKey:foo",
				"cloud.aws.credentials.secretKey:bar").applyTo(this.context);
		this.context.refresh();
		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(
				AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
				AWSCredentialsProviderChain.class);
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<CredentialsProvider> credentialsProviders = (List<CredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(2);
		assertThat(AWSStaticCredentialsProvider.class
				.isInstance(credentialsProviders.get(0))).isTrue();
		assertThat(
				ProfileCredentialsProvider.class.isInstance(credentialsProviders.get(1)))
						.isTrue();

		assertThat(awsCredentialsProvider.getCredentials().getAWSAccessKeyId())
				.isEqualTo("foo");
		assertThat(awsCredentialsProvider.getCredentials().getAWSSecretKey())
				.isEqualTo("bar");

	}

	@Test
	public void credentialsProvider_instanceProfileConfigured_configuresInstanceProfileCredentialsProvider() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextCredentialsAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.credentials.instanceProfile")
				.applyTo(this.context);
		this.context.refresh();
		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(
				AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
				AWSCredentialsProvider.class);
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<CredentialsProvider> credentialsProviders = (List<CredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(2);
		assertThat(EC2ContainerCredentialsProviderWrapper.class
				.isInstance(credentialsProviders.get(0))).isTrue();
		assertThat(
				ProfileCredentialsProvider.class.isInstance(credentialsProviders.get(1)))
						.isTrue();
	}

	@Test
	public void credentialsProvider_profileNameConfigured_configuresProfileCredentialsProvider() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextCredentialsAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.credentials.profileName:test")
				.applyTo(this.context);
		this.context.refresh();
		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(
				AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
				AWSCredentialsProvider.class);
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<CredentialsProvider> credentialsProviders = (List<CredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(2);
		assertThat(EC2ContainerCredentialsProviderWrapper.class
				.isInstance(credentialsProviders.get(0))).isTrue();
		assertThat(
				ProfileCredentialsProvider.class.isInstance(credentialsProviders.get(1)))
						.isTrue();

		assertThat(
				ReflectionTestUtils.getField(credentialsProviders.get(1), "profileName"))
						.isEqualTo("test");
	}

	@Test
	public void credentialsProvider_profileNameAndPathConfigured_configuresProfileCredentialsProvider()
			throws IOException {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextCredentialsAutoConfiguration.class);
		TestPropertyValues
				.of("cloud.aws.credentials.profileName:customProfile",
						"cloud.aws.credentials.profilePath:" + new ClassPathResource(
								getClass().getSimpleName() + "-profile", getClass())
										.getFile().getAbsolutePath())
				.applyTo(this.context);
		this.context.refresh();
		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(
				AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
				AWSCredentialsProvider.class);
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<CredentialsProvider> credentialsProviders = (List<CredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(2);
		assertThat(EC2ContainerCredentialsProviderWrapper.class
				.isInstance(credentialsProviders.get(0))).isTrue();
		assertThat(
				ProfileCredentialsProvider.class.isInstance(credentialsProviders.get(1)))
						.isTrue();

		ProfileCredentialsProvider provider = (ProfileCredentialsProvider) credentialsProviders
				.get(1);
		assertThat(provider.getCredentials().getAWSAccessKeyId())
				.isEqualTo("testAccessKey");
		assertThat(provider.getCredentials().getAWSSecretKey())
				.isEqualTo("testSecretKey");
	}

}
