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
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

/**
 * Stage responsible for executing the {@link AsyncMessageInterceptor}s after message processing.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class AfterProcessingInterceptorExecutionStage<T> implements MessageProcessingPipeline<T> {

	private static final Logger logger = LoggerFactory.getLogger(AfterProcessingInterceptorExecutionStage.class);

	private final Collection<AsyncMessageInterceptor<T>> messageInterceptors;

	public AfterProcessingInterceptorExecutionStage(MessageProcessingConfiguration<T> configuration) {
		this.messageInterceptors = configuration.getMessageInterceptors();
	}

	// @formatter:off
	@Override
	public CompletableFuture<Message<T>> process(CompletableFuture<Message<T>> messageFuture,
			MessageProcessingContext<T> context) {
		return CompletableFutures.handleCompose(messageFuture,
			(v, t) -> t == null
				? applyInterceptors(v, null, this.messageInterceptors)
				: applyInterceptors(ListenerExecutionFailedException.unwrapMessage(t), t, this.messageInterceptors)
				.thenCompose(msg -> CompletableFutures.failedFuture(t)));
	}

	private CompletableFuture<Message<T>> applyInterceptors(Message<T> message, Throwable t,
			Collection<AsyncMessageInterceptor<T>> messageInterceptors) {
		return messageInterceptors.stream()
			.reduce(CompletableFuture.<Void> completedFuture(null),
				(voidFuture, interceptor) -> voidFuture.thenCompose(theVoid -> interceptor.afterProcessing(message, t)),
				(a, b) -> a)
			.thenApply(theVoid -> message);
	}

	@Override
	public CompletableFuture<Collection<Message<T>>> processMany(
			CompletableFuture<Collection<Message<T>>> messagesFuture, MessageProcessingContext<T> context) {
		return CompletableFutures.handleCompose(messagesFuture,
			(v, t) -> t == null
				? applyInterceptors(v, null, this.messageInterceptors)
				: applyInterceptors(ListenerExecutionFailedException.unwrapMessages(t), t, this.messageInterceptors)
				.thenCompose(msg -> CompletableFutures.failedFuture(t)));
	}

	private CompletableFuture<Collection<Message<T>>> applyInterceptors(Collection<Message<T>> messages, Throwable t,
			Collection<AsyncMessageInterceptor<T>> messageInterceptors) {
		return messageInterceptors.stream()
			.reduce(CompletableFuture.<Void>completedFuture(null),
				(voidFuture, interceptor) -> voidFuture.thenCompose(theVoid -> interceptor.afterProcessing(messages, t)), (a, b) -> a)
			.thenApply(theVoid -> messages);
	}
	// @formatter:on
}
