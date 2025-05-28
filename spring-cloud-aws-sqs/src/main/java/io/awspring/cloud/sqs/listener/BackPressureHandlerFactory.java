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
 * A set of default implementations are provided by the {@link BackPressureHandlerFactories} class.
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
