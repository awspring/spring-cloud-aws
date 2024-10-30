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

import static io.awspring.cloud.test.sqs.SqsSampleListener.QUEUE_NAME;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
abstract class BaseSqsIntegrationTest {

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:3.8.1"));

	@BeforeAll
	static void beforeAll() throws IOException, InterruptedException {
		// create needed queues in SQS
		localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME);
	}

	@DynamicPropertySource
	static void registerSqsProperties(DynamicPropertyRegistry registry) {
		// overwrite SQS endpoint with one provided by LocalStack
		registry.add("spring.cloud.aws.sqs.endpoint", () -> localstack.getEndpoint());
	}

}
