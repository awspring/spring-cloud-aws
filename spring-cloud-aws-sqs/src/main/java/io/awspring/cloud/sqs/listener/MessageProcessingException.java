/*
 * Copyright 2013-2025 the original author or authors.
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
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;

/**
 * Implemented by exceptions that carry references to the {@link Message} instances that failed during processing.
 * Provides static utility methods for traversing the cause chain and extracting message references.
 *
 * @author Tomaz Fernandes
 * @since 4.1
 * @see ListenerExecutionFailedException
 * @see InterceptorExecutionFailedException
 */
public interface MessageProcessingException {

	/**
	 * Return the messages for which processing failed.
	 * @return the messages.
	 */
	Collection<Message<?>> getFailedMessages();

	/**
	 * Look for a potentially nested {@link MessageProcessingException} in the cause chain and if found return the
	 * wrapped {@link Message} instance.
	 * @param t the throwable
	 * @param <T> the message payload type.
	 * @return the message, or {@code null} if {@code t} is {@code null}.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	static <T> Message<T> unwrapMessage(@Nullable Throwable t) {
		Throwable exception = findProcessingException(t);
		return t == null ? null
				: exception != null
						? (Message<T>) ((MessageProcessingException) exception).getFailedMessages().iterator().next()
						: (Message<T>) wrapAndRethrowError(t);
	}

	/**
	 * Look for a potentially nested {@link MessageProcessingException} in the cause chain and if found return the
	 * wrapped {@link Message} instances.
	 * @param t the throwable
	 * @param <T> the message payload type.
	 * @return the messages, or {@code null} if {@code t} is {@code null}.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	static <T> Collection<Message<T>> unwrapMessages(@Nullable Throwable t) {
		Throwable exception = findProcessingException(t);
		return t == null ? null
				: exception != null
						? ((MessageProcessingException) exception).getFailedMessages().stream()
								.map(msg -> (Message<T>) msg).collect(Collectors.toList())
						: (Collection<Message<T>>) wrapAndRethrowError(t);
	}

	/**
	 * Check whether a {@link MessageProcessingException} is present anywhere in the cause chain of {@code t}.
	 * @param t the throwable.
	 * @return {@code true} if a {@link MessageProcessingException} is present.
	 */
	static boolean hasProcessingException(Throwable t) {
		return findProcessingException(t) != null;
	}

	@Nullable
	private static Throwable findProcessingException(@Nullable Throwable t) {
		return t == null ? null : t instanceof MessageProcessingException ? t : findProcessingException(t.getCause());
	}

	private static Object wrapAndRethrowError(Throwable t) {
		throw new IllegalArgumentException("No MessageProcessingException found to unwrap messages.", t);
	}

}
