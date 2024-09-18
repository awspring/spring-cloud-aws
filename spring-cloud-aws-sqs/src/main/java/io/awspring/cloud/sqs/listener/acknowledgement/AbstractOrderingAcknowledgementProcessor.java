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

import io.awspring.cloud.sqs.CollectionUtils;
import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;

/**
 * Base implementation for a {@link AcknowledgementProcessor} with {@link org.springframework.context.SmartLifecycle}
 * capabilities. Also provides acknowledgement ordering capabilities for {@link AcknowledgementOrdering#ORDERED}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractOrderingAcknowledgementProcessor<T>
		implements ExecutingAcknowledgementProcessor<T>, AcknowledgementCallback<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractOrderingAcknowledgementProcessor.class);

	private final Object lifecycleMonitor = new Object();

	private static final String DEFAULT_MESSAGE_GROUP = "default";

	private final Lock orderedExecutionLock = new ReentrantLock(true);

	private int maxAcknowledgementsPerBatch;

	private AcknowledgementExecutor<T> acknowledgementExecutor;

	private AcknowledgementOrdering acknowledgementOrdering;

	private final Map<String, CompletableFuture<Void>> lastAcknowledgementFutureMap = new ConcurrentHashMap<>();

	private AsyncAcknowledgementResultCallback<T> acknowledgementResultCallback = new AsyncAcknowledgementResultCallback<T>() {
	};

	private boolean running;

	private String id;

	private Function<Message<T>, String> messageGroupingFunction;

	@Override
	public AcknowledgementCallback<T> getAcknowledgementCallback() {
		return this;
	}

	@Override
	public void configure(ContainerOptions<?, ?> containerOptions) {
		this.acknowledgementOrdering = containerOptions.getAcknowledgementOrdering();
		doConfigure(containerOptions);
	}

	protected void doConfigure(ContainerOptions<?, ?> containerOptions) {
	}

	@Override
	public void setAcknowledgementExecutor(AcknowledgementExecutor<T> acknowledgementExecutor) {
		Assert.notNull(acknowledgementExecutor, "acknowledgementExecutor cannot be null");
		this.acknowledgementExecutor = acknowledgementExecutor;
	}

	@Override
	public void setAcknowledgementResultCallback(AsyncAcknowledgementResultCallback<T> acknowledgementResultCallback) {
		Assert.notNull(acknowledgementResultCallback, "acknowledgementResultCallback cannot be null");
		this.acknowledgementResultCallback = acknowledgementResultCallback;
	}

	public void setMaxAcknowledgementsPerBatch(int maxAcknowledgementsPerBatch) {
		Assert.isTrue(maxAcknowledgementsPerBatch > 0, "maxAcknowledgementsPerBatch must be greater than zero");
		this.maxAcknowledgementsPerBatch = maxAcknowledgementsPerBatch;
	}

	public void setMessageGroupingFunction(Function<Message<T>, String> messageGroupingFunction) {
		Assert.notNull(messageGroupingFunction, "messageGroupingFunction cannot be null");
		this.messageGroupingFunction = messageGroupingFunction;
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
			validateAndInitializeMessageGrouping();
			doStart();
		}
	}

	private void validateAndInitializeMessageGrouping() {
		Assert.isTrue(isValidOrderedByGroup() || isValidNotOrderedByGroup(),
				"Invalid configuration for acknowledgement ordering.");
		if (this.messageGroupingFunction == null) {
			this.messageGroupingFunction = msg -> DEFAULT_MESSAGE_GROUP;
		}
	}

	private boolean isValidOrderedByGroup() {
		return AcknowledgementOrdering.ORDERED_BY_GROUP.equals(this.acknowledgementOrdering)
				&& this.messageGroupingFunction != null;
	}

	private boolean isValidNotOrderedByGroup() {
		return !AcknowledgementOrdering.ORDERED_BY_GROUP.equals(this.acknowledgementOrdering)
				&& this.messageGroupingFunction == null;
	}

	protected void doStart() {
	}

	@Override
	public CompletableFuture<Void> onAcknowledge(Message<T> message) {
		if (!isRunning()) {
			logger.debug("{} not running, returning for message {}", this.id, MessageHeaderUtils.getId(message));
			return CompletableFuture.completedFuture(null);
		}
		logger.trace("Received message {} to acknowledge.", MessageHeaderUtils.getId(message));
		return doOnAcknowledge(message);
	}

	@Override
	public CompletableFuture<Void> onAcknowledge(Collection<Message<T>> messages) {
		logger.trace("Received messages {} to acknowledge.", MessageHeaderUtils.getId(messages));
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

	protected Function<Message<T>, String> getMessageGroupingFunction() {
		return this.messageGroupingFunction;
	}

	protected CompletableFuture<Void> sendToExecutor(Collection<Message<T>> messagesToAck) {
		StopWatch watch = new StopWatch();
		watch.start();
		return CompletableFutures
				.exceptionallyCompose(sendToExecutorParallelOrOrdered(messagesToAck),
						t -> logAcknowledgementError(messagesToAck, t))
				.whenComplete(logExecutionTime(messagesToAck, watch));
	}

	private BiConsumer<Void, Throwable> logExecutionTime(Collection<Message<T>> messagesToAck, StopWatch watch) {
		return (v, t) -> {
			watch.stop();
			logger.trace("Took {}ms to acknowledge messages {}", watch.getTotalTimeMillis(),
					MessageHeaderUtils.getId(messagesToAck));
		};
	}

	private CompletableFuture<Void> sendToExecutorParallelOrOrdered(Collection<Message<T>> messagesToAck) {
		return AcknowledgementOrdering.PARALLEL.equals(this.acknowledgementOrdering)
				? sendToExecutorParallel(messagesToAck)
				: sendToExecutorOrdered(messagesToAck);
	}

	private CompletableFuture<Void> sendToExecutorParallel(Collection<Message<T>> messagesToAck) {
		return CompletableFuture.allOf(partitionMessages(messagesToAck).stream().map(this::doSendToExecutor)
				.toArray(CompletableFuture[]::new));
	}

	private CompletableFuture<Void> sendToExecutorOrdered(Collection<Message<T>> messagesToAck) {
		this.orderedExecutionLock.lock();
		try {
			return CompletableFuture.allOf(partitionMessages(messagesToAck).stream().map(this::doSendToExecutorOrdered)
					.flatMap(Collection::stream).toArray(CompletableFuture[]::new));
		}
		finally {
			this.orderedExecutionLock.unlock();
		}
	}

	private Collection<CompletableFuture<Void>> doSendToExecutorOrdered(Collection<Message<T>> messagesToAck) {
		return messagesToAck.stream().collect(Collectors.groupingBy(this.messageGroupingFunction)).entrySet().stream()
				.filter(entry -> entry.getValue().size() > 0)
				.map(entry -> sendGroupToExecutor(entry.getKey(), entry.getValue())).collect(Collectors.toList());
	}

	private CompletableFuture<Void> sendGroupToExecutor(String group, List<Message<T>> messages) {
		CompletableFuture<Void> nextFuture = this.lastAcknowledgementFutureMap
				.computeIfAbsent(group, newGroup -> CompletableFuture.completedFuture(null)).exceptionally(t -> null)
				.thenCompose(theVoid -> doSendToExecutor(messages));
		this.lastAcknowledgementFutureMap.put(group, nextFuture);
		removeCompletedFutures();
		return nextFuture;
	}

	private void removeCompletedFutures() {
		List<String> completedFutures = this.lastAcknowledgementFutureMap.entrySet().stream()
				.filter(entry -> entry.getValue().isDone()).map(Map.Entry::getKey).collect(Collectors.toList());
		logger.trace("Removing completed futures from groups {}", completedFutures);
		completedFutures.forEach(this.lastAcknowledgementFutureMap::remove);
	}

	private CompletableFuture<Void> doSendToExecutor(Collection<Message<T>> messagesToAck) {
		return CompletableFutures.handleCompose(this.acknowledgementExecutor.execute(messagesToAck), (v, t) -> t == null
				? executeResultCallback(messagesToAck, null)
				: executeResultCallback(messagesToAck, t).thenCompose(theVoid -> CompletableFutures.failedFuture(t)));
	}

	private CompletableFuture<Void> executeResultCallback(Collection<Message<T>> messagesToAck,
			Throwable ackThrowable) {
		return CompletableFutures.exceptionallyCompose(doExecuteResultCallback(messagesToAck, ackThrowable),
				t -> CompletableFutures.failedFuture(new AcknowledgementResultCallbackException(
						"Error executing acknowledgement result callback", t)));
	}

	private CompletableFuture<Void> doExecuteResultCallback(Collection<Message<T>> messagesToAck, Throwable t) {
		logger.trace("Executing result callback for {} in {}", MessageHeaderUtils.getId(messagesToAck), this.id);
		return t == null ? this.acknowledgementResultCallback.onSuccess(messagesToAck)
				: this.acknowledgementResultCallback.onFailure(messagesToAck, t);
	}

	private CompletableFuture<Void> logAcknowledgementError(Collection<Message<T>> messagesToAck, Throwable t) {
		logger.error("Acknowledgement processing has thrown an error for messages {} in {}",
				MessageHeaderUtils.getId(messagesToAck), this.id, t);
		return CompletableFutures.failedFuture(t);
	}

	private Collection<Collection<Message<T>>> partitionMessages(Collection<Message<T>> messagesToAck) {
		logger.trace("Partitioning {} messages in {}", messagesToAck.size(), this.id);
		return CollectionUtils.partition(messagesToAck, this.maxAcknowledgementsPerBatch);
	}

	protected abstract CompletableFuture<Void> doOnAcknowledge(Message<T> message);

	protected abstract CompletableFuture<Void> doOnAcknowledge(Collection<Message<T>> messages);

}
