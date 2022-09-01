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

import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
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

	private final Object lifecycleMonitor = new Object();

	private boolean isRunning;

	private String id;

	private Collection<String> queueNames = new ArrayList<>();

	private ContainerComponentFactory<T> containerComponentFactory;

	private AsyncMessageListener<T> messageListener;

	private AsyncErrorHandler<T> errorHandler = new AsyncErrorHandler<T>() {
	};

	private final Collection<AsyncMessageInterceptor<T>> messageInterceptors = new ArrayList<>();

	private ContainerOptions containerOptions;

	protected AbstractMessageListenerContainer(ContainerOptions containerOptions) {
		Assert.notNull(containerOptions, "containerOptions cannot be null");
		this.containerOptions = containerOptions;
	}

	/**
	 * Set the id for this container instance.
	 * @param id the id.
	 */
	@Override
	public void setId(String id) {
		Assert.notNull(id, "id cannot be null");
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
	public void setErrorHandler(AsyncErrorHandler<T> errorHandler) {
		Assert.notNull(errorHandler, "errorHandler cannot be null");
		this.errorHandler = errorHandler;
	}

	/**
	 * Add a collection of interceptors that will intercept the message before processing. Interceptors are executed
	 * sequentially and in order.
	 * @param messageInterceptor the interceptor instances.
	 */
	public void addMessageInterceptor(MessageInterceptor<T> messageInterceptor) {
		Assert.notNull(messageInterceptor, "messageInterceptor cannot be null");
		this.messageInterceptors.add(AsyncComponentAdapters.adapt(messageInterceptor));
	}

	/**
	 * Add an interceptor that will intercept the message before processing. Interceptors are executed sequentially and
	 * in order.
	 * @param messageInterceptor the interceptor instances.
	 */
	public void addMessageInterceptor(AsyncMessageInterceptor<T> messageInterceptor) {
		Assert.notNull(messageInterceptor, "messageInterceptor cannot be null");
		this.messageInterceptors.add(messageInterceptor);
	}

	@Override
	public void setMessageListener(MessageListener<T> messageListener) {
		this.messageListener = AsyncComponentAdapters.adapt(messageListener);
	}

	@Override
	public void setAsyncMessageListener(AsyncMessageListener<T> asyncMessageListener) {
		this.messageListener = asyncMessageListener;
	}

	public void setComponentFactory(ContainerComponentFactory<T> containerComponentFactory) {
		this.containerComponentFactory = containerComponentFactory;
	}

	/**
	 * Returns the {@link ContainerOptions} instance for this container. Changed options will take effect on container
	 * restart.
	 * @return the container options.
	 */
	public void configure(Consumer<ContainerOptions.Builder> options) {
		Assert.state(!isRunning(), "Stop the container before making changes to the options");
		ContainerOptions.Builder builder = this.containerOptions.toBuilder();
		options.accept(builder);
		this.containerOptions = builder.build();
	}

	public ContainerOptions getContainerOptions() {
		return this.containerOptions;
	}

	/**
	 * Return the {@link MessageSource} instances used by this container.
	 * @return the instances.
	 */
	public ContainerComponentFactory<T> getContainerComponentFactory() {
		return this.containerComponentFactory;
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
			Assert.state(!this.queueNames.isEmpty(), "Queue names not set");
			Assert.notNull(this.messageListener, "messageListener cannot be null");
			this.isRunning = true;
			if (this.id == null) {
				this.id = resolveContainerId();
			}
			this.containerOptions.validate();
			logger.debug("Starting container {}", getId());
			doStart();
		}
		logger.info("Container {} started", this.id);
	}

	private String resolveContainerId() {
		String firstQueueName = this.queueNames.iterator().next();
		return firstQueueName.startsWith("http")
				? firstQueueName.substring(Math.max(firstQueueName.length() - 10, 0)) + "-container"
				: firstQueueName.substring(0, Math.min(15, firstQueueName.length())) + "-container";
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
		logger.info("Container {} stopped", this.id);
	}

	protected void doStop() {
	}

}
