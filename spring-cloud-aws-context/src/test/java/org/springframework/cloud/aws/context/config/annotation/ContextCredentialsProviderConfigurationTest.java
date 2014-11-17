/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.context.config.annotation;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import org.apache.http.client.CredentialsProvider;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ContextCredentialsProviderConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void credentialsProvider_defaultCredentialsProviderWithoutFurtherConfig_awsCredentialsProviderConfigured() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithDefaultCredentialsProvider.class);

		//Act
		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

		//Assert
		assertNotNull(awsCredentialsProvider);
		assertTrue(DefaultAWSCredentialsProviderChain.class.isInstance(awsCredentialsProvider));
	}

	@Test
	public void credentialsProvider_configWithAccessAndSecretKey_staticAwsCredentialsProviderConfigured() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithAccessKeyAndSecretKey.class);

		//Act
		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

		//Assert
		assertNotNull(awsCredentialsProvider);

		@SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
				(List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
		assertEquals(1, credentialsProviders.size());
		assertTrue(StaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)));

		assertEquals("accessTest", awsCredentialsProvider.getCredentials().getAWSAccessKeyId());
		assertEquals("testSecret", awsCredentialsProvider.getCredentials().getAWSSecretKey());
	}

	@Test
	public void credentialsProvider_configWithAccessAndSecretKeyAndInstanceProfile_staticAwsCredentialsProviderConfiguredWithInstanceProfile() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithAccessKeyAndSecretKeyAndInstanceProfile.class);

		//Act
		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

		//Assert
		assertNotNull(awsCredentialsProvider);

		@SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
				(List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
		assertEquals(2, credentialsProviders.size());
		assertTrue(StaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)));
		assertTrue(InstanceProfileCredentialsProvider.class.isInstance(credentialsProviders.get(1)));
	}

	@Test
	public void credentialsProvider_configWithInstanceProfile_instanceProfileCredentialsProviderConfigured() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithInstanceProfileOnly.class);

		//Act
		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

		//Assert
		assertNotNull(awsCredentialsProvider);

		@SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
				(List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
		assertEquals(1, credentialsProviders.size());
		assertTrue(InstanceProfileCredentialsProvider.class.isInstance(credentialsProviders.get(0)));
	}



	@EnableCredentialsProvider
	public static class ApplicationConfigurationWithDefaultCredentialsProvider {
	}

	@EnableCredentialsProvider(accessKey = "accessTest",secretKey = "testSecret")
	public static class ApplicationConfigurationWithAccessKeyAndSecretKey {
	}

	@EnableCredentialsProvider(accessKey = "accessTest",secretKey = "testSecret",instanceProfile = true)
	public static class ApplicationConfigurationWithAccessKeyAndSecretKeyAndInstanceProfile {
	}

	@EnableCredentialsProvider(instanceProfile = true)
	public static class ApplicationConfigurationWithInstanceProfileOnly {
	}
}