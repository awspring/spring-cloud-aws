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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link BatchAwareBackPressureHandler} implementation that uses an internal {@link Semaphore} for adapting the
 * maximum number of permits that can be acquired by the {@link #backPressureHandler} based on the downstream
 * backpressure limit computed by the {@link #backPressureLimiter}.
 *
 * @see BackPressureLimiter
 */
public class BackPressureHandlerLimiter implements BatchAwareBackPressureHandler, IdentifiableContainerComponent {

	/**
	 * The {@link BackPressureLimiter} which computes a limit on how many permits can be requested at a given moment.
	 */
	private final BackPressureLimiter backPressureLimiter;

	/**
	 * The duration to wait for permits to be acquired.
	 */
	private final Duration acquireTimeout;

	/**
	 * The duration to sleep when the queue processing is in standby.
	 */
	private final Duration standbyLimitPollingInterval;

	/**
	 * The limit of permits that can be acquired at the current time. The permits limit is defined in the [0,
	 * Integer.MAX_VALUE] interval. A value of {@literal 0} means that no permits can be acquired.
	 * <p>
	 * This value is updated based on the downstream backpressure reported by the {@link #backPressureLimiter}.
	 */
	private final AtomicInteger permitsLimit = new AtomicInteger(0);

	private final ReducibleSemaphore semaphore = new ReducibleSemaphore(0);

	private final int batchSize;

	private String id;

	public BackPressureHandlerLimiter(BackPressureLimiter backPressureLimiter, Duration acquireTimeout,
			Duration standbyLimitPollingInterval, int batchSize) {
		this.backPressureLimiter = backPressureLimiter;
		this.acquireTimeout = acquireTimeout;
		this.standbyLimitPollingInterval = standbyLimitPollingInterval;
		this.batchSize = batchSize;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public int requestBatch() throws InterruptedException {
		return request(batchSize);
	}

	@Override
	public int request(int amount) throws InterruptedException {
		int permits = Math.min(updatePermitsLimit(), amount);
		if (permits == 0) {
			Thread.sleep(standbyLimitPollingInterval.toMillis());
			return 0;
		}
		if (semaphore.tryAcquire(permits, acquireTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
			return permits;
		}
		return 0;
	}

	@Override
	public void release(int amount, ReleaseReason reason) {
		semaphore.release(amount);
	}

	@Override
	public boolean drain(Duration timeout) {
		return true;
	}

	private int updatePermitsLimit() {
		return permitsLimit.updateAndGet(oldLimit -> {
			int newLimit = Math.max(0, backPressureLimiter.limit());
			if (newLimit < oldLimit) {
				int blockedPermits = oldLimit - newLimit;
				semaphore.reducePermits(blockedPermits);
			}
			else if (newLimit > oldLimit) {
				int releasedPermits = newLimit - oldLimit;
				semaphore.release(releasedPermits);
			}
			return newLimit;
		});
	}

	private static class ReducibleSemaphore extends Semaphore {

		ReducibleSemaphore(int permits) {
			super(permits);
		}

		@Override
		public void reducePermits(int reduction) {
			super.reducePermits(reduction);
		}
	}
}
