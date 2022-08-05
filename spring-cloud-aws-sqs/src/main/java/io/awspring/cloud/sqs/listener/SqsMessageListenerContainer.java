/*
 * Copyright 2022 the original author or authors.
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
import io.awspring.cloud.sqs.SqsThreadFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AlwaysAcknowledgementHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.NeverAcknowledgementHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.OnSuccessAcknowledgementHandler;
import io.awspring.cloud.sqs.listener.pipeline.AcknowledgementHandlerExecutionStage;
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
import io.awspring.cloud.sqs.listener.source.AcknowledgingMessageSource;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import io.awspring.cloud.sqs.listener.source.PollingMessageSource;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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

	private Executor componentsTaskExecutor;

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

	private void configureComponents(ContainerComponentFactory<T> componentFactory) {
		this.componentsTaskExecutor = resolveTaskExecutor();
		getContainerOptions()
			.configure(this.messageSources)
			.configure(this.messageSink);
		configureMessageSources(componentFactory);
		configureMessageSink();
		configurePipelineComponents();
	}

	@SuppressWarnings("unchecked")
	private void configureMessageSources(ContainerComponentFactory<T> componentFactory) {
		ConfigUtils.INSTANCE
			.acceptMany(this.messageSources, source -> source.setMessageSink(this.messageSink))
			.acceptManyIfInstance(this.messageSources, SqsAsyncClientAware.class, asca -> asca.setSqsAsyncClient(this.sqsAsyncClient))
			.acceptManyIfInstance(this.messageSources, PollingMessageSource.class, pms -> pms.setBackPressureHandler(createBackPressureHandler()))
			.acceptManyIfInstance(this.messageSources, AcknowledgingMessageSource.class, ams -> ams.setAcknowledgementProcessor(componentFactory.createAcknowledgementProcessor(getContainerOptions())))
			.acceptManyIfInstance(this.messageSources, ExecutorAware.class, teac -> teac.setExecutor(createSourceTaskExecutor()));
	}

	@SuppressWarnings("unchecked")
	private void configureMessageSink() {
		ConfigUtils.INSTANCE
			.acceptIfInstance(this.messageSink, SqsAsyncClientAware.class, asca -> asca.setSqsAsyncClient(this.sqsAsyncClient))
			.acceptIfInstance(this.messageSink, ExecutorAware.class, teac -> teac.setExecutor(this.componentsTaskExecutor))
			.acceptIfInstance(this.messageSink, MessageProcessingPipelineSink.class, mls -> mls.setMessagePipeline(createMessageProcessingPipeline()));
	}

	private void configurePipelineComponents() {
		ConfigUtils.INSTANCE
			.acceptManyIfInstance(getMessageInterceptors(), ExecutorAware.class, teac -> teac.setExecutor(this.componentsTaskExecutor))
			.acceptIfInstance(getMessageListener(), ExecutorAware.class, teac -> teac.setExecutor(this.componentsTaskExecutor))
			.acceptIfInstance(getErrorHandler(), ExecutorAware.class, teac -> teac.setExecutor(this.componentsTaskExecutor));
	}

	private MessageProcessingPipeline<T> createMessageProcessingPipeline() {
		return MessageProcessingPipelineBuilder
			.<T>first(BeforeProcessingContextInterceptorExecutionStage::new)
			.then(BeforeProcessingInterceptorExecutionStage::new)
			.then(MessageListenerExecutionStage::new)
			.thenWrapWith(ErrorHandlerExecutionStage::new)
			.thenWrapWith(AcknowledgementHandlerExecutionStage::new)
			.then(AfterProcessingInterceptorExecutionStage::new)
			.thenWrapWith(AfterProcessingContextInterceptorExecutionStage::new)
			.build(MessageProcessingConfiguration.<T>builder()
				.interceptors(getMessageInterceptors())
				.messageListener(getMessageListener())
				.errorHandler(getErrorHandler())
				.ackHandler(createAcknowledgementHandler(getContainerOptions()))
				.build());
	}

	private Executor resolveTaskExecutor() {
		return getContainerOptions().getContainerComponentsTaskExecutor() != null
			? getContainerOptions().getContainerComponentsTaskExecutor()
			: createComponentsTaskExecutor();
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

	private Executor createSourceTaskExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setThreadNamePrefix(getId() + "#message_source");
		return executor;
	}

	private Executor createComponentsTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		int poolSize = getContainerOptions().getMaxInFlightMessagesPerQueue() * this.messageSources.size();
		executor.setMaxPoolSize(poolSize);
		executor.setCorePoolSize(getContainerOptions().getMessagesPerPoll());
		executor.setQueueCapacity(0);
		executor.setAllowCoreThreadTimeOut(true);
		executor.setThreadFactory(createSqsThreadFactory());
		executor.afterPropertiesSet();
		return executor;
	}

	private SqsThreadFactory createSqsThreadFactory() {
		SqsThreadFactory threadFactory = new SqsThreadFactory();
		threadFactory.setThreadNamePrefix(getId() + "-");
		return threadFactory;
	}

	private AcknowledgementHandler<T> createAcknowledgementHandler(ContainerOptions options) {
		AcknowledgementMode mode = options.getAcknowledgementMode();
		return AcknowledgementMode.ON_SUCCESS.equals(mode)
			? new OnSuccessAcknowledgementHandler<>()
			: AcknowledgementMode.ALWAYS.equals(mode)
			? new AlwaysAcknowledgementHandler<>()
			: new NeverAcknowledgementHandler<>();
	}

	@Override
	protected void doStop() {
		LifecycleUtils.stopParallel(this.messageSources, this.messageSink);
		shutdownComponentsTaskExecutor();
		logger.debug("Container {} stopped", getId());
	}

	private void shutdownComponentsTaskExecutor() {
		if (this.componentsTaskExecutor instanceof DisposableBean) {
			try {
				((DisposableBean) this.componentsTaskExecutor).destroy();
			}
			catch (Exception e) {
				throw new IllegalStateException("Error destroying TaskExecutor for sink in container " + getId());
			}
		}
		else if (this.componentsTaskExecutor instanceof ExecutorService) {
			((ExecutorService) this.componentsTaskExecutor).shutdownNow();
		}
	}

}
