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
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.Message;

/**
 * Represents a stage in the processing pipeline that will be used to process {@link Message} instances.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface MessageProcessingPipeline<T> {

	default CompletableFuture<Message<T>> process(Message<T> message, MessageProcessingContext<T> context) {
		return CompletableFutures.failedFuture(new UnsupportedOperationException(
				"Single message handling not implemented by pipeline " + getClass().getSimpleName()));
	}

	default CompletableFuture<Collection<Message<T>>> process(Collection<Message<T>> messages,
			MessageProcessingContext<T> context) {
		return CompletableFutures.failedFuture(new UnsupportedOperationException(
				"Batch handling not implemented by pipeline " + getClass().getSimpleName()));
	}

	default CompletableFuture<Message<T>> process(CompletableFuture<Message<T>> message,
			MessageProcessingContext<T> context) {
		return CompletableFutures.failedFuture(new UnsupportedOperationException(
				"CompletableFuture single message handling not implemented by pipeline " + getClass().getSimpleName()));
	}

	default CompletableFuture<Collection<Message<T>>> processMany(CompletableFuture<Collection<Message<T>>> messages,
			MessageProcessingContext<T> context) {
		return CompletableFutures.failedFuture(new UnsupportedOperationException(
				"CompletableFuture batch handling not implemented by pipeline " + getClass().getSimpleName()));
	}

}
