/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.kinesis.listener.errorhandler;

import io.awspring.cloud.kinesis.listener.ListenerExecutionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

/**
 * {@link ErrorHandler} retrying a failed delivery according to a {@link BackOff}, then handing it to a
 * {@link FailedDeliveryRecoverer} once the back-off stops.
 * <p>
 * Every retry re-invokes the listener through {@link FailedDelivery#retry()} after sleeping for the interval returned
 * by the back-off, on the shard thread. Only the failed shard waits, and its records stay in order. Deliveries that are
 * not {@link FailedDelivery#isRetryable() retryable} (records that failed conversion) go to the recoverer straight
 * away.
 * <p>
 * The default recoverer logs the failure and rethrows it, which halts the shard so that no record is skipped. Provide a
 * recoverer to dead-letter the record and continue instead.
 * <p>
 * Use a bounded back-off, such as a {@link FixedBackOff} with a maximum number of attempts or an
 * {@link ExponentialBackOff} with {@code maxAttempts} or {@code maxElapsedTime}: an unbounded back-off retries forever
 * and the shard makes no progress. If the shard thread is interrupted while waiting, for example during shutdown, the
 * failure is left unresolved.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class BackOffErrorHandler implements ErrorHandler {

	private static final Logger logger = LoggerFactory.getLogger(BackOffErrorHandler.class);

	private final BackOff backOff;

	private final FailedDeliveryRecoverer recoverer;

	/**
	 * Create a handler retrying with the given back-off, halting the shard once the retries are exhausted.
	 * @param backOff the back-off between retries.
	 */
	public BackOffErrorHandler(BackOff backOff) {
		this(backOff, BackOffErrorHandler::logAndRethrow);
	}

	/**
	 * Create a handler retrying with the given back-off, then handing the delivery to the given recoverer.
	 * @param backOff the back-off between retries.
	 * @param recoverer invoked once the retries are exhausted or when the delivery cannot be retried.
	 */
	public BackOffErrorHandler(BackOff backOff, FailedDeliveryRecoverer recoverer) {
		Assert.notNull(backOff, "backOff must not be null");
		Assert.notNull(recoverer, "recoverer must not be null");
		this.backOff = backOff;
		this.recoverer = recoverer;
	}

	@Override
	public void handle(FailedDelivery failedDelivery) throws Exception {
		Throwable lastFailure = failedDelivery.getCause();
		if (failedDelivery.isRetryable()) {
			BackOffExecution execution = this.backOff.start();
			long interval;
			while ((interval = execution.nextBackOff()) != BackOffExecution.STOP) {
				try {
					sleep(interval);
				}
				catch (InterruptedException ex) {
					// Shutting down: leave the failure unresolved so that nothing is skipped.
					Thread.currentThread().interrupt();
					throw ex;
				}
				try {
					failedDelivery.retry();
					logger.debug("Delivery for shard {} of stream {} succeeded on attempt {}",
							failedDelivery.getShardId(), failedDelivery.getStreamName(), failedDelivery.getAttempts());
					return;
				}
				catch (Exception ex) {
					lastFailure = ex;
					logger.debug("Attempt {} for shard {} of stream {} failed", failedDelivery.getAttempts(),
							failedDelivery.getShardId(), failedDelivery.getStreamName(), ex);
				}
			}
		}
		this.recoverer.recover(failedDelivery, lastFailure);
	}

	/**
	 * Wait between two attempts. Override to change how the wait happens, for example in tests.
	 * @param intervalMillis the interval in milliseconds.
	 * @throws InterruptedException if the shard thread is interrupted while waiting.
	 */
	protected void sleep(long intervalMillis) throws InterruptedException {
		if (intervalMillis > 0) {
			Thread.sleep(intervalMillis);
		}
	}

	private static void logAndRethrow(FailedDelivery failedDelivery, Throwable lastFailure) throws Exception {
		logger.error(
				"Delivery for shard {} of stream {} failed after {} attempts; halting the shard until its lease "
						+ "is reacquired",
				failedDelivery.getShardId(), failedDelivery.getStreamName(), failedDelivery.getAttempts(), lastFailure);
		if (lastFailure instanceof Exception exception) {
			throw exception;
		}
		if (lastFailure instanceof Error error) {
			throw error;
		}
		throw new ListenerExecutionFailedException("Kinesis listener failed", lastFailure);
	}

}
