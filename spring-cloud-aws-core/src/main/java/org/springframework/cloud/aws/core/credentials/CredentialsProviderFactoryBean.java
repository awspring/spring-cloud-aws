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

import java.util.Collections;
import java.util.List;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that creates a composite
 * {@link AWSCredentialsProvider} based on the delegates.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class CredentialsProviderFactoryBean
		extends AbstractFactoryBean<AWSCredentialsProvider> {

	/**
	 * Name of the credentials provider bean.
	 */
	public static final String CREDENTIALS_PROVIDER_BEAN_NAME = "credentialsProvider";

	private final List<AWSCredentialsProvider> delegates;

	public CredentialsProviderFactoryBean() {
		this(Collections.emptyList());
	}

	public CredentialsProviderFactoryBean(List<AWSCredentialsProvider> delegates) {
		Assert.notNull(delegates, "Delegates must not be null");
		this.delegates = delegates;
	}

	@Override
	public Class<?> getObjectType() {
		return AWSCredentialsProvider.class;
	}

	@Override
	protected AWSCredentialsProvider createInstance() throws Exception {
		AWSCredentialsProviderChain awsCredentialsProviderChain;
		if (this.delegates.isEmpty()) {
			awsCredentialsProviderChain = new DefaultAWSCredentialsProviderChain();
		}
		else {
			awsCredentialsProviderChain = new AWSCredentialsProviderChain(this.delegates
					.toArray(new AWSCredentialsProvider[this.delegates.size()]));
		}

		awsCredentialsProviderChain.setReuseLastProvider(false);
		return awsCredentialsProviderChain;
	}

}
