package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.BackPressureMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link BackPressureHandler} implementation that uses a {@link Semaphore} for handling
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SemaphoreBackPressureHandler implements BackPressureHandler {

	private static final Logger logger = LoggerFactory.getLogger(SemaphoreBackPressureHandler.class);

	private final Semaphore semaphore;

	private final int batchSize;

	private final int totalPermits;

	private final Duration acquireTimeout;

	private final BackPressureMode backPressureConfiguration;

	private volatile ThroughputMode currentThroughputMode;

	private final AtomicBoolean hasAcquiredFullPermits = new AtomicBoolean(false);

	private String clientId;

	private SemaphoreBackPressureHandler(Builder builder) {
		this.batchSize = builder.batchSize;
		this.totalPermits = builder.totalPermits;
		this.acquireTimeout = builder.acquireTimeout;
		this.backPressureConfiguration = builder.backPressureMode;
		this.semaphore = new Semaphore(totalPermits);
		this.currentThroughputMode = BackPressureMode.HIGH_THROUGHPUT.equals(backPressureConfiguration)
			? ThroughputMode.HIGH
			: ThroughputMode.LOW;
		logger.debug("SemaphoreBackPressureHandler created with configuration {} and {} total permits", backPressureConfiguration, totalPermits);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	@Override
	public int request() throws InterruptedException {
		return ThroughputMode.LOW.equals(this.currentThroughputMode)
			? requestInLowThroughputMode()
			: requestInHighThroughputMode();
	}

	private int requestInHighThroughputMode() throws InterruptedException {
		return tryAcquire(this.batchSize)
			? this.batchSize
			: tryAcquirePartial();
	}

	private int tryAcquirePartial() throws InterruptedException {
		int availablePermits = this.semaphore.availablePermits();
		if (availablePermits == 0) {
			return 0;
		}
		int permitsToRequest = Math.min(availablePermits, this.batchSize);
		logger.trace("Trying to acquire partial batch of {} permits from {} available for {} in TM {}", permitsToRequest,
			availablePermits, this.clientId, this.currentThroughputMode);
		boolean hasAcquiredPartial = tryAcquire(permitsToRequest);
		return hasAcquiredPartial ? permitsToRequest : 0;
	}

	private int requestInLowThroughputMode() throws InterruptedException {
		// Although LTM can be set / unset by many processes, only the MessageSource thread gets here,
		// so no actual concurrency
		logger.trace("Trying to acquire full permits for {}. Permits left: {}", this.clientId, this.semaphore.availablePermits());
		boolean hasAcquired = tryAcquire(this.totalPermits);
		if (hasAcquired) {
			// We've acquired all permits - there's no other process currently processing messages
			if (!this.hasAcquiredFullPermits.compareAndSet(false, true)) {
				logger.warn("hasAcquiredFullPermits was already true");
			}
			return this.batchSize;
		}
		else {
			return 0;
		}
	}

	private boolean tryAcquire(int amount) throws InterruptedException {
		logger.trace("Acquiring {} permits for {} in TM {}", amount, this.clientId, this.currentThroughputMode);
		boolean hasAcquired = this.semaphore.tryAcquire(amount, this.acquireTimeout.getSeconds(), TimeUnit.SECONDS);
		if (hasAcquired) {
			logger.trace("{} permits acquired for {} in TM {}. Permits left: {}", amount, this.clientId,
				this.currentThroughputMode, this.semaphore.availablePermits());
		}
		else {
			logger.trace("Not able to acquire {} permits in {} seconds for {} in TM {}. Permits left: {}", amount,
				this.acquireTimeout.getSeconds(), this.clientId, this.currentThroughputMode, this.semaphore.availablePermits());
		}
		return hasAcquired;
	}

	@Override
	public void release(int amount) {
		maybeSwitchThroughputMode(amount);
		int permitsToRelease = getPermitsToRelease(amount);
		this.semaphore.release(permitsToRelease);
		logger.trace("Released {} permits for {}. Permits left: {}", permitsToRelease, this.clientId, this.semaphore.availablePermits());
	}

	private int getPermitsToRelease(int amount) {
		return this.hasAcquiredFullPermits.compareAndSet(true, false)
			// The first process that gets here should release all permits except for inflight messages
			// We can have only one batch of messages at this point since we have all permits
			? this.totalPermits - (this.batchSize - amount)
			: amount;
	}

	private void maybeSwitchThroughputMode(int amount) {
		if (!BackPressureMode.AUTO.equals(this.backPressureConfiguration)) {
			return;
		}
		if (amount == this.batchSize && ThroughputMode.HIGH.equals(this.currentThroughputMode)) {
			logger.debug("All {} permits were returned for {}, setting low throughput mode. Permits left: {}",
				this.batchSize, this.clientId, this.semaphore.availablePermits());
			this.currentThroughputMode = ThroughputMode.LOW;
		}
		else if (amount != this.batchSize && ThroughputMode.LOW.equals(this.currentThroughputMode)) {
			logger.debug("Only {} permit(s) returned, setting high throughput mode for {}. Permits left: {}",
				amount, this.clientId, this.semaphore.availablePermits());
			this.currentThroughputMode = ThroughputMode.HIGH;
		}
	}

	@Override
	public boolean drain(Duration timeout) {
		logger.debug("Waiting for up to {} seconds for approx. {} permits to be released for {}", timeout.getSeconds(),
			this.totalPermits - this.semaphore.availablePermits(), this.clientId);
		try {
			return this.semaphore.tryAcquire(this.totalPermits, (int) timeout.getSeconds(),
				TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting to acquire permits", e);
		}
	}

	private enum ThroughputMode {

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
			Assert.noNullElements(Arrays.asList(this.batchSize, this.totalPermits, this.acquireTimeout, this.backPressureMode),
				"Missing configuration");
			return new SemaphoreBackPressureHandler(this);
		}

	}

}
