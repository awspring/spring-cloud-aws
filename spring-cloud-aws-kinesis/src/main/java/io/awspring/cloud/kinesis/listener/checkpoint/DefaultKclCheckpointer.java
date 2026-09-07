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
package io.awspring.cloud.kinesis.listener.checkpoint;

import io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders;
import java.time.Duration;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.backoff.ExponentialBackOff;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.exceptions.ThrottlingException;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
public class DefaultKclCheckpointer implements KclCheckpointer {

	static final int DEFAULT_MAX_THROTTLING_RETRIES = 3;

	static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(200);

	private final RecordProcessorCheckpointer delegate;

	private final RetryTemplate retryTemplate;

	public DefaultKclCheckpointer(RecordProcessorCheckpointer delegate) {
		this(delegate, DEFAULT_MAX_THROTTLING_RETRIES, DEFAULT_INITIAL_BACKOFF);
	}

	DefaultKclCheckpointer(RecordProcessorCheckpointer delegate, int maxThrottlingRetries, Duration initialBackoff) {
		Assert.notNull(delegate, "delegate must not be null");
		Assert.isTrue(maxThrottlingRetries >= 0, "maxThrottlingRetries must not be negative");
		Assert.notNull(initialBackoff, "initialBackoff must not be null");
		this.delegate = delegate;
		this.retryTemplate = new RetryTemplate(throttlingRetryPolicy(maxThrottlingRetries, initialBackoff));
	}

	private static RetryPolicy throttlingRetryPolicy(int maxThrottlingRetries, Duration initialBackoff) {
		ExponentialBackOff backOff = new ExponentialBackOff();
		backOff.setInitialInterval(initialBackoff.toMillis());
		backOff.setMultiplier(2.0);
		backOff.setMaxAttempts(maxThrottlingRetries);
		return RetryPolicy.builder().predicate(ThrottlingException.class::isInstance).backOff(backOff).build();
	}

	@Override
	public void checkpoint() {
		execute(this.delegate::checkpoint, "latest");
	}

	@Override
	public void checkpoint(Message<?> message) {
		Assert.notNull(message, "message must not be null");
		String sequenceNumber = message.getHeaders().get(KinesisMessageHeaders.SEQUENCE_NUMBER, String.class);
		Assert.notNull(sequenceNumber, "message is missing the " + KinesisMessageHeaders.SEQUENCE_NUMBER + " header");
		Long subSequenceNumber = message.getHeaders().get(KinesisMessageHeaders.SUBSEQUENCE_NUMBER, Long.class);
		execute(() -> {
			if (subSequenceNumber != null && subSequenceNumber > 0) {
				this.delegate.checkpoint(sequenceNumber, subSequenceNumber);
			}
			else {
				this.delegate.checkpoint(sequenceNumber);
			}
		}, sequenceNumber);
	}

	private void execute(CheckpointOperation operation, String position) {
		try {
			this.retryTemplate.execute(() -> {
				operation.run();
				return null;
			});
		}
		catch (RetryException ex) {
			throw translate(ex.getCause(), position);
		}
	}

	private RuntimeException translate(Throwable cause, String position) {
		if (cause instanceof ShutdownException) {
			return new LeaseLostCheckpointException(
					"Cannot checkpoint at position " + position + ", the lease is no longer held", cause);
		}
		if (cause instanceof InvalidStateException) {
			return new CheckpointException(
					"Checkpoint at position " + position
							+ " failed, the lease table is in an invalid state; verify the DynamoDB metadata tables",
					cause);
		}
		if (cause instanceof ThrottlingException) {
			return new CheckpointException(
					"Checkpoint at position " + position + " throttled and retries were exhausted", cause);
		}
		return new CheckpointException("Failed to checkpoint at position " + position, cause);
	}

	@FunctionalInterface
	private interface CheckpointOperation {

		void run() throws Exception;

	}

}
