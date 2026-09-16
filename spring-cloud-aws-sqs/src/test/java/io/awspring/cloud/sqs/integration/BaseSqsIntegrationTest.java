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

import io.awspring.cloud.sqs.CompletableFutures;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

@Testcontainers(disabledWithoutDocker = true)
abstract class BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(BaseSqsIntegrationTest.class);

	protected static final boolean useLocalStackClient = true;

	protected static boolean purgeQueues = false;

	protected static boolean waitForPurge = false;

	private static final String LOCAL_STACK_VERSION = "localstack/localstack:4.4.0";

	static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse(LOCAL_STACK_VERSION));

	static StaticCredentialsProvider credentialsProvider;

	private static final CompletableFuture<Void> STARTED = new CompletableFuture<>();

	private static final AtomicBoolean STARTING = new AtomicBoolean();

	/**
	 * Starts the container off the JUnit worker threads. The workers block in {@link #beforeAll()} until it is up, and
	 * a blocking wait does not make the pool compensate with another thread, so starting it from a test would hold as
	 * many workers as there are integration classes scheduled and leave the rest of the suite idle.
	 */
	static void startAsync() {
		if (!STARTING.compareAndSet(false, true)) {
			return;
		}
		Thread starter = new Thread(() -> {
			try {
				localstack.start();
				credentialsProvider = StaticCredentialsProvider
						.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));
				STARTED.complete(null);
			}
			catch (Throwable t) {
				STARTED.completeExceptionally(t);
			}
		}, "localstack-starter");
		starter.setDaemon(true);
		starter.start();
	}

	@BeforeAll
	static void beforeAll() {
		STARTED.join();
	}

	@DynamicPropertySource
	static void registerSqsProperties(DynamicPropertyRegistry registry) {
		// overwrite SQS endpoint with one provided by LocalStack
		registry.add("spring.cloud.aws.endpoint", () -> localstack.getEndpoint());
	}

	protected static CompletableFuture<?> createQueue(SqsAsyncClient client, String queueName) {
		return createQueue(client, queueName, Collections.emptyMap());
	}

	protected static CompletableFuture<?> createFifoQueue(SqsAsyncClient client, String queueName) {
		return createFifoQueue(client, queueName, Collections.emptyMap());
	}

	protected static CompletableFuture<?> createFifoQueue(SqsAsyncClient client, String queueName,
			Map<QueueAttributeName, String> additionalAttributes) {
		Map<QueueAttributeName, String> attributes = new HashMap<>(additionalAttributes);
		attributes.put(QueueAttributeName.FIFO_QUEUE, "true");
		return createQueue(client, queueName, attributes);
	}

	// A test class creates its queues in parallel, but only one class does so at a time. Localstack serves
	// creations from every class at a similar rate, so when all classes create at once each one's last queue lands
	// near the end of the whole batch and no class can start its tests before then. Giving a class exclusive access
	// lets it be ready in the time its own queues take, and start running while the next class creates its own.
	private static final Semaphore CREATION_SLOT = new Semaphore(1);

	private static final Map<Thread, AtomicInteger> PENDING_CREATIONS = new ConcurrentHashMap<>();

	protected static CompletableFuture<?> createQueue(SqsAsyncClient client, String queueName,
			Map<QueueAttributeName, String> attributes) {
		Thread owner = Thread.currentThread();
		AtomicInteger pending = PENDING_CREATIONS.computeIfAbsent(owner, thread -> new AtomicInteger());
		if (pending.getAndIncrement() == 0) {
			CREATION_SLOT.acquireUninterruptibly();
		}
		return doCreateQueue(client, queueName, attributes).whenComplete((v, t) -> {
			if (pending.decrementAndGet() == 0) {
				PENDING_CREATIONS.remove(owner);
				CREATION_SLOT.release();
			}
		});
	}

	private static CompletableFuture<?> doCreateQueue(SqsAsyncClient client, String queueName,
			Map<QueueAttributeName, String> attributes) {
		logger.debug("Creating queue {} with attributes {}", queueName, attributes);
		return client.createQueue(req -> getCreateQueueRequest(queueName, attributes, req)).handle((v, t) -> {
			if (t != null) {
				logger.error("Error creating queue {} with attributes {}", queueName, attributes, t);
				return CompletableFutures.failedFuture(t);
			}
			if (purgeQueues) {
				String queueUrl = v.queueUrl();
				logger.debug("Purging queue {}", queueName);
				return client.purgeQueue(req -> req.queueUrl(queueUrl).build()).thenRun(() -> {
					if (waitForPurge) {
						logger.info("Waiting 30000 seconds to start sending.");
						sleep(30000);
						logger.info("Done waiting.");
					}
				});
			}
			else {
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

	private static void sleep(int time) {
		try {
			Thread.sleep(time);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while sleeping");
		}
	}

	private static CreateQueueRequest getCreateQueueRequest(String queueName,
			Map<QueueAttributeName, String> attributes, CreateQueueRequest.Builder builder) {
		if (!attributes.isEmpty()) {
			builder.attributes(attributes);
		}
		return builder.queueName(queueName).build();
	}

	protected static SqsAsyncClient createAsyncClient() {
		return useLocalStackClient ? createLocalStackClient() : SqsAsyncClient.builder().build();
	}

	protected static SqsAsyncClient createHighThroughputAsyncClient() {
		return useLocalStackClient ? createLocalStackClient()
				: SqsAsyncClient.builder().httpClientBuilder(NettyNioAsyncHttpClient.builder().maxConcurrency(6000))
						.build();
	}

	private static SqsAsyncClient createLocalStackClient() {
		return SqsAsyncClient.builder().credentialsProvider(credentialsProvider)
				.endpointOverride(localstack.getEndpoint()).region(Region.of(localstack.getRegion())).build();
	}

	protected static class LoadSimulator {

		private static final Random RANDOM = new Random();

		private int bound = 1000;

		private boolean loadEnabled = false;

		private boolean random = false;

		public void runLoad() {
			runLoad(this.bound);
		}

		public void runLoad(int amount) {
			if (!this.loadEnabled) {
				return;
			}
			try {
				Thread.sleep(getLoadTime(amount));
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		}

		private long getLoadTime(int amount) {
			return this.random ? RANDOM.nextInt(amount) : amount;
		}

		public LoadSimulator setBound(int bound) {
			this.bound = bound;
			return this;
		}

		public LoadSimulator setLoadEnabled(boolean loadEnabled) {
			this.loadEnabled = loadEnabled;
			return this;
		}

		public LoadSimulator setRandom(boolean random) {
			this.random = random;
			return this;
		}

		@Override
		public String toString() {
			if (!this.loadEnabled) {
				return "no load";
			}
			StringBuilder sb = new StringBuilder();
			if (this.random) {
				sb.append("random load of up to ");
			}
			else {
				sb.append("load of ");
			}
			sb.append(this.bound).append("ms");
			return sb.toString();
		}
	}

}
