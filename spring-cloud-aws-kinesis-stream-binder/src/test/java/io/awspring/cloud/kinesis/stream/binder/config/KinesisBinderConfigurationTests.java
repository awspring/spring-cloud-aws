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
package io.awspring.cloud.kinesis.stream.binder.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.metrics.CloudWatchProperties;
import io.awspring.cloud.core.region.StaticRegionProvider;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisBinderConfigurationProperties;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.stream.binding.Bindable;
import org.springframework.core.ResolvableType;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;

/**
 * Tests for {@link KinesisBinderConfiguration}.
 *
 * @author kobeomseok95
 *
 * @since 4.1
 */
class KinesisBinderConfigurationTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	void cloudWatchClientIsCreatedWhenNoCustomizerBeanIsRegistered() {
		KinesisBinderConfiguration configuration = binderConfiguration();

		try (CloudWatchAsyncClient cloudWatch = configuration.cloudWatch(new CloudWatchProperties(),
				cloudWatchCustomizers())) {

			assertThat(cloudWatch).isNotNull();
		}
	}

	@Test
	void cloudWatchClientAppliesRegisteredCustomizer() {
		KinesisBinderConfiguration configuration = binderConfiguration();
		TrackingCloudWatchCustomizer customizer = new TrackingCloudWatchCustomizer();
		this.beanFactory.registerSingleton("cloudWatchCustomizer", customizer);

		try (CloudWatchAsyncClient cloudWatch = configuration.cloudWatch(new CloudWatchProperties(),
				cloudWatchCustomizers())) {

			assertThat(cloudWatch).isNotNull();
			assertThat(customizer.applied).isTrue();
		}
	}

	private KinesisBinderConfiguration binderConfiguration() {
		AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider
				.create(AwsBasicCredentials.create("noop", "noop"));
		AwsRegionProvider regionProvider = new StaticRegionProvider("eu-west-1");
		AwsClientBuilderConfigurer awsClientBuilderConfigurer = new AwsClientBuilderConfigurer(credentialsProvider,
				regionProvider, new AwsProperties());
		Bindable bindableWithInput = new Bindable() {

			@Override
			public Set<String> getInputs() {
				return Set.of("input-in-0");
			}

		};
		return new KinesisBinderConfiguration(new KinesisBinderConfigurationProperties(), credentialsProvider,
				regionProvider, awsClientBuilderConfigurer, List.of(bindableWithInput));
	}

	private ObjectProvider<AwsClientCustomizer<CloudWatchAsyncClientBuilder>> cloudWatchCustomizers() {
		return this.beanFactory.getBeanProvider(
				ResolvableType.forClassWithGenerics(AwsClientCustomizer.class, CloudWatchAsyncClientBuilder.class));
	}

	private static final class TrackingCloudWatchCustomizer
			implements AwsClientCustomizer<CloudWatchAsyncClientBuilder> {

		private boolean applied;

		@Override
		public void customize(CloudWatchAsyncClientBuilder builder) {
			this.applied = true;
		}

	}

}
