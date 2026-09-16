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
package io.awspring.cloud.autoconfigure.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedAsyncClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBeanNames;
import io.awspring.cloud.sqs.listener.DefaultListenerContainerRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.shaded.org.bouncycastle.util.Arrays;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

/**
 * Integration tests for SQS listener startup.
 *
 * @author Bruno Garcia
 */
@Testcontainers
@SpringBootTest
class SqsListenerContainerStartupIntegrationTest {

	private static final String EXISTING_QUEUE_NAME = "messaging-greetings-notifications";

	private static final String MISSING_QUEUE_NAME = "not-existing-queue";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:4.4.0"));

	static {
		localstack.start();
	}

	private static final String[] BASE_PARAMS = { "spring.cloud.aws.sqs.region=eu-west-1",
			"spring.cloud.aws.sqs.endpoint=" + localstack.getEndpoint(), "spring.cloud.aws.credentials.access-key=noop",
			"spring.cloud.aws.credentials.secret-key=noop", "spring.cloud.aws.region.static=eu-west-1" };

	private static final AutoConfigurations BASE_CONFIGURATIONS = AutoConfigurations.of(
			RegionProviderAutoConfiguration.class, CredentialsProviderAutoConfiguration.class,
			SqsAutoConfiguration.class, AwsAutoConfiguration.class, ExistingAndMissingQueueListenerConfiguration.class);

	private final ApplicationContextRunner applicationContextRunnerWithFailStrategy = new ApplicationContextRunner()
			.withClassLoader(new FilteredClassLoader(AmazonSQSExtendedAsyncClient.class))
			.withPropertyValues(Arrays.append(BASE_PARAMS, "spring.cloud.aws.sqs.queue-not-found-strategy=fail"))
			.withConfiguration(BASE_CONFIGURATIONS);

	@Test
	void stopsRegistryWhenOneSqsListenerFailsToResolveQueueOnStartup() {
		createQueue(EXISTING_QUEUE_NAME);
		TrackingListenerContainerRegistry registry = new TrackingListenerContainerRegistry();

		applicationContextRunnerWithFailStrategy.withBean(SqsBeanNames.ENDPOINT_REGISTRY_BEAN_NAME,
				TrackingListenerContainerRegistry.class, () -> registry).run(context -> {
					assertThat(context.getStartupFailure()).isInstanceOf(ApplicationContextException.class)
							.hasRootCauseInstanceOf(QueueDoesNotExistException.class);

					assertThat(registry.stopInvocations).hasValue(1);
					assertThat(registry.getListenerContainers()).hasSize(2)
							.allSatisfy(container -> assertThat(container.isRunning())
									.as("Container %s should be stopped", container.getId()).isFalse());
					assertThat(registry.isRunning()).isFalse();
				});
	}

	private static void createQueue(String queueName) {
		try (SqsAsyncClient client = SqsAsyncClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("noop", "noop")))
				.region(Region.EU_WEST_1).endpointOverride(localstack.getEndpoint()).build()) {
			client.createQueue(request -> request.queueName(queueName)).join();
		}
	}

	static class ExistingAndMissingQueueListener {

		@SqsListener(EXISTING_QUEUE_NAME)
		void listenToExistingQueue(String message) {
		}

		@SqsListener(MISSING_QUEUE_NAME)
		void listenToMissingQueue(String message) {
		}
	}

	@Configuration
	static class ExistingAndMissingQueueListenerConfiguration {

		@Bean
		ExistingAndMissingQueueListener existingAndMissingQueueListener() {
			return new ExistingAndMissingQueueListener();
		}
	}

	static class TrackingListenerContainerRegistry extends DefaultListenerContainerRegistry {

		private final AtomicInteger stopInvocations = new AtomicInteger();

		@Override
		public void stop() {
			this.stopInvocations.incrementAndGet();
			super.stop();
		}
	}

}
