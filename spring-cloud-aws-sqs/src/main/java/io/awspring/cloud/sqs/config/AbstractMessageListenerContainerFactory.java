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
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
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
 * {@link ContainerOptions} that will be used by {@link MessageListenerContainer} instances created by this factory.
 *
 * @param <T> the {@link Message} type to be consumed by the {@link AbstractMessageListenerContainer}
 * @param <C> the {@link AbstractMessageListenerContainer} type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractMessageListenerContainerFactory<T, C extends MessageListenerContainer<T>>
		implements MessageListenerContainerFactory<C> {

	private final ContainerOptions.Builder containerOptionsBuilder;

	private AsyncErrorHandler<T> errorHandler;

	private AsyncMessageListener<T> messageListener;

	private final Collection<AsyncMessageInterceptor<T>> messageInterceptors = new ArrayList<>();

	private ContainerComponentFactory<T> componentFactory;

	protected AbstractMessageListenerContainerFactory() {
		this.containerOptionsBuilder = ContainerOptions.builder();
	}

	/**
	 * Set the {@link ErrorHandler} instance to be used by containers created with this factory. The component will be
	 * adapted to an {@link AsyncErrorHandler}.
	 * @param errorHandler the error handler instance.
	 */
	public void setErrorHandler(ErrorHandler<T> errorHandler) {
		Assert.notNull(errorHandler, "errorHandler cannot be null");
		this.errorHandler = AsyncComponentAdapters.adapt(errorHandler);
	}

	/**
	 * Set the {@link AsyncErrorHandler} instance to be used by containers created with this factory.
	 * @param errorHandler the error handler instance.
	 */
	public void setErrorHandler(AsyncErrorHandler<T> errorHandler) {
		Assert.notNull(errorHandler, "errorHandler cannot be null");
		this.errorHandler = errorHandler;
	}

	/**
	 * Add a {@link MessageInterceptor} to be used by containers created with this factory. Interceptors will be applied
	 * just before method invocation. The component will be adapted to an {@link AsyncMessageInterceptor}.
	 * @param messageInterceptor the message interceptor instance.
	 */
	public void addMessageInterceptor(MessageInterceptor<T> messageInterceptor) {
		Assert.notNull(messageInterceptor, "messageInterceptor cannot be null");
		this.messageInterceptors.add(AsyncComponentAdapters.adapt(messageInterceptor));
	}

	/**
	 * Add a {@link AsyncMessageInterceptor} to be used by containers created with this factory. Interceptors will be
	 * applied just before method invocation.
	 * @param messageInterceptor the message interceptor instance.
	 */
	public void addMessageInterceptor(AsyncMessageInterceptor<T> messageInterceptor) {
		Assert.notNull(messageInterceptor, "messageInterceptor cannot be null");
		this.messageInterceptors.add(messageInterceptor);
	}

	/**
	 * Set the {@link MessageListener} instance to be used by containers created with this factory. If none is provided,
	 * a default one will be created according to the endpoint's configuration. The component will be adapted to an
	 * {@link AsyncMessageListener}.
	 * @param messageListener the message listener instance.
	 */
	public void setMessageListener(MessageListener<T> messageListener) {
		Assert.notNull(messageListener, "messageListener cannot be null");
		this.messageListener = AsyncComponentAdapters.adapt(messageListener);
	}

	/**
	 * Set the {@link AsyncMessageListener} instance to be used by containers created with this factory. If none is
	 * provided, a default one will be created according to the endpoint's configuration.
	 * @param messageListener the message listener instance.
	 */
	public void setAsyncMessageListener(AsyncMessageListener<T> messageListener) {
		Assert.notNull(messageListener, "messageListener cannot be null");
		this.messageListener = messageListener;
	}

	public void setComponentFactory(ContainerComponentFactory<T> componentFactory) {
		Assert.notNull(componentFactory, "componentFactory cannot be null");
		this.componentFactory = componentFactory;
	}

	/**
	 * Return the {@link ContainerOptions} instance that will be used for configuring the
	 * {@link MessageListenerContainer} instances created by this factory.
	 * @return the container options instance.
	 */
	public void configure(Consumer<ContainerOptions.Builder> options) {
		options.accept(this.containerOptionsBuilder);
	}

	@Override
	public C createContainer(Endpoint endpoint) {
		Assert.notNull(endpoint, "endpoint cannot be null");
		ContainerOptions.Builder options = this.containerOptionsBuilder.createCopy();
		configure(endpoint, options);
		C container = createContainerInstance(endpoint, options.build());
		endpoint.setupContainer(container);
		configureContainer(container, endpoint);
		return container;
	}

	private void configure(Endpoint endpoint, ContainerOptions.Builder options) {
		ConfigUtils.INSTANCE.acceptIfInstance(endpoint, HandlerMethodEndpoint.class,
				abstractEndpoint -> abstractEndpoint
						.configureMessageDeliveryStrategy(options::messageDeliveryStrategy));
		doConfigureContainerOptions(endpoint, options);
	}

	protected abstract void doConfigureContainerOptions(Endpoint endpoint, ContainerOptions.Builder containerOptions);

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

	protected void configureAbstractContainer(AbstractMessageListenerContainer<T> container, Endpoint endpoint) {
		container.setQueueNames(endpoint.getLogicalNames());
		ConfigUtils.INSTANCE.acceptIfNotNull(endpoint.getId(), container::setId)
				.acceptIfNotNull(this.componentFactory, container::setComponentFactory)
				.acceptIfNotNull(this.messageListener, container::setAsyncMessageListener)
				.acceptIfNotNull(this.errorHandler, container::setErrorHandler)
				.acceptIfNotNull(this.messageInterceptors,
						interceptors -> interceptors.forEach(container::addMessageInterceptor));
	}

	protected abstract C createContainerInstance(Endpoint endpoint, ContainerOptions containerOptions);

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
