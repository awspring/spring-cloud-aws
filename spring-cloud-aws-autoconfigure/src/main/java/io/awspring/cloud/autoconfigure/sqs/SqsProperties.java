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
package io.awspring.cloud.autoconfigure.sqs;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

/**
 * Properties related to AWS SQS.
 *
 * @author Tomaz Fernandes
 * @author Wei Jiang
 * @since 3.0
 */
@ConfigurationProperties(prefix = SqsProperties.PREFIX)
public class SqsProperties extends AwsClientProperties {

	/**
	 * The prefix used for AWS SQS configuration.
	 */
	public static final String PREFIX = "spring.cloud.aws.sqs";

	private Listener listener = new Listener();

	public Listener getListener() {
		return this.listener;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Nullable
	private QueueNotFoundStrategy queueNotFoundStrategy;

	private Boolean observationEnabled = false;

	/**
	 * Return the strategy to use if the queue is not found.
	 * @return the {@link QueueNotFoundStrategy}
	 */
	@Nullable
	public QueueNotFoundStrategy getQueueNotFoundStrategy() {
		return queueNotFoundStrategy;
	}

	/**
	 * Set the strategy to use if the queue is not found.
	 * @param queueNotFoundStrategy the strategy to set.
	 */
	public void setQueueNotFoundStrategy(QueueNotFoundStrategy queueNotFoundStrategy) {
		this.queueNotFoundStrategy = queueNotFoundStrategy;
	}

	public Boolean isObservationEnabled() {
		return this.observationEnabled;
	}

	public void setObservationEnabled(Boolean observationEnabled) {
		this.observationEnabled = observationEnabled;
	}

	public static class Listener {

		/**
		 * The maximum concurrent messages that can be processed simultaneously for each queue. Note that if
		 * acknowledgement batching is being used, the actual maximum number of messages inflight might be higher.
		 */
		@Nullable
		private Integer maxConcurrentMessages;

		/**
		 * The maximum number of messages to be retrieved in a single poll to SQS.
		 */
		@Nullable
		private Integer maxMessagesPerPoll;

		/**
		 * The maximum amount of time for a poll to SQS.
		 */
		@Nullable
		private Duration pollTimeout;

		/**
		 * The maximum amount of time to wait between consecutive polls to SQS.
		 */
		@Nullable
		private Duration maxDelayBetweenPolls;

		/**
		 * Defines whether SQS listeners will start automatically or not.
		 */
		@Nullable
		private Boolean autoStartup;

		@Nullable
		public Integer getMaxConcurrentMessages() {
			return this.maxConcurrentMessages;
		}

		public void setMaxConcurrentMessages(Integer maxConcurrentMessages) {
			this.maxConcurrentMessages = maxConcurrentMessages;
		}

		@Nullable
		public Integer getMaxMessagesPerPoll() {
			return this.maxMessagesPerPoll;
		}

		public void setMaxMessagesPerPoll(Integer maxMessagesPerPoll) {
			this.maxMessagesPerPoll = maxMessagesPerPoll;
		}

		@Nullable
		public Duration getPollTimeout() {
			return this.pollTimeout;
		}

		public void setPollTimeout(Duration pollTimeout) {
			this.pollTimeout = pollTimeout;
		}

		@Nullable
		public Duration getMaxDelayBetweenPolls() {
			return maxDelayBetweenPolls;
		}

		public void setMaxDelayBetweenPolls(Duration maxDelayBetweenPolls) {
			this.maxDelayBetweenPolls = maxDelayBetweenPolls;
		}

		@Nullable
		public Boolean getAutoStartup() {
			return autoStartup;
		}

		public void setAutoStartup(Boolean autoStartup) {
			this.autoStartup = autoStartup;
		}
	}

}
