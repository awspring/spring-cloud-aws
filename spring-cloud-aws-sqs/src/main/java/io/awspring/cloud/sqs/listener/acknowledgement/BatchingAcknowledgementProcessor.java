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
import io.awspring.cloud.sqs.listener.TaskExecutorAware;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class BatchingAcknowledgementProcessor<T> extends AbstractAcknowledgementProcessor<T>
		implements TaskExecutorAware {

	private static final Logger logger = LoggerFactory.getLogger(BatchingAcknowledgementProcessor.class);

	private BufferingAcknowledgementProcessor<T> acknowledgementProcessor;

	private BlockingQueue<Message<T>> acks;

	private Integer ackThreshold;

	private Duration ackInterval;

	private TaskExecutor taskExecutor;

	private TaskScheduler taskScheduler;

	public void setAcknowledgementInterval(Duration ackInterval) {
		Assert.notNull(ackInterval, "ackInterval cannot be null");
		this.ackInterval = ackInterval;
	}

	public void setAcknowledgementThreshold(Integer ackThreshold) {
		Assert.notNull(ackThreshold, "ackThreshold cannot be null");
		this.ackThreshold = ackThreshold;
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
		logger.trace("Received message {} to ack in {}. Queue size: {}", MessageHeaderUtils.getId(message), getId(),
				this.acks.size());
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

		private final Duration ackInterval;

		private final Integer ackThreshold;

		private final Lock ackLock = new ReentrantLock();

		private final Collection<CompletableFuture<Void>> runningAcks;

		private final BatchingAcknowledgementProcessor<T> parent;

		private final TaskScheduler taskScheduler;

		private final BlockingQueue<Message<T>> acksBuffer;

		// Should always be updated under ackLock
		private volatile Instant lastAcknowledgement;

		private BufferingAcknowledgementProcessor(BatchingAcknowledgementProcessor<T> parent) {
			this.acks = parent.acks;
			this.ackInterval = parent.ackInterval;
			this.ackThreshold = parent.ackThreshold;
			this.taskScheduler = parent.taskScheduler;
			this.parent = parent;
			this.runningAcks = Collections.synchronizedSet(new HashSet<>());
			this.acksBuffer = new LinkedBlockingQueue<>();
			this.lastAcknowledgement = Instant.now();
		}

		@Override
		public void run() {
			maybeStartScheduledThread();
			logger.debug("Starting acknowledgement processor thread with batchSize: {}", this.ackThreshold);
			while (this.parent.isRunning()) {
				try {
					Message<T> poll = this.acks.poll(1, TimeUnit.SECONDS);
					if (poll != null) {
						this.acksBuffer.put(poll);
					}
					while (this.ackThreshold != 0 && acksBuffer.size() >= this.ackThreshold) {
						this.ackLock.lock();
						int bufferSize = acksBuffer.size();
						if (bufferSize >= this.ackThreshold) {
							logger.trace("Acknowledgement buffer threshold of {} reached for {}. Buffer size: {}",
									this.ackThreshold, this.parent.getId(), bufferSize);
							pollAndExecuteAcks(this.ackThreshold);
							this.lastAcknowledgement = Instant.now();
						}
						this.ackLock.unlock();
					}
				}
				catch (Exception e) {
					logger.error("Error while handling acknowledgements for {}, resuming.", this.parent.getId(), e);
				}
			}
			logger.debug("Acknowledgement processor thread stopped");
		}

		private void maybeStartScheduledThread() {
			if (this.ackInterval != Duration.ZERO) {
				logger.debug("Starting scheduled thread with interval of {}ms", this.ackInterval.toMillis());
				scheduleNextExecution(Instant.now().plus(this.ackInterval));
			}
		}

		private void scheduleNextExecution(Instant nextExecutionDelay) {
			if (!this.parent.isRunning()) {
				return;
			}
			this.taskScheduler.schedule(() -> {
				this.ackLock.lock();
				pollAndExecuteScheduled();
				scheduleNextExecution(this.lastAcknowledgement.plus(this.ackInterval));
				this.ackLock.unlock();
			}, nextExecutionDelay);
		}

		private void pollAndExecuteScheduled() {
			boolean isTimeElapsed = Instant.now().isAfter(this.lastAcknowledgement.plus(this.ackInterval));
			int bufferSize = acksBuffer.size();
			if (isTimeElapsed && bufferSize > 0) {
				logger.trace("Scheduled polling and executing {} acknowledgements for {}", bufferSize, parent.getId());
				pollAndExecuteAcks(bufferSize);
			}
			this.lastAcknowledgement = Instant.now();
		}

		private void pollAndExecuteAcks(int amount) {
			logger.trace("Polling {} messages from ack queue.", amount);
			List<Message<T>> messagesToAck = pollMessagesToAck(amount);
			manageFuture(this.parent.sendToExecutor(messagesToAck));
		}

		private void manageFuture(CompletableFuture<Void> future) {
			this.runningAcks.add(future);
			future.whenComplete((v, t) -> this.runningAcks.remove(future));
		}

		// @formatter:off
		private List<Message<T>> pollMessagesToAck(int numberOfMessagesToPoll) {
			return IntStream
				.range(0, numberOfMessagesToPoll)
				.mapToObj(index -> pollMessage())
				.collect(Collectors.toList());
		}
		// @formatter:on

		private Message<T> pollMessage() {
			Message<T> polledMessage = this.acksBuffer.poll();
			Assert.notNull(polledMessage, "poll should never return null");
			logger.trace("Retrieved message {} from the queue. Queue size: {}", MessageHeaderUtils.getId(polledMessage),
					this.acks.size());
			return polledMessage;
		}

		public void waitAcknowledgementsToFinish() {
			Duration ackShutdownTimeout = Duration.ofSeconds(20);
			Instant start = Instant.now();
			while (!this.runningAcks.isEmpty() && Instant.now().isBefore(start.plus(ackShutdownTimeout))) {
				logger.debug("Waiting up to {} seconds for {} acks to finish", ackShutdownTimeout.getSeconds(),
						this.runningAcks);
				try {
					Thread.sleep(200);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("Interrupted while waiting for tasks to finish");
				}
			}
			if (!this.runningAcks.isEmpty()) {
				logger.warn("{} acks not finished in {} seconds, proceeding with shutdown.", this.runningAcks.size(),
						ackShutdownTimeout.getSeconds());
				this.runningAcks.forEach(future -> future.cancel(true));
			}
		}
	}

}
