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

package org.springframework.cloud.aws.autoconfigure.metrics;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
import io.micrometer.cloudwatch.CloudWatchConfig;
import io.micrometer.cloudwatch.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.autoconfigure.context.ContextCredentialsAutoConfiguration;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for exporting metrics to CloudWatch.
 *
 * @author Jon Schneider
 * @author Dawid Kublik
 * @author Jan Sauer
 * @since 2.0.0
 */
@Configuration
@Import(ContextCredentialsAutoConfiguration.class)
@AutoConfigureBefore({ CompositeMeterRegistryAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@EnableConfigurationProperties(CloudWatchProperties.class)
@ConditionalOnProperty(prefix = "management.metrics.export.cloudwatch",
		name = "namespace")
@ConditionalOnClass({ CloudWatchMeterRegistry.class, RegionProvider.class })
public class CloudWatchExportAutoConfiguration {

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Bean
	@ConditionalOnProperty(value = "management.metrics.export.cloudwatch.enabled",
			matchIfMissing = true)
	public CloudWatchMeterRegistry cloudWatchMeterRegistry(CloudWatchConfig config,
			Clock clock, AmazonCloudWatchAsync client) {
		return new CloudWatchMeterRegistry(config, clock, client);
	}

	@Bean
	@ConditionalOnMissingAmazonClient(AmazonCloudWatchAsync.class)
	public AmazonWebserviceClientFactoryBean<AmazonCloudWatchAsyncClient> amazonCloudWatchAsync(
			AWSCredentialsProvider credentialsProvider) {
		return new AmazonWebserviceClientFactoryBean<>(AmazonCloudWatchAsyncClient.class,
				credentialsProvider, this.regionProvider);
	}

	@Bean
	@ConditionalOnMissingBean
	public CloudWatchConfig cloudWatchConfig(CloudWatchProperties cloudWatchProperties) {
		return new CloudWatchPropertiesConfigAdapter(cloudWatchProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public Clock micrometerClock() {
		return Clock.SYSTEM;
	}

}
