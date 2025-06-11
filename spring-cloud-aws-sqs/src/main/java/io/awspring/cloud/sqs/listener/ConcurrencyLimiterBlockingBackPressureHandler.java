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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * {@link BackPressureHandler} implementation that uses a {@link Semaphore} for handling backpressure.
 *
 * @author Tomaz Fernandes
 * @see io.awspring.cloud.sqs.listener.source.PollingMessageSource
 * @since 3.0
 */
public class ConcurrencyLimiterBlockingBackPressureHandler
		implements BatchAwareBackPressureHandler, IdentifiableContainerComponent {

	private static final Logger logger = LoggerFactory.getLogger(ConcurrencyLimiterBlockingBackPressureHandler.class);

	private final Semaphore semaphore;

	private final int batchSize;

	private final int totalPermits;

	private final Duration acquireTimeout;

	private final boolean alwaysPollMasMessages;

	private String id = getClass().getSimpleName();

	private ConcurrencyLimiterBlockingBackPressureHandler(Builder builder) {
		this.batchSize = builder.batchSize;
		this.totalPermits = builder.totalPermits;
		this.acquireTimeout = builder.acquireTimeout;
		this.alwaysPollMasMessages = BackPressureMode.ALWAYS_POLL_MAX_MESSAGES.equals(builder.backPressureMode);
		this.semaphore = new Semaphore(totalPermits);
		logger.debug(
				"ConcurrencyLimiterBlockingBackPressureHandler created with configuration "
						+ "totalPermits: {}, batchSize: {}, acquireTimeout: {}, an alwaysPollMasMessages: {}",
				this.totalPermits, this.batchSize, this.acquireTimeout, this.alwaysPollMasMessages);
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
		return request(this.batchSize);
	}

	@Override
	public int request(int amount) throws InterruptedException {
		int acquiredPermits = tryAcquire(amount, this.acquireTimeout);
		if (alwaysPollMasMessages || acquiredPermits > 0) {
			return acquiredPermits;
		}
		int availablePermits = Math.min(this.semaphore.availablePermits(), amount);
		if (availablePermits > 0) {
			return tryAcquire(availablePermits, this.acquireTimeout);
		}
		return 0;
	}

	private int tryAcquire(int amount, Duration duration) throws InterruptedException {
		if (this.semaphore.tryAcquire(amount, duration.toMillis(), TimeUnit.MILLISECONDS)) {
			logger.debug("[{}] Acquired {} permits ({} / {} available)", this.id, amount,
					this.semaphore.availablePermits(), this.totalPermits);
			return amount;
		}
		return 0;
	}

	@Override
	public void release(int amount, ReleaseReason reason) {
		this.semaphore.release(amount);
		logger.debug("[{}] Released {} permits ({}) ({} / {} available)", this.id, amount, reason,
				this.semaphore.availablePermits(), this.totalPermits);
	}

	@Override
	public boolean drain(Duration timeout) {
		logger.debug("[{}] Waiting for up to {} for approx. {} permits to be released", this.id, timeout,
				this.totalPermits - this.semaphore.availablePermits());
		try {
			return tryAcquire(this.totalPermits, timeout) > 0;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.debug("[{}] Draining interrupted", this.id);
			return false;
		}
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

		public ConcurrencyLimiterBlockingBackPressureHandler build() {
			Assert.noNullElements(
					Arrays.asList(this.batchSize, this.totalPermits, this.acquireTimeout, this.backPressureMode),
					"Missing configuration");
			Assert.isTrue(this.batchSize > 0, "The batch size must be greater than 0");
			Assert.isTrue(this.totalPermits >= this.batchSize, "Total permits must be greater than the batch size");
			return new ConcurrencyLimiterBlockingBackPressureHandler(this);
		}
	}
}
