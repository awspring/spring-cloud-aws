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
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Exception thrown when the {@link AsyncMessageListener} completes with an exception. Contains the {@link Message}
 * instance or instances which execution failed, as well as some convenience methods for handling such messages.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class ListenerExecutionFailedException extends RuntimeException implements MessageProcessingException {

	private final Collection<Message<?>> failedMessages;

	public ListenerExecutionFailedException(String message, Throwable cause, Message<?> failedMessage) {
		super(message, cause);
		this.failedMessages = Collections.singletonList(failedMessage);
	}

	public <T> ListenerExecutionFailedException(String message, Throwable cause,
			Collection<Message<T>> failedMessages) {
		super(message, cause);
		this.failedMessages = failedMessages.stream().map(msg -> (Message<?>) msg).collect(Collectors.toList());
	}

	/**
	 * Return the message which listener execution failed.
	 * @return the message.
	 */
	public Message<?> getFailedMessage() {
		Assert.isTrue(this.failedMessages.size() == 1, () -> "Not a unique failed message: " + this.failedMessages);
		return this.failedMessages.iterator().next();
	}

	/**
	 * Return the messages which listener execution failed.
	 * @return the messages.
	 */
	public Collection<Message<?>> getFailedMessages() {
		return this.failedMessages;
	}

	/**
	 * Look for a potentially nested {@link MessageProcessingException} in the cause chain and if found return the
	 * wrapped {@link Message} instance.
	 * @param t the throwable
	 * @param <T> the message type.
	 * @return the message.
	 * @deprecated use {@link MessageProcessingException#unwrapMessage(Throwable)} instead.
	 */
	@Deprecated
	@Nullable
	public static <T> Message<T> unwrapMessage(Throwable t) {
		return MessageProcessingException.unwrapMessage(t);
	}

	/**
	 * Look for a potentially nested {@link MessageProcessingException} in the cause chain and if found return the
	 * wrapped {@link Message} instances.
	 * @param t the throwable
	 * @param <T> the message type.
	 * @return the messages.
	 * @deprecated use {@link MessageProcessingException#unwrapMessages(Throwable)} instead.
	 */
	@Deprecated
	@Nullable
	public static <T> Collection<Message<T>> unwrapMessages(Throwable t) {
		return MessageProcessingException.unwrapMessages(t);
	}

	/**
	 * Check whether a {@link MessageProcessingException} is present anywhere in the cause chain of {@code t}.
	 * @param t the throwable.
	 * @return whether a {@link MessageProcessingException} is present.
	 * @deprecated use {@link MessageProcessingException#hasProcessingException(Throwable)} instead.
	 */
	@Deprecated
	public static boolean hasListenerException(Throwable t) {
		return MessageProcessingException.hasProcessingException(t);
	}

}
