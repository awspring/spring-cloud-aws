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

import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Utility class for retrieving {@link QueueAttributes} for a given queue.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class QueueAttributesResolver {

	private QueueAttributesResolver() {
	}

	private static final Logger logger = LoggerFactory.getLogger(QueueAttributesResolver.class);

	/**
	 * Resolve the attributes for the provided queue.
	 * @param queueName the queue name.
	 * @param sqsAsyncClient the client to be used to fetch the attributes.
	 * @return the attributes.
	 */
	public static QueueAttributes resolveAttributes(String queueName, SqsAsyncClient sqsAsyncClient) {
		try {
			logger.debug("Fetching attributes for queue {}", queueName);
			String queueUrl = sqsAsyncClient.getQueueUrl(req -> req.queueName(queueName)).get().queueUrl();
			GetQueueAttributesResponse getQueueAttributesResponse = sqsAsyncClient
					.getQueueAttributes(req -> req.queueUrl(queueUrl).attributeNames(QueueAttributeName.ALL)).get();
			Map<QueueAttributeName, String> attributes = getQueueAttributesResponse.attributes();
			boolean hasRedrivePolicy = attributes.containsKey(QueueAttributeName.REDRIVE_POLICY);
			boolean isFifo = queueName.endsWith(".fifo");
			return new QueueAttributes(queueUrl, hasRedrivePolicy, getVisibility(attributes), isFifo);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while fetching attributes for queue " + queueName, e);
		}
		catch (ExecutionException e) {
			throw new IllegalStateException("ExecutionException while fetching attributes for queue " + queueName, e);
		}
	}

	private static Integer getVisibility(Map<QueueAttributeName, String> attributes) {
		String visibilityTimeout = attributes.get(QueueAttributeName.VISIBILITY_TIMEOUT);
		return visibilityTimeout != null ? Integer.parseInt(visibilityTimeout) : null;
	}

}
