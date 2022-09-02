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
package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.MessageExecutionThread;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.AsyncAcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Utility class for adapting blocking components to their asynchronous variants.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class AsyncComponentAdapters {

	private static final Logger logger = LoggerFactory.getLogger(AsyncComponentAdapters.class);

	private AsyncComponentAdapters() {
	}

	/**
	 * Adapt the provided {@link ErrorHandler} to an {@link AsyncErrorHandler}
	 * @param errorHandler the handler to be adapted
	 * @param <T> the message payload type
	 * @return the adapted component.
	 */
	public static <T> AsyncErrorHandler<T> adapt(ErrorHandler<T> errorHandler) {
		return new BlockingErrorHandlerAdapter<>(errorHandler);
	}

	/**
	 * Adapt the provided {@link MessageInterceptor} to an {@link AsyncMessageInterceptor}
	 * @param messageInterceptor the interceptor to be adapted
	 * @param <T> the message payload type
	 * @return the adapted component.
	 */
	public static <T> AsyncMessageInterceptor<T> adapt(MessageInterceptor<T> messageInterceptor) {
		return new BlockingMessageInterceptorAdapter<>(messageInterceptor);
	}

	/**
	 * Adapt the provided {@link MessageListener} to an {@link AsyncMessageListener}
	 * @param messageListener the listener to be adapted
	 * @param <T> the message payload type
	 * @return the adapted component.
	 */
	public static <T> AsyncMessageListener<T> adapt(MessageListener<T> messageListener) {
		return new BlockingMessageListenerAdapter<>(messageListener);
	}

	public static <T> AsyncAcknowledgementResultCallback<T> adapt(
			AcknowledgementResultCallback<T> acknowledgementResultCallback) {
		return new BlockingAcknowledgementResultCallbackAdapter<>(acknowledgementResultCallback);
	}

	/**
	 * Base class for BlockingComponentAdapters.
	 * @see io.awspring.cloud.sqs.MessageExecutionThreadFactory
	 * @see io.awspring.cloud.sqs.MessageExecutionThread
	 */
	protected static class AbstractThreadingComponentAdapter implements TaskExecutorAware {

		private TaskExecutor taskExecutor;

		@Override
		public void setTaskExecutor(TaskExecutor taskExecutor) {
			this.taskExecutor = taskExecutor;
		}

		protected <T> CompletableFuture<T> execute(Supplier<T> executable) {
			if (Thread.currentThread() instanceof MessageExecutionThread) {
				logger.trace("Already in a {}, not switching", MessageExecutionThread.class.getSimpleName());
				return supplyInSameThread(executable);
			}
			logger.trace("Not in a {}, submitting to executor", MessageExecutionThread.class.getSimpleName());
			Assert.notNull(this.taskExecutor, "Task executor not set");
			return supplyInNewThread(executable);
		}

		protected CompletableFuture<Void> execute(Runnable executable) {
			if (Thread.currentThread() instanceof MessageExecutionThread) {
				logger.trace("Already in a {}, not switching", MessageExecutionThread.class.getSimpleName());
				return runInSameThread(executable);
			}
			logger.trace("Not in a {}, submitting to executor", MessageExecutionThread.class.getSimpleName());
			Assert.notNull(this.taskExecutor, "Task executor not set");
			return runInNewThread(executable);
		}

		private CompletableFuture<Void> runInSameThread(Runnable blockingProcess) {
			try {
				blockingProcess.run();
				return CompletableFuture.completedFuture(null);
			}
			catch (Exception e) {
				return CompletableFutures.failedFuture(wrapWithBlockingException(e));
			}
		}

		private CompletableFuture<Void> runInNewThread(Runnable blockingProcess) {
			try {
				return CompletableFutures.exceptionallyCompose(
						CompletableFuture.runAsync(blockingProcess, this.taskExecutor),
						t -> CompletableFutures.failedFuture(wrapWithBlockingException(t)));
			}
			catch (Exception e) {
				return CompletableFutures.failedFuture(wrapWithBlockingException(e));
			}
		}

		private <T> CompletableFuture<T> supplyInSameThread(Supplier<T> blockingProcess) {
			try {
				return CompletableFuture.completedFuture(blockingProcess.get());
			}
			catch (Exception e) {
				return CompletableFutures.failedFuture(wrapWithBlockingException(e));
			}
		}

		private <T> CompletableFuture<T> supplyInNewThread(Supplier<T> blockingProcess) {
			try {
				return CompletableFutures.exceptionallyCompose(
						CompletableFuture.supplyAsync(blockingProcess, this.taskExecutor),
						t -> CompletableFutures.failedFuture(wrapWithBlockingException(t)));
			}
			catch (Exception e) {
				return CompletableFutures.failedFuture(wrapWithBlockingException(e));
			}
		}

		private AsyncAdapterBlockingExecutionFailedException wrapWithBlockingException(Throwable t) {
			return new AsyncAdapterBlockingExecutionFailedException(
					"Error executing action in " + this.getClass().getSimpleName(),
					t instanceof CompletionException ? t.getCause() : t);
		}

	}

	private static class BlockingMessageInterceptorAdapter<T> extends AbstractThreadingComponentAdapter
			implements AsyncMessageInterceptor<T> {

		private final MessageInterceptor<T> blockingMessageInterceptor;

		public BlockingMessageInterceptorAdapter(MessageInterceptor<T> blockingMessageInterceptor) {
			this.blockingMessageInterceptor = blockingMessageInterceptor;
		}

		@Override
		public CompletableFuture<Message<T>> intercept(Message<T> message) {
			return execute(() -> this.blockingMessageInterceptor.intercept(message));
		}

		@Override
		public CompletableFuture<Collection<Message<T>>> intercept(Collection<Message<T>> messages) {
			return execute(() -> this.blockingMessageInterceptor.intercept(messages));
		}

		@Override
		public CompletableFuture<Void> afterProcessing(Message<T> message, Throwable t) {
			return execute(() -> this.blockingMessageInterceptor.afterProcessing(message, t));
		}

		@Override
		public CompletableFuture<Void> afterProcessing(Collection<Message<T>> messages, Throwable t) {
			return execute(() -> this.blockingMessageInterceptor.afterProcessing(messages, t));
		}
	}

	private static class BlockingMessageListenerAdapter<T> extends AbstractThreadingComponentAdapter
			implements AsyncMessageListener<T> {

		private final MessageListener<T> blockingMessageListener;

		public BlockingMessageListenerAdapter(MessageListener<T> blockingMessageListener) {
			this.blockingMessageListener = blockingMessageListener;
		}

		@Override
		public CompletableFuture<Void> onMessage(Message<T> message) {
			return execute(() -> this.blockingMessageListener.onMessage(message));
		}

		@Override
		public CompletableFuture<Void> onMessage(Collection<Message<T>> messages) {
			return execute(() -> this.blockingMessageListener.onMessage(messages));
		}
	}

	private static class BlockingErrorHandlerAdapter<T> extends AbstractThreadingComponentAdapter
			implements AsyncErrorHandler<T> {

		private final ErrorHandler<T> blockingErrorHandler;

		public BlockingErrorHandlerAdapter(ErrorHandler<T> blockingErrorHandler) {
			this.blockingErrorHandler = blockingErrorHandler;
		}

		@Override
		public CompletableFuture<Void> handle(Message<T> message, Throwable t) {
			return execute(() -> this.blockingErrorHandler.handle(message, t));
		}

		@Override
		public CompletableFuture<Void> handle(Collection<Message<T>> messages, Throwable t) {
			return execute(() -> this.blockingErrorHandler.handle(messages, t));
		}

	}

	private static class BlockingAcknowledgementResultCallbackAdapter<T> extends AbstractThreadingComponentAdapter
			implements AsyncAcknowledgementResultCallback<T> {

		private final AcknowledgementResultCallback<T> blockingAcknowledgementResultCallback;

		public BlockingAcknowledgementResultCallbackAdapter(
				AcknowledgementResultCallback<T> blockingAcknowledgementResultCallback) {
			this.blockingAcknowledgementResultCallback = blockingAcknowledgementResultCallback;
		}

		@Override
		public CompletableFuture<Void> onSuccess(Collection<Message<T>> messages) {
			return execute(() -> this.blockingAcknowledgementResultCallback.onSuccess(messages));
		}

		@Override
		public CompletableFuture<Void> onFailure(Collection<Message<T>> messages, Throwable t) {
			return execute(() -> this.blockingAcknowledgementResultCallback.onFailure(messages, t));
		}
	}
}
