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

import java.util.List;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
			AwsRegionProvider regionProvider = context.getBean(AwsRegionProvider.class);
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

	@Test
	void credentialsProvider_instanceProfileConfigured_configuresInstanceProfileCredentialsProvider() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.region.instance-profile:true").run((context) -> {
			AwsRegionProvider awsCredentialsProvider = context.getBean("awsRegionProvider", AwsRegionProvider.class);
			assertThat(awsCredentialsProvider).isNotNull();

			@SuppressWarnings("unchecked")
			List<AwsRegionProvider> credentialsProviders = (List<AwsRegionProvider>) ReflectionTestUtils
					.getField(awsCredentialsProvider, "providers");
			assertThat(credentialsProviders).hasSize(1).hasOnlyElementsOfType(InstanceProfileRegionProvider.class);
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
