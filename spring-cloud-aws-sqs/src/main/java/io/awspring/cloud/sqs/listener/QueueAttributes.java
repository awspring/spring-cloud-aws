/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sqs.listener;

/**
 * Queue attributes extracted from SQS.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see QueueAttributesResolver
 */
public class QueueAttributes {

	private final String queueUrl;

	private final boolean hasRedrivePolicy;

	private final Integer visibilityTimeout;

	private final boolean isFifo;

	/**
	 * Create an instance with the provided arguments.
	 * @param queueUrl the url for this queue.
	 * @param hasRedrivePolicy whether this queue has a redrive policy.
	 * @param visibilityTimeout the visibility timeout for this queue.
	 * @param isFifo whether this is a FIFO queue.
	 */
	public QueueAttributes(String queueUrl, boolean hasRedrivePolicy, Integer visibilityTimeout, boolean isFifo) {
		this.hasRedrivePolicy = hasRedrivePolicy;
		this.queueUrl = queueUrl;
		this.visibilityTimeout = visibilityTimeout;
		this.isFifo = isFifo;
	}

	/**
	 * Return the url for this queue.
	 * @return the url.
	 */
	public String getQueueUrl() {
		return this.queueUrl;
	}

	/**
	 * Return whether this queue has a redrive policy.
	 * @return true if this queue has a redrive policy.
	 */
	public boolean hasRedrivePolicy() {
		return this.hasRedrivePolicy;
	}

	/**
	 * Return the visibility timeout for this queue.
	 * @return the visibility timeout.
	 */
	public Integer getVisibilityTimeout() {
		return this.visibilityTimeout;
	}

	/**
	 * Return whether this is a FIFO queue.
	 * @return true if this is a FIFO queue.
	 */
	boolean isFifo() {
		return this.isFifo;
	}
}
