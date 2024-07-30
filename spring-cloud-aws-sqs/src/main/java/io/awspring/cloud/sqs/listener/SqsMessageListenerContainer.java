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

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.AsyncAcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * {@link MessageListenerContainer} implementation for SQS queues. To create an instance, both constructors or the
 * {@link #builder()} method can be used, and further configuration can be achieved by using the
 * {@link #configure(Consumer)} method.
 * <p>
 * The {@link SqsAsyncClient} instance to be used by this container must be set through the constructor or the
 * {@link Builder#sqsAsyncClient} method.
 * <p>
 * The container also accepts the following components:
 * <ul>
 * <li>{@link MessageInterceptor}</li>
 * <li>{@link MessageListener}</li>
 * <li>{@link ErrorHandler}</li>
 * <li>{@link AsyncMessageInterceptor}</li>
 * <li>{@link AsyncMessageListener}</li>
 * <li>{@link AsyncErrorHandler}</li>
 * </ul>
 * The non-async components will be adapted to their async counterparts. Components and {@link ContainerOptions} can be
 * changed when the container is stopped. Such changes will be effective upon container restart.
 * <p>
 * Containers created through the {@link SqsListener} annotation will be registered in a
 * {@link MessageListenerContainerRegistry} which will be responsible for managing their lifecycle. Containers created
 * manually and declared as beans will have their lifecycle managed by Spring Context.
 * <p>
 * Example using the builder:
 *
 * <pre>
 * <code>
 * &#064;Bean
 * public SqsMessageListenerContainer<Object> mySqsListenerContainer(SqsAsyncClient sqsAsyncClient) {
 *     return SqsMessageListenerContainer
 *             .builder()
 *             .configure(options -> options
 *                     .maxMessagesPerPoll(5)
 *                     .pollTimeout(Duration.ofSeconds(10)))
 *             .sqsAsyncClient(sqsAsyncClient)
 *             .messageListener(System.out::println)
 *             .queueNames("myTestQueue")
 *             .build();
 * }
 * </code>
 * </pre>
 *
 * <p>
 * Example using the constructor:
 *
 * <pre>
 * <code>
 * &#064;Bean
 * public SqsMessageListenerContainer<Object> myListenerContainer(SqsAsyncClient sqsAsyncClient) {
 *     SqsMessageListenerContainer<Object> container = new SqsMessageListenerContainer<>(sqsAsyncClient);
 *     container.configure(options -> options
 *             .maxMessagesPerPoll(5)
 *             .pollTimeout(Duration.ofSeconds(10)));
 *     container.setQueueNames("myTestQueue");
 *     container.setMessageListener(System.out::println);
 *     return container;
 * }
 * </code>
 * </pre>
 *
 * @param <T> the {@link Message} payload type. This type is used to ensure at compile time that all components in this
 *     container expect the same payload type. If the factory will be used with many payload types, {@link Object} can
 *     be used.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsMessageListenerContainer<T>
		extends AbstractPipelineMessageListenerContainer<T, SqsContainerOptions, SqsContainerOptionsBuilder> {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageListenerContainer.class);

	private final SqsAsyncClient sqsAsyncClient;

	public SqsMessageListenerContainer(SqsAsyncClient sqsAsyncClient, SqsContainerOptions options) {
		super(options);
		Assert.notNull(sqsAsyncClient, "sqsAsyncClient cannot be null");
		this.sqsAsyncClient = sqsAsyncClient;
	}

	public SqsMessageListenerContainer(SqsAsyncClient sqsAsyncClient) {
		this(sqsAsyncClient, SqsContainerOptions.builder().build());
	}

	@Override
	protected Collection<ContainerComponentFactory<T, SqsContainerOptions>> createDefaultComponentFactories() {
		return Arrays.asList(new FifoSqsComponentFactory<>(), new StandardSqsComponentFactory<>());
	}

	@Override
	public void setQueueNames(Collection<String> queueNames) {
		Assert.isTrue(
			queueNames.stream().allMatch(this::isFifoQueue) || queueNames.stream().noneMatch(this::isFifoQueue),
			"SqsMessageListenerContainer must contain either all FIFO or all Standard queues.");
		super.setQueueNames(queueNames);
	}

	private boolean isFifoQueue(String name) {
		return name.endsWith(".fifo");
	}

	@Override
	protected void doConfigureMessageSources(Collection<MessageSource<T>> messageSources) {
		ConfigUtils.INSTANCE.acceptManyIfInstance(messageSources, SqsAsyncClientAware.class,
				asca -> asca.setSqsAsyncClient(this.sqsAsyncClient));
	}

	@Override
	protected void doConfigureMessageSink(MessageSink<T> messageSink) {
		ConfigUtils.INSTANCE.acceptIfInstance(messageSink, SqsAsyncClientAware.class,
				asca -> asca.setSqsAsyncClient(this.sqsAsyncClient));
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	public static class Builder<T> {

		private final Collection<String> queueNames = new ArrayList<>();

		private final Collection<AsyncMessageInterceptor<T>> asyncMessageInterceptors = new ArrayList<>();

		private final Collection<MessageInterceptor<T>> messageInterceptors = new ArrayList<>();

		private SqsAsyncClient sqsAsyncClient;

		private Collection<ContainerComponentFactory<T, SqsContainerOptions>> containerComponentFactories;

		private AsyncMessageListener<T> asyncMessageListener;

		private MessageListener<T> messageListener;

		private String id;

		private AsyncErrorHandler<T> asyncErrorHandler;

		private ErrorHandler<T> errorHandler;

		private Consumer<SqsContainerOptionsBuilder> optionsConsumer = options -> {
		};

		private AsyncAcknowledgementResultCallback<T> asyncAcknowledgementResultCallback;

		private AcknowledgementResultCallback<T> acknowledgementResultCallback;

		private Integer phase;

		public Builder<T> id(String id) {
			this.id = id;
			return this;
		}

		public Builder<T> sqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
			this.sqsAsyncClient = sqsAsyncClient;
			return this;
		}

		public Builder<T> queueNames(String... queueNames) {
			this.queueNames.addAll(Arrays.asList(queueNames));
			return this;
		}

		public Builder<T> queueNames(Collection<String> queueNames) {
			this.queueNames.addAll(queueNames);
			return this;
		}

		public Builder<T> componentFactories(
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

		public Builder<T> phase(Integer phase) {
			this.phase = phase;
			return this;
		}

		// @formatter:off
		public SqsMessageListenerContainer<T> build() {
			SqsMessageListenerContainer<T> container = new SqsMessageListenerContainer<>(this.sqsAsyncClient);
			ConfigUtils.INSTANCE
					.acceptIfNotNull(this.id, container::setId)
					.acceptIfNotNull(this.messageListener, container::setMessageListener)
					.acceptIfNotNull(this.asyncMessageListener, container::setAsyncMessageListener)
					.acceptIfNotNull(this.errorHandler, container::setErrorHandler)
					.acceptIfNotNull(this.asyncErrorHandler, container::setErrorHandler)
					.acceptIfNotNull(this.acknowledgementResultCallback, container::setAcknowledgementResultCallback)
					.acceptIfNotNull(this.asyncAcknowledgementResultCallback, container::setAcknowledgementResultCallback)
					.acceptIfNotNull(this.containerComponentFactories, container::setComponentFactories)
					.acceptIfNotEmpty(this.queueNames, container::setQueueNames)
					.acceptIfNotNullOrElse(container::setPhase, this.phase, DEFAULT_PHASE);
			this.messageInterceptors.forEach(container::addMessageInterceptor);
			this.asyncMessageInterceptors.forEach(container::addMessageInterceptor);

			container.configure(this.optionsConsumer);
			return container;
		}
		// @formatter:on

	}

}
