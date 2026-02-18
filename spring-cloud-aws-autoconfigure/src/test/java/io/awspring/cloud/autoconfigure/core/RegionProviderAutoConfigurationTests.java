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

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.core.region.StaticRegionProvider;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsProfileRegionProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider;

class RegionProviderAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class));

	@Test
	void autoDetectionConfigured_noConfigurationProvided_DefaultAwsRegionProviderChainDelegateConfigured() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBean(DefaultAwsRegionProviderChain.class)).isNotNull();
		});
	}

	@Test
	void autoDetectionConfigured_emptyStaticRegionConfigured_DefaultAwsRegionProviderChainDelegateConfigured() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.region.static:").run((context) -> {
			assertThat(context.getBean(DefaultAwsRegionProviderChain.class)).isNotNull();
		});
	}

	@Test
	void staticRegionConfigured_staticRegionProviderWithConfiguredRegionConfigured() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.region.static:eu-west-1").run((context) -> {
			AwsRegionProvider awsRegionProvider = context.getBean("regionProvider", StaticRegionProvider.class);
			assertThat(awsRegionProvider.getRegion()).isEqualTo(Region.EU_WEST_1);
		});
	}

	@Test
	void customRegionConfigured() {
		this.contextRunner.withUserConfiguration(CustomRegionProviderConfiguration.class).run((context) -> {
			AwsRegionProvider regionProvider = context.getBean(AwsRegionProvider.class);
			assertThat(regionProvider).isNotNull().isInstanceOf(CustomRegionProvider.class);
		});

	}

	@Test
	void regionProvider_profileNameAndPathConfigured_profileRegionProviderConfiguredWithCustomProfile()
			throws IOException {
		this.contextRunner.withPropertyValues("spring.cloud.aws.region.profile.name:customProfile",
				"spring.cloud.aws.region.profile.path:"
						+ new ClassPathResource(getClass().getSimpleName() + "-profile", getClass()).getFile()
								.getAbsolutePath())
				.run((context) -> {
					AwsRegionProvider awsRegionProvider = context.getBean("regionProvider",
							AwsProfileRegionProvider.class);
					assertThat(awsRegionProvider).isNotNull();
					assertThat(awsRegionProvider.getRegion()).isEqualTo(Region.EU_WEST_1);
				});
	}

	@Test
	void regionProvider_credentialProfileNameAndPathConfigured_profileRegionProviderConfiguredWithCustomProfile()
			throws IOException {
		this.contextRunner.withPropertyValues("spring.cloud.aws.credentials.profile.name:customProfile",
				"spring.cloud.aws.credentials.profile.path:"
						+ new ClassPathResource(getClass().getSimpleName() + "-profile", getClass()).getFile()
								.getAbsolutePath())
				.run((context) -> {
					AwsRegionProvider awsRegionProvider = context.getBean("regionProvider",
							AwsProfileRegionProvider.class);
					assertThat(awsRegionProvider).isNotNull();
					assertThat(awsRegionProvider.getRegion()).isEqualTo(Region.EU_WEST_1);
				});
	}

	@Test
	void regionProvider_credentialAndRegionProfileNameAndPathBothConfigured_profileRegionProviderConfiguredWithCustomProfile()
			throws IOException {
		this.contextRunner.withPropertyValues("spring.cloud.aws.credentials.profile.name:noneProfile",
				"spring.cloud.aws.credentials.profile.path:",
				"spring.cloud.aws.region.profile.name:customProfile",
				"spring.cloud.aws.region.profile.path:"
						+ new ClassPathResource(getClass().getSimpleName() + "-profile", getClass()).getFile()
								.getAbsolutePath())
				.run((context) -> {
					AwsRegionProvider awsRegionProvider = context.getBean("regionProvider",
							AwsProfileRegionProvider.class);
					assertThat(awsRegionProvider).isNotNull();
					assertThat(awsRegionProvider.getRegion()).isEqualTo(Region.EU_WEST_1);
				});
	}

	@Test
	void regionProvider_instanceProfileConfigured_configuresInstanceProfileCredentialsProvider() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.region.instance-profile:true").run((context) -> {
			AwsRegionProvider awsCredentialsProvider = context.getBean("regionProvider",
					InstanceProfileRegionProvider.class);
			assertThat(awsCredentialsProvider).isNotNull();
		});
	}

	@Test
	void isNotCreatedWhenAwsRegionModuleIsNotInClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(AwsRegionProvider.class)).run(context -> {
			assertThat(context).doesNotHaveBean(RegionProviderAutoConfiguration.class);
		});
	}

	@Test
	void isNotCreatedWhenAwsProfilesModuleIsNotInClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ProfileFile.class)).run(context -> {
			assertThat(context).doesNotHaveBean(RegionProviderAutoConfiguration.class);
		});
	}

	@Test
	void isNotCreatedWhenCoreModuleIsNotInClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(StaticRegionProvider.class)).run(context -> {
			assertThat(context).doesNotHaveBean(RegionProviderAutoConfiguration.class);
		});
	}

	@Configuration
	static class CustomRegionProviderConfiguration {

		@Bean
		public AwsRegionProvider customRegionProvider() {
			return new CustomRegionProvider();
		}

	}

	static class CustomRegionProvider implements AwsRegionProvider {

		@Override
		public Region getRegion() {
			return null;
		}

	}

}
