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
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Base implementation for {@link ContainerOptions}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractContainerOptions<O extends ContainerOptions<O, B>, B extends ContainerOptions.Builder<B, O>>
		implements ContainerOptions<O, B> {

	private final int maxInflightMessagesPerQueue;

	private final int maxMessagesPerPoll;

	private final Duration pollTimeout;

	private final Duration permitAcquireTimeout;

	private final Duration shutdownTimeout;

	private final BackPressureMode backPressureMode;

	private final ListenerMode listenerMode;

	private final MessagingMessageConverter<?> messageConverter;

	private final AcknowledgementMode acknowledgementMode;

	@Nullable
	private final AcknowledgementOrdering acknowledgementOrdering;

	@Nullable
	private final Duration acknowledgementInterval;

	@Nullable
	private final Integer acknowledgementThreshold;

	@Nullable
	private final TaskExecutor componentsTaskExecutor;

	@Nullable
	private final TaskExecutor acknowledgementResultTaskExecutor;

	protected AbstractContainerOptions(Builder<?, ?> builder) {
		this.maxInflightMessagesPerQueue = builder.maxInflightMessagesPerQueue;
		this.maxMessagesPerPoll = builder.maxMessagesPerPoll;
		this.pollTimeout = builder.pollTimeout;
		this.permitAcquireTimeout = builder.permitAcquireTimeout;
		this.shutdownTimeout = builder.shutdownTimeout;
		this.backPressureMode = builder.backPressureMode;
		this.listenerMode = builder.listenerMode;
		this.messageConverter = builder.messageConverter;
		this.acknowledgementMode = builder.acknowledgementMode;
		this.acknowledgementOrdering = builder.acknowledgementOrdering;
		this.acknowledgementInterval = builder.acknowledgementInterval;
		this.acknowledgementThreshold = builder.acknowledgementThreshold;
		this.componentsTaskExecutor = builder.componentsTaskExecutor;
		this.acknowledgementResultTaskExecutor = builder.acknowledgementResultTaskExecutor;
		Assert.isTrue(this.maxMessagesPerPoll <= this.maxInflightMessagesPerQueue, String.format(
				"messagesPerPoll should be less than or equal to maxInflightMessagesPerQueue. Values provided: %s and %s respectively",
				this.maxMessagesPerPoll, this.maxInflightMessagesPerQueue));
	}

	@Override
	public int getMaxInFlightMessagesPerQueue() {
		return this.maxInflightMessagesPerQueue;
	}

	@Override
	public int getMaxMessagesPerPoll() {
		return this.maxMessagesPerPoll;
	}

	@Override
	public Duration getPollTimeout() {
		return this.pollTimeout;
	}

	@Override
	public Duration getPermitAcquireTimeout() {
		return this.permitAcquireTimeout;
	}

	@Nullable
	@Override
	public TaskExecutor getComponentsTaskExecutor() {
		return this.componentsTaskExecutor;
	}

	@Nullable
	@Override
	public TaskExecutor getAcknowledgementResultTaskExecutor() {
		return this.acknowledgementResultTaskExecutor;
	}

	@Override
	public Duration getShutdownTimeout() {
		return this.shutdownTimeout;
	}

	@Override
	public BackPressureMode getBackPressureMode() {
		return this.backPressureMode;
	}

	@Override
	public ListenerMode getListenerMode() {
		return this.listenerMode;
	}

	@Override
	public MessagingMessageConverter<?> getMessageConverter() {
		return this.messageConverter;
	}

	@Nullable
	@Override
	public Duration getAcknowledgementInterval() {
		return this.acknowledgementInterval;
	}

	@Nullable
	@Override
	public Integer getAcknowledgementThreshold() {
		return this.acknowledgementThreshold;
	}

	@Override
	public AcknowledgementMode getAcknowledgementMode() {
		return this.acknowledgementMode;
	}

	@Nullable
	@Override
	public AcknowledgementOrdering getAcknowledgementOrdering() {
		return this.acknowledgementOrdering;
	}

	protected abstract static class Builder<B extends ContainerOptions.Builder<B, O>, O extends ContainerOptions<O, B>>
			implements ContainerOptions.Builder<B, O> {

		private static final int DEFAULT_MAX_INFLIGHT_MSG_PER_QUEUE = 10;

		private static final int DEFAULT_MAX_MESSAGES_PER_POLL = 10;

		private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofSeconds(10);

		private static final Duration DEFAULT_SEMAPHORE_TIMEOUT = Duration.ofSeconds(10);

		private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(20);

		private static final BackPressureMode DEFAULT_THROUGHPUT_CONFIGURATION = BackPressureMode.AUTO;

		private static final ListenerMode DEFAULT_MESSAGE_DELIVERY_STRATEGY = ListenerMode.SINGLE_MESSAGE;

		private static final MessagingMessageConverter<?> DEFAULT_MESSAGE_CONVERTER = new SqsMessagingMessageConverter();

		private static final AcknowledgementMode DEFAULT_ACKNOWLEDGEMENT_MODE = AcknowledgementMode.ON_SUCCESS;

		private int maxInflightMessagesPerQueue = DEFAULT_MAX_INFLIGHT_MSG_PER_QUEUE;

		private int maxMessagesPerPoll = DEFAULT_MAX_MESSAGES_PER_POLL;

		private Duration pollTimeout = DEFAULT_POLL_TIMEOUT;

		private Duration permitAcquireTimeout = DEFAULT_SEMAPHORE_TIMEOUT;

		private BackPressureMode backPressureMode = DEFAULT_THROUGHPUT_CONFIGURATION;

		private Duration shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;

		private ListenerMode listenerMode = DEFAULT_MESSAGE_DELIVERY_STRATEGY;

		private MessagingMessageConverter<?> messageConverter = DEFAULT_MESSAGE_CONVERTER;

		private AcknowledgementMode acknowledgementMode = DEFAULT_ACKNOWLEDGEMENT_MODE;

		@Nullable
		private AcknowledgementOrdering acknowledgementOrdering;

		@Nullable
		private Duration acknowledgementInterval;

		@Nullable
		private Integer acknowledgementThreshold;

		@Nullable
		private TaskExecutor componentsTaskExecutor;

		@Nullable
		private TaskExecutor acknowledgementResultTaskExecutor;

		protected Builder() {
		}

		protected Builder(AbstractContainerOptions<?, ?> options) {
			this.maxInflightMessagesPerQueue = options.maxInflightMessagesPerQueue;
			this.maxMessagesPerPoll = options.maxMessagesPerPoll;
			this.pollTimeout = options.pollTimeout;
			this.permitAcquireTimeout = options.permitAcquireTimeout;
			this.shutdownTimeout = options.shutdownTimeout;
			this.backPressureMode = options.backPressureMode;
			this.listenerMode = options.listenerMode;
			this.messageConverter = options.messageConverter;
			this.acknowledgementMode = options.acknowledgementMode;
			this.acknowledgementOrdering = options.acknowledgementOrdering;
			this.acknowledgementInterval = options.acknowledgementInterval;
			this.acknowledgementThreshold = options.acknowledgementThreshold;
			this.componentsTaskExecutor = options.componentsTaskExecutor;
			this.acknowledgementResultTaskExecutor = options.acknowledgementResultTaskExecutor;
		}

		@Override
		public B maxInflightMessagesPerQueue(int maxInflightMessagesPerQueue) {
			Assert.isTrue(maxInflightMessagesPerQueue > 0, "maxInflightMessagesPerQueue must be greater than zero");
			this.maxInflightMessagesPerQueue = maxInflightMessagesPerQueue;
			return self();
		}

		@Override
		public B maxMessagesPerPoll(int maxMessagesPerPoll) {
			this.maxMessagesPerPoll = maxMessagesPerPoll;
			return self();
		}

		@Override
		public B pollTimeout(Duration pollTimeout) {
			Assert.notNull(pollTimeout, "pollTimeout cannot be null");
			this.pollTimeout = pollTimeout;
			return self();
		}

		@Override
		public B permitAcquireTimeout(Duration permitAcquireTimeout) {
			Assert.notNull(permitAcquireTimeout, "semaphoreAcquireTimeout cannot be null");
			this.permitAcquireTimeout = permitAcquireTimeout;
			return self();
		}

		@Override
		public B listenerMode(ListenerMode listenerMode) {
			Assert.notNull(listenerMode, "listenerMode cannot be null");
			this.listenerMode = listenerMode;
			return self();
		}

		@Override
		public B componentsTaskExecutor(TaskExecutor taskExecutor) {
			Assert.notNull(taskExecutor, "taskExecutor cannot be null");
			this.componentsTaskExecutor = taskExecutor;
			return self();
		}

		@Override
		public B acknowledgementResultTaskExecutor(TaskExecutor taskExecutor) {
			Assert.notNull(taskExecutor, "taskExecutor cannot be null");
			this.acknowledgementResultTaskExecutor = taskExecutor;
			return self();
		}

		@Override
		public B shutdownTimeout(Duration shutdownTimeout) {
			Assert.notNull(shutdownTimeout, "shutdownTimeout cannot be null");
			this.shutdownTimeout = shutdownTimeout;
			return self();
		}

		@Override
		public B backPressureMode(BackPressureMode backPressureMode) {
			Assert.notNull(backPressureMode, "backPressureMode cannot be null");
			this.backPressureMode = backPressureMode;
			return self();
		}

		@Override
		public B acknowledgementInterval(Duration acknowledgementInterval) {
			Assert.notNull(acknowledgementInterval, "acknowledgementInterval cannot be null");
			this.acknowledgementInterval = acknowledgementInterval;
			return self();
		}

		@Override
		public B acknowledgementThreshold(int acknowledgementThreshold) {
			Assert.isTrue(acknowledgementThreshold >= 0,
					"acknowledgementThreshold must be greater than or equal to zero");
			this.acknowledgementThreshold = acknowledgementThreshold;
			return self();
		}

		@Override
		public B acknowledgementMode(AcknowledgementMode acknowledgementMode) {
			Assert.notNull(acknowledgementMode, "acknowledgementMode cannot be null");
			this.acknowledgementMode = acknowledgementMode;
			return self();
		}

		@Override
		public B acknowledgementOrdering(AcknowledgementOrdering acknowledgementOrdering) {
			Assert.notNull(acknowledgementOrdering, "acknowledgementOrdering cannot be null");
			this.acknowledgementOrdering = acknowledgementOrdering;
			return self();
		}

		@Override
		public B messageConverter(MessagingMessageConverter<?> messageConverter) {
			Assert.notNull(messageConverter, "messageConverter cannot be null");
			this.messageConverter = messageConverter;
			return self();
		}

		@SuppressWarnings("unchecked")
		private B self() {
			return (B) this;
		}

	}

}
