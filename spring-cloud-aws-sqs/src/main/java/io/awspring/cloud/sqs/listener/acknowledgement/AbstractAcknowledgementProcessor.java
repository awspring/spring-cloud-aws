package io.awspring.cloud.sqs.listener.acknowledgement;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractAcknowledgementProcessor<T> implements ExecutingAcknowledgementProcessor<T>, AcknowledgementCallback<T>{

	private static final Logger logger = LoggerFactory.getLogger(AbstractAcknowledgementProcessor.class);

	private final Object lifecycleMonitor = new Object();

	private final Lock orderedExecutionLock = new ReentrantLock(true);

	private AcknowledgementExecutor<T> acknowledgementExecutor;

	private AcknowledgementOrdering acknowledgementOrdering;

	private CompletableFuture<Void> lastAcknowledgementFuture = CompletableFuture.completedFuture(null);

	private boolean running;

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

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			Assert.notNull(this.acknowledgementExecutor, "acknowledgementExecutor not set");
			Assert.notNull(this.acknowledgementOrdering, "acknowledgementOrdering not set");
			logger.debug("Starting {}", getClass().getSimpleName());
			this.running = true;
			doStart();
		}
	}

	protected void doStart() {
	}

	@Override
	public CompletableFuture<Void> onAcknowledge(Message<T> message) {
		if (!isRunning()) {
			logger.debug("{} not running, returning for message {}", getClass().getSimpleName(), MessageHeaderUtils.getId(message));
			return CompletableFuture.completedFuture(null);
		}
		return doOnAcknowledge(message);
	}

	@Override
	public CompletableFuture<Void> onAcknowledge(Collection<Message<T>> messages) {
		if (!isRunning()) {
			logger.debug("{} not running, returning for messages {}", getClass().getSimpleName(), MessageHeaderUtils.getId(messages));
			return CompletableFuture.completedFuture(null);
		}
		return doOnAcknowledge(messages);
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			logger.info("Stopping {}", getClass().getSimpleName());
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

	protected CompletableFuture<Void> execute(Collection<Message<T>> messagesToAck) {
		return AcknowledgementOrdering.PARALLEL.equals(this.acknowledgementOrdering)
			? executeParallel(messagesToAck)
			: executeOrdered(messagesToAck);
	}

	private CompletableFuture<Void> executeParallel(Collection<Message<T>> messagesToAck) {
		return CompletableFuture.allOf(partitionMessages(messagesToAck)
			.stream()
			.map(this.acknowledgementExecutor::execute)
			.toArray(CompletableFuture[]::new));
	}

	private CompletableFuture<Void> executeOrdered(Collection<Message<T>> messagesToAck) {
		this.orderedExecutionLock.lock();
		try {
			partitionMessages(messagesToAck).forEach(batch -> doExecuteOrdered(messagesToAck));
		}
		finally {
			this.orderedExecutionLock.unlock();
		}
		return this.lastAcknowledgementFuture;
	}

	private void doExecuteOrdered(Collection<Message<T>> messagesToAck) {
		this.lastAcknowledgementFuture = this.lastAcknowledgementFuture
			.thenCompose(theVoid -> this.acknowledgementExecutor.execute(messagesToAck));
	}

	private Collection<Collection<Message<T>>> partitionMessages(Collection<Message<T>> messagesToAck) {
		List<Message<T>> messagesToUse = getMessagesAsList(messagesToAck);
		int totalSize = messagesToUse.size();
		return IntStream.rangeClosed(0, (totalSize - 1) / 10)
			.mapToObj(index -> messagesToUse.subList(index * 10, Math.min((index + 1) * 10, totalSize)))
			.collect(Collectors.toList());
	}

	private List<Message<T>> getMessagesAsList(Collection<Message<T>> messagesToAck) {
		return messagesToAck instanceof List ? (List<Message<T>>) messagesToAck : new ArrayList<>(messagesToAck);
	}

	protected abstract CompletableFuture<Void> doOnAcknowledge(Message<T> message);

	protected abstract CompletableFuture<Void> doOnAcknowledge(Collection<Message<T>> messages);

}
