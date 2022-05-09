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
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.acknowledgement.AckHandler;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.awspring.cloud.sqs.listener.sink.MessageProcessingPipelineSink;
import io.awspring.cloud.sqs.listener.source.MessageSourceFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

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
public abstract class AbstractMessageListenerContainerFactory<T, C extends AbstractMessageListenerContainer<T>>
		implements MessageListenerContainerFactory<C> {

	private final ContainerOptions containerOptions;

	private AsyncErrorHandler<T> errorHandler;

	private AckHandler<T> ackHandler;

	private Supplier<MessageProcessingPipelineSink<T>> messageSinkSupplier;

	private AsyncMessageListener<T> messageListener;

	private MessageSourceFactory<T> messageSourceFactory;

	private final Collection<AsyncMessageInterceptor<T>> messageInterceptors = new ArrayList<>();

	protected AbstractMessageListenerContainerFactory(ContainerOptions containerOptions) {
		Assert.notNull(containerOptions, "containerOptions cannot be null");
		this.containerOptions = containerOptions.createCopy();
	}

	/**
	 * Set the {@link ErrorHandler} instance to be used by containers created with this factory. If none is provided, a
	 * default {@link io.awspring.cloud.sqs.listener.errorhandler.LoggingErrorHandler} is used. The component will be
	 * adapted to an {@link AsyncErrorHandler}.
	 * @param errorHandler the error handler instance.
	 */
	public void setErrorHandler(ErrorHandler<T> errorHandler) {
		Assert.notNull(errorHandler, "errorHandler cannot be null");
		this.errorHandler = AsyncComponentAdapters.adapt(errorHandler);
	}

	/**
	 * Set the {@link AsyncErrorHandler} instance to be used by containers created with this factory. If none is
	 * provided, a default {@link io.awspring.cloud.sqs.listener.errorhandler.LoggingErrorHandler} is used.
	 * @param errorHandler the error handler instance.
	 */
	public void setAsyncErrorHandler(AsyncErrorHandler<T> errorHandler) {
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
	public void addAsyncMessageInterceptor(AsyncMessageInterceptor<T> messageInterceptor) {
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

	/**
	 * Set the {@link AckHandler} instance to be used by containers created with this factory. If none is provided, a
	 * default {@link io.awspring.cloud.sqs.listener.acknowledgement.OnSuccessAckHandler} is used.
	 * @param ackHandler the acknowledgement handler instance.
	 */
	public void setAckHandler(AckHandler<T> ackHandler) {
		Assert.notNull(ackHandler, "ackHandler cannot be null");
		this.ackHandler = ackHandler;
	}

	/**
	 * Set the {@link MessageSourceFactory} instance to be used by containers created with this factory. If none is
	 * provided, a default will be instantiated.
	 * @param messageSourceFactory the message source instance.
	 */
	public void setMessageSourceFactory(MessageSourceFactory<T> messageSourceFactory) {
		Assert.notNull(messageSourceFactory, "messageSourceFactory cannot be null");
		this.messageSourceFactory = messageSourceFactory;
	}

	/**
	 * Set the {@link MessageProcessingPipelineSink} supplier to be used to create instances for
	 * containers created with this factory. If none is provided, a default will be
	 * instantiated according to each endpoint's configuration.
	 * @param messageSinkSupplier the instance.
	 */
	public void setMessageSinkSupplier(Supplier<MessageProcessingPipelineSink<T>> messageSinkSupplier) {
		Assert.notNull(messageSinkSupplier, "messageSplitter cannot be null");
		this.messageSinkSupplier = messageSinkSupplier;
	}

	/**
	 * Return the {@link ContainerOptions} instance that will be used for configuring the
	 * {@link MessageListenerContainer} instances created by this factory.
	 * @return the container options instance.
	 */
	public ContainerOptions getContainerOptions() {
		return this.containerOptions;
	}

	@Override
	public C createContainer(Endpoint endpoint) {
		Assert.notNull(endpoint, "endpoint cannot be null");
		C container = createContainerInstance(endpoint, this.containerOptions.createCopy());
		endpoint.setupContainer(container);
		configureContainer(container, endpoint);
		return container;
	}

	@Override
	public C createContainer(String... logicalEndpointNames) {
		Assert.notEmpty(logicalEndpointNames, "endpointNames cannot be empty");
		return createContainer(new EndpointAdapter(Arrays.asList(logicalEndpointNames)));
	}

	private void configureContainer(AbstractMessageListenerContainer<T> container, Endpoint endpoint) {
		container.setId(endpoint.getId());
		container.setQueueNames(endpoint.getLogicalNames());
		ConfigUtils.INSTANCE.acceptIfNotNull(this.messageSinkSupplier, supplier -> container.setMessageSink(messageSinkSupplier.get()))
				.acceptIfNotNull(this.messageListener, container::setAsyncMessageListener)
				.acceptIfNotNull(this.errorHandler, container::setAsyncErrorHandler)
				.acceptIfNotNull(this.ackHandler, container::setAckHandler)
				.acceptIfNotNull(this.messageSourceFactory, container::setMessageSourceFactory)
				.acceptIfNotNull(this.messageInterceptors,
						interceptors -> interceptors.forEach(container::addAsyncMessageInterceptor));
	}

	protected abstract C createContainerInstance(Endpoint endpoint, ContainerOptions containerOptions);

	private static class EndpointAdapter extends AbstractEndpoint {

		protected EndpointAdapter(Collection<String> logicalNames) {
			super(logicalNames, null, null, false);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void setupContainer(MessageListenerContainer container) {
			// No ops - container should be setup manually.
		}

		@Override
		protected MessageSourceFactory<?> createMessageSourceFactory() {
			return null;
		}
	}
}
