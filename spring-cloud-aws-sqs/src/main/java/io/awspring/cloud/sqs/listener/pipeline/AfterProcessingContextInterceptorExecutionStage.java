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

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Stage responsible for executing the {@link AsyncMessageInterceptor}s.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class AfterProcessingContextInterceptorExecutionStage<T> implements MessageProcessingPipeline<T> {

	private static final Logger logger = LoggerFactory.getLogger(AfterProcessingContextInterceptorExecutionStage.class);

	private final MessageProcessingPipeline<T> pipeline;

	public AfterProcessingContextInterceptorExecutionStage(MessageProcessingConfiguration<T> configuration,
															   MessageProcessingPipeline<T> pipeline) {

		this.pipeline = pipeline;
	}

	@Override
	public CompletableFuture<Message<T>> process(Message<T> message, MessageProcessingContext<T> context) {
		logger.debug("Processing message {}", MessageHeaderUtils.getId(message));
		return this.pipeline.process(message, context).exceptionally(t -> logError(message, t)).thenCompose(msg -> context.getInterceptors().stream().reduce(CompletableFuture.completedFuture(msg),
			(messageFuture, interceptor) -> messageFuture.thenCompose(interceptor::afterProcessing), (a, b) -> a));
	}

	@Override
	public CompletableFuture<Collection<Message<T>>> process(Collection<Message<T>> messages, MessageProcessingContext<T> context) {
		logger.debug("Processing {} messages", messages.size());
		return this.pipeline.process(messages, context).exceptionally(t -> logError(messages, t)).thenCompose(msg -> context.getInterceptors().stream().reduce(CompletableFuture.completedFuture(msg),
			(messageFuture, interceptor) -> messageFuture.thenCompose(interceptor::afterProcessing), (a, b) -> a));
	}

	private Message<T> logError(Message<T> message, Throwable t) {
		logger.error("Error processing message {}", MessageHeaderUtils.getId(message), t);
		return message;
	}

	private Collection<Message<T>> logError(Collection<Message<T>> messages, Throwable t) {
		logger.error("Error processing messages {}", MessageHeaderUtils.getId(messages), t);
		return messages;
	}

}
