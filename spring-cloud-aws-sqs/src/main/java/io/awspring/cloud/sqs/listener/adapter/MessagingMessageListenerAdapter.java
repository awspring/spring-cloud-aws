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
package io.awspring.cloud.sqs.listener.adapter;

import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException;
import java.util.Collection;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.support.GenericMessage;

/**
 *
 * Base class for invoking a {@link InvocableHandlerMethod}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class MessagingMessageListenerAdapter<T> {

	private final InvocableHandlerMethod handlerMethod;

	protected MessagingMessageListenerAdapter(InvocableHandlerMethod handlerMethod) {
		this.handlerMethod = handlerMethod;
	}

	protected final Object invokeHandler(Message<T> message) {
		try {
			return handlerMethod.invoke(message);
		}
		catch (Exception ex) {
			throw new ListenerExecutionFailedException("Listener failed to process message", ex);
		}
	}

	protected final Object invokeHandler(Collection<Message<T>> messages) {
		try {
			return handlerMethod.invoke(new GenericMessage<>(messages));
		}
		catch (Exception ex) {
			throw new ListenerExecutionFailedException("Listener failed to process message", ex);
		}
	}

}
