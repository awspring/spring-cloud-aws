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

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException;
import java.util.Collection;
import java.util.Collections;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 *
 * Base class for invoking an {@link InvocableHandlerMethod}.
 *
 * Also handles wrapping the failed messages into a {@link ListenerExecutionFailedException}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractMethodInvokingListenerAdapter<T> {

	private final InvocableHandlerMethod handlerMethod;

	/**
	 * Create an instance with the provided {@link InvocableHandlerMethod}.
	 * @param handlerMethod the handler method.
	 */
	protected AbstractMethodInvokingListenerAdapter(InvocableHandlerMethod handlerMethod) {
		Assert.notNull(handlerMethod, "handlerMethod cannot be null");
		this.handlerMethod = handlerMethod;
	}

	/**
	 * Invokes the handler for the provided message.
	 * @param message the message.
	 * @return the invocation result.
	 */
	protected final Object invokeHandler(Message<T> message) {
		try {
			return this.handlerMethod.invoke(message);
		}
		catch (Throwable t) {
			throw createListenerException(message, t);
		}
	}

	/**
	 * Invokes the handler for the provided messages.
	 * @param messages the messages.
	 * @return the invocation result.
	 */
	protected final Object invokeHandler(Collection<Message<T>> messages) {
		try {
			return this.handlerMethod.invoke(MessageBuilder.withPayload(messages).build());
		}
		catch (Throwable t) {
			throw createListenerException(messages, t);
		}
	}

	protected ListenerExecutionFailedException createListenerException(Collection<Message<T>> messages, Throwable t) {
		return new ListenerExecutionFailedException(
				"Listener failed to process messages " + MessageHeaderUtils.getId(messages), t, messages);
	}

	protected ListenerExecutionFailedException createListenerException(Message<T> message, Throwable t) {
		return createListenerException(Collections.singletonList(message), t);
	}

}
