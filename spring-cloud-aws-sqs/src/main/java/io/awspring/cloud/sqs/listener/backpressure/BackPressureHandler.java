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
package io.awspring.cloud.sqs.listener.backpressure;

import java.time.Duration;

/**
 * Abstraction to handle backpressure within a {@link io.awspring.cloud.sqs.listener.source.PollingMessageSource}.
 * Release methods must be thread-safe so that many messages can be processed asynchronously. Example strategies are
 * semaphore-based, rate limiter-based, a mix of both, or any other.
 *
 * @author Tomaz Fernandes
 * @author Loïc Rouchon
 * @since 3.0
 */
public interface BackPressureHandler {

	/**
	 * Requests a number of permits. Each obtained permit allows the
	 * {@link io.awspring.cloud.sqs.listener.source.MessageSource} to retrieve one message.
	 * @param amount the amount of permits to request.
	 * @return the amount of permits obtained.
	 * @throws InterruptedException if the Thread is interrupted while waiting for permits.
	 */
	int request(int amount) throws InterruptedException;

	/**
	 * Releases the specified amount of permits for processed messages. Each message that has been processed should
	 * release one permit, whether processing was successful or not.
	 * <p>
	 * This method can be called in the following use cases:
	 * <ul>
	 * <li>{@link ReleaseReason#LIMITED}: all/some permits were not used because another BackPressureHandler has a lower
	 * permits limit and the difference in permits needs to be returned.</li>
	 * <li>{@link ReleaseReason#NONE_FETCHED}: none of the permits were actually used because no messages were retrieved
	 * from SQS. Permits need to be returned.</li>
	 * <li>{@link ReleaseReason#PARTIAL_FETCH}: some of the permits were used (some messages were retrieved from SQS).
	 * The unused ones need to be returned. The amount to be returned might be {@literal 0}, in which case it means all
	 * the permits will be used as the same number of messages were fetched from SQS.</li>
	 * <li>{@link ReleaseReason#PROCESSED}: a message processing finished, successfully or not.</li>
	 * </ul>
	 * @param amount the amount of permits to release.
	 * @param reason the reason why the permits were released.
	 */
	void release(int amount, ReleaseReason reason);

	/**
	 * Attempts to acquire all permits up to the specified timeout. If successful, means all permits were returned and
	 * thus no activity is left in the {@link io.awspring.cloud.sqs.listener.source.MessageSource}.
	 * @param timeout the maximum amount of time to wait for all permits to be released.
	 * @return whether all permits were acquired.
	 */
	boolean drain(Duration timeout);

	/**
	 * @author Loïc Rouchon
	 * @since 4.0.0
	 */
	enum ReleaseReason {
		/**
		 * All/Some permits were not used because another BackPressureHandler has a lower permits limit and the permits
		 * difference need to be aligned across all handlers.
		 */
		LIMITED,
		/**
		 * No messages were retrieved from SQS, so all permits need to be returned.
		 */
		NONE_FETCHED,
		/**
		 * Some messages were fetched from SQS. Unused permits if any need to be returned.
		 */
		PARTIAL_FETCH,
		/**
		 * The processing of one or more messages finished, successfully or not.
		 */
		PROCESSED;
	}

}
