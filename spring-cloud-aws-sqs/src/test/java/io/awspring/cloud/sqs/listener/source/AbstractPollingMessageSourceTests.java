/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sqs.listener.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.awspring.cloud.sqs.MessageExecutionThreadFactory;
import io.awspring.cloud.sqs.listener.BackPressureMode;
import io.awspring.cloud.sqs.listener.SemaphoreBackPressureHandler;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * @author Tomaz Fernandes
 */
class AbstractPollingMessageSourceTests {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPollingMessageSourceTests.class);

	@Test
	void shouldAcquireAndReleaseFullPermits() {
		String testName = "shouldAcquireAndReleaseFullPermits";

		SemaphoreBackPressureHandler backPressureHandler = SemaphoreBackPressureHandler.builder()
				.acquireTimeout(Duration.ofMillis(200)).batchSize(10).totalPermits(10)
				.throughputConfiguration(BackPressureMode.ALWAYS_POLL_MAX_MESSAGES).build();
		ExecutorService threadPool = Executors.newCachedThreadPool();
		CountDownLatch pollingCounter = new CountDownLatch(3);
		CountDownLatch processingCounter = new CountDownLatch(1);

		AbstractPollingMessageSource<Object, Message> source = new AbstractPollingMessageSource<>() {

			private final AtomicBoolean hasReceived = new AtomicBoolean(false);

			private final AtomicBoolean hasMadeSecondPoll = new AtomicBoolean(false);

			@Override
			protected CompletableFuture<Collection<Message>> doPollForMessages(int messagesToRequest) {
				return CompletableFuture.supplyAsync(() -> {
					try {
						// Since BackPressureMode.ALWAYS_POLL_MAX_MESSAGES, should always be 10.
						assertThat(messagesToRequest).isEqualTo(10);
						assertAvailablePermits(backPressureHandler, 0);
						boolean firstPoll = hasReceived.compareAndSet(false, true);
						if (firstPoll) {
							logger.debug("First poll");
							// No permits released yet, should be TM low
							assertThroughputMode(backPressureHandler, "low");
						}
						else if (hasMadeSecondPoll.compareAndSet(false, true)) {
							logger.debug("Second poll");
							// Permits returned, should be high
							assertThroughputMode(backPressureHandler, "high");
						}
						else {
							logger.debug("Third poll");
							// Already returned full permits, should be low
							assertThroughputMode(backPressureHandler, "low");
						}
						return firstPoll
								? (Collection<Message>) List.of(Message.builder()
										.messageId(UUID.randomUUID().toString()).body("message").build())
								: Collections.<Message> emptyList();
					}
					catch (Throwable t) {
						logger.error("Error", t);
						throw new RuntimeException(t);
					}
				}, threadPool).whenComplete((v, t) -> {
					if (t == null) {
						pollingCounter.countDown();
					}
				});
			}
		};

		source.setBackPressureHandler(backPressureHandler);
		source.setMessageSink((msgs, context) -> {
			assertAvailablePermits(backPressureHandler, 9);
			msgs.forEach(msg -> context.runBackPressureReleaseCallback());
			return CompletableFuture.runAsync(processingCounter::countDown);
		});

		source.setId(testName + " source");
		source.configure(SqsContainerOptions.builder().build());
		source.setTaskExecutor(createTaskExecutor(testName));
		source.setAcknowledgementProcessor(getAcknowledgementProcessor());
		source.start();
		assertThat(doAwait(pollingCounter)).isTrue();
		assertThat(doAwait(processingCounter)).isTrue();
	}

	private static final AtomicInteger testCounter = new AtomicInteger();

	@Test
	void shouldAcquireAndReleasePartialPermits() {
		String testName = "shouldAcquireAndReleasePartialPermits";
		SemaphoreBackPressureHandler backPressureHandler = SemaphoreBackPressureHandler.builder()
				.acquireTimeout(Duration.ofMillis(150)).batchSize(10).totalPermits(10)
				.throughputConfiguration(BackPressureMode.AUTO).build();
		ExecutorService threadPool = Executors
				.newCachedThreadPool(new MessageExecutionThreadFactory("test " + testCounter.incrementAndGet()));
		CountDownLatch pollingCounter = new CountDownLatch(4);
		CountDownLatch processingCounter = new CountDownLatch(1);
		CountDownLatch processingLatch = new CountDownLatch(1);
		AtomicBoolean hasThrownError = new AtomicBoolean(false);

		AbstractPollingMessageSource<Object, Message> source = new AbstractPollingMessageSource<>() {

			private final AtomicBoolean hasReceived = new AtomicBoolean(false);

			private final AtomicBoolean hasAcquired9 = new AtomicBoolean(false);

			private final AtomicBoolean hasMadeThirdPoll = new AtomicBoolean(false);

			@Override
			protected CompletableFuture<Collection<Message>> doPollForMessages(int messagesToRequest) {
				return CompletableFuture.supplyAsync(() -> {
					try {
						// Give it some time between returning empty and polling again
						// doSleep(100);

						// Will only be true the first time it sets hasReceived to true
						boolean shouldReturnMessage = hasReceived.compareAndSet(false, true);
						if (shouldReturnMessage) {
							// First poll, should have 10
							logger.debug("First poll - should request 10 messages");
							assertThat(messagesToRequest).isEqualTo(10);
							assertAvailablePermits(backPressureHandler, 0);
							// No permits have been released yet
							assertThroughputMode(backPressureHandler, "low");
						}
						else if (hasAcquired9.compareAndSet(false, true)) {
							// Second poll, should have 9
							logger.debug("Second poll - should request 9 messages");
							assertThat(messagesToRequest).isEqualTo(9);
							assertAvailablePermitsLessThanOrEqualTo(backPressureHandler, 1);
							// Has released 9 permits, should be TM HIGH
							assertThroughputMode(backPressureHandler, "high");
							processingLatch.countDown(); // Release processing now
						}
						else {
							boolean thirdPoll = hasMadeThirdPoll.compareAndSet(false, true);
							// Third poll or later, should have 10 again
							logger.debug("Third poll - should request 10 messages");
							assertThat(messagesToRequest).isEqualTo(10);
							assertAvailablePermits(backPressureHandler, 0);
							if (thirdPoll) {
								// Hasn't yet returned a full batch, should be TM High
								assertThroughputMode(backPressureHandler, "high");
							}
							else {
								// Has returned all permits in third poll
								assertThroughputMode(backPressureHandler, "low");
							}
						}
						if (shouldReturnMessage) {
							logger.debug("shouldReturnMessage, returning one message");
							return (Collection<Message>) List.of(
									Message.builder().messageId(UUID.randomUUID().toString()).body("message").build());
						}
						logger.debug("should not return message, returning empty list");
						return Collections.<Message> emptyList();
					}
					catch (Error e) {
						hasThrownError.set(true);
						throw new RuntimeException("Error polling for messages", e);
					}
				}, threadPool).whenComplete((v, t) -> pollingCounter.countDown());
			}
		};

		source.setBackPressureHandler(backPressureHandler);
		source.setMessageSink((msgs, context) -> {
			logger.debug("Processing {} messages", msgs.size());
			assertAvailablePermits(backPressureHandler, 9);
			assertThat(doAwait(processingLatch)).isTrue();
			logger.debug("Finished processing {} messages", msgs.size());
			msgs.forEach(msg -> context.runBackPressureReleaseCallback());
			return CompletableFuture.completedFuture(null).thenRun(processingCounter::countDown);
		});
		source.setId(testName + " source");
		source.configure(SqsContainerOptions.builder().build());
		source.setTaskExecutor(createTaskExecutor(testName));
		source.setAcknowledgementProcessor(getAcknowledgementProcessor());
		source.start();
		assertThat(doAwait(processingCounter)).isTrue();
		assertThat(doAwait(pollingCounter)).isTrue();
		source.stop();
		assertThat(hasThrownError.get()).isFalse();
	}

	private static boolean doAwait(CountDownLatch processingLatch) {
		try {
			return processingLatch.await(4, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while waiting for latch", e);
		}
	}

	private void assertThroughputMode(SemaphoreBackPressureHandler backPressureHandler, String expectedThroughputMode) {
		assertThat(ReflectionTestUtils.getField(backPressureHandler, "currentThroughputMode"))
				.extracting(Object::toString).extracting(String::toLowerCase)
				.isEqualTo(expectedThroughputMode.toLowerCase());
	}

	private void assertAvailablePermits(SemaphoreBackPressureHandler backPressureHandler, int expectedPermits) {
		assertThat(ReflectionTestUtils.getField(backPressureHandler, "semaphore")).asInstanceOf(type(Semaphore.class))
				.extracting(Semaphore::availablePermits).isEqualTo(expectedPermits);
	}

	private void assertAvailablePermitsLessThanOrEqualTo(SemaphoreBackPressureHandler backPressureHandler,
			int maxExpectedPermits) {
		assertThat(ReflectionTestUtils.getField(backPressureHandler, "semaphore")).asInstanceOf(type(Semaphore.class))
				.extracting(Semaphore::availablePermits).asInstanceOf(InstanceOfAssertFactories.INTEGER)
				.isLessThanOrEqualTo(maxExpectedPermits);
	}

	private void doSleep(int time) {
		try {
			Thread.sleep(time);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}

	protected TaskExecutor createTaskExecutor(String testName) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		int poolSize = 10;
		executor.setMaxPoolSize(poolSize);
		executor.setCorePoolSize(10);
		executor.setQueueCapacity(poolSize);
		executor.setAllowCoreThreadTimeOut(true);
		executor.setThreadFactory(createThreadFactory(testName));
		executor.afterPropertiesSet();
		return executor;
	}

	protected ThreadFactory createThreadFactory(String testName) {
		MessageExecutionThreadFactory threadFactory = new MessageExecutionThreadFactory();
		threadFactory.setThreadNamePrefix(testName + "-thread" + "-");
		return threadFactory;
	}

	private AcknowledgementProcessor<Object> getAcknowledgementProcessor() {
		return new AcknowledgementProcessor<>() {
			@Override
			public AcknowledgementCallback<Object> getAcknowledgementCallback() {
				return new AcknowledgementCallback<>() {
				};
			}

			@Override
			public void setId(String id) {
			}

			@Override
			public String getId() {
				return "test processor";
			}

			@Override
			public void start() {
			}

			@Override
			public void stop() {
			}

			@Override
			public boolean isRunning() {
				return false;
			}
		};
	}

}
