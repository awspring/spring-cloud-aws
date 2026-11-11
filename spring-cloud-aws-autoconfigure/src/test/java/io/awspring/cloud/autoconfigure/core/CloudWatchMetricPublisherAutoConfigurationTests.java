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

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.sns.SnsAutoConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Tests for {@link CloudWatchMetricPublisherAutoConfiguration}.
 *
 * @author Sharang Gupta
 */
class CloudWatchMetricPublisherAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1",
					"spring.cloud.aws.credentials.access-key:noop", "spring.cloud.aws.credentials.secret-key:noop")
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, AwsAutoConfiguration.class,
					CloudWatchMetricPublisherAutoConfiguration.class, SnsAutoConfiguration.class));

	@Test
	void publishesTheMetricsOfAutoConfiguredClientsToCloudWatch() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(CloudWatchMetricPublisher.class);
			assertThat(metricPublishersOf(context.getBean(SnsClient.class)))
					.containsExactly(context.getBean(CloudWatchMetricPublisher.class));
		});
	}

	@Test
	void doesNotPublishTheMetricsOfItsOwnCloudWatchClient() {
		this.contextRunner.run(context -> {
			CloudWatchAsyncClient cloudWatchClient = context
					.getBean(CloudWatchMetricPublisherAutoConfiguration.MetricPublisherClientHolder.class)
					.cloudWatchClient();
			assertThat(cloudWatchClient.serviceClientConfiguration().overrideConfiguration().metricPublishers())
					.isEmpty();
		});
	}

	@Test
	void canBeDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.client.metrics.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(CloudWatchMetricPublisher.class);
			assertThat(metricPublishersOf(context.getBean(SnsClient.class))).isEmpty();
		});
	}

	@Test
	void backsOffWhenTheMetricPublisherIsNotOnTheClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(CloudWatchMetricPublisher.class)).run(context -> {
			assertThat(context).doesNotHaveBean(CloudWatchMetricPublisher.class);
			assertThat(metricPublishersOf(context.getBean(SnsClient.class))).isEmpty();
		});
	}

	@Test
	void bindsThePublisherProperties() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.client.metrics.namespace:Orders/AwsSdk",
				"spring.cloud.aws.client.metrics.maximum-calls-per-upload:5",
				"spring.cloud.aws.client.metrics.upload-frequency:30s",
				"spring.cloud.aws.client.metrics.metric-level:trace").run(context -> {
					ClientMetricsProperties properties = context.getBean(ClientMetricsProperties.class);
					assertThat(properties.getNamespace()).isEqualTo("Orders/AwsSdk");
					assertThat(properties.getMaximumCallsPerUpload()).isEqualTo(5);
					assertThat(properties.getUploadFrequency()).isEqualTo(Duration.ofSeconds(30));
					assertThat(properties.getMetricLevel()).isEqualTo(MetricLevel.TRACE);
				});
	}

	@Test
	void createsThePublisherOnlyWhenTheFirstClientIsConfigured() {
		new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.aws.credentials.access-key:noop",
						"spring.cloud.aws.credentials.secret-key:noop")
				.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
						CredentialsProviderAutoConfiguration.class, AwsAutoConfiguration.class,
						CloudWatchMetricPublisherAutoConfiguration.class))
				.withBean(AwsRegionProvider.class, () -> () -> {
					throw SdkClientException.create("No region");
				}).run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBeanFactory().containsSingleton("cloudWatchMetricPublisher")).isFalse();
				});
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	void publishesNoMetricsAndWarnsWhenThePublisherHasNoRegion(CapturedOutput output) {
		perServiceRegionContextRunner().run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(metricPublishersOf(context.getBean(SnsClient.class))).isEmpty();
			assertThat(output).contains("spring.cloud.aws.client.metrics.region");
		});
	}

	@Test
	void usesTheRegionOfThePublisherProperties() {
		perServiceRegionContextRunner().withPropertyValues("spring.cloud.aws.client.metrics.region:eu-central-1")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context
							.getBean(CloudWatchMetricPublisherAutoConfiguration.MetricPublisherClientHolder.class)
							.cloudWatchClient().serviceClientConfiguration().region()).isEqualTo(Region.EU_CENTRAL_1);
				});
	}

	private static ApplicationContextRunner perServiceRegionContextRunner() {
		return new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.aws.sns.region:eu-west-1",
						"spring.cloud.aws.credentials.access-key:noop", "spring.cloud.aws.credentials.secret-key:noop")
				.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
						CredentialsProviderAutoConfiguration.class, AwsAutoConfiguration.class,
						CloudWatchMetricPublisherAutoConfiguration.class, SnsAutoConfiguration.class))
				.withBean(AwsRegionProvider.class, () -> () -> {
					throw SdkClientException.create("No region");
				});
	}

	@Test
	void backsOffWhenTheApplicationDefinesItsOwnCloudWatchMetricPublisher() {
		this.contextRunner.withUserConfiguration(CustomCloudWatchMetricPublisherConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(CloudWatchMetricPublisher.class);
			assertThat(context)
					.doesNotHaveBean(CloudWatchMetricPublisherAutoConfiguration.MetricPublisherClientHolder.class);
			assertThat(metricPublishersOf(context.getBean(SnsClient.class)))
					.containsExactly(context.getBean(CloudWatchMetricPublisher.class));
		});
	}

	@Test
	void addsEveryMetricPublisherBeanToTheClients() {
		this.contextRunner.withUserConfiguration(CustomMetricPublisherConfiguration.class).run(context -> {
			assertThat(metricPublishersOf(context.getBean(SnsClient.class))).containsExactlyInAnyOrder(
					context.getBean(CloudWatchMetricPublisher.class), context.getBean("customMetricPublisher"));
		});
	}

	private static Object[] metricPublishersOf(SnsClient client) {
		return client.serviceClientConfiguration().overrideConfiguration().metricPublishers().toArray();
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomCloudWatchMetricPublisherConfiguration {

		@Bean
		CloudWatchMetricPublisher customCloudWatchMetricPublisher() {
			return CloudWatchMetricPublisher.builder()
					.cloudWatchClient(CloudWatchAsyncClient.builder().region(Region.EU_WEST_1).build()).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomMetricPublisherConfiguration {

		@Bean
		MetricPublisher customMetricPublisher() {
			return new MetricPublisher() {
				@Override
				public void publish(MetricCollection metricCollection) {
				}

				@Override
				public void close() {
				}
			};
		}

	}

}
