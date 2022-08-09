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
import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.ContainerComponentFactory;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * {@link MessageListenerContainerFactory} implementation for creating {@link SqsMessageListenerContainer} instances.
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsMessageListenerContainerFactory<T>
		extends AbstractMessageListenerContainerFactory<T, SqsMessageListenerContainer<T>> {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageListenerContainerFactory.class);

	private Supplier<SqsAsyncClient> sqsAsyncClientSupplier;

	@Override
	protected SqsMessageListenerContainer<T> createContainerInstance(Endpoint endpoint,
			ContainerOptions containerOptions) {
		logger.debug("Creating {} for endpoint {}", SqsMessageListenerContainer.class.getSimpleName(),
				endpoint.getId() != null ? endpoint.getId() : endpoint.getLogicalNames());
		Assert.notNull(this.sqsAsyncClientSupplier, "asyncClientSupplier not set");
		SqsAsyncClient asyncClient = getSqsAsyncClientInstance();
		return new SqsMessageListenerContainer<>(asyncClient, containerOptions);
	}

	protected SqsAsyncClient getSqsAsyncClientInstance() {
		return this.sqsAsyncClientSupplier.get();
	}

	protected void doConfigureContainerOptions(Endpoint endpoint, ContainerOptions.Builder options) {
		ConfigUtils.INSTANCE.acceptIfInstance(endpoint, SqsEndpoint.class,
				sqsEndpoint -> configureFromSqsEndpoint(sqsEndpoint, options));
	}

	private void configureFromSqsEndpoint(SqsEndpoint sqsEndpoint, ContainerOptions.Builder options) {
		ConfigUtils.INSTANCE
				.acceptIfNotNull(sqsEndpoint.getMaxInflightMessagesPerQueue(), options::maxInflightMessagesPerQueue)
				.acceptIfNotNull(sqsEndpoint.getPollTimeout(), options::pollTimeout)
				.acceptIfNotNull(sqsEndpoint.getMessageVisibilityDuration(), options::messageVisibility);
	}

	/**
	 * Set a supplier for {@link SqsAsyncClient} instances. A new instance will be used for each container created by
	 * this factory. Useful for high throughput containers where sharing an {@link SqsAsyncClient} would be harmful to
	 * performance.
	 *
	 * @param sqsAsyncClientSupplier the supplier.
	 */
	public void setSqsAsyncClientSupplier(Supplier<SqsAsyncClient> sqsAsyncClientSupplier) {
		Assert.notNull(sqsAsyncClientSupplier, "sqsAsyncClientSupplier cannot be null.");
		this.sqsAsyncClientSupplier = sqsAsyncClientSupplier;
	}

	/**
	 * Set the {@link SqsAsyncClient} instance to be shared by the containers. Useful for not-so-high throughput
	 * scenarios or when the client is tuned for more than the default maximum connections.
	 * @param sqsAsyncClient the client instance.
	 */
	public void setSqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
		Assert.notNull(sqsAsyncClient, "sqsAsyncClient cannot be null.");
		setSqsAsyncClientSupplier(() -> sqsAsyncClient);
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	public static class Builder<T> {

		private final Collection<AsyncMessageInterceptor<T>> asyncMessageInterceptors = new ArrayList<>();

		private final Collection<MessageInterceptor<T>> messageInterceptors = new ArrayList<>();

		private Supplier<SqsAsyncClient> sqsAsyncClientSupplier;

		private SqsAsyncClient sqsAsyncClient;

		private ContainerComponentFactory<T> componentFactory;

		private AsyncMessageListener<T> asyncMessageListener;

		private MessageListener<T> messageListener;

		private AsyncErrorHandler<T> asyncErrorHandler;

		private ErrorHandler<T> errorHandler;

		private Consumer<ContainerOptions.Builder> options = options -> {
		};

		public Builder<T> sqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
			this.sqsAsyncClient = sqsAsyncClient;
			return this;
		}

		public Builder<T> sqsAsyncClientSupplier(Supplier<SqsAsyncClient> sqsAsyncClientSupplier) {
			this.sqsAsyncClientSupplier = sqsAsyncClientSupplier;
			return this;
		}

		public Builder<T> componentFactory(ContainerComponentFactory<T> componentFactory) {
			this.componentFactory = componentFactory;
			return this;
		}

		public Builder<T> asyncMessageListener(AsyncMessageListener<T> asyncMessageListener) {
			this.asyncMessageListener = asyncMessageListener;
			return this;
		}

		public Builder<T> messageListener(MessageListener<T> messageListener) {
			this.messageListener = messageListener;
			return this;
		}

		public Builder<T> errorHandler(AsyncErrorHandler<T> asyncErrorHandler) {
			this.asyncErrorHandler = asyncErrorHandler;
			return this;
		}

		public Builder<T> errorHandler(ErrorHandler<T> errorHandler) {
			this.errorHandler = errorHandler;
			return this;
		}

		public Builder<T> messageInterceptor(AsyncMessageInterceptor<T> asyncMessageInterceptor) {
			this.asyncMessageInterceptors.add(asyncMessageInterceptor);
			return this;
		}

		public Builder<T> messageInterceptor(MessageInterceptor<T> messageInterceptor) {
			this.messageInterceptors.add(messageInterceptor);
			return this;
		}

		public Builder<T> configure(Consumer<ContainerOptions.Builder> options) {
			this.options = options;
			return this;
		}

		// @formatter:off
		public SqsMessageListenerContainerFactory<T> build() {
			SqsMessageListenerContainerFactory<T> factory = new SqsMessageListenerContainerFactory<>();
			ConfigUtils.INSTANCE
				.acceptIfNotNull(this.messageListener, factory::setMessageListener)
				.acceptIfNotNull(this.asyncMessageListener, factory::setAsyncMessageListener)
				.acceptIfNotNull(this.errorHandler, factory::setErrorHandler)
				.acceptIfNotNull(this.asyncErrorHandler, factory::setErrorHandler)
				.acceptIfNotNull(this.componentFactory, factory::setComponentFactory)
				.acceptIfNotNull(this.sqsAsyncClient, factory::setSqsAsyncClient)
				.acceptIfNotNull(this.sqsAsyncClientSupplier, factory::setSqsAsyncClientSupplier);
			this.messageInterceptors.forEach(factory::addMessageInterceptor);
			this.asyncMessageInterceptors.forEach(factory::addMessageInterceptor);
			factory.configure(this.options);
			return factory;
		}
		// @formatter:on

	}

}
