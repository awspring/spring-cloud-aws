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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * {@link BackPressureHandler} implementation that uses a switches between high and low throughput modes.
 * <p>
 * The initial throughput mode is low, which means, only one batch at a time can be requested. If some messages are
 * fetched, then the throughput mode is switched to high, which means, the multiple batches can be requested (i.e. there
 * is no need to wait for the previous batch's processing to complete before requesting a new one). If no messages are
 * returned fetched by a poll, the throughput mode is switched back to low.
 * <p>
 * This {@link BackPressureHandler} is designed to be used in combination with another {@link BackPressureHandler} like
 * the {@link ConcurrencyLimiterBlockingBackPressureHandler} that will handle the maximum concurrency level within the
 * application.
 *
 * @author Tomaz Fernandes
 * @see PollingMessageSource
 * @since 3.0
 */
public class ThroughputBackPressureHandler implements BatchAwareBackPressureHandler, IdentifiableContainerComponent {

	private static final Logger logger = LoggerFactory.getLogger(ThroughputBackPressureHandler.class);

	private final int batchSize;

	private final AtomicReference<CurrentThroughputMode> currentThroughputMode = new AtomicReference<>(
			CurrentThroughputMode.LOW);

	private final AtomicInteger inFlightRequests = new AtomicInteger(0);

	private final AtomicBoolean drained = new AtomicBoolean(false);

	private String id = getClass().getSimpleName();

	private ThroughputBackPressureHandler(Builder builder) {
		this.batchSize = builder.batchSize;
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
		int permits;
		int inFlight = inFlightRequests.get();
		if (CurrentThroughputMode.LOW == this.currentThroughputMode.get()) {
			permits = Math.max(0, Math.min(amount, this.batchSize - inFlight));
			logger.debug("[{}] Acquired {} permits (low throughput mode), in flight: {}", this.id, amount, inFlight);
		}
		else {
			permits = amount;
			logger.debug("[{}] Acquired {} permits (high throughput mode), in flight: {}", this.id, amount, inFlight);
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

		public Builder batchSize(int batchSize) {
			this.batchSize = batchSize;
			return this;
		}

		public ThroughputBackPressureHandler build() {
			Assert.noNullElements(List.of(this.batchSize), "Missing configuration");
			Assert.isTrue(this.batchSize > 0, "batch size must be greater than 0");
			return new ThroughputBackPressureHandler(this);
		}
	}
}
