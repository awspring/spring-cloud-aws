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
package io.awspring.cloud.sqs;

/**
 * Exception thrown when a {@link QueueAttributesResolver} fails.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see io.awspring.cloud.sqs.listener.QueueNotFoundStrategy
 */
public class QueueAttributesResolvingException extends RuntimeException {

	private final boolean queueIgnored;

	/**
	 * Create an instance with the message and throwable cause.
	 * @param message the error message.
	 * @param cause the cause.
	 */
	public QueueAttributesResolvingException(String message, Throwable cause) {
		this(message, cause, false);
	}

	/**
	 * Create an instance with the message, cause, and a flag indicating that the resolver treated the missing queue as
	 * ignored per {@link io.awspring.cloud.sqs.listener.QueueNotFoundStrategy#IGNORE}.
	 * @param message the error message.
	 * @param cause the cause.
	 * @param queueIgnored whether the resolver signalled that the queue should be ignored.
	 */
	public QueueAttributesResolvingException(String message, Throwable cause, boolean queueIgnored) {
		super(message, cause);
		this.queueIgnored = queueIgnored;
	}

	/**
	 * Whether the resolver signalled that the missing queue should be ignored under
	 * {@link io.awspring.cloud.sqs.listener.QueueNotFoundStrategy#IGNORE}, so the listener can skip startup rather than
	 * fail the application context.
	 * @return {@code true} if the queue should be ignored.
	 */
	public boolean isQueueIgnored() {
		return this.queueIgnored;
	}
}
