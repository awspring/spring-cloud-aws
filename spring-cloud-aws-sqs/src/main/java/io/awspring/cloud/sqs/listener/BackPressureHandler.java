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
 * Abstraction to handle backpressure within a {@link io.awspring.cloud.sqs.listener.source.PollingMessageSource}.
 *
 * Release methods must be thread-safe so that many messages can be processed asynchronously. Example strategies are
 * semaphore-based, rate limiter-based, a mix of both, or any other.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface BackPressureHandler {

	/**
	 * Request a number of permits. Each obtained permit allows the
	 * {@link io.awspring.cloud.sqs.listener.source.MessageSource} to retrieve one message.
	 * @param amount the amount of permits to request.
	 * @return the amount of permits obtained.
	 * @throws InterruptedException if the Thread is interrupted while waiting for permits.
	 */
	int request(int amount) throws InterruptedException;

	/**
	 * Release the specified amount of permits. Each message that has been processed should release one permit, whether
	 * processing was successful or not.
	 * @param amount the amount of permits to release.
	 */
	void release(int amount);

	/**
	 * Attempts to acquire all permits up to the specified timeout. If successful, means all permits were returned and
	 * thus no activity is left in the {@link io.awspring.cloud.sqs.listener.source.MessageSource}.
	 * @param timeout the maximum amount of time to wait for all permits to be released.
	 * @return whether all permits were acquired.
	 */
	boolean drain(Duration timeout);

}
