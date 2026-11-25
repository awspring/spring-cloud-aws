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

import io.awspring.cloud.kinesis.listener.ReplyPublicationFailedException;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A failed delivery handed to an {@link ErrorHandler}: the message that could not be processed, the failure, and a
 * {@link #retry()} action for the stage that failed. A listener failure retries the listener with the same message; a
 * reply-publication failure republishes the cached listener result without invoking the listener again.
 * <p>
 * For a single-record listener the message is the failed record. For a batch listener it is a message whose payload is
 * the whole {@code Collection<Message<?>>} batch. When the record could not be converted into a message at all, the
 * payload is the raw {@link software.amazon.kinesis.retrieval.KinesisClientRecord} and the delivery is not
 * {@link #isRetryable() retryable}, since there is no converted delivery stage to retry.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public final class FailedDelivery {

	private final Message<?> message;

	private final Throwable cause;

	private final String streamName;

	@Nullable
	private final String shardId;

	@Nullable
	private RetryAction retryAction;

	private int attempts = 1;

	/**
	 * Create a failed delivery.
	 * @param message the message that failed.
	 * @param cause the failure.
	 * @param streamName the stream the record was read from.
	 * @param shardId the shard the record was read from.
	 * @param retryAction retries the failed delivery stage, or {@code null} when the delivery cannot be retried.
	 */
	public FailedDelivery(Message<?> message, Throwable cause, String streamName, @Nullable String shardId,
			@Nullable RetryAction retryAction) {
		Assert.notNull(message, "message must not be null");
		Assert.notNull(cause, "cause must not be null");
		Assert.notNull(streamName, "streamName must not be null");
		this.message = message;
		this.cause = cause;
		this.streamName = streamName;
		this.shardId = shardId;
		this.retryAction = retryAction;
	}

	public Message<?> getMessage() {
		return this.message;
	}

	/**
	 * The failure of the original delivery. Failures of {@link #retry()} calls are thrown from {@code retry()} itself
	 * and do not replace this value.
	 * @return the original failure.
	 */
	public Throwable getCause() {
		return this.cause;
	}

	public String getStreamName() {
		return this.streamName;
	}

	@Nullable
	public String getShardId() {
		return this.shardId;
	}

	/**
	 * Whether {@link #retry()} can be called. A record that failed conversion is not retryable.
	 * @return whether the failed delivery stage can be retried.
	 */
	public boolean isRetryable() {
		return this.retryAction != null;
	}

	/**
	 * The number of delivery attempts so far, starting at {@code 1} for the original attempt and incremented by every
	 * {@link #retry()} call.
	 * @return the number of attempts.
	 */
	public int getAttempts() {
		return this.attempts;
	}

	/**
	 * Retry the failed delivery stage on the calling shard thread. Returns normally if the retry succeeds and throws
	 * the listener or reply-publication exception otherwise.
	 * @throws Exception the retry failure.
	 * @throws IllegalStateException if this delivery is not {@link #isRetryable() retryable}.
	 */
	public void retry() throws Exception {
		RetryAction action = this.retryAction;
		Assert.state(action != null, "This delivery cannot be retried, the record failed conversion");
		this.attempts++;
		try {
			action.retry();
		}
		catch (ReplyPublicationFailedException ex) {
			// The listener has now succeeded. Further attempts must only republish its cached result.
			this.retryAction = ex::resend;
			throw ex;
		}
	}

	/**
	 * Retries the failed stage of a delivery.
	 */
	@FunctionalInterface
	public interface RetryAction {

		void retry() throws Exception;

	}

}
