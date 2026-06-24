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

import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;

/**
 * Signals that a queue was not found and the configured {@link QueueNotFoundStrategy} is
 * {@link QueueNotFoundStrategy#IGNORE IGNORE}. The listener container catches this to skip starting the source while
 * allowing the application context to come up; other resolver failures continue to surface as
 * {@link QueueAttributesResolvingException}.
 *
 * @author Bill Kim
 * @since 4.1
 */
public class QueueNotFoundException extends QueueAttributesResolvingException {

	private final String queueName;

	/**
	 * Create an instance for the given queue name.
	 * @param queueName the name of the queue that was not found.
	 * @param cause the underlying cause, typically a
	 *     {@code software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException}.
	 */
	public QueueNotFoundException(String queueName, Throwable cause) {
		super("Queue not found: " + queueName, cause);
		this.queueName = queueName;
	}

	/**
	 * Return the name of the queue that was not found.
	 * @return the queue name.
	 */
	public String getQueueName() {
		return this.queueName;
	}

}
