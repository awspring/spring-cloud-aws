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

package io.awspring.cloud.test.sqs;

import java.io.IOException;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static io.awspring.cloud.test.sqs.SqsSampleListener.QUEUE_NAME;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@SqsTest(listeners = SqsSampleListener.class)
@Testcontainers
class SqsTestListenersDefinedTest {

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.0")).withServices(SQS).withReuse(true);

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private QueueMessagingTemplate queueMessagingTemplate;

	@MockBean
	private SampleComponent sampleComponent;

	@BeforeAll
	static void beforeAll() throws IOException, InterruptedException {
		// create needed queues in SQS
		localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME);
	}

	@DynamicPropertySource
	static void registerSqsProperties(DynamicPropertyRegistry registry) {
		// overwrite SQS endpoint with one provided by Localstack
		registry.add("cloud.aws.sqs.endpoint", () -> localstack.getEndpointOverride(SQS).toString());
	}

	@Test
	void createsQueueMessagingTemplate() {
		assertThatNoException().isThrownBy(() -> this.ctx.getBean(QueueMessagingTemplate.class));
	}

	@Test
	void createsListener() {
		assertThatNoException().isThrownBy(() -> this.ctx.getBean(SqsSampleListener.class));
	}

	@Test
	void listenerHandlesMessage() {
		queueMessagingTemplate.convertAndSend(QUEUE_NAME, "message");

		await().untilAsserted(() -> verify(sampleComponent).save("message"));
	}

}
