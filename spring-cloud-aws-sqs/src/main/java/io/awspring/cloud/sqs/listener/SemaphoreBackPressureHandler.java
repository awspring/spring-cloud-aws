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
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * {@link BackPressureHandler} implementation that uses a {@link Semaphore} for handling backpressure.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see io.awspring.cloud.sqs.listener.source.PollingMessageSource
 */
public class SemaphoreBackPressureHandler implements BatchAwareBackPressureHandler, IdentifiableContainerComponent {

	private static final Logger logger = LoggerFactory.getLogger(SemaphoreBackPressureHandler.class);

	private final BackPressureLimiter backPressureLimiter;

	private final ReducibleSemaphore semaphore;

	private final int batchSize;

	/**
	 * The theoretical maximum numbers of permits that can be acquired if no limit is set.
	 * @see #permitsLimit for the current limit.
	 */
	private final int totalPermits;

	/**
	 * The limit of permits that can be acquired at the current time. The permits limit is defined in the [0,
	 * totalPermits] interval. A value of {@literal 0} means that no permits can be acquired.
	 * <p>
	 * This value is updated based on the downstream backpressure reported by the {@link #backPressureLimiter}.
	 */
	private final AtomicInteger permitsLimit;

	/**
	 * The duration to sleep when the queue processing is in standby.
	 */
	private final Duration standbyLimitPollingInterval;

	private final Duration acquireTimeout;

	private final BackPressureMode backPressureConfiguration;

	private volatile CurrentThroughputMode currentThroughputMode;

	/**
	 * The number of permits acquired in low throughput mode. This value is minimum value between {@link #permitsLimit}
	 * at the time of the acquire and {@link #totalPermits}.
	 */
	private final AtomicInteger lowThroughputAcquiredPermits = new AtomicInteger(0);

	private final AtomicBoolean hasAcquiredFullPermits = new AtomicBoolean(false);

	private String id;

	private final AtomicBoolean isDraining = new AtomicBoolean(false);

	private SemaphoreBackPressureHandler(Builder builder) {
		this.batchSize = builder.batchSize;
		this.totalPermits = builder.totalPermits;
		this.standbyLimitPollingInterval = builder.standbyLimitPollingInterval;
		this.acquireTimeout = builder.acquireTimeout;
		this.backPressureConfiguration = builder.backPressureMode;
		this.semaphore = new ReducibleSemaphore(totalPermits);
		this.currentThroughputMode = BackPressureMode.FIXED_HIGH_THROUGHPUT.equals(backPressureConfiguration)
				? CurrentThroughputMode.HIGH
				: CurrentThroughputMode.LOW;
		logger.debug("SemaphoreBackPressureHandler created with configuration {} and {} total permits",
				backPressureConfiguration, totalPermits);
		this.permitsLimit = new AtomicInteger(totalPermits);
		this.backPressureLimiter = Objects.requireNonNullElse(builder.backPressureLimiter, () -> totalPermits);
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
		updateAvailablePermitsBasedOnDownstreamBackpressure();
		return tryAcquire(amount, this.currentThroughputMode) ? amount : 0;
	}

	// @formatter:off
	@Override
	public int requestBatch() throws InterruptedException {
		updateAvailablePermitsBasedOnDownstreamBackpressure();
		boolean useLowThroughput = CurrentThroughputMode.LOW.equals(this.currentThroughputMode)
			|| this.permitsLimit.get() < this.totalPermits;
		return useLowThroughput	? requestInLowThroughputMode() : requestInHighThroughputMode();
	}

	private int requestInHighThroughputMode() throws InterruptedException {
		return tryAcquire(this.batchSize, CurrentThroughputMode.HIGH)
			? this.batchSize
			: tryAcquirePartial();
	}
	// @formatter:on

	private int tryAcquirePartial() throws InterruptedException {
		int availablePermits = this.semaphore.availablePermits();
		if (availablePermits == 0 || BackPressureMode.ALWAYS_POLL_MAX_MESSAGES.equals(this.backPressureConfiguration)) {
			return 0;
		}
		int permitsToRequest = min(availablePermits, this.batchSize);
		CurrentThroughputMode currentThroughputModeNow = this.currentThroughputMode;
		logger.trace("Trying to acquire partial batch of {} permits from {} (limit {}) available for {} in TM {}",
				permitsToRequest, availablePermits, this.permitsLimit.get(), this.id, currentThroughputModeNow);
		boolean hasAcquiredPartial = tryAcquire(permitsToRequest, currentThroughputModeNow);
		return hasAcquiredPartial ? permitsToRequest : 0;
	}

	private int requestInLowThroughputMode() throws InterruptedException {
		// Although LTM can be set / unset by many processes, only the MessageSource thread gets here,
		// so no actual concurrency
		logger.debug("Trying to acquire full permits for {}. Permits left: {}, Permits limit: {}", this.id,
				this.semaphore.availablePermits(), this.permitsLimit.get());
		int permitsToRequest = min(this.permitsLimit.get(), this.totalPermits);
		if (permitsToRequest == 0) {
			logger.info("No permits usable for {} (limit = 0), sleeping for {}", this.id,
					this.standbyLimitPollingInterval);
			Thread.sleep(standbyLimitPollingInterval.toMillis());
			return 0;
		}
		boolean hasAcquired = tryAcquire(permitsToRequest, CurrentThroughputMode.LOW);
		if (hasAcquired) {
			if (permitsToRequest >= this.totalPermits) {
				logger.debug("Acquired full permits for {}. Permits left: {}, Permits limit: {}", this.id,
						this.semaphore.availablePermits(), this.permitsLimit.get());
			}
			else {
				logger.debug("Acquired limited permits ({}) for {} . Permits left: {}, Permits limit: {}",
						permitsToRequest, this.id, this.semaphore.availablePermits(), this.permitsLimit.get());
			}
			int tokens = min(this.batchSize, permitsToRequest);
			// We've acquired all permits - there's no other process currently processing messages
			if (!this.hasAcquiredFullPermits.compareAndSet(false, true)) {
				logger.warn("hasAcquiredFullPermits was already true. Permits left: {}, Permits limit: {}",
						this.semaphore.availablePermits(), this.permitsLimit.get());
			}
			else {
				lowThroughputAcquiredPermits.set(permitsToRequest);
			}
			return tokens;
		}
		else {
			return 0;
		}
	}

	private boolean tryAcquire(int amount, CurrentThroughputMode currentThroughputModeNow) throws InterruptedException {
		if (isDraining.get()) {
			return false;
		}
		logger.trace("Acquiring {} permits for {} in TM {}", amount, this.id, this.currentThroughputMode);
		boolean hasAcquired = this.semaphore.tryAcquire(amount, this.acquireTimeout.toMillis(), TimeUnit.MILLISECONDS);
		if (hasAcquired) {
			logger.trace("{} permits acquired for {} in TM {}. Permits left: {}, Permits limit: {}", amount, this.id,
					currentThroughputModeNow, this.semaphore.availablePermits(), this.permitsLimit.get());
		}
		else {
			logger.trace(
					"Not able to acquire {} permits in {} milliseconds for {} in TM {}. Permits left: {}, Permits limit: {}",
					amount, this.acquireTimeout.toMillis(), this.id, currentThroughputModeNow,
					this.semaphore.availablePermits(), this.permitsLimit.get());
		}
		return hasAcquired;
	}

	@Override
	public void releaseBatch() {
		maybeSwitchToLowThroughputMode();
		int permitsToRelease = getPermitsToRelease(this.batchSize);
		this.semaphore.release(permitsToRelease);
		logger.trace("Released {} permits for {}. Permits left: {}", permitsToRelease, this.id,
				this.semaphore.availablePermits());
	}

	@Override
	public int getBatchSize() {
		return this.batchSize;
	}

	private void maybeSwitchToLowThroughputMode() {
		if (!BackPressureMode.FIXED_HIGH_THROUGHPUT.equals(this.backPressureConfiguration)
				&& CurrentThroughputMode.HIGH.equals(this.currentThroughputMode)) {
			logger.debug("Entire batch of permits released for {}, setting TM LOW. Permits left: {}", this.id,
					this.semaphore.availablePermits());
			this.currentThroughputMode = CurrentThroughputMode.LOW;
		}
	}

	@Override
	public void release(int amount) {
		logger.trace("Releasing {} permits for {}. Permits left: {}", amount, this.id,
				this.semaphore.availablePermits());
		maybeSwitchToHighThroughputMode(amount);
		int permitsToRelease = getPermitsToRelease(amount);
		this.semaphore.release(permitsToRelease);
		logger.trace("Released {} permits for {}. Permits left: {}", permitsToRelease, this.id,
				this.semaphore.availablePermits());
	}

	private int getPermitsToRelease(int amount) {
		if (this.hasAcquiredFullPermits.compareAndSet(true, false)) {
			int allAcquiredPermits = this.lowThroughputAcquiredPermits.getAndSet(0);
			// The first process that gets here should release all permits except for inflight messages
			// We can have only one batch of messages at this point since we have all permits
			return (allAcquiredPermits - (min(this.batchSize, allAcquiredPermits) - amount));
		}
		return amount;
	}

	private void maybeSwitchToHighThroughputMode(int amount) {
		if (CurrentThroughputMode.LOW.equals(this.currentThroughputMode)) {
			logger.debug("{} unused permit(s), setting TM HIGH for {}. Permits left: {}", amount, this.id,
					this.semaphore.availablePermits());
			this.currentThroughputMode = CurrentThroughputMode.HIGH;
		}
	}

	@Override
	public boolean drain(Duration timeout) {
		logger.debug("Waiting for up to {} seconds for approx. {} permits to be released for {}", timeout.getSeconds(),
				this.totalPermits - this.semaphore.availablePermits(), this.id);
		isDraining.set(true);
		updateMaxPermitsLimit(this.totalPermits);
		try {
			return this.semaphore.tryAcquire(this.totalPermits, (int) timeout.getSeconds(), TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting to acquire permits", e);
		}
	}

	private int min(int a, int p) {
		return Math.max(0, Math.min(a, p));
	}

	private void updateAvailablePermitsBasedOnDownstreamBackpressure() {
		if (!isDraining.get()) {
			int limit = backPressureLimiter.limit();
			int newCurrentMaxPermits = min(limit, totalPermits);
			updateMaxPermitsLimit(newCurrentMaxPermits);
			if (isDraining.get()) {
				updateMaxPermitsLimit(totalPermits);
			}
		}
	}

	private void updateMaxPermitsLimit(int newCurrentMaxPermits) {
		int oldValue = permitsLimit.getAndUpdate(i -> min(newCurrentMaxPermits, totalPermits));
		if (newCurrentMaxPermits < oldValue) {
			int blockedPermits = oldValue - newCurrentMaxPermits;
			semaphore.reducePermits(blockedPermits);
		}
		else if (newCurrentMaxPermits > oldValue) {
			int releasedPermits = newCurrentMaxPermits - oldValue;
			semaphore.release(releasedPermits);
		}
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

	private enum CurrentThroughputMode {

		HIGH,

		LOW;

	}

	public static class Builder {

		private int batchSize;

		private int totalPermits;

		private Duration standbyLimitPollingInterval;

		private Duration acquireTimeout;

		private BackPressureMode backPressureMode;

		private BackPressureLimiter backPressureLimiter;

		public Builder batchSize(int batchSize) {
			this.batchSize = batchSize;
			return this;
		}

		public Builder totalPermits(int totalPermits) {
			this.totalPermits = totalPermits;
			return this;
		}

		public Builder standbyLimitPollingInterval(Duration standbyLimitPollingInterval) {
			this.standbyLimitPollingInterval = standbyLimitPollingInterval;
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

		public Builder backPressureLimiter(BackPressureLimiter backPressureLimiter) {
			this.backPressureLimiter = backPressureLimiter;
			return this;
		}

		public SemaphoreBackPressureHandler build() {
			Assert.noNullElements(Arrays.asList(this.batchSize, this.totalPermits, this.standbyLimitPollingInterval,
					this.acquireTimeout, this.backPressureMode), "Missing configuration");
			return new SemaphoreBackPressureHandler(this);
		}

	}

}
