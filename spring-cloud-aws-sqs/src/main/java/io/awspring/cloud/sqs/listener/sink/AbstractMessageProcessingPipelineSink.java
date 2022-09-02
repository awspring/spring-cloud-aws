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
package io.awspring.cloud.sqs.listener.sink;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.TaskExecutorAware;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingPipeline;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;

/**
 * Base implementation for {@link MessageProcessingPipelineSink} containing {@link SmartLifecycle} features and useful
 * execution methods that can be used by subclasses.
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractMessageProcessingPipelineSink<T>
		implements MessageProcessingPipelineSink<T>, TaskExecutorAware {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMessageProcessingPipelineSink.class);

	private final Object lifecycleMonitor = new Object();

	private volatile boolean running;

	private Executor taskExecutor;

	private MessageProcessingPipeline<T> messageProcessingPipeline;

	private String id;

	@Override
	public void setMessagePipeline(MessageProcessingPipeline<T> messageProcessingPipeline) {
		Assert.notNull(messageProcessingPipeline, "messageProcessingPipeline must not be null.");
		this.messageProcessingPipeline = messageProcessingPipeline;
	}

	@Override
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "executor cannot be null");
		this.taskExecutor = taskExecutor;
	}

	@Override
	public CompletableFuture<Void> emit(Collection<Message<T>> messages, MessageProcessingContext<T> context) {
		Assert.notNull(messages, "messages cannot be null");
		if (!isRunning()) {
			logger.debug("{} {} not running, returning", getClass().getSimpleName(), this.id);
			return CompletableFuture.completedFuture(null);
		}
		if (messages.size() == 0) {
			logger.debug("No messages provided for {} {}, returning.", getClass().getSimpleName(), this.id);
			return CompletableFuture.completedFuture(null);
		}
		return doEmit(messages, context);
	}

	protected abstract CompletableFuture<Void> doEmit(Collection<Message<T>> messages,
			MessageProcessingContext<T> context);

	/**
	 * Send the provided {@link Message} to the {@link TaskExecutor} as a unit of work.
	 * @param message the message to be executed.
	 * @param context the processing context.
	 * @return the processing result.
	 */
	protected CompletableFuture<Void> execute(Message<T> message, MessageProcessingContext<T> context) {
		logger.trace("Executing message {}", MessageHeaderUtils.getId(message));
		StopWatch watch = getStartedWatch();
		return doExecute(() -> this.messageProcessingPipeline.process(message, context))
				.whenComplete((v, t) -> context.runBackPressureReleaseCallback())
				.whenComplete((v, t) -> measureExecution(watch, Collections.singletonList(message)));
	}

	/**
	 * Send the provided {@link Message} instances to the {@link TaskExecutor} as a unit of work.
	 * @param messages the messages to be executed.
	 * @param context the processing context.
	 * @return the processing result.
	 */
	protected CompletableFuture<Void> execute(Collection<Message<T>> messages, MessageProcessingContext<T> context) {
		StopWatch watch = getStartedWatch();
		return doExecute(() -> this.messageProcessingPipeline.process(messages, context))
				.whenComplete((v, t) -> messages.forEach(msg -> context.runBackPressureReleaseCallback()))
				.whenComplete((v, t) -> measureExecution(watch, messages));
	}

	protected Void logError(Throwable t, Message<T> msg) {
		logger.error("Error processing message {}.", MessageHeaderUtils.getId(msg), t);
		return null;
	}

	protected Void logError(Throwable t, Collection<Message<T>> msgs) {
		logger.error("Error processing message {}.", MessageHeaderUtils.getId(msgs), t);
		return null;
	}

	private StopWatch getStartedWatch() {
		StopWatch watch = new StopWatch();
		watch.start();
		return watch;
	}

	private void measureExecution(StopWatch watch, Collection<Message<T>> messages) {
		watch.stop();
		if (logger.isTraceEnabled()) {
			logger.trace("Messages {} processed in {}ms", MessageHeaderUtils.getId(messages),
					watch.getTotalTimeMillis());
		}
	}

	private CompletableFuture<Void> doExecute(Supplier<CompletableFuture<?>> supplier) {
		return CompletableFuture.supplyAsync(supplier, this.taskExecutor).thenCompose(x -> x).thenRun(() -> {
		});
	}

	@Override
	public void start() {
		if (isRunning()) {
			logger.debug("{} {} already running", getClass().getSimpleName(), this.id);
			return;
		}
		synchronized (this.lifecycleMonitor) {
			Assert.notNull(this.messageProcessingPipeline, "messageListener not set");
			Assert.notNull(this.taskExecutor, "taskExecutor not set");
			this.id = getOrCreateId();
			logger.debug("Starting {} {}", getClass().getSimpleName(), this.id);
			this.running = true;
		}
	}

	private String getOrCreateId() {
		return this.taskExecutor instanceof ThreadPoolTaskExecutor
				? ((ThreadPoolTaskExecutor) this.taskExecutor).getThreadNamePrefix()
				: UUID.randomUUID().toString();
	}

	@Override
	public void stop() {
		if (!isRunning()) {
			logger.debug("{} {} already stopped", getClass().getSimpleName(), this.id);
			return;
		}
		synchronized (this.lifecycleMonitor) {
			logger.debug("Stopping {} {}", this.getClass().getSimpleName(), this.id);
			this.running = false;
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

}
