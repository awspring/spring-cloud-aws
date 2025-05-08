/*
 * Copyright 2013-2023 the original author or authors.
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
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.backoff.BackOffPolicy;

/**
 * A builder for creating a {@link ContainerOptions} instance.
 * @param <B> the concrete {@link ContainerOptionsBuilder} type.
 * @param <O> the concrete {@link ContainerOptions} type.
 */
public interface ContainerOptionsBuilder<B extends ContainerOptionsBuilder<B, O>, O extends ContainerOptions<O, B>> {

	/**
	 * Set the maximum concurrent messages that can be processed simultaneously for each queue. Default is 10. Note that
	 * if acknowledgement batching is being used, the actual maximum number of inflight messages might be higher.
	 *
	 * @return this instance.
	 */
	B maxConcurrentMessages(int maxConcurrentMessages);

	/**
	 * Set the number of messages that should be returned per poll. If a value greater than 10 is provided, the result
	 * of multiple polls will be combined, which can be useful for
	 * {@link io.awspring.cloud.sqs.listener.ListenerMode#BATCH} Default is 10.
	 *
	 * @param maxMessagesPerPoll the number of messages.
	 * @return this instance.
	 */
	B maxMessagesPerPoll(int maxMessagesPerPoll);

	/**
	 * Set whether the container should be started automatically or manually. By default, the container is set to start
	 * automatically.
	 *
	 * @param autoStartup true if the container is set to start automatically, false if it should be started manually
	 * @return this instance.
	 */
	B autoStartup(boolean autoStartup);

	/**
	 * Set the maximum time the polling thread should wait for a full batch of permits to be available before trying to
	 * acquire a partial batch if so configured. A poll is only actually executed if at least one permit is available.
	 * Default is 10 seconds.
	 *
	 * @param maxDelayBetweenPolls the maximum delay.
	 * @return this instance.
	 * @see BackPressureMode
	 */
	B maxDelayBetweenPolls(Duration maxDelayBetweenPolls);

	/**
	 * Set the timeout for polling messages for this endpoint. Default is 10 seconds.
	 *
	 * @param pollTimeout the poll timeout.
	 * @return this instance.
	 */
	B pollTimeout(Duration pollTimeout);

	/**
	 * Set the {@link BackOffPolicy} to use when polling throws an exception.
	 * @param pollBackOffPolicy the back off policy.
	 * @return this instance.
	 * @since 3.2
	 */
	default B pollBackOffPolicy(BackOffPolicy pollBackOffPolicy) {
		throw new UnsupportedOperationException("Poll back off not supported by this container options builder");
	}

	/**
	 * Set the {@link ListenerMode} mode for this container. Default is {@link ListenerMode#SINGLE_MESSAGE}
	 *
	 * @param listenerMode the listener mode.
	 * @return this instance.
	 */
	B listenerMode(ListenerMode listenerMode);

	/**
	 * Set the {@link TaskExecutor} to be used by this container's components. It's shared by the
	 * {@link io.awspring.cloud.sqs.listener.sink.MessageSink} and any blocking components the container might have. Due
	 * to performance concerns, the provided executor MUST have a
	 * {@link io.awspring.cloud.sqs.MessageExecutionThreadFactory}. The container should have enough Threads to support
	 * the full load, including if it's shared between containers.
	 *
	 * @param taskExecutor the task executor.
	 * @return this instance.
	 * @see ContainerOptions#getComponentsTaskExecutor()
	 */
	B componentsTaskExecutor(TaskExecutor taskExecutor);

	/**
	 * Set the {@link TaskExecutor} to be used by blocking
	 * {@link io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback} implementations for this
	 * container. Due to performance concerns, the provided executor MUST have a
	 * {@link io.awspring.cloud.sqs.MessageExecutionThreadFactory}. The container should have enough Threads to support
	 * the full load, including if it's shared between containers.
	 *
	 * @param taskExecutor the task executor.
	 * @return this instance.
	 */
	B acknowledgementResultTaskExecutor(TaskExecutor taskExecutor);

	/**
	 * Set the maximum amount of time that the container should wait for tasks to finish before shutting down. Default
	 * is 20 seconds.
	 *
	 * @param shutdownTimeout the timeout.
	 * @return this instance.
	 */
	B listenerShutdownTimeout(Duration shutdownTimeout);

	/**
	 * Set the maximum amount of time that the container should wait for batched acknowledgements to finish before
	 * shutting down. Note that this timeout starts counting after listener processing is done or timed out. Default is
	 * 20 seconds.
	 *
	 * @param acknowledgementShutdownTimeout the timeout.
	 * @return this instance.
	 */
	B acknowledgementShutdownTimeout(Duration acknowledgementShutdownTimeout);

	/**
	 * Set the {@link BackPressureMode} for this container. Default is {@link BackPressureMode#AUTO}
	 *
	 * @param backPressureMode the backpressure mode.
	 * @return this instance.
	 */
	B backPressureMode(BackPressureMode backPressureMode);

	/**
	 * Sets the {@link Supplier} of {@link BackPressureHandler} for this container. Default is {@code null} which
	 * results in a default {@link SemaphoreBackPressureHandler} to be instantiated. In case a supplier is provided, the
	 * {@link BackPressureHandler} will be instantiated by the supplier.
	 * <p>
	 * <strong>NOTE:</strong> <em>it is important for the supplier to always return a new instance as otherwise it might
	 * result in a BackPressureHandler internal resources (counters, semaphores, ...) to be shared by multiple
	 * containers which is very likely not the desired behavior.</em>
	 * <p>
	 * Spring Cloud AWS provides the following {@link BackPressureHandler} implementations:
	 * <ul>
	 * <li>{@link ConcurrencyLimiterBlockingBackPressureHandler}: Limits the maximum number of messages that can be
	 * processed concurrently by the application.</li>
	 * <li>{@link ThroughputBackPressureHandler}: Adapts the throughput dynamically between high and low modes in order
	 * to reduce SQS pull costs when few messages are coming in.</li>
	 * <li>{@link CompositeBackPressureHandler}: Allows combining multiple {@link BackPressureHandler} together and
	 * ensures they cooperate.</li>
	 * </ul>
	 * <p>
	 * Below are a few examples of how common use cases can be achieved. Keep in mind you can always create your own
	 * {@link BackPressureHandler} implementation and if needed combine it with the provided ones thanks to the
	 * {@link CompositeBackPressureHandler}.
	 *
	 * <h3>A BackPressureHandler limiting the max concurrency with high throughput</h3>
	 * 
	 * <pre>{@code
	 * containerOptionsBuilder.backPressureHandlerSupplier(() -> {
	 * 		return ConcurrencyLimiterBlockingBackPressureHandler.builder()
	 * 			.batchSize(batchSize)
	 * 			.totalPermits(maxConcurrentMessages)
	 * 			.acquireTimeout(acquireTimeout)
	 * 			.throughputConfiguration(BackPressureMode.FIXED_HIGH_THROUGHPUT)
	 * 			.build()
	 * }}</pre>
	 *
	 * <h3>A BackPressureHandler limiting the max concurrency with dynamic throughput</h3>
	 *
	 * <pre>{@code
	 * containerOptionsBuilder.backPressureHandlerSupplier(() -> {
	 * 		var concurrencyLimiterBlockingBackPressureHandler = ConcurrencyLimiterBlockingBackPressureHandler.builder()
	 * 			.batchSize(batchSize)
	 * 			.totalPermits(maxConcurrentMessages)
	 * 			.acquireTimeout(acquireTimeout)
	 * 			.throughputConfiguration(BackPressureMode.AUTO)
	 * 			.build()
	 * 		var throughputBackPressureHandler = ThroughputBackPressureHandler.builder()
	 * 			.batchSize(batchSize)
	 * 			.build();
	 * 		return new CompositeBackPressureHandler(List.of(
	 * 				concurrencyLimiterBlockingBackPressureHandler,
	 * 				throughputBackPressureHandler
	 * 			),
	 * 			batchSize,
	 * 			standbyLimitPollingInterval
	 * 		);
	 * }}</pre>
	 *
	 * @param backPressureHandlerSupplier the BackPressureHandler supplier.
	 * @return this instance.
	 */
	B backPressureHandlerSupplier(Supplier<BackPressureHandler> backPressureHandlerSupplier);

	/**
	 * Set the maximum interval between acknowledgements for batch acknowledgements. The default depends on the specific
	 * {@link ContainerComponentFactory} implementation.
	 *
	 * @param acknowledgementInterval the interval.
	 * @return this instance.
	 */
	B acknowledgementInterval(Duration acknowledgementInterval);

	/**
	 * Set the threshold for triggering a batch acknowledgement. The default depends on the specific
	 * {@link ContainerComponentFactory} implementation.
	 *
	 * @param acknowledgementThreshold the threshold.
	 * @return this instance.
	 */
	B acknowledgementThreshold(int acknowledgementThreshold);

	/**
	 * Set the {@link AcknowledgementMode} for this container. Default is {@link AcknowledgementMode#ON_SUCCESS}.
	 *
	 * @param acknowledgementMode the acknowledgement mode.
	 * @return this instance.
	 */
	B acknowledgementMode(AcknowledgementMode acknowledgementMode);

	/**
	 * Set the {@link AcknowledgementOrdering} for this container. Default is {@link AcknowledgementOrdering#PARALLEL}.
	 *
	 * @param acknowledgementOrdering the acknowledgement ordering.
	 * @return this instance
	 */
	B acknowledgementOrdering(AcknowledgementOrdering acknowledgementOrdering);

	/**
	 * Set the {@link MessagingMessageConverter} for this container.
	 *
	 * @param messageConverter the message converter.
	 * @return this instance.
	 */
	B messageConverter(MessagingMessageConverter<?> messageConverter);

	/**
	 * Set the {@link ObservationRegistry} for this container.
	 *
	 * @param observationRegistry the observation registry.
	 * @return this instance.
	 */
	B observationRegistry(ObservationRegistry observationRegistry);

	/**
	 * Create the {@link ContainerOptions} instance.
	 *
	 * @return the new instance.
	 */
	O build();

	/**
	 * Create a copy of this builder.
	 *
	 * @return the copy.
	 */
	B createCopy();

	/**
	 * Copy the given builder settings to this builder.
	 *
	 * @param builder the builder from which to copy settings from.
	 */
	void fromBuilder(B builder);

}
