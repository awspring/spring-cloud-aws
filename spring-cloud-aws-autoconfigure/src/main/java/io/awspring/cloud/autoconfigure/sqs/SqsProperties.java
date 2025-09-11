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
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

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

	private Batch batch = new Batch();

	public Listener getListener() {
		return this.listener;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public Batch getBatch() {
		return batch;
	}

	public void setBatch(Batch batch) {
		this.batch = batch;
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

	/**
	 * Configuration properties for SQS automatic batching using AWS SDK's {@code SqsAsyncBatchManager}.
	 * 
	 * <p>
	 * Automatic batching improves performance and reduces costs by combining multiple SQS requests into fewer AWS API
	 * calls. When enabled, Spring Cloud AWS will use a {@code BatchingSqsClientAdapter} that wraps the standard
	 * {@code SqsAsyncClient} with batching capabilities.
	 * 
	 * <p>
	 * <strong>Important:</strong> Batched operations are processed asynchronously, which may result in false positives
	 * where method calls appear to succeed locally but fail during actual transmission to AWS. Applications should
	 * handle the returned {@code CompletableFuture} objects to detect actual transmission errors.
	 * 
	 * @since 3.2
	 */
	public static class Batch {

		/**
		 * Enables SQS automatic batching using AWS SDK's SqsAsyncBatchManager.
		 * 
		 * <p>
		 * When set to {@code true}, the {@code SqsAsyncClient} bean will be wrapped with a
		 * {@code BatchingSqsClientAdapter} that automatically batches requests to improve performance and reduce AWS
		 * API calls.
		 * 
		 * <p>
		 * Default is {@code false}.
		 */
		private boolean enabled = false;

		/**
		 * The maximum number of messages that can be processed in a single batch. The maximum is 10.
		 */
		@Nullable
		private Integer maxNumberOfMessages;

		/**
		 * The frequency at which requests are sent to SQS when processing messages in a batch.
		 */
		@Nullable
		private Duration sendBatchFrequency;

		/**
		 * The visibility timeout to set for messages received in a batch. If unset, the queue default is used.
		 */
		@Nullable
		private Duration visibilityTimeout;

		/**
		 * The minimum wait duration for a receiveMessage request in a batch. To avoid unnecessary CPU usage, do not set
		 * this value to 0.
		 */
		@Nullable
		private Duration waitTimeSeconds;

		/**
		 * The list of system attribute names to request for receiveMessage calls.
		 */
		@Nullable
		private List<MessageSystemAttributeName> systemAttributeNames;

		/**
		 * The list of attribute names to request for receiveMessage calls.
		 */
		@Nullable
		private List<String> attributeNames;

		/**
		 * The size of the scheduled thread pool used for batching operations. This thread pool handles periodic batch
		 * sending and other scheduled tasks.
		 * 
		 * <p>
		 * Default is {@code 5}.
		 */
		private int scheduledExecutorPoolSize = 5;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Nullable
		public Integer getMaxNumberOfMessages() {
			return maxNumberOfMessages;
		}

		public void setMaxNumberOfMessages(Integer maxNumberOfMessages) {
			this.maxNumberOfMessages = maxNumberOfMessages;
		}

		@Nullable
		public Duration getSendBatchFrequency() {
			return sendBatchFrequency;
		}

		public void setSendBatchFrequency(Duration sendBatchFrequency) {
			this.sendBatchFrequency = sendBatchFrequency;
		}

		@Nullable
		public Duration getVisibilityTimeout() {
			return visibilityTimeout;
		}

		public void setVisibilityTimeout(Duration visibilityTimeout) {
			this.visibilityTimeout = visibilityTimeout;
		}

		@Nullable
		public Duration getWaitTimeSeconds() {
			return waitTimeSeconds;
		}

		public void setWaitTimeSeconds(Duration waitTimeSeconds) {
			this.waitTimeSeconds = waitTimeSeconds;
		}

		@Nullable
		public List<MessageSystemAttributeName> getSystemAttributeNames() {
			return systemAttributeNames;
		}

		public void setSystemAttributeNames(List<MessageSystemAttributeName> systemAttributeNames) {
			this.systemAttributeNames = systemAttributeNames;
		}

		@Nullable
		public List<String> getAttributeNames() {
			return attributeNames;
		}

		public void setAttributeNames(List<String> attributeNames) {
			this.attributeNames = attributeNames;
		}

		public int getScheduledExecutorPoolSize() {
			return scheduledExecutorPoolSize;
		}

		public void setScheduledExecutorPoolSize(int scheduledExecutorPoolSize) {
			this.scheduledExecutorPoolSize = scheduledExecutorPoolSize;
		}

	}

}
