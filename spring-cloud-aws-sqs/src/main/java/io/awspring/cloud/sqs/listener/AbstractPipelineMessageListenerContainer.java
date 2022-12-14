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
import io.awspring.cloud.sqs.LifecycleHandler;
import io.awspring.cloud.sqs.MessageExecutionThreadFactory;
import io.awspring.cloud.sqs.listener.pipeline.AcknowledgementHandlerExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.AfterProcessingContextInterceptorExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.AfterProcessingInterceptorExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.BeforeProcessingContextInterceptorExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.BeforeProcessingInterceptorExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.ErrorHandlerExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.MessageListenerExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingConfiguration;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingPipeline;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingPipelineBuilder;
import io.awspring.cloud.sqs.listener.sink.MessageProcessingPipelineSink;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.source.AcknowledgementProcessingMessageSource;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import io.awspring.cloud.sqs.listener.source.PollingMessageSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Base {@link MessageListenerContainer} implementation for managing {@link org.springframework.messaging.Message}
 * instances' lifecycles.
 *
 * This container uses a {@link MessageSource} to create the {@link org.springframework.messaging.Message} instances,
 * which are forwarded to a {@link MessageSink} and finally emitted to a {@link MessageProcessingPipeline}.
 *
 * The pipeline has several stages for processing the messages and executing logic in components such as
 * {@link AsyncMessageListener}, {@link io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler} and
 * {@link io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor}.
 *
 * Such components are created by the {@link ContainerComponentFactory} and the container manages their lifecycles.
 *
 * Components and {@link ContainerOptions} can be changed at runtime and such changes will be valid upon container
 * restart.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractPipelineMessageListenerContainer<T> extends AbstractMessageListenerContainer<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPipelineMessageListenerContainer.class);

	private Collection<MessageSource<T>> messageSources;

	private MessageSink<T> messageSink;

	private TaskExecutor componentsTaskExecutor;

	@Nullable
	private TaskExecutor acknowledgementResultTaskExecutor;

	protected AbstractPipelineMessageListenerContainer(ContainerOptions options) {
		super(options);
	}

	@Override
	protected void doStart() {
		ContainerComponentFactory<T> componentFactory = determineComponentFactory();
		this.messageSources = createMessageSources(componentFactory);
		this.messageSink = componentFactory.createMessageSink(getContainerOptions());
		configureComponents(componentFactory);
		LifecycleHandler.get().start(this.messageSink, this.messageSources);
	}

	// @formatter:off
	private ContainerComponentFactory<T> determineComponentFactory() {
		return getComponentFactories()
			.stream()
			.filter(factory -> factory.supports(getQueueNames(), getContainerOptions()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("No ContainerComponentFactory found for queues " + getQueueNames()));
	}

	private Collection<ContainerComponentFactory<T>> getComponentFactories() {
		return getContainerComponentFactories() != null
			? getContainerComponentFactories()
			: getDefaultComponentFactories();
	}

	protected abstract Collection<ContainerComponentFactory<T>> getDefaultComponentFactories();

	protected Collection<MessageSource<T>> createMessageSources(ContainerComponentFactory<T> componentFactory) {
		List<String> queueNames = new ArrayList<>(getQueueNames());
		return IntStream.range(0, queueNames.size())
				.mapToObj(index -> createMessageSource(queueNames.get(index), index, componentFactory))
				.collect(Collectors.toList());
	}

	protected MessageSource<T> createMessageSource(String queueName, int index,
			ContainerComponentFactory<T> componentFactory) {
		MessageSource<T> messageSource = componentFactory.createMessageSource(getContainerOptions());
		ConfigUtils.INSTANCE
			.acceptIfInstance(messageSource, PollingMessageSource.class,
				pms -> pms.setPollingEndpointName(queueName))
			.acceptIfInstance(messageSource, IdentifiableContainerComponent.class,
				icc -> icc.setId(getId() + "-" + index));
		return messageSource;
	}

	private void configureComponents(ContainerComponentFactory<T> componentFactory) {
		getContainerOptions()
			.configure(this.messageSources)
			.configure(this.messageSink);
		this.componentsTaskExecutor = resolveComponentsTaskExecutor();
		configureMessageSources(componentFactory);
		configureMessageSink(createMessageProcessingPipeline(componentFactory));
		configureContainerComponents();
	}
	// @formatter:on

	@SuppressWarnings("unchecked")
	protected void configureMessageSources(ContainerComponentFactory<T> componentFactory) {
		TaskExecutor taskExecutor = createSourcesTaskExecutor();
		ConfigUtils.INSTANCE.acceptMany(this.messageSources, source -> source.setMessageSink(this.messageSink))
				.acceptManyIfInstance(this.messageSources, PollingMessageSource.class,
						pms -> pms.setBackPressureHandler(createBackPressureHandler()))
				.acceptManyIfInstance(this.messageSources, AcknowledgementProcessingMessageSource.class,
						ams -> ams.setAcknowledgementProcessor(
								componentFactory.createAcknowledgementProcessor(getContainerOptions())))
				.acceptManyIfInstance(this.messageSources, AcknowledgementProcessingMessageSource.class,
						ams -> ams.setAcknowledgementResultCallback(getAcknowledgementResultCallback()))
				.acceptManyIfInstance(this.messageSources, TaskExecutorAware.class,
						teac -> teac.setTaskExecutor(taskExecutor));
		doConfigureMessageSources(this.messageSources);
	}

	protected void doConfigureMessageSources(Collection<MessageSource<T>> messageSources) {
	};

	@SuppressWarnings("unchecked")
	protected void configureMessageSink(MessageProcessingPipeline<T> messageProcessingPipeline) {
		ConfigUtils.INSTANCE
				.acceptIfInstance(this.messageSink, IdentifiableContainerComponent.class, icc -> icc.setId(getId()))
				.acceptIfInstance(this.messageSink, TaskExecutorAware.class,
						teac -> teac.setTaskExecutor(getComponentsTaskExecutor()))
				.acceptIfInstance(this.messageSink, MessageProcessingPipelineSink.class,
						mls -> mls.setMessagePipeline(messageProcessingPipeline));
		doConfigureMessageSink(this.messageSink);
	}

	protected void doConfigureMessageSink(MessageSink<T> messageSink) {
	}

	protected void configureContainerComponents() {
		ConfigUtils.INSTANCE
				.acceptManyIfInstance(getMessageInterceptors(), TaskExecutorAware.class,
						teac -> teac.setTaskExecutor(getComponentsTaskExecutor()))
				.acceptIfInstance(getMessageListener(), TaskExecutorAware.class,
						teac -> teac.setTaskExecutor(getComponentsTaskExecutor()))
				.acceptIfInstance(getErrorHandler(), TaskExecutorAware.class,
						teac -> teac.setTaskExecutor(getComponentsTaskExecutor()))
				.acceptIfInstance(getAcknowledgementResultCallback(), TaskExecutorAware.class,
						teac -> teac.setTaskExecutor(getAcknowledgementResultTaskExecutor()));
	}

	// @formatter:off
	protected MessageProcessingPipeline<T> createMessageProcessingPipeline(
			ContainerComponentFactory<T> componentFactory) {
		return MessageProcessingPipelineBuilder.
			<T> first(BeforeProcessingContextInterceptorExecutionStage::new)
				.then(BeforeProcessingInterceptorExecutionStage::new)
				.then(MessageListenerExecutionStage::new)
				.thenInTheFuture(ErrorHandlerExecutionStage::new)
				.thenInTheFuture(AfterProcessingInterceptorExecutionStage::new)
				.thenInTheFuture(AfterProcessingContextInterceptorExecutionStage::new)
				.thenInTheFuture(AcknowledgementHandlerExecutionStage::new)
				.build(MessageProcessingConfiguration.<T> builder()
					.interceptors(getMessageInterceptors())
					.messageListener(getMessageListener())
					.errorHandler(getErrorHandler())
					.ackHandler(componentFactory.createAcknowledgementHandler(getContainerOptions()))
					.build());
	}
	// @formatter:on

	private TaskExecutor resolveComponentsTaskExecutor() {
		return getContainerOptions().getComponentsTaskExecutor() != null
				? getContainerOptions().getComponentsTaskExecutor()
				: createTaskExecutor();
	}

	protected BackPressureHandler createBackPressureHandler() {
		return SemaphoreBackPressureHandler.builder().batchSize(getContainerOptions().getMaxMessagesPerPoll())
				.totalPermits(getContainerOptions().getMaxInFlightMessagesPerQueue())
				.acquireTimeout(getContainerOptions().getPermitAcquireTimeout())
				.throughputConfiguration(getContainerOptions().getBackPressureMode()).build();
	}

	protected TaskExecutor createSourcesTaskExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setThreadNamePrefix(getId() + "#message_source-");
		return executor;
	}

	protected TaskExecutor createTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		int poolSize = getContainerOptions().getMaxInFlightMessagesPerQueue() * this.messageSources.size();
		executor.setMaxPoolSize(poolSize);
		executor.setCorePoolSize(getContainerOptions().getMaxMessagesPerPoll());
		executor.setQueueCapacity(0);
		executor.setAllowCoreThreadTimeOut(true);
		executor.setThreadFactory(createThreadFactory());
		executor.afterPropertiesSet();
		return executor;
	}

	protected ThreadFactory createThreadFactory() {
		MessageExecutionThreadFactory threadFactory = new MessageExecutionThreadFactory();
		threadFactory.setThreadNamePrefix(getId() + "-");
		return threadFactory;
	}

	@Override
	protected void doStop() {
		LifecycleHandler.get().stop(this.messageSources, this.messageSink);
		shutdownComponentsTaskExecutor();
		logger.debug("Container {} stopped", getId());
	}

	protected TaskExecutor getComponentsTaskExecutor() {
		return this.componentsTaskExecutor;
	}

	protected TaskExecutor getAcknowledgementResultTaskExecutor() {
		if (this.acknowledgementResultTaskExecutor == null) {
			this.acknowledgementResultTaskExecutor = createTaskExecutor();
		}
		return this.acknowledgementResultTaskExecutor;
	}

	private void shutdownComponentsTaskExecutor() {
		if (!this.componentsTaskExecutor.equals(getContainerOptions().getComponentsTaskExecutor())) {
			LifecycleHandler.get().dispose(getComponentsTaskExecutor());
		}
		if (this.acknowledgementResultTaskExecutor != null && !this.acknowledgementResultTaskExecutor
				.equals(getContainerOptions().getAcknowledgementResultTaskExecutor())) {
			LifecycleHandler.get().dispose(getAcknowledgementResultTaskExecutor());
		}
	}

}
