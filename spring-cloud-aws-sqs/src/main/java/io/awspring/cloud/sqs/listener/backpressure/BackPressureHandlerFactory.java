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

import io.awspring.cloud.sqs.listener.ContainerOptions;

/**
 * Factory interface for creating {@link BackPressureHandler} instances to manage queue consumption backpressure.
 * <p>
 * Implementations of this interface are responsible for producing a new {@link BackPressureHandler} for each container,
 * configured according to the provided {@link ContainerOptions}. This ensures that internal resources (such as counters
 * or semaphores) are not shared across containers, which could lead to unintended side effects.
 * <p>
 * Default factory implementations can be found in the {@link BackPressureHandlerFactories} class.
 *
 * @author Loïc Rouchon
 * @author Tomaz Fernandes
 *
 * @see BackPressureHandlerFactories
 */
@FunctionalInterface
public interface BackPressureHandlerFactory {

	/**
	 * Creates a new {@link BackPressureHandler} instance based on the provided {@link ContainerOptions}.
	 * <p>
	 * <strong>NOTE:</strong> <em>it is important for the factory to always return a new instance as otherwise it might
	 * result in a BackPressureHandler internal resources (counters, semaphores, ...) to be shared by multiple
	 * containers, unless that's the desired behavior.</em>
	 *
	 * @param containerOptions the container options to use for creating the BackPressureHandler.
	 * @return the created BackPressureHandler
	 */
	BackPressureHandler createBackPressureHandler(ContainerOptions<?, ?> containerOptions);
}
