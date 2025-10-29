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
package io.awspring.cloud.sqs.listener.backpressure;

import io.awspring.cloud.sqs.listener.IdentifiableContainerComponent;
import io.awspring.cloud.sqs.listener.source.PollingMessageSource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Composite {@link BackPressureHandler} implementation that delegates the back-pressure handling to a list of
 * {@link BackPressureHandler}s.
 * <p>
 * This class is used to combine multiple back-pressure handlers into a single one. It allows for more complex
 * back-pressure handling strategies by combining different implementations.
 * <p>
 * The order in which the back-pressure handlers are registered in the {@link CompositeBackPressureHandler} is important
 * as it will affect the blocking and limiting behaviour of the back-pressure handling.
 * <p>
 * When {@link #request(int amount)} is called, the first back-pressure handler in the list is called with
 * {@code amount} as the requested amount of permits. The returned amount of permits (which is less than or equal to the
 * initial amount) is then passed to the next back-pressure handler in the list. This process of reducing the amount to
 * request for the next handlers in the chain is called "limiting". This process continues until all back-pressure
 * handlers have been called or {@literal 0} permits has been returned.
 * <p>
 * Once the final amount of available permits have been computed, unused acquired permits on back-pressure handlers (due
 * to later limiting happening in the chain) are released.
 * <p>
 * If no permits were obtained, the {@link #request(int)} method will wait up to {@code noPermitsReturnedWaitTimeout}
 * for a release of permits before returning.
 *
 * <p>
 * This handler builds on the original <a href=
 * "https://github.com/awspring/spring-cloud-aws/blob/v3.4.0/spring-cloud-aws-sqs/src/main/java/io/awspring/cloud/sqs/listener/SemaphoreBackPressureHandler.java">
 * SemaphoreBackPressureHandler</a>, separating specific responsibilities into a more modular form and enabling
 * composition with other handlers as part of an extensible backpressure strategy.
 *
 * @author Loïc Rouchon
 * @author Tomaz Fernandes
 *
 * @see PollingMessageSource
 * @see BackPressureHandlerFactories
 *
 * @since 4.0.0
 */
public class CompositeBackPressureHandler implements BatchAwareBackPressureHandler, IdentifiableContainerComponent {

	private static final Logger logger = LoggerFactory.getLogger(CompositeBackPressureHandler.class);

	private String id;

	private final int batchSize;

	private final Duration noPermitsReturnedWaitTimeout;

	private final List<BackPressureHandler> backPressureHandlers;

	private final ReentrantLock noPermitsReturnedWaitLock = new ReentrantLock();

	private final Condition permitsReleasedCondition = noPermitsReturnedWaitLock.newCondition();

	private CompositeBackPressureHandler(Builder builder) {
		this.batchSize = builder.batchSize;
		this.noPermitsReturnedWaitTimeout = builder.noPermitsReturnedWaitTimeout;
		this.backPressureHandlers = List.copyOf(builder.backPressureHandlers);

	}

	@Override
	public void setId(String id) {
		this.id = id;
		backPressureHandlers.stream().filter(IdentifiableContainerComponent.class::isInstance)
				.map(IdentifiableContainerComponent.class::cast)
				.forEach(bph -> bph.setId(bph.getClass().getSimpleName() + "-" + id));
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
		logger.debug("[{}] Requesting {} permits", this.id, amount);
		int obtained = amount;
		int[] obtainedPerBph = new int[backPressureHandlers.size()];
		for (int i = 0; i < backPressureHandlers.size() && obtained > 0; i++) {
			obtainedPerBph[i] = backPressureHandlers.get(i).request(obtained);
			obtained = Math.min(obtained, obtainedPerBph[i]);
		}
		for (int i = 0; i < backPressureHandlers.size(); i++) {
			int obtainedForBph = obtainedPerBph[i];
			if (obtainedForBph > obtained) {
				backPressureHandlers.get(i).release(obtainedForBph - obtained, ReleaseReason.LIMITED);
			}
		}
		if (obtained == 0) {
			waitForPermitsToBeReleased();
		}
		logger.debug("[{}] Obtained {} permits ({} requested)", this.id, obtained, amount);
		return obtained;
	}

	@Override
	public void release(int amount, ReleaseReason reason) {
		logger.debug("[{}] Releasing {} permits ({})", this.id, amount, reason);
		for (BackPressureHandler handler : backPressureHandlers) {
			handler.release(amount, reason);
		}
		if (amount > 0) {
			signalPermitsWereReleased();
		}
	}

	/**
	 * Waits for permits to be released up to {@link #noPermitsReturnedWaitTimeout}. If no permits were released within
	 * the configured {@link #noPermitsReturnedWaitTimeout}, returns immediately. This allows {@link #request(int)} to
	 * return {@code 0} permits and will trigger another round of back-pressure handling.
	 *
	 * @throws InterruptedException if the Thread is interrupted while waiting for permits.
	 */
	@SuppressWarnings({ "java:S899" // we are not interested in the await return value here
	})
	private void waitForPermitsToBeReleased() throws InterruptedException {
		noPermitsReturnedWaitLock.lock();
		try {
			logger.trace("[{}] No permits were obtained, waiting for a release up to {}", this.id,
					noPermitsReturnedWaitTimeout);
			permitsReleasedCondition.await(noPermitsReturnedWaitTimeout.toMillis(), TimeUnit.MILLISECONDS);
		}
		finally {
			noPermitsReturnedWaitLock.unlock();
		}
	}

	private void signalPermitsWereReleased() {
		noPermitsReturnedWaitLock.lock();
		try {
			permitsReleasedCondition.signal();
		}
		finally {
			noPermitsReturnedWaitLock.unlock();
		}
	}

	@Override
	public boolean drain(Duration timeout) {
		logger.debug("[{}] Draining back-pressure handlers initiated", this.id);
		boolean result = true;
		Instant start = Instant.now();
		for (BackPressureHandler handler : backPressureHandlers) {
			Duration remainingTimeout = maxDuration(timeout.minus(Duration.between(start, Instant.now())),
					Duration.ZERO);
			result &= handler.drain(remainingTimeout);
		}
		logger.debug("[{}] Draining back-pressure handlers completed", this.id);
		return result;
	}

	private static Duration maxDuration(Duration first, Duration second) {
		return first.compareTo(second) > 0 ? first : second;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private int batchSize;
		private Duration noPermitsReturnedWaitTimeout;
		private List<BackPressureHandler> backPressureHandlers;

		public Builder backPressureHandlers(List<BackPressureHandler> backPressureHandlers) {
			this.backPressureHandlers = backPressureHandlers;
			return this;
		}

		public Builder batchSize(int batchSize) {
			this.batchSize = batchSize;
			return this;
		}

		public Builder noPermitsReturnedWaitTimeout(Duration noPermitsReturnedWaitTimeout) {
			this.noPermitsReturnedWaitTimeout = noPermitsReturnedWaitTimeout;
			return this;
		}

		public CompositeBackPressureHandler build() {
			Assert.notNull(this.batchSize, "Missing configuration for batch size");
			Assert.notNull(this.noPermitsReturnedWaitTimeout, "Missing configuration for noPermitsReturnedWaitTimeout");
			Assert.noNullElements(this.backPressureHandlers, "backPressureHandlers must not be null");
			return new CompositeBackPressureHandler(this);
		}
	}
}
