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

package io.awspring.cloud.sqs.sample;

import java.io.IOException;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import io.awspring.cloud.test.sqs.SqsTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@SqsTest
@Testcontainers
class SqsSampleApplicationTests {

	private static final String QUEUE_NAME = "InfrastructureStack-spring-aws";

	// create an SQS locally running equivalent with Localstack and Testcontainers
	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.0")).withServices(SQS);

	@Autowired
	private QueueMessagingTemplate queueMessagingTemplate;

	@SpyBean
	private SampleListener sampleListener;

	@BeforeAll
	static void beforeAll() throws IOException, InterruptedException {
		// create needed queues in SQS
		localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME);
	}

	@Test
	void receivesMessage() {
		// send a message
		queueMessagingTemplate.send(QUEUE_NAME, MessageBuilder.withPayload("hello").build());

		// verify that bean handling the message was invoked
		await().untilAsserted(() -> verify(sampleListener).listenToMessage("hello"));
	}

	@DynamicPropertySource
	static void registerPgProperties(DynamicPropertyRegistry registry) {
		// overwrite SQS endpoint with one provided by Localstack
		registry.add("cloud.aws.sqs.endpoint", () -> localstack.getEndpointOverride(SQS).toString());
	}

}
