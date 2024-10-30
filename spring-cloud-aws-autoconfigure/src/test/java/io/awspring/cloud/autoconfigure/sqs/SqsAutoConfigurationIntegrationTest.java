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
package io.awspring.cloud.autoconfigure.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.sqs.QueueAttributesResolvingException;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.bouncycastle.util.Arrays;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

/**
 * Integration tests for {@link SqsAutoConfiguration}.
 *
 * @author Tomaz Fernandes
 * @author Wei Jiang
 */
@SpringBootTest
@Testcontainers
class SqsAutoConfigurationIntegrationTest {

	private static final String QUEUE_NAME = "my_queue_name";

	private static final String PAYLOAD = "Test";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:3.8.1"));

	static {
		localstack.start();
	}

	private static final String[] BASE_PARAMS = { "spring.cloud.aws.sqs.region=eu-west-1",
			"spring.cloud.aws.sqs.endpoint=" + localstack.getEndpoint(), "spring.cloud.aws.credentials.access-key=noop",
			"spring.cloud.aws.credentials.secret-key=noop", "spring.cloud.aws.region.static=eu-west-1" };

	private static final AutoConfigurations BASE_CONFIGURATIONS = AutoConfigurations.of(
			RegionProviderAutoConfiguration.class, CredentialsProviderAutoConfiguration.class,
			SqsAutoConfiguration.class, AwsAutoConfiguration.class, ListenerConfiguration.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues(BASE_PARAMS).withConfiguration(BASE_CONFIGURATIONS);

	private final ApplicationContextRunner applicationContextRunnerWithFailStrategy = new ApplicationContextRunner()
			.withPropertyValues(Arrays.append(BASE_PARAMS, "spring.cloud.aws.sqs.queue-not-found-strategy=fail"))
			.withConfiguration(BASE_CONFIGURATIONS);

	@SuppressWarnings("unchecked")
	@Test
	void sendsAndReceivesMessage() {
		this.contextRunner.run(context -> {
			SqsTemplate sqsTemplate = context.getBean(SqsTemplate.class);
			sqsTemplate.send(to -> to.queue(QUEUE_NAME).payload(PAYLOAD));
			CountDownLatch latch = context.getBean(CountDownLatch.class);
			assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
		});
	}

	@Test
	void containerReceivesMessageWithFailQueueNotFoundStrategy() {
		applicationContextRunnerWithFailStrategy.run(context -> assertThatThrownBy(
				() -> context.getBean(SqsTemplate.class).send(to -> to.queue("QUEUE_DOES_NOT_EXISTS").payload(PAYLOAD)))
				.isInstanceOf(IllegalStateException.class).cause().isInstanceOf(ApplicationContextException.class)
				.cause().isInstanceOf(CompletionException.class).cause()
				.isInstanceOf(QueueAttributesResolvingException.class).cause()
				.isInstanceOf(QueueDoesNotExistException.class));
	}

	@Test
	void templateReceivesMessageWithFailQueueNotFoundStrategy() {
		applicationContextRunnerWithFailStrategy.run(context -> assertThatThrownBy(
				() -> context.getBean(SqsTemplate.class).receive("QUEUE_DOES_NOT_EXIST", String.class))
				.isInstanceOf(IllegalStateException.class).cause().isInstanceOf(ApplicationContextException.class)
				.cause().isInstanceOf(CompletionException.class).cause()
				.isInstanceOf(QueueAttributesResolvingException.class).cause()
				.isInstanceOf(QueueDoesNotExistException.class));
	}

	static class Listener {

		@Autowired
		CountDownLatch messageLatch;

		@SqsListener(QUEUE_NAME)
		void listen(String message) {
			assertThat(message).isEqualTo(PAYLOAD);
			messageLatch.countDown();
		}
	}

	@Configuration
	static class ListenerConfiguration {

		@Bean
		CountDownLatch messageLatch() {
			return new CountDownLatch(1);
		}

		@Bean
		Listener listener() {
			return new Listener();
		}
	}

}
