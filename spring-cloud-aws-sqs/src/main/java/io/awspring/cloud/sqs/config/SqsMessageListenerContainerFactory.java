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
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.ContainerComponentFactory;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.SqsContainerOptionsBuilder;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.AsyncAcknowledgementResultCallback;
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
 * {@link MessageListenerContainerFactory} implementation for creating {@link SqsMessageListenerContainer} instances. A
 * factory can be assigned to a {@link io.awspring.cloud.sqs.annotation.SqsListener @SqsListener} by using the
 * {@link SqsListener#factory()} property. The factory can also be used to create container instances manually.
 * <p>
 * To create an instance, both the default constructor or the {@link #builder()} method can be used, and further
 * configuration can be achieved by using the {@link #configure(Consumer)} method.
 * <p>
 * The {@link SqsAsyncClient} instance to be used by the containers created by this factory can be set using either the
 * {@link #setSqsAsyncClient} or {@link #setSqsAsyncClientSupplier} methods, or their builder counterparts. The former
 * will result in the containers sharing the supplied instance, where the later will result in a different instance
 * being used by each container.
 * <p>
 * The factory also accepts the following components:
 * <ul>
 * <li>{@link MessageInterceptor}</li>
 * <li>{@link MessageListener}</li>
 * <li>{@link ErrorHandler}</li>
 * <li>{@link AsyncMessageInterceptor}</li>
 * <li>{@link AsyncMessageListener}</li>
 * <li>{@link AsyncErrorHandler}</li>
 * </ul>
 * The non-async components will be adapted to their async counterparts. When using Spring Boot and auto-configuration,
 * beans implementing these interfaces will be set to the default factory.
 * <p>
 * Example using the builder:
 *
 * <pre>
 * <code>
 * &#064;Bean
 * SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
 *     return SqsMessageListenerContainerFactory
 *             .builder()
 *             .configure(options -> options
 *                     .maxMessagesPerPoll(5)
 *                     .pollTimeout(Duration.ofSeconds(10)))
 *             .sqsAsyncClient(sqsAsyncClient)
 *             .build();
 * }
 * </code>
 * </pre>
 *
 * <p>
 * Example using the default constructor:
 *
 * <pre>
 * <code>
 * &#064;Bean
 * SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
 *     SqsMessageListenerContainerFactory<Object> factory = new SqsMessageListenerContainerFactory<>();
 *     factory.setSqsAsyncClient(sqsAsyncClient);
 *     factory.configure(options -> options
 *             .maxMessagesPerPoll(5)
 *             .pollTimeout(Duration.ofSeconds(10)));
 *     return factory;
 * }
 * </code>
 * </pre>
 * <p>
 * Example creating a container manually:
 *
 * <pre>
 * <code>
 * &#064;Bean
 * SqsMessageListenerContainer<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
 *     return SqsMessageListenerContainerFactory
 *             .builder()
 *             .configure(options -> options
 *                     .maxMessagesPerPoll(5)
 *                     .pollTimeout(Duration.ofSeconds(10)))
 *             .sqsAsyncClient(sqsAsyncClient)
 *             .build()
 *             .createContainer("myQueue");
 * }
 * </code>
 * </pre>
 *
 * @param <T> the {@link Message} payload type. This type is used to ensure at compile time that all components in this
 *     factory expect the same payload type. If the factory will be used with many payload types, {@link Object} can be
 *     used.
 *
 * @author Tomaz Fernandes
 * @author Joao Calassio
 * @author José Iêdo
 * @since 3.0
 * @see SqsMessageListenerContainer
 * @see ContainerOptions
 * @see io.awspring.cloud.sqs.listener.AsyncComponentAdapters
 */
public class SqsMessageListenerContainerFactory<T> extends
		AbstractMessageListenerContainerFactory<T, SqsMessageListenerContainer<T>, SqsContainerOptions, SqsContainerOptionsBuilder> {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageListenerContainerFactory.class);

	private Supplier<SqsAsyncClient> sqsAsyncClientSupplier;

	public SqsMessageListenerContainerFactory() {
		super(SqsContainerOptions.builder().build());
	}

	@Override
	protected SqsMessageListenerContainer<T> createContainerInstance(Endpoint endpoint,
			SqsContainerOptions containerOptions) {
		logger.debug("Creating {} for endpoint {}", SqsMessageListenerContainer.class.getSimpleName(),
				endpoint.getId() != null ? endpoint.getId() : endpoint.getLogicalNames());
		Assert.notNull(this.sqsAsyncClientSupplier, "asyncClientSupplier not set");
		SqsAsyncClient asyncClient = getSqsAsyncClientInstance();
		return new SqsMessageListenerContainer<>(asyncClient, containerOptions);
	}

	protected SqsAsyncClient getSqsAsyncClientInstance() {
		return this.sqsAsyncClientSupplier.get();
	}

	protected void configureContainerOptions(Endpoint endpoint, SqsContainerOptionsBuilder options) {
		ConfigUtils.INSTANCE.acceptIfInstance(endpoint, SqsEndpoint.class,
				sqsEndpoint -> configureFromSqsEndpoint(sqsEndpoint, options));

		ConfigUtils.INSTANCE.acceptIfInstance(endpoint, MultiMethodSqsEndpoint.class,
				multiMethodSqsEndpoint -> configureFromMultiMethodSqsEndpoint(multiMethodSqsEndpoint, options));
	}

	private void configureFromMultiMethodSqsEndpoint(MultiMethodSqsEndpoint multiMethodSqsEndpoint,
			SqsContainerOptionsBuilder options) {
		ConfigUtils.INSTANCE.acceptIfInstance(multiMethodSqsEndpoint.getEndpoint(), SqsEndpoint.class,
				endpoint -> configureFromSqsEndpoint(endpoint, options));
	}

	private void configureFromSqsEndpoint(SqsEndpoint sqsEndpoint, SqsContainerOptionsBuilder options) {
		ConfigUtils.INSTANCE.acceptIfNotNull(sqsEndpoint.getMaxConcurrentMessages(), options::maxConcurrentMessages)
				.acceptIfNotNull(sqsEndpoint.getMaxMessagesPerPoll(), options::maxMessagesPerPoll)
				.acceptIfNotNull(sqsEndpoint.getPollTimeout(), options::pollTimeout)
				.acceptIfNotNull(sqsEndpoint.getMessageVisibility(), options::messageVisibility)
				.acceptIfNotNull(sqsEndpoint.getAcknowledgementMode(), options::acknowledgementMode);
	}

	/**
	 * Set a supplier for {@link SqsAsyncClient} instances. A new instance will be used for each container created by
	 * this factory. Useful for high throughput containers where sharing an {@link SqsAsyncClient} would be detrimental
	 * to performance.
	 *
	 * @param sqsAsyncClientSupplier the supplier.
	 */
	public void setSqsAsyncClientSupplier(Supplier<SqsAsyncClient> sqsAsyncClientSupplier) {
		Assert.notNull(sqsAsyncClientSupplier, "sqsAsyncClientSupplier cannot be null.");
		this.sqsAsyncClientSupplier = sqsAsyncClientSupplier;
	}

	/**
	 * Set the {@link SqsAsyncClient} instance to be shared by the containers. For high throughput scenarios the client
	 * should be tuned for allowing higher maximum connections.
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

		private Collection<ContainerComponentFactory<T, SqsContainerOptions>> containerComponentFactories;

		private AsyncMessageListener<T> asyncMessageListener;

		private MessageListener<T> messageListener;

		private AsyncErrorHandler<T> asyncErrorHandler;

		private ErrorHandler<T> errorHandler;

		private Consumer<SqsContainerOptionsBuilder> optionsConsumer = options -> {
		};

		private AcknowledgementResultCallback<T> acknowledgementResultCallback;

		private AsyncAcknowledgementResultCallback<T> asyncAcknowledgementResultCallback;

		/**
		 * Set the {@link SqsAsyncClient} instance to be shared by the containers. For high throughput scenarios the
		 * client should be tuned for allowing higher maximum connections.
		 * @param sqsAsyncClient the client instance.
		 */
		public Builder<T> sqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
			this.sqsAsyncClient = sqsAsyncClient;
			return this;
		}

		/**
		 * Set a supplier for {@link SqsAsyncClient} instances. A new instance will be used for each container created
		 * by this factory. Useful for high throughput containers where sharing an {@link SqsAsyncClient} would be
		 * detrimental to performance.
		 *
		 * @param sqsAsyncClientSupplier the supplier.
		 */
		public Builder<T> sqsAsyncClientSupplier(Supplier<SqsAsyncClient> sqsAsyncClientSupplier) {
			this.sqsAsyncClientSupplier = sqsAsyncClientSupplier;
			return this;
		}

		public Builder<T> containerComponentFactories(
				Collection<ContainerComponentFactory<T, SqsContainerOptions>> containerComponentFactories) {
			this.containerComponentFactories = containerComponentFactories;
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

		public Builder<T> acknowledgementResultCallback(
				AsyncAcknowledgementResultCallback<T> asyncAcknowledgementResultCallback) {
			this.asyncAcknowledgementResultCallback = asyncAcknowledgementResultCallback;
			return this;
		}

		public Builder<T> acknowledgementResultCallback(
				AcknowledgementResultCallback<T> acknowledgementResultCallback) {
			this.acknowledgementResultCallback = acknowledgementResultCallback;
			return this;
		}

		public Builder<T> configure(Consumer<SqsContainerOptionsBuilder> options) {
			this.optionsConsumer = options;
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
				.acceptIfNotNull(this.acknowledgementResultCallback, factory::setAcknowledgementResultCallback)
				.acceptIfNotNull(this.asyncAcknowledgementResultCallback, factory::setAcknowledgementResultCallback)
				.acceptIfNotNull(this.containerComponentFactories, factory::setContainerComponentFactories)
				.acceptIfNotNull(this.sqsAsyncClient, factory::setSqsAsyncClient)
				.acceptIfNotNull(this.sqsAsyncClientSupplier, factory::setSqsAsyncClientSupplier);
			this.messageInterceptors.forEach(factory::addMessageInterceptor);
			this.asyncMessageInterceptors.forEach(factory::addMessageInterceptor);
			factory.configure(this.optionsConsumer);
			return factory;
		}
		// @formatter:on

	}

}
