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
package io.awspring.cloud.sqs.listener;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Exception thrown when the {@link AsyncMessageListener} completes with an exception.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class ListenerExecutionFailedException extends RuntimeException {

	private final Collection<Message<?>> failedMessages;

	public ListenerExecutionFailedException(String message, Throwable cause, Message<?> failedMessage) {
		super(message, cause);
		this.failedMessages = Collections.singletonList(failedMessage);
	}

	public <T> ListenerExecutionFailedException(String message, Throwable cause,
			Collection<Message<T>> failedMessages) {
		super(message, cause);
		this.failedMessages = failedMessages.stream().map(msg -> (Message<?>) msg).collect(Collectors.toList());
		;
	}

	public Message<?> getFailedMessage() {
		return this.failedMessages.iterator().next();
	}

	public Collection<Message<?>> getFailedMessages() {
		return this.failedMessages;
	}

	// @formatter:off
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> Message<T> unwrapMessage(Throwable t) {
		Throwable exception = findListenerException(t);
		return t == null
				? null
				: exception != null
					? (Message<T>) ((ListenerExecutionFailedException) exception).getFailedMessage()
					: (Message<T>) createDefaultErrorMessage(t);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> Collection<Message<T>> unwrapMessages(Throwable t) {
		Throwable exception = findListenerException(t);
		return t == null
			? null
			: exception != null
				? ((ListenerExecutionFailedException) exception).getFailedMessages().stream().map(msg -> (Message<T>) msg).collect(Collectors.toList())
				: Collections.singletonList((Message<T>) createDefaultErrorMessage(t));
	}

	@Nullable
	private static Throwable findListenerException(Throwable t) {
		return t == null
			? null
			: t instanceof ListenerExecutionFailedException
				? t
				: t.getCause();
	}
	// @formatter:on

	private static Message<Throwable> createDefaultErrorMessage(Throwable t) {
		return MessageBuilder.withPayload(t).build();
	}

	public static boolean hasListenerException(Throwable t) {
		return findListenerException(t) != null;
	}

}
