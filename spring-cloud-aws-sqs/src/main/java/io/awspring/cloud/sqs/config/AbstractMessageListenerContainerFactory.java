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
package io.awspring.cloud.sqs.config;

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.listener.AbstractMessageListenerContainer;
import io.awspring.cloud.sqs.listener.AsyncComponentAdapters;
import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.ContainerComponentFactory;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.ContainerOptionsBuilder;
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.AsyncAcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Base implementation for a {@link MessageListenerContainerFactory}. Contains the components and
 * {@link ContainerOptions} that will be used as a template for {@link MessageListenerContainer} instances created by
 * this factory.
 *
 * @param <T> the {@link Message}'s payload type to be consumed by the {@link AbstractMessageListenerContainer}.
 * @param <C> the type of {@link AbstractMessageListenerContainer} instances that will be created by this container.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractMessageListenerContainerFactory<T, C extends MessageListenerContainer<T>, O extends ContainerOptions<O, B>, B extends ContainerOptionsBuilder<B, O>>
		implements MessageListenerContainerFactory<C> {

	private final B containerOptionsBuilder;

	private final Collection<AsyncMessageInterceptor<T>> asyncMessageInterceptors = new ArrayList<>();

	private final Collection<MessageInterceptor<T>> messageInterceptors = new ArrayList<>();

	private AsyncErrorHandler<T> asyncErrorHandler;

	private ErrorHandler<T> errorHandler;

	private AsyncMessageListener<T> asyncMessageListener;

	private MessageListener<T> messageListener;

	private AsyncAcknowledgementResultCallback<T> asyncAcknowledgementResultCallback;

	private AcknowledgementResultCallback<T> acknowledgementResultCallback;

	private Collection<ContainerComponentFactory<T, O>> containerComponentFactories;

	protected AbstractMessageListenerContainerFactory(O containerOptions) {
		this.containerOptionsBuilder = containerOptions.toBuilder();
	}

	/**
	 * Set the {@link ErrorHandler} instance to be used by containers created with this factory. The component will be
	 * adapted to an {@link AsyncErrorHandler}.
	 * @param errorHandler the error handler instance.
	 * @see AsyncComponentAdapters
	 */
	public void setErrorHandler(ErrorHandler<T> errorHandler) {
		Assert.notNull(errorHandler, "errorHandler cannot be null");
		this.errorHandler = errorHandler;
	}

	/**
	 * Set the {@link AsyncErrorHandler} instance to be used by containers created with this factory.
	 * @param errorHandler the error handler instance.
	 */
	public void setErrorHandler(AsyncErrorHandler<T> errorHandler) {
		Assert.notNull(errorHandler, "errorHandler cannot be null");
		this.asyncErrorHandler = errorHandler;
	}

	/**
	 * Add a {@link MessageInterceptor} to be used by containers created with this factory. Interceptors will be applied
	 * just before method invocation. The component will be adapted to an {@link AsyncMessageInterceptor}.
	 * @param messageInterceptor the message interceptor instance.
	 * @see AsyncComponentAdapters
	 */
	public void addMessageInterceptor(MessageInterceptor<T> messageInterceptor) {
		Assert.notNull(messageInterceptor, "messageInterceptor cannot be null");
		this.messageInterceptors.add(messageInterceptor);
	}

	/**
	 * Add a {@link AsyncMessageInterceptor} to be used by containers created with this factory. Interceptors will be
	 * applied just before method invocation.
	 * @param messageInterceptor the message interceptor instance.
	 */
	public void addMessageInterceptor(AsyncMessageInterceptor<T> messageInterceptor) {
		Assert.notNull(messageInterceptor, "messageInterceptor cannot be null");
		this.asyncMessageInterceptors.add(messageInterceptor);
	}

	/**
	 * Set the {@link MessageListener} instance to be used by containers created with this factory. If none is provided,
	 * a default one will be created according to the endpoint's configuration. The component will be adapted to an
	 * {@link AsyncMessageListener}.
	 * @param messageListener the message listener instance.
	 * @see AsyncComponentAdapters
	 */
	public void setMessageListener(MessageListener<T> messageListener) {
		Assert.notNull(messageListener, "messageListener cannot be null");
		this.messageListener = messageListener;
	}

	/**
	 * Set the {@link AsyncMessageListener} instance to be used by containers created with this factory. If none is
	 * provided, a default one will be created according to the endpoint's configuration.
	 * @param messageListener the message listener instance.
	 */
	public void setAsyncMessageListener(AsyncMessageListener<T> messageListener) {
		Assert.notNull(messageListener, "messageListener cannot be null");
		this.asyncMessageListener = messageListener;
	}

	/**
	 * Set the {@link AsyncAcknowledgementResultCallback} instance to be used by containers created by this factory.
	 * @param acknowledgementResultCallback the instance.
	 */
	public void setAcknowledgementResultCallback(AsyncAcknowledgementResultCallback<T> acknowledgementResultCallback) {
		Assert.notNull(acknowledgementResultCallback, "acknowledgementResultCallback cannot be null");
		this.asyncAcknowledgementResultCallback = acknowledgementResultCallback;
	}

	/**
	 * Set the {@link AcknowledgementResultCallback} instance to be used by containers created by this factory.
	 * @param acknowledgementResultCallback the instance.
	 */
	public void setAcknowledgementResultCallback(AcknowledgementResultCallback<T> acknowledgementResultCallback) {
		Assert.notNull(acknowledgementResultCallback, "acknowledgementResultCallback cannot be null");
		this.acknowledgementResultCallback = acknowledgementResultCallback;
	}

	/**
	 * Set the {@link ContainerComponentFactory} instances that will be used to create components for listener
	 * containers created by this factory.
	 * @param containerComponentFactories the factory instances.
	 */
	public void setContainerComponentFactories(
			Collection<ContainerComponentFactory<T, O>> containerComponentFactories) {
		Assert.notEmpty(containerComponentFactories, "containerComponentFactories cannot be null or empty");
		this.containerComponentFactories = containerComponentFactories;
	}

	/**
	 * Allows configuring this factories' {@link ContainerOptionsBuilder}.
	 */
	public void configure(Consumer<B> options) {
		options.accept(this.containerOptionsBuilder);
	}

	@Override
	public C createContainer(Endpoint endpoint) {
		Assert.notNull(endpoint, "endpoint cannot be null");
		B options = this.containerOptionsBuilder.createCopy();
		configure(endpoint, options);
		C container = createContainerInstance(endpoint, options.build());
		endpoint.setupContainer(container);
		configureContainer(container, endpoint);
		return container;
	}

	private void configure(Endpoint endpoint, B options) {
		ConfigUtils.INSTANCE.acceptIfInstance(endpoint, HandlerMethodEndpoint.class,
				abstractEndpoint -> abstractEndpoint.configureListenerMode(options::listenerMode));
		configureContainerOptions(endpoint, options);
	}

	protected void configureContainerOptions(Endpoint endpoint, B containerOptions) {
	}

	@Override
	public C createContainer(String... logicalEndpointNames) {
		Assert.notEmpty(logicalEndpointNames, "endpointNames cannot be empty");
		return createContainer(new EndpointAdapter(Arrays.asList(logicalEndpointNames)));
	}

	@SuppressWarnings("unchecked")
	protected void configureContainer(C container, Endpoint endpoint) {
		ConfigUtils.INSTANCE.acceptIfInstance(container, AbstractMessageListenerContainer.class,
				abstractContainer -> configureAbstractContainer(abstractContainer, endpoint));
	}

	protected void configureAbstractContainer(AbstractMessageListenerContainer<T, O, B> container, Endpoint endpoint) {
		container.setQueueNames(endpoint.getLogicalNames());
		ConfigUtils.INSTANCE.acceptIfNotNull(endpoint.getId(), container::setId)
				.acceptIfNotNull(this.containerComponentFactories, container::setComponentFactories)
				.acceptIfNotNull(this.messageListener, container::setMessageListener)
				.acceptIfNotNull(this.asyncMessageListener, container::setAsyncMessageListener)
				.acceptIfNotNull(this.errorHandler, container::setErrorHandler)
				.acceptIfNotNull(this.asyncErrorHandler, container::setErrorHandler)
				.acceptIfNotNull(this.asyncAcknowledgementResultCallback, container::setAcknowledgementResultCallback)
				.acceptIfNotNull(this.acknowledgementResultCallback, container::setAcknowledgementResultCallback)
				.acceptIfNotEmpty(this.messageInterceptors,
						interceptors -> interceptors.forEach(container::addMessageInterceptor))
				.acceptIfNotEmpty(this.asyncMessageInterceptors,
						interceptors -> interceptors.forEach(container::addMessageInterceptor));
	}

	protected abstract C createContainerInstance(Endpoint endpoint, O containerOptions);

	private static class EndpointAdapter implements Endpoint {

		private final Collection<String> endpointNames;

		public EndpointAdapter(Collection<String> endpointNames) {
			this.endpointNames = endpointNames;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void setupContainer(MessageListenerContainer container) {
			// No ops - container should be setup manually.
		}

		@Override
		public Collection<String> getLogicalNames() {
			return this.endpointNames;
		}

		@Override
		public String getListenerContainerFactoryName() {
			// we're already in the factory
			return null;
		}

		@Override
		public String getId() {
			// Container will setup its own id
			return null;
		}
	}
}
