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
package io.awspring.cloud.sqs.listener.acknowledgement;

import io.awspring.cloud.sqs.LifecycleHandler;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.TaskExecutorAware;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;

/**
 * {@link io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor} implementation that adds the messages
 * to a {@link BlockingQueue} to be acknowledged according to {@link ContainerOptions#getAcknowledgementInterval()} and
 * {@link ContainerOptions#getAcknowledgementThreshold()}.
 *
 * The messages are constantly polled from the queue and added to a buffer. When a message is polled, the processor
 * checks the queue size against the configured threshold and sends batches for execution if the threshold is breached.
 *
 * A separate scheduled thread is activated when the configured amount of time has passed between the last
 * acknowledgement execution. This thread then empties the buffer and sends all messages to execution.
 *
 * All buffer access must be synchronized by the {@link Lock}.
 *
 * When this processor is signaled to {@link SmartLifecycle#stop()}, it waits for up to 20 seconds for ongoing
 * acknowledgement executions to complete. After that time, it will cancel all executions and return the flow to the
 * caller.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class BatchingAcknowledgementProcessor<T> extends AbstractOrderingAcknowledgementProcessor<T>
		implements TaskExecutorAware {

	private static final Logger logger = LoggerFactory.getLogger(BatchingAcknowledgementProcessor.class);

	private BufferingAcknowledgementProcessor<T> acknowledgementProcessor;

	private BlockingQueue<Message<T>> acks;

	private Integer ackThreshold;

	private Duration ackInterval;

	private TaskExecutor taskExecutor;

	private TaskScheduler taskScheduler;

	private Duration shutdownTimeout;

	@Override
	protected void doConfigure(ContainerOptions containerOptions) {
		this.ackInterval = containerOptions.getAcknowledgementInterval();
		this.ackThreshold = containerOptions.getAcknowledgementThreshold();
		this.shutdownTimeout = containerOptions.getShutdownTimeout();
	}

	@Override
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "taskExecutor cannot be null");
		this.taskExecutor = taskExecutor;
	}

	@Override
	protected CompletableFuture<Void> doOnAcknowledge(Message<T> message) {
		if (!this.acks.offer(message)) {
			logger.warn("Acknowledgement queue full, dropping acknowledgement for message {}",
					MessageHeaderUtils.getId(message));
		}
		logger.trace("Received message {} to ack in {}.", MessageHeaderUtils.getId(message), getId());
		return CompletableFuture.completedFuture(null);
	}

	@Override
	protected CompletableFuture<Void> doOnAcknowledge(Collection<Message<T>> messages) {
		messages.forEach(this::onAcknowledge);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void doStart() {
		Assert.notNull(this.ackInterval, "ackInterval not set");
		Assert.notNull(this.ackThreshold, "ackThreshold not set");
		Assert.notNull(this.taskExecutor, "executor not set");
		Assert.state(this.ackInterval != Duration.ZERO || this.ackThreshold > 0,
				() -> getClass().getSimpleName() + " cannot be used with Duration.ZERO and acknowledgement threshold 0."
						+ "Consider using a " + ImmediateAcknowledgementProcessor.class + "instead");
		this.acks = new LinkedBlockingQueue<>();
		this.taskScheduler = createTaskScheduler();
		this.acknowledgementProcessor = createAcknowledgementProcessor();
		this.taskExecutor.execute(this.acknowledgementProcessor);
	}

	protected TaskScheduler createTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix(getId() + "-");
		scheduler.initialize();
		return scheduler;
	}

	protected BufferingAcknowledgementProcessor<T> createAcknowledgementProcessor() {
		return new BufferingAcknowledgementProcessor<>(this);
	}

	@Override
	public void doStop() {
		this.acknowledgementProcessor.waitAcknowledgementsToFinish();
		LifecycleHandler.get().dispose(this.taskScheduler);
	}

	private static class BufferingAcknowledgementProcessor<T> implements Runnable {

		private final BlockingQueue<Message<T>> acks;

		private final Integer ackThreshold;

		private final BatchingAcknowledgementProcessor<T> parent;

		private final Map<String, BlockingQueue<Message<T>>> acksBuffer;

		private final Duration ackShutdownTimeout;

		private final AcknowledgementExecutionContext<T> context;

		private final ScheduledAcknowledgementExecution<T> scheduledExecution;

		private final ThresholdAcknowledgementExecutor<T> thresholdAcknowledgementExecution;

		private final Function<Message<T>, String> messageGroupingFunction;

		private BufferingAcknowledgementProcessor(BatchingAcknowledgementProcessor<T> parent) {
			this.acks = parent.acks;
			this.ackThreshold = parent.ackThreshold;
			this.ackShutdownTimeout = parent.shutdownTimeout;
			this.parent = parent;
			this.acksBuffer = new ConcurrentHashMap<>();
			this.messageGroupingFunction = parent.getMessageGroupingFunction();
			this.context = new AcknowledgementExecutionContext<>(parent.getId(), this.acksBuffer, new ReentrantLock(),
					parent::isRunning, parent::sendToExecutor);
			this.scheduledExecution = new ScheduledAcknowledgementExecution<>(parent.ackInterval, parent.taskScheduler,
					this.context);
			this.thresholdAcknowledgementExecution = new ThresholdAcknowledgementExecutor<>(parent.ackThreshold,
					this.context);

		}

		@Override
		public void run() {
			logger.debug("Starting acknowledgement processor thread with batchSize: {}", this.ackThreshold);
			this.scheduledExecution.start();
			while (this.parent.isRunning()) {
				try {
					Message<T> polledMessage = this.acks.poll(1, TimeUnit.SECONDS);
					if (polledMessage != null) {
						this.acksBuffer.computeIfAbsent(this.messageGroupingFunction.apply(polledMessage),
								newGroup -> new LinkedBlockingQueue<>()).add(polledMessage);
						this.thresholdAcknowledgementExecution.checkAndExecute();
					}
				}
				catch (Exception e) {
					logger.error("Error while handling acknowledgements for {}, resuming.", this.parent.getId(), e);
				}
			}
			logger.debug("Acknowledgement processor thread stopped");
		}

		public void waitAcknowledgementsToFinish() {
			try {
				CompletableFuture.allOf(this.context.runningAcks.toArray(new CompletableFuture[] {}))
						.get(this.ackShutdownTimeout.toMillis(), TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while waiting for acks to finish");
			}
			catch (TimeoutException e) {
				logger.warn("Acknowledgements did not finish in {} ms. Proceeding with shutdown.",
						this.ackShutdownTimeout.toMillis());
			}
			catch (Exception e) {
				logger.warn(
						"Error thrown when waiting for acknowledgement tasks to finish in {}. Continuing with shutdown.",
						this.parent.getId(), e);
			}
			if (!this.context.runningAcks.isEmpty()) {
				this.context.runningAcks.forEach(future -> future.cancel(true));
			}
		}

	}

	private static class AcknowledgementExecutionContext<T> {

		private final String id;

		private final Lock ackLock;

		private final Supplier<Boolean> runningFunction;

		private final Map<String, BlockingQueue<Message<T>>> acksBuffer;

		private final Function<Collection<Message<T>>, CompletableFuture<Void>> executingFunction;

		private final Collection<CompletableFuture<Void>> runningAcks = Collections.synchronizedSet(new HashSet<>());

		private Instant lastAcknowledgement = Instant.now();

		public AcknowledgementExecutionContext(String id, Map<String, BlockingQueue<Message<T>>> acksBuffer,
				Lock ackLock, Supplier<Boolean> runningFunction,
				Function<Collection<Message<T>>, CompletableFuture<Void>> executingFunction) {
			this.id = id;
			this.acksBuffer = acksBuffer;
			this.ackLock = ackLock;
			this.runningFunction = runningFunction;
			this.executingFunction = executingFunction;
		}

		private List<CompletableFuture<Void>> executeAcksUpTo(int minSize, int maxSize) {
			verifyLock();
			List<CompletableFuture<Void>> futures = this.acksBuffer.entrySet().stream()
					.filter(entry -> entry.getValue().size() >= minSize)
					.map(entry -> doExecute(entry.getKey(), entry.getValue(),
							maxSize == Integer.MAX_VALUE ? entry.getValue().size() : maxSize))
					.collect(Collectors.toList());
			if (!futures.isEmpty()) {
				purgeEmptyBuffers();
				return Collections.singletonList(null);
			}
			return Collections.emptyList();
		}

		private List<CompletableFuture<Void>> executeAllAcks() {
			verifyLock();
			List<CompletableFuture<Void>> futures = this.acksBuffer.entrySet().stream()
					.filter(entry -> entry.getValue().size() > 0)
					.map(entry -> doExecute(entry.getKey(), entry.getValue(), entry.getValue().size()))
					.collect(Collectors.toList());
			if (!futures.isEmpty()) {
				purgeEmptyBuffers();
			}
			return futures;
		}

		private void verifyLock() {
			if (this.ackLock instanceof ReentrantLock) {
				Assert.isTrue(((ReentrantLock) this.ackLock).isHeldByCurrentThread(),
						"no lock for executing acknowledgements");
			}
		}

		private CompletableFuture<Void> doExecute(String groupKey, BlockingQueue<Message<T>> messages, int maxSize) {
			logger.trace("Executing acknowledgement for up to {} messages {} of group {} in {}.", maxSize,
					MessageHeaderUtils.getId(messages), groupKey, this.id);
			List<Message<T>> messagesToAck = pollUpToThreshold(groupKey, messages, maxSize);
			CompletableFuture<Void> future = manageFuture(execute(messagesToAck));
			this.lastAcknowledgement = Instant.now();
			return future;
		}

		private List<Message<T>> pollUpToThreshold(String groupKey, BlockingQueue<Message<T>> messages, int maxSize) {
			return IntStream.range(0, maxSize).mapToObj(index -> pollMessage(groupKey, messages))
					.collect(Collectors.toList());
		}

		private Message<T> pollMessage(String groupKey, BlockingQueue<Message<T>> messages) {
			Message<T> polledMessage = messages.poll();
			Assert.notNull(polledMessage, "poll should never return null");
			logger.trace("Retrieved message {} from the queue for group {}. Queue size: {}",
					MessageHeaderUtils.getId(polledMessage), groupKey, messages.size());
			return polledMessage;
		}

		private CompletableFuture<Void> execute(Collection<Message<T>> messages) {
			Assert.notEmpty(messages, "empty collection sent for acknowledgement");
			logger.trace("Executing {} acknowledgements for {}", messages.size(), this.id);
			return this.executingFunction.apply(messages);
		}

		private CompletableFuture<Void> manageFuture(CompletableFuture<Void> future) {
			this.runningAcks.add(future);
			future.whenComplete((v, t) -> {
				if (isRunning()) {
					this.runningAcks.remove(future);
				}
			});
			return future;
		}

		private boolean isRunning() {
			return runningFunction.get();
		}

		private void purgeEmptyBuffers() {
			lock();
			try {
				List<String> emptyAcks = this.acksBuffer.entrySet().stream().filter(entry -> entry.getValue().isEmpty())
						.map(Map.Entry::getKey).collect(Collectors.toList());
				logger.trace("Removing groups {} from buffer in {}", emptyAcks, this.id);
				emptyAcks.forEach(this.acksBuffer::remove);
			}
			finally {
				unlock();
			}
		}

		private void lock() {
			this.ackLock.lock();
		}

		private void unlock() {
			this.ackLock.unlock();
		}

	}

	private static class ThresholdAcknowledgementExecutor<T> {

		private final AcknowledgementExecutionContext<T> context;

		private final int ackThreshold;

		public ThresholdAcknowledgementExecutor(int ackThreshold, AcknowledgementExecutionContext<T> context) {
			this.context = context;
			this.ackThreshold = ackThreshold;
		}

		private void checkAndExecute() {
			if (this.ackThreshold == 0) {
				return;
			}
			// Will eventually finish since we're not buffering new messages
			while (!executeThresholdAcks().isEmpty())
				;
		}

		private List<CompletableFuture<Void>> executeThresholdAcks() {
			this.context.lock();
			try {
				logger.trace("Executing acknowledgement for threshold in {}.", this.context.id);
				return this.context.executeAcksUpTo(this.ackThreshold, this.ackThreshold);
			}
			finally {
				this.context.unlock();
			}
		}
	}

	private static class ScheduledAcknowledgementExecution<T> {

		private final AcknowledgementExecutionContext<T> context;

		private final TaskScheduler taskScheduler;

		private final Duration ackInterval;

		public ScheduledAcknowledgementExecution(Duration ackInterval, TaskScheduler taskScheduler,
				AcknowledgementExecutionContext<T> context) {
			this.ackInterval = ackInterval;
			this.taskScheduler = taskScheduler;
			this.context = context;
		}

		private void start() {
			if (this.ackInterval != Duration.ZERO) {
				logger.debug("Starting scheduled thread with interval of {}ms for {}", this.ackInterval.toMillis(),
						this.context.id);
				scheduleNextExecution(Instant.now().plus(this.ackInterval));
			}
		}

		private void scheduleNextExecution(Instant nextExecutionDelay) {
			if (!this.context.isRunning()) {
				logger.debug("AcknowledgementProcessor {} stopped, not scheduling next acknowledgement execution.",
						this.context.id);
				return;
			}
			try {
				logger.trace("Scheduling next acknowledgement execution in {}ms",
						nextExecutionDelay.toEpochMilli() - Instant.now().toEpochMilli());
				this.taskScheduler.schedule(this::executeScheduledAcknowledgement, nextExecutionDelay);
			}
			catch (Exception e) {
				if (this.context.isRunning()) {
					logger.warn("Error thrown when scheduling next execution in {}. Resuming.", this.context.id, e);
				}
				scheduleNextExecution(this.context.lastAcknowledgement.plus(this.ackInterval));
			}
		}

		private void executeScheduledAcknowledgement() {
			this.context.lock();
			try {
				pollAndExecuteScheduled();
				scheduleNextExecution(this.context.lastAcknowledgement.plus(this.ackInterval));
			}
			catch (Exception e) {
				logger.error("Error executing scheduled acknowledgement in {}. Resuming.", this.context.id, e);
				scheduleNextExecution(this.context.lastAcknowledgement.plus(this.ackInterval));
			}
			finally {
				this.context.unlock();
			}
		}

		private void pollAndExecuteScheduled() {
			if (Instant.now().isAfter(this.context.lastAcknowledgement.plus(this.ackInterval))) {
				List<CompletableFuture<Void>> executionFutures = this.context.executeAllAcks();
				if (executionFutures.isEmpty()) {
					this.context.lastAcknowledgement = Instant.now();
				}
			}
		}
	}

}
