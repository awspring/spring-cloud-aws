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
package io.awspring.cloud.autoconfigure.config;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.core.CredentialsProperties;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProperties;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.core.SpringCloudClientConfiguration;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

/**
 * Base class for AWS specific {@link ConfigDataLocationResolver}s.
 *
 * @param <T> - the location type
 * @author Maciej Walkowiak
 * @since 3.0
 */
public abstract class AbstractAwsConfigDataLocationResolver<T extends ConfigDataResource>
		implements ConfigDataLocationResolver<T> {

	protected abstract String getPrefix();

	private final boolean CLOUDWATCH_METRICS_PUBLISHER_IN_CLASSPATH = ClassUtils.isPresent(
			"software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher",
			getClass().getClassLoader());

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		return location.hasPrefix(getPrefix());
	}

	@Override
	public List<T> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location)
			throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
		return Collections.emptyList();
	}

	protected <C> void registerAndPromoteBean(ConfigDataLocationResolverContext context, Class<C> type,
			BootstrapRegistry.InstanceSupplier<C> supplier) {
		registerBean(context, type, supplier);
		context.getBootstrapContext().addCloseListener(event -> {
			String name = "configData" + type.getSimpleName();
			C instance = event.getBootstrapContext().get(type);
			ConfigurableApplicationContext appContext = event.getApplicationContext();
			// Since hook can be activated more than one time, ApplicationContext needs to
			// be checked if bean is already registered to prevent Exception. See
			// https://github.com/awspring/spring-cloud-aws/issues/108 for more
			// information.
			if (!appContext.getBeanFactory().containsBean(name)) {
				event.getApplicationContext().getBeanFactory().registerSingleton(name, instance);
			}
		});
	}

	protected <C> void registerBean(ConfigDataLocationResolverContext context, Class<C> type, C instance) {
		context.getBootstrapContext().registerIfAbsent(type, BootstrapRegistry.InstanceSupplier.of(instance));
	}

	protected <C> void registerBean(ConfigDataLocationResolverContext context, Class<C> type,
			BootstrapRegistry.InstanceSupplier<C> supplier) {
		ConfigurableBootstrapContext bootstrapContext = context.getBootstrapContext();
		bootstrapContext.registerIfAbsent(type, supplier);
	}

	protected CredentialsProperties loadCredentialsProperties(Binder binder) {
		return binder.bind(CredentialsProperties.PREFIX, Bindable.of(CredentialsProperties.class))
				.orElseGet(CredentialsProperties::new);
	}

	protected RegionProperties loadRegionProperties(Binder binder) {
		return binder.bind(RegionProperties.PREFIX, Bindable.of(RegionProperties.class))
				.orElseGet(RegionProperties::new);
	}

	protected AwsProperties loadAwsProperties(Binder binder) {
		return binder.bind(AwsProperties.CONFIG_PREFIX, Bindable.of(AwsProperties.class)).orElseGet(AwsProperties::new);
	}

	protected List<String> getCustomContexts(String keys) {
		if (StringUtils.hasLength(keys)) {
			return Arrays.asList(keys.split(";"));
		}
		return Collections.emptyList();
	}

	protected <T extends AwsClientBuilder<?, ?>> T configure(T builder, AwsClientProperties properties,
			BootstrapContext context) {
		AwsCredentialsProvider credentialsProvider;

		try {
			credentialsProvider = context.get(AwsCredentialsProvider.class);
		}
		catch (IllegalStateException e) {
			CredentialsProperties credentialsProperties = context.get(CredentialsProperties.class);
			credentialsProvider = CredentialsProviderAutoConfiguration.createCredentialsProvider(credentialsProperties);
		}

		AwsRegionProvider regionProvider;

		try {
			regionProvider = context.get(AwsRegionProvider.class);
		}
		catch (IllegalStateException e) {
			RegionProperties regionProperties = context.get(RegionProperties.class);
			regionProvider = RegionProviderAutoConfiguration.createRegionProvider(regionProperties);
		}

		AwsProperties awsProperties = context.get(AwsProperties.class);

		if (StringUtils.hasLength(properties.getRegion())) {
			builder.region(Region.of(properties.getRegion()));
		}
		else {
			builder.region(regionProvider.getRegion());
		}
		if (properties.getEndpoint() != null) {
			builder.endpointOverride(properties.getEndpoint());
		}
		else if (awsProperties.getEndpoint() != null) {
			builder.endpointOverride(awsProperties.getEndpoint());
		}
		builder.credentialsProvider(credentialsProvider);

		if (CLOUDWATCH_METRICS_PUBLISHER_IN_CLASSPATH) {
			CloudWatchMetricPublisher metricPublisher = createMetricPublisher(awsProperties, properties, regionProvider,
					builder, credentialsProvider);
			ClientOverrideConfiguration.Builder clientOverrideConfigurationBuilder = new SpringCloudClientConfiguration()
					.clientOverrideConfiguration().toBuilder();
			clientOverrideConfigurationBuilder.addMetricPublisher(metricPublisher);
			builder.overrideConfiguration(clientOverrideConfigurationBuilder.build());
		}

		return builder;
	}

	// TODO: This is very ugly, also not registering any bean but rather just creating the client.
	private <T extends AwsClientBuilder<?, ?>> CloudWatchMetricPublisher createMetricPublisher(
			AwsProperties awsProperties, AwsClientProperties awsClientProperties, AwsRegionProvider regionProvider,
			T clientBuilder, AwsCredentialsProvider credentialsProvider) {
		CloudWatchMetricPublisher.Builder builder = CloudWatchMetricPublisher.builder();
		PropertyMapper map = PropertyMapper.get();
		if ((awsClientProperties.getMetrics() != null && awsClientProperties.getMetrics().getEnabled())) {
			CloudWatchAsyncClient cloudwatchAsyncClient = createCloudwatchAsyncClient(awsClientProperties.getRegion(),
					awsClientProperties.getEndpoint(), credentialsProvider);

			map.from(awsClientProperties.getMetrics()::getNamespace).whenNonNull().to(builder::namespace);
			map.from(awsClientProperties.getMetrics()::getUploadFrequencyInSeconds).whenNonNull()
					.to(v -> builder.uploadFrequency(Duration.ofSeconds(v)));
			builder.cloudWatchClient(cloudwatchAsyncClient);
		}
		else if (awsProperties.getMetrics() == null
				|| (awsProperties.getMetrics() != null && awsProperties.getMetrics().getEnabled())) {
			CloudWatchAsyncClient cloudwatchAsyncClient = createCloudwatchAsyncClient(regionProvider.getRegion().id(),
					awsProperties.getEndpoint(), credentialsProvider);
			if (awsProperties.getMetrics() != null) {
				map.from(awsProperties.getMetrics()::getNamespace).whenNonNull().to(builder::namespace);
				map.from(awsProperties.getMetrics()::getUploadFrequencyInSeconds).whenNonNull()
						.to(v -> builder.uploadFrequency(Duration.ofSeconds(v)));
			}
			builder.cloudWatchClient(cloudwatchAsyncClient);
		}
		return builder.build();
	}

	private <T extends AwsClientBuilder<?, ?>> CloudWatchAsyncClient createCloudwatchAsyncClient(String region,
			URI endpoint, AwsCredentialsProvider credentialsProvider) {
		return CloudWatchAsyncClient.builder().region(Region.of(region)).endpointOverride(endpoint)
				.credentialsProvider(credentialsProvider).build();
	}
}
