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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Integration tests for {@link CloudWatchMetricPublisherAutoConfiguration}.
 *
 * @author Sharang Gupta
 */
@Testcontainers
class CloudWatchMetricPublisherIntegrationTests {

	private static final String NAMESPACE = "Orders/AwsSdk";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:4.14.0"));

	@Test
	void publishesTheMetricsOfTheClientsWhenTheContextCloses() {
		new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.aws.endpoint:" + localstack.getEndpoint(),
						"spring.cloud.aws.region.static:us-east-1", "spring.cloud.aws.credentials.access-key:noop",
						"spring.cloud.aws.credentials.secret-key:noop",
						"spring.cloud.aws.client.metrics.namespace:" + NAMESPACE)
				.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
						CredentialsProviderAutoConfiguration.class, AwsAutoConfiguration.class,
						CloudWatchMetricPublisherAutoConfiguration.class, SnsAutoConfiguration.class))
				.run(context -> context.getBean(SnsClient.class).listTopics());

		// closing the context closed the publisher, which uploaded the metrics it had
		// collected
		try (CloudWatchClient cloudWatch = CloudWatchClient.builder().endpointOverride(localstack.getEndpoint())
				.region(Region.US_EAST_1)
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("noop", "noop")))
				.build()) {
			assertThat(cloudWatch.listMetrics(request -> request.namespace(NAMESPACE)).metrics())
					.extracting(Metric::metricName).contains("ApiCallDuration");
		}
	}

}
