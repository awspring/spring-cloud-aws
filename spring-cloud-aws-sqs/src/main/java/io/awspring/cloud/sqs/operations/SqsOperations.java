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
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

/**
 * Sqs-specific synchronous messaging operations for Standard and Fifo queues.
 * <p>
 * Note that the Standard queue methods can be used for Fifo queues as long as necessary headers are added for required
 * attributes such as message deduplication id. See {@link io.awspring.cloud.sqs.listener.SqsHeaders} for reference of
 * available headers.
 * <p>
 * Fifo queue methods accept the required attributes and add a random value if none is specified.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @param <T> the payload type
 */
public interface SqsOperations<T> extends MessagingOperations<T> {

	/**
	 * Send a message to a Standard SQS queue using the {@link SqsSendOptions.Standard} options.
	 * @param to a {@link SqsSendOptions.Standard} consumer.
	 * @return The {@link UUID} of the message.
	 */
	SendResult<T> send(Consumer<SqsSendOptions.Standard<T>> to);

	/**
	 * Send a message to a Fifo SQS queue using the {@link SqsSendOptions.Fifo} options.
	 * @param to a {@link SqsSendOptions.Fifo} consumer.
	 * @return The {@link UUID} of the message.
	 */
	SendResult<T> sendFifo(Consumer<SqsSendOptions.Fifo<T>> to);

	/**
	 * Send a batch of messages to a Fifo SQS queue.
	 * @param endpoint the endpoint to which to send the messages.
	 * @param messages the messages.
	 * @return the {@link SendMessageBatchResponse}
	 */
	SendResult.Batch<T> sendManyFifo(String endpoint, Collection<Message<T>> messages);

	/**
	 * Receive a message from a Standard SQS queue using the {@link SqsReceiveOptions.Standard} options.
	 * @param from a {@link SqsReceiveOptions.Standard} consumer.
	 * @return The message, or an empty collection if none is returned.
	 */
	Optional<Message<T>> receive(Consumer<SqsReceiveOptions.Standard<T>> from);

	/**
	 * Receive a message from a Fifo SQS queue using the {@link SqsReceiveOptions.Fifo} options.
	 * @param from a {@link SqsReceiveOptions.Fifo} consumer.
	 * @return The message, or an empty collection if none is returned.
	 */
	Optional<Message<T>> receiveFifo(Consumer<SqsReceiveOptions.Fifo<T>> from);

	/**
	 * Receive a batch of messages from a Standard SQS queue using the {@link SqsReceiveOptions.Standard} options.
	 * @param from a {@link SqsReceiveOptions.Standard} consumer.
	 * @return The message, or an empty collection if none is returned.
	 */
	Collection<Message<T>> receiveMany(Consumer<SqsReceiveOptions.Standard<T>> from);

	/**
	 * Receive a batch of messages from a Fifo SQS queue using the {@link SqsReceiveOptions.Fifo} options.
	 * @param from a {@link SqsReceiveOptions.Fifo} consumer.
	 * @return The message, or an empty collection if none is returned.
	 */
	Collection<Message<T>> receiveManyFifo(Consumer<SqsReceiveOptions.Fifo<T>> from);

}
