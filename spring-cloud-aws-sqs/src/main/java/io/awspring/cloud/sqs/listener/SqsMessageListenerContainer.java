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
import io.awspring.cloud.sqs.LifecycleUtils;
import io.awspring.cloud.sqs.listener.pipeline.AckHandlerExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.AfterProcessingContextInterceptorExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.BeforeProcessingContextInterceptorExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.ErrorHandlerExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.BeforeProcessingInterceptorExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.AfterProcessingInterceptorExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.MessageListenerExecutionStage;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingConfiguration;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingPipeline;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingPipelineBuilder;
import io.awspring.cloud.sqs.listener.sink.MessageProcessingPipelineSink;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import io.awspring.cloud.sqs.listener.source.PollingMessageSource;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
public class SqsMessageListenerContainer<T> extends AbstractMessageListenerContainer<T> {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageListenerContainer.class);

	private final SqsAsyncClient sqsAsyncClient;

	private Collection<MessageSource<T>> messageSources;

	private MessageSink<T> messageSink;

	private TaskExecutor sinkTaskExecutor;

	public SqsMessageListenerContainer(SqsAsyncClient sqsAsyncClient, ContainerOptions options) {
		super(options);
		this.sqsAsyncClient = sqsAsyncClient;
	}

	@Override
	protected void doStart() {
		ContainerComponentFactory<T> componentFactory = determineComponentFactory();
		this.messageSources = createMessageSources(componentFactory);
		this.messageSink = componentFactory.createMessageSink(getContainerOptions());
		configureComponents(componentFactory);
		LifecycleUtils.startParallel(this.messageSink, this.messageSources);
	}

	private ContainerComponentFactory<T> determineComponentFactory() {
		return getContainerComponentFactory() != null
			? getContainerComponentFactory()
			: createComponentFactory();
	}

	private ContainerComponentFactory<T> createComponentFactory() {
		Assert.isTrue(getQueueNames().stream().map(this::isFifoQueue).distinct().count() == 1,
			"The container must contain either all FIFO or all Standard queues.");
		return isFifoQueue(getQueueNames().iterator().next())
			? new FifoSqsComponentFactory<>()
			: new StandardSqsComponentFactory<>();
	}

	private boolean isFifoQueue(String name) {
		return name.endsWith(".fifo");
	}

	private Collection<MessageSource<T>> createMessageSources(ContainerComponentFactory<T> componentFactory) {
		return getQueueNames()
			.stream()
			.map(queueName -> createMessageSource(queueName, componentFactory))
			.collect(Collectors.toList());
	}

	private MessageSource<T> createMessageSource(String queueName, ContainerComponentFactory<T> componentFactory) {
		MessageSource<T> messageSource = componentFactory.createMessageSource(getContainerOptions());
		ConfigUtils.INSTANCE
			.acceptIfInstance(messageSource, PollingMessageSource.class, pms -> pms.setPollingEndpointName(queueName));
		return messageSource;
	}

	@SuppressWarnings("unchecked")
	private void configureComponents(ContainerComponentFactory<T> componentFactory) {
		getContainerOptions()
			.configure(this.messageSources)
			.configure(this.messageSink);
		ConfigUtils.INSTANCE
			.acceptMany(this.messageSources, source -> source.setMessageSink(this.messageSink))
			.acceptManyIfInstance(this.messageSources, SqsAsyncClientAware.class, asca -> asca.setSqsAsyncClient(this.sqsAsyncClient))
			.acceptManyIfInstance(this.messageSources, PollingMessageSource.class, pms -> pms.setBackPressureHandler(createBackPressureHandler()))
			.acceptManyIfInstance(this.messageSources, TaskExecutorAware.class, teac -> teac.setTaskExecutor(createSourceTaskExecutor()))
			.acceptIfInstance(this.messageSink, SqsAsyncClientAware.class, asca -> asca.setSqsAsyncClient(this.sqsAsyncClient))
			.acceptIfInstance(this.messageSink, TaskExecutorAware.class, teac -> teac.setTaskExecutor(getOrCreateSinkTaskExecutor()))
			.acceptIfInstance(this.messageSink, MessageProcessingPipelineSink.class, mls -> mls.setMessagePipeline(createMessageProcessingPipeline(componentFactory)));
	}

	private MessageProcessingPipeline<T> createMessageProcessingPipeline(ContainerComponentFactory<T> componentFactory) {
		return MessageProcessingPipelineBuilder
			.<T>first(BeforeProcessingContextInterceptorExecutionStage::new)
			.then(BeforeProcessingInterceptorExecutionStage::new)
			.then(MessageListenerExecutionStage::new)
			.thenWrapWith(ErrorHandlerExecutionStage::new)
			.thenWrapWith(AckHandlerExecutionStage::new)
			.then(AfterProcessingInterceptorExecutionStage::new)
			.thenWrapWith(AfterProcessingContextInterceptorExecutionStage::new)
			.build(MessageProcessingConfiguration.<T>builder()
				.interceptors(getMessageInterceptors())
				.messageListener(getMessageListener())
				.errorHandler(getErrorHandler())
				.ackHandler(componentFactory.createAckHandler(getContainerOptions())).build());
	}

	private SemaphoreBackPressureHandler createBackPressureHandler() {
		return SemaphoreBackPressureHandler
			.builder()
			.batchSize(getContainerOptions().getMessagesPerPoll())
			.totalPermits(getContainerOptions().getMaxInFlightMessagesPerQueue())
			.acquireTimeout(getContainerOptions().getPermitAcquireTimeout())
			.throughputConfiguration(getContainerOptions().getBackPressureMode())
			.build();
	}

	private TaskExecutor createSourceTaskExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setThreadNamePrefix(getId() + "#message_source-");
		return executor;
	}

	private TaskExecutor getOrCreateSinkTaskExecutor() {
		return getContainerOptions().getSinkTaskExecutor() != null
			? getContainerOptions().getSinkTaskExecutor()
			: createSinkTaskExecutor();
	}

	private ThreadPoolTaskExecutor createSinkTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		int poolSize = getContainerOptions().getMaxInFlightMessagesPerQueue() * this.messageSources.size();
		executor.setMaxPoolSize(poolSize);
		executor.setCorePoolSize(getContainerOptions().getMessagesPerPoll());
		executor.setQueueCapacity(0);
		executor.setThreadNamePrefix(getId() + "#message_sink-");
		executor.afterPropertiesSet();
		this.sinkTaskExecutor = executor;
		return executor;
	}

	@Override
	protected void doStop() {
		LifecycleUtils.stopParallel(this.messageSources, this.messageSink);
		disposeSinkTaskExecutor();
		logger.debug("Container {} stopped", getId());
	}

	private void disposeSinkTaskExecutor() {
		if (this.sinkTaskExecutor instanceof DisposableBean) {
			try {
				((DisposableBean) this.sinkTaskExecutor).destroy();
			}
			catch (Exception e) {
				throw new IllegalStateException("Error destroying TaskExecutor for sink in container " + getId());
			}
		}
	}

}
