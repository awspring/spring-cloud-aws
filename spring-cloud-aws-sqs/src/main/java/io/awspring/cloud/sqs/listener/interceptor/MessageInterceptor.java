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
	 * Intercept the message before processing.
	 * @param message the message to be intercepted.
	 */
	default Message<T> intercept(Message<T> message) {
		return message;
	}

	/**
	 * Intercept the messages before processing.
	 * @param messages the messages to be intercepted.
	 */
	default Collection<Message<T>> intercept(Collection<Message<T>> messages) {
		return messages;
	}

	default void afterProcessing(Message<T> message, Throwable t) {
	}

	default void afterProcessing(Collection<Message<T>> messages, Throwable t) {
	}

}
