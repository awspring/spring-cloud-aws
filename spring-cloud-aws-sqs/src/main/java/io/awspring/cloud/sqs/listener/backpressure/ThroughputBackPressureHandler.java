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

import io.awspring.cloud.sqs.listener.IdentifiableContainerComponent;
import io.awspring.cloud.sqs.listener.source.PollingMessageSource;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * A non-blocking {@link BackPressureHandler} that dynamically switches between high- and low-throughput modes to
 * optimize polling behavior based on recent message availability.
 *
 * <p>
 * <strong>Throughput modes</strong>
 * <ul>
 * <li><strong>Low-throughput mode</strong>: Only a single batch may be requested at a time. If a batch is already
 * in-flight, zero permits are granted.</li>
 * <li><strong>High-throughput mode</strong>: Multiple batches may be requested concurrently. All requested permits are
 * granted.</li>
 * </ul>
 *
 * <p>
 * <strong>Throughput mode switching</strong>: The handler starts in low-throughput mode. It switches to high-throughput
 * mode if messages are returned from a poll, and reverts to low-throughput mode if no messages are returned.
 *
 * <p>
 * Typically used in conjunction with a concurrency-limiting {@link BackPressureHandler} such as
 * {@link ConcurrencyLimiterBlockingBackPressureHandler}.
 *
 * <p>
 * This handler builds on the original <a href=
 * "https://github.com/awspring/spring-cloud-aws/blob/v3.4.0/spring-cloud-aws-sqs/src/main/java/io/awspring/cloud/sqs/listener/SemaphoreBackPressureHandler.java">
 * SemaphoreBackPressureHandler</a>, separating specific responsibilities into a more modular form and enabling
 * composition with other handlers as part of an extensible backpressure strategy.
 *
 * @see PollingMessageSource
 * @see CompositeBackPressureHandler
 * @see BackPressureHandlerFactories
 *
 * @author Loïc Rouchon
 * @author Tomaz Fernandes
 *
 * @since 4.0.0
 */
public class ThroughputBackPressureHandler implements BackPressureHandler, IdentifiableContainerComponent {

	private static final Logger logger = LoggerFactory.getLogger(ThroughputBackPressureHandler.class);

	private final AtomicReference<CurrentThroughputMode> currentThroughputMode = new AtomicReference<>(
			CurrentThroughputMode.LOW);

	private final AtomicBoolean occupied = new AtomicBoolean(false);

	private final AtomicBoolean drained = new AtomicBoolean(false);

	private final int batchSize;

	private String id = getClass().getSimpleName();

	private ThroughputBackPressureHandler(Builder builder) {
		this.batchSize = builder.batchSize;
		logger.debug("ThroughputBackPressureHandler created with batch size: {}", this.batchSize);
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
		return Math.min(amount, this.batchSize);
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

		private int batchSize;

		public Builder batchSize(int batchSize) {
			this.batchSize = batchSize;
			return this;
		}

		public ThroughputBackPressureHandler build() {
			Assert.isTrue(this.batchSize > 0, "The batch size must be greater than 0");
			return new ThroughputBackPressureHandler(this);
		}
	}
}
