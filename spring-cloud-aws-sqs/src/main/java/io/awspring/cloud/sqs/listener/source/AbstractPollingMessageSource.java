/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.listener.source;

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.IdentifiableContainerComponent;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.TaskExecutorAware;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.AsyncAcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.ExecutingAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.backpressure.BackPressureHandler;
import io.awspring.cloud.sqs.listener.backpressure.BackPressureHandler.ReleaseReason;
import io.awspring.cloud.sqs.listener.backpressure.BatchAwareBackPressureHandler;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base {@link PollingMessageSource} implementation with {@link org.springframework.context.SmartLifecycle}
 * capabilities.
 * <p>
 * Polling backpressure is handled the provided {@link BackPressureHandler}. The connected {@link MessageSink} should
 * use the provided {@link MessageProcessingContext#getAcknowledgmentCallback()} to signal each message processing
 * completion and enable further polling.
 * <p>
 * Message conversion capabilities are inherited by the {@link AbstractMessageConvertingMessageSource} superclass.
 * <p>
 * The {@link AcknowledgementProcessor} instance provides the {@link AcknowledgementCallback} to be set in the
 * {@link MessageProcessingContext} and executed downstream when applicable.
 *
 * @author Tomaz Fernandes
 * @author Loïc Rouchon
 * @since 3.0
 */
public abstract class AbstractPollingMessageSource<T, S> extends AbstractMessageConvertingMessageSource<T, S>
		implements PollingMessageSource<T>, IdentifiableContainerComponent {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPollingMessageSource.class);

	private String pollingEndpointName;

	private Duration shutdownTimeout;

	private BackOffPolicy pollBackOffPolicy;

	private AtomicReference<BackOffContext> pollBackOffContext = new AtomicReference<>();

	private TaskExecutor taskExecutor;

	private BatchAwareBackPressureHandler backPressureHandler;

	private AcknowledgementProcessor<T> acknowledgmentProcessor;

	private MessageSink<T> messageSink;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();

	private final Collection<CompletableFuture<?>> pollingFutures = Collections
			.synchronizedCollection(new ArrayList<>());

	private String id;

	private AsyncAcknowledgementResultCallback<T> acknowledgementResultCallback;

	@Override
	protected void configureMessageSource(ContainerOptions<?, ?> containerOptions) {
		this.shutdownTimeout = containerOptions.getListenerShutdownTimeout();
		this.pollBackOffPolicy = containerOptions.getPollBackOffPolicy();
		doConfigure(containerOptions);
	}

	/**
	 * Override to configure subclasses.
	 * @param containerOptions the {@link ContainerOptions} for this source.
	 */
	protected void doConfigure(ContainerOptions<?, ?> containerOptions) {
	}

	@Override
	public void setId(String id) {
		Assert.notNull(id, "id cannot be null");
		this.id = id;
	}

	@Override
	public void setPollingEndpointName(String pollingEndpointName) {
		Assert.isTrue(StringUtils.hasText(pollingEndpointName), "pollingEndpointName must have text");
		this.pollingEndpointName = pollingEndpointName;
	}

	@Override
	public void setBackPressureHandler(BackPressureHandler backPressureHandler) {
		Assert.notNull(backPressureHandler, "backPressureHandler cannot be null");
		Assert.isInstanceOf(BatchAwareBackPressureHandler.class, backPressureHandler,
				getClass().getSimpleName() + " requires a " + BatchAwareBackPressureHandler.class);
		this.backPressureHandler = (BatchAwareBackPressureHandler) backPressureHandler;
	}

	@Override
	public void setAcknowledgementProcessor(AcknowledgementProcessor<T> acknowledgementProcessor) {
		Assert.notNull(acknowledgementProcessor, "acknowledgementProcessor cannot be null");
		this.acknowledgmentProcessor = acknowledgementProcessor;
	}

	@Override
	public void setAcknowledgementResultCallback(AsyncAcknowledgementResultCallback<T> acknowledgementResultCallback) {
		Assert.notNull(acknowledgementResultCallback, "acknowledgementResultCallback must not be null");
		this.acknowledgementResultCallback = acknowledgementResultCallback;
	}

	@Override
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "taskExecutor cannot be null");
		this.taskExecutor = taskExecutor;
	}

	@Override
	public void setMessageSink(MessageSink<T> messageSink) {
		Assert.notNull(messageSink, "messageSink cannot be null");
		this.messageSink = messageSink;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void start() {
		if (isRunning()) {
			logger.debug("{} for queue {} already running", getClass().getSimpleName(), this.pollingEndpointName);
			return;
		}
		synchronized (this.lifecycleMonitor) {
			Assert.notNull(this.id, "id not set");
			Assert.notNull(this.messageSink, "messageSink not set");
			Assert.notNull(this.backPressureHandler, "backPressureHandler not set");
			Assert.notNull(this.acknowledgmentProcessor, "acknowledgmentProcessor not set");
			Assert.notNull(this.pollBackOffPolicy, "pollBackOffPolicy not set");
			logger.debug("Starting {} for queue {}", getClass().getSimpleName(), this.pollingEndpointName);
			this.running = true;
			ConfigUtils.INSTANCE
					.acceptIfInstance(this.backPressureHandler, IdentifiableContainerComponent.class,
							icc -> icc.setId(this.id))
					.acceptIfInstance(this.acknowledgmentProcessor, IdentifiableContainerComponent.class,
							icc -> icc.setId(this.id))
					.acceptIfInstance(this.acknowledgmentProcessor, ExecutingAcknowledgementProcessor.class,
							eap -> eap.setAcknowledgementResultCallback(this.acknowledgementResultCallback))
					.acceptIfInstance(this.acknowledgmentProcessor, TaskExecutorAware.class,
							ea -> ea.setTaskExecutor(this.taskExecutor));
			doStart();
			setupAcknowledgementForConversion(this.acknowledgmentProcessor.getAcknowledgementCallback());
			this.acknowledgmentProcessor.start();
			startPollingThread();
		}
	}

	protected void doStart() {
	}

	private void startPollingThread() {
		this.taskExecutor.execute(this::pollAndEmitMessages);
	}

	private void pollAndEmitMessages() {
		while (isRunning()) {
			try {
				if (!isRunning()) {
					continue;
				}
				handlePollBackOff();
				logger.trace("Requesting permits for queue {}", this.pollingEndpointName);
				final int acquiredPermits = this.backPressureHandler.requestBatch();
				if (acquiredPermits == 0) {
					logger.trace("No permits acquired for queue {}", this.pollingEndpointName);
					continue;
				}
				logger.trace("{} permits acquired for queue {}", acquiredPermits, this.pollingEndpointName);
				if (!isRunning()) {
					logger.debug("MessageSource was stopped after permits where acquired. Returning {} permits",
							acquiredPermits);
					this.backPressureHandler.release(acquiredPermits, ReleaseReason.NONE_FETCHED);
					continue;
				}
				// @formatter:off
				managePollingFuture(doPollForMessages(acquiredPermits))
					.thenApply(this::resetBackOffContext)
					.exceptionally(this::handlePollingException)
					.thenApply(this::convertMessages)
					.thenApply(msgs -> releaseUnusedPermits(acquiredPermits, msgs))
					.thenCompose(this::emitMessagesToPipeline)
					.exceptionally(this::handleSinkException);
				// @formatter:on
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(
						"MessageSource thread interrupted for endpoint " + this.pollingEndpointName, e);
			}
			catch (Exception e) {
				logger.error("Error in MessageSource for queue {}. Resuming", this.pollingEndpointName, e);
			}
		}
		logger.debug("Execution thread stopped for queue {}", this.pollingEndpointName);
	}

	private void handlePollBackOff() {
		BackOffContext backOffContext = this.pollBackOffContext.get();
		if (backOffContext == null) {
			return;
		}
		logger.trace("Back off context found, backing off");
		this.pollBackOffPolicy.backOff(backOffContext);
		logger.trace("Resuming from back off");
	}

	protected abstract CompletableFuture<Collection<S>> doPollForMessages(int messagesToRequest);

	public Collection<Message<T>> releaseUnusedPermits(int permits, Collection<Message<T>> msgs) {
		int polledMessages = msgs.size();
		int permitsToRelease = permits - polledMessages;
		ReleaseReason releaseReason = polledMessages == 0 ? ReleaseReason.NONE_FETCHED : ReleaseReason.PARTIAL_FETCH;
		this.backPressureHandler.release(permitsToRelease, releaseReason);
		logger.trace("Released {} unused ({}) permits for queue {} (messages polled {})", permitsToRelease,
				releaseReason, this.pollingEndpointName, polledMessages);
		return msgs;
	}

	private CompletableFuture<Void> emitMessagesToPipeline(Collection<Message<T>> messages) {
		if (messages.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}
		return this.messageSink.emit(messages, createContext());
	}

	// @formatter:off
	protected MessageProcessingContext<T> createContext() {
		return MessageProcessingContext.<T> create()
			.setBackPressureReleaseCallback(this::releaseBackPressure)
			.setAcknowledgmentCallback(getAcknowledgementCallback());
	}
	// @formatter:on

	protected AcknowledgementCallback<T> getAcknowledgementCallback() {
		return this.acknowledgmentProcessor.getAcknowledgementCallback();
	}

	private void releaseBackPressure() {
		logger.debug("Releasing permit for queue {}", this.pollingEndpointName);
		this.backPressureHandler.release(1, ReleaseReason.PROCESSED);
	}

	private Void handleSinkException(Throwable t) {
		logger.error("Error processing message", t instanceof CompletionException ? t.getCause() : t);
		return null;
	}

	private Collection<S> handlePollingException(Throwable t) {
		logger.error("Error polling for messages in queue {}.", this.pollingEndpointName, t);
		if (this.pollBackOffContext.get() == null) {
			logger.trace("Setting back off policy in queue {}", this.pollingEndpointName);
			this.pollBackOffContext.set(createBackOffContext());
		}
		return Collections.emptyList();
	}

	private BackOffContext createBackOffContext() {
		BackOffContext context = this.pollBackOffPolicy.start(null);
		return context != null ? context : new NoOpsBackOffContext();
	}

	private static class NoOpsBackOffContext implements BackOffContext {
	}

	private Collection<S> resetBackOffContext(Collection<S> messages) {
		if (this.pollBackOffContext.get() != null) {
			logger.trace("Polling successful, resetting back off context.");
			this.pollBackOffContext.set(null);
		}
		return messages;
	}

	private <F> CompletableFuture<F> managePollingFuture(CompletableFuture<F> pollingFuture) {
		this.pollingFutures.add(pollingFuture);
		pollingFuture.whenComplete((result, throwable) -> this.pollingFutures.remove(pollingFuture));
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
			logger.debug("{} for queue {} not running", getClass().getSimpleName(), this.pollingEndpointName);
		}
		synchronized (this.lifecycleMonitor) {
			logger.debug("Stopping {} for queue {}", getClass().getSimpleName(), this.pollingEndpointName);
			this.running = false;
			if (!waitExistingTasksToFinish()) {
				logger.warn("Tasks did not finish in {} seconds for queue {}, proceeding with shutdown",
						this.shutdownTimeout.getSeconds(), this.pollingEndpointName);
				this.pollingFutures.forEach(pollingFuture -> pollingFuture.cancel(true));
			}
			doStop();
			this.acknowledgmentProcessor.stop();
			logger.debug("{} for queue {} stopped", getClass().getSimpleName(), this.pollingEndpointName);
		}
	}

	protected void doStop() {
	}

	private boolean waitExistingTasksToFinish() {
		if (this.shutdownTimeout.isZero()) {
			logger.debug("Shutdown timeout set to zero for queue {} - not waiting for tasks to finish",
					this.pollingEndpointName);
			return false;
		}
		return this.backPressureHandler.drain(this.shutdownTimeout);
	}

}
