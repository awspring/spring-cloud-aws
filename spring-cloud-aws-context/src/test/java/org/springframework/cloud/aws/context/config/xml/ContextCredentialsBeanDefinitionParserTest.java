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

package org.springframework.cloud.aws.context.config.xml;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import org.apache.http.client.CredentialsProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextCredentialsBeanDefinitionParser}.
 *
 * @author Agim Emruli
 */
public class ContextCredentialsBeanDefinitionParserTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testCreateBeanDefinition() throws Exception {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-context.xml", getClass());

		// Check that the result of the factory bean is available
		AWSCredentialsProvider awsCredentialsProvider = applicationContext
				.getBean(AWSCredentialsProvider.class);

		assertThat(AWSCredentialsProviderChain.class.isInstance(awsCredentialsProvider))
				.isTrue();

		// Using reflection to really test if the chain is stable
		AWSCredentialsProviderChain awsCredentialsProviderChain = (AWSCredentialsProviderChain) awsCredentialsProvider;

		@SuppressWarnings("unchecked")
		List<AWSCredentialsProvider> providerChain = (List<AWSCredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProviderChain, "credentialsProviders");

		assertThat(providerChain).isNotNull();
		assertThat(providerChain.size()).isEqualTo(2);

		assertThat(
				InstanceProfileCredentialsProvider.class.isInstance(providerChain.get(0)))
						.isTrue();
		assertThat(AWSStaticCredentialsProvider.class.isInstance(providerChain.get(1)))
				.isTrue();

		AWSStaticCredentialsProvider staticCredentialsProvider = (AWSStaticCredentialsProvider) providerChain
				.get(1);
		assertThat(staticCredentialsProvider.getCredentials().getAWSAccessKeyId())
				.isEqualTo("staticAccessKey");
		assertThat(staticCredentialsProvider.getCredentials().getAWSSecretKey())
				.isEqualTo("staticSecretKey");

	}

	@Test
	public void testMultipleElements() throws Exception {
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("only allowed once per");

		// noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-testMultipleElements.xml", getClass());
	}

	@Test
	public void testWithEmptyAccessKey() throws Exception {
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException
				.expectMessage("The 'access-key' attribute must not be empty");
		// noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-testWithEmptyAccessKey.xml", getClass());
	}

	@Test
	public void testWithEmptySecretKey() throws Exception {
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException
				.expectMessage("The 'secret-key' attribute must not be empty");
		// noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-testWithEmptySecretKey.xml", getClass());
	}

	@Test
	public void testWithPlaceHolder() throws Exception {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-testWithPlaceHolder.xml", getClass());

		AWSCredentialsProvider awsCredentialsProvider = applicationContext
				.getBean(AWSCredentialsProvider.class);
		AWSCredentials credentials = awsCredentialsProvider.getCredentials();
		assertThat(credentials.getAWSAccessKeyId()).isEqualTo("foo");
		assertThat(credentials.getAWSSecretKey()).isEqualTo("bar");
	}

	@Test
	public void testWithExpressions() throws Exception {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-testWithExpressions.xml", getClass());

		AWSCredentialsProvider awsCredentialsProvider = applicationContext
				.getBean(AWSCredentialsProvider.class);
		AWSCredentials credentials = awsCredentialsProvider.getCredentials();
		assertThat(credentials.getAWSAccessKeyId()).isEqualTo("foo");
		assertThat(credentials.getAWSSecretKey()).isEqualTo("bar");
	}

	@Test
	public void parseBean_withProfileCredentialsProvider_createProfileCredentialsProvider() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-profileCredentialsProvider.xml",
				getClass());

		AWSCredentialsProvider awsCredentialsProvider = applicationContext.getBean(
				AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
				AWSCredentialsProvider.class);
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<CredentialsProvider> credentialsProviders = (List<CredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(1);
		assertThat(
				ProfileCredentialsProvider.class.isInstance(credentialsProviders.get(0)))
						.isTrue();

		assertThat(
				ReflectionTestUtils.getField(credentialsProviders.get(0), "profileName"))
						.isEqualTo("test");
	}

	@Test
	public void parseBean_withProfileCredentialsProviderAndProfileFile_createProfileCredentialsProvider()
			throws IOException {
		GenericApplicationContext applicationContext = new GenericApplicationContext();

		Map<String, Object> secretAndAccessKeyMap = new HashMap<>();
		secretAndAccessKeyMap.put("profilePath",
				new ClassPathResource(getClass().getSimpleName() + "-profile", getClass())
						.getFile().getAbsolutePath());

		applicationContext.getEnvironment().getPropertySources()
				.addLast(new MapPropertySource("test", secretAndAccessKeyMap));
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setPropertySources(
				applicationContext.getEnvironment().getPropertySources());

		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-profileCredentialsProviderWithFile.xml",
				getClass()));

		applicationContext.refresh();

		AWSCredentialsProvider provider = applicationContext.getBean(
				AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
				AWSCredentialsProvider.class);
		assertThat(provider).isNotNull();

		assertThat(provider.getCredentials().getAWSAccessKeyId())
				.isEqualTo("testAccessKey");
		assertThat(provider.getCredentials().getAWSSecretKey())
				.isEqualTo("testSecretKey");
	}

}
