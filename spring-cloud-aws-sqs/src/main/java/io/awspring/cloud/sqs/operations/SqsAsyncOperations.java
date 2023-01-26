/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sqs.operations;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.springframework.messaging.Message;

/**
 * Sqs-specific asynchronous messaging operations for Standard and Fifo queues.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface SqsAsyncOperations extends AsyncMessagingOperations {

	/**
	 * Send a message to a Standard SQS queue using {@link SqsSendOptions}.
	 * @param to a {@link SqsSendOptions} consumer.
	 * @return a {@link CompletableFuture} to be completed with the {@link UUID} of the message.
	 */
	<T> CompletableFuture<SendResult<T>> sendAsync(Consumer<SqsSendOptions<T>> to);

	/**
	 * Receive a message from a Standard SQS queue using the {@link SqsReceiveOptions} options.
	 * @param from a {@link SqsReceiveOptions} consumer.
	 * @return a {@link CompletableFuture} to be completed with the message, or {@link Optional#empty()} if none is
	 * returned.
	 */
	CompletableFuture<Optional<Message<?>>> receiveAsync(Consumer<SqsReceiveOptions> from);

	/**
	 * Receive a message from a Standard SQS queue using the {@link SqsReceiveOptions} options.
	 * @param from a {@link SqsReceiveOptions} consumer.
	 * @return a {@link CompletableFuture} to be completed with the message, or {@link Optional#empty()} if none is
	 * returned.
	 */
	<T> CompletableFuture<Optional<Message<T>>> receiveAsync(Consumer<SqsReceiveOptions> from, Class<T> payloadClass);

	/**
	 * Receive a batch of messages from a Standard SQS queue using the {@link SqsReceiveOptions} options.
	 * @param from a {@link SqsReceiveOptions} consumer.
	 * @return a {@link CompletableFuture} to be completed with the messages, or an empty collection if none is
	 * returned.
	 */
	CompletableFuture<Collection<Message<?>>> receiveManyAsync(Consumer<SqsReceiveOptions> from);

	/**
	 * Receive a batch of messages from a Standard SQS queue using the {@link SqsReceiveOptions} options.
	 * @param from a {@link SqsReceiveOptions} consumer.
	 * @return a {@link CompletableFuture} to be completed with the messages, or an empty collection if none is
	 * returned.
	 */
	<T> CompletableFuture<Collection<Message<T>>> receiveManyAsync(Consumer<SqsReceiveOptions> from,
			Class<T> payloadClass);

}
