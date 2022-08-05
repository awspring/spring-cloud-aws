package io.awspring.cloud.sqs.listener.source;


import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.listener.BackPressureHandler;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Base {@link PollingMessageSource} implementation with
 * {@link org.springframework.context.SmartLifecycle} and backpressure handling capabilities.
 *
 * The connected {@link MessageSink} should use the provided completion callback to signal
 * each completed message processing.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractPollingMessageSource<T> implements PollingMessageSource<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPollingMessageSource.class);

	private String pollingEndpointName;

	private Duration shutdownTimeout;

	private Executor executor;

	private BackPressureHandler backPressureHandler;

	private AcknowledgementProcessor<T> acknowledgmentProcessor;

	private MessageSink<T> messageSink;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();

	private final Collection<CompletableFuture<?>> pollingFutures = Collections.synchronizedCollection(new ArrayList<>());

	@SuppressWarnings("unchecked")
	@Override
	public void configure(ContainerOptions containerOptions) {
		this.shutdownTimeout = containerOptions.getSourceShutdownTimeout();
	}

	@Override
	public void setPollingEndpointName(String pollingEndpointName) {
		Assert.isTrue(StringUtils.hasText(pollingEndpointName), "pollingEndpointName must have text");
		this.pollingEndpointName = pollingEndpointName;
	}

	@Override
	public void setBackPressureHandler(BackPressureHandler backPressureHandler) {
		Assert.notNull(backPressureHandler, "backPressureHandler cannot be null");
		this.backPressureHandler = backPressureHandler;
	}

	@Override
	public void setAcknowledgementProcessor(AcknowledgementProcessor<T> acknowledgementProcessor) {
		Assert.notNull(acknowledgementProcessor, "acknowledgementProcessor cannot be null");
		this.acknowledgmentProcessor = acknowledgementProcessor;
	}

	@Override
	public void setExecutor(Executor executor) {
		Assert.notNull(executor, "executor cannot be null.");
		addEndpointNameToPrefix(executor);
		this.executor = executor;
	}

	private void addEndpointNameToPrefix(Executor taskExecutor) {
		ConfigUtils.INSTANCE
			.acceptIfInstance(taskExecutor, CustomizableThreadCreator.class,
				ctc -> ctc.setThreadNamePrefix(ctc.getThreadNamePrefix() + this.pollingEndpointName + "-"));
	}

	@Override
	public void setMessageSink(MessageSink<T> messageSink) {
		Assert.notNull(messageSink, "messageSink cannot be null.");
		this.messageSink = messageSink;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public void start() {
		if (isRunning()) {
			logger.debug("Message source for queue {} already running.", this.pollingEndpointName);
			return;
		}
		synchronized (this.lifecycleMonitor) {
			Assert.notNull(this.messageSink, "No MessageSink was set");
			logger.debug("Starting MessageSource for queue {}", this.pollingEndpointName);
			this.running = true;
			this.backPressureHandler.setClientId(this.pollingEndpointName);
			doStart();
			this.acknowledgmentProcessor.start();
			startPollingThread();
		}
	}

	protected void doStart() {
	}

	private void startPollingThread() {
		this.executor.execute(this::pollAndEmitMessages);
	}

	private void pollAndEmitMessages() {
		while (isRunning()) {
			try {
				if (!isRunning()) {
					continue;
				}
				logger.trace("Requesting permits for queue {}", this.pollingEndpointName);
				final int acquiredPermits = this.backPressureHandler.request();
				if (acquiredPermits == 0) {
					logger.trace("No permits acquired for queue {}.", this.pollingEndpointName);
					continue;
				}
				logger.trace("{} permits acquired for queue {}", acquiredPermits, this.pollingEndpointName);
				if (!isRunning()) {
					logger.debug("MessageSource was stopped after permits where acquired. Returning {} permits.", acquiredPermits);
					this.backPressureHandler.release(acquiredPermits);
					continue;
				}
				managePollingFuture(doPollForMessages(acquiredPermits))
					.exceptionally(this::handlePollingException)
					.thenApply(msgs -> releaseUnusedPermits(acquiredPermits, msgs))
					.thenCompose(this::emitMessagesToPipeline)
					.exceptionally(this::handleSinkException);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("MessageSource thread interrupted for endpoint " + this.pollingEndpointName, e);
			}
			catch (Exception e) {
				logger.error("Error in MessageSource for queue {}. Resuming.", this.pollingEndpointName, e);
			}
		}
		logger.debug("Execution thread stopped for queue {}.", this.pollingEndpointName);
	}

	protected abstract CompletableFuture<Collection<Message<T>>> doPollForMessages(int messagesToRequest);

	public Collection<Message<T>> releaseUnusedPermits(int permits, Collection<Message<T>> msgs) {
		int permitsToRelease = permits - msgs.size();
		logger.trace("Releasing {} unused permits for queue {}", permitsToRelease, this.pollingEndpointName);
		this.backPressureHandler.release(permitsToRelease);
		return msgs;
	}

	private CompletableFuture<Void> emitMessagesToPipeline(Collection<Message<T>> messages) {
		if (messages.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}
		return this.messageSink.emit(messages, createContext());
	}

	private MessageProcessingContext<T> createContext() {
		return MessageProcessingContext.<T>create()
			.setBackPressureReleaseCallback(() -> {
				logger.debug("Releasing permit for queue {}", this.pollingEndpointName);
				this.backPressureHandler.release(1);
			})
			.setAcknowledgmentCallback(this.acknowledgmentProcessor.getAcknowledgementCallback());
	}

	private Void handleSinkException(Throwable throwable) {
		logger.warn("Sink returned an error.", throwable);
		return null;
	}

	private Collection<Message<T>> handlePollingException(Throwable t) {
		logger.error("Error polling for messages in queue {}.", this.pollingEndpointName, t);
		return Collections.emptyList();
	}

	private <F> CompletableFuture<F> managePollingFuture(CompletableFuture<F> pollingFuture) {
		this.pollingFutures.add(pollingFuture);
		pollingFuture.thenRun(() -> this.pollingFutures.remove(pollingFuture));
		return pollingFuture;
	}

	protected String getPollingEndpointName() {
		return this.pollingEndpointName;
	}

	protected AcknowledgementProcessor<T> getAcknowledgmentProcessor() {
		return this.acknowledgmentProcessor;
	}

	@Override
	public void stop() {
		if (!isRunning()) {
			logger.debug("Message source for queue {} not running.", this.pollingEndpointName);
		}
		synchronized (this.lifecycleMonitor) {
			logger.debug("Stopping MessageSource for queue {}", this.pollingEndpointName);
			this.running = false;
			waitExistingTasksToFinish();
			doStop();
			this.pollingFutures.forEach(pollingFuture -> pollingFuture.cancel(true));
			logger.debug("MessageSource for queue {} stopped", this.pollingEndpointName);
		}
	}

	protected void doStop() {
	}

	private void waitExistingTasksToFinish() {
		Duration shutDownTimeout = this.shutdownTimeout;
		if (shutDownTimeout.isZero()) {
			logger.debug("Container shutdown timeout set to zero - not waiting for tasks to finish.");
			return;
		}
		boolean tasksFinished = this.backPressureHandler.drain(shutDownTimeout);
		if (!tasksFinished) {
			logger.warn("Tasks did not finish in {} seconds for queue {}, proceeding with shutdown.",
				shutDownTimeout.getSeconds(), this.pollingEndpointName);
		}
	}

}
