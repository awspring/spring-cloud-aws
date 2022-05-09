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

import io.awspring.cloud.sqs.listener.acknowledgement.AckHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.OnSuccessAckHandler;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.LoggingErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.awspring.cloud.sqs.listener.sink.FanOutMessageSink;
import io.awspring.cloud.sqs.listener.sink.MessageProcessingPipelineSink;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import io.awspring.cloud.sqs.listener.source.MessageSourceFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Base implementation for {@link MessageListenerContainer} with {@link SmartLifecycle} and component management
 * capabilities.
 *
 * @param <T> the {@link Message} type to be consumed by the {@link AbstractMessageListenerContainer}
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractMessageListenerContainer<T> implements MessageListenerContainer<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMessageListenerContainer.class);

	private final LoggingErrorHandler<T> DEFAULT_ERROR_HANDLER = new LoggingErrorHandler<>();

	private final OnSuccessAckHandler<T> DEFAULT_ACK_HANDLER = new OnSuccessAckHandler<>();

	private final FanOutMessageSink<T> DEFAULT_MESSAGE_SINK = new FanOutMessageSink<>();

	private final Object lifecycleMonitor = new Object();

	private volatile boolean isRunning;

	private String id;

	private Collection<String> queueNames = new ArrayList<>();

	private MessageSourceFactory<T> messageSourceFactory;

	private AsyncMessageListener<T> messageListener;

	private AsyncErrorHandler<T> errorHandler = DEFAULT_ERROR_HANDLER;

	private AckHandler<T> ackHandler = DEFAULT_ACK_HANDLER;

	private MessageSink<T> messageSink = DEFAULT_MESSAGE_SINK;

	private final Collection<AsyncMessageInterceptor<T>> messageInterceptors = new ArrayList<>();

	private final ContainerOptions containerOptions;

	protected AbstractMessageListenerContainer(ContainerOptions containerOptions) {
		Assert.notNull(containerOptions, "containerOptions cannot be null");
		this.containerOptions = containerOptions;
	}

	/**
	 * Set the id for this container instance. The id will be used for creating the processing thread name.
	 * @param id the id.
	 */
	public void setId(String id) {
		Assert.state(this.id == null, () -> "id already set for container " + this.id);
		this.id = id;
	}

	/**
	 * Set the {@link ErrorHandler} instance to be used by this container. The component will be adapted to an
	 * {@link AsyncErrorHandler}.
	 * @param errorHandler the instance.
	 */
	public void setErrorHandler(ErrorHandler<T> errorHandler) {
		Assert.notNull(errorHandler, "errorHandler cannot be null");
		this.errorHandler = AsyncComponentAdapters.adapt(errorHandler);
	}

	/**
	 * Set the {@link AsyncErrorHandler} instance to be used by this container.
	 * @param errorHandler the instance.
	 */
	public void setAsyncErrorHandler(AsyncErrorHandler<T> errorHandler) {
		Assert.notNull(errorHandler, "errorHandler cannot be null");
		this.errorHandler = errorHandler;
	}

	/**
	 * Add a collection of interceptors that will intercept the message before processing. Interceptors are executed
	 * sequentially and in order.
	 * @param messageInterceptor the interceptor instances.
	 */
	public void addMessageInterceptor(MessageInterceptor<T> messageInterceptor) {
		Assert.notNull(messageInterceptor, "messageInterceptors cannot be null");
		this.messageInterceptors.add(AsyncComponentAdapters.adapt(messageInterceptor));
	}

	/**
	 * Add an interceptor that will intercept the message before processing. Interceptors are executed sequentially and
	 * in order.
	 * @param messageInterceptor the interceptor instances.
	 */
	public void addAsyncMessageInterceptor(AsyncMessageInterceptor<T> messageInterceptor) {
		Assert.notNull(messageInterceptor, "messageInterceptors cannot be null");
		this.messageInterceptors.add(messageInterceptor);
	}

	public void setMessageListener(MessageListener<T> messageListener) {
		this.messageListener = AsyncComponentAdapters.adapt(messageListener);
	}

	@Override
	public void setAsyncMessageListener(AsyncMessageListener<T> asyncMessageListener) {
		this.messageListener = asyncMessageListener;
	}

	/**
	 * Set the {@link AckHandler} instance to be used by this container.
	 * @param ackHandler the instance.
	 */
	public void setAckHandler(AckHandler<T> ackHandler) {
		Assert.notNull(ackHandler, "ackHandler cannot be null");
		this.ackHandler = ackHandler;
	}

	public void setMessageSourceFactory(MessageSourceFactory<T> messageSourceFactory) {
		this.messageSourceFactory = messageSourceFactory;
	}

	@Override
	public void setMessageSink(MessageSink<T> messageSink) {
		this.messageSink = messageSink;
	}

	/**
	 * Returns the {@link ContainerOptions} instance for this container. Changed options will take effect on container
	 * restart.
	 * @return the container options.
	 */
	public ContainerOptions getContainerOptions() {
		return this.containerOptions;
	}

	/**
	 * Return the {@link MessageSource} instances used by this container.
	 * @return the instances.
	 */
	public MessageSourceFactory<T> getMessageSourceFactory() {
		return this.messageSourceFactory;
	}

	/**
	 * Return the {@link AsyncMessageListener} instance used by this container.
	 * @return the instance.
	 */
	public AsyncMessageListener<T> getMessageListener() {
		return this.messageListener;
	}

	/**
	 * Return the {@link AsyncErrorHandler} instance used by this container.
	 * @return the instance.
	 */
	public AsyncErrorHandler<T> getErrorHandler() {
		return this.errorHandler;
	}

	/**
	 * Return the {@link AckHandler} instance used by this container.
	 * @return the instance.
	 */
	public AckHandler<T> getAckHandler() {
		return this.ackHandler;
	}

	/**
	 * Return the {@link MessageProcessingPipelineSink} instances used by this container.
	 * @return the instance.
	 */
	public MessageSink<T> getMessageSink() {
		return this.messageSink;
	}

	/**
	 * Return the {@link AsyncMessageInterceptor} instances used by this container.
	 * @return the instances.
	 */
	public Collection<AsyncMessageInterceptor<T>> getMessageInterceptors() {
		return Collections.unmodifiableCollection(this.messageInterceptors);
	}

	@Override
	public String getId() {
		return this.id;
	}

	/**
	 * Set the queue logical names that will be handled by the container. Required for container start.
	 * @param queueNames the queue names.
	 */
	public void setQueueNames(Collection<String> queueNames) {
		Assert.notEmpty(queueNames, "queueNames cannot be empty");
		this.queueNames = queueNames;
	}

	/**
	 * Set the queue logical names that will be handled by the container. Required for container start.
	 * @param queueNames the queue names.
	 */
	public void setQueueNames(String... queueNames) {
		setQueueNames(Arrays.asList(queueNames));
	}

	/**
	 * Return the queue names assigned to this container. May be empty if custom {@link MessageSource} instances are
	 * provided.
	 * @return the queue names.
	 */
	public Collection<String> getQueueNames() {
		return Collections.unmodifiableCollection(this.queueNames);
	}

	@Override
	public boolean isRunning() {
		return this.isRunning;
	}

	@Override
	public void start() {
		if (this.isRunning) {
			return;
		}
		synchronized (this.lifecycleMonitor) {
			Assert.state(!this.queueNames.isEmpty(), "Queue logical names not set");
			this.isRunning = true;
			if (this.id == null) {
				this.id = resolveContainerId();
			}
			this.containerOptions.validate();
			logger.debug("Starting container {}", getId());
			doStart();
		}
		logger.debug("Container started {}", this.id);
	}

	private String resolveContainerId() {
		return "io.awspring.cloud.sqs.sqsListenerEndpointContainer#"
				+ this.queueNames.stream().findFirst().orElseGet(() -> UUID.randomUUID().toString());
	}

	protected void doStart() {
	}

	@Override
	public void stop() {
		if (!this.isRunning) {
			return;
		}
		logger.debug("Stopping container {}", this.id);
		synchronized (this.lifecycleMonitor) {
			this.isRunning = false;
			doStop();
		}
		logger.debug("Container stopped {}", this.id);
	}

	protected void doStop() {
	}

}
