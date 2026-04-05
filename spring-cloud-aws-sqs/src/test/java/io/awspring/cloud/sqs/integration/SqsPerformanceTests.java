/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sqs.integration.SqsPerformanceHarness.RunParameters;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Performance comparison tests for SQS message processing under different container configurations.
 *
 * <p>
 * These tests measure relative throughput — assertions compare configurations against each other rather than against
 * absolute thresholds, making them valid across different environments.
 *
 * <p>
 * Disabled by default. Run with {@code mvn test -Dperformance=true -Dtest=SqsPerformanceTests}.
 *
 * <p>
 * By default tests run against LocalStack. To run against AWS, set {@code useLocalStackClient = false} in
 * {@link BaseSqsIntegrationTest}.
 *
 * @author Tomaz Fernandes
 * @since 4.1.0
 */
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "performance", matches = "true")
class SqsPerformanceTests extends BaseSqsIntegrationTest {

	private static final String Q = "perf-test-";

	private static SqsPerformanceHarness harness;

	@BeforeAll
	static void setupHarness() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(createQueue(client, Q + "default"), createQueue(client, Q + "virtual"),
				createQueue(client, Q + "default-loaded"), createQueue(client, Q + "virtual-loaded"),
				createQueue(client, Q + "default-500"), createQueue(client, Q + "virtual-500"),
				createQueue(client, Q + "virtual-10k"), createQueue(client, Q + "vt-interceptor")).join();
		harness = new SqsPerformanceHarness(client);
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void virtualThreadsShouldMatchDefaultPerformance() {
		var defaultResult = harness.run(RunParameters.builder().queueName(Q + "default").messageCount(200).build());
		var virtualResult = harness.run(RunParameters.builder().queueName(Q + "virtual").messageCount(200).build(),
				builder -> builder.configure(options -> options.componentsTaskExecutor(virtualThreadExecutor())));

		defaultResult.report("Default (Platform Threads)");
		virtualResult.report("Virtual Threads");

		assertThat(virtualResult.messagesPerSecond()).as("Virtual threads should be within 80%% of default throughput")
				.isGreaterThan(defaultResult.messagesPerSecond() * 0.8);
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void virtualThreadsShouldMatchDefaultPerformanceUnderLoad() {
		var defaultResult = harness.run(RunParameters.builder().queueName(Q + "default-loaded").messageCount(50)
				.processingDelay(Duration.ofSeconds(1)).build());
		var virtualResult = harness.run(
				RunParameters.builder().queueName(Q + "virtual-loaded").messageCount(50)
						.processingDelay(Duration.ofSeconds(1)).build(),
				builder -> builder.configure(options -> options.componentsTaskExecutor(virtualThreadExecutor())));

		defaultResult.report("Default (Platform Threads) - 1s load");
		virtualResult.report("Virtual Threads - 1s load");

		assertThat(virtualResult.messagesPerSecond())
				.as("Virtual threads with load should be within 80%% of default throughput")
				.isGreaterThan(defaultResult.messagesPerSecond() * 0.8);
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void virtualThreadsShouldScaleWithHighConcurrencyUnderLoad() {
		var defaultResult = harness.run(
				RunParameters.builder().queueName(Q + "default-500").messageCount(500)
						.processingDelay(Duration.ofSeconds(1)).build(),
				builder -> builder.configure(options -> options.maxConcurrentMessages(500)));
		var virtualResult = harness.run(
				RunParameters.builder().queueName(Q + "virtual-500").messageCount(500)
						.processingDelay(Duration.ofSeconds(1)).build(),
				builder -> builder.configure(
						options -> options.maxConcurrentMessages(500).componentsTaskExecutor(virtualThreadExecutor())));

		defaultResult.report("Default (Platform Threads) - 500 concurrent, 1s load");
		virtualResult.report("Virtual Threads - 500 concurrent, 1s load");

		assertThat(virtualResult.messagesPerSecond())
				.as("Virtual threads should be at least as fast as platform threads at high concurrency")
				.isGreaterThan(defaultResult.messagesPerSecond() * 0.8);
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void virtualThreadsShouldHandleTenThousandConcurrentMessages() {
		var params = RunParameters.builder().queueName(Q + "virtual-10k").messageCount(10_000)
				.timeout(Duration.ofSeconds(60)).processingDelay(Duration.ofSeconds(1)).build();

		var result = harness.run(params, builder -> builder.configure(
				options -> options.maxConcurrentMessages(10_000).componentsTaskExecutor(virtualThreadExecutor())));

		result.report("Virtual Threads - 10k concurrent, 1s load");

		assertThat(result.messagesPerSecond()).as("Should process at high throughput with 10k virtual threads")
				.isGreaterThan(100);
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void virtualThreadsShouldWorkWithBlockingInterceptor() {
		var params = RunParameters.builder().queueName(Q + "vt-interceptor").messageCount(50)
				.timeout(Duration.ofSeconds(60)).processingDelay(Duration.ofSeconds(1)).build();

		var result = harness.run(params,
				builder -> builder.configure(options -> options.componentsTaskExecutor(virtualThreadExecutor()))
						.messageInterceptor(blockingInterceptor(Duration.ofMillis(200))));

		result.report("Virtual Threads - blocking interceptor (200ms) + 1s listener load");

		assertThat(result.messagesPerSecond()).isGreaterThan(0);
		assertThat(result.p50()).as("Latency should reflect interceptor + listener delay").isGreaterThanOrEqualTo(1200);
	}

	private static SimpleAsyncTaskExecutor virtualThreadExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setVirtualThreads(true);
		return executor;
	}

	private static io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor<Object> blockingInterceptor(
			Duration delay) {
		return new io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor<>() {
			@Override
			public org.springframework.messaging.Message<Object> intercept(
					org.springframework.messaging.Message<Object> message) {
				try {
					Thread.sleep(delay.toMillis());
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return message;
			}
		};
	}

}
