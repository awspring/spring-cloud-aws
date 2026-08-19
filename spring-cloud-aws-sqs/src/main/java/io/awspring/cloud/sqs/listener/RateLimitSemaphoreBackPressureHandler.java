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
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.concurrent.TimedSemaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * {@link BackPressureHandler} implementation that uses a {@link Semaphore} for handling backpressure.
 *
 * @author Jeroen Vandevelde
 * @see io.awspring.cloud.sqs.listener.source.PollingMessageSource
 * @since 3.0
 */
public class RateLimitSemaphoreBackPressureHandler
		implements BatchAwareBackPressureHandler, IdentifiableContainerComponent {

	private static final Logger logger = LoggerFactory.getLogger(RateLimitSemaphoreBackPressureHandler.class);

	private final Semaphore semaphore;
	private final TimedSemaphore timedSemaphore;

	private final int batchSize;

	private final int totalPermits;

	private final Duration acquireTimeout;

	private final BackPressureMode backPressureConfiguration;

	private volatile CurrentThroughputMode currentThroughputMode;

	private final AtomicBoolean hasAcquiredFullPermits = new AtomicBoolean(false);

	private String id;

	private RateLimitSemaphoreBackPressureHandler(Builder builder) {
		this.batchSize = builder.batchSize;
		this.totalPermits = builder.totalPermits;
		this.acquireTimeout = builder.acquireTimeout;
		this.backPressureConfiguration = builder.backPressureMode;
		this.semaphore = new Semaphore(totalPermits);
		this.timedSemaphore = new TimedSemaphore(1, TimeUnit.SECONDS, 10);
		this.currentThroughputMode = BackPressureMode.FIXED_HIGH_THROUGHPUT.equals(backPressureConfiguration)
				? CurrentThroughputMode.HIGH
				: CurrentThroughputMode.LOW;
		logger.debug("SemaphoreBackPressureHandler created with configuration {} and {} total permits",
				backPressureConfiguration, totalPermits);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public int request(int amount) throws InterruptedException {
		if (timedSemaphore.getAvailablePermits() >= amount) {
			for (int i = 0; i < amount; i++) {
				boolean acquired = timedSemaphore.tryAcquire();
				if (!acquired) {
					return i;
				}
			}
			return amount;
		}
		else if (timedSemaphore.getAvailablePermits() > 0) {
			for (int i = 0; i < timedSemaphore.getAvailablePermits(); i++) {
				boolean acquired = timedSemaphore.tryAcquire();
				if (!acquired) {
					return i;
				}
			}
			return amount;
		}
		else {
			return 0;
		}
	}

	// @formatter:off
	@Override
	public int requestBatch() throws InterruptedException {
		return CurrentThroughputMode.LOW.equals(this.currentThroughputMode)
			? requestInLowThroughputMode()
			: requestInHighThroughputMode();
	}

	private int requestInHighThroughputMode() throws InterruptedException {
		return tryAcquire(this.batchSize, CurrentThroughputMode.HIGH)
			? this.batchSize
			: tryAcquirePartial();
	}
	// @formatter:on

	private int tryAcquirePartial() throws InterruptedException {
		int availablePermits = this.timedSemaphore.getAvailablePermits();
		if (availablePermits == 0 || BackPressureMode.ALWAYS_POLL_MAX_MESSAGES.equals(this.backPressureConfiguration)) {
			return 0;
		}
		int permitsToRequest = Math.min(availablePermits, this.batchSize);
		CurrentThroughputMode currentThroughputModeNow = this.currentThroughputMode;
		logger.trace("Trying to acquire partial batch of {} permits from {} available for {} in TM {}",
				permitsToRequest, availablePermits, this.id, currentThroughputModeNow);
		boolean hasAcquiredPartial = tryAcquire(permitsToRequest, currentThroughputModeNow);
		return hasAcquiredPartial ? permitsToRequest : 0;
	}

	private int requestInLowThroughputMode() throws InterruptedException {
		// Although LTM can be set / unset by many processes, only the MessageSource thread gets here,
		// so no actual concurrency
		logger.debug("Trying to acquire full permits for {}. Permits left: {}", this.id,
				this.timedSemaphore.getAvailablePermits());
		boolean hasAcquired = tryAcquire(this.totalPermits, CurrentThroughputMode.LOW);
		if (hasAcquired) {
			logger.debug("Acquired full permits for {}. Permits left: {}", this.id,
					this.timedSemaphore.getAvailablePermits());
			// We've acquired all permits - there's no other process currently processing messages
			if (!this.hasAcquiredFullPermits.compareAndSet(false, true)) {
				logger.warn("hasAcquiredFullPermits was already true. Permits left: {}",
						this.timedSemaphore.getAvailablePermits());
			}
			return this.batchSize;
		}
		else {
			return 0;
		}
	}

	private boolean tryAcquire(int amount, CurrentThroughputMode currentThroughputModeNow) throws InterruptedException {
		logger.trace("Acquiring {} permits for {} in TM {}", amount, this.id, this.currentThroughputMode);

		for (int i = 0; i < amount; i++) {
			if (!this.timedSemaphore.tryAcquire()) {
				logger.trace("Not able to acquire {} permits in {} milliseconds for {} in TM {}. Permits left: {}",
						amount, this.acquireTimeout.toMillis(), this.id, currentThroughputModeNow,
						this.timedSemaphore.getAvailablePermits());
				return false;
			}
		}

		logger.trace("{} permits acquired for {} in TM {}. Permits left: {}", amount, this.id, currentThroughputModeNow,
				this.timedSemaphore.getAvailablePermits());
		return true;
	}

	@Override
	public void releaseBatch() {
		maybeSwitchToLowThroughputMode();
		// TODO is this still needed with a timed one
		// int permitsToRelease = getPermitsToRelease(this.batchSize);
		// this.semaphore.release(permitsToRelease);
		// logger.trace("Released {} permits for {}. Permits left: {}", permitsToRelease, this.id,
		// this.timedSemaphore.getAvailablePermits());
	}

	@Override
	public int getBatchSize() {
		return this.batchSize;
	}

	private void maybeSwitchToLowThroughputMode() {
		if (!BackPressureMode.FIXED_HIGH_THROUGHPUT.equals(this.backPressureConfiguration)
				&& CurrentThroughputMode.HIGH.equals(this.currentThroughputMode)) {
			logger.debug("Entire batch of permits released for {}, setting TM LOW. Permits left: {}", this.id,
					this.timedSemaphore.getAvailablePermits());
			this.currentThroughputMode = CurrentThroughputMode.LOW;
		}
	}

	@Override
	public void release(int amount) {
		// logger.trace("Releasing {} permits for {}. Permits left: {}", amount, this.id,
		// this.timedSemaphore.getAvailablePermits());
		// maybeSwitchToHighThroughputMode(amount);
		// int permitsToRelease = getPermitsToRelease(amount);
		// this.semaphore.release(permitsToRelease);
		// logger.trace("Released {} permits for {}. Permits left: {}", permitsToRelease, this.id,
		// this.timedSemaphore.getAvailablePermits());

		// TODO figure out how to deal with release capabilities
	}

	private int getPermitsToRelease(int amount) {
		return this.hasAcquiredFullPermits.compareAndSet(true, false)
				// The first process that gets here should release all permits except for inflight messages
				// We can have only one batch of messages at this point since we have all permits
				? this.totalPermits - (this.batchSize - amount)
				: amount;
	}

	private void maybeSwitchToHighThroughputMode(int amount) {
		if (CurrentThroughputMode.LOW.equals(this.currentThroughputMode)) {
			logger.debug("{} unused permit(s), setting TM HIGH for {}. Permits left: {}", amount, this.id,
					this.timedSemaphore.getAvailablePermits());
			this.currentThroughputMode = CurrentThroughputMode.HIGH;
		}
	}

	@Override
	public boolean drain(Duration timeout) {
		// TODO figure out a different way to make certain all the messages have been processed
		timedSemaphore.shutdown();
		return true;
	}

	private enum CurrentThroughputMode {

		HIGH,

		LOW;

	}

	public static class Builder {

		private int batchSize;

		private int totalPermits;

		private Duration acquireTimeout;

		private BackPressureMode backPressureMode;

		public Builder batchSize(int batchSize) {
			this.batchSize = batchSize;
			return this;
		}

		public Builder totalPermits(int totalPermits) {
			this.totalPermits = totalPermits;
			return this;
		}

		public Builder acquireTimeout(Duration acquireTimeout) {
			this.acquireTimeout = acquireTimeout;
			return this;
		}

		public Builder throughputConfiguration(BackPressureMode backPressureConfiguration) {
			this.backPressureMode = backPressureConfiguration;
			return this;
		}

		public RateLimitSemaphoreBackPressureHandler build() {
			Assert.noNullElements(
					Arrays.asList(this.batchSize, this.totalPermits, this.acquireTimeout, this.backPressureMode),
					"Missing configuration");
			return new RateLimitSemaphoreBackPressureHandler(this);
		}

	}

}
