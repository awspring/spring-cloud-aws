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
package io.awspring.cloud.messaging.support.listener;

import io.awspring.cloud.messaging.support.listener.acknowledgement.AsyncAckHandler;
import io.awspring.cloud.messaging.support.listener.acknowledgement.OnSuccessAckHandler;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractMessageListenerContainer<T> implements MessageListenerContainer, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMessageListenerContainer.class);

	private final AbstractContainerOptions<?> options;

	private boolean isRunning;

	private final TaskExecutor taskExecutor;

	private final Semaphore producersSemaphore;

	private final Object lifecycleMonitor = new Object();

	private final Collection<AsyncMessageProducer<T>> messageProducers;

	private final AsyncMessageListener<T> messageListener;

	private AsyncErrorHandler<T> errorHandler = new LoggingErrorHandler<>();

	private AsyncAckHandler<T> ackHandler = new OnSuccessAckHandler<>();

	private AsyncMessageInterceptor<T> messageInterceptor = null;

	protected AbstractMessageListenerContainer(AbstractContainerOptions<?> options, TaskExecutor taskExecutor,
			AsyncMessageListener<T> messageListener, Collection<AsyncMessageProducer<T>> producers) {
		this.messageListener = messageListener;
		handleCallbackListener(messageListener);
		this.messageProducers = producers;
		this.options = options;
		this.taskExecutor = taskExecutor;
		this.producersSemaphore = new Semaphore(options.getSimultaneousProduceCalls());
	}

	private void handleCallbackListener(AsyncMessageListener<T> messageListener) {
		if (messageListener instanceof CallbackMessageListener) {
			((CallbackMessageListener<T>) messageListener).addResultCallback(this::handleResult);
		}
	}

	public void setErrorHandler(AsyncErrorHandler<T> errorHandler) {
		Assert.notNull(errorHandler, "errorHandler cannot be null");
		this.errorHandler = errorHandler;
	}

	public void setAckHandler(AsyncAckHandler<T> ackHandler) {
		Assert.notNull(ackHandler, "ackHandler cannot be null");
		this.ackHandler = ackHandler;
	}

	public void setMessageInterceptor(AsyncMessageInterceptor<T> messageInterceptor) {
		Assert.notNull(messageInterceptor, "messageInterceptor cannot be null");
		this.messageInterceptor = messageInterceptor;
	}

	public AsyncMessageInterceptor<T> getMessageInterceptor() {
		return messageInterceptor;
	}

	@Override
	public void start() {
		logger.debug("Starting container {}", this);
		synchronized (this.lifecycleMonitor) {
			this.isRunning = true;
			doStart();
			this.taskExecutor.execute(this::produceAndProcessMessages);
		}
		logger.debug("Container started {}", this);
	}

	protected void doStart() {
	}

	private void produceAndProcessMessages() {
		while (this.isRunning) {
			this.messageProducers.forEach(producer -> {
				try {
					acquireSemaphore();
					producer.produce(options.getMessagesPerProduce(), options.getProduceTimeout())
							.thenComposeAsync(this::splitAndProcessMessages).handle(handleProcessingResult())
							.thenRun(releaseSemaphore());
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					logger.debug("Thread interrupted", e);
				}
				catch (Exception e) {
					logger.error("Error in ListenerContainer", e);
				}
			});
		}
	}

	protected CompletableFuture<?> splitAndProcessMessages(Collection<Message<T>> messages) {
		logger.trace("Received {} messages in Thread {}", messages.size(), Thread.currentThread().getName());
		return CompletableFuture
				.allOf(messages.stream().map(this::processMessageAsync).toArray(CompletableFuture[]::new));
	}

	protected CompletableFuture<?> processMessageAsync(Message<T> msg) {
		return CompletableFuture.supplyAsync(() -> processMessage(msg), this.taskExecutor).thenCompose(x -> x);
	}

	protected CompletableFuture<?> processMessage(Message<T> message) {
		logger.debug("Processing message {} in thread {}", message, Thread.currentThread().getName());
		CompletableFuture<Void> messageListenerResult = maybeIntercept(message, this.messageListener::onMessage);
		return this.messageListener instanceof CallbackMessageListener ? CompletableFuture.completedFuture(null)
				: messageListenerResult.handle((val, t) -> handleResult(message, t));
	}

	protected CompletableFuture<?> handleResult(Message<T> message, Throwable throwable) {
		return throwable == null ? this.ackHandler.onSuccess(message)
				: this.errorHandler.handleError(message, throwable)
						.thenCompose(val -> this.ackHandler.onError(message, throwable));
	}

	private CompletableFuture<Void> maybeIntercept(Message<T> message,
			Function<Message<T>, CompletableFuture<Void>> listener) {
		return this.messageInterceptor != null ? this.messageInterceptor.intercept(message).thenComposeAsync(listener)
				: listener.apply(message);
	}

	private void acquireSemaphore() throws InterruptedException {
		producersSemaphore.acquire();
		logger.trace("Semaphore acquired for producer {} in thread {} ", this, Thread.currentThread().getName());
	}

	private Runnable releaseSemaphore() {
		return () -> {
			this.producersSemaphore.release();
			logger.trace("Semaphore released for producer {} in thread {} ", this, Thread.currentThread().getName());
		};
	}

	protected BiFunction<Object, Throwable, Void> handleProcessingResult() {
		return (value, t) -> {
			if (t != null) {
				logger.error("Error handling messages in container {} ", this);
			}
			return null;
		};
	}

	@Override
	public void stop() {
		logger.debug("Stopping container {}", this);
		synchronized (this.lifecycleMonitor) {
			this.isRunning = false;
			doStop();
			if (this.taskExecutor instanceof DisposableBean) {
				try {
					((DisposableBean) this.taskExecutor).destroy();
				}
				catch (Exception e) {
					throw new IllegalStateException("Error shutting down TaskExecutor", e);
				}
			}
		}
		logger.debug("Container stopped {}", this);
	}

	protected void doStop() {
	}

	protected Collection<AsyncMessageProducer<T>> getMessageProducers() {
		return this.messageProducers;
	}

	@Override
	public boolean isRunning() {
		return this.isRunning;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.taskExecutor instanceof InitializingBean) {
			try {
				((InitializingBean) this.taskExecutor).afterPropertiesSet();
			}
			catch (Exception e) {
				throw new IllegalStateException("Could not initialize TaskExecutor", e);
			}
		}
	}
}
