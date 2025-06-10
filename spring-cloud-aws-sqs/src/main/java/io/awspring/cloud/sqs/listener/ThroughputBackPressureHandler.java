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
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Non-blocking {@link BackPressureHandler} implementation that uses a switch between high and low throughput modes.
 * <p>
 * <strong>Throughput modes</strong>
 * <ul>
 * <li>In low-throughput mode, a single batch can be requested at a time. The number of permits that will be * delivered
 * is the requested amount or 0 is a batch is already in-flight.</li>
 * <li>In high-throughput mode, multiple batches can be requested at a time. The number of permits that will be
 * delivered is the requested amount.</li>
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
public class ThroughputBackPressureHandler implements BackPressureHandler, IdentifiableContainerComponent {

	private static final Logger logger = LoggerFactory.getLogger(ThroughputBackPressureHandler.class);

	private final AtomicReference<CurrentThroughputMode> currentThroughputMode = new AtomicReference<>(
			CurrentThroughputMode.LOW);

	private final AtomicBoolean occupied = new AtomicBoolean(false);

	private final AtomicBoolean drained = new AtomicBoolean(false);

	private String id = getClass().getSimpleName();

	private ThroughputBackPressureHandler() {
		logger.debug("ThroughputBackPressureHandler created");
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
		if (drained.get()) {
			return 0;
		}
		CurrentThroughputMode throughputMode = this.currentThroughputMode.get();
		if (throughputMode == CurrentThroughputMode.LOW) {
			if (this.occupied.get()) {
				logger.debug("[{}] No permits acquired because a batch already being processed in low throughput mode",
						this.id);
				return 0;
			}
			this.occupied.set(true);
		}
		logger.debug("[{}] Acquired {} permits ({} mode)", this.id, amount, throughputMode);
		return amount;
	}

	@Override
	public void release(int amount, ReleaseReason reason) {
		if (drained.get()) {
			return;
		}
		logger.debug("[{}] Releasing {} permits ({})", this.id, amount, reason);
		switch (reason) {
		case NONE_FETCHED -> {
			this.occupied.compareAndSet(true, false);
			updateThroughputMode(CurrentThroughputMode.HIGH, CurrentThroughputMode.LOW);
		}
		case PARTIAL_FETCH -> {
			this.occupied.compareAndSet(true, false);
			updateThroughputMode(CurrentThroughputMode.LOW, CurrentThroughputMode.HIGH);
		}
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

		public ThroughputBackPressureHandler build() {
			return new ThroughputBackPressureHandler();
		}
	}
}
