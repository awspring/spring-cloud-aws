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
package io.awspring.cloud.sqs.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.listener.*;
import io.awspring.cloud.sqs.listener.backpressure.BackPressureHandler;
import io.awspring.cloud.sqs.listener.backpressure.BackPressureHandlerFactories;
import io.awspring.cloud.sqs.listener.backpressure.BatchAwareBackPressureHandler;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Integration tests for SQS containers back pressure management.
 *
 * @author Loïc Rouchon
 * @author Tomaz Fernandes
 */
@SpringBootTest
class SqsBackPressureIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsBackPressureIntegrationTests.class);

	@Autowired
	SqsTemplate sqsTemplate;

	static final class NonBlockingExternalConcurrencyLimiterBackPressureHandler implements BackPressureHandler {
		private final AtomicInteger limit;
		private final AtomicInteger inFlight = new AtomicInteger(0);
		private final AtomicBoolean draining = new AtomicBoolean(false);

		NonBlockingExternalConcurrencyLimiterBackPressureHandler(int max) {
			limit = new AtomicInteger(max);
		}

		public void setLimit(int value) {
			logger.info("adjusting limit from {} to {}", limit.get(), value);
			limit.set(value);
		}

		@Override
		public int request(int amount) {
			if (draining.get()) {
				return 0;
			}
			int permits = Math.max(0, Math.min(limit.get() - inFlight.get(), amount));
			inFlight.addAndGet(permits);
			return permits;
		}

		@Override
		public void release(int amount, ReleaseReason reason) {
			inFlight.addAndGet(-amount);
		}

		@Override
		public boolean drain(Duration timeout) {
			Duration drainingTimeout = Duration.ofSeconds(10L);
			Duration drainingPollingIntervalCheck = Duration.ofMillis(50L);
			draining.set(true);
			limit.set(0);
			Instant start = Instant.now();
			while (Duration.between(start, Instant.now()).compareTo(drainingTimeout) < 0) {
				if (inFlight.get() == 0) {
					return true;
				}
				sleep(drainingPollingIntervalCheck.toMillis());
			}
			return false;
		}
	}

	@ParameterizedTest
	@CsvSource({ "2,2", "4,4", "5,5", "20,5" })
	void staticBackPressureLimitShouldCapQueueProcessingCapacity(int staticLimit, int expectedMaxConcurrentRequests)
			throws Exception {
		AtomicInteger concurrentRequest = new AtomicInteger();
		AtomicInteger maxConcurrentRequest = new AtomicInteger();
		NonBlockingExternalConcurrencyLimiterBackPressureHandler limiter = new NonBlockingExternalConcurrencyLimiterBackPressureHandler(
				staticLimit);
		String queueName = "BACK_PRESSURE_LIMITER_QUEUE_NAME_STATIC_LIMIT_" + staticLimit;
		IntStream.range(0, 10).forEach(index -> {
			List<Message<String>> messages = create10Messages("staticBackPressureLimit" + staticLimit);
			sqsTemplate.sendMany(queueName, messages);
		});
		logger.debug("Sent 100 messages to queue {}", queueName);
		var latch = new CountDownLatch(100);
		var container = SqsMessageListenerContainer.builder().sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient())
				.queueNames(queueName)
				.configure(options -> options.maxMessagesPerPoll(5).maxConcurrentMessages(5)
						.backPressureMode(BackPressureMode.AUTO).maxDelayBetweenPolls(Duration.ofSeconds(1))
						.pollTimeout(Duration.ofSeconds(1)).maxDelayBetweenPolls(Duration.ofMillis(50))
						.backPressureHandlerFactory(BackPressureHandlerFactories.compositeBackPressureHandler(
								opt -> limiter, BackPressureHandlerFactories.concurrencyLimiterBackPressureHandler())))
				.messageListener(msg -> {
					int concurrentRqs = concurrentRequest.incrementAndGet();
					maxConcurrentRequest.updateAndGet(max -> Math.max(max, concurrentRqs));
					sleep(50L);
					logger.debug("concurrent rq {}, max concurrent rq {}, latch count {}", concurrentRequest.get(),
							maxConcurrentRequest.get(), latch.getCount());
					latch.countDown();
					concurrentRequest.decrementAndGet();
				}).build();
		container.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(maxConcurrentRequest.get()).isLessThanOrEqualTo(expectedMaxConcurrentRequests);
		container.stop();
	}

	@Test
	void zeroBackPressureLimitShouldStopQueueProcessing() throws Exception {
		AtomicInteger concurrentRequest = new AtomicInteger();
		AtomicInteger maxConcurrentRequest = new AtomicInteger();
		NonBlockingExternalConcurrencyLimiterBackPressureHandler limiter = new NonBlockingExternalConcurrencyLimiterBackPressureHandler(
				0);
		String queueName = "BACK_PRESSURE_LIMITER_QUEUE_NAME_STATIC_LIMIT_0";
		IntStream.range(0, 10).forEach(index -> {
			List<Message<String>> messages = create10Messages("staticBackPressureLimit0");
			sqsTemplate.sendMany(queueName, messages);
		});
		logger.debug("Sent 100 messages to queue {}", queueName);
		var latch = new CountDownLatch(100);
		var container = SqsMessageListenerContainer.builder().sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient())
				.queueNames(queueName)
				.configure(options -> options.maxMessagesPerPoll(5).maxConcurrentMessages(5)
						.backPressureMode(BackPressureMode.AUTO).maxDelayBetweenPolls(Duration.ofSeconds(1))
						.pollTimeout(Duration.ofSeconds(1)).maxDelayBetweenPolls(Duration.ofMillis(50L))
						.backPressureHandlerFactory(BackPressureHandlerFactories.compositeBackPressureHandler(
								opt -> limiter, BackPressureHandlerFactories.concurrencyLimiterBackPressureHandler())))
				.messageListener(msg -> {
					int concurrentRqs = concurrentRequest.incrementAndGet();
					maxConcurrentRequest.updateAndGet(max -> Math.max(max, concurrentRqs));
					sleep(50L);
					logger.debug("concurrent rq {}, max concurrent rq {}, latch count {}", concurrentRequest.get(),
							maxConcurrentRequest.get(), latch.getCount());
					latch.countDown();
					concurrentRequest.decrementAndGet();
				}).build();
		container.start();
		assertThat(latch.await(2, TimeUnit.SECONDS)).isFalse();
		assertThat(maxConcurrentRequest.get()).isZero();
		assertThat(latch.getCount()).isEqualTo(100L);
		container.stop();
	}

	@Test
	void changeInBackPressureLimitShouldAdaptQueueProcessingCapacity() throws Exception {
		AtomicInteger concurrentRequest = new AtomicInteger();
		AtomicInteger maxConcurrentRequest = new AtomicInteger();
		NonBlockingExternalConcurrencyLimiterBackPressureHandler limiter = new NonBlockingExternalConcurrencyLimiterBackPressureHandler(
				5);
		String queueName = "BACK_PRESSURE_LIMITER_QUEUE_NAME_SYNC_ADAPTIVE_LIMIT";
		int nbMessages = 280;
		IntStream.range(0, nbMessages / 10).forEach(index -> {
			List<Message<String>> messages = create10Messages("syncAdaptiveBackPressureLimit");
			sqsTemplate.sendMany(queueName, messages);
		});
		logger.debug("Sent {} messages to queue {}", nbMessages, queueName);
		var latch = new CountDownLatch(nbMessages);
		var controlSemaphore = new Semaphore(0);
		var advanceSemaphore = new Semaphore(0);
		var processingFailed = new AtomicBoolean(false);
		var isDraining = new AtomicBoolean(false);
		var container = SqsMessageListenerContainer.builder().sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient())
				.queueNames(queueName)
				.configure(options -> options.maxMessagesPerPoll(5).maxConcurrentMessages(5)
						.backPressureMode(BackPressureMode.AUTO).maxDelayBetweenPolls(Duration.ofSeconds(1))
						.pollTimeout(Duration.ofSeconds(1)).maxDelayBetweenPolls(Duration.ofMillis(50))
						.backPressureHandlerFactory(BackPressureHandlerFactories.compositeBackPressureHandler(
								opt -> limiter, BackPressureHandlerFactories.concurrencyLimiterBackPressureHandler())))
				.messageListener(msg -> {
					try {
						if (!controlSemaphore.tryAcquire(5, TimeUnit.SECONDS) && !isDraining.get()) {
							processingFailed.set(true);
							throw new IllegalStateException("Failed to wait for control semaphore");
						}
					}
					catch (InterruptedException e) {
						if (!isDraining.get()) {
							processingFailed.set(true);
							Thread.currentThread().interrupt();
							throw new RuntimeException(e);
						}
					}
					int concurrentRqs = concurrentRequest.incrementAndGet();
					maxConcurrentRequest.updateAndGet(max -> Math.max(max, concurrentRqs));
					latch.countDown();
					logger.debug("concurrent rq {}, max concurrent rq {}, latch count {}", concurrentRequest.get(),
							maxConcurrentRequest.get(), latch.getCount());
					sleep(10L);
					concurrentRequest.decrementAndGet();
					advanceSemaphore.release();
				}).build();
		class Controller {
			private final Semaphore advanceSemaphore;
			private final Semaphore controlSemaphore;
			private final NonBlockingExternalConcurrencyLimiterBackPressureHandler limiter;
			private final AtomicInteger maxConcurrentRequest;
			private final AtomicBoolean processingFailed;

			Controller(Semaphore advanceSemaphore, Semaphore controlSemaphore,
					NonBlockingExternalConcurrencyLimiterBackPressureHandler limiter,
					AtomicInteger maxConcurrentRequest, AtomicBoolean processingFailed) {
				this.advanceSemaphore = advanceSemaphore;
				this.controlSemaphore = controlSemaphore;
				this.limiter = limiter;
				this.maxConcurrentRequest = maxConcurrentRequest;
				this.processingFailed = processingFailed;
			}

			public void updateLimit(int newLimit) {
				limiter.setLimit(newLimit);
			}

			void updateLimitAndWaitForReset(int newLimit) throws InterruptedException {
				updateLimit(newLimit);
				int atLeastTwoPollingCycles = 2 * 5;
				controlSemaphore.release(atLeastTwoPollingCycles);
				waitForAdvance(atLeastTwoPollingCycles);
				maxConcurrentRequest.set(0);
			}

			void advance(int permits) {
				controlSemaphore.release(permits);
			}

			void waitForAdvance(int permits) throws InterruptedException {
				assertThat(advanceSemaphore.tryAcquire(permits, 5, TimeUnit.SECONDS))
						.withFailMessage(() -> "Waiting for %d permits timed out. Only %d permits available"
								.formatted(permits, advanceSemaphore.availablePermits()))
						.isTrue();
				assertThat(processingFailed.get()).isFalse();
			}
		}
		var controller = new Controller(advanceSemaphore, controlSemaphore, limiter, maxConcurrentRequest,
				processingFailed);
		try {
			container.start();

			controller.advance(50);
			controller.waitForAdvance(50);
			// not limiting queue processing capacity
			assertThat(controller.maxConcurrentRequest.get()).isLessThanOrEqualTo(5);
			controller.updateLimitAndWaitForReset(2);
			controller.advance(50);

			controller.waitForAdvance(50);
			// limiting queue processing capacity
			assertThat(controller.maxConcurrentRequest.get()).isLessThanOrEqualTo(2);
			controller.updateLimitAndWaitForReset(7);
			controller.advance(50);

			controller.waitForAdvance(50);
			// not limiting queue processing capacity
			assertThat(controller.maxConcurrentRequest.get()).isLessThanOrEqualTo(5);
			controller.updateLimitAndWaitForReset(3);
			controller.advance(50);
			sleep(10L);
			limiter.setLimit(1);
			sleep(10L);
			limiter.setLimit(2);
			sleep(10L);
			limiter.setLimit(3);

			controller.waitForAdvance(50);
			assertThat(controller.maxConcurrentRequest.get()).isLessThanOrEqualTo(3);
			// stopping processing of the queue
			controller.updateLimit(0);
			controller.advance(50);
			assertThat(advanceSemaphore.tryAcquire(10, 5, TimeUnit.SECONDS))
					.withFailMessage("Acquiring semaphore should have timed out as limit was set to 0").isFalse();

			// resume queue processing
			controller.updateLimit(6);

			controller.waitForAdvance(50);
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(controller.maxConcurrentRequest.get()).isLessThanOrEqualTo(5);
			assertThat(processingFailed.get()).isFalse();
		}
		finally {
			isDraining.set(true);
			container.stop();
		}
	}

	static class EventsCsvWriter {
		private final Queue<String> events = new ConcurrentLinkedQueue<>(List.of("event,time,value"));

		void registerEvent(String event, int value) {
			events.add("%s,%s,%d".formatted(event, Instant.now(), value));
		}

		void write(Path path) throws Exception {
			Files.writeString(path, String.join("\n", events), StandardCharsets.UTF_8, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		}
	}

	static class StatisticsBphDecorator implements BatchAwareBackPressureHandler, IdentifiableContainerComponent {
		private final BatchAwareBackPressureHandler delegate;
		private final EventsCsvWriter eventCsv;
		private String id;

		StatisticsBphDecorator(BatchAwareBackPressureHandler delegate, EventsCsvWriter eventsCsvWriter) {
			this.delegate = delegate;
			this.eventCsv = eventsCsvWriter;
		}

		@Override
		public int requestBatch() throws InterruptedException {
			int permits = delegate.requestBatch();
			if (permits > 0) {
				eventCsv.registerEvent("obtained_permits", permits);
			}
			return permits;
		}

		@Override
		public int request(int amount) throws InterruptedException {
			int permits = delegate.request(amount);
			if (permits > 0) {
				eventCsv.registerEvent("obtained_permits", permits);
			}
			return permits;
		}

		@Override
		public void release(int amount, ReleaseReason reason) {
			if (amount > 0) {
				eventCsv.registerEvent("release_" + reason, amount);
			}
			delegate.release(amount, reason);
		}

		@Override
		public boolean drain(Duration timeout) {
			eventCsv.registerEvent("drain", 1);
			return delegate.drain(timeout);
		}

		@Override
		public void setId(String id) {
			this.id = id;
			if (delegate instanceof IdentifiableContainerComponent icc) {
				icc.setId("delegate-" + id);
			}
		}

		@Override
		public String getId() {
			return id;
		}
	}

	/**
	 * This test simulates a progressive change in the back pressure limit. Unlike
	 * {@link #changeInBackPressureLimitShouldAdaptQueueProcessingCapacity()}, this test does not block message
	 * consumption while updating the limit.
	 * <p>
	 * The limit is updated in a loop until all messages are consumed. The update follows a triangle wave pattern with a
	 * minimum of 0, a maximum of 15, and a period of 30 iterations. After each update of the limit, the test waits up
	 * to 10ms and samples the maximum number of concurrent messages that were processed since the update. This number
	 * can be higher than the defined limit during the adaptation period of the decreasing limit wave. For the
	 * increasing limit wave, it is usually lower due to the adaptation delay. In both cases, the maximum number of
	 * concurrent messages being processed rapidly converges toward the defined limit.
	 * <p>
	 * The test passes if the sum of the sampled maximum number of concurrently processed messages is lower than the sum
	 * of the limits at those points in time.
	 */
	@Test
	void unsynchronizedChangesInBackPressureLimitShouldAdaptQueueProcessingCapacity() throws Exception {
		AtomicInteger concurrentRequest = new AtomicInteger();
		AtomicInteger maxConcurrentRequest = new AtomicInteger();
		NonBlockingExternalConcurrencyLimiterBackPressureHandler limiter = new NonBlockingExternalConcurrencyLimiterBackPressureHandler(
				0);
		String queueName = "REACTIVE_BACK_PRESSURE_LIMITER_QUEUE_NAME_ADAPTIVE_LIMIT";
		int nbMessages = 1000;
		Semaphore advanceSemaphore = new Semaphore(0);
		IntStream.range(0, nbMessages / 10).forEach(index -> {
			List<Message<String>> messages = create10Messages("reactAdaptiveBackPressureLimit");
			sqsTemplate.sendMany(queueName, messages);
		});
		logger.debug("Sent {} messages to queue {}", nbMessages, queueName);
		var latch = new CountDownLatch(nbMessages);
		EventsCsvWriter eventsCsvWriter = new EventsCsvWriter();
		var container = SqsMessageListenerContainer.builder().sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient())
				.queueNames(queueName)
				.configure(
						options -> options.maxMessagesPerPoll(10).maxConcurrentMessages(10)
								.backPressureMode(BackPressureMode.AUTO).maxDelayBetweenPolls(Duration.ofSeconds(1))
								.pollTimeout(Duration.ofSeconds(1)).maxDelayBetweenPolls(Duration.ofMillis(50L))
								.backPressureHandlerFactory(opt -> new StatisticsBphDecorator(
										(BatchAwareBackPressureHandler) BackPressureHandlerFactories
												.compositeBackPressureHandler(any -> limiter,
														BackPressureHandlerFactories
																.concurrencyLimiterBackPressureHandler())
												.createBackPressureHandler(opt),
										eventsCsvWriter)))
				.messageListener(msg -> {
					int currentConcurrentRq = concurrentRequest.incrementAndGet();
					maxConcurrentRequest.updateAndGet(max -> Math.max(max, currentConcurrentRq));
					sleep(ThreadLocalRandom.current().nextInt(10));
					latch.countDown();
					logger.debug("concurrent rq {}, max concurrent rq {}, latch count {}", concurrentRequest.get(),
							maxConcurrentRequest.get(), latch.getCount());
					concurrentRequest.decrementAndGet();
					advanceSemaphore.release();
				}).build();
		IntUnaryOperator progressiveLimitChange = (int x) -> {
			int period = 30;
			int halfPeriod = period / 2;
			if (x % period < halfPeriod) {
				return (x % halfPeriod);
			}
			else {
				return (halfPeriod - (x % halfPeriod));
			}
		};
		try {
			container.start();
			Random random = new Random();
			int limitsSum = 0;
			int maxConcurrentRqSum = 0;
			int changeLimitCount = 0;
			while (latch.getCount() > 0 && changeLimitCount < nbMessages) {
				changeLimitCount++;
				int limit = progressiveLimitChange.applyAsInt(changeLimitCount);
				int expectedMax = Math.min(10, limit);
				limiter.setLimit(limit);
				maxConcurrentRequest.set(0);
				sleep(random.nextInt(20));
				int actualLimit = Math.min(10, limit);
				int max = maxConcurrentRequest.get();
				if (max > 0) {
					// Ignore iterations where nothing was polled (messages consumption slower than iteration)
					limitsSum += actualLimit;
					maxConcurrentRqSum += max;
				}
				eventsCsvWriter.registerEvent("max_concurrent_rq", max);
				eventsCsvWriter.registerEvent("concurrent_rq", concurrentRequest.get());
				eventsCsvWriter.registerEvent("limit", limit);
				eventsCsvWriter.registerEvent("in_flight", limiter.inFlight.get());
				eventsCsvWriter.registerEvent("expected_max", expectedMax);
				eventsCsvWriter.registerEvent("max_minus_expected_max", max - expectedMax);
			}
			eventsCsvWriter.write(Path.of("target/stats-%s.csv".formatted(queueName)));
			assertThat(maxConcurrentRqSum).isLessThanOrEqualTo(limitsSum);
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		}
		finally {
			container.stop();
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private List<Message<String>> create10Messages(String testName) {
		return IntStream.range(0, 10).mapToObj(index -> testName + "-payload-" + index)
				.map(payload -> MessageBuilder.withPayload(payload).build()).collect(Collectors.toList());
	}

	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class SQSConfiguration {

		@Bean
		SqsTemplate sqsTemplate() {
			return SqsTemplate.builder().sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient()).build();
		}
	}
}
