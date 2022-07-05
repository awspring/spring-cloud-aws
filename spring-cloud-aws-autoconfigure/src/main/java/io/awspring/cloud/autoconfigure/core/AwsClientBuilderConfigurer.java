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

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import io.awspring.cloud.autoconfigure.metrics.CloudWatchProperties;
import io.awspring.cloud.core.SpringCloudClientConfiguration;
import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;

/**
 * Provides a convenience method to apply common configuration to any {@link AwsClientBuilder}.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public class AwsClientBuilderConfigurer {
	private final AwsCredentialsProvider credentialsProvider;
	private final AwsRegionProvider regionProvider;
	private final AwsProperties awsProperties;
	private final ClientOverrideConfiguration clientOverrideConfiguration;

	AwsClientBuilderConfigurer(AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider,
			AwsProperties awsProperties) {
		this.credentialsProvider = credentialsProvider;
		this.regionProvider = regionProvider;
		this.awsProperties = awsProperties;
		this.clientOverrideConfiguration = new SpringCloudClientConfiguration().clientOverrideConfiguration();
	}

	public <T extends AwsClientBuilder<?, ?>> T configure(T builder) {
		return configure(builder, null, null, null);
	}

	public <T extends AwsClientBuilder<?, ?>> T configure(T builder, @Nullable AwsClientProperties clientProperties,
			@Nullable AwsClientCustomizer<T> customizer, @Nullable MetricPublisher metricPublisher) {
		ClientOverrideConfiguration.Builder clientOverrideConfigurationBuilder = clientOverrideConfiguration
				.toBuilder();
		Assert.notNull(builder, "builder is required");
		Assert.notNull(clientProperties, "clientProperties are required");
		if (metricPublisher != null) {
			clientOverrideConfigurationBuilder.addMetricPublisher(metricPublisher).build();
		}
		builder.credentialsProvider(this.credentialsProvider).region(resolveRegion(clientProperties))
				.overrideConfiguration(clientOverrideConfigurationBuilder.build());
		Optional.ofNullable(this.awsProperties.getEndpoint()).ifPresent(builder::endpointOverride);
		Optional.ofNullable(clientProperties).map(AwsClientProperties::getEndpoint)
				.ifPresent(builder::endpointOverride);

		Optional.ofNullable(this.awsProperties.getDefaultsMode()).ifPresent(builder::defaultsMode);
		Optional.ofNullable(this.awsProperties.getFipsEnabled()).ifPresent(builder::fipsEnabled);
		Optional.ofNullable(this.awsProperties.getDualstackEnabled()).ifPresent(builder::dualstackEnabled);
		if (customizer != null) {
			AwsClientCustomizer.apply(customizer, builder);
		}

		return builder;
	}

	public Region resolveRegion(@Nullable AwsClientProperties clientProperties) {
		return clientProperties != null && StringUtils.hasLength(clientProperties.getRegion())
				? Region.of(clientProperties.getRegion())
				: this.regionProvider.getRegion();
	}

	public static @Nullable MetricPublisher createSpecificMetricPublisher(MetricPublisher metricPublisher,
			AwsClientProperties properties, AwsClientBuilderConfigurer awsClientBuilderConfigurer) {

		if (ClassUtils.isPresent("software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher", null)
				&& properties.getMetrics() != null) {
			if (properties.getMetrics().getEnabled() == null || properties.getMetrics().getEnabled()) {
				PropertyMapper propertyMapper = PropertyMapper.get();

				CloudWatchAsyncClientBuilder cloudWatchAsyncClientBuilder = CloudWatchAsyncClient.builder();
				CloudWatchProperties cloudWatchProperties = new CloudWatchProperties();
				propertyMapper.from(cloudWatchProperties.getEndpoint()).whenNonNull().to(cloudWatchProperties::setEndpoint);
				propertyMapper.from(cloudWatchProperties.getRegion()).whenNonNull().to(cloudWatchProperties::setRegion);
				CloudWatchAsyncClient cloudWatchAsyncClient = awsClientBuilderConfigurer.configure(cloudWatchAsyncClientBuilder, cloudWatchProperties, null, null).build();

				CloudWatchMetricPublisher.Builder builder = CloudWatchMetricPublisher.builder();
				builder.cloudWatchClient(cloudWatchAsyncClient);
				propertyMapper.from(properties.getMetrics()::getNamespace).whenNonNull().to(builder::namespace);
				propertyMapper.from(properties.getMetrics()::getUploadFrequencyInSeconds).whenNonNull()
						.to(v -> builder.uploadFrequency(Duration.ofSeconds(v)));
				return builder.build();
			}
			if (properties.getMetrics().getEnabled() != null && !properties.getMetrics().getEnabled()) {
				return null;
			}

		}
		return metricPublisher;
	}
}
