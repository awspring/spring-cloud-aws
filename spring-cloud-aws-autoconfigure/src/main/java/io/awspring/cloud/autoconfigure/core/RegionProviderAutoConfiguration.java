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

import io.awspring.cloud.core.region.StaticRegionProvider;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.providers.AwsProfileRegionProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider;

/**
 * {@link EnableAutoConfiguration} for {@link AwsRegionProvider}.
 *
 * @author Siva Katamreddy
 * @author Eddú Meléndez
 * @author Wei Jiang
 */
@AutoConfiguration
@ConditionalOnClass({ StaticRegionProvider.class, AwsRegionProvider.class, ProfileFile.class })
@EnableConfigurationProperties({ CredentialsProperties.class, RegionProperties.class })
public class RegionProviderAutoConfiguration {

	private final CredentialsProperties credentialsProperties;

	private final RegionProperties regionProperties;

	public RegionProviderAutoConfiguration(CredentialsProperties credentialsProperties,
			RegionProperties regionProperties) {
		this.credentialsProperties = credentialsProperties;
		this.regionProperties = regionProperties;
	}

	@Bean
	@ConditionalOnMissingBean
	public AwsRegionProvider regionProvider() {
		return createRegionProvider(this.credentialsProperties, this.regionProperties);
	}

	public static AwsRegionProvider createRegionProvider(CredentialsProperties credentialsProperties,
			RegionProperties regionProperties) {
		final List<AwsRegionProvider> providers = new ArrayList<>();

		if (regionProperties.getStatic() != null && regionProperties.isStatic()) {
			providers.add(new StaticRegionProvider(regionProperties.getStatic()));
		}

		if (regionProperties.isInstanceProfile()) {
			providers.add(new InstanceProfileRegionProvider());
		}

		Profile profile = getConfigurationProfile(credentialsProperties, regionProperties);
		if (profile != null && profile.getName() != null) {
			providers.add(createProfileRegionProvider(profile));
		}

		if (providers.isEmpty()) {
			return DefaultAwsRegionProviderChain.builder().build();
		}
		else if (providers.size() == 1) {
			return providers.get(0);
		}
		else {
			return new AwsRegionProviderChain(providers.toArray(new AwsRegionProvider[0]));
		}
	}

	private static Profile getConfigurationProfile(CredentialsProperties credentialsProperties,
			RegionProperties regionProperties) {
		Profile profile = regionProperties.getProfile();

		if (profile != null) {
			return profile;
		}

		return credentialsProperties.getProfile();
	}

	private static AwsProfileRegionProvider createProfileRegionProvider(Profile profile) {
		Supplier<ProfileFile> profileFileFn = () -> {
			if (profile.getPath() != null) {
				return ProfileFile.builder().type(ProfileFile.Type.CONFIGURATION).content(Paths.get(profile.getPath()))
						.build();
			}
			else {
				return ProfileFile.defaultProfileFile();
			}
		};
		return new AwsProfileRegionProvider(profileFileFn, profile.getName());
	}

}
