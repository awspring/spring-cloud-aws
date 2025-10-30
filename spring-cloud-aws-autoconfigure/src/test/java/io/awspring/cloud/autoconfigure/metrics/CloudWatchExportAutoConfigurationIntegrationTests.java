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

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * Integration tests for {@link CloudWatchExportAutoConfiguration}.
 *
 * @author Eddú Meléndez
 */
@Testcontainers
class CloudWatchExportAutoConfigurationIntegrationTests {

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:4.4.0"));

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.cloud.aws.cloudwatch.endpoint", () -> localstack.getEndpoint());
	}

	@Test
	void testCounter() {
		SpringApplication application = new SpringApplication(
				CloudWatchExportAutoConfigurationIntegrationTests.Application.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=us-east-1", "--management.cloudwatch.metrics.export.step=5s",
				"--management.cloudwatch.metrics.export.namespace=awspring/spring-cloud-aws",
				"--management.metrics.enable.all=false", "--management.metrics.enable.test=true")) {

			MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
			CloudWatchAsyncClient cloudWatchAsyncClient = context.getBean(CloudWatchAsyncClient.class);

			Instant startTime = Instant.now();
			Instant endTime = startTime.plus(Duration.ofSeconds(30));

			Counter testCounter = Counter.builder("test").tag("app", "sample").register(meterRegistry);
			testCounter.increment();

			Dimension dimension = Dimension.builder().name("app").value("sample").build();
			Metric metric = Metric.builder().namespace("awspring/spring-cloud-aws").metricName("test.count")
					.dimensions(dimension).build();
			MetricStat metricStat = MetricStat.builder().stat("Maximum").metric(metric).unit(StandardUnit.COUNT)
					.period(5).build();
			MetricDataQuery metricDataQuery = MetricDataQuery.builder().metricStat(metricStat).id("test1")
					.returnData(true).build();
			await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> {
				GetMetricDataResponse response = cloudWatchAsyncClient.getMetricData(GetMetricDataRequest.builder()
						.startTime(startTime).endTime(endTime).metricDataQueries(metricDataQuery).build()).get();
				assertThat(response.metricDataResults()).hasSize(1);
				assertThat(response.metricDataResults().get(0).values()).contains(1d);
			});
		}
	}

	@SpringBootApplication
	@AutoConfigureObservability(tracing = false)
	static class Application {

	}
}
