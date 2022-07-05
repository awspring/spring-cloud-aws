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

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.metrics.LoggingMetricPublisher;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

import java.time.Duration;

/**
 * Autoconfigures AWS environment.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsProperties.class)
public class AwsAutoConfiguration {

	private final AwsProperties awsProperties;

	public AwsAutoConfiguration(AwsProperties awsProperties) {
		this.awsProperties = awsProperties;
	}

	@ConditionalOnClass(CloudWatchMetricPublisher.class)
	static class MetricsAutoConfiguration {
		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(name = "spring.cloud.aws.metrics-enabled", havingValue = "true", matchIfMissing = true)
		MetricPublisher cloudWatchMetricPublisher() {
			return CloudWatchMetricPublisher.builder().build();
		}
	}

	@Bean
	public AwsClientBuilderConfigurer awsClientBuilderConfigurer(AwsCredentialsProvider credentialsProvider,
			AwsRegionProvider awsRegionProvider) {
		return new AwsClientBuilderConfigurer(credentialsProvider, awsRegionProvider, awsProperties);
	}
}
