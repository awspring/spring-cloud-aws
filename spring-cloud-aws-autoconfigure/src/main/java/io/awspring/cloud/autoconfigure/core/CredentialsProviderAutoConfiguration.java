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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
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
@EnableConfigurationProperties({ CredentialsProperties.class, StsProperties.class })
public class CredentialsProviderAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(CredentialsProviderAutoConfiguration.class);

	/**
	 * The required class that has to be on the classpath to create StsWebIdentityTokenFileCredentialsProvider
	 */
	public static final String STS_WEB_IDENTITY_TOKEN_FILE_CREDENTIALS_PROVIDER = "software.amazon.awssdk.services.sts.auth.StsWebIdentityTokenFileCredentialsProvider";

	private final CredentialsProperties properties;

	private final StsProperties stsProperties;

	public CredentialsProviderAutoConfiguration(CredentialsProperties properties, StsProperties stsProperties) {
		this.properties = properties;
		this.stsProperties = stsProperties;
	}

	@Bean
	@ConditionalOnMissingBean
	public AwsCredentialsProvider credentialsProvider() {
		return createCredentialsProvider(this.properties, this.stsProperties);
	}

	public static AwsCredentialsProvider createCredentialsProvider(CredentialsProperties properties, StsProperties stsProperties) {
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

		if (shouldCreateStsIdentityTokenCredentialsProvider(stsProperties)) {
			providers.add(createStsCredentialsProvider(stsProperties));
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

	private static boolean shouldCreateStsIdentityTokenCredentialsProvider(StsProperties stsProperties) {
		return ClassUtils.isPresent(STS_WEB_IDENTITY_TOKEN_FILE_CREDENTIALS_PROVIDER, null)
			&& stsProperties.isValid();
	}

	private static StsWebIdentityTokenFileCredentialsProvider createStsCredentialsProvider(StsProperties stsProperties) {
		logger.debug("Creating StsWebIdentityTokenFileCredentialsProvider");
		StsWebIdentityTokenFileCredentialsProvider.Builder builder = StsWebIdentityTokenFileCredentialsProvider.builder()
			.stsClient(StsClient.builder().region(Region.of(stsProperties.getRegion())).build())
			.roleArn(stsProperties.getRoleArn())
			.webIdentityTokenFile(stsProperties.getWebIdentityTokenFile());

		if (stsProperties.isAsyncCredentialsUpdate() != null) {
			builder.asyncCredentialUpdateEnabled(stsProperties.isAsyncCredentialsUpdate());
		}

		if (stsProperties.getRoleSessionName() != null) {
			builder.roleSessionName(stsProperties.getRoleSessionName());
		}
		return builder.build();
	}

}
