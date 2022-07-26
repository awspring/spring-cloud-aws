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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Queue attributes extracted from SQS.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class QueueAttributes {

	private static final Logger logger = LoggerFactory.getLogger(QueueAttributes.class);

	private final String queueName;

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
	private QueueAttributes(String queueName, String queueUrl, boolean hasRedrivePolicy, Integer visibilityTimeout, boolean isFifo) {
		this.queueName = queueName;
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
	@Nullable
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

	public String getQueueName() {
		return this.queueName;
	}

	public static QueueAttributes fetchFor(String queueName, SqsAsyncClient sqsAsyncClient) {
		try {
			logger.debug("Fetching attributes for queue {}", queueName);
			String queueUrl = sqsAsyncClient.getQueueUrl(req -> req.queueName(queueName)).get().queueUrl();
			Map<QueueAttributeName, String> attributes = sqsAsyncClient
				.getQueueAttributes(req -> req.queueUrl(queueUrl).attributeNames(QueueAttributeName.ALL)).get().attributes();
			boolean hasRedrivePolicy = attributes.containsKey(QueueAttributeName.REDRIVE_POLICY);
			boolean isFifo = queueName.endsWith(".fifo");
			return new QueueAttributes(queueName, queueUrl, hasRedrivePolicy, getVisibility(attributes), isFifo);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while fetching attributes for queue " + queueName, e);
		}
		catch (ExecutionException e) {
			throw new IllegalStateException("ExecutionException while fetching attributes for queue " + queueName, e);
		}
	}

	@Nullable
	private static Integer getVisibility(Map<QueueAttributeName, String> attributes) {
		String visibilityTimeout = attributes.get(QueueAttributeName.VISIBILITY_TIMEOUT);
		return visibilityTimeout != null ? Integer.parseInt(visibilityTimeout) : null;
	}

}
