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
public class BeforeProcessingContextInterceptorExecutionStage<T> implements MessageProcessingPipeline<T> {

	private static final Logger logger = LoggerFactory.getLogger(BeforeProcessingContextInterceptorExecutionStage.class);

	public BeforeProcessingContextInterceptorExecutionStage(MessageProcessingConfiguration<T> configuration) {
	}

	@Override
	public CompletableFuture<Message<T>> process(Message<T> message, MessageProcessingContext<T> context) {
		logger.trace("Processing message {}", MessageHeaderUtils.getId(message));
		return context.getInterceptors().stream().reduce(CompletableFuture.completedFuture(message),
			(messageFuture, interceptor) -> messageFuture.thenCompose(interceptor::intercept), (a, b) -> a);
	}

	@Override
	public CompletableFuture<Collection<Message<T>>> process(Collection<Message<T>> messages, MessageProcessingContext<T> context) {
		logger.trace("Processing messages {}", MessageHeaderUtils.getId(messages));
		return context.getInterceptors().stream().reduce(CompletableFuture.completedFuture(messages),
			(messageFuture, interceptor) -> messageFuture.thenCompose(interceptor::intercept), (a, b) -> a);
	}

}
