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

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.autoconfigure.context.properties.AwsCredentialsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME;

/**
 * {@link EnableAutoConfiguration} for {@link AWSCredentialsProvider}.
 *
 * @author Agim Emruli
 * @author Maciej Walkowiak
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsCredentialsProperties.class)
@ConditionalOnClass(com.amazonaws.auth.AWSCredentialsProvider.class)
public class ContextCredentialsAutoConfiguration {

	@Bean(name = CREDENTIALS_PROVIDER_BEAN_NAME)
	@ConditionalOnMissingBean(name = CREDENTIALS_PROVIDER_BEAN_NAME)
	public AWSCredentialsProvider awsCredentialsProvider(
			AwsCredentialsProperties properties) {

		List<AWSCredentialsProvider> providers = resolveCredentialsProviders(properties);

		if (providers.isEmpty()) {
			return new DefaultAWSCredentialsProviderChain();
		}
		else {
			return new AWSCredentialsProviderChain(providers);
		}
	}

	private List<AWSCredentialsProvider> resolveCredentialsProviders(
			AwsCredentialsProperties properties) {
		List<AWSCredentialsProvider> providers = new ArrayList<>();

		if (StringUtils.hasText(properties.getAccessKey())
				&& StringUtils.hasText(properties.getSecretKey())) {
			providers.add(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
					properties.getAccessKey(), properties.getSecretKey())));
		}

		if (properties.isInstanceProfile()) {
			providers.add(new EC2ContainerCredentialsProviderWrapper());
		}

		if (properties.getProfileName() != null) {
			providers.add(properties.getProfilePath() != null
					? new ProfileCredentialsProvider(properties.getProfilePath(),
							properties.getProfileName())
					: new ProfileCredentialsProvider(properties.getProfileName()));
		}

		return providers;
	}

}
