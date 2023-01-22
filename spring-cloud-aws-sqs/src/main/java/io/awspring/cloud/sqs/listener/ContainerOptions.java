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
import java.time.Duration;
import java.util.Collection;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * Contains the options to be used by the {@link MessageListenerContainer} at runtime. Note that after the object has
 * been built by the {@link Builder} it's immutable and thread-safe. If a new {@link Builder} is created from this
 * object, any changes on it won't reflect on the original instance. Also note that any copies are shallow, meaning that
 * complex objects are not copied but shared between the original instance and the copy.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface ContainerOptions<O extends ContainerOptions<O, B>, B extends ContainerOptions.Builder<B, O>> {

	/**
	 * Return the maximum allowed number of inflight messages for each queue.
	 * @return the number.
	 */
	int getMaxInFlightMessagesPerQueue();

	/**
	 * Return the number of messages that should be returned per poll.
	 * @return the number.
	 */
	int getMaxMessagesPerPoll();

	/**
	 * Return the timeout for polling messages for this endpoint.
	 * @return the timeout duration.
	 */
	Duration getPollTimeout();

	/**
	 * Return the maximum time the polling thread should wait for permits.
	 * @return the timeout.
	 */
	Duration getPermitAcquireTimeout();

	/**
	 * Return the {@link TaskExecutor} to be used by this container's components. It's shared by the
	 * {@link io.awspring.cloud.sqs.listener.sink.MessageSink} and any blocking components the container might have. For
	 * custom executors, it's highly recommended to add a {@link io.awspring.cloud.sqs.MessageExecutionThreadFactory},
	 * as this will significantly decrease Thread hopping, which both increases performance and decreases the number of
	 * Threads the executor must support. Also, specially if the executor is to be shared between containers or multiple
	 * queues, make sure there's enough Thread capacity / queues to support the load otherwise some tasks might be
	 * rejected.
	 * @return the task executor.
	 */
	@Nullable
	TaskExecutor getComponentsTaskExecutor();

	/**
	 * Return the {@link TaskExecutor} to be used by blocking
	 * {@link io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback} implementations for this
	 * container.
	 * @return the task executor.
	 */
	@Nullable
	TaskExecutor getAcknowledgementResultTaskExecutor();

	/**
	 * Return the maximum amount of time that the container should wait for tasks to finish before shutting down.
	 * @return the timeout.
	 */
	Duration getShutdownTimeout();

	/**
	 * Return the {@link BackPressureMode} for this container.
	 * @return the backpressure mode.
	 */
	BackPressureMode getBackPressureMode();

	/**
	 * Return the {@link ListenerMode} mode for this container.
	 * @return the listener mode.
	 */
	ListenerMode getListenerMode();

	/**
	 * Return the {@link MessagingMessageConverter} for this container.
	 * @return the message converter.
	 */
	MessagingMessageConverter<?> getMessageConverter();

	/**
	 * Return the maximum interval between acknowledgements for batch acknowledgements.
	 * @return the interval.
	 */
	@Nullable
	Duration getAcknowledgementInterval();

	/**
	 * Return the threshold for triggering a batch acknowledgement.
	 * @return the threshold.
	 */
	@Nullable
	Integer getAcknowledgementThreshold();

	/**
	 * Return the {@link AcknowledgementMode} for this container.
	 * @return the acknowledgement mode.
	 */
	AcknowledgementMode getAcknowledgementMode();

	/**
	 * Return the {@link AcknowledgementOrdering} for this container.
	 * @return the acknowledgement ordering.
	 */
	@Nullable
	AcknowledgementOrdering getAcknowledgementOrdering();

	/**
	 * Configure a {@link ConfigurableContainerComponent} with this options. Internal use mostly.
	 * @param configurable the component to be configured.
	 * @return this instance.
	 */
	@SuppressWarnings("unchecked")
	default O configure(ConfigurableContainerComponent configurable) {
		configurable.configure(this);
		return (O) this;
	}

	/**
	 * Configure a collection of {@link ConfigurableContainerComponent} with this options. Internal use mostly.
	 * @param configurables the components to be configured.
	 * @return this instance.
	 */
	@SuppressWarnings("unchecked")
	default O configure(Collection<? extends ConfigurableContainerComponent> configurables) {
		configurables.forEach(this::configure);
		return (O) this;
	}

	/**
	 * Create a copy of this options. Note that this does not copy complex objects and those will be shared between the
	 * original and the copy.
	 * @return a copy of this instance;
	 */
	default O createCopy() {
		O newCopy = createInstance();
		ReflectionUtils.shallowCopyFieldState(this, newCopy);
		return newCopy;
	}

	/**
	 * Create a new instance of this container options object.
	 * @return the new instance.
	 */
	O createInstance();

	/**
	 * Creates a new {@link Builder) instance configured with this options. Note that any changes made to the builder
	 * will have no effect on this object.
	 * @return the new builder instance.
	 */
	B toBuilder();

	/**
	 * Builder to create an immutable instance of {@link ContainerOptions}.
	 * @param <B> the builder subclass type
	 * @param <O> the ContainerOptions subclass type
	 */
	interface Builder<B extends Builder<B, O>, O extends ContainerOptions<O, B>> {

		/**
		 * Set the maximum allowed number of inflight messages for each queue. Default is 10.
		 * @return this instance.
		 */
		B maxInflightMessagesPerQueue(int maxInflightMessagesPerQueue);

		/**
		 * Set the number of messages that should be returned per poll. If a value greater than 10 is provided, the
		 * result of multiple polls will be combined, which can be useful for
		 * {@link io.awspring.cloud.sqs.listener.ListenerMode#BATCH} Default is 10.
		 * @param maxMessagesPerPoll the number of messages.
		 * @return this instance.
		 */
		B maxMessagesPerPoll(int maxMessagesPerPoll);

		/**
		 * Set the timeout for polling messages for this endpoint. Default is 10 seconds.
		 * @param pollTimeout the poll timeout.
		 * @return this instance.
		 */
		B pollTimeout(Duration pollTimeout);

		/**
		 * Set the maximum time the polling thread should wait for permits. Default is 10 seconds.
		 * @param permitAcquireTimeout the timeout.
		 * @return this instance.
		 */
		B permitAcquireTimeout(Duration permitAcquireTimeout);

		/**
		 * Set the {@link ListenerMode} mode for this container. Default is {@link ListenerMode#SINGLE_MESSAGE}
		 * @param listenerMode the listener mode.
		 * @return this instance.
		 */
		B listenerMode(ListenerMode listenerMode);

		/**
		 * Set the {@link TaskExecutor} to be used by this container's components. It's shared by the
		 * {@link io.awspring.cloud.sqs.listener.sink.MessageSink} and any blocking components the container might have.
		 * For custom executors, it's highly recommended to add a
		 * {@link io.awspring.cloud.sqs.MessageExecutionThreadFactory}, as this will significantly decrease Thread
		 * hopping, which both increases performance and decreases the number of Threads the executor must support.
		 * Also, specially if the executor is to be shared between containers or multiple queues, make sure there's
		 * enough Thread capacity / queues to support the load otherwise some tasks might be rejected.
		 * @see ContainerOptions#getComponentsTaskExecutor()
		 * @param taskExecutor the task executor.
		 * @return this instance.
		 */
		B componentsTaskExecutor(TaskExecutor taskExecutor);

		/**
		 * Set the {@link TaskExecutor} to be used by blocking
		 * {@link io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback} implementations for this
		 * container.
		 * @param taskExecutor the task executor.
		 * @return this instance.
		 */
		B acknowledgementResultTaskExecutor(TaskExecutor taskExecutor);

		/**
		 * Set the maximum amount of time that the container should wait for tasks to finish before shutting down.
		 * Default is 10 seconds.
		 * @param shutdownTimeout the timeout.
		 * @return this instance.
		 */
		B shutdownTimeout(Duration shutdownTimeout);

		/**
		 * Set the {@link BackPressureMode} for this container. Default is {@link BackPressureMode#AUTO}
		 * @param backPressureMode the backpressure mode.
		 * @return this instance.
		 */
		B backPressureMode(BackPressureMode backPressureMode);

		/**
		 * Set the maximum interval between acknowledgements for batch acknowledgements. The default depends on the
		 * specific {@link ContainerComponentFactory} implementation.
		 * @param acknowledgementInterval the interval.
		 * @return this instance.
		 */
		B acknowledgementInterval(Duration acknowledgementInterval);

		/**
		 * Set the threshold for triggering a batch acknowledgement. The default depends on the specific
		 * {@link ContainerComponentFactory} implementation.
		 * @param acknowledgementThreshold the threshold.
		 * @return this instance.
		 */
		B acknowledgementThreshold(int acknowledgementThreshold);

		/**
		 * Set the {@link AcknowledgementMode} for this container. Default is {@link AcknowledgementMode#ON_SUCCESS}.
		 * @param acknowledgementMode the acknowledgement mode.
		 * @return this instance.
		 */
		B acknowledgementMode(AcknowledgementMode acknowledgementMode);

		/**
		 * Set the {@link AcknowledgementOrdering} for this container. Default is
		 * {@link AcknowledgementOrdering#PARALLEL}.
		 * @param acknowledgementOrdering the acknowledgement ordering.
		 * @return this instance
		 */
		B acknowledgementOrdering(AcknowledgementOrdering acknowledgementOrdering);

		/**
		 * Set the {@link MessagingMessageConverter} for this container.
		 * @param messageConverter the message converter.
		 * @return this instance.
		 */
		B messageConverter(MessagingMessageConverter<?> messageConverter);

		/**
		 * Create the {@link ContainerOptions} instance.
		 * @return the new instance.
		 */
		O build();

		/**
		 * Create a copy of this builder.
		 * @return the copy.
		 */
		B createCopy();

		/**
		 * Copy the given builder settings to this builder.
		 * @param builder the builder from which to copy settings from.
		 */
		void fromBuilder(B builder);

	}

}
