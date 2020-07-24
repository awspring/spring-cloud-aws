/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;

/**
 * Properties related to SQS integration.
 *
 * @author Maciej Walkowiak
 */
@ConfigurationProperties("cloud.aws.sqs")
public class SqsProperties {

	/**
	 * Properties related to {@link SimpleMessageListenerContainer}.
	 */
	private ListenerProperties listener = new ListenerProperties();

	/**
	 * Properties related to {@link QueueMessageHandler}.
	 */
	private HandlerProperties handler = new HandlerProperties();

	public ListenerProperties getListener() {
		return listener;
	}

	public void setListener(ListenerProperties listener) {
		this.listener = listener;
	}

	public HandlerProperties getHandler() {
		return handler;
	}

	public void setHandler(HandlerProperties handler) {
		this.handler = handler;
	}

	public static class ListenerProperties {

		/**
		 * The maximum number of messages that should be retrieved during one poll to the
		 * Amazon SQS system. This number must be a positive, non-zero number that has a
		 * maximum number of 10. Values higher then 10 are currently not supported by the
		 * queueing system.
		 */
		private Integer maxNumberOfMessages = 10;

		/**
		 * The duration (in seconds) that the received messages are hidden from subsequent
		 * poll requests after being retrieved from the system.
		 */
		private Integer visibilityTimeout;

		/**
		 * The wait timeout that the poll request will wait for new message to arrive if
		 * the are currently no messages on the queue. Higher values will reduce poll
		 * request to the system significantly. The value should be between 1 and 20. For
		 * more information read the <a href=
		 * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-long-polling.html">documentation</a>.
		 */
		private Integer waitTimeout = 20;

		/**
		 * The queue stop timeout that waits for a queue to stop before interrupting the
		 * running thread.
		 */
		private Long queueStopTimeout;

		/**
		 * The number of milliseconds the polling thread must wait before trying to
		 * recover when an error occurs (e.g. connection timeout).
		 */
		private Long backOffTime;

		/**
		 * Configures if this container should be automatically started.
		 */
		private boolean autoStartup = true;

		public Integer getMaxNumberOfMessages() {
			return maxNumberOfMessages;
		}

		public void setMaxNumberOfMessages(Integer maxNumberOfMessages) {
			this.maxNumberOfMessages = maxNumberOfMessages;
		}

		public Integer getVisibilityTimeout() {
			return visibilityTimeout;
		}

		public void setVisibilityTimeout(Integer visibilityTimeout) {
			this.visibilityTimeout = visibilityTimeout;
		}

		public Integer getWaitTimeout() {
			return waitTimeout;
		}

		public void setWaitTimeout(Integer waitTimeout) {
			this.waitTimeout = waitTimeout;
		}

		public Long getQueueStopTimeout() {
			return queueStopTimeout;
		}

		public void setQueueStopTimeout(Long queueStopTimeout) {
			this.queueStopTimeout = queueStopTimeout;
		}

		public Long getBackOffTime() {
			return backOffTime;
		}

		public void setBackOffTime(Long backOffTime) {
			this.backOffTime = backOffTime;
		}

		public boolean isAutoStartup() {
			return autoStartup;
		}

		public void setAutoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

	}

	public static class HandlerProperties {

		/**
		 * Configures global deletion policy used if deletion policy is not explicitly set
		 * on {@link SqsListener}.
		 */
		private SqsMessageDeletionPolicy defaultDeletionPolicy = SqsMessageDeletionPolicy.NO_REDRIVE;

		public SqsMessageDeletionPolicy getDefaultDeletionPolicy() {
			return defaultDeletionPolicy;
		}

		public void setDefaultDeletionPolicy(
				SqsMessageDeletionPolicy defaultDeletionPolicy) {
			this.defaultDeletionPolicy = defaultDeletionPolicy;
		}

	}

}
