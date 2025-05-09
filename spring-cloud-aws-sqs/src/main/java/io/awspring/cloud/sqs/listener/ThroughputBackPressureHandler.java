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

import io.awspring.cloud.sqs.listener.source.PollingMessageSource;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Non-blocking {@link BackPressureHandler} implementation that uses a switch between high and low throughput modes.
 * <p>
 * <strong>Throughput modes</strong>
 * <ul>
 * <li>In low-throughput mode, a single batch can be requested at a time. The number of permits that will be delivered
 * is adjusted so that the number of in flight messages will not exceed the batch size.</li>
 * <li>In high-throughput mode, multiple batches can be requested at a time. The number of permits that will be
 * delivered is adjusted so that the number of in flight messages will not exceed the maximum number of concurrent
 * messages. Note that for a single poll the maximum number of permits that will be delivered will not exceed the batch
 * size.</li>
 * </ul>
 * <p>
 * <strong>Throughput mode switch:</strong> The initial throughput mode is the low-throughput mode. If some messages are
 * fetched, then the throughput mode is switched to high-throughput mode. If no messages are returned fetched by a poll,
 * the throughput mode is switched back to low-throughput mode.
 * <p>
 * This {@link BackPressureHandler} is designed to be used in combination with another {@link BackPressureHandler} like
 * the {@link ConcurrencyLimiterBlockingBackPressureHandler} that will handle the maximum concurrency level within the
 * application in a blocking way.
 *
 * @see PollingMessageSource
 */
public class ThroughputBackPressureHandler implements BatchAwareBackPressureHandler, IdentifiableContainerComponent {

	private static final Logger logger = LoggerFactory.getLogger(ThroughputBackPressureHandler.class);

	private final int batchSize;
	private final int maxConcurrentMessages;

	private final AtomicReference<CurrentThroughputMode> currentThroughputMode = new AtomicReference<>(
			CurrentThroughputMode.LOW);

	private final AtomicInteger inFlightRequests = new AtomicInteger(0);

	private final AtomicBoolean drained = new AtomicBoolean(false);

	private String id = getClass().getSimpleName();

	private ThroughputBackPressureHandler(Builder builder) {
		this.batchSize = builder.batchSize;
		this.maxConcurrentMessages = builder.maxConcurrentMessages;
		logger.debug("ThroughputBackPressureHandler created with batchSize {}", this.batchSize);
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
		if (drained.get()) {
			return 0;
		}
		int amountCappedAtBatchSize = Math.min(amount, this.batchSize);
		int permits;
		int inFlight = inFlightRequests.get();
		if (CurrentThroughputMode.LOW == this.currentThroughputMode.get()) {
			// In low-throughput mode, we only acquire one batch at a time,
			// so we need to limit the available permits to the batchSize - inFlight messages.
			permits = Math.max(0, Math.min(amountCappedAtBatchSize, this.batchSize - inFlight));
			logger.debug("[{}] Acquired {} permits (low-throughput mode), requested: {}, in flight: {}", this.id,
					permits, amount, inFlight);
		}
		else {
			// In high-throughput mode, we can acquire more permits than the batch size,
			// but we need to limit the available permits to the maxConcurrentMessages - inFlight messages.
			permits = Math.max(0, Math.min(amountCappedAtBatchSize, this.maxConcurrentMessages - inFlight));
			logger.debug("[{}] Acquired {} permits (high-throughput mode), requested: {}, in flight: {}", this.id,
					permits, amount, inFlight);
		}
		inFlightRequests.addAndGet(permits);
		return permits;
	}

	@Override
	public void release(int amount, ReleaseReason reason) {
		if (drained.get()) {
			return;
		}
		logger.debug("[{}] Releasing {} permits ({})", this.id, amount, reason);
		inFlightRequests.addAndGet(-amount);
		switch (reason) {
		case NONE_FETCHED -> updateThroughputMode(CurrentThroughputMode.HIGH, CurrentThroughputMode.LOW);
		case PARTIAL_FETCH -> updateThroughputMode(CurrentThroughputMode.LOW, CurrentThroughputMode.HIGH);
		case LIMITED, PROCESSED -> {
			// No need to switch throughput mode
		}
		}
	}

	private void updateThroughputMode(CurrentThroughputMode currentTarget, CurrentThroughputMode newTarget) {
		if (this.currentThroughputMode.compareAndSet(currentTarget, newTarget)) {
			logger.debug("[{}] throughput mode updated to {}", this.id, newTarget);
		}
	}

	@Override
	public boolean drain(Duration timeout) {
		logger.debug("[{}] Draining", this.id);
		drained.set(true);
		return true;
	}

	private enum CurrentThroughputMode {

		HIGH,

		LOW;

	}

	public static class Builder {

		private int batchSize;
		private int maxConcurrentMessages;

		public Builder batchSize(int batchSize) {
			this.batchSize = batchSize;
			return this;
		}

		public Builder totalPermits(int maxConcurrentMessages) {
			this.maxConcurrentMessages = maxConcurrentMessages;
			return this;
		}

		public ThroughputBackPressureHandler build() {
			Assert.notNull(this.batchSize, "Missing batchSize configuration");
			Assert.isTrue(this.batchSize > 0, "batch size must be greater than 0");
			Assert.notNull(this.maxConcurrentMessages, "Missing maxConcurrentMessages configuration");
			Assert.notNull(this.maxConcurrentMessages >= this.batchSize,
					"maxConcurrentMessages must be greater than or equal to batchSize");
			return new ThroughputBackPressureHandler(this);
		}
	}
}
