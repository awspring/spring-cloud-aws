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

import java.io.IOException;
import java.util.List;

import io.awspring.cloud.v3.core.region.StaticRegionProvider;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsProfileRegionProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

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
			AwsRegionProvider awsRegionProvider = context.getBean(AwsRegionProvider.class);

			@SuppressWarnings("unchecked")
			List<AwsRegionProvider> regionProviders = (List<AwsRegionProvider>) ReflectionTestUtils
					.getField(awsRegionProvider, "providers");
			assertThat(regionProviders).hasSize(1).hasOnlyElementsOfType(StaticRegionProvider.class);

			StaticRegionProvider regionProvider = (StaticRegionProvider) regionProviders.get(0);
			assertThat(regionProvider.getRegion()).isEqualTo(Region.EU_WEST_1);
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
					AwsRegionProvider awsRegionProvider = context.getBean("awsRegionProvider", AwsRegionProvider.class);
					assertThat(awsRegionProvider).isNotNull();

					@SuppressWarnings("unchecked")
					List<AwsRegionProvider> regionProviders = (List<AwsRegionProvider>) ReflectionTestUtils
							.getField(awsRegionProvider, "providers");
					assertThat(regionProviders).hasSize(1).hasOnlyElementsOfType(AwsProfileRegionProvider.class);

					AwsProfileRegionProvider regionProvider = (AwsProfileRegionProvider) regionProviders.get(0);
					assertThat(regionProvider.getRegion()).isEqualTo(Region.EU_WEST_1);
				});
	}

	@Test
	void customRegionConfigured() {
		this.contextRunner.withUserConfiguration(CustomRegionProviderConfiguration.class).run((context) -> {
			AwsRegionProvider regionProvider = context.getBean(AwsRegionProvider.class);
			assertThat(regionProvider).isNotNull().isInstanceOf(CustomRegionProvider.class);
		});

	}

	@Configuration
	static class CustomRegionProviderConfiguration {

		@Bean(name = "awsRegionProvider")
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
