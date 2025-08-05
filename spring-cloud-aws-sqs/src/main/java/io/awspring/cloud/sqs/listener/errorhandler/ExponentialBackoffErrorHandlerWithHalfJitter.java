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
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An implementation of an Exponential Backoff with half-jitter error handler for asynchronous message processing.
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
public class ExponentialBackoffErrorHandlerWithHalfJitter<T> implements AsyncErrorHandler<T> {
	private static final Logger logger = LoggerFactory.getLogger(ExponentialBackoffErrorHandlerWithHalfJitter.class);

	private final int initialVisibilityTimeoutSeconds;
	private final double multiplier;
	private final int maxVisibilityTimeoutSeconds;
	private final Supplier<Random> randomSupplier;

	private ExponentialBackoffErrorHandlerWithHalfJitter(int initialVisibilityTimeoutSeconds, double multiplier,
			int maxVisibilityTimeoutSeconds, Supplier<Random> randomSupplier) {
		this.initialVisibilityTimeoutSeconds = initialVisibilityTimeoutSeconds;
		this.multiplier = multiplier;
		this.maxVisibilityTimeoutSeconds = maxVisibilityTimeoutSeconds;
		this.randomSupplier = randomSupplier;
	}

	@Override
	public CompletableFuture<Void> handle(Message<T> message, Throwable t) {
		return applyExponentialBackoffVisibilityTimeoutWithHalfJitter(message)
				.thenCompose(theVoid -> CompletableFuture.failedFuture(t));
	}

	@Override
	public CompletableFuture<Void> handle(Collection<Message<T>> messages, Throwable t) {
		return applyExponentialBackoffVisibilityTimeoutWithHalfJitter(messages)
				.thenCompose(theVoid -> CompletableFuture.failedFuture(t));
	}

	private CompletableFuture<Void> applyExponentialBackoffVisibilityTimeoutWithHalfJitter(
			Collection<Message<T>> messages) {
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

	private CompletableFuture<Void> applyExponentialBackoffVisibilityTimeoutWithHalfJitter(Message<T> message) {
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
		int timeout = ErrorHandlerVisibilityHelper.calculateVisibilityTimeoutExponentially(receiveMessageCount,
				initialVisibilityTimeoutSeconds, multiplier, maxVisibilityTimeoutSeconds);
		int half = timeout / 2;
		int jitter = randomSupplier.get().nextInt(0, half + 1);
		return half + jitter;
	}

	public static <T> ExponentialBackoffErrorHandlerWithHalfJitter.Builder<T> builder() {
		return new ExponentialBackoffErrorHandlerWithHalfJitter.Builder<>();
	}

	public static class Builder<T> {

		private int initialVisibilityTimeoutSeconds = BackoffVisibilityConstants.DEFAULT_INITIAL_VISIBILITY_TIMEOUT_SECONDS;
		private double multiplier = BackoffVisibilityConstants.DEFAULT_MULTIPLIER;
		private int maxVisibilityTimeoutSeconds = BackoffVisibilityConstants.DEFAULT_MAX_VISIBILITY_TIMEOUT_SECONDS;
		private Supplier<Random> randomSupplier = ThreadLocalRandom::current;

		public ExponentialBackoffErrorHandlerWithHalfJitter.Builder<T> randomSupplier(Supplier<Random> randomSupplier) {
			Assert.notNull(randomSupplier, "randomSupplier cannot be null");
			this.randomSupplier = randomSupplier;
			return this;
		}

		public ExponentialBackoffErrorHandlerWithHalfJitter.Builder<T> initialVisibilityTimeoutSeconds(
				int initialVisibilityTimeoutSeconds) {
			ErrorHandlerVisibilityHelper.checkVisibilityTimeout(initialVisibilityTimeoutSeconds);
			this.initialVisibilityTimeoutSeconds = initialVisibilityTimeoutSeconds;
			return this;
		}

		public ExponentialBackoffErrorHandlerWithHalfJitter.Builder<T> multiplier(double multiplier) {
			Assert.isTrue(multiplier >= 1,
					() -> "Invalid multiplier '" + multiplier + "'. Should be greater than " + "or equal to 1.");
			this.multiplier = multiplier;
			return this;
		}

		public ExponentialBackoffErrorHandlerWithHalfJitter.Builder<T> maxVisibilityTimeoutSeconds(
				int maxVisibilityTimeoutSeconds) {
			ErrorHandlerVisibilityHelper.checkVisibilityTimeout(maxVisibilityTimeoutSeconds);
			this.maxVisibilityTimeoutSeconds = maxVisibilityTimeoutSeconds;
			return this;
		}

		public ExponentialBackoffErrorHandlerWithHalfJitter<T> build() {
			Assert.isTrue(initialVisibilityTimeoutSeconds <= maxVisibilityTimeoutSeconds,
					"Initial visibility timeout must not exceed max visibility timeout");
			return new ExponentialBackoffErrorHandlerWithHalfJitter<>(initialVisibilityTimeoutSeconds, multiplier,
					maxVisibilityTimeoutSeconds, randomSupplier);
		}
	}
}
