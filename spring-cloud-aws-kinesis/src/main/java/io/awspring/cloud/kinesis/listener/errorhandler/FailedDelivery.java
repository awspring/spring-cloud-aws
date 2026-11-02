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

import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A failed delivery handed to an {@link ErrorHandler}: the message that could not be processed, the failure, and a
 * {@link #retry()} hook that re-invokes the listener with the same message.
 * <p>
 * For a single-record listener the message is the failed record. For a batch listener it is a message whose payload is
 * the whole {@code Collection<Message<?>>} batch. When the record could not be converted into a message at all, the
 * payload is the raw {@link software.amazon.kinesis.retrieval.KinesisClientRecord} and the delivery is not
 * {@link #isRetryable() retryable}, since there is no message to hand to the listener.
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
	private final ListenerInvocation retryInvocation;

	private int attempts = 1;

	/**
	 * Create a failed delivery.
	 * @param message the message that failed.
	 * @param cause the failure.
	 * @param streamName the stream the record was read from.
	 * @param shardId the shard the record was read from.
	 * @param retryInvocation re-invokes the listener with the same message, or {@code null} when the delivery cannot be
	 *     retried.
	 */
	public FailedDelivery(Message<?> message, Throwable cause, String streamName, @Nullable String shardId,
			@Nullable ListenerInvocation retryInvocation) {
		Assert.notNull(message, "message must not be null");
		Assert.notNull(cause, "cause must not be null");
		Assert.notNull(streamName, "streamName must not be null");
		this.message = message;
		this.cause = cause;
		this.streamName = streamName;
		this.shardId = shardId;
		this.retryInvocation = retryInvocation;
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
	 * @return whether the listener can be re-invoked.
	 */
	public boolean isRetryable() {
		return this.retryInvocation != null;
	}

	/**
	 * The number of listener invocations made for this delivery so far, starting at {@code 1} for the original delivery
	 * and incremented by every {@link #retry()} call.
	 * @return the number of attempts.
	 */
	public int getAttempts() {
		return this.attempts;
	}

	/**
	 * Re-invoke the listener with the same message, on the calling (shard) thread. Returns normally if the listener
	 * succeeded and throws the listener's exception otherwise.
	 * @throws Exception the listener failure.
	 * @throws IllegalStateException if this delivery is not {@link #isRetryable() retryable}.
	 */
	public void retry() throws Exception {
		Assert.state(this.retryInvocation != null, "This delivery cannot be retried, the record failed conversion");
		this.attempts++;
		this.retryInvocation.invoke();
	}

	/**
	 * Re-invokes the listener for a failed delivery.
	 */
	@FunctionalInterface
	public interface ListenerInvocation {

		void invoke() throws Exception;

	}

}
