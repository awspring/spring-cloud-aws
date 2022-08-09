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
package io.awspring.cloud.sqs.listener;

import java.time.Duration;

/**
 * Abstraction to handle backpressure within a {@link io.awspring.cloud.sqs.listener.source.MessageSource}.
 *
 * Implementations must be thread-safe if being shared among sources within a container. Strategies can be
 * semaphore-based, rate limiter-based, a mix of both, or any other.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface BackPressureHandler {

	int request(int amount) throws InterruptedException;

	void release(int amount);

	boolean drain(Duration timeout);

}
