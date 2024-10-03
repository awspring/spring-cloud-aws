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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

/**
 * Autoconfigures AWS environment.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
@AutoConfiguration
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@EnableConfigurationProperties(AwsProperties.class)
public class AwsAutoConfiguration {

	private final AwsProperties awsProperties;

	public AwsAutoConfiguration(AwsProperties awsProperties) {
		this.awsProperties = awsProperties;
	}

	@Bean
	public AwsClientBuilderConfigurer awsClientBuilderConfigurer(AwsCredentialsProvider credentialsProvider,
			AwsRegionProvider awsRegionProvider) {
		return new AwsClientBuilderConfigurer(credentialsProvider, awsRegionProvider, awsProperties);
	}
}
