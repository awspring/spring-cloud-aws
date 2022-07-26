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

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import io.awspring.cloud.sqs.BackPressureMode;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Contains the options to be used by the {@link MessageListenerContainer} at runtime.
 * If changes are made after the container has started, those changes will be reflected upon
 * container restart.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class ContainerOptions {

	private static final int DEFAULT_MAX_INFLIGHT_MSG_PER_QUEUE = 10;

	private static final int DEFAULT_MESSAGES_PER_POLL = 10;

	private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofSeconds(10);

	private static final Duration DEFAULT_SEMAPHORE_TIMEOUT = Duration.ofSeconds(10);

	private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(20);

	private static final BackPressureMode DEFAULT_THROUGHPUT_CONFIGURATION = BackPressureMode.AUTO;

	private static final MessageDeliveryStrategy DEFAULT_MESSAGE_DELIVERY_STRATEGY = MessageDeliveryStrategy.SINGLE_MESSAGE;

	private int maxInflightMessagesPerQueue = DEFAULT_MAX_INFLIGHT_MSG_PER_QUEUE;

	private int messagesPerPoll = DEFAULT_MESSAGES_PER_POLL;

	private Duration pollTimeout = DEFAULT_POLL_TIMEOUT;

	private Duration permitAcquireTimeout = DEFAULT_SEMAPHORE_TIMEOUT;

	private Duration shutDownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;

	private TaskExecutor sinkTaskExecutor;

	private BackPressureMode backPressureMode = DEFAULT_THROUGHPUT_CONFIGURATION;

	private MessageDeliveryStrategy messageDeliveryStrategy = DEFAULT_MESSAGE_DELIVERY_STRATEGY;

	private Collection<QueueAttributeName> queueAttributeNames = Collections.singletonList(QueueAttributeName.ALL);
	private Duration messageVisibility;

	public static ContainerOptions create() {
		return new ContainerOptions();
	}

	/**
	 * Set the maximum allowed number of inflight messages for each queue.
	 * @return this instance.
	 */
	public ContainerOptions maxInflightMessagesPerQueue(int maxInflightMessagesPerQueue) {
		this.maxInflightMessagesPerQueue = maxInflightMessagesPerQueue;
		return this;
	}

	/**
	 * Set the maximum time the polling thread should wait for permits.
	 * @param permitAcquireTimeout the timeout.
	 * @return this instance.
	 */
	public ContainerOptions permitAcquireTimeout(Duration permitAcquireTimeout) {
		Assert.notNull(permitAcquireTimeout, "semaphoreAcquireTimeout cannot be null");
		this.permitAcquireTimeout = permitAcquireTimeout;
		return this;
	}

	/**
	 * Set the number of messages that should be returned per poll.
	 * @param messagesPerPoll the number of messages.
	 * @return this instance.
	 */
	public ContainerOptions messagesPerPoll(int messagesPerPoll) {
		this.messagesPerPoll = messagesPerPoll;
		return this;
	}

	/**
	 * Set the timeout for polling messages for this endpoint.
	 * @param pollTimeout the poll timeout.
	 * @return this instance.
	 */
	public ContainerOptions pollTimeout(Duration pollTimeout) {
		Assert.notNull(pollTimeout, "pollTimeout cannot be null");
		this.pollTimeout = pollTimeout;
		return this;
	}

	public ContainerOptions messageDeliveryStrategy(MessageDeliveryStrategy messageDeliveryStrategy) {
		Assert.notNull(messageDeliveryStrategy, "messageDeliveryStrategy cannot be null");
		this.messageDeliveryStrategy = messageDeliveryStrategy;
		return this;
	}

	public ContainerOptions sinkTaskExecutor(TaskExecutor sinkTaskExecutor) {
		Assert.notNull(sinkTaskExecutor, "sinkTaskExecutor cannot be null");
		this.sinkTaskExecutor = sinkTaskExecutor;
		return this;
	}

	public ContainerOptions shutDownTimeout(Duration shutDownTimeout) {
		this.shutDownTimeout = shutDownTimeout;
		return this;
	}

	public ContainerOptions backPressureMode(BackPressureMode backPressureMode) {
		this.backPressureMode = backPressureMode;
		return this;
	}

	public ContainerOptions queueAttributes(Collection<QueueAttributeName> queueAttributeNames) {
		this.queueAttributeNames = queueAttributeNames;
		return this;
	}

	public ContainerOptions messageVisibility(Duration messageVisibility) {
		this.messageVisibility = messageVisibility;
		return this;
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
	public int getMessagesPerPoll() {
		return this.messagesPerPoll;
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

	public TaskExecutor getSinkTaskExecutor() {
		return this.sinkTaskExecutor;
	}

	public Duration getShutDownTimeout() {
		return this.shutDownTimeout;
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

	public Duration getMessageVisibility() {
		return this.messageVisibility;
	}

	/**
	 * Create a shallow copy of these options.
	 * @return the copy.
	 */
	public ContainerOptions createCopy() {
		ContainerOptions newCopy = new ContainerOptions();
		ReflectionUtils.shallowCopyFieldState(this, newCopy);
		return newCopy;
	}

	public ContainerOptions configure(ConfigurableContainerComponent configurable) {
		configurable.configure(createCopy());
		return this;
	}

	public ContainerOptions configure(Collection<? extends ConfigurableContainerComponent> configurables) {
		configurables.forEach(this::configure);
		return this;
	}

	/**
	 * Validate these options.
	 */
	public void validate() {
		Assert.isTrue(this.messagesPerPoll <= maxInflightMessagesPerQueue,
			String.format("messagesPerPoll should be less than or equal to maxInflightMessagesPerQueue. Values provided: %s and %s respectively",
				this.messagesPerPoll, this.maxInflightMessagesPerQueue));
		Assert.isTrue(this.messagesPerPoll <= 10, "messagesPerPoll must be less than or equal to 10.");
	}

}
