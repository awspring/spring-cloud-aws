
package io.awspring.cloud.v3.autoconfigure.sqs;

import io.awspring.cloud.v3.autoconfigure.AwsClientProperties;
import io.awspring.cloud.v3.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties related to SQS integration.
 *
 * @author Maciej Walkowiak
 * @author Eddú Meléndez
 */
@ConfigurationProperties(SqsProperties.PREFIX)
public class SqsProperties extends AwsClientProperties {

	/**
	 * The prefix used for AWS credentials related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.sqs";

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

		public void setDefaultDeletionPolicy(SqsMessageDeletionPolicy defaultDeletionPolicy) {
			this.defaultDeletionPolicy = defaultDeletionPolicy;
		}

	}
}
