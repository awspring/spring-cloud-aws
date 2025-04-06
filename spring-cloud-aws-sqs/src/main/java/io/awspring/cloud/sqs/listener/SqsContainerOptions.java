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

import io.awspring.cloud.sqs.support.observation.SqsListenerObservation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Sqs specific implementation of {@link ContainerOptions}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsContainerOptions extends AbstractContainerOptions<SqsContainerOptions, SqsContainerOptionsBuilder> {

	@Nullable
	private final Duration messageVisibility;

	private final FifoBatchGroupingStrategy fifoBatchGroupingStrategy;

	private final Collection<QueueAttributeName> queueAttributeNames;

	private final Collection<String> messageAttributeNames;

	private final Collection<String> messageSystemAttributeNames;

	private final QueueNotFoundStrategy queueNotFoundStrategy;

	/**
	 * Create a {@link ContainerOptions} instance from the builder.
	 * @param builder the builder.
	 */
	private SqsContainerOptions(BuilderImpl builder) {
		super(builder);
		this.queueAttributeNames = builder.queueAttributeNames;
		this.messageAttributeNames = builder.messageAttributeNames;
		this.messageSystemAttributeNames = builder.messageSystemAttributeNames;
		this.messageVisibility = builder.messageVisibility;
		this.queueNotFoundStrategy = builder.queueNotFoundStrategy;
		this.fifoBatchGroupingStrategy = builder.fifoBatchGroupingStrategy;
	}

	/**
	 * Create a new builder instance.
	 * @return the new builder instance.
	 */
	public static SqsContainerOptionsBuilder builder() {
		return new BuilderImpl();
	}

	/**
	 * Get the {@link QueueAttributeName}s that will be retrieved from the queue and added as headers to the messages.
	 * @return the names.
	 */
	public Collection<QueueAttributeName> getQueueAttributeNames() {
		return this.queueAttributeNames;
	}

	/**
	 * Get the messageAttributeNames that will be retrieved and added as headers in messages. Default is ALL.
	 * @return the names.
	 */
	public Collection<String> getMessageAttributeNames() {
		return this.messageAttributeNames;
	}

	/**
	 * Get the {@link MessageSystemAttributeName}s that will be retrieved and added as headers in messages.
	 * @return the names.
	 */
	public Collection<String> getMessageSystemAttributeNames() {
		return this.messageSystemAttributeNames;
	}

	/**
	 * Get the message visibility for messages retrieved by the container.
	 * @return the visibility.
	 */
	@Nullable
	public Duration getMessageVisibility() {
		return this.messageVisibility;
	}

	/**
	 * Get messages grouping strategy in FIFO queues when retrieved by the container in listener mode
	 * {@link ListenerMode#BATCH}.
	 * @return the fifo batch message grouping strategy.
	 */
	public FifoBatchGroupingStrategy getFifoBatchGroupingStrategy() {
		return this.fifoBatchGroupingStrategy;
	}

	/**
	 * Get the {@link QueueNotFoundStrategy} for the container.
	 * @return the strategy.
	 */
	public QueueNotFoundStrategy getQueueNotFoundStrategy() {
		return this.queueNotFoundStrategy;
	}

	@Override
	public SqsContainerOptionsBuilder toBuilder() {
		return new BuilderImpl(this);
	}

	protected static class BuilderImpl
			extends AbstractContainerOptions.Builder<SqsContainerOptionsBuilder, SqsContainerOptions>
			implements SqsContainerOptionsBuilder {

		private static final List<QueueAttributeName> DEFAULT_QUEUE_ATTRIBUTES_NAMES = Collections.emptyList();

		private static final List<String> DEFAULT_MESSAGE_ATTRIBUTES_NAMES = Collections
				.singletonList(QueueAttributeName.ALL.toString());

		private static final List<String> DEFAULT_MESSAGE_SYSTEM_ATTRIBUTES = Collections
				.singletonList(QueueAttributeName.ALL.toString());

		private static final QueueNotFoundStrategy DEFAULT_QUEUE_NOT_FOUND_STRATEGY = QueueNotFoundStrategy.CREATE;

		private Collection<QueueAttributeName> queueAttributeNames = DEFAULT_QUEUE_ATTRIBUTES_NAMES;

		private Collection<String> messageAttributeNames = DEFAULT_MESSAGE_ATTRIBUTES_NAMES;

		private Collection<String> messageSystemAttributeNames = DEFAULT_MESSAGE_SYSTEM_ATTRIBUTES;

		private QueueNotFoundStrategy queueNotFoundStrategy = DEFAULT_QUEUE_NOT_FOUND_STRATEGY;

		private FifoBatchGroupingStrategy fifoBatchGroupingStrategy = FifoBatchGroupingStrategy.PROCESS_MESSAGE_GROUPS_IN_PARALLEL_BATCHES;

		@Nullable
		private Duration messageVisibility;

		protected BuilderImpl() {
			super();
		}

		protected BuilderImpl(SqsContainerOptions options) {
			super(options);
			this.queueAttributeNames = options.queueAttributeNames;
			this.messageAttributeNames = options.messageAttributeNames;
			this.messageSystemAttributeNames = options.messageSystemAttributeNames;
			this.messageVisibility = options.messageVisibility;
			this.fifoBatchGroupingStrategy = options.fifoBatchGroupingStrategy;
			this.queueNotFoundStrategy = options.queueNotFoundStrategy;
		}

		@Override
		public SqsContainerOptionsBuilder queueAttributeNames(Collection<QueueAttributeName> queueAttributeNames) {
			Assert.notEmpty(queueAttributeNames, "queueAttributeNames cannot be empty");
			this.queueAttributeNames = Collections.unmodifiableCollection(new ArrayList<>(queueAttributeNames));
			return this;
		}

		@Override
		public SqsContainerOptionsBuilder messageAttributeNames(Collection<String> messageAttributeNames) {
			Assert.notEmpty(messageAttributeNames, "messageAttributeNames cannot be empty");
			this.messageAttributeNames = Collections.unmodifiableCollection(new ArrayList<>(messageAttributeNames));
			return this;
		}

		@Override
		public SqsContainerOptionsBuilder messageSystemAttributeNames(
				Collection<MessageSystemAttributeName> messageSystemAttributeNames) {
			Assert.notEmpty(messageSystemAttributeNames, "messageSystemAttributeNames cannot be empty");
			this.messageSystemAttributeNames = messageSystemAttributeNames.stream()
					.map(MessageSystemAttributeName::toString).collect(Collectors.toList());
			return this;
		}

		@Override
		public SqsContainerOptionsBuilder messageVisibility(Duration messageVisibility) {
			Assert.notNull(messageVisibility, "messageVisibility cannot be null");
			this.messageVisibility = messageVisibility;
			return this;
		}

		@Override
		public SqsContainerOptionsBuilder fifoBatchGroupingStrategy(
				FifoBatchGroupingStrategy fifoBatchGroupingStrategy) {
			Assert.notNull(fifoBatchGroupingStrategy, "fifoBatchGroupingStrategy cannot be null");
			this.fifoBatchGroupingStrategy = fifoBatchGroupingStrategy;
			return this;
		}

		@Override
		public SqsContainerOptionsBuilder queueNotFoundStrategy(QueueNotFoundStrategy queueNotFoundStrategy) {
			Assert.notNull(queueNotFoundStrategy, "queueNotFoundStrategy cannot be null");
			this.queueNotFoundStrategy = queueNotFoundStrategy;
			return this;
		}

		@Override
		public SqsContainerOptionsBuilder observationConvention(
				SqsListenerObservation.Convention observationConvention) {
			Assert.notNull(observationConvention, "observationConvention cannot be null");
			super.observationConvention(observationConvention);
			return this;
		}

		@Override
		public SqsContainerOptions build() {
			return new SqsContainerOptions(this);
		}

		@Override
		public SqsContainerOptionsBuilder createCopy() {
			BuilderImpl builder = new BuilderImpl();
			ReflectionUtils.shallowCopyFieldState(this, builder);
			return builder;
		}

		@Override
		public void fromBuilder(SqsContainerOptionsBuilder builder) {
			ReflectionUtils.shallowCopyFieldState(builder, this);
		}
	}

}
