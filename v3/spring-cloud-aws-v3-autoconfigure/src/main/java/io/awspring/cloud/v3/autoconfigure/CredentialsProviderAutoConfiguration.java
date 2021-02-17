/*
 * Copyright 2013-2021 the original author or authors.
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

package io.awspring.cloud.v3.autoconfigure;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.awspring.cloud.v3.autoconfigure.properties.AwsCredentialsProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration} for {@link AwsCredentialsProvider}.
 *
 * @author Maciej Walkowiak
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsCredentialsProperties.class)
public class CredentialsProviderAutoConfiguration {

	private final AwsCredentialsProperties properties;

	public CredentialsProviderAutoConfiguration(AwsCredentialsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public AwsCredentialsProvider awsCredentialsProvider() {
		final List<AwsCredentialsProvider> providers = new ArrayList<>();

		if (StringUtils.hasText(properties.getAccessKey()) && StringUtils.hasText(properties.getSecretKey())) {
			providers.add(StaticCredentialsProvider
					.create(AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())));
		}

		if (properties.isInstanceProfile()) {
			providers.add(InstanceProfileCredentialsProvider.create());
		}

		if (properties.getProfile() != null && properties.getProfile().getName() != null) {
			providers.add(ProfileCredentialsProvider.builder().profileName(properties.getProfile().getName())
					.profileFile(properties.getProfile().getPath() != null
							? ProfileFile.builder().type(ProfileFile.Type.CREDENTIALS)
									.content(Paths.get(properties.getProfile().getPath())).build()
							: ProfileFile.defaultProfileFile())
					.build());
		}

		if (providers.isEmpty()) {
			return DefaultCredentialsProvider.create();
		}
		else {
			return AwsCredentialsProviderChain.builder().credentialsProviders(providers).build();
		}
	}

}
