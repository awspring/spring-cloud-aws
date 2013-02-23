/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.context.config.xml;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.STSSessionCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

/**
 * Tests for {@link CredentialsBeanDefinitionParser}
 *
 * @author Agim Emruli
 */
public class CredentialsBeanDefinitionParserTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testCreateBeanDefinition() throws Exception {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		//Check that the result of the factory bean is available
		AWSCredentialsProvider awsCredentialsProvider = applicationContext.getBean(AWSCredentialsProvider.class);

		Assert.assertTrue(AWSCredentialsProviderChain.class.isInstance(awsCredentialsProvider));

		// Using reflection to really test if the chain is stable
		AWSCredentialsProviderChain awsCredentialsProviderChain = (AWSCredentialsProviderChain) awsCredentialsProvider;

		@SuppressWarnings("unchecked") List<AWSCredentialsProvider> providerChain = (List<AWSCredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProviderChain, "credentialsProviders");

		Assert.assertNotNull(providerChain);
		Assert.assertEquals(3, providerChain.size());

		Assert.assertTrue(InstanceProfileCredentialsProvider.class.isInstance(providerChain.get(0)));
		Assert.assertTrue(STSSessionCredentialsProvider.class.isInstance(providerChain.get(1)));
		Assert.assertTrue(StaticCredentialsProvider.class.isInstance(providerChain.get(2)));

		StaticCredentialsProvider staticCredentialsProvider = (StaticCredentialsProvider) providerChain.get(2);
		Assert.assertEquals("staticAccessKey", staticCredentialsProvider.getCredentials().getAWSAccessKeyId());
		Assert.assertEquals("staticSecretKey", staticCredentialsProvider.getCredentials().getAWSSecretKey());

	}

	@Test
	public void testMultipleElements() throws Exception {
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("only allowed once per");

		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-testMultipleElements.xml", getClass());
	}

	@Test
	public void testWithEmptyAccessKey() throws Exception {
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("The 'access-key' attribute must not be empty");
		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-testWithEmptyAccessKey.xml", getClass());
	}

	@Test
	public void testWithEmptySecretKey() throws Exception {
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("The 'secret-key' attribute must not be empty");
		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-testWithEmptySecretKey.xml", getClass());
	}

	@Test
	public void testWithPlaceHolder() throws Exception {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-testWithPlaceHolder.xml", getClass());

		AWSCredentialsProvider awsCredentialsProvider = applicationContext.getBean(AWSCredentialsProvider.class);
		AWSCredentials credentials = awsCredentialsProvider.getCredentials();
		Assert.assertEquals("foo", credentials.getAWSAccessKeyId());
		Assert.assertEquals("bar", credentials.getAWSSecretKey());
	}

	@Test
	public void testWithExpressions() throws Exception {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-testWithExpressions.xml", getClass());

		AWSCredentialsProvider awsCredentialsProvider = applicationContext.getBean(AWSCredentialsProvider.class);
		AWSCredentials credentials = awsCredentialsProvider.getCredentials();
		Assert.assertEquals("foo", credentials.getAWSAccessKeyId());
		Assert.assertEquals("bar", credentials.getAWSSecretKey());
	}
}