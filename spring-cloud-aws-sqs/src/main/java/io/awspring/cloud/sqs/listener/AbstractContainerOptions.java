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
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.BackOffPolicyBuilder;
import org.springframework.util.Assert;

/**
 * Base implementation for {@link ContainerOptions}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractContainerOptions<O extends ContainerOptions<O, B>, B extends ContainerOptionsBuilder<B, O>>
		implements ContainerOptions<O, B> {

	private final int maxConcurrentMessages;

	private final int maxMessagesPerPoll;

	private final boolean autoStartup;

	private final Duration pollTimeout;

	private final BackOffPolicy pollBackOffPolicy;

	private final Duration maxDelayBetweenPolls;

	private final Duration listenerShutdownTimeout;

	private final Duration acknowledgementShutdownTimeout;

	private final BackPressureMode backPressureMode;

	private final ListenerMode listenerMode;

	private final MessagingMessageConverter<?> messageConverter;

	private final AcknowledgementMode acknowledgementMode;

	private final ObservationRegistry observationRegistry;

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

	private final ObservationConvention<?> observationConvention;

	protected AbstractContainerOptions(Builder<?, ?> builder) {
		this.maxConcurrentMessages = builder.maxConcurrentMessages;
		this.maxMessagesPerPoll = builder.maxMessagesPerPoll;
		this.autoStartup = builder.autoStartup;
		this.pollTimeout = builder.pollTimeout;
		this.pollBackOffPolicy = builder.pollBackOffPolicy;
		this.maxDelayBetweenPolls = builder.maxDelayBetweenPolls;
		this.listenerShutdownTimeout = builder.listenerShutdownTimeout;
		this.acknowledgementShutdownTimeout = builder.acknowledgementShutdownTimeout;
		this.backPressureMode = builder.backPressureMode;
		this.listenerMode = builder.listenerMode;
		this.messageConverter = builder.messageConverter;
		this.acknowledgementMode = builder.acknowledgementMode;
		this.acknowledgementOrdering = builder.acknowledgementOrdering;
		this.acknowledgementInterval = builder.acknowledgementInterval;
		this.acknowledgementThreshold = builder.acknowledgementThreshold;
		this.componentsTaskExecutor = builder.componentsTaskExecutor;
		this.acknowledgementResultTaskExecutor = builder.acknowledgementResultTaskExecutor;
		this.observationRegistry = builder.observationRegistry;
		this.observationConvention = builder.observationConvention;
		Assert.isTrue(this.maxMessagesPerPoll <= this.maxConcurrentMessages, String.format(
				"messagesPerPoll should be less than or equal to maxConcurrentMessages. Values provided: %s and %s respectively",
				this.maxMessagesPerPoll, this.maxConcurrentMessages));
	}

	@Override
	public int getMaxConcurrentMessages() {
		return this.maxConcurrentMessages;
	}

	@Override
	public int getMaxMessagesPerPoll() {
		return this.maxMessagesPerPoll;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public Duration getPollTimeout() {
		return this.pollTimeout;
	}

	@Override
	public BackOffPolicy getPollBackOffPolicy() {
		return this.pollBackOffPolicy;
	}

	@Override
	public Duration getMaxDelayBetweenPolls() {
		return this.maxDelayBetweenPolls;
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
	public Duration getListenerShutdownTimeout() {
		return this.listenerShutdownTimeout;
	}

	@Override
	public Duration getAcknowledgementShutdownTimeout() {
		return this.acknowledgementShutdownTimeout;
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

	@Override
	public ObservationRegistry getObservationRegistry() {
		return this.observationRegistry;
	}

	@Override
	public ObservationConvention<?> getObservationConvention() {
		return this.observationConvention;
	}

	protected abstract static class Builder<B extends ContainerOptionsBuilder<B, O>, O extends ContainerOptions<O, B>>
			implements ContainerOptionsBuilder<B, O> {

		private static final int DEFAULT_MAX_INFLIGHT_MSG_PER_QUEUE = 10;

		private static final int DEFAULT_MAX_MESSAGES_PER_POLL = 10;

		private static final boolean DEFAULT_AUTO_STARTUP = true;

		private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofSeconds(10);

		private static final double DEFAULT_BACK_OFF_MULTIPLIER = 2.0;

		private static final int DEFAULT_BACK_OFF_DELAY = 1000;

		private static final int DEFAULT_BACK_OFF_MAX_DELAY = 10000;

		private static final BackOffPolicy DEFAULT_POLL_BACK_OFF_POLICY = buildDefaultBackOffPolicy();

		private static final Duration DEFAULT_SEMAPHORE_TIMEOUT = Duration.ofSeconds(10);

		private static final Duration DEFAULT_LISTENER_SHUTDOWN_TIMEOUT = Duration.ofSeconds(20);

		private static final Duration DEFAULT_ACKNOWLEDGEMENT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(20);

		private static final BackPressureMode DEFAULT_THROUGHPUT_CONFIGURATION = BackPressureMode.AUTO;

		private static final ListenerMode DEFAULT_MESSAGE_DELIVERY_STRATEGY = ListenerMode.SINGLE_MESSAGE;

		private static final MessagingMessageConverter<?> DEFAULT_MESSAGE_CONVERTER = new SqsMessagingMessageConverter();

		private static final AcknowledgementMode DEFAULT_ACKNOWLEDGEMENT_MODE = AcknowledgementMode.ON_SUCCESS;

		private static final ObservationRegistry DEFAULT_OBSERVATION_REGISTRY = ObservationRegistry.NOOP;

		private int maxConcurrentMessages = DEFAULT_MAX_INFLIGHT_MSG_PER_QUEUE;

		private int maxMessagesPerPoll = DEFAULT_MAX_MESSAGES_PER_POLL;

		private boolean autoStartup = DEFAULT_AUTO_STARTUP;

		private Duration pollTimeout = DEFAULT_POLL_TIMEOUT;

		private BackOffPolicy pollBackOffPolicy = DEFAULT_POLL_BACK_OFF_POLICY;

		private Duration maxDelayBetweenPolls = DEFAULT_SEMAPHORE_TIMEOUT;

		private BackPressureMode backPressureMode = DEFAULT_THROUGHPUT_CONFIGURATION;

		private Duration listenerShutdownTimeout = DEFAULT_LISTENER_SHUTDOWN_TIMEOUT;

		private Duration acknowledgementShutdownTimeout = DEFAULT_ACKNOWLEDGEMENT_SHUTDOWN_TIMEOUT;

		private ListenerMode listenerMode = DEFAULT_MESSAGE_DELIVERY_STRATEGY;

		private MessagingMessageConverter<?> messageConverter = DEFAULT_MESSAGE_CONVERTER;

		private AcknowledgementMode acknowledgementMode = DEFAULT_ACKNOWLEDGEMENT_MODE;

		private ObservationRegistry observationRegistry = DEFAULT_OBSERVATION_REGISTRY;

		private ObservationConvention<?> observationConvention;

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
			this.maxConcurrentMessages = options.maxConcurrentMessages;
			this.maxMessagesPerPoll = options.maxMessagesPerPoll;
			this.autoStartup = options.autoStartup;
			this.pollTimeout = options.pollTimeout;
			this.pollBackOffPolicy = options.pollBackOffPolicy;
			this.maxDelayBetweenPolls = options.maxDelayBetweenPolls;
			this.listenerShutdownTimeout = options.listenerShutdownTimeout;
			this.acknowledgementShutdownTimeout = options.acknowledgementShutdownTimeout;
			this.backPressureMode = options.backPressureMode;
			this.listenerMode = options.listenerMode;
			this.messageConverter = options.messageConverter;
			this.acknowledgementMode = options.acknowledgementMode;
			this.acknowledgementOrdering = options.acknowledgementOrdering;
			this.acknowledgementInterval = options.acknowledgementInterval;
			this.acknowledgementThreshold = options.acknowledgementThreshold;
			this.componentsTaskExecutor = options.componentsTaskExecutor;
			this.acknowledgementResultTaskExecutor = options.acknowledgementResultTaskExecutor;
			this.observationRegistry = options.observationRegistry;
			this.observationConvention = options.observationConvention;
		}

		@Override
		public B maxConcurrentMessages(int maxConcurrentMessages) {
			Assert.isTrue(maxConcurrentMessages > 0, "maxConcurrentMessages must be greater than zero");
			this.maxConcurrentMessages = maxConcurrentMessages;
			return self();
		}

		@Override
		public B maxMessagesPerPoll(int maxMessagesPerPoll) {
			this.maxMessagesPerPoll = maxMessagesPerPoll;
			return self();
		}

		@Override
		public B autoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
			return self();
		}

		@Override
		public B pollTimeout(Duration pollTimeout) {
			Assert.notNull(pollTimeout, "pollTimeout cannot be null");
			this.pollTimeout = pollTimeout;
			return self();
		}

		@Override
		public B pollBackOffPolicy(BackOffPolicy pollBackOffPolicy) {
			Assert.notNull(pollBackOffPolicy, "pollBackOffPolicy cannot be null");
			this.pollBackOffPolicy = pollBackOffPolicy;
			return self();
		}

		@Override
		public B maxDelayBetweenPolls(Duration maxDelayBetweenPolls) {
			Assert.notNull(maxDelayBetweenPolls, "semaphoreAcquireTimeout cannot be null");
			this.maxDelayBetweenPolls = maxDelayBetweenPolls;
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
		public B listenerShutdownTimeout(Duration listenerShutdownTimeout) {
			Assert.notNull(listenerShutdownTimeout, "listenerShutdownTimeout cannot be null");
			this.listenerShutdownTimeout = listenerShutdownTimeout;
			return self();
		}

		@Override
		public B acknowledgementShutdownTimeout(Duration acknowledgementShutdownTimeout) {
			Assert.notNull(acknowledgementShutdownTimeout, "acknowledgementShutdownTimeout cannot be null");
			this.acknowledgementShutdownTimeout = acknowledgementShutdownTimeout;
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

		@Override
		public B observationRegistry(ObservationRegistry observationRegistry) {
			Assert.notNull(observationRegistry, "observationRegistry cannot be null");
			this.observationRegistry = observationRegistry;
			return self();
		}

		protected B observationConvention(ObservationConvention<?> observationConvention) {
			Assert.notNull(observationConvention, "observationConvention cannot be null");
			this.observationConvention = observationConvention;
			return self();
		}

		@SuppressWarnings("unchecked")
		private B self() {
			return (B) this;
		}

		private static BackOffPolicy buildDefaultBackOffPolicy() {
			return BackOffPolicyBuilder.newBuilder().multiplier(DEFAULT_BACK_OFF_MULTIPLIER)
					.delay(DEFAULT_BACK_OFF_DELAY).maxDelay(DEFAULT_BACK_OFF_MAX_DELAY).build();
		}
	}

}
