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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsWebIdentityTokenFileCredentialsProvider;

/**
 * {@link EnableAutoConfiguration} for {@link AwsCredentialsProvider}.
 *
 * @author Maciej Walkowiak
 * @author Eddú Meléndez
 * @author Eduan Bekker
 */
@AutoConfiguration
@ConditionalOnClass({ AwsCredentialsProvider.class, ProfileFile.class })
@ConditionalOnMissingBean(AwsCredentialsProvider.class)
@EnableConfigurationProperties(CredentialsProperties.class)
public class CredentialsProviderAutoConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(CredentialsProviderAutoConfiguration.class);
	private static final String STS_WEB_IDENTITY_TOKEN_FILE_CREDENTIALS_PROVIDER = "software.amazon.awssdk.services.sts.auth.StsWebIdentityTokenFileCredentialsProvider";

	private final CredentialsProperties properties;
	private final AwsRegionProvider regionProvider;
	private final ObjectProvider<AwsConnectionDetails> connectionDetails;

	public CredentialsProviderAutoConfiguration(CredentialsProperties properties, AwsRegionProvider regionProvider,
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		this.properties = properties;
		this.regionProvider = regionProvider;
		this.connectionDetails = connectionDetails;
	}

	@Bean
	public AwsCredentialsProvider credentialsProvider() {
		return createCredentialsProvider(this.properties, this.regionProvider, this.connectionDetails.getIfAvailable());
	}

	public static AwsCredentialsProvider createCredentialsProvider(CredentialsProperties properties,
			AwsRegionProvider regionProvider) {
		return createCredentialsProvider(properties, regionProvider, null);
	}

	public static AwsCredentialsProvider createCredentialsProvider(CredentialsProperties properties,
			AwsRegionProvider regionProvider, @Nullable AwsConnectionDetails connectionDetails) {
		final List<AwsCredentialsProvider> providers = new ArrayList<>();

		if (connectionDetails != null && StringUtils.hasText(connectionDetails.getAccessKey())
				&& StringUtils.hasText(connectionDetails.getSecretKey())) {
			providers.add(createStaticCredentialsProvider(connectionDetails));
		}

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

		StsProperties sts = properties.getSts();
		if (ClassUtils.isPresent(STS_WEB_IDENTITY_TOKEN_FILE_CREDENTIALS_PROVIDER, null) && sts.getEnableStsWebIdentityCredentialsProvider()) {
			try {
				providers.add(StsCredentialsProviderFactory.create(sts, regionProvider));
			}
			catch (IllegalStateException e) {
				LOGGER.warn(
						"Skipping creating `StsCredentialsProvider`. `software.amazon.awssdk:sts` is on the classpath, but neither `spring.cloud.aws.credentials.sts` properties are configured nor `AWS_WEB_IDENTITY_TOKEN_FILE` or the javaproperty `aws.webIdentityTokenFile` is set");
			}
		}

		if (providers.isEmpty()) {
			return DefaultCredentialsProvider.create();
		}
		else if (providers.size() == 1) {
			return providers.get(0);
		}
		else {
			return AwsCredentialsProviderChain.builder().credentialsProviders(providers).build();
		}
	}

	private static StaticCredentialsProvider createStaticCredentialsProvider(CredentialsProperties properties) {
		return StaticCredentialsProvider
				.create(AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
	}

	private static StaticCredentialsProvider createStaticCredentialsProvider(AwsConnectionDetails connectionDetails) {
		return StaticCredentialsProvider
				.create(AwsBasicCredentials.create(connectionDetails.getAccessKey(), connectionDetails.getSecretKey()));
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

	/**
	 * Wrapper class to avoid {@link NoClassDefFoundError}.
	 */
	private static class StsCredentialsProviderFactory {
		private static AwsCredentialsProvider create(@Nullable StsProperties stsProperties,
				AwsRegionProvider regionProvider) {
			PropertyMapper propertyMapper = PropertyMapper.get();
			StsWebIdentityTokenFileCredentialsProvider.Builder builder = StsWebIdentityTokenFileCredentialsProvider
					.builder().stsClient(StsClient.builder().credentialsProvider(AnonymousCredentialsProvider.create())
							.region(regionProvider.getRegion()).build());

			if (stsProperties != null) {
				builder.asyncCredentialUpdateEnabled(stsProperties.isAsyncCredentialsUpdate());
				propertyMapper.from(stsProperties::getRoleArn).whenNonNull().to(builder::roleArn);
				propertyMapper.from(stsProperties::getWebIdentityTokenFile).whenNonNull()
						.to(b -> builder.webIdentityTokenFile(Paths.get(b)));
				propertyMapper.from(stsProperties::getRoleSessionName).whenNonNull().to(builder::roleSessionName);
			}
			return builder.build();
		}
	}
}
