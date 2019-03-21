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

package org.springframework.cloud.aws.core.credentials;

import java.util.Arrays;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for
 * {@link org.springframework.cloud.aws.core.credentials.CredentialsProviderFactoryBean}.
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
		// noinspection ResultOfObjectAllocationIgnored
		new CredentialsProviderFactoryBean(null);
	}

	@Test
	public void getObject_withZeroConfiguredProviders_returnsDefaultAwsCredentialsProviderChain()
			throws Exception {
		// Arrange
		CredentialsProviderFactoryBean credentialsProviderFactoryBean = new CredentialsProviderFactoryBean();
		credentialsProviderFactoryBean.afterPropertiesSet();

		// Act
		AWSCredentialsProvider credentialsProvider = credentialsProviderFactoryBean
				.getObject();

		// Assert
		assertThat(credentialsProvider).isNotNull();
		assertThat(
				DefaultAWSCredentialsProviderChain.class.isInstance(credentialsProvider))
						.isTrue();
	}

	@Test
	public void testCreateWithMultiple() throws Exception {
		AWSCredentialsProvider first = mock(AWSCredentialsProvider.class);
		AWSCredentialsProvider second = mock(AWSCredentialsProvider.class);

		CredentialsProviderFactoryBean credentialsProviderFactoryBean = new CredentialsProviderFactoryBean(
				Arrays.asList(first, second));
		credentialsProviderFactoryBean.afterPropertiesSet();

		AWSCredentialsProvider provider = credentialsProviderFactoryBean.getObject();

		BasicAWSCredentials foo = new BasicAWSCredentials("foo", "foo");
		BasicAWSCredentials bar = new BasicAWSCredentials("bar", "bar");

		when(first.getCredentials()).thenReturn(null, foo);
		when(second.getCredentials()).thenReturn(bar);

		assertThat(provider.getCredentials()).isEqualTo(bar);
		assertThat(provider.getCredentials()).isEqualTo(foo);
	}

}
