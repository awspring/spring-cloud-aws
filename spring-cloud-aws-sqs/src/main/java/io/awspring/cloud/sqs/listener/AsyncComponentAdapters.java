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

import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import org.springframework.messaging.Message;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for adapting blocking components to asynchronous
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class AsyncComponentAdapters {

	private AsyncComponentAdapters() {
	}

	/**
	 * Adapt the provided {@link ErrorHandler} to an {@link AsyncErrorHandler}
	 * @param errorHandler the handler to be adapted
	 * @param <T> the message payload type
	 * @return the adapted component.
	 */
	public static <T> AsyncErrorHandler<T> adapt(ErrorHandler<T> errorHandler) {
		return new AsyncErrorHandler<T>() {
			@Override
			public CompletableFuture<Void> handleError(Message<T> message, Throwable t) {
				return AsyncExecutionAdapters.adaptFromBlocking(() -> errorHandler.handle(message, t));
			}

			@Override
			public CompletableFuture<Void> handleError(Collection<Message<T>> messages, Throwable t) {
				return AsyncExecutionAdapters.adaptFromBlocking(() -> errorHandler.handle(messages, t));
			}
		};
	}

	/**
	 * Adapt the provided {@link MessageInterceptor} to an {@link AsyncMessageInterceptor}
	 * @param messageInterceptor the interceptor to be adapted
	 * @param <T> the message payload type
	 * @return the adapted component.
	 */
	public static <T> AsyncMessageInterceptor<T> adapt(MessageInterceptor<T> messageInterceptor) {
		return new AsyncMessageInterceptor<T>() {
			@Override
			public CompletableFuture<Message<T>> intercept(Message<T> message) {
				return AsyncExecutionAdapters.adaptFromBlocking(() -> messageInterceptor.intercept(message));
			}

			@Override
			public CompletableFuture<Collection<Message<T>>> intercept(Collection<Message<T>> messages) {
				return AsyncExecutionAdapters.adaptFromBlocking(() -> messageInterceptor.intercept(messages));
			}
		};
	}

	/**
	 * Adapt the provided {@link MessageListener} to an {@link AsyncMessageListener}
	 * @param messageListener the listener to be adapted
	 * @param <T> the message payload type
	 * @return the adapted component.
	 */
	public static <T> AsyncMessageListener<T> adapt(MessageListener<T> messageListener) {
		return new AsyncMessageListener<T>() {
			@Override
			public CompletableFuture<Void> onMessage(Message<T> message) {
				return AsyncExecutionAdapters.adaptFromBlocking(() -> messageListener.onMessage(message));
			}

			@Override
			public CompletableFuture<Void> onMessage(Collection<Message<T>> messages) {
				return AsyncExecutionAdapters.adaptFromBlocking(() -> messageListener.onMessage(messages));
			}
		};
	}

}
