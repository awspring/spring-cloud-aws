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
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * {@link MessageListenerContainer} implementation for SQS queues.
 *
 * Components and {@link ContainerOptions} can be changed at runtime and such changes will be valid upon container
 * restart.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsMessageListenerContainer<T> extends AbstractPipelineMessageListenerContainer<T> {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageListenerContainer.class);

	private final SqsAsyncClient sqsAsyncClient;

	public SqsMessageListenerContainer(SqsAsyncClient sqsAsyncClient, ContainerOptions options) {
		super(options);
		Assert.notNull(sqsAsyncClient, "sqsAsyncClient cannot be null");
		this.sqsAsyncClient = sqsAsyncClient;
	}

	public SqsMessageListenerContainer(SqsAsyncClient sqsAsyncClient) {
		this(sqsAsyncClient, ContainerOptions.builder().build());
	}

	// @formatter:off
	@Override
	protected ContainerComponentFactory<T> createComponentFactory() {
		Assert.isTrue(getQueueNames().stream().map(this::isFifoQueue).distinct().count() == 1,
			"The container must contain either all FIFO or all Standard queues.");
		return isFifoQueue(getQueueNames().iterator().next())
			? new FifoSqsComponentFactory<>()
			: new StandardSqsComponentFactory<>();
	}
	// @formatter:on

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

		private ContainerComponentFactory<T> componentFactory;

		private AsyncMessageListener<T> asyncMessageListener;

		private MessageListener<T> messageListener;

		private String id;

		private AsyncErrorHandler<T> asyncErrorHandler;

		private ErrorHandler<T> errorHandler;

		private Consumer<ContainerOptions.Builder> options = options -> {
		};

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

		public Builder<T> messageInterceptors(MessageInterceptor<T> messageInterceptor) {
			this.messageInterceptors.add(messageInterceptor);
			return this;
		}

		public Builder<T> configure(Consumer<ContainerOptions.Builder> options) {
			this.options = options;
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
					.acceptIfNotNull(this.componentFactory, container::setComponentFactory)
					.acceptIfNotEmpty(this.queueNames, container::setQueueNames);
			this.messageInterceptors.forEach(container::addMessageInterceptor);
			this.asyncMessageInterceptors.forEach(container::addMessageInterceptor);
			container.configure(this.options);
			return container;
		}
		// @formatter:on

	}

}
