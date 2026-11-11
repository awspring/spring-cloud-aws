/*
 * Copyright 2013-2026 the original author or authors.
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

import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a {@link CloudWatchMetricPublisher} that publishes the metrics
 * of the AWS SDK clients configured by Spring Cloud AWS to CloudWatch.
 *
 * @author Sharang Gupta
 * @since 4.3.0
 * @see AwsAutoConfiguration
 */
@AutoConfiguration
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnClass(CloudWatchMetricPublisher.class)
@ConditionalOnProperty(prefix = ClientMetricsProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({ ClientMetricsProperties.class, AwsProperties.class })
public class CloudWatchMetricPublisherAutoConfiguration {

	private static final Log logger = LogFactory.getLog(CloudWatchMetricPublisherAutoConfiguration.class);

	// Both beans are lazy, so that they are only created, and only need a region, when
	// the first client is configured. Without a region, both are null beans, which the
	// clients skip, so the application starts without publishing metrics.

	@Bean
	@Lazy
	@ConditionalOnMissingBean(CloudWatchMetricPublisher.class)
	@Nullable
	MetricPublisherClientHolder metricPublisherClientHolder(AwsCredentialsProvider credentialsProvider,
			AwsRegionProvider regionProvider, AwsProperties awsProperties, ClientMetricsProperties properties) {
		Region region = resolveRegion(properties, regionProvider);
		if (region == null) {
			return null;
		}
		// the CloudWatch client of the publisher must not publish metrics itself, so it is
		// configured without any metric publisher
		AwsClientBuilderConfigurer configurer = new AwsClientBuilderConfigurer(credentialsProvider, () -> region,
				awsProperties);
		return new MetricPublisherClientHolder(
				configurer.configure(CloudWatchAsyncClient.builder(), properties, null).build());
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	@Nullable
	CloudWatchMetricPublisher cloudWatchMetricPublisher(@Nullable MetricPublisherClientHolder client,
			ClientMetricsProperties properties) {
		if (client == null) {
			return null;
		}
		CloudWatchMetricPublisher.Builder builder = CloudWatchMetricPublisher.builder()
				.cloudWatchClient(client.cloudWatchClient());
		Optional.ofNullable(properties.getNamespace()).ifPresent(builder::namespace);
		Optional.ofNullable(properties.getUploadFrequency()).ifPresent(builder::uploadFrequency);
		Optional.ofNullable(properties.getMaximumCallsPerUpload()).ifPresent(builder::maximumCallsPerUpload);
		Optional.ofNullable(properties.getMetricLevel()).ifPresent(builder::metricLevel);
		return builder.build();
	}

	@Nullable
	private static Region resolveRegion(ClientMetricsProperties properties, AwsRegionProvider regionProvider) {
		try {
			return AwsClientBuilderConfigurer.resolveRegion(properties, null, regionProvider);
		}
		catch (SdkClientException ex) {
			logger.warn("The metrics of the AWS SDK clients are not published to CloudWatch, because no region was "
					+ "found for the CloudWatch client of the publisher. Set spring.cloud.aws.client.metrics.region or "
					+ "spring.cloud.aws.region.static, or set spring.cloud.aws.client.metrics.enabled=false to hide "
					+ "this warning. " + ex.getMessage());
			return null;
		}
	}

	/**
	 * Holds the CloudWatch client of the metric publisher. {@link CloudWatchMetricPublisher#close()} does not close a
	 * client that it did not create, so this bean closes it, after the publisher has flushed its last metrics. It is
	 * not a {@link CloudWatchAsyncClient} bean, so that it is never injected where the application expects one.
	 */
	static final class MetricPublisherClientHolder implements AutoCloseable {

		private final CloudWatchAsyncClient cloudWatchClient;

		MetricPublisherClientHolder(CloudWatchAsyncClient cloudWatchClient) {
			this.cloudWatchClient = cloudWatchClient;
		}

		CloudWatchAsyncClient cloudWatchClient() {
			return this.cloudWatchClient;
		}

		@Override
		public void close() {
			this.cloudWatchClient.close();
		}

	}

}
