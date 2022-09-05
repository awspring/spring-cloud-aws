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

import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.messaging.Message;

/**
 * Entrypoint for constructing a {@link MessageProcessingPipeline} {@link ComposingMessagePipelineStage}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessageProcessingPipelineBuilder<T> {

	private final Function<MessageProcessingConfiguration<T>, MessageProcessingPipeline<T>> pipelineFactory;

	public MessageProcessingPipelineBuilder(
			Function<MessageProcessingConfiguration<T>, MessageProcessingPipeline<T>> pipelineFactory) {
		this.pipelineFactory = pipelineFactory;
	}

	public static <T> MessageProcessingPipelineBuilder<T> first(
			Function<MessageProcessingConfiguration<T>, MessageProcessingPipeline<T>> pipelineFactory) {
		return new MessageProcessingPipelineBuilder<>(pipelineFactory);
	}

	public MessageProcessingPipelineBuilder<T> then(
			Function<MessageProcessingConfiguration<T>, MessageProcessingPipeline<T>> pipelineFactory) {
		return new MessageProcessingPipelineBuilder<>(configuration -> new ComposingMessagePipelineStage<>(
				this.pipelineFactory.apply(configuration), pipelineFactory.apply(configuration)));
	}

	public MessageProcessingPipelineBuilder<T> thenWrapWith(
			BiFunction<MessageProcessingConfiguration<T>, MessageProcessingPipeline<T>, MessageProcessingPipeline<T>> pipelineFactory) {
		return new MessageProcessingPipelineBuilder<>(
				configuration -> pipelineFactory.apply(configuration, this.pipelineFactory.apply(configuration)));
	}

	public MessageProcessingPipelineBuilder<T> thenInTheFuture(
			Function<MessageProcessingConfiguration<T>, MessageProcessingPipeline<T>> pipelineFactory) {
		return new MessageProcessingPipelineBuilder<>(configuration -> new FutureComposingMessagePipelineStage<>(
				this.pipelineFactory.apply(configuration), pipelineFactory.apply(configuration)));
	}

	public MessageProcessingPipeline<T> build(MessageProcessingConfiguration<T> configuration) {
		return this.pipelineFactory.apply(configuration);
	}

	private static class ComposingMessagePipelineStage<T> implements MessageProcessingPipeline<T> {

		private final MessageProcessingPipeline<T> first;

		private final MessageProcessingPipeline<T> second;

		private ComposingMessagePipelineStage(MessageProcessingPipeline<T> first, MessageProcessingPipeline<T> second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public CompletableFuture<Message<T>> process(Message<T> message, MessageProcessingContext<T> context) {
			return first.process(message, context).thenCompose(msg -> second.process(msg, context));
		}

		@Override
		public CompletableFuture<Collection<Message<T>>> process(Collection<Message<T>> messages,
				MessageProcessingContext<T> context) {
			return first.process(messages, context).thenCompose(msgs -> second.process(msgs, context));
		}
	}

	private static class FutureComposingMessagePipelineStage<T> implements MessageProcessingPipeline<T> {

		private final MessageProcessingPipeline<T> first;

		private final MessageProcessingPipeline<T> second;

		private FutureComposingMessagePipelineStage(MessageProcessingPipeline<T> first,
				MessageProcessingPipeline<T> second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public CompletableFuture<Message<T>> process(Message<T> message, MessageProcessingContext<T> context) {
			return second.process(first.process(message, context), context);
		}

		@Override
		public CompletableFuture<Collection<Message<T>>> process(Collection<Message<T>> messages,
				MessageProcessingContext<T> context) {
			return second.processMany(first.process(messages, context), context);
		}
	}

}
