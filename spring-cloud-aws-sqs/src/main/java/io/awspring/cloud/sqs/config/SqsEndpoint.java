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
package io.awspring.cloud.sqs.config;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Collection;

/**
 * {@link Endpoint} implementation for SQS endpoints.
 *
 * Contains properties that should be mapped from {@link SqsListener @SqsListener} annotations.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsEndpoint extends AbstractEndpoint {

	private final Integer maxInflightMessagesPerQueue;

	private final Integer pollTimeoutSeconds;

	private final Integer messageVisibility;

	private SqsEndpoint(Collection<String> logicalEndpointNames, String listenerContainerFactoryName,
						Integer maxInflightMessagesPerQueue, Integer pollTimeoutSeconds, Integer messageVisibility,
						String id) {
		super(logicalEndpointNames, listenerContainerFactoryName, id);
		this.maxInflightMessagesPerQueue = maxInflightMessagesPerQueue;
		this.pollTimeoutSeconds = pollTimeoutSeconds;
		this.messageVisibility = messageVisibility;
	}

	/**
	 * Return a {@link SqsEndpointBuilder} instance with the provided logical endpoint names.
	 * @param logicalEndpointNames the logical endpoint names for this endpoint.
	 * @return the builder instance.
	 */
	public static SqsEndpointBuilder from(Collection<String> logicalEndpointNames) {
		return new SqsEndpointBuilder(logicalEndpointNames);
	}

	/**
	 * The maximum number of inflight messages each queue in this endpoint can process simultaneously.
	 * @return the maximum number of inflight messages.
	 */
	@Nullable
	public Integer getMaxInflightMessagesPerQueue() {
		return this.maxInflightMessagesPerQueue;
	}

	/**
	 * The maximum duration to wait for messages in a given poll.
	 * @return the poll timeout.
	 */
	@Nullable
	public Duration getPollTimeout() {
		return this.pollTimeoutSeconds != null ? Duration.ofSeconds(this.pollTimeoutSeconds) : null;
	}

	/**
	 *
	 */
	@Nullable
	public Duration getMessageVisibilityDuration() {
		return this.messageVisibility != null ? Duration.ofSeconds(this.messageVisibility) : null;
	}

	public static class SqsEndpointBuilder {

		private final Collection<String> logicalEndpointNames;

		private Integer maxInflightMessagesPerQueue;

		private Integer pollTimeoutSeconds;

		private String factoryName;

		private Integer messageVisibility;

		private Boolean async;

		private String id;

		public SqsEndpointBuilder(Collection<String> logicalEndpointNames) {
			this.logicalEndpointNames = logicalEndpointNames;
		}

		public SqsEndpointBuilder factoryBeanName(String factoryName) {
			this.factoryName = factoryName;
			return this;
		}

		public SqsEndpointBuilder maxInflightMessagesPerQueue(Integer maxInflightMessagesPerQueue) {
			this.maxInflightMessagesPerQueue = maxInflightMessagesPerQueue;
			return this;
		}

		public SqsEndpointBuilder pollTimeoutSeconds(Integer pollTimeoutSeconds) {
			this.pollTimeoutSeconds = pollTimeoutSeconds;
			return this;
		}

		public SqsEndpointBuilder messageVisibility(Integer messageVisibility) {
			this.messageVisibility = messageVisibility;
			return this;
		}

		public SqsEndpointBuilder id(String id) {
			this.id = id;
			return this;
		}

		public SqsEndpoint build() {
			return new SqsEndpoint(this.logicalEndpointNames, this.factoryName, this.maxInflightMessagesPerQueue,
					this.pollTimeoutSeconds, this.messageVisibility, this.id);
		}
	}

}
