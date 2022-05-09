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
package io.awspring.cloud.autoconfigure.sqs.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Integration tests for SQS.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
@SpringBootTest
@Testcontainers
class SqsAutoConfigurationIntegrationTest {

	private static final String REGION = "eu-west-1";

	private static final String QUEUE_NAME = "my_queue_name";

	private static final String PAYLOAD = "Test";

	private AnnotationConfigApplicationContext context;

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.2")).withServices(SQS).withReuse(true);

	@BeforeAll
	static void beforeAll() throws Exception {
		AnnotationConfigApplicationContext context = doLoad(null, "spring.cloud.aws.sqs.region=" + REGION,
				"spring.cloud.aws.sqs.endpoint=" + localstack.getEndpointOverride(SQS).toString(),
				"spring.cloud.aws.credentials.access-key=noop", "spring.cloud.aws.credentials.secret-key=noop",
				"spring.cloud.aws.region.static=eu-west-1");
		SqsAsyncClient sqsAsyncClient = context.getBean(SqsAsyncClient.class);
		sqsAsyncClient.createQueue(req -> req.queueName(QUEUE_NAME)).get();
	}

	@Test
	void sendsAndReceivesMessage() throws Exception {
		load(ListenerConfiguration.class, "spring.cloud.aws.sqs.region=" + REGION,
				"spring.cloud.aws.sqs.endpoint=" + localstack.getEndpointOverride(SQS).toString(),
				"spring.cloud.aws.credentials.access-key=noop", "spring.cloud.aws.credentials.secret-key=noop",
				"spring.cloud.aws.region.static=eu-west-1");
		SqsAsyncClient sqsAsyncClient = context.getBean(SqsAsyncClient.class);
		String queueUrl = sqsAsyncClient.getQueueUrl(req -> req.queueName(QUEUE_NAME)).get().queueUrl();
		sqsAsyncClient.sendMessage(req -> req.queueUrl(queueUrl).messageBody(PAYLOAD));
		CountDownLatch latch = context.getBean(CountDownLatch.class);
		assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(new Class<?>[] { config }, environment);
	}

	private static AnnotationConfigApplicationContext doLoad(Class<?>[] configs, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		if (configs != null) {
			applicationContext.register(configs);
		}
		applicationContext.register(AwsAutoConfiguration.class);
		applicationContext.register(CredentialsProviderAutoConfiguration.class);
		applicationContext.register(RegionProviderAutoConfiguration.class);
		applicationContext.register(SqsAutoConfiguration.class);
		TestPropertyValues.of(environment).applyTo(applicationContext);
		applicationContext.refresh();
		return applicationContext;
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
