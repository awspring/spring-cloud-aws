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
 * An implementation of a Linear Backoff error handler for asynchronous message processing.
 *
 * <p>
 * This error handler sets the SQS message visibility timeout linearly based on the number of received attempts whenever
 * an exception occurs.
 *
 * <p>
 * When AcknowledgementMode is set to ON_SUCCESS (the default), returning a failed future prevents the message from
 * being acknowledged.
 *
 * @author Bruno Garcia
 * @author Rafael Pavarini
 */

public class LinearBackoffErrorHandler<T> implements AsyncErrorHandler<T> {
	private static final Logger logger = LoggerFactory.getLogger(LinearBackoffErrorHandler.class);

	private final int initialVisibilityTimeoutSeconds;
	private final int increment;
	private final int maxVisibilityTimeoutSeconds;

	private LinearBackoffErrorHandler(int initialVisibilityTimeoutSeconds, int increment,
			int maxVisibilityTimeoutSeconds) {
		this.initialVisibilityTimeoutSeconds = initialVisibilityTimeoutSeconds;
		this.increment = increment;
		this.maxVisibilityTimeoutSeconds = maxVisibilityTimeoutSeconds;
	}

	@Override
	public CompletableFuture<Void> handle(Message<T> message, Throwable t) {
		return applyLinearBackoffVisibilityTimeout(message).thenCompose(theVoid -> CompletableFuture.failedFuture(t));
	}

	@Override
	public CompletableFuture<Void> handle(Collection<Message<T>> messages, Throwable t) {
		return applyLinearBackoffVisibilityTimeout(messages).thenCompose(theVoid -> CompletableFuture.failedFuture(t));
	}

	private CompletableFuture<Void> applyLinearBackoffVisibilityTimeout(Collection<Message<T>> messages) {
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

	private CompletableFuture<Void> applyLinearBackoffVisibilityTimeout(Message<T> message) {
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
		return ErrorHandlerVisibilityHelper.calculateVisibilityTimeoutLinearly(receiveMessageCount,
				initialVisibilityTimeoutSeconds, increment, maxVisibilityTimeoutSeconds);
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	public static class Builder<T> {

		private int initialVisibilityTimeoutSeconds = BackoffVisibilityConstants.DEFAULT_INITIAL_VISIBILITY_TIMEOUT_SECONDS;
		private int increment = BackoffVisibilityConstants.DEFAULT_INCREMENT;
		private int maxVisibilityTimeoutSeconds = BackoffVisibilityConstants.DEFAULT_MAX_VISIBILITY_TIMEOUT_SECONDS;

		public Builder<T> initialVisibilityTimeoutSeconds(int initialVisibilityTimeoutSeconds) {
			ErrorHandlerVisibilityHelper.checkVisibilityTimeout(initialVisibilityTimeoutSeconds);
			this.initialVisibilityTimeoutSeconds = initialVisibilityTimeoutSeconds;
			return this;
		}

		public Builder<T> increment(int increment) {
			Assert.isTrue(increment >= 1,
					() -> "Invalid increment '" + increment + "'. Should be greater than " + "or equal to 1.");
			this.increment = increment;
			return this;
		}

		public Builder<T> maxVisibilityTimeoutSeconds(int maxVisibilityTimeoutSeconds) {
			ErrorHandlerVisibilityHelper.checkVisibilityTimeout(maxVisibilityTimeoutSeconds);
			this.maxVisibilityTimeoutSeconds = maxVisibilityTimeoutSeconds;
			return this;
		}

		public LinearBackoffErrorHandler<T> build() {
			Assert.isTrue(initialVisibilityTimeoutSeconds <= maxVisibilityTimeoutSeconds,
					"Initial visibility timeout must not exceed max visibility timeout");
			return new LinearBackoffErrorHandler<>(initialVisibilityTimeoutSeconds, increment,
					maxVisibilityTimeoutSeconds);
		}
	}
}
