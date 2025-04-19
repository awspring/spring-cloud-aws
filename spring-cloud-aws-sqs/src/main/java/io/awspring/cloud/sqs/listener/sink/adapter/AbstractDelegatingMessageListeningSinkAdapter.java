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
package io.awspring.cloud.sqs.listener.sink.adapter;

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.LifecycleHandler;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.ObservableComponent;
import io.awspring.cloud.sqs.listener.SqsAsyncClientAware;
import io.awspring.cloud.sqs.listener.TaskExecutorAware;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingPipeline;
import io.awspring.cloud.sqs.listener.sink.MessageProcessingPipelineSink;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.support.observation.AbstractListenerObservation;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * {@link MessageProcessingPipelineSink} implementation that delegates method invocations to the provided delegate.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractDelegatingMessageListeningSinkAdapter<T>
		implements MessageProcessingPipelineSink<T>, TaskExecutorAware, SqsAsyncClientAware, ObservableComponent {

	private final MessageSink<T> delegate;

	/**
	 * Create an instance with the provided delegate.
	 * @param delegate the delegate.
	 */
	protected AbstractDelegatingMessageListeningSinkAdapter(MessageSink<T> delegate) {
		Assert.notNull(delegate, "delegate cannot be null");
		this.delegate = delegate;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setMessagePipeline(MessageProcessingPipeline<T> messageProcessingPipeline) {
		ConfigUtils.INSTANCE.acceptIfInstance(this.delegate, MessageProcessingPipelineSink.class,
				mpps -> mpps.setMessagePipeline(messageProcessingPipeline));
	}

	@Override
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		ConfigUtils.INSTANCE.acceptIfInstance(this.delegate, TaskExecutorAware.class,
				ea -> ea.setTaskExecutor(taskExecutor));
	}

	@Override
	public void setSqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
		ConfigUtils.INSTANCE.acceptIfInstance(this.delegate, SqsAsyncClientAware.class,
				saca -> saca.setSqsAsyncClient(sqsAsyncClient));
	}

	@Override
	public void setObservationSpecifics(AbstractListenerObservation.Specifics<?> observationSpecifics) {
		ConfigUtils.INSTANCE.acceptIfInstance(this.delegate, ObservableComponent.class,
				saca -> saca.setObservationSpecifics(observationSpecifics));
	}

	@Override
	public void start() {
		LifecycleHandler.get().start(this.delegate);
	}

	@Override
	public void stop() {
		LifecycleHandler.get().stop(this.delegate);
	}

	@Override
	public boolean isRunning() {
		return LifecycleHandler.get().isRunning(this.delegate);
	}

	@Override
	public void configure(ContainerOptions containerOptions) {
		this.delegate.configure(containerOptions);
	}

	protected MessageSink<T> getDelegate() {
		return this.delegate;
	}
}
