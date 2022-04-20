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
package io.awspring.cloud.autoconfigure.metrics;

import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.core.SpringCloudClientConfiguration;
import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import java.util.Optional;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;

/**
 * Configuration for exporting metrics to CloudWatch.
 *
 * @author Jon Schneider
 * @author Dawid Kublik
 * @author Jan Sauer
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({ CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class,
		MetricsAutoConfiguration.class })
@EnableConfigurationProperties(CloudWatchProperties.class)
@ConditionalOnProperty(prefix = "spring.cloud.aws.cloudwatch", name = "namespace")
@ConditionalOnClass({ CloudWatchAsyncClient.class, CloudWatchMeterRegistry.class, AwsRegionProvider.class })
public class CloudWatchExportAutoConfiguration {

	private final CloudWatchProperties properties;

	public CloudWatchExportAutoConfiguration(CloudWatchProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnProperty(value = "spring.cloud.aws.cloudwatch.enabled", matchIfMissing = true)
	public CloudWatchMeterRegistry cloudWatchMeterRegistry(CloudWatchConfig config, Clock clock,
			CloudWatchAsyncClient client) {
		return new CloudWatchMeterRegistry(config, clock, client);
	}

	@Bean
	@ConditionalOnMissingBean
	public CloudWatchAsyncClient cloudWatchAsyncClient(AwsCredentialsProvider credentialsProvider,
			AwsRegionProvider regionProvider) {
		Region region = StringUtils.hasLength(this.properties.getRegion()) ? Region.of(this.properties.getRegion())
				: regionProvider.getRegion();
		CloudWatchAsyncClientBuilder client = CloudWatchAsyncClient.builder().credentialsProvider(credentialsProvider)
				.region(region).overrideConfiguration(SpringCloudClientConfiguration.clientOverrideConfiguration());
		Optional.ofNullable(this.properties.getEndpoint()).ifPresent(client::endpointOverride);
		return client.build();
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
