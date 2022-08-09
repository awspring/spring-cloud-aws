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
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementHandler;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

/**
 * Stage responsible for executing the {@link AcknowledgementHandler}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class AcknowledgementHandlerExecutionStage<T> implements MessageProcessingPipeline<T> {

	private static final Logger logger = LoggerFactory.getLogger(AcknowledgementHandlerExecutionStage.class);

	private final AcknowledgementHandler<T> acknowledgementHandler;

	public AcknowledgementHandlerExecutionStage(MessageProcessingConfiguration<T> configuration) {
		this.acknowledgementHandler = configuration.getAckHandler();
	}

	@Override
	public CompletableFuture<Message<T>> process(CompletableFuture<Message<T>> messageFuture,
			MessageProcessingContext<T> context) {
		return CompletableFutures.handleCompose(messageFuture, (v, t) -> t == null
				? this.acknowledgementHandler.onSuccess(v, context.getAcknowledgmentCallback()).thenApply(theVoid -> v)
				: this.acknowledgementHandler
						.onError(ListenerExecutionFailedException.unwrapMessage(t), t,
								context.getAcknowledgmentCallback())
						.thenCompose(theVoid -> CompletableFutures.failedFuture(t)));
	}

	@Override
	public CompletableFuture<Collection<Message<T>>> processMany(
			CompletableFuture<Collection<Message<T>>> messagesFuture, MessageProcessingContext<T> context) {
		return CompletableFutures.handleCompose(messagesFuture, (v, t) -> {
			Collection<Message<T>> originalMessages = ListenerExecutionFailedException.unwrapMessages(t);
			return t == null
					? this.acknowledgementHandler.onSuccess(v, context.getAcknowledgmentCallback())
							.thenApply(theVoid -> v)
					: this.acknowledgementHandler.onError(originalMessages, t, context.getAcknowledgmentCallback())
							.thenApply(theVoid -> originalMessages);
		});
	}

}
