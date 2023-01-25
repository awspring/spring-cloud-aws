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
import java.util.function.Consumer;
import org.springframework.messaging.Message;

/**
 * Sqs-specific synchronous messaging operations for Standard and Fifo queues.
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface SqsOperations extends MessagingOperations {

	/**
	 * Send a message to a Standard SQS queue using the {@link SqsSendOptions} options.
	 * @param to a {@link SqsSendOptions} consumer.
	 * @return The {@link UUID} of the message.
	 */
	<T> SendResult<T> send(Consumer<SqsSendOptions<T>> to);

	/**
	 * Receive a message from a Standard SQS queue using the {@link SqsReceiveOptions} options.
	 * @param from a {@link SqsReceiveOptions} consumer.
	 * @return The message, or an empty collection if none is returned.
	 */
	<T> Optional<Message<T>> receive(Consumer<SqsReceiveOptions<T>> from);

	/**
	 * Receive a batch of messages from a Standard SQS queue using {@link SqsReceiveOptions}.
	 * @param from a {@link SqsReceiveOptions} consumer.
	 * @return The message, or an empty collection if none is returned.
	 */
	<T> Collection<Message<T>> receiveMany(Consumer<SqsReceiveOptions<T>> from);

}
