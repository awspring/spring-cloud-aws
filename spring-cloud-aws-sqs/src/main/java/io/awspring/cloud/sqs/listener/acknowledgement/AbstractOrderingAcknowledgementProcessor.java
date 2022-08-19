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

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.IdentifiableContainerComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Base implementation for a {@link AcknowledgementProcessor} with {@link org.springframework.context.SmartLifecycle}
 * capabilities. Also provides acknowledgement ordering capabilities for {@link AcknowledgementOrdering#ORDERED}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractOrderingAcknowledgementProcessor<T>
		implements ExecutingAcknowledgementProcessor<T>, AcknowledgementCallback<T>, IdentifiableContainerComponent {

	private static final Logger logger = LoggerFactory.getLogger(AbstractOrderingAcknowledgementProcessor.class);

	private final Object lifecycleMonitor = new Object();

	private final Lock orderedExecutionLock = new ReentrantLock(true);

	private int maxAcknowledgementsPerBatch;

	private AcknowledgementExecutor<T> acknowledgementExecutor;

	private AcknowledgementOrdering acknowledgementOrdering;

	private CompletableFuture<Void> lastAcknowledgementFuture = CompletableFuture.completedFuture(null);

	private boolean running;

	private String id;

	@Override
	public AcknowledgementCallback<T> getAcknowledgementCallback() {
		return this;
	}

	@Override
	public void setAcknowledgementExecutor(AcknowledgementExecutor<T> acknowledgementExecutor) {
		Assert.notNull(acknowledgementExecutor, "acknowledgementExecutor cannot be null");
		this.acknowledgementExecutor = acknowledgementExecutor;
	}

	@Override
	public void setAcknowledgementOrdering(AcknowledgementOrdering acknowledgementOrdering) {
		Assert.notNull(acknowledgementOrdering, "acknowledgementOrdering cannot be null");
		this.acknowledgementOrdering = acknowledgementOrdering;
	}

	public void setMaxAcknowledgementsPerBatch(int maxAcknowledgementsPerBatch) {
		Assert.isTrue(maxAcknowledgementsPerBatch > 0, "maxAcknowledgementsPerBatch must be greater than zero");
		this.maxAcknowledgementsPerBatch = maxAcknowledgementsPerBatch;
	}

	@Override
	public void setId(String id) {
		Assert.notNull(id, "id cannot be null");
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			Assert.notNull(this.acknowledgementExecutor, "acknowledgementExecutor not set");
			Assert.notNull(this.acknowledgementOrdering, "acknowledgementOrdering not set");
			Assert.notNull(this.id, "id not set");
			logger.debug("Starting {} with ordering {} and batch size {}", this.id, this.acknowledgementOrdering,
					this.maxAcknowledgementsPerBatch);
			this.running = true;
			doStart();
		}
	}

	protected void doStart() {
	}

	@Override
	public CompletableFuture<Void> onAcknowledge(Message<T> message) {
		if (!isRunning()) {
			logger.debug("{} not running, returning for message {}", this.id, MessageHeaderUtils.getId(message));
			return CompletableFuture.completedFuture(null);
		}
		return doOnAcknowledge(message);
	}

	@Override
	public CompletableFuture<Void> onAcknowledge(Collection<Message<T>> messages) {
		if (!isRunning()) {
			logger.debug("{} not running, returning for messages {}", this.id, MessageHeaderUtils.getId(messages));
			return CompletableFuture.completedFuture(null);
		}
		return doOnAcknowledge(messages);
	}

	@Override
	public void stop() {
		if (!isRunning()) {
			logger.debug("{} already stopped", this.id);
			return;
		}
		synchronized (this.lifecycleMonitor) {
			logger.debug("Stopping {}", this.id);
			this.running = false;
			doStop();
		}
	}

	protected void doStop() {
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	protected CompletableFuture<Void> sendToExecutor(Collection<Message<T>> messagesToAck) {
		return AcknowledgementOrdering.PARALLEL.equals(this.acknowledgementOrdering)
				? sendToExecutorParallel(messagesToAck)
				: sendToExecutorOrdered(messagesToAck);
	}

	private CompletableFuture<Void> sendToExecutorParallel(Collection<Message<T>> messagesToAck) {
		return CompletableFuture.allOf(partitionMessages(messagesToAck).stream()
				.map(this.acknowledgementExecutor::execute).toArray(CompletableFuture[]::new));
	}

	private CompletableFuture<Void> sendToExecutorOrdered(Collection<Message<T>> messagesToAck) {
		this.orderedExecutionLock.lock();
		try {
			partitionMessages(messagesToAck).forEach(batch -> doSendToExecutorOrdered(messagesToAck));
		}
		finally {
			this.orderedExecutionLock.unlock();
		}
		return this.lastAcknowledgementFuture;
	}

	private void doSendToExecutorOrdered(Collection<Message<T>> messagesToAck) {
		this.lastAcknowledgementFuture = this.lastAcknowledgementFuture
				.thenCompose(theVoid -> this.acknowledgementExecutor.execute(messagesToAck));
	}

	private Collection<Collection<Message<T>>> partitionMessages(Collection<Message<T>> messagesToAck) {
		logger.trace("Partitioning {} messages in {}", messagesToAck.size(), this.id);
		List<Message<T>> messagesToUse = getMessagesAsList(messagesToAck);
		int totalSize = messagesToUse.size();
		return IntStream.rangeClosed(0, (totalSize - 1) / this.maxAcknowledgementsPerBatch)
				.mapToObj(index -> messagesToUse.subList(index * this.maxAcknowledgementsPerBatch,
						Math.min((index + 1) * this.maxAcknowledgementsPerBatch, totalSize)))
				.collect(Collectors.toList());
	}

	private List<Message<T>> getMessagesAsList(Collection<Message<T>> messagesToAck) {
		return messagesToAck instanceof List ? (List<Message<T>>) messagesToAck : new ArrayList<>(messagesToAck);
	}

	protected abstract CompletableFuture<Void> doOnAcknowledge(Message<T> message);

	protected abstract CompletableFuture<Void> doOnAcknowledge(Collection<Message<T>> messages);

}
