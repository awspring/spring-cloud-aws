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
}
