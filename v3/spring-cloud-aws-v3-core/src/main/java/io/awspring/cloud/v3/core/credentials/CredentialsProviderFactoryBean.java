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

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that creates a composite
 * {@link AwsCredentialsProvider} based on the delegates.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class CredentialsProviderFactoryBean extends AbstractFactoryBean<AwsCredentialsProvider> {

	/**
	 * Name of the credentials provider bean.
	 */
	public static final String CREDENTIALS_PROVIDER_BEAN_NAME = "credentialsProvider";

	private final List<AwsCredentialsProvider> delegates;

	public CredentialsProviderFactoryBean() {
		this(Collections.emptyList());
	}

	public CredentialsProviderFactoryBean(List<AwsCredentialsProvider> delegates) {
		Assert.notNull(delegates, "Delegates must not be null");
		this.delegates = delegates;
	}

	@Override
	public Class<?> getObjectType() {
		return AwsCredentialsProvider.class;
	}

	@Override
	protected AwsCredentialsProvider createInstance() throws Exception {
		AwsCredentialsProviderChain.Builder builder = AwsCredentialsProviderChain.builder()
			.reuseLastProviderEnabled(false);

		if (CollectionUtils.isEmpty(delegates)) {
			return builder.credentialsProviders(DefaultCredentialsProvider.create())
				.build();
		}
		return builder.credentialsProviders(this.delegates)
			.build();
	}

}
