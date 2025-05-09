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
package io.awspring.cloud.sqs.listener;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A factory for creating {@link BackPressureHandler} for managing queue consumption backpressure. Implementations can
 * configure each the {@link BackPressureHandler} according to its strategies, using the provided
 * {@link ContainerOptions}.
 * <p>
 * Spring Cloud AWS provides the following {@link BackPressureHandler} implementations:
 * <ul>
 * <li>{@link ConcurrencyLimiterBlockingBackPressureHandler}: Limits the maximum number of messages that can be
 * processed concurrently by the application.</li>
 * <li>{@link ThroughputBackPressureHandler}: Adapts the throughput dynamically between high and low modes in order to
 * reduce SQS pull costs when few messages are coming in.</li>
 * <li>{@link CompositeBackPressureHandler}: Allows combining multiple {@link BackPressureHandler} together and ensures
 * they cooperate.</li>
 * </ul>
 * <p>
 * Below are a few examples of how common use cases can be achieved. Keep in mind you can always create your own
 * {@link BackPressureHandler} implementation and if needed combine it with the provided ones thanks to the
 * {@link CompositeBackPressureHandler}.
 *
 * <h3>A BackPressureHandler limiting the max concurrency with high throughput</h3>
 *
 * <pre>{@code
 * containerOptionsBuilder.backPressureHandlerFactory(containerOptions -> {
 * 		return ConcurrencyLimiterBlockingBackPressureHandler.builder()
 * 			.batchSize(containerOptions.getMaxMessagesPerPoll())
 * 			.totalPermits(containerOptions.getMaxConcurrentMessages())
 * 			.acquireTimeout(containerOptions.getMaxDelayBetweenPolls())
 * 			.throughputConfiguration(BackPressureMode.FIXED_HIGH_THROUGHPUT)
 * 			.build()
 * }}</pre>
 *
 * <h3>A BackPressureHandler limiting the max concurrency with dynamic throughput</h3>
 *
 * <pre>{@code
 * containerOptionsBuilder.backPressureHandlerFactory(containerOptions -> {
 * 		int batchSize = containerOptions.getMaxMessagesPerPoll();
 * 		var concurrencyLimiterBlockingBackPressureHandler = ConcurrencyLimiterBlockingBackPressureHandler.builder()
 * 			.batchSize(batchSize)
 * 			.totalPermits(containerOptions.getMaxConcurrentMessages())
 * 			.acquireTimeout(containerOptions.getMaxDelayBetweenPolls())
 * 			.throughputConfiguration(BackPressureMode.AUTO)
 * 			.build()
 * 		var throughputBackPressureHandler = ThroughputBackPressureHandler.builder()
 * 			.batchSize(batchSize)
 * 			.build();
 * 		return new CompositeBackPressureHandler(List.of(
 * 				concurrencyLimiterBlockingBackPressureHandler,
 * 				throughputBackPressureHandler
 * 			),
 * 			batchSize,
 * 			standbyLimitPollingInterval
 * 		);
 * }}</pre>
 */
public interface BackPressureHandlerFactory {

	/**
	 * Creates a new {@link BackPressureHandler} instance based on the provided {@link ContainerOptions}.
	 * <p>
	 * <strong>NOTE:</strong> <em>it is important for the factory to always return a new instance as otherwise it might
	 * result in a BackPressureHandler internal resources (counters, semaphores, ...) to be shared by multiple
	 * containers which is very likely not the desired behavior.</em>
	 *
	 * @param containerOptions the container options to use for creating the BackPressureHandler.
	 * @return the created BackPressureHandler
	 */
	BackPressureHandler createBackPressureHandler(ContainerOptions<?, ?> containerOptions);

	/**
	 * Creates a new {@link SemaphoreBackPressureHandler} instance based on the provided {@link ContainerOptions}.
	 *
	 * @param options the container options.
	 * @return the created SemaphoreBackPressureHandler.
	 */
	static BatchAwareBackPressureHandler semaphoreBackPressureHandler(ContainerOptions<?, ?> options) {
		return SemaphoreBackPressureHandler.builder().batchSize(options.getMaxMessagesPerPoll())
				.totalPermits(options.getMaxConcurrentMessages()).acquireTimeout(options.getMaxDelayBetweenPolls())
				.throughputConfiguration(options.getBackPressureMode()).build();
	}

	/**
	 * Creates a new {@link BackPressureHandler} instance based on the provided {@link ContainerOptions} combining a
	 * {@link ConcurrencyLimiterBlockingBackPressureHandler}, a {@link ThroughputBackPressureHandler} and a
	 * {@link FullBatchBackPressureHandler}. The exact combination of depends on the given {@link ContainerOptions}.
	 *
	 * @param options the container options.
	 * @param maxIdleWaitTime the maximum amount of time to wait for a permit to be released in case no permits were
	 *     obtained.
	 * @return the created SemaphoreBackPressureHandler.
	 */
	static BatchAwareBackPressureHandler concurrencyLimiterBackPressureHandler(ContainerOptions<?, ?> options,
			Duration maxIdleWaitTime) {
		BackPressureMode backPressureMode = options.getBackPressureMode();

		var concurrencyLimiterBlockingBackPressureHandler = concurrencyLimiterBackPressureHandler2(options);
		if (backPressureMode == BackPressureMode.FIXED_HIGH_THROUGHPUT) {
			return concurrencyLimiterBlockingBackPressureHandler;
		}
		var backPressureHandlers = new ArrayList<BackPressureHandler>();
		backPressureHandlers.add(concurrencyLimiterBlockingBackPressureHandler);

		// The ThroughputBackPressureHandler should run second in the chain as it is non-blocking.
		// Running it first would result in more polls as it would potentially limit the
		// ConcurrencyLimiterBlockingBackPressureHandler to a lower amount of requested permits
		// which means the ConcurrencyLimiterBlockingBackPressureHandler blocking behavior would
		// not be optimally leveraged.
		if (backPressureMode == BackPressureMode.AUTO
				|| backPressureMode == BackPressureMode.ALWAYS_POLL_MAX_MESSAGES) {
			backPressureHandlers.add(throughputBackPressureHandler(options));
		}

		// The FullBatchBackPressureHandler should run last in the chain to ensure that a full batch is requested or not
		if (backPressureMode == BackPressureMode.ALWAYS_POLL_MAX_MESSAGES) {
			backPressureHandlers.add(fullBatchBackPressureHandler(options));
		}
		return compositeBackPressureHandler(options, maxIdleWaitTime, backPressureHandlers);
	}

	/**
	 * Creates a new {@link ConcurrencyLimiterBlockingBackPressureHandler} instance based on the provided
	 * {@link ContainerOptions}.
	 *
	 * @param options the container options.
	 * @return the created ConcurrencyLimiterBlockingBackPressureHandler.
	 */
	static CompositeBackPressureHandler compositeBackPressureHandler(ContainerOptions<?, ?> options,
			Duration maxIdleWaitTime, List<BackPressureHandler> backPressureHandlers) {
		return new CompositeBackPressureHandler(List.copyOf(backPressureHandlers), options.getMaxMessagesPerPoll(),
				maxIdleWaitTime);
	}

	/**
	 * Creates a new {@link ConcurrencyLimiterBlockingBackPressureHandler} instance based on the provided
	 * {@link ContainerOptions}.
	 * @param options the container options.
	 * @return the created ConcurrencyLimiterBlockingBackPressureHandler.
	 */
	static ConcurrencyLimiterBlockingBackPressureHandler concurrencyLimiterBackPressureHandler2(
			ContainerOptions<?, ?> options) {
		return ConcurrencyLimiterBlockingBackPressureHandler.builder().batchSize(options.getMaxMessagesPerPoll())
				.totalPermits(options.getMaxConcurrentMessages()).throughputConfiguration(options.getBackPressureMode())
				.acquireTimeout(options.getMaxDelayBetweenPolls()).build();
	}

	/**
	 * Creates a new {@link ThroughputBackPressureHandler} instance based on the provided {@link ContainerOptions}.
	 * @param options the container options.
	 * @return the created ThroughputBackPressureHandler.
	 */
	static ThroughputBackPressureHandler throughputBackPressureHandler(ContainerOptions<?, ?> options) {
		return ThroughputBackPressureHandler.builder().batchSize(options.getMaxMessagesPerPoll())
				.totalPermits(options.getMaxConcurrentMessages()).build();
	}

	/**
	 * Creates a new {@link FullBatchBackPressureHandler} instance based on the provided {@link ContainerOptions}.
	 * @param options the container options.
	 * @return the created FullBatchBackPressureHandler.
	 */
	static FullBatchBackPressureHandler fullBatchBackPressureHandler(ContainerOptions<?, ?> options) {
		return FullBatchBackPressureHandler.builder().batchSize(options.getMaxMessagesPerPoll()).build();
	}
}
