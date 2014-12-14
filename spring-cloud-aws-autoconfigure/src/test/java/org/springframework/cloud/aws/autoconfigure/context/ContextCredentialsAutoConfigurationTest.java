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

package org.springframework.cloud.aws.autoconfigure.context;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import org.apache.http.client.CredentialsProvider;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

	@Test
	public void credentialsProvider_noExplicitCredentialsProviderConfigured_configuresDefaultAwsCredentialsProviderChain() throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextCredentialsAutoConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		assertNotNull(this.context.getBean(AWSCredentialsProvider.class));
		assertTrue(DefaultAWSCredentialsProviderChain.class.isInstance(this.context.getBean(AWSCredentialsProvider.class)));
	}

	@Test
	public void credentialsProvider_accessKeyAndSecretKeyConfigured_configuresStaticCredentialsProviderWithAccessAndSecretKey() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextCredentialsAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"cloud.aws.credentials.accessKey:foo",
				"cloud.aws.credentials.secretKey:bar");
		this.context.refresh();
		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME, AWSCredentialsProviderChain.class);
		assertNotNull(awsCredentialsProvider);

		@SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
				(List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
		assertEquals(1, credentialsProviders.size());
		assertTrue(StaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)));

		assertEquals("foo", awsCredentialsProvider.getCredentials().getAWSAccessKeyId());
		assertEquals("bar", awsCredentialsProvider.getCredentials().getAWSSecretKey());

	}

	@Test
	public void credentialsProvider_instanceProfileConfigured_configuresInstanceProfileCredentialsProvider() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextCredentialsAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"cloud.aws.credentials.instanceProfile");
		this.context.refresh();
		AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME, AWSCredentialsProvider.class);
		assertNotNull(awsCredentialsProvider);

		@SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
				(List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
		assertEquals(1, credentialsProviders.size());
		assertTrue(InstanceProfileCredentialsProvider.class.isInstance(credentialsProviders.get(0)));
	}
}