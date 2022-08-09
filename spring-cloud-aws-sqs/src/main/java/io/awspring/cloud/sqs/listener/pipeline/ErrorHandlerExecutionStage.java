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
package io.awspring.cloud.sqs.listener.pipeline;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

/**
 * Stage responsible for executing the {@link AsyncErrorHandler}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class ErrorHandlerExecutionStage<T> implements MessageProcessingPipeline<T> {

	private static final Logger logger = LoggerFactory.getLogger(ErrorHandlerExecutionStage.class);

	private final AsyncErrorHandler<T> errorHandler;

	public ErrorHandlerExecutionStage(MessageProcessingConfiguration<T> context) {
		this.errorHandler = context.getErrorHandler();
	}

	@Override
	public CompletableFuture<Message<T>> process(CompletableFuture<Message<T>> messageFuture,
			MessageProcessingContext<T> context) {
		return CompletableFutures.exceptionallyCompose(messageFuture,
				t -> handleError(ListenerExecutionFailedException.unwrapMessage(t), t));
	}

	private CompletableFuture<Message<T>> handleError(Message<T> failedMessage, Throwable t) {
		logger.debug("Handling error {} for message {}", t, MessageHeaderUtils.getId(failedMessage));
		return CompletableFutures.exceptionallyCompose(
				this.errorHandler.handle(failedMessage, t).thenApply(theVoid -> failedMessage),
				eht -> CompletableFutures.failedFuture(maybeWrap(failedMessage, eht)));
	}

	private Throwable maybeWrap(Message<T> failedMessage, Throwable eht) {
		return ListenerExecutionFailedException.hasListenerException(eht) ? eht
				: new ListenerExecutionFailedException("Error handler returned an exception", eht, failedMessage);
	}

	@Override
	public CompletableFuture<Collection<Message<T>>> processMany(
			CompletableFuture<Collection<Message<T>>> messagesFuture, MessageProcessingContext<T> context) {
		return CompletableFutures.exceptionallyCompose(messagesFuture,
				t -> handleErrors(ListenerExecutionFailedException.unwrapMessages(t), t));
	}

	private CompletableFuture<Collection<Message<T>>> handleErrors(Collection<Message<T>> failedMessages, Throwable t) {
		logger.debug("Handling error {} for message {}", t, MessageHeaderUtils.getId(failedMessages));
		return CompletableFutures.exceptionallyCompose(
				this.errorHandler.handle(failedMessages, t).thenApply(theVoid -> failedMessages),
				eht -> CompletableFutures.failedFuture(new ListenerExecutionFailedException(
						"Error handler returned an exception", eht, failedMessages)));
	}

}
