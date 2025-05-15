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
package io.awspring.cloud.sqs.listener.errorhandler;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.BatchVisibility;
import io.awspring.cloud.sqs.listener.Visibility;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An implementation of an Exponential Backoff error handler for asynchronous message processing.
 *
 * <p>
 * This error handler sets the SQS message visibility timeout exponentially based on the number of received attempts
 * whenever an exception occurs.
 *
 * <p>
 * When AcknowledgementMode is set to ON_SUCCESS (the default), returning a failed future prevents the message from
 * being acknowledged.
 *
 * @author Bruno Garcia
 * @author Rafael Pavarini
 */

public class ExponentialBackoffErrorHandler<T> implements AsyncErrorHandler<T> {
	private static final Logger logger = LoggerFactory.getLogger(ExponentialBackoffErrorHandler.class);

	private final int initialVisibilityTimeoutSeconds;
	private final double multiplier;
	private final int maxVisibilityTimeoutSeconds;

	private ExponentialBackoffErrorHandler(int initialVisibilityTimeoutSeconds, double multiplier,
			int maxVisibilityTimeoutSeconds) {
		this.initialVisibilityTimeoutSeconds = initialVisibilityTimeoutSeconds;
		this.multiplier = multiplier;
		this.maxVisibilityTimeoutSeconds = maxVisibilityTimeoutSeconds;
	}

	@Override
	public CompletableFuture<Void> handle(Message<T> message, Throwable t) {
		return applyExponentialBackoffVisibilityTimeout(message)
				.thenCompose(theVoid -> CompletableFuture.failedFuture(t));
	}

	@Override
	public CompletableFuture<Void> handle(Collection<Message<T>> messages, Throwable t) {
		return applyExponentialBackoffVisibilityTimeout(messages)
				.thenCompose(theVoid -> CompletableFuture.failedFuture(t));
	}

	private CompletableFuture<Void> applyExponentialBackoffVisibilityTimeout(Collection<Message<T>> messages) {
		CompletableFuture<?>[] futures = ErrorHandlerVisibilityHelper.groupMessagesByReceiveMessageCount(messages)
				.entrySet().stream().map(entry -> {
					int timeout = calculateTimeout(entry.getKey());
					return applyBatchVisibilityChange(entry.getValue(), timeout);
				}).toArray(CompletableFuture[]::new);

		return CompletableFuture.allOf(futures);
	}

	private CompletableFuture<Void> applyBatchVisibilityChange(Collection<Message<T>> messages, int timeout) {
		logger.debug("Changing batch visibility timeout to {} - Messages Id {}", timeout,
				MessageHeaderUtils.getId(messages));
		BatchVisibility visibility = ErrorHandlerVisibilityHelper.getVisibility(messages);
		return visibility.changeToAsync(timeout).exceptionallyCompose(throwable -> {
			logger.warn("Failed to change batch visibility timeout to {} - Messages Id {}", timeout,
					MessageHeaderUtils.getId(messages), throwable);
			return CompletableFuture.failedFuture(throwable);
		});
	}

	private CompletableFuture<Void> applyExponentialBackoffVisibilityTimeout(Message<T> message) {
		int timeout = calculateTimeout(message);
		Visibility visibility = ErrorHandlerVisibilityHelper.getVisibility(message);
		logger.debug("Changing visibility timeout to {} - Message Id {}", timeout, message.getHeaders().getId());
		return visibility.changeToAsync(timeout).exceptionallyCompose(throwable -> {
			logger.warn("Failed to change visibility timeout to {} - Message Id {}", timeout,
					message.getHeaders().getId(), throwable);
			return CompletableFuture.failedFuture(throwable);
		});
	}

	private int calculateTimeout(Message<T> message) {
		long receiveMessageCount = ErrorHandlerVisibilityHelper.getReceiveMessageCount(message);
		return calculateTimeout(receiveMessageCount);
	}

	private int calculateTimeout(long receiveMessageCount) {
		double exponential = initialVisibilityTimeoutSeconds * Math.pow(multiplier, receiveMessageCount - 1);
		int seconds = (int) Math.min(exponential, (long) Integer.MAX_VALUE);
		return Math.min(seconds, maxVisibilityTimeoutSeconds);
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	public static class Builder<T> {

		/**
		 * The default initial visibility timeout.
		 */
		private static final int DEFAULT_INITIAL_VISIBILITY_TIMEOUT_SECONDS = 100;

		/**
		 * The default multiplier, which doubles the visibility timeout.
		 */
		private static final double DEFAULT_MULTIPLIER = 2.0;

		private int initialVisibilityTimeoutSeconds = DEFAULT_INITIAL_VISIBILITY_TIMEOUT_SECONDS;
		private double multiplier = DEFAULT_MULTIPLIER;
		private int maxVisibilityTimeoutSeconds = Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS;

		public Builder<T> initialVisibilityTimeoutSeconds(int initialVisibilityTimeoutSeconds) {
			checkVisibilityTimeout(initialVisibilityTimeoutSeconds);
			this.initialVisibilityTimeoutSeconds = initialVisibilityTimeoutSeconds;
			return this;
		}

		public Builder<T> multiplier(double multiplier) {
			Assert.isTrue(multiplier >= 1,
					() -> "Invalid multiplier '" + multiplier + "'. Should be greater than " + "or equal to 1.");
			this.multiplier = multiplier;
			return this;
		}

		public Builder<T> maxVisibilityTimeoutSeconds(int maxVisibilityTimeoutSeconds) {
			checkVisibilityTimeout(maxVisibilityTimeoutSeconds);
			this.maxVisibilityTimeoutSeconds = maxVisibilityTimeoutSeconds;
			return this;
		}

		public ExponentialBackoffErrorHandler<T> build() {
			Assert.isTrue(initialVisibilityTimeoutSeconds <= maxVisibilityTimeoutSeconds,
					"Initial visibility timeout must not exceed max visibility timeout");
			return new ExponentialBackoffErrorHandler<T>(initialVisibilityTimeoutSeconds, multiplier,
					maxVisibilityTimeoutSeconds);
		}

		private void checkVisibilityTimeout(long visibilityTimeout) {
			Assert.isTrue(visibilityTimeout > 0,
					() -> "Invalid visibility timeout '" + visibilityTimeout + "'. Should be greater than 0 ");
			Assert.isTrue(visibilityTimeout <= Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS,
					() -> "Invalid visibility timeout '" + visibilityTimeout + "'. Should be less than or equal to "
							+ Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS);
		}
	}
}
