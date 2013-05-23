/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.core;

/**
 * Simple operations interface to interact with the Amazon Webservices Simple Queueing Service. This interface allows
 * message to be sent and synchronously received without any dependencies to the underlying messaging system.
 * <p/>
 * <b>Note:</b>Receiving messages with this interface is synchronous and should be done very carefully. Consider using
 * a{@link org.elasticspring.messaging.config.annotation.QueueListener} method to be asynchronously called if new
 * messages appear on the queue.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public interface QueueingOperations {

	/**
	 * Converts and send the message payload to the Amazon SQS system. The payload will be first converted into a
	 * messaging specific format before being sent to the queueing system. This method will send all the method to a
	 * static destination which is configured by the underlying implementation.
	 *
	 * @param payload
	 * 		- the payload that will be converted and sent
	 */
	void convertAndSend(Object payload);

	/**
	 * Converts and send the message with the same semantics like {@link #convertAndSend(Object)} but with a dynamic
	 * destination name. The destination name might be a logical or a physical destination name which will be used to send
	 * the message.
	 *
	 * @param destinationName
	 * 		- the logical or physical destination name
	 * @param payload
	 * 		- the payload that will be converted and sent
	 */
	void convertAndSend(String destinationName, Object payload);

	/**
	 * Receives and converts one message from the configured default destination. This method is a synchronous call that
	 * will immediately return with one message or null in case of no message. This method is especially useful if the
	 * caller knows that he will receive exactly one message that he can process.
	 *
	 * @return - the converted object or null if there is no message in the configured default destination available
	 */
	Object receiveAndConvert();

	/**
	 * Receives and converts one message from the configured default destination like {@link #receiveAndConvert()} but
	 * provides additional support to automatically cast the object with the specific type. This method is a convenience
	 * method to allow type safe retrieval of messages. THis method is especially useful in case of data queues where all
	 * messages are of the same type.
	 *
	 * @param expectedType
	 * 		the class of the expected type to which the message should be cast
	 * @return the messages with the expected type of null in case of no message
	 * @throws IllegalArgumentException
	 * 		if the message is not of the expected type
	 */
	<T> T receiveAndConvert(Class<T> expectedType);

	/**
	 * Receives and converts one message from the destination name and returns the to the caller. This method takes a
	 * dynamic destination name which might be a logical or physical name to the queue.
	 *
	 * @param destinationName
	 * 		- the logical or physical destination name which will pe polled for a new message
	 * @return a converted message or null if there is not message available on the queue
	 */
	Object receiveAndConvert(String destinationName);

	/**
	 * Receives and converts one message like {@link #receiveAndConvert(String)} but with additional support to specify
	 * the
	 * expected type and received the casted object.
	 *
	 * @param destinationName
	 * 		- the logical or physical destination name which will pe polled for a new message
	 * @param expectedType
	 * 		the class of the expected type to which the message should be cast
	 * @return a converted message or null if there is not message available on the queue
	 */
	<T> T receiveAndConvert(String destinationName, Class<T> expectedType);
}