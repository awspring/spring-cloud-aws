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
package io.awspring.cloud.sqs.listener.errorhandler;

import io.awspring.cloud.sqs.listener.BatchVisibility;
import io.awspring.cloud.sqs.listener.Visibility;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.Message;

/**
 * A default error handler implementation for asynchronous message processing.
 *
 * <p>
 * This error handler attempts to set the SQS message visibility timeout to zero whenever an exception occurs,
 * effectively making the message immediately available for reprocessing.
 *
 * <p>
 * When AcknowledgementMode is set to ON_SUCCESS (the default value), returning a failed future will prevent the message
 * from being acknowledged
 *
 * @author Bruno Garcia
 * @author Rafael Pavarini
 */
public class ImmediateRetryAsyncErrorHandler<T> implements AsyncErrorHandler<T> {

	@Override
	public CompletableFuture<Void> handle(Message<T> message, Throwable t) {
		return changeTimeoutToZero(message).thenCompose(theVoid -> CompletableFuture.failedFuture(t));
	}

	@Override
	public CompletableFuture<Void> handle(Collection<Message<T>> messages, Throwable t) {
		return changeTimeoutToZero(messages).thenCompose(theVoid -> CompletableFuture.failedFuture(t));

	}

	private CompletableFuture<Void> changeTimeoutToZero(Message<T> message) {
		Visibility visibilityTimeout = ErrorHandlerVisibilityHelper.getVisibility(message);
		return visibilityTimeout.changeToAsync(0);
	}

	private CompletableFuture<Void> changeTimeoutToZero(Collection<Message<T>> messages) {
		BatchVisibility visibility = ErrorHandlerVisibilityHelper.getVisibility(messages);
		return visibility.changeToAsync(0);
	}
}
