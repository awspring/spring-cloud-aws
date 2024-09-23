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

import io.awspring.cloud.autoconfigure.AwsAsyncClientCustomizer;
import io.awspring.cloud.autoconfigure.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.AwsClientProperties;
import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.core.SpringCloudClientConfiguration;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

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

	public AwsClientBuilderConfigurer(AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider,
			AwsProperties awsProperties) {
		this.credentialsProvider = credentialsProvider;
		this.regionProvider = regionProvider;
		this.awsProperties = awsProperties;
		this.clientOverrideConfiguration = new SpringCloudClientConfiguration().clientOverrideConfiguration();
	}

	public <T extends AwsClientBuilder<T, ?>> T configure(T builder) {
		return configure(builder, null, null, null);
	}

	/**
	 * @deprecated use
	 * {@link #configureSyncClient(AwsClientBuilder, AwsClientProperties, AwsConnectionDetails, Stream, Stream)} for
	 * sync client or
	 * {@link #configureAsyncClient(AwsClientBuilder, AwsClientProperties, AwsConnectionDetails, Stream, Stream)} for
	 * async client.
	 */
	@Deprecated
	public <T extends AwsClientBuilder<T, ?>> T configure(T builder, @Nullable AwsClientProperties clientProperties,
			@Nullable io.awspring.cloud.autoconfigure.core.AwsClientCustomizer<T> customizer) {
		return configure(builder, clientProperties, null, customizer);
	}

	/**
	 * @deprecated use
	 * {@link #configureSyncClient(AwsClientBuilder, AwsClientProperties, AwsConnectionDetails, Stream, Stream)} for
	 * sync client or
	 * {@link #configureAsyncClient(AwsClientBuilder, AwsClientProperties, AwsConnectionDetails, Stream, Stream)} for
	 * async client.
	 */
	@Deprecated
	public <T extends AwsClientBuilder<T, ?>> T configure(T builder, @Nullable AwsClientProperties clientProperties,
			@Nullable AwsConnectionDetails connectionDetails,
			@Nullable io.awspring.cloud.autoconfigure.core.AwsClientCustomizer<T> customizer) {
		Assert.notNull(builder, "builder is required");

		builder.credentialsProvider(this.credentialsProvider).region(resolveRegion(clientProperties, connectionDetails))
				.overrideConfiguration(this.clientOverrideConfiguration);
		Optional.ofNullable(this.awsProperties.getEndpoint()).ifPresent(builder::endpointOverride);
		Optional.ofNullable(clientProperties).map(AwsClientProperties::getEndpoint)
				.ifPresent(builder::endpointOverride);
		Optional.ofNullable(connectionDetails).map(AwsConnectionDetails::getEndpoint)
				.ifPresent(builder::endpointOverride);

		Optional.ofNullable(this.awsProperties.getDefaultsMode()).ifPresent(builder::defaultsMode);
		Optional.ofNullable(this.awsProperties.getFipsEnabled()).ifPresent(builder::fipsEnabled);
		Optional.ofNullable(this.awsProperties.getDualstackEnabled()).ifPresent(builder::dualstackEnabled);
		if (customizer != null) {
			io.awspring.cloud.autoconfigure.core.AwsClientCustomizer.apply(customizer, builder);
		}
		return builder;
	}

	public <T extends AwsClientBuilder<T, ?>> T configureSyncClient(T builder,
			@Nullable AwsClientProperties clientProperties, @Nullable AwsConnectionDetails connectionDetails,
			@Nullable Stream<? extends AwsClientCustomizer<T>> clientBuilderCustomizer,
			@Nullable Stream<? extends AwsSyncClientCustomizer> commonCustomizers) {
		return configureSyncClient(builder, clientProperties, connectionDetails, null, clientBuilderCustomizer,
				commonCustomizers);
	}

	public <T extends AwsClientBuilder<T, ?>> T configureAsyncClient(T builder,
			@Nullable AwsClientProperties clientProperties, @Nullable AwsConnectionDetails connectionDetails,
			@Nullable Stream<? extends AwsClientCustomizer<T>> clientBuilderCustomizer,
			@Nullable Stream<? extends AwsAsyncClientCustomizer> commonCustomizers) {
		return configureAsyncClient(builder, clientProperties, connectionDetails, null, clientBuilderCustomizer,
				commonCustomizers);
	}

	@Deprecated
	public <T extends AwsClientBuilder<T, ?>> T configure(T builder, @Nullable AwsClientProperties clientProperties,
			@Nullable AwsConnectionDetails connectionDetails,
			@Nullable io.awspring.cloud.autoconfigure.core.AwsClientCustomizer<T> customizer,
			@Nullable Stream<? extends AwsClientCustomizer<T>> clientBuilderCustomizer) {
		return configure(builder, clientProperties, connectionDetails, null, clientBuilderCustomizer);
	}

	/**
	 * @deprecated use
	 * {@link #configureSyncClient(AwsClientBuilder, AwsClientProperties, AwsConnectionDetails, Stream, Stream)}.
	 */
	@Deprecated
	public <T extends AwsClientBuilder<T, ?>> T configureSyncClient(T builder,
			@Nullable AwsClientProperties clientProperties, @Nullable AwsConnectionDetails connectionDetails,
			@Nullable io.awspring.cloud.autoconfigure.core.AwsClientCustomizer<T> customizer,
			@Nullable Stream<? extends AwsClientCustomizer<T>> clientBuilderCustomizer,
			@Nullable Stream<? extends AwsSyncClientCustomizer> commonBuilderCustomizer) {
		T result = configure(builder, clientProperties, connectionDetails, customizer);
		if (commonBuilderCustomizer != null && builder instanceof AwsSyncClientBuilder<?, ?>) {
			commonBuilderCustomizer.forEach(it -> it.customize((AwsSyncClientBuilder<?, ?>) result));
		}
		if (clientBuilderCustomizer != null) {
			clientBuilderCustomizer.forEach(it -> it.customize(result));
		}
		return result;
	}

	@Deprecated
	public <T extends AwsClientBuilder<T, ?>> T configureAsyncClient(T builder,
			@Nullable AwsClientProperties clientProperties, @Nullable AwsConnectionDetails connectionDetails,
			@Nullable io.awspring.cloud.autoconfigure.core.AwsClientCustomizer<T> customizer,
			@Nullable Stream<? extends AwsClientCustomizer<T>> clientBuilderCustomizer,
			@Nullable Stream<? extends AwsAsyncClientCustomizer> commonBuilderCustomizer) {
		T result = configure(builder, clientProperties, connectionDetails, customizer);
		if (commonBuilderCustomizer != null && builder instanceof AwsAsyncClientBuilder<?, ?>) {
			commonBuilderCustomizer.forEach(it -> it.customize((AwsAsyncClientBuilder<?, ?>) result));
		}
		if (clientBuilderCustomizer != null) {
			clientBuilderCustomizer.forEach(it -> it.customize(result));
		}
		return result;
	}

	public Region resolveRegion(@Nullable AwsClientProperties clientProperties,
			@Nullable AwsConnectionDetails connectionDetails) {
		return resolveRegion(clientProperties, connectionDetails, this.regionProvider);
	}

	public static Region resolveRegion(@Nullable AwsClientProperties clientProperties,
			@Nullable AwsConnectionDetails connectionDetails, AwsRegionProvider regionProvider) {
		if (connectionDetails != null && StringUtils.hasLength(connectionDetails.getRegion())) {
			return Region.of(connectionDetails.getRegion());
		}

		if (clientProperties != null && StringUtils.hasLength(clientProperties.getRegion())) {
			return Region.of(clientProperties.getRegion());
		}

		return regionProvider.getRegion();
	}
}
