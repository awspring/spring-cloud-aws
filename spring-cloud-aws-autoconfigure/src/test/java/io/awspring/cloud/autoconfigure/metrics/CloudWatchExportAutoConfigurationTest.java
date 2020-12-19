/*
 * Copyright 2013-2020 the original author or authors.
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

import java.net.URI;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
import io.micrometer.cloudwatch.CloudWatchConfig;
import io.micrometer.cloudwatch.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME;

/**
 * Test for the {@link CloudWatchExportAutoConfiguration}.
 *
 * @author Dawid Kublik
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 */
class CloudWatchExportAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CloudWatchExportAutoConfiguration.class));

	@Test
	void testWithoutSettingAnyConfigProperties() {
		this.contextRunner
				.run(context -> assertThat(context.getBeansOfType(CloudWatchMeterRegistry.class).isEmpty()).isTrue());
	}

	@Test
	void enableAutoConfigurationSettingNamespace() {
		this.contextRunner.withPropertyValues("management.metrics.export.cloudwatch.namespace:test").run(context -> {
			CloudWatchMeterRegistry metricsExporter = context.getBean(CloudWatchMeterRegistry.class);
			assertThat(metricsExporter).isNotNull();

			CloudWatchConfig cloudWatchConfig = context.getBean(CloudWatchConfig.class);
			assertThat(cloudWatchConfig).isNotNull();

			Clock clock = context.getBean(Clock.class);
			assertThat(clock).isNotNull();

			CloudWatchProperties cloudWatchProperties = context.getBean(CloudWatchProperties.class);
			assertThat(cloudWatchProperties).isNotNull();

			assertThat(cloudWatchProperties.getNamespace()).isEqualTo(cloudWatchConfig.namespace());

			AmazonCloudWatchAsyncClient client = context.getBean(AmazonCloudWatchAsyncClient.class);

			Object region = ReflectionTestUtils.getField(client, "signingRegion");
			assertThat(region).isEqualTo(Regions.DEFAULT_REGION.getName());
		});
	}

	@Test
	void enableAutoConfigurationWithSpecificRegion() {
		this.contextRunner.withPropertyValues("management.metrics.export.cloudwatch.namespace:test",
				"management.metrics.export.cloudwatch.region:us-east-1").run(context -> {
					CloudWatchMeterRegistry metricsExporter = context.getBean(CloudWatchMeterRegistry.class);
					assertThat(metricsExporter).isNotNull();

					CloudWatchConfig cloudWatchConfig = context.getBean(CloudWatchConfig.class);
					assertThat(cloudWatchConfig).isNotNull();

					Clock clock = context.getBean(Clock.class);
					assertThat(clock).isNotNull();

					CloudWatchProperties cloudWatchProperties = context.getBean(CloudWatchProperties.class);
					assertThat(cloudWatchProperties).isNotNull();

					assertThat(cloudWatchProperties.getNamespace()).isEqualTo(cloudWatchConfig.namespace());

					AmazonCloudWatchAsyncClient client = context.getBean(AmazonCloudWatchAsyncClient.class);

					Object region = ReflectionTestUtils.getField(client, "signingRegion");
					assertThat(region).isEqualTo(Regions.US_EAST_1.getName());
				});
	}

	@Test
	void enableAutoConfigurationWithCustomEndpoint() {
		this.contextRunner.withPropertyValues("management.metrics.export.cloudwatch.namespace:test",
				"management.metrics.export.cloudwatch.endpoint:http://localhost:8090").run(context -> {
					AmazonCloudWatchAsyncClient client = context.getBean(AmazonCloudWatchAsyncClient.class);

					Object endpoint = ReflectionTestUtils.getField(client, "endpoint");
					assertThat(endpoint).isEqualTo(URI.create("http://localhost:8090"));

					Boolean isEndpointOverridden = (Boolean) ReflectionTestUtils.getField(client,
							"isEndpointOverridden");
					assertThat(isEndpointOverridden).isTrue();
				});
	}

	@Test
	void configuration_withGlobalClientConfiguration_shouldUseItForClient() throws Exception {
		// Arrange & Act
		this.contextRunner.withPropertyValues("management.metrics.export.cloudwatch.namespace:test")
				.withUserConfiguration(ConfigurationWithGlobalClientConfiguration.class).run((context) -> {
					AmazonCloudWatchAsyncClient client = context.getBean(AmazonCloudWatchAsyncClient.class);

					// Assert
					ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(client,
							"clientConfiguration");
					assertThat(clientConfiguration.getProxyHost()).isEqualTo("global");
				});
	}

	@Test
	void configuration_withCloudWatchClientConfiguration_shouldUseItForClient() throws Exception {
		// Arrange & Act
		this.contextRunner.withPropertyValues("management.metrics.export.cloudwatch.namespace:test")
				.withUserConfiguration(ConfigurationWithCloudWatchClientConfiguration.class).run((context) -> {
					AmazonCloudWatchAsyncClient client = context.getBean(AmazonCloudWatchAsyncClient.class);

					// Assert
					ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(client,
							"clientConfiguration");
					assertThat(clientConfiguration.getProxyHost()).isEqualTo("cloudWatch");
				});
	}

	@Test
	void configuration_withGlobalAndCloudWatchClientConfigurations_shouldUseCloudWatchConfigurationForClient() {
		// Arrange & Act
		this.contextRunner.withPropertyValues("management.metrics.export.cloudwatch.namespace:test")
				.withUserConfiguration(ConfigurationWithGlobalAndCloudWatchClientConfiguration.class).run((context) -> {
					AmazonCloudWatchAsyncClient client = context.getBean(AmazonCloudWatchAsyncClient.class);

					// Assert
					ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(client,
							"clientConfiguration");
					assertThat(clientConfiguration.getProxyHost()).isEqualTo("cloudWatch");
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithGlobalClientConfiguration {

		@Bean(name = GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME)
		ClientConfiguration globalClientConfiguration() {
			return new ClientConfiguration().withProxyHost("global");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithCloudWatchClientConfiguration {

		@Bean
		ClientConfiguration cloudWatchClientConfiguration() {
			return new ClientConfiguration().withProxyHost("cloudWatch");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithGlobalAndCloudWatchClientConfiguration {

		@Bean
		ClientConfiguration cloudWatchClientConfiguration() {
			return new ClientConfiguration().withProxyHost("cloudWatch");
		}

		@Bean(name = GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME)
		ClientConfiguration globalClientConfiguration() {
			return new ClientConfiguration().withProxyHost("global");
		}

	}

}
