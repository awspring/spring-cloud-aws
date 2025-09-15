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
package io.awspring.cloud.sqs.listener.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import io.awspring.cloud.sqs.MessageExecutionThreadFactory;
import io.awspring.cloud.sqs.listener.BackPressureMode;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.ContainerOptionsBuilder;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.backpressure.BackPressureHandler;
import io.awspring.cloud.sqs.listener.backpressure.BackPressureHandlerFactories;
import io.awspring.cloud.sqs.listener.backpressure.CompositeBackPressureHandler;
import io.awspring.cloud.sqs.listener.backpressure.ConcurrencyLimiterBlockingBackPressureHandler;
import io.awspring.cloud.sqs.listener.backpressure.ThroughputBackPressureHandler;
import io.awspring.cloud.sqs.support.converter.MessageConversionContext;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * @author Tomaz Fernandes
 * @author Loïc Rouchon
 */
class AbstractPollingMessageSourceTests {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPollingMessageSourceTests.class);

	@Test
	void shouldAcquireAndReleaseFullPermits() {
		String testName = "shouldAcquireAndReleaseFullPermits";
		SqsContainerOptions options = SqsContainerOptions.builder().maxMessagesPerPoll(10).maxConcurrentMessages(10)
				.backPressureMode(BackPressureMode.ALWAYS_POLL_MAX_MESSAGES)
				.maxDelayBetweenPolls(Duration.ofMillis(200)).listenerShutdownTimeout(Duration.ZERO).build();
		BackPressureHandler backPressureHandler = BackPressureHandlerFactories.adaptiveThroughputBackPressureHandler()
				.createBackPressureHandler(options);

		ExecutorService threadPool = Executors.newCachedThreadPool();
		CountDownLatch pollingCounter = new CountDownLatch(3);
		CountDownLatch processingCounter = new CountDownLatch(1);

		AbstractPollingMessageSource<Object, Message> source = new AbstractPollingMessageSource<>() {

			private final AtomicBoolean hasReceived = new AtomicBoolean(false);

			@Override
			protected CompletableFuture<Collection<Message>> doPollForMessages(int messagesToRequest) {
				return CompletableFuture.supplyAsync(() -> {
					try {
						// Since BackPressureMode.ALWAYS_POLL_MAX_MESSAGES, should always be 10.
						assertThat(messagesToRequest).isEqualTo(10);
						assertAvailablePermits(backPressureHandler, 0);
						boolean firstPoll = hasReceived.compareAndSet(false, true);
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
		source.configure(options);
		source.setTaskExecutor(createTaskExecutor(testName));
		source.setAcknowledgementProcessor(getNoOpsAcknowledgementProcessor());
		try {
			source.start();
			assertThat(doAwait(pollingCounter)).isTrue();
			assertThat(doAwait(processingCounter)).isTrue();
		}
		finally {
			source.stop();
			threadPool.shutdownNow();
		}
	}

	@Test
	void shouldAdaptThroughputMode() {
		String testName = "shouldAdaptThroughputMode";
		SqsContainerOptions options = SqsContainerOptions.builder().maxMessagesPerPoll(10).maxConcurrentMessages(10)
				.backPressureMode(BackPressureMode.ALWAYS_POLL_MAX_MESSAGES)
				.maxDelayBetweenPolls(Duration.ofMillis(150)).listenerShutdownTimeout(Duration.ZERO).build();
		BackPressureHandler backPressureHandler = BackPressureHandlerFactories.adaptiveThroughputBackPressureHandler()
				.createBackPressureHandler(options);

		ExecutorService threadPool = Executors.newCachedThreadPool();
		CountDownLatch pollingCounter = new CountDownLatch(3);
		CountDownLatch processingCounter = new CountDownLatch(1);
		Collection<Throwable> errors = new ConcurrentLinkedQueue<>();

		AbstractPollingMessageSource<Object, Message> source = new AbstractPollingMessageSource<>() {

			private final AtomicInteger pollAttemptCounter = new AtomicInteger(0);

			@Override
			protected CompletableFuture<Collection<Message>> doPollForMessages(int messagesToRequest) {
				return CompletableFuture.supplyAsync(() -> {
					try {
						int pollAttempt = pollAttemptCounter.incrementAndGet();
						logger.warn("Poll attempt {}", pollAttempt);
						if (pollAttempt == 1) {
							// Initial poll; throughput mode should be low
							assertThroughputMode(backPressureHandler, "low");
							// Since no permits were acquired yet, should be 10
							assertThat(messagesToRequest).isEqualTo(10);
							return (Collection<Message>) List.of(
									Message.builder().messageId(UUID.randomUUID().toString()).body("message").build());
						}
						else if (pollAttempt == 2) {
							// Messages returned in the previous poll; throughput mode should be high
							assertThroughputMode(backPressureHandler, "high");
							// Since throughput mode is high, should be 10
							assertThat(messagesToRequest).isEqualTo(10);
							return Collections.<Message> emptyList();
						}
						else {
							// No Messages returned in the previous poll; throughput mode should be low
							assertThroughputMode(backPressureHandler, "low");
							return Collections.<Message> emptyList();
						}
					}
					catch (Throwable t) {
						logger.error("Error (not expecting it)", t);
						errors.add(t);
						throw new RuntimeException(t);
					}
				}, threadPool).whenComplete((v, t) -> {
					if (t == null) {
						logger.warn("Polling succeeded", t);
						pollingCounter.countDown();
					}
					else {
						logger.warn("Polling failed with error", t);
						errors.add(t);
					}
				});
			}
		};

		source.setBackPressureHandler(backPressureHandler);
		source.setMessageSink((msgs, context) -> {
			msgs.forEach(msg -> context.runBackPressureReleaseCallback());
			return CompletableFuture.runAsync(processingCounter::countDown);
		});

		source.setId(testName + " source");
		source.configure(options);
		source.setTaskExecutor(createTaskExecutor(testName));
		source.setAcknowledgementProcessor(getNoOpsAcknowledgementProcessor());
		try {
			source.start();
			assertThat(doAwait(pollingCounter)).isTrue();
			assertThat(doAwait(processingCounter)).isTrue();
			assertThat(errors).isEmpty();
		}
		finally {
			source.stop();
			threadPool.shutdownNow();
		}
	}

	private static final AtomicInteger testCounter = new AtomicInteger();

	@Test
	void shouldAcquireAndReleasePartialPermits() {
		String testName = "shouldAcquireAndReleasePartialPermits";
		SqsContainerOptions options = SqsContainerOptions.builder().maxMessagesPerPoll(10).maxConcurrentMessages(10)
				.backPressureMode(BackPressureMode.AUTO).maxDelayBetweenPolls(Duration.ofMillis(150))
				.listenerShutdownTimeout(Duration.ZERO).build();
		BackPressureHandler backPressureHandler = BackPressureHandlerFactories.adaptiveThroughputBackPressureHandler()
				.createBackPressureHandler(options);
		ExecutorService threadPool = Executors
				.newCachedThreadPool(new MessageExecutionThreadFactory("test " + testCounter.incrementAndGet()));
		CountDownLatch pollingCounter = new CountDownLatch(4);
		CountDownLatch processingCounter = new CountDownLatch(1);
		CountDownLatch processingLatch = new CountDownLatch(1);
		AtomicBoolean hasThrownError = new AtomicBoolean(false);

		AbstractPollingMessageSource<Object, Message> source = new AbstractPollingMessageSource<>() {

			private final AtomicInteger pollAttemptCounter = new AtomicInteger(0);

			@Override
			protected CompletableFuture<Collection<Message>> doPollForMessages(int messagesToRequest) {
				return CompletableFuture.supplyAsync(() -> {
					try {
						int pollAttempt = pollAttemptCounter.incrementAndGet();
						if (pollAttempt == 1) {
							// First poll, should have 10
							logger.debug("First poll - should request 10 messages");
							assertThat(messagesToRequest).isEqualTo(10);
							Message message = Message.builder().messageId(UUID.randomUUID().toString()).body("message")
									.build();
							return (Collection<Message>) List.of(message);
						}
						else if (pollAttempt == 2) {
							// Second poll, should have 9
							logger.debug("Second poll - should request 9 messages");
							assertThat(messagesToRequest).isEqualTo(9);
							processingLatch.countDown(); // Release processing now
							return Collections.<Message> emptyList();
						}
						else {
							// Third poll or later, should have 10 again
							logger.debug("Third (or later) poll - should request 10 messages");
							assertThat(messagesToRequest).isEqualTo(10);
							return Collections.<Message> emptyList();
						}
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
		source.configure(options);
		source.setTaskExecutor(createTaskExecutor(testName));
		source.setAcknowledgementProcessor(getNoOpsAcknowledgementProcessor());
		try {
			source.start();
			assertThat(doAwait(processingCounter)).isTrue();
			assertThat(doAwait(pollingCounter)).isTrue();
			assertThat(hasThrownError.get()).isFalse();
		}
		finally {
			threadPool.shutdownNow();
			source.stop();
		}
	}

	@Test
	void shouldReleasePermitsOnConversionErrors() {
		String testName = "shouldReleasePermitsOnConversionErrors";

		AtomicInteger convertedMessages = new AtomicInteger(0);
		var converter = new SqsMessagingMessageConverter() {
			@Override
			public org.springframework.messaging.Message<?> toMessagingMessage(Message source,
					@Nullable MessageConversionContext context) {
				var converted = convertedMessages.incrementAndGet();
				logger.trace("Messages converted: {}", converted);
				if (converted % 9 == 0) {
					throw new RuntimeException("Expected error");
				}
				return super.toMessagingMessage(source, context);
			}
		};

		SqsContainerOptions options = SqsContainerOptions.builder().maxMessagesPerPoll(10).maxConcurrentMessages(10)
				.backPressureMode(BackPressureMode.ALWAYS_POLL_MAX_MESSAGES)
				.maxDelayBetweenPolls(Duration.ofMillis(150)).messageConverter(converter)
				.listenerShutdownTimeout(Duration.ZERO).build();
		BackPressureHandler backPressureHandler = BackPressureHandlerFactories.adaptiveThroughputBackPressureHandler()
				.createBackPressureHandler(options);

		AtomicInteger messagesInSink = new AtomicInteger(0);
		AtomicBoolean hasFailed = new AtomicBoolean(false);

		AbstractPollingMessageSource<Object, Message> source = new AbstractPollingMessageSource<>() {

			@Override
			protected CompletableFuture<Collection<Message>> doPollForMessages(int messagesToRequest) {
				if (messagesToRequest != 10) {
					logger.error("Expected 10 messages to requesst, received {}", messagesToRequest);
					hasFailed.set(true);
				}
				return convertedMessages.get() < 30 ? CompletableFuture.completedFuture(create10Messages())
						: CompletableFuture.completedFuture(List.of());
			}

			private Collection<Message> create10Messages() {
				return IntStream.range(0, 10).mapToObj(
						index -> Message.builder().messageId(UUID.randomUUID().toString()).body("test-message").build())
						.toList();
			}
		};

		source.setBackPressureHandler(backPressureHandler);
		source.setMessageSink((msgs, context) -> {
			msgs.forEach(message -> messagesInSink.incrementAndGet());
			msgs.forEach(msg -> context.runBackPressureReleaseCallback());
			return CompletableFuture.completedFuture(null);
		});
		source.setId(testName + " source");
		source.configure(options);
		source.setPollingEndpointName("shouldReleasePermitsOnConversionErrors-queue");
		ThreadPoolTaskExecutor taskExecutor = createTaskExecutor(testName);
		source.setTaskExecutor(taskExecutor);
		source.setAcknowledgementProcessor(getNoOpsAcknowledgementProcessor());
		try {
			source.start();
			Awaitility.waitAtMost(Duration.ofSeconds(10)).until(() -> convertedMessages.get() == 30);
			assertThat(hasFailed).isFalse();
			assertThat(messagesInSink).hasValue(27);
		}
		finally {
			source.stop();
			taskExecutor.shutdown();
		}
	}

	@Test
	void shouldBackOffIfPollingThrowsAnError() {
		var testName = "shouldBackOffIfPollingThrowsAnError";

		var policy = mock(BackOffPolicy.class);
		var backOffContext = mock(BackOffContext.class);
		given(policy.start(null)).willReturn(backOffContext);
		SqsContainerOptions options = SqsContainerOptions.builder().maxMessagesPerPoll(10).maxConcurrentMessages(40)
				.backPressureMode(BackPressureMode.ALWAYS_POLL_MAX_MESSAGES)
				.maxDelayBetweenPolls(Duration.ofMillis(200)).pollBackOffPolicy(policy)
				.listenerShutdownTimeout(Duration.ZERO).build();
		BackPressureHandler backPressureHandler = BackPressureHandlerFactories.adaptiveThroughputBackPressureHandler()
				.createBackPressureHandler(options);

		var currentPoll = new AtomicInteger(0);
		var waitThirdPollLatch = new CountDownLatch(4);

		AbstractPollingMessageSource<Object, Message> source = new AbstractPollingMessageSource<>() {
			@Override
			protected CompletableFuture<Collection<Message>> doPollForMessages(int messagesToRequest) {
				waitThirdPollLatch.countDown();
				if (currentPoll.compareAndSet(0, 1)) {
					logger.debug("First poll - returning empty list");
					return CompletableFuture.completedFuture(List.of());
				}
				else if (currentPoll.compareAndSet(1, 2)) {
					logger.debug("Second poll - returning error");
					return CompletableFuture.failedFuture(new RuntimeException("Expected exception on second poll"));
				}
				else if (currentPoll.compareAndSet(2, 3)) {
					logger.debug("Third poll - returning error");
					return CompletableFuture.failedFuture(new RuntimeException("Expected exception on third poll"));
				}
				else {
					logger.debug("Fourth poll - returning empty list");
					return CompletableFuture.completedFuture(List.of());
				}
			}
		};

		source.setBackPressureHandler(backPressureHandler);
		source.setMessageSink((msgs, context) -> CompletableFuture.completedFuture(null));
		source.setId(testName + " source");
		source.configure(options);

		ThreadPoolTaskExecutor taskExecutor = createTaskExecutor(testName);
		source.setTaskExecutor(taskExecutor);
		source.setAcknowledgementProcessor(getNoOpsAcknowledgementProcessor());
		try {
			source.start();

			doAwait(waitThirdPollLatch);

			then(policy).should().start(null);
			then(policy).should(times(2)).backOff(backOffContext);
		}
		finally {
			source.stop();
			taskExecutor.shutdown();
		}
	}

	@Test
	void shouldRemovePollingFutureOnException() throws InterruptedException {
		String testName = "shouldClearPollingFuturesOnException";

		BackPressureHandler backPressureHandler = BackPressureHandlerFactories
			.adaptiveThroughputBackPressureHandler()
			.createBackPressureHandler(SqsContainerOptions.builder()
				.maxDelayBetweenPolls(Duration.ofMillis(100)).backPressureMode(BackPressureMode.ALWAYS_POLL_MAX_MESSAGES).build());

		AbstractPollingMessageSource<Object, Message> source = new AbstractPollingMessageSource<>() {
			@Override
			protected CompletableFuture<Collection<Message>> doPollForMessages(int messagesToRequest) {
				return CompletableFuture.failedFuture(new RuntimeException("Simulating a polling error"));
			}
		};

		BackOffPolicy policy = mock(BackOffPolicy.class);
		BackOffContext ctx = mock(BackOffContext.class);
		given(policy.start(null)).willReturn(ctx);

		source.setBackPressureHandler(backPressureHandler);
		source.setMessageSink((msgs, context) -> CompletableFuture.completedFuture(null));
		source.setId(testName + " source");
		source.setPollingEndpointName("test-queue");
		source.configure(SqsContainerOptions.builder().pollBackOffPolicy(policy).build());
		source.setTaskExecutor(createTaskExecutor(testName));
		source.setAcknowledgementProcessor(getNoOpsAcknowledgementProcessor());

		@SuppressWarnings("unchecked")
		Collection<CompletableFuture<?>> futures = (Collection<CompletableFuture<?>>) ReflectionTestUtils
				.getField(source, "pollingFutures");
		// Verify that the pollingFutures collection is initially empty
		assertThat(futures).isEmpty();

		source.start();

		// Verify that the pollingFutures collection is empty after the exceptional completion
		assertThat(futures).isEmpty();

		source.stop();
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

	private void assertThroughputMode(BackPressureHandler backPressureHandler, String expectedThroughputMode) {
		var bph = extractBackPressureHandler(backPressureHandler, ThroughputBackPressureHandler.class);
		assertThat(getThroughputModeValue(bph, "currentThroughputMode"))
				.isEqualTo(expectedThroughputMode.toLowerCase());
	}

	private static String getThroughputModeValue(ThroughputBackPressureHandler bph, String targetThroughputMode) {
		return ((AtomicReference<Object>) ReflectionTestUtils.getField(bph, targetThroughputMode)).get().toString()
				.toLowerCase(Locale.ROOT);
	}

	private void assertAvailablePermits(BackPressureHandler backPressureHandler, int expectedPermits) {
		var bph = extractBackPressureHandler(backPressureHandler, ConcurrencyLimiterBlockingBackPressureHandler.class);
		assertThat(ReflectionTestUtils.getField(bph, "semaphore")).asInstanceOf(type(Semaphore.class))
				.extracting(Semaphore::availablePermits).isEqualTo(expectedPermits);
	}

	private void assertAvailablePermitsLessThanOrEqualTo(BackPressureHandler backPressureHandler,
			int maxExpectedPermits) {
		var bph = extractBackPressureHandler(backPressureHandler, ConcurrencyLimiterBlockingBackPressureHandler.class);
		assertThat(ReflectionTestUtils.getField(bph, "semaphore")).asInstanceOf(type(Semaphore.class))
				.extracting(Semaphore::availablePermits).asInstanceOf(InstanceOfAssertFactories.INTEGER)
				.isLessThanOrEqualTo(maxExpectedPermits);
	}

	private <T extends BackPressureHandler> T extractBackPressureHandler(BackPressureHandler bph, Class<T> type) {
		if (type.isInstance(bph)) {
			return type.cast(bph);
		}
		if (bph instanceof CompositeBackPressureHandler cbph) {
			List<BackPressureHandler> backPressureHandlers = (List<BackPressureHandler>) ReflectionTestUtils
					.getField(cbph, "backPressureHandlers");
			return extractBackPressureHandler(
					backPressureHandlers.stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow(),
					type);
		}
		throw new NoSuchElementException("%s not found in %s".formatted(type.getSimpleName(), bph));
	}

	// Used to slow down tests while developing
	private void doSleep(int time) {
		try {
			Thread.sleep(time);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}

	protected ThreadPoolTaskExecutor createTaskExecutor(String testName) {
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

	private AcknowledgementProcessor<Object> getNoOpsAcknowledgementProcessor() {
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
