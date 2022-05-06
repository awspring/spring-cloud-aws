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
package io.awspring.cloud.sqs.endpoint;

import io.awspring.cloud.messaging.support.endpoint.Endpoint;
import io.awspring.cloud.sqs.listener.QueueAttributes;
import java.util.Collection;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsEndpoint implements Endpoint {

	private final Collection<String> logicalEndpointNames;

	private final String listenerContainerFactoryName;

	private final Integer simultaneousPollsPerQueue;

	private final Integer pollTimeoutSeconds;

	private final Integer minTimeToProcess;

	private final Map<String, QueueAttributes> queueAttributesMap;

	private final boolean isAsync;

	private SqsEndpoint(Collection<String> logicalEndpointNames, String listenerContainerFactoryName,
			Integer simultaneousPollsPerQueue, Integer pollTimeoutSeconds, Integer minTimeToProcess,
			Map<String, QueueAttributes> queueAttributesMap, boolean isAsync) {
		Assert.notEmpty(logicalEndpointNames, "logicalEndpointNames cannot be null.");
		this.queueAttributesMap = queueAttributesMap;
		this.logicalEndpointNames = logicalEndpointNames;
		this.listenerContainerFactoryName = listenerContainerFactoryName;
		this.simultaneousPollsPerQueue = simultaneousPollsPerQueue;
		this.pollTimeoutSeconds = pollTimeoutSeconds;
		this.minTimeToProcess = minTimeToProcess;
		this.isAsync = isAsync;
	}

	public static SqsEndpointBuilder from(Collection<String> logicalEndpointNames) {
		return new SqsEndpointBuilder(logicalEndpointNames);
	}

	@Override
	public Collection<String> getLogicalEndpointNames() {
		return logicalEndpointNames;
	}

	@Override
	public String getListenerContainerFactoryName() {
		return this.listenerContainerFactoryName;
	}

	public Integer getSimultaneousPollsPerQueue() {
		return this.simultaneousPollsPerQueue;
	}

	public Integer getPollTimeoutSeconds() {
		return this.pollTimeoutSeconds;
	}

	public Integer getMinTimeToProcess() {
		return this.minTimeToProcess;
	}

	public QueueAttributes getAttributesFor(String queueName) {
		return this.queueAttributesMap.get(queueName);
	}

	public Map<String, QueueAttributes> getQueueAttributes() {
		return queueAttributesMap;
	}

	public boolean isAsync() {
		return isAsync;
	}

	public static class SqsEndpointBuilder {

		private final Collection<String> logicalEndpointNames;

		private Integer simultaneousPollsPerQueue;

		private Integer pollTimeoutSeconds;

		private String factoryName;

		private Integer minTimeToProcess;

		private Map<String, QueueAttributes> queueAttributesMap;

		private boolean async;

		public SqsEndpointBuilder(Collection<String> logicalEndpointNames) {
			this.logicalEndpointNames = logicalEndpointNames;
		}

		public SqsEndpointBuilder factoryBeanName(String factoryName) {
			this.factoryName = factoryName;
			return this;
		}

		public SqsEndpointBuilder simultaneousPollsPerQueue(Integer simultaneousPollsPerQueue) {
			this.simultaneousPollsPerQueue = simultaneousPollsPerQueue;
			return this;
		}

		public SqsEndpointBuilder pollTimeoutSeconds(Integer pollTimeoutSeconds) {
			this.pollTimeoutSeconds = pollTimeoutSeconds;
			return this;
		}

		public SqsEndpointBuilder minTimeToProcess(Integer minTimeToProcess) {
			this.minTimeToProcess = minTimeToProcess;
			return this;
		}

		public SqsEndpointBuilder queuesAttributes(Map<String, QueueAttributes> queueAttributesMap) {
			this.queueAttributesMap = queueAttributesMap;
			return this;
		}

		public SqsEndpointBuilder async(boolean async) {
			this.async = async;
			return this;
		}

		public SqsEndpoint build() {
			return new SqsEndpoint(this.logicalEndpointNames, this.factoryName, this.simultaneousPollsPerQueue,
					this.pollTimeoutSeconds, this.minTimeToProcess, this.queueAttributesMap, this.async);
		}
	}

}
