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

	private final Semaphore semaphore;

	private final int batchSize;

	private final int totalPermits;

	private final Duration acquireTimeout;

	private final BackPressureMode backPressureConfiguration;

	private volatile CurrentThroughputMode currentThroughputMode;

	private final AtomicInteger lowThroughputPermitsAcquired = new AtomicInteger(0);

	private String id;

	private SemaphoreBackPressureHandler(Builder builder) {
		this.batchSize = builder.batchSize;
		this.totalPermits = builder.totalPermits;
		this.acquireTimeout = builder.acquireTimeout;
		this.backPressureConfiguration = builder.backPressureMode;
		this.semaphore = new Semaphore(totalPermits);
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
	public int requestBatch() throws InterruptedException {
		return request(batchSize);
	}

	// @formatter:off
	@Override
	public int request(int amount) throws InterruptedException {
		return CurrentThroughputMode.LOW.equals(this.currentThroughputMode)
			? requestInLowThroughputMode(amount)
			: requestInHighThroughputMode(amount);
	}

	private int requestInHighThroughputMode(int amount) throws InterruptedException {
		return tryAcquire(amount, CurrentThroughputMode.HIGH)
			? amount
			: tryAcquirePartial(amount);
	}
	// @formatter:on

	private int tryAcquirePartial(int max) throws InterruptedException {
		int availablePermits = this.semaphore.availablePermits();
		if (availablePermits == 0 || BackPressureMode.ALWAYS_POLL_MAX_MESSAGES.equals(this.backPressureConfiguration)) {
			return 0;
		}
		int permitsToRequest = Math.min(availablePermits, max);
		CurrentThroughputMode currentThroughputModeNow = this.currentThroughputMode;
		logger.trace("Trying to acquire partial batch of {} permits from {} available for {} in TM {}",
				permitsToRequest, availablePermits, this.id, currentThroughputModeNow);
		boolean hasAcquiredPartial = tryAcquire(permitsToRequest, currentThroughputModeNow);
		return hasAcquiredPartial ? permitsToRequest : 0;
	}

	private int requestInLowThroughputMode(int amount) throws InterruptedException {
		// Although LTM can be set / unset by many processes, only the MessageSource thread gets here,
		// so no actual concurrency
		logger.debug("Trying to acquire full permits for {}. Permits left: {}", this.id,
				this.semaphore.availablePermits());
		boolean hasAcquired = tryAcquire(this.totalPermits, CurrentThroughputMode.LOW);
		if (hasAcquired) {
			logger.debug("Acquired full permits for {}. Permits left: {}", this.id, this.semaphore.availablePermits());
			// We've acquired all permits - there's no other process currently processing messages
			if (this.lowThroughputPermitsAcquired.getAndSet(amount) != 0) {
				logger.warn("hasAcquiredFullPermits was already true. Permits left: {}",
						this.semaphore.availablePermits());
			}
			return amount;
		}
		else {
			return 0;
		}
	}

	private boolean tryAcquire(int amount, CurrentThroughputMode currentThroughputModeNow) throws InterruptedException {
		logger.trace("Acquiring {} permits for {} in TM {}", amount, this.id, this.currentThroughputMode);
		boolean hasAcquired = this.semaphore.tryAcquire(amount, this.acquireTimeout.toMillis(), TimeUnit.MILLISECONDS);
		if (hasAcquired) {
			logger.trace("{} permits acquired for {} in TM {}. Permits left: {}", amount, this.id,
					currentThroughputModeNow, this.semaphore.availablePermits());
		}
		else {
			logger.trace("Not able to acquire {} permits in {} milliseconds for {} in TM {}. Permits left: {}", amount,
					this.acquireTimeout.toMillis(), this.id, currentThroughputModeNow,
					this.semaphore.availablePermits());
		}
		return hasAcquired;
	}

	@Override
	public void release(int amount, ReleaseReason reason) {
		logger.trace("Releasing {} permits ({}) for {}. Permits left: {}", amount, reason, this.id,
				this.semaphore.availablePermits());
		switch (reason) {
		case NONE_FETCHED -> maybeSwitchToLowThroughputMode();
		case PARTIAL_FETCH -> maybeSwitchToHighThroughputMode(amount);
		case PROCESSED, LIMITED -> {
			// No need to switch throughput mode
		}
		}
		int permitsToRelease = getPermitsToRelease(amount);
		this.semaphore.release(permitsToRelease);
		logger.debug("Released {} permits ({}) for {}. Permits left: {}", permitsToRelease, reason, this.id,
				this.semaphore.availablePermits());
	}

	private void maybeSwitchToLowThroughputMode() {
		if (!BackPressureMode.FIXED_HIGH_THROUGHPUT.equals(this.backPressureConfiguration)
				&& CurrentThroughputMode.HIGH.equals(this.currentThroughputMode)) {
			logger.debug("Entire batch of permits released for {}, setting TM LOW. Permits left: {}", this.id,
					this.semaphore.availablePermits());
			this.currentThroughputMode = CurrentThroughputMode.LOW;
		}
	}

	private void maybeSwitchToHighThroughputMode(int amount) {
		if (CurrentThroughputMode.LOW.equals(this.currentThroughputMode)) {
			logger.debug("{} unused permit(s), setting TM HIGH for {}. Permits left: {}", amount, this.id,
					this.semaphore.availablePermits());
			this.currentThroughputMode = CurrentThroughputMode.HIGH;
		}
	}

	private int getPermitsToRelease(int amount) {
		int lowThroughputPermits = this.lowThroughputPermitsAcquired.getAndSet(0);
		return lowThroughputPermits > 0
				// The first process that gets here should release all permits except for inflight messages
				// We can have only one batch of messages at this point since we have all permits
				? this.totalPermits - (lowThroughputPermits - amount)
				: amount;
	}

	@Override
	public boolean drain(Duration timeout) {
		logger.debug("Waiting for up to {} seconds for approx. {} permits to be released for {}", timeout.getSeconds(),
				this.totalPermits - this.semaphore.availablePermits(), this.id);
		try {
			return this.semaphore.tryAcquire(this.totalPermits, (int) timeout.getSeconds(), TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting to acquire permits", e);
		}
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

		public SemaphoreBackPressureHandler build() {
			Assert.noNullElements(
					Arrays.asList(this.batchSize, this.totalPermits, this.acquireTimeout, this.backPressureMode),
					"Missing configuration");
			return new SemaphoreBackPressureHandler(this);
		}

	}

}
