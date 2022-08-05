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
package io.awspring.cloud.sqs.listener.acknowledgement;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class BatchingAcknowledgementProcessor<T> extends AbstractAcknowledgementProcessor<T> {

	private static final Logger logger = LoggerFactory.getLogger(BatchingAcknowledgementProcessor.class);

	private final Object waitingMonitor = new Object();

	private ThreadWaitingAcknowledgementProcessor<T> acknowledgementProcessor;

	private BlockingQueue<Message<T>> acks;

	private Integer ackThreshold;

	private Duration ackInterval;

	public void setAcknowledgementInterval(Duration ackInterval) {
		Assert.notNull(ackInterval, "ackInterval cannot be null");
		this.ackInterval = ackInterval;
	}

	public void setAcknowledgementThreshold(Integer ackThreshold) {
		Assert.notNull(ackThreshold, "ackThreshold cannot be null");
		this.ackThreshold = ackThreshold;
	}

	@Override
	protected CompletableFuture<Void> doOnAcknowledge(Message<T> message) {
		if (!this.acks.offer(message)) {
			logger.warn("Acknowledgement queue full, dropping acknowledgement for message {}", MessageHeaderUtils.getId(message));
		}
		logger.trace("Received message {} to ack. Queue size: {}", MessageHeaderUtils.getId(message), this.acks.size());
		synchronized (this.waitingMonitor) {
			this.waitingMonitor.notifyAll();
		}
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
		this.acks = new LinkedBlockingQueue<>();
		this.acknowledgementProcessor = new ThreadWaitingAcknowledgementProcessor<>(this);
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setThreadNamePrefix("acknowledgement-processor-");
		executor.execute(this.acknowledgementProcessor);
	}

	@Override
	public void doStop() {
		this.acknowledgementProcessor.waitAcksToFinish();
	}

	private static class ThreadWaitingAcknowledgementProcessor<T> implements Runnable {

		private final BlockingQueue<Message<T>> acks;

		private final Object waitingMonitor;

		private final Duration ackInterval;

		private final Integer ackThreshold;

		private final Collection<CompletableFuture<Void>> runningAcks;

		private final BatchingAcknowledgementProcessor<T> parent;

		private Instant lastAcknowledgement;

		private ThreadWaitingAcknowledgementProcessor(BatchingAcknowledgementProcessor<T> parent) {
			this.acks = parent.acks;
			this.waitingMonitor = parent.waitingMonitor;
			this.ackInterval = parent.ackInterval;
			this.ackThreshold = parent.ackThreshold;
			this.parent = parent;
			this.lastAcknowledgement = Instant.now();
			this.runningAcks = Collections.synchronizedSet(new HashSet<>());
		}

		@Override
		public void run() {
			logger.debug("Starting ack processor");
			while (this.parent.isRunning()) {
				try {
					Instant now = Instant.now();
					boolean isTimeElapsed = now.isAfter(this.lastAcknowledgement.plus(this.ackInterval));
					int currentQueueSize = this.acks.size();
					logger.trace("Queue size: {} now: {} lastAcknowledgement: {} isTimeElapsed: {}", currentQueueSize, now, this.lastAcknowledgement, isTimeElapsed);
					if (currentQueueSize > 0 && (isTimeElapsed || currentQueueSize >= this.ackThreshold)) {
						int numberOfMessagesToPoll = isTimeElapsed ? currentQueueSize : this.ackThreshold;
						logger.trace("Polling {} messages from queue. Queue size: {}", numberOfMessagesToPoll, currentQueueSize);
						List<Message<T>> messagesToAck = pollMessagesToAck(numberOfMessagesToPoll);
						manageFuture(this.parent.execute(messagesToAck));
						this.lastAcknowledgement = Instant.now();
					}
					else {
						if (isTimeElapsed) {
							// Queue is empty, refresh timer
							this.lastAcknowledgement = Instant.now();
						}
						long waitTimeoutMillis = this.lastAcknowledgement.plus(this.ackInterval).toEpochMilli() - System.currentTimeMillis();
						logger.trace("Waiting on monitor for {}ms. Queue size: {}", waitTimeoutMillis, this.acks.size());
						synchronized (this.waitingMonitor) {
							this.waitingMonitor.wait(waitTimeoutMillis > 0 ? waitTimeoutMillis : 0);
						}
						logger.trace("Ack processor thread awakened. Queue size: {}", this.acks.size());
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("Interrupted while waiting for acks", e);
				}
				catch (Exception e) {
					logger.error("Error while handling acknowledgements, resuming.", e);
				}
			}
			logger.debug("Ack thread stopped");
		}

		private void manageFuture(CompletableFuture<Void> future) {
			this.runningAcks.add(future);
			future.whenComplete((v, t) -> this.runningAcks.remove(future));
		}

		private List<Message<T>> pollMessagesToAck(int numberOfMessagesToPoll) {
			return IntStream
				.range(0, numberOfMessagesToPoll)
				.mapToObj(index -> getPoll())
				.collect(Collectors.toList());
		}

		private Message<T> getPoll() {
			Message<T> polledMessage = this.acks.poll();
			if (polledMessage == null) {
				logger.warn("Poll returned null. Queue size: {}", this.acks.size());
			}
			else {
				logger.trace("Retrieved message {} from the queue. Queue size: {}", MessageHeaderUtils.getId(polledMessage), this.acks.size());
			}
			return polledMessage;
		}

		public void waitAcksToFinish() {
			Duration ackShutdownTimeout = Duration.ofSeconds(20);
			Instant start = Instant.now();
			while (!this.runningAcks.isEmpty() || Instant.now().isAfter(start.plus(ackShutdownTimeout))) {
				logger.debug("Waiting up to {} seconds for {} acks to finish", ackShutdownTimeout.getSeconds(), this.runningAcks);
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			if (!this.runningAcks.isEmpty()) {
				logger.warn("{} acks not finished in {} seconds, proceeding with shutdown.", this.runningAcks.size(), ackShutdownTimeout.getSeconds());
			}
			this.runningAcks.forEach(future -> future.cancel(true));
		}
	}


}
