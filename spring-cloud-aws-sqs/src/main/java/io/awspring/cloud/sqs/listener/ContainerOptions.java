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
import io.awspring.cloud.sqs.listener.backpressure.BackPressureHandler;
import io.awspring.cloud.sqs.listener.backpressure.BackPressureHandlerFactory;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.Collection;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.retry.backoff.BackOffPolicy;

/**
 * Contains the options to be used by the {@link MessageListenerContainer} at runtime. Note that after the object has
 * been built by the {@link ContainerOptionsBuilder} it's immutable and thread-safe. If a new
 * {@link ContainerOptionsBuilder} is created from this object, any changes on it won't reflect on the original
 * instance. Also note that any copies are shallow, meaning that complex objects are not copied but shared between the
 * original instance and the copy.
 *
 * @author Tomaz Fernandes
 * @author Loïc Rouchon
 * @since 3.0
 */
public interface ContainerOptions<O extends ContainerOptions<O, B>, B extends ContainerOptionsBuilder<B, O>> {

	/**
	 * Set the maximum concurrent messages that can be processed simultaneously for each queue. Note that if
	 * acknowledgement batching is being used, the actual maximum number of messages inflight might be higher. Default
	 * is 10.
	 *
	 * @return the maximum concurrent messages.
	 */
	int getMaxConcurrentMessages();

	/**
	 * Return the number of messages that should be returned per poll.
	 * @return the maximum number of messages per poll.
	 */
	int getMaxMessagesPerPoll();

	/**
	 * Checks whether the container should be started automatically or manually. Default is true.
	 *
	 * @return true if the container starts automatically, false if it should be started manually
	 */
	boolean isAutoStartup();

	/**
	 * Sets the maximum time the polling thread should wait for a full batch of permits to be available before trying to
	 * acquire a partial batch if so configured. A poll is only actually executed if at least one permit is available.
	 * Default is 10 seconds.
	 *
	 * @return the maximum delay between polls.
	 * @see BackPressureMode
	 */
	Duration getMaxDelayBetweenPolls();

	/**
	 * Return the timeout for polling messages for this endpoint.
	 * @return the timeout duration.
	 */
	Duration getPollTimeout();

	/**
	 * Return the {@link BackOffPolicy} to be applied when polling throws an exception.
	 * @return the timeout duration.
	 * @since 3.2
	 */
	default BackOffPolicy getPollBackOffPolicy() {
		throw new UnsupportedOperationException("Poll Back Off not supported by this ContainerOptions");
	}

	/**
	 * Return the {@link TaskExecutor} to be used by this container's components. It's shared by the
	 * {@link io.awspring.cloud.sqs.listener.sink.MessageSink} and any blocking components the container might have. Due
	 * to performance concerns, the provided executor MUST have a
	 * {@link io.awspring.cloud.sqs.MessageExecutionThreadFactory}. The container should have enough Threads to support
	 * the full load, including if it's shared between containers.
	 * @return the task executor.
	 */
	@Nullable
	TaskExecutor getComponentsTaskExecutor();

	/**
	 * Return the {@link TaskExecutor} to be used by blocking
	 * {@link io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback} implementations for this
	 * container. Due to performance concerns, the provided executor MUST have a
	 * {@link io.awspring.cloud.sqs.MessageExecutionThreadFactory}. The container should have enough Threads to support
	 * the full load, including if it's shared between containers.
	 * @return the task executor.
	 */
	@Nullable
	TaskExecutor getAcknowledgementResultTaskExecutor();

	/**
	 * Return the maximum amount of time that the container should wait for processing tasks to finish before shutting
	 * down. Note that when acknowledgement batching is used, the container will also wait for
	 * {@link #getAcknowledgementShutdownTimeout()}.
	 * @return the timeout.
	 */
	Duration getListenerShutdownTimeout();

	/**
	 * Return the maximum amount of time that the container should wait for batched acknowledgements to finish before
	 * shutting down. This timeout starts counting after listener processing is finished, including due to
	 * {@link #getListenerShutdownTimeout()}.
	 * @return the timeout.
	 */
	Duration getAcknowledgementShutdownTimeout();

	/**
	 * Return the {@link BackPressureMode} for this container.
	 * @return the backpressure mode.
	 */
	BackPressureMode getBackPressureMode();

	/**
	 * Return the {@link BackPressureHandlerFactory} to create a {@link BackPressureHandler} for this container.
	 * @return the BackPressureHandlerFactory.
	 */
	BackPressureHandlerFactory getBackPressureHandlerFactory();

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
	 * Return the {@link ObservationRegistry} to use in this container.
	 * @return the observation Registry.
	 */
	ObservationRegistry getObservationRegistry();

	/**
	 * Return a custom {@link ObservationConvention} to use with this container.
	 * @return the observation convention.
	 */
	ObservationConvention<?> getObservationConvention();

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
		return toBuilder().build();
	}

	/**
	 * Creates a new {@link ContainerOptionsBuilder ) instance configured with this options. Note that any changes made
	 * to the builder will have no effect on this object.
	 * @return the new builder instance.
	 */
	B toBuilder();

}
