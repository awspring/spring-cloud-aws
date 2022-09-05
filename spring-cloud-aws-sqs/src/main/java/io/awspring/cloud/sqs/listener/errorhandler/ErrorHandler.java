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
package io.awspring.cloud.sqs.listener.errorhandler;

import java.util.Collection;
import org.springframework.messaging.Message;

/**
 * Interface for handling errors. If the error handler completes normally, the message or messages will be considered
 * recovered for further processing purposes. If the message should not be considered recovered, an exception must be
 * thrown from the error handler.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface ErrorHandler<T> {

	/**
	 * Handle errors thrown when processing a {@link Message}.
	 * @param message the message.
	 * @param t the thrown exception.
	 */
	default void handle(Message<T> message, Throwable t) {
		throw new UnsupportedOperationException("Single message error handling not implemented");
	}

	/**
	 * Handle errors thrown when processing a batch of {@link Message}s.
	 * @param messages the messages.
	 * @param t the thrown exception.
	 */
	default void handle(Collection<Message<T>> messages, Throwable t) {
		throw new UnsupportedOperationException("Batch error handling not implemented");
	}

}
