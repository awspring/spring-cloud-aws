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
package io.awspring.cloud.sqs.listener.sink.adapter;

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.LifecycleUtils;
import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingPipeline;
import io.awspring.cloud.sqs.listener.sink.AbstractMessageListeningSink;
import io.awspring.cloud.sqs.listener.sink.MessageProcessingPipelineSink;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.sink.TaskExecutorAwareComponent;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractDelegatingMessageListeningSinkAdapter<T> implements MessageProcessingPipelineSink<T>, TaskExecutorAwareComponent {

	private final MessageSink<T> delegate;

	protected AbstractDelegatingMessageListeningSinkAdapter(MessageSink<T> delegate) {
		Assert.notNull(delegate, "delegate cannot be null");
		this.delegate = delegate;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setMessagePipeline(MessageProcessingPipeline<T> messageProcessingPipeline) {
		ConfigUtils.INSTANCE
			.acceptIfInstance(this.delegate, MessageProcessingPipelineSink.class,
				mpps -> mpps.setMessagePipeline(messageProcessingPipeline));
	}

	@Override
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		ConfigUtils.INSTANCE
			.acceptIfInstance(this.delegate, TaskExecutorAwareComponent.class,
				teac -> teac.setTaskExecutor(taskExecutor));
	}

	@Override
	public void start() {
		LifecycleUtils.start(this.delegate);
	}

	@Override
	public void stop() {
		LifecycleUtils.stop(this.delegate);
	}

	@Override
	public boolean isRunning() {
		return LifecycleUtils.isRunning(this.delegate);
	}

	protected MessageSink<T> getDelegate() {
		return this.delegate;
	}
}
