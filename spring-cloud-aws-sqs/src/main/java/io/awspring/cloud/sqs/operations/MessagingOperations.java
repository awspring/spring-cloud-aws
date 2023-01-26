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

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * Synchronous messaging operations. Implementations should have defaults for {@link Nullable} fields, and can provide
 * chained methods interfaces to provide a more fluent API.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface MessagingOperations {

	/**
	 * Send a {@link Message} to the default queue with the provided payload. The payload will be serialized if
	 * necessary.
	 *
	 * @param payload the payload to send.
	 * @return The message's {@link UUID}.
	 */
	<T> SendResult<T> send(T payload);

	/**
	 * Send a message to the provided queue with the provided payload. The payload will be serialized if necessary.
	 *
	 * @param queue the queue to send the message to.
	 * @param payload the payload to send.
	 * @return The message's {@link UUID}.
	 */
	<T> SendResult<T> send(String queue, T payload);

	/**
	 * Send the provided message along with its headers to the provided queue. The payload will be serialized if
	 * necessary, and headers will be converted to the specific messaging system metadata types.
	 *
	 * @param queue the queue to send the message to.
	 * @param message the message to be sent.
	 * @return The message's {@link UUID}.
	 */
	<T> SendResult<T> send(String queue, Message<T> message);

	/**
	 * Send the provided messages along with their headers to the provided queue. The payloads will be serialized if
	 * necessary, and headers will be converted to the specific messaging system metadata types.
	 *
	 * @param queue the queue to send the messages to.
	 * @param messages the messages to be sent.
	 * @return The message's {@link UUID}.
	 */
	<T> SendResult.Batch<T> sendMany(String queue, Collection<Message<T>> messages);

	/**
	 * Receive a message from the default queue with default settings.
	 * @return the message or {@link Optional#empty()} if none is returned.
	 */
	Optional<Message<?>> receive();

	/**
	 * Receive a message from the provided queue and convert the payload to the provided class. If no message is
	 * returned after the default {@link Duration}, an {@link Optional#empty()} is returned.
	 *
	 * @param queue the queue from which to receive the messages.
	 * @param payloadClass the class to which the payload should be converted to.
	 * @return the message or {@link Optional#empty()} if none is returned.
	 */
	<T> Optional<Message<T>> receive(String queue, Class<T> payloadClass);

	/**
	 * Receive a batch of messages from the default queue with default settings.
	 * @return The messages, or an empty collection if none is returned.
	 */
	Collection<Message<?>> receiveMany();

	/**
	 * Receive a batch of messages from the provided queue and convert the payloads to the provided class. If no message
	 * is returned after the default {@link Duration}, an empty collection is returned.
	 *
	 * @param queue the queue from which to receive the messages.
	 * @param payloadClass the class to which the payloads should be converted to.
	 * @return The messages, or an empty collection if none is returned.
	 */
	<T> Collection<Message<T>> receiveMany(String queue, Class<T> payloadClass);

}
