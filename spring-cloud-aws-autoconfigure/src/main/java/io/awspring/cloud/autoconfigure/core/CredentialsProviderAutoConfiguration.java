/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.autoconfigure.core;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;

/**
 * {@link EnableAutoConfiguration} for {@link AwsCredentialsProvider}.
 *
 * @author Maciej Walkowiak
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ AwsCredentialsProvider.class, ProfileFile.class })
@EnableConfigurationProperties(CredentialsProperties.class)
public class CredentialsProviderAutoConfiguration {

	private final CredentialsProperties properties;

	public CredentialsProviderAutoConfiguration(CredentialsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public AwsCredentialsProvider credentialsProvider() {
		return createCredentialsProvider(this.properties);
	}

	public static AwsCredentialsProvider createCredentialsProvider(CredentialsProperties properties) {
		final List<AwsCredentialsProvider> providers = new ArrayList<>();

		if (StringUtils.hasText(properties.getAccessKey()) && StringUtils.hasText(properties.getSecretKey())) {
			providers.add(createStaticCredentialsProvider(properties));
		}

		if (properties.isInstanceProfile()) {
			providers.add(InstanceProfileCredentialsProvider.create());
		}

		Profile profile = properties.getProfile();
		if (profile != null && profile.getName() != null) {
			providers.add(createProfileCredentialProvider(profile));
		}

		if (providers.isEmpty()) {
			return DefaultCredentialsProvider.create();
		}
		else {
			return AwsCredentialsProviderChain.builder().credentialsProviders(providers).build();
		}
	}

	private static StaticCredentialsProvider createStaticCredentialsProvider(CredentialsProperties properties) {
		return StaticCredentialsProvider
				.create(AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
	}

	private static ProfileCredentialsProvider createProfileCredentialProvider(Profile profile) {
		ProfileFile profileFile;
		if (profile.getPath() != null) {
			profileFile = ProfileFile.builder().type(ProfileFile.Type.CREDENTIALS).content(Paths.get(profile.getPath()))
					.build();
		}
		else {
			profileFile = ProfileFile.defaultProfileFile();
		}
		return ProfileCredentialsProvider.builder().profileName(profile.getName()).profileFile(profileFile).build();
	}

}
