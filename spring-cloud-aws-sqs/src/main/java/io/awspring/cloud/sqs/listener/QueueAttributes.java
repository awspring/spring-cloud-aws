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

import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Queue attributes extracted from SQS, as well as the queue name and url.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see ContainerOptions#getQueueAttributeNames().
 */
public class QueueAttributes {

	private final String queueName;

	private final String queueUrl;

	private final Map<QueueAttributeName, String> attributes;

	/**
	 * Create an instance with the provided arguments.
	 * @param queueName the queue name.
	 * @param queueUrl the queue url.
	 * @param attributes the queue attributes retrieved from SQS.
	 */
	public QueueAttributes(String queueName, String queueUrl, Map<QueueAttributeName, String> attributes) {
		Assert.notNull(queueName, "queueName cannot be null");
		Assert.notNull(queueUrl, "queueUrl cannot be null");
		Assert.notNull(attributes, "attributes cannot be null");
		this.queueName = queueName;
		this.queueUrl = queueUrl;
		this.attributes = attributes;
	}

	/**
	 * Return the queue url.
	 * @return the url.
	 */
	public String getQueueUrl() {
		return this.queueUrl;
	}

	/**
	 * Return the queue name.
	 * @return the queue name.
	 */
	public String getQueueName() {
		return this.queueName;
	}

	/**
	 * Return the attributes for this queue.
	 * @return the queue attributes.
	 */
	public Map<QueueAttributeName, String> getQueueAttributes() {
		return new HashMap<>(this.attributes);
	}

	/**
	 * Return a specific attribute for this queue, if present.
	 * @param queueAttributeName the attribute name.
	 * @return the queue attribute, or null if no such attribute is present.
	 */
	@Nullable
	public String getQueueAttribute(QueueAttributeName queueAttributeName) {
		return this.attributes.get(queueAttributeName);
	}

}
