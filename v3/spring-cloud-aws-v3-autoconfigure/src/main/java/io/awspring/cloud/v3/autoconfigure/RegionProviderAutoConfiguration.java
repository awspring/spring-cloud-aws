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

package io.awspring.cloud.v3.autoconfigure;

import io.awspring.cloud.v3.autoconfigure.properties.AwsRegionProperties;
import io.awspring.cloud.v3.core.region.StaticRegionProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration} for {@link AwsRegionProvider}.
 *
 * @author Maciej Walkowiak
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsRegionProperties.class)
public class RegionProviderAutoConfiguration {

	private final AwsRegionProperties properties;

	public RegionProviderAutoConfiguration(AwsRegionProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public AwsRegionProvider awsRegionProvider() {
		if (properties.isStatic()) {
			return new StaticRegionProvider(properties.getStatic());
		}
		else {
			return DefaultAwsRegionProviderChain.builder().build();
		}
	}

}
