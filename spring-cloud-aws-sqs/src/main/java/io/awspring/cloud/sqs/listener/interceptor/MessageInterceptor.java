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
package io.awspring.cloud.sqs.listener.interceptor;

import java.util.Collection;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * Interface for intercepting messages before and after execution.
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface MessageInterceptor<T> {

	/**
	 * Perform an action on the message or return a different one before processing. Executed before processing. This
	 * method must not return null.
	 * @param message the message to be intercepted.
	 * @return a completable future containing the resulting message.
	 */
	default Message<T> intercept(Message<T> message) {
		return message;
	}

	/**
	 * Perform an action on the messages or return a different ones before processing. Executed before processing. This
	 * method must not return null or an empty collection.
	 * @param messages the message to be intercepted.
	 * @return a completable future containing the resulting message.
	 */
	default Collection<Message<T>> intercept(Collection<Message<T>> messages) {
		return messages;
	}

	/**
	 * Perform an action after the listener completes either with success or error.
	 * @param message the message to be intercepted.
	 */
	default void afterProcessing(Message<T> message, @Nullable Throwable t) {
	}

	/**
	 * Perform an action after the listener completes either with success or error.
	 * @param messages the messages to be intercepted.
	 */
	default void afterProcessing(Collection<Message<T>> messages, @Nullable Throwable t) {
	}

}
