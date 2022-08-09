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

import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Contains the options to be used by the {@link MessageListenerContainer} at runtime.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class ContainerOptions {

	private final int maxInflightMessagesPerQueue;

	private final int maxMessagesPerPoll;

	private final Duration pollTimeout;

	private final Duration permitAcquireTimeout;

	private final Duration sourceShutdownTimeout;

	private final BackPressureMode backPressureMode;

	private final MessageDeliveryStrategy messageDeliveryStrategy;

	private final Collection<QueueAttributeName> queueAttributeNames;

	private final Collection<String> messageAttributeNames;

	private final Collection<String> messageSystemAttributeNames;

	private final MessagingMessageConverter<?> messageConverter;

	private final AcknowledgementMode acknowledgementMode;

	private final QueueNotFoundStrategy queueNotFoundStrategy;

	private final AcknowledgementOrdering acknowledgementOrdering;

	private final Duration acknowledgementInterval;

	private final Integer acknowledgementThreshold;

	private final TaskExecutor containerComponentsTaskExecutor;

	private final Duration messageVisibility;

	private ContainerOptions(Builder builder) {
		this.maxInflightMessagesPerQueue = builder.maxInflightMessagesPerQueue;
		this.maxMessagesPerPoll = builder.maxMessagesPerPoll;
		this.pollTimeout = builder.pollTimeout;
		this.permitAcquireTimeout = builder.permitAcquireTimeout;
		this.sourceShutdownTimeout = builder.shutdownTimeout;
		this.backPressureMode = builder.backPressureMode;
		this.messageDeliveryStrategy = builder.messageDeliveryStrategy;
		this.queueAttributeNames = builder.queueAttributeNames;
		this.messageAttributeNames = builder.messageAttributeNames;
		this.messageSystemAttributeNames = builder.messageSystemAttributeNames;
		this.messageConverter = builder.messageConverter;
		this.acknowledgementMode = builder.acknowledgementMode;
		this.queueNotFoundStrategy = builder.queueNotFoundStrategy;
		this.acknowledgementOrdering = builder.acknowledgementOrdering;
		this.acknowledgementInterval = builder.acknowledgementInterval;
		this.acknowledgementThreshold = builder.acknowledgementThreshold;
		this.containerComponentsTaskExecutor = builder.componentsTaskExecutor;
		this.messageVisibility = builder.messageVisibility;
	}

	public static ContainerOptions.Builder builder() {
		return new ContainerOptions.Builder();
	}

	/**
	 * Return the maximum allowed number of inflight messages for each queue.
	 * @return the number.
	 */
	public int getMaxInFlightMessagesPerQueue() {
		return this.maxInflightMessagesPerQueue;
	}

	/**
	 * Return the number of messages that should be returned per poll.
	 * @return the number.
	 */
	public int getMaxMessagesPerPoll() {
		return this.maxMessagesPerPoll;
	}

	/**
	 * Return the timeout for polling messages for this endpoint.
	 * @return the timeout duration.
	 */
	public Duration getPollTimeout() {
		return this.pollTimeout;
	}

	/**
	 * Return the maximum time the polling thread should wait for permits.
	 * @return the timeout.
	 */
	public Duration getPermitAcquireTimeout() {
		return this.permitAcquireTimeout;
	}

	public TaskExecutor getContainerComponentsTaskExecutor() {
		return this.containerComponentsTaskExecutor;
	}

	public Duration getShutdownTimeout() {
		return this.sourceShutdownTimeout;
	}

	public BackPressureMode getBackPressureMode() {
		return this.backPressureMode;
	}

	public MessageDeliveryStrategy getMessageDeliveryStrategy() {
		return this.messageDeliveryStrategy;
	}

	public Collection<QueueAttributeName> getQueueAttributeNames() {
		return this.queueAttributeNames;
	}

	public Collection<String> getMessageAttributeNames() {
		return this.messageAttributeNames;
	}

	public Collection<String> getMessageSystemAttributeNames() {
		return this.messageSystemAttributeNames;
	}

	public Duration getMessageVisibility() {
		return this.messageVisibility;
	}

	public MessagingMessageConverter<?> getMessageConverter() {
		return this.messageConverter;
	}

	public Duration getAcknowledgementInterval() {
		return this.acknowledgementInterval;
	}

	public Integer getAcknowledgementThreshold() {
		return this.acknowledgementThreshold;
	}

	public AcknowledgementMode getAcknowledgementMode() {
		return this.acknowledgementMode;
	}

	public AcknowledgementOrdering getAcknowledgementOrdering() {
		return this.acknowledgementOrdering;
	}

	public QueueNotFoundStrategy getQueueNotFoundStrategy() {
		return this.queueNotFoundStrategy;
	}

	public ContainerOptions configure(ConfigurableContainerComponent configurable) {
		configurable.configure(this);
		return this;
	}

	public ContainerOptions configure(Collection<? extends ConfigurableContainerComponent> configurables) {
		configurables.forEach(this::configure);
		return this;
	}

	public ContainerOptions createCopy() {
		ContainerOptions newCopy = ContainerOptions.builder().build();
		ReflectionUtils.shallowCopyFieldState(this, newCopy);
		return newCopy;
	}

	/**
	 * Validate these options.
	 */
	public void validate() {
		Assert.isTrue(this.maxMessagesPerPoll <= maxInflightMessagesPerQueue, String.format(
				"messagesPerPoll should be less than or equal to maxInflightMessagesPerQueue. Values provided: %s and %s respectively",
				this.maxMessagesPerPoll, this.maxInflightMessagesPerQueue));
		Assert.isTrue(this.maxMessagesPerPoll <= 10, "messagesPerPoll must be less than or equal to 10.");
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static class Builder {

		private static final int DEFAULT_MAX_INFLIGHT_MSG_PER_QUEUE = 10;

		private static final int DEFAULT_MAX_MESSAGES_PER_POLL = 10;

		private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofSeconds(10);

		private static final Duration DEFAULT_SEMAPHORE_TIMEOUT = Duration.ofSeconds(10);

		private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(20);

		private static final BackPressureMode DEFAULT_THROUGHPUT_CONFIGURATION = BackPressureMode.AUTO;

		private static final MessageDeliveryStrategy DEFAULT_MESSAGE_DELIVERY_STRATEGY = MessageDeliveryStrategy.SINGLE_MESSAGE;

		private static final List<QueueAttributeName> DEFAULT_QUEUE_ATTRIBUTES_NAMES = Collections.emptyList();

		private static final List<String> DEFAULT_MESSAGE_ATTRIBUTES_NAMES = Collections
				.singletonList(QueueAttributeName.ALL.toString());

		private static final List<String> DEFAULT_MESSAGE_SYSTEM_ATTRIBUTES = Collections
				.singletonList(QueueAttributeName.ALL.toString());

		private static final MessagingMessageConverter<?> DEFAULT_MESSAGE_CONVERTER = new SqsMessagingMessageConverter();

		private static final AcknowledgementMode DEFAULT_ACKNOWLEDGEMENT_MODE = AcknowledgementMode.ON_SUCCESS;

		private static final QueueNotFoundStrategy DEFAULT_QUEUE_NOT_FOUND_STRATEGY = QueueNotFoundStrategy.CREATE;

		private int maxInflightMessagesPerQueue = DEFAULT_MAX_INFLIGHT_MSG_PER_QUEUE;

		private int maxMessagesPerPoll = DEFAULT_MAX_MESSAGES_PER_POLL;

		private Duration pollTimeout = DEFAULT_POLL_TIMEOUT;

		private Duration permitAcquireTimeout = DEFAULT_SEMAPHORE_TIMEOUT;

		private BackPressureMode backPressureMode = DEFAULT_THROUGHPUT_CONFIGURATION;

		private Duration shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;

		private MessageDeliveryStrategy messageDeliveryStrategy = DEFAULT_MESSAGE_DELIVERY_STRATEGY;

		private Collection<QueueAttributeName> queueAttributeNames = DEFAULT_QUEUE_ATTRIBUTES_NAMES;

		private Collection<String> messageAttributeNames = DEFAULT_MESSAGE_ATTRIBUTES_NAMES;

		private Collection<String> messageSystemAttributeNames = DEFAULT_MESSAGE_SYSTEM_ATTRIBUTES;

		private MessagingMessageConverter<?> messageConverter = DEFAULT_MESSAGE_CONVERTER;

		private QueueNotFoundStrategy queueNotFoundStrategy = DEFAULT_QUEUE_NOT_FOUND_STRATEGY;

		private AcknowledgementMode acknowledgementMode = DEFAULT_ACKNOWLEDGEMENT_MODE;

		private AcknowledgementOrdering acknowledgementOrdering;

		private Duration acknowledgementInterval;

		private Integer acknowledgementThreshold;

		private TaskExecutor componentsTaskExecutor;

		private Duration messageVisibility;

		private Builder() {
		}

		private Builder(ContainerOptions options) {
			this.maxInflightMessagesPerQueue = options.maxInflightMessagesPerQueue;
			this.maxMessagesPerPoll = options.maxMessagesPerPoll;
			this.pollTimeout = options.pollTimeout;
			this.permitAcquireTimeout = options.permitAcquireTimeout;
			this.shutdownTimeout = options.sourceShutdownTimeout;
			this.backPressureMode = options.backPressureMode;
			this.messageDeliveryStrategy = options.messageDeliveryStrategy;
			this.queueAttributeNames = options.queueAttributeNames;
			this.messageAttributeNames = options.messageAttributeNames;
			this.messageSystemAttributeNames = options.messageSystemAttributeNames;
			this.messageConverter = options.messageConverter;
			this.acknowledgementMode = options.acknowledgementMode;
			this.queueNotFoundStrategy = options.queueNotFoundStrategy;
			this.acknowledgementOrdering = options.acknowledgementOrdering;
			this.acknowledgementInterval = options.acknowledgementInterval;
			this.acknowledgementThreshold = options.acknowledgementThreshold;
			this.componentsTaskExecutor = options.containerComponentsTaskExecutor;
			this.messageVisibility = options.messageVisibility;
		}

		/**
		 * Set the maximum allowed number of inflight messages for each queue.
		 * @return this instance.
		 */
		public Builder maxInflightMessagesPerQueue(int maxInflightMessagesPerQueue) {
			Assert.isTrue(maxInflightMessagesPerQueue > 0, "maxInflightMessagesPerQueue must be greater than zero");
			this.maxInflightMessagesPerQueue = maxInflightMessagesPerQueue;
			return this;
		}

		/**
		 * Set the number of messages that should be returned per poll.
		 * @param maxMessagesPerPoll the number of messages.
		 * @return this instance.
		 */
		public Builder maxMessagesPerPoll(int maxMessagesPerPoll) {
			Assert.isTrue(maxMessagesPerPoll > 0 && maxMessagesPerPoll <= 10, "maxMessagesPerPoll must be between 1 and 10");
			this.maxMessagesPerPoll = maxMessagesPerPoll;
			return this;
		}

		/**
		 * Set the timeout for polling messages for this endpoint.
		 * @param pollTimeout the poll timeout.
		 * @return this instance.
		 */
		public Builder pollTimeout(Duration pollTimeout) {
			Assert.notNull(pollTimeout, "pollTimeout cannot be null");
			this.pollTimeout = pollTimeout;
			return this;
		}

		/**
		 * Set the maximum time the polling thread should wait for permits.
		 * @param permitAcquireTimeout the timeout.
		 * @return this instance.
		 */
		public Builder permitAcquireTimeout(Duration permitAcquireTimeout) {
			Assert.notNull(permitAcquireTimeout, "semaphoreAcquireTimeout cannot be null");
			this.permitAcquireTimeout = permitAcquireTimeout;
			return this;
		}

		public Builder messageDeliveryStrategy(MessageDeliveryStrategy messageDeliveryStrategy) {
			Assert.notNull(messageDeliveryStrategy, "messageDeliveryStrategy cannot be null");
			this.messageDeliveryStrategy = messageDeliveryStrategy;
			return this;
		}

		public Builder componentsTaskExecutor(TaskExecutor taskExecutor) {
			Assert.notNull(taskExecutor, "executor cannot be null");
			this.componentsTaskExecutor = taskExecutor;
			return this;
		}

		public Builder shutdownTimeout(Duration shutdownTimeout) {
			Assert.notNull(shutdownTimeout, "shutdownTimeout cannot be null");
			this.shutdownTimeout = shutdownTimeout;
			return this;
		}

		public Builder backPressureMode(BackPressureMode backPressureMode) {
			Assert.notNull(backPressureMode, "backPressureMode cannot be null");
			this.backPressureMode = backPressureMode;
			return this;
		}

		public Builder queueAttributeNames(Collection<QueueAttributeName> queueAttributeNames) {
			Assert.notEmpty(queueAttributeNames, "queueAttributeNames cannot be empty");
			this.queueAttributeNames = queueAttributeNames;
			return this;
		}

		public Builder messageAttributeNames(Collection<String> messageAttributeNames) {
			Assert.notEmpty(messageAttributeNames, "messageAttributeNames cannot be empty");
			this.messageAttributeNames = messageAttributeNames;
			return this;
		}

		public Builder messageSystemAttributeNames(Collection<MessageSystemAttributeName> messageSystemAttributeNames) {
			Assert.notEmpty(messageSystemAttributeNames, "messageSystemAttributeNames cannot be empty");
			this.messageSystemAttributeNames = messageSystemAttributeNames.stream()
					.map(MessageSystemAttributeName::toString).collect(Collectors.toList());
			return this;
		}

		public Builder messageVisibility(Duration messageVisibility) {
			Assert.notNull(messageVisibility, "messageVisibility cannot be null");
			this.messageVisibility = messageVisibility;
			return this;
		}

		public Builder acknowledgementInterval(Duration acknowledgementInterval) {
			Assert.notNull(acknowledgementInterval, "acknowledgementInterval cannot be null");
			this.acknowledgementInterval = acknowledgementInterval;
			return this;
		}

		public Builder acknowledgementThreshold(int acknowledgementThreshold) {
			Assert.isTrue(acknowledgementThreshold >= 0,
					"acknowledgementThreshold must be greater than or equal to zero");
			this.acknowledgementThreshold = acknowledgementThreshold;
			return this;
		}

		public Builder acknowledgementMode(AcknowledgementMode acknowledgementMode) {
			Assert.notNull(acknowledgementMode, "acknowledgementMode cannot be null");
			this.acknowledgementMode = acknowledgementMode;
			return this;
		}

		public Builder acknowledgementOrdering(AcknowledgementOrdering acknowledgementOrdering) {
			Assert.notNull(acknowledgementOrdering, "acknowledgementOrdering cannot be null");
			this.acknowledgementOrdering = acknowledgementOrdering;
			return this;
		}

		public Builder messageConverter(MessagingMessageConverter<?> messageConverter) {
			Assert.notNull(messageConverter, "messageConverter cannot be null");
			this.messageConverter = messageConverter;
			return this;
		}

		public Builder queueNotFoundStrategy(QueueNotFoundStrategy queueNotFoundStrategy) {
			Assert.notNull(queueNotFoundStrategy, "queueNotFoundStrategy cannot be null");
			this.queueNotFoundStrategy = queueNotFoundStrategy;
			return this;
		}

		public ContainerOptions build() {
			return new ContainerOptions(this);
		}

		public ContainerOptions.Builder createCopy() {
			ContainerOptions.Builder newCopy = ContainerOptions.builder();
			ReflectionUtils.shallowCopyFieldState(this, newCopy);
			return newCopy;
		}

		public void fromBuilder(Builder builder) {
			ReflectionUtils.shallowCopyFieldState(builder, this);
		}

	}

}
