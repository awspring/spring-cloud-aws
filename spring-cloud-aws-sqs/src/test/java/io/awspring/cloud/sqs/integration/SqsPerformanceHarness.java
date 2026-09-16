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

import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Reusable harness for measuring SQS message processing performance under different container configurations.
 *
 * <p>
 * The harness sends messages to a queue, starts a container, and measures how long it takes to process all messages. It
 * reports throughput (messages/second) and latency percentiles (p50, p95, p99).
 *
 * <p>
 * Usage:
 *
 * <pre>{@code
 * var harness = new SqsPerformanceHarness(sqsAsyncClient);
 *
 * // Simple run with default container
 * var result = harness.run(RunParameters.builder().queueName("my-queue").messageCount(1000)
 * 		.processingDelay(Duration.ofSeconds(1)).build());
 *
 * // Run with custom container configuration
 * var result = harness.run(RunParameters.builder().queueName("my-queue").messageCount(1000).build(),
 * 		builder -> builder
 * 				.configure(options -> options.componentsTaskExecutor(myExecutor).maxConcurrentMessages(500))
 * 				.messageInterceptor(myInterceptor));
 *
 * result.report("My Test");
 * }</pre>
 *
 * <p>
 * The harness owns the message listener (for latch/latency tracking). The {@code containerConfigurer} can set any other
 * container property (options, interceptors, error handlers) but should not override the listener.
 *
 * <p>
 * Messages are sent in parallel with a throttle of 50 concurrent batch sends and retry with backoff.
 *
 * @author Tomaz Fernandes
 * @since 4.1.0
 * @see SqsPerformanceTests
 */
class SqsPerformanceHarness {

	private static final Logger logger = LoggerFactory.getLogger(SqsPerformanceHarness.class);

	private final SqsAsyncClient sqsAsyncClient;

	private final SqsTemplate sqsTemplate;

	SqsPerformanceHarness(SqsAsyncClient sqsAsyncClient) {
		this.sqsAsyncClient = sqsAsyncClient;
		this.sqsTemplate = SqsTemplate.newTemplate(sqsAsyncClient);
	}

	/**
	 * Run a performance test with the given parameters and container configuration.
	 * @param params the test parameters.
	 * @param containerConfigurer customization for the container builder (options, interceptors, error handlers, etc).
	 *     The message listener is set by the harness and should not be overridden.
	 * @return the performance result.
	 */
	PerformanceResult run(RunParameters params,
			Consumer<SqsMessageListenerContainer.Builder<Object>> containerConfigurer) {
		Assert.isTrue(params.messageCount > 0, "messageCount must be positive");
		CountDownLatch latch = new CountDownLatch(params.messageCount);
		List<Long> latencies = Collections.synchronizedList(new ArrayList<>(params.messageCount));

		SqsMessageListenerContainer.Builder<Object> builder = SqsMessageListenerContainer.builder()
				.sqsAsyncClient(this.sqsAsyncClient).queueNames(params.queueName).messageListener(message -> {
					if (!params.processingDelay.isZero()) {
						try {
							Thread.sleep(params.processingDelay.toMillis());
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
					Instant sent = Instant.ofEpochMilli(message.getHeaders().getTimestamp());
					latencies.add(Duration.between(sent, Instant.now()).toMillis());
					latch.countDown();
				});
		containerConfigurer.accept(builder);
		SqsMessageListenerContainer<Object> container = builder.build();

		sendMessages(params.queueName, params.messageCount);
		container.start();
		try {
			Instant start = Instant.now();
			boolean completed = latch.await(params.timeout.toSeconds(), TimeUnit.SECONDS);
			Duration totalDuration = Duration.between(start, Instant.now());
			Assert.isTrue(completed, "Timed out waiting for messages. Processed: "
					+ (params.messageCount - latch.getCount()) + "/" + params.messageCount);
			return new PerformanceResult(params.messageCount, totalDuration, new ArrayList<>(latencies));
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted during performance test", e);
		}
		finally {
			container.stop();
		}
	}

	/**
	 * Run a performance test with default container configuration.
	 */
	PerformanceResult run(RunParameters params) {
		return run(params, builder -> {
		});
	}

	private void sendMessages(String queueName, int messageCount) {
		int batchSize = Math.min(messageCount, 10);
		int batches = messageCount / batchSize;
		int remainder = messageCount % batchSize;
		int concurrentSends = Math.min(batches, 50);
		Semaphore sendThrottle = new Semaphore(concurrentSends);
		List<CompletableFuture<?>> futures = new ArrayList<>();
		for (int i = 0; i < batches; i++) {
			int batchIndex = i;
			try {
				sendThrottle.acquire();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
			var messages = IntStream.range(0, batchSize)
					.mapToObj(j -> MessageBuilder.withPayload("perf-msg-" + (batchIndex * batchSize + j)).build())
					.toList();
			futures.add(sendWithRetry(queueName, messages, 3).whenComplete((r, t) -> sendThrottle.release()));
		}
		if (remainder > 0) {
			futures.add(sqsTemplate.sendManyAsync(queueName, IntStream.range(0, remainder)
					.mapToObj(j -> MessageBuilder.withPayload("perf-msg-remainder-" + j).build()).toList()));
		}
		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
		logger.info("Sent {} messages to {}", messageCount, queueName);
	}

	private <T> CompletableFuture<?> sendWithRetry(String queueName,
			java.util.Collection<org.springframework.messaging.Message<T>> messages, int retriesLeft) {
		return sqsTemplate.sendManyAsync(queueName, messages).thenApply(r -> (Object) r).exceptionally(t -> {
			if (retriesLeft <= 0) {
				throw new RuntimeException("Send failed after retries", t);
			}
			logger.warn("Send failed, retrying ({} left): {}", retriesLeft, t.getMessage());
			try {
				Thread.sleep(1000L * (4 - retriesLeft));
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			sendWithRetry(queueName, messages, retriesLeft - 1).join();
			return null;
		});
	}

	/**
	 * Parameters for a performance test run.
	 */
	static class RunParameters {

		final String queueName;

		final int messageCount;

		final Duration timeout;

		final Duration processingDelay;

		private RunParameters(Builder builder) {
			this.queueName = builder.queueName;
			this.messageCount = builder.messageCount;
			this.timeout = builder.timeout;
			this.processingDelay = builder.processingDelay;
		}

		static Builder builder() {
			return new Builder();
		}

		static class Builder {

			private String queueName;

			private int messageCount = 200;

			private Duration timeout = Duration.ofSeconds(60);

			private Duration processingDelay = Duration.ZERO;

			Builder queueName(String queueName) {
				this.queueName = queueName;
				return this;
			}

			Builder messageCount(int messageCount) {
				this.messageCount = messageCount;
				return this;
			}

			Builder timeout(Duration timeout) {
				this.timeout = timeout;
				return this;
			}

			Builder processingDelay(Duration processingDelay) {
				this.processingDelay = processingDelay;
				return this;
			}

			RunParameters build() {
				Assert.hasText(queueName, "queueName must be set");
				return new RunParameters(this);
			}

		}

	}

	/**
	 * Result of a performance test run.
	 */
	static class PerformanceResult {

		private final int messageCount;

		private final Duration totalDuration;

		private final List<Long> latencies;

		PerformanceResult(int messageCount, Duration totalDuration, List<Long> latencies) {
			this.messageCount = messageCount;
			this.totalDuration = totalDuration;
			this.latencies = latencies;
			Collections.sort(this.latencies);
		}

		double messagesPerSecond() {
			return messageCount / (totalDuration.toMillis() / 1000.0);
		}

		long p50() {
			return percentile(50);
		}

		long p95() {
			return percentile(95);
		}

		long p99() {
			return percentile(99);
		}

		Duration totalDuration() {
			return totalDuration;
		}

		int messageCount() {
			return messageCount;
		}

		private long percentile(int p) {
			if (latencies.isEmpty()) {
				return 0;
			}
			int index = (int) Math.ceil(p / 100.0 * latencies.size()) - 1;
			return latencies.get(Math.max(0, index));
		}

		void report(String name) {
			logger.info("""

					=== {} ===
					Messages:     {}
					Duration:     {}.{}s
					Throughput:   {} msg/s
					Latency p50:  {}ms | p95: {}ms | p99: {}ms""", name, messageCount, totalDuration.toSeconds(),
					String.format("%03d", totalDuration.toMillisPart()), String.format("%.0f", messagesPerSecond()),
					p50(), p95(), p99());
		}

	}

}
