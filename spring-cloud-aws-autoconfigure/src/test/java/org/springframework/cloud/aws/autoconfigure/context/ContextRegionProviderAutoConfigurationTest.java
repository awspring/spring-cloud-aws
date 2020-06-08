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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.core.region.DefaultAwsRegionProviderChainDelegate;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextRegionProviderAutoConfiguration}.
 *
 * @author Agim Emruli
 * @author Petromir Dzhunev
 * @author Maciej Walkowiak
 */
class ContextRegionProviderAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(ContextRegionProviderAutoConfiguration.class));

	@Test
	void autoDetectionConfigured_noConfigurationProvided_DefaultAwsRegionProviderChainDelegateConfigured() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBean(DefaultAwsRegionProviderChainDelegate.class))
					.isNotNull();
		});
	}

	@Test
	void autoDetectionConfigured_emptyStaticRegionConfigured_DefaultAwsRegionProviderChainDelegateConfigured() {
		this.contextRunner.withPropertyValues("cloud.aws.region.static:")
				.run((context) -> {
					assertThat(
							context.getBean(DefaultAwsRegionProviderChainDelegate.class))
									.isNotNull();
				});
	}

	@Test
	void staticRegionConfigured_staticRegionProviderWithConfiguredRegionConfigured() {
		this.contextRunner.withPropertyValues("cloud.aws.region.static:eu-west-1")
				.run((context) -> {
					StaticRegionProvider regionProvider = context
							.getBean(StaticRegionProvider.class);
					assertThat(regionProvider.getRegion())
							.isEqualTo(Region.getRegion(Regions.EU_WEST_1));
				});
	}

}
