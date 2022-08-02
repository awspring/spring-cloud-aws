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

import com.amazonaws.auth.AWSCredentials;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

@Testcontainers
abstract class BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(BaseSqsIntegrationTest.class);

	protected static final boolean useLocalStackClient = true;

	protected static final boolean purgeQueues = false;

	private static final String LOCAL_STACK_VERSION = "localstack/localstack:1.0.3";

	private static final Object beforeAllMonitor = new Object();

	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse(LOCAL_STACK_VERSION)).withServices(SQS).withReuse(true);

	static StaticCredentialsProvider credentialsProvider;

	@BeforeAll
	static void beforeAll() {
		synchronized (beforeAllMonitor) {
			if (!localstack.isRunning()) {
				localstack.start();
				AWSCredentials localstackCredentials = localstack.getDefaultCredentialsProvider().getCredentials();
				credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials
					.create(localstackCredentials.getAWSAccessKeyId(), localstackCredentials.getAWSSecretKey()));
			}
		}
	}

	@DynamicPropertySource
	static void registerSqsProperties(DynamicPropertyRegistry registry) {
		// overwrite SQS endpoint with one provided by Localstack
		registry.add("spring.cloud.aws.endpoint", () -> localstack.getEndpointOverride(SQS).toString());
	}

	protected static CompletableFuture<?> createQueue(SqsAsyncClient client, String queueName) {
		return createQueue(client, queueName, Collections.emptyMap());
	}

	protected static CompletableFuture<?> createFifoQueue(SqsAsyncClient client, String queueName) {
		return createFifoQueue(client, queueName, Collections.emptyMap());
	}

	protected static CompletableFuture<?> createFifoQueue(SqsAsyncClient client, String queueName, Map<QueueAttributeName, String> additionalAttributes) {
		Map<QueueAttributeName, String> attributes = new HashMap<>(additionalAttributes);
		attributes.put(QueueAttributeName.FIFO_QUEUE, "true");
		return createQueue(client, queueName, attributes);
	}

	protected static CompletableFuture<?> createQueue(SqsAsyncClient client, String queueName, Map<QueueAttributeName, String> attributes) {
		logger.debug("Creating queue {} with attributes {}", queueName, attributes);
		return client.createQueue(req -> getCreateQueueRequest(queueName, attributes, req))
			.handle((v, t) -> {
				if (t != null) {
					logger.error("Error creating queue {} with attributes {}", queueName, attributes, t);
					return CompletableFutures.failedFuture(t);
				}
				if (purgeQueues) {
					String queueUrl = v.queueUrl();
					logger.debug("Purging queue {}", queueName);
					return client.purgeQueue(req -> req.queueUrl(queueUrl).build());
				} else {
					logger.debug("Skipping purge for queue {}", queueName);
					return CompletableFuture.completedFuture(null);
				}
			}).thenCompose(x -> x).whenComplete((v, t) -> {
				if (t != null) {
					logger.error("Error purging queue {}", queueName, t);
					return;
				}
				logger.debug("Done purging queue {}", queueName);
			});
	}

	private static CreateQueueRequest getCreateQueueRequest(String queueName, Map<QueueAttributeName, String> attributes, CreateQueueRequest.Builder builder) {
		if (!attributes.isEmpty()) {
			builder.attributes(attributes);
		}
		return builder
			.queueName(queueName)
			.build();
	}

	protected static SqsAsyncClient createAsyncClient() {
		return useLocalStackClient
					? createLocalStackClient()
					: SqsAsyncClient.builder().build();
	}

	protected static SqsAsyncClient createLocalStackClient() {
		return SqsAsyncClient.builder()
			.credentialsProvider(credentialsProvider)
			.endpointOverride(localstack.getEndpointOverride(SQS)).region(Region.of(localstack.getRegion()))
			.build();
	}

	protected static SqsAsyncClient createHighThroughputAsyncClient() {
		return useLocalStackClient
			? createLocalStackClient()
			: SqsAsyncClient.builder().httpClientBuilder(NettyNioAsyncHttpClient.builder()
				//.maxConcurrency(6000)
			)
			.build();
	}

	protected static class Sleeper {

		private static final Random RANDOM = new Random();

		private int bound = 1000;

		private boolean sleepEnabled = false;

		private boolean random = true;

		public void sleep() {
			sleep(this.bound);
		}
		public void sleep(int amount) {
			if (!sleepEnabled) {
				return;
			}
			try {
				Thread.sleep(getSleepTime(amount));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		}

		private long getSleepTime(int amount) {
			return this.random
				? RANDOM.nextInt(amount)
				: amount;
		}

		public Sleeper setBound(int bound) {
			this.bound = bound;
			return this;
		}

		public Sleeper setSleepEnabled(boolean sleepEnabled) {
			this.sleepEnabled = sleepEnabled;
			return this;
		}

		public Sleeper setRandom(boolean random) {
			this.random = random;
			return this;
		}
	}

}
