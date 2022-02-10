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

package io.awspring.cloud.v3.core.credentials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import io.awspring.cloud.v3.core.credentials.CredentialsProviderFactoryBean;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;

/**
 * Unit tests for
 * {@link io.awspring.cloud.v3.core.credentials.CredentialsProviderFactoryBean}.
 *
 * @author Agim Emruli
 */
class CredentialsProviderFactoryBeanTest {

	@Test
	void testCreateWithNullCredentialsProvider() throws Exception {
		assertThatThrownBy(() -> new CredentialsProviderFactoryBean(null)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not be null");
	}

	@Test
	void getObject_withZeroConfiguredProviders_returnsDefaultAwsCredentialsProviderChain() throws Exception {
		// Arrange
		CredentialsProviderFactoryBean credentialsProviderFactoryBean = new CredentialsProviderFactoryBean();
		credentialsProviderFactoryBean.afterPropertiesSet();

		// Act
		AwsCredentialsProvider credentialsProvider = credentialsProviderFactoryBean.getObject();

		// Assert
		assertThat(credentialsProvider).isNotNull().isInstanceOf(AwsCredentialsProviderChain.class);
	}

	@Test
	void testCreateWithMultiple() throws Exception {
		AwsCredentialsProvider first = mock(AwsCredentialsProvider.class);
		AwsCredentialsProvider second = mock(AwsCredentialsProvider.class);

		CredentialsProviderFactoryBean credentialsProviderFactoryBean = new CredentialsProviderFactoryBean(
				Arrays.asList(first, second));
		credentialsProviderFactoryBean.afterPropertiesSet();

		AwsCredentialsProvider provider = credentialsProviderFactoryBean.getObject();

		AwsCredentials foo = AwsBasicCredentials.create("foo", "foo");
		AwsCredentials bar = AwsBasicCredentials.create("bar", "bar");

		when(first.resolveCredentials())
			.thenThrow(RuntimeException.class)
			.thenReturn(foo);
		when(second.resolveCredentials()).thenReturn(bar);

		assertThat(provider.resolveCredentials()).isEqualTo(bar);
		assertThat(provider.resolveCredentials()).isEqualTo(foo);
	}

}
