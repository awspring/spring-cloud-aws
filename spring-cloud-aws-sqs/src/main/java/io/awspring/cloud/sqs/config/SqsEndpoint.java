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
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import java.time.Duration;
import java.util.Collection;
import org.springframework.lang.Nullable;

/**
 * {@link Endpoint} implementation for SQS endpoints.
 *
 * Contains properties that should be mapped from {@link SqsListener @SqsListener} annotations.
 *
 * @author Tomaz Fernandes
 * @author Joao Calassio
 * @since 3.0
 */
public class SqsEndpoint extends AbstractEndpoint {

	private final Integer maxConcurrentMessages;

	private final Integer pollTimeoutSeconds;

	private final Integer messageVisibility;

	private final Integer maxMessagesPerPoll;

	@Nullable
	private final AcknowledgementMode acknowledgementMode;

	protected SqsEndpoint(SqsEndpointBuilder builder) {
		super(builder.queueNames, builder.factoryName, builder.id);
		this.maxConcurrentMessages = builder.maxConcurrentMessages;
		this.pollTimeoutSeconds = builder.pollTimeoutSeconds;
		this.messageVisibility = builder.messageVisibility;
		this.maxMessagesPerPoll = builder.maxMessagesPerPoll;
		this.acknowledgementMode = builder.acknowledgementMode;
	}

	/**
	 * Return a {@link SqsEndpointBuilder} instance with the provided queue names.
	 * @return the builder instance.
	 */
	public static SqsEndpointBuilder builder() {
		return new SqsEndpointBuilder();
	}

	/**
	 * Set the maximum concurrent messages that can be processed simultaneously for each queue. Note that if
	 * acknowledgement batching is being used, the actual maximum number of messages inflight might be higher.
	 * @return the maximum number of inflight messages.
	 */
	@Nullable
	public Integer getMaxConcurrentMessages() {
		return this.maxConcurrentMessages;
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
	 * Return the maximum amount of messages that should be returned in a poll.
	 * @return the maximum amount of messages.
	 */
	@Nullable
	public Integer getMaxMessagesPerPoll() {
		return this.maxMessagesPerPoll;
	}

	/**
	 * Return the message visibility for this endpoint.
	 * @return the message visibility.
	 */
	@Nullable
	public Duration getMessageVisibility() {
		return this.messageVisibility != null ? Duration.ofSeconds(this.messageVisibility) : null;
	}

	/**
	 * Returns the acknowledgement mode configured for this endpoint.
	 * @return the acknowledgement mode.
	 */
	@Nullable
	public AcknowledgementMode getAcknowledgementMode() {
		return this.acknowledgementMode;
	}

	public static class SqsEndpointBuilder {

		private Collection<String> queueNames;

		private Integer maxConcurrentMessages;

		private Integer pollTimeoutSeconds;

		private String factoryName;

		private Integer messageVisibility;

		private String id;

		private Integer maxMessagesPerPoll;

		@Nullable
		private AcknowledgementMode acknowledgementMode;

		public SqsEndpointBuilder queueNames(Collection<String> queueNames) {
			this.queueNames = queueNames;
			return this;
		}

		public SqsEndpointBuilder factoryBeanName(String factoryName) {
			this.factoryName = factoryName;
			return this;
		}

		public SqsEndpointBuilder maxConcurrentMessages(Integer maxConcurrentMessages) {
			this.maxConcurrentMessages = maxConcurrentMessages;
			return this;
		}

		public SqsEndpointBuilder pollTimeoutSeconds(Integer pollTimeoutSeconds) {
			this.pollTimeoutSeconds = pollTimeoutSeconds;
			return this;
		}

		public SqsEndpointBuilder maxMessagesPerPoll(Integer maxMessagesPerPoll) {
			this.maxMessagesPerPoll = maxMessagesPerPoll;
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

		public SqsEndpointBuilder acknowledgementMode(@Nullable AcknowledgementMode acknowledgementMode) {
			this.acknowledgementMode = acknowledgementMode;
			return this;
		}

		public SqsEndpoint build() {
			return new SqsEndpoint(this);
		}
	}

}
