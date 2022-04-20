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
package io.awspring.cloud.autoconfigure.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Test for the {@link CloudWatchExportAutoConfiguration}.
 *
 * @author Dawid Kublik
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 */
class CloudWatchExportAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, CloudWatchExportAutoConfiguration.class));

	@Test
	void testWithoutSettingAnyConfigProperties() {
		this.contextRunner
				.run(context -> assertThat(context.getBeansOfType(CloudWatchMeterRegistry.class).isEmpty()).isTrue());
	}

	@Test
	void enableAutoConfigurationSettingNamespace() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.cloudwatch.namespace:test").run(context -> {
			CloudWatchMeterRegistry metricsExporter = context.getBean(CloudWatchMeterRegistry.class);
			assertThat(metricsExporter).isNotNull();

			CloudWatchConfig cloudWatchConfig = context.getBean(CloudWatchConfig.class);
			assertThat(cloudWatchConfig).isNotNull();

			Clock clock = context.getBean(Clock.class);
			assertThat(clock).isNotNull();

			CloudWatchProperties cloudWatchProperties = context.getBean(CloudWatchProperties.class);
			assertThat(cloudWatchProperties).isNotNull();

			assertThat(cloudWatchProperties.getNamespace()).isEqualTo("test");
		});
	}

	@Test
	void enableAutoConfigurationWithSpecificRegion() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.cloudwatch.namespace:test",
				"spring.cloud.aws.cloudwatch.region:us-east-1").run(context -> {
					CloudWatchMeterRegistry metricsExporter = context.getBean(CloudWatchMeterRegistry.class);
					assertThat(metricsExporter).isNotNull();

					CloudWatchConfig cloudWatchConfig = context.getBean(CloudWatchConfig.class);
					assertThat(cloudWatchConfig).isNotNull();

					Clock clock = context.getBean(Clock.class);
					assertThat(clock).isNotNull();

					CloudWatchProperties cloudWatchProperties = context.getBean(CloudWatchProperties.class);
					assertThat(cloudWatchProperties).isNotNull();

					assertThat(cloudWatchProperties.getNamespace()).isEqualTo(cloudWatchConfig.namespace());

					CloudWatchAsyncClient client = context.getBean(CloudWatchAsyncClient.class);

					SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils
							.getField(client, "clientConfiguration");
					AttributeMap attributes = (AttributeMap) ReflectionTestUtils.getField(clientConfiguration,
							"attributes");
					assertThat(attributes.get(AwsClientOption.AWS_REGION)).isEqualTo(Region.US_EAST_1);
				});
	}

	@Test
	void enableAutoConfigurationWithCustomEndpoint() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.cloudwatch.namespace:test",
				"spring.cloud.aws.cloudwatch.endpoint:http://localhost:8090").run(context -> {
					CloudWatchAsyncClient client = context.getBean(CloudWatchAsyncClient.class);

					SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils
							.getField(client, "clientConfiguration");
					AttributeMap attributes = (AttributeMap) ReflectionTestUtils.getField(clientConfiguration,
							"attributes");
					assertThat(attributes.get(SdkClientOption.ENDPOINT)).isEqualTo(URI.create("http://localhost:8090"));
					assertThat(attributes.get(SdkClientOption.ENDPOINT_OVERRIDDEN)).isTrue();
				});
	}

}
