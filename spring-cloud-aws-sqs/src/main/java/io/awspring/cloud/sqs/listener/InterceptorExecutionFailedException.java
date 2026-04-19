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
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.messaging.Message;

/**
 * Exception thrown when a {@link io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor} throws during
 * before-processing execution. Contains the {@link Message} instance or instances for which the interceptor failed,
 * allowing downstream pipeline stages such as the error handler and acknowledgement handler to retrieve the message and
 * act accordingly.
 *
 * @author Tomaz Fernandes
 * @since 4.1
 * @see MessageProcessingException
 * @see ListenerExecutionFailedException
 */
public class InterceptorExecutionFailedException extends RuntimeException implements MessageProcessingException {

	private final Collection<Message<?>> failedMessages;

	public InterceptorExecutionFailedException(String message, Throwable cause, Message<?> failedMessage) {
		super(message, cause);
		this.failedMessages = Collections.singletonList(failedMessage);
	}

	public <T> InterceptorExecutionFailedException(String message, Throwable cause,
			Collection<Message<T>> failedMessages) {
		super(message, cause);
		this.failedMessages = failedMessages.stream().map(msg -> (Message<?>) msg).collect(Collectors.toList());
	}

	@Override
	public Collection<Message<?>> getFailedMessages() {
		return this.failedMessages;
	}

}
