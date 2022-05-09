/*
 * Copyright 2022 the original author or authors.
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
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Stage responsible for executing the {@link AsyncErrorHandler}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class ErrorHandlerExecutionStage<T> implements MessageProcessingPipeline<T> {

	private static final Logger logger = LoggerFactory.getLogger(ErrorHandlerExecutionStage.class);

	private final AsyncErrorHandler<T> errorHandler;

	private final MessageProcessingPipeline<T> wrapped;

	public ErrorHandlerExecutionStage(MessageProcessingConfiguration<T> context, MessageProcessingPipeline<T> wrapped) {
		this.errorHandler = context.getErrorHandler();
		this.wrapped = wrapped;
	}

	@Override
	public CompletableFuture<Message<T>> process(Message<T> message, MessageProcessingContext<T> context) {
		logger.trace("Processing message {}", MessageHeaderUtils.getId(message));
		return CompletableFutures.exceptionallyCompose(wrapped.process(message, context),
			t -> errorHandler.handleError(message, t).thenApply(theVoid -> message));
	}

	@Override
	public CompletableFuture<Collection<Message<T>>> process(Collection<Message<T>> messages, MessageProcessingContext<T> context) {
		logger.trace("Processing messages {}", MessageHeaderUtils.getId(messages));
		return CompletableFutures.exceptionallyCompose(wrapped.process(messages, context),
			t -> errorHandler.handleError(messages, t).thenApply(theVoid -> messages));
	}

}
