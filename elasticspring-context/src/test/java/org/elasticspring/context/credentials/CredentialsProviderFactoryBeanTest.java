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

package org.elasticspring.context.credentials;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for {@link CredentialsProviderFactoryBean}
 *
 * @author Agim Emruli
 */
public class CredentialsProviderFactoryBeanTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testCreateWithNullCredentialsProvider() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("not be null");
		//noinspection ResultOfObjectAllocationIgnored
		new CredentialsProviderFactoryBean(null);
	}


	@Test
	public void testCreateWithZeroProviders() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("No credential providers specified");
		//noinspection ResultOfObjectAllocationIgnored
		CredentialsProviderFactoryBean credentialsProviderFactoryBean = new CredentialsProviderFactoryBean(Collections.<AWSCredentialsProvider>emptyList());
		credentialsProviderFactoryBean.afterPropertiesSet();
	}


	@Test
	public void testCreateWithMultiple() throws Exception {
		AWSCredentialsProvider first = Mockito.mock(AWSCredentialsProvider.class);
		AWSCredentialsProvider second = Mockito.mock(AWSCredentialsProvider.class);

		CredentialsProviderFactoryBean credentialsProviderFactoryBean = new CredentialsProviderFactoryBean(Arrays.asList(first, second));
		credentialsProviderFactoryBean.afterPropertiesSet();

		AWSCredentialsProvider provider = credentialsProviderFactoryBean.getObject();

		BasicAWSCredentials foo = new BasicAWSCredentials("foo", "foo");
		BasicAWSCredentials bar = new BasicAWSCredentials("bar", "bar");

		Mockito.when(first.getCredentials()).thenReturn(null, foo);
		Mockito.when(second.getCredentials()).thenReturn(bar);

		Assert.assertEquals(bar, provider.getCredentials());
		Assert.assertEquals(foo, provider.getCredentials());
	}
}