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
package io.awspring.cloud.sqs;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

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

	protected static final String RECEIVES_MESSAGE_QUEUE_NAME = "receives.message.test.queue";
	protected static final String DOES_NOT_ACK_ON_ERROR_QUEUE_NAME = "does.not.ack.test.queue";
	protected static final String RESOLVES_PARAMETER_TYPES_QUEUE_NAME = "resolves.parameter.test.queue";
	protected static final String RESOLVES_POJO_TYPES_QUEUE_NAME = "resolves.pojo.test.queue";
	protected static final String RECEIVE_FROM_MANY_1_QUEUE_NAME = "receive.many.test.queue.1";
	protected static final String RECEIVE_FROM_MANY_2_QUEUE_NAME = "receive.many.test.queue.2";
	protected static final String ASYNC_RECEIVE_FROM_MANY_1_QUEUE_NAME = "async.receive.many.test.queue.1";
	protected static final String ASYNC_RECEIVE_FROM_MANY_2_QUEUE_NAME = "async.receive.many.test.queue.2";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.0")).withServices(SQS).withReuse(false);

	@BeforeAll
	static void beforeAll() throws IOException, InterruptedException {
		// create needed queues in SQS
		// TODO: Not working as expected due to some port mapping issue - will look into in the future
		localstack.execInContainer("awslocal", "io/awspring/cloud/sqs", "create-queue", "--queue-name",
				RECEIVES_MESSAGE_QUEUE_NAME);
		localstack.execInContainer("awslocal", "io/awspring/cloud/sqs", "create-queue", "--queue-name",
				DOES_NOT_ACK_ON_ERROR_QUEUE_NAME);
		localstack.execInContainer("awslocal", "io/awspring/cloud/sqs", "create-queue", "--queue-name",
				RECEIVE_FROM_MANY_1_QUEUE_NAME);
	}

	@DynamicPropertySource
	static void registerSqsProperties(DynamicPropertyRegistry registry) {
		// overwrite SQS endpoint with one provided by Localstack
		registry.add("cloud.aws.sqs.endpoint", () -> localstack.getEndpointOverride(SQS).toString());
	}

}
