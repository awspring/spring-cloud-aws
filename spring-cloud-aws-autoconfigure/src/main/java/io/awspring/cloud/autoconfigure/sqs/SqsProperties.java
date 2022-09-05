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
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

/**
 * Properties related to AWS SQS.
 *
 * @author Tomaz Fernandes
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

	public static class Listener {
		/**
		 * The maximum number of simultaneous inflight messages in a queue.
		 */
		@Nullable
		private Integer maxInflightMessagesPerQueue;

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

		@Nullable
		public Integer getMaxInflightMessagesPerQueue() {
			return this.maxInflightMessagesPerQueue;
		}

		public void setMaxInflightMessagesPerQueue(Integer maxInflightMessagesPerQueue) {
			this.maxInflightMessagesPerQueue = maxInflightMessagesPerQueue;
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
	}

}
