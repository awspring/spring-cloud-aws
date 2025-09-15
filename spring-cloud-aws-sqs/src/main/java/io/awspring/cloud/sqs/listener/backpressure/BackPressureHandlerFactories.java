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
package io.awspring.cloud.sqs.listener.backpressure;

import io.awspring.cloud.sqs.listener.BackPressureMode;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for creating {@link BackPressureHandlerFactory} instances used to apply back pressure strategies in SQS
 * listener containers.
 *
 * <p>
 * The factories provided by this class can be passed to ContainerOptions during container configuration.
 *
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * factory.configure(options -> options.maxConcurrentMessages(100).maxDelayBetweenPolls(Duration.ofSeconds(1))
 * 		.maxMessagesPerPoll(8)
 * 		.backPressureHandlerFactory(BackPressureHandlerFactories.concurrencyLimiterBackPressureHandler()));
 * }</pre>
 *
 * <p>
 * The {@link BackPressureMode} setting in {@link ContainerOptions} is only used when using
 * {@link #adaptiveThroughputBackPressureHandler()}. If you're passing a specific factory such as
 * {@code concurrencyLimiterBackPressureHandler()}, the mode will be ignored.
 *
 * <p>
 * The following {@link ContainerOptions} properties are used by the built-in back pressure handler implementations:
 *
 * <ul>
 * <li>{@link ConcurrencyLimiterBlockingBackPressureHandler}
 * <ul>
 * <li>{@code maxConcurrentMessages}</li>
 * <li>{@code maxMessagesPerPoll}</li>
 * <li>{@code maxDelayBetweenPolls}</li>
 * </ul>
 * </li>
 * <li>{@link ThroughputBackPressureHandler}
 * <ul>
 * <li>{@code maxMessagesPerPoll}</li>
 * </ul>
 * </li>
 * <li>{@link FullBatchBackPressureHandler}
 * <ul>
 * <li>{@code maxMessagesPerPoll}</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <p>
 * For advanced scenarios, multiple handlers can be combined using {@link #compositeBackPressureHandler(List)} or
 * {@link #compositeBackPressureHandler(BackPressureHandlerFactory...)}.
 *
 * @since 4.0.0
 * @author Loïc Rouchon
 * @author Tomaz Fernandes
 */
public class BackPressureHandlerFactories {

	private BackPressureHandlerFactories() {
	}

	/**
	 * Creates a factory for an adaptive {@link BackPressureHandler}, combining different strategies based on the
	 * provided {@link ContainerOptions} and its configured {@link BackPressureMode}.
	 *
	 * <p>
	 * This factory dynamically builds a composite handler using one or more of the following:
	 * <ul>
	 * <li>{@link ConcurrencyLimiterBlockingBackPressureHandler}</li>
	 * <li>{@link ThroughputBackPressureHandler}</li>
	 * <li>{@link FullBatchBackPressureHandler}</li>
	 * </ul>
	 *
	 * <p>
	 * The resulting behavior is similar to the original {@code SemaphoreBackpressureHandler} in previous versions.
	 *
	 * <p>
	 * This is the only built-in factory that makes use of {@link BackPressureMode}. If you use a specific factory such
	 * as {@link #concurrencyLimiterBackPressureHandler()} the mode is not consulted.
	 *
	 * <p>
	 * This is the default BackPressureHandlerFactory used by listener containers.
	 *
	 * @return a {@link BackPressureHandlerFactory} producing adaptive handlers.
	 */
	public static BackPressureHandlerFactory adaptiveThroughputBackPressureHandler() {
		return BackPressureHandlerFactories::createAdaptiveThroughputBackPressureHandler;
	}

	private static BackPressureHandler createAdaptiveThroughputBackPressureHandler(ContainerOptions<?, ?> options) {
		BackPressureMode backPressureMode = options.getBackPressureMode();

		var concurrencyLimiterBlockingBackPressureHandler = concurrencyLimiterBackPressureHandler();
		if (backPressureMode == BackPressureMode.FIXED_HIGH_THROUGHPUT) {
			return concurrencyLimiterBlockingBackPressureHandler.createBackPressureHandler(options);
		}
		var backPressureHandlers = new ArrayList<BackPressureHandlerFactory>();
		backPressureHandlers.add(concurrencyLimiterBlockingBackPressureHandler);

		if (backPressureMode == BackPressureMode.AUTO
				|| backPressureMode == BackPressureMode.ALWAYS_POLL_MAX_MESSAGES) {
			backPressureHandlers.add(throughputBackPressureHandler());
		}

		// The FullBatchBackPressureHandler should run last in the chain.
		// If less than a batch was requested as a result of previous handlers, it'll return 0 permits,
		// which will force a new permit requesting round.
		if (backPressureMode == BackPressureMode.ALWAYS_POLL_MAX_MESSAGES) {
			backPressureHandlers.add(fullBatchBackPressureHandler());
		}
		return compositeBackPressureHandler(backPressureHandlers).createBackPressureHandler(options);
	}

	/**
	 * Creates a new {@link CompositeBackPressureHandler} from the given array of {@link BackPressureHandlerFactory}
	 * instances.
	 *
	 * @param backPressureHandlerFactories the factories to compose.
	 * @return a {@link BackPressureHandlerFactory} producing a composite handler.
	 */
	public static BackPressureHandlerFactory compositeBackPressureHandler(
			BackPressureHandlerFactory... backPressureHandlerFactories) {
		return compositeBackPressureHandler(Arrays.asList(backPressureHandlerFactories));
	}

	/**
	 * Creates a new {@link CompositeBackPressureHandler} from the given list of {@link BackPressureHandlerFactory}
	 * instances.
	 *
	 * @param factories the list of factories to compose.
	 * @return a {@link BackPressureHandlerFactory} producing a composite handler.
	 */
	public static BackPressureHandlerFactory compositeBackPressureHandler(List<BackPressureHandlerFactory> factories) {
		return options -> CompositeBackPressureHandler.builder().batchSize(options.getMaxMessagesPerPoll())
				.noPermitsReturnedWaitTimeout(options.getMaxDelayBetweenPolls()).backPressureHandlers(
						factories.stream().map(factory -> factory.createBackPressureHandler(options)).toList())
				.build();
	}

	/**
	 * Creates a new {@link ConcurrencyLimiterBlockingBackPressureHandler} using the provided {@link ContainerOptions}.
	 *
	 * @return a {@link BackPressureHandlerFactory} producing a concurrency-limiting handler.
	 */
	public static BackPressureHandlerFactory concurrencyLimiterBackPressureHandler() {
		return options -> ConcurrencyLimiterBlockingBackPressureHandler.builder()
				.batchSize(options.getMaxMessagesPerPoll()).totalPermits(options.getMaxConcurrentMessages())
				.acquireTimeout(options.getMaxDelayBetweenPolls()).build();
	}

	/**
	 * Creates a new {@link ThroughputBackPressureHandler} using the provided {@link ContainerOptions}.
	 *
	 * @return a {@link BackPressureHandlerFactory} producing a throughput-adaptive handler.
	 */
	public static BackPressureHandlerFactory throughputBackPressureHandler() {
		return options -> ThroughputBackPressureHandler.builder().batchSize(options.getMaxMessagesPerPoll()).build();
	}

	/**
	 * Creates a new {@link FullBatchBackPressureHandler} using the provided {@link ContainerOptions}.
	 *
	 * @return a {@link BackPressureHandlerFactory} producing a full-batch enforcing handler.
	 */
	public static BackPressureHandlerFactory fullBatchBackPressureHandler() {
		return options -> FullBatchBackPressureHandler.builder().batchSize(options.getMaxMessagesPerPoll()).build();
	}
}
