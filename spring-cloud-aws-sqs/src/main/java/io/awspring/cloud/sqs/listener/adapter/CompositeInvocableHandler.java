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
package io.awspring.cloud.sqs.listener.adapter;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * A composite of {@link InvocableHandlerMethod} that delegates message handling to a list of registered handlers,
 * selecting the appropriate one based on the received message payload type.
 *
 * <p>
 * If no matching handler is found, the {@link CompositeInvocableHandler#defaultHandler} is invoked. If a default
 * handler is not configured, an {@link IllegalStateException} is thrown.
 *
 * <p>
 * This class also does caching of resolved handlers and check for ambiguous handler methods.
 *
 * @author José Iêdo
 */
public class CompositeInvocableHandler {

	private final List<InvocableHandlerMethod> handlers;

	private final ConcurrentMap<Class<?>, InvocableHandlerMethod> cachedHandlers = new ConcurrentHashMap<>();

	private final @Nullable InvocableHandlerMethod defaultHandler;

	/**
	 * Create a new CompositeInvocableHandler instance with the given handler.
	 *
	 * @param handler the handler.
	 */
	public CompositeInvocableHandler(InvocableHandlerMethod handler) {
		this(Collections.emptyList(), handler);
	}

	/**
	 * Create a new CompositeInvocableHandler instance with the given handlers.
	 *
	 * @param handlers the handlers.
	 * @param defaultHandler the default handler.
	 */
	public CompositeInvocableHandler(List<InvocableHandlerMethod> handlers,
			@Nullable InvocableHandlerMethod defaultHandler) {
		this.handlers = handlers;
		this.defaultHandler = defaultHandler;
	}

	/**
	 * Invoke the appropriate handler for the given message. If no handler is found, the default handler is invoked.
	 * @param message the message to handle
	 * @return the result of the handler invocation
	 * @throws IllegalStateException if no handler is found and no default handler is configured.
	 */
	@Nullable
	public Object invoke(Message<?> message) throws Exception {
		if (handlers.isEmpty()) {
			if (defaultHandler == null) {
				throw new IllegalStateException("No handler found for message: " + message);
			}
			return defaultHandler.invoke(message);
		}

		Class<?> payloadClass = message.getPayload().getClass();
		InvocableHandlerMethod handler = getHandlerForPayload(payloadClass);
		return handler.invoke(message);
	}

	/**
	 * Get the {@link InvocableHandlerMethod} for the provided type.
	 *
	 * @param payloadClass the payload class
	 * @return the handler.
	 */
	private InvocableHandlerMethod getHandlerForPayload(Class<?> payloadClass) {
		InvocableHandlerMethod handler = this.cachedHandlers.get(payloadClass);
		if (handler == null) {
			handler = findHandlerForPayload(payloadClass);
			if (handler == null) {
				throw new IllegalStateException("No handler found for payload type: " + payloadClass);
			}
			this.cachedHandlers.putIfAbsent(payloadClass, handler);
		}
		return handler;
	}

	/**
	 * Finds the appropriate handler method for the given payload type. Iterates through the list of handlers to find a
	 * match for the payload type. If multiple handlers match and none of them is the default handler, an exception is
	 * thrown. If no handler is found, the default handler is returned (if available).
	 *
	 * @param payloadClass the class of the payload to find a handler for
	 * @return the matching {@link InvocableHandlerMethod}, or the default handler if no match is found
	 * @throws IllegalArgumentException if multiple non-default handlers match the payload type
	 */
	@Nullable
	protected InvocableHandlerMethod findHandlerForPayload(Class<?> payloadClass) {
		InvocableHandlerMethod result = null;

		for (InvocableHandlerMethod handler : this.handlers) {
			if (matchHandlerMethod(payloadClass, handler)) {
				if (result != null && !result.equals(this.defaultHandler)) {
					if (!handler.equals(this.defaultHandler)) {
						throw new IllegalArgumentException(
								"Ambiguous handler method for payload type: " + payloadClass);
					}
				}
				else {
					result = handler;
				}
			}
		}

		return result != null ? result : this.defaultHandler;
	}

	/**
	 * Checks if the given handler method matches the payload type. If a match is found, the corresponding
	 * {@link MethodParameter} is cached.
	 *
	 * @param payloadClass the class of the payload
	 * @param handler the handler method to check
	 * @return true if the handler method matches the payload type, false otherwise
	 */
	private boolean matchHandlerMethod(Class<?> payloadClass, InvocableHandlerMethod handler) {
		Method method = handler.getMethod();
		MethodParameter foundCandidate = findCandidate(payloadClass, method);
		return foundCandidate != null;
	}

	/**
	 * Finds the method that matches the given payload type. If multiple parameters match, an exception is thrown.
	 *
	 * @param payloadClass the class of the payload
	 * @param method the method to inspect
	 * @return the matching {@link MethodParameter}, or null if no match is found
	 * @throws IllegalArgumentException if multiple parameters match the payload type
	 */
	@Nullable
	private MethodParameter findCandidate(Class<?> payloadClass, Method method) {
		MethodParameter foundCandidate = null;
		for (int i = 0; i < method.getParameterCount(); i++) {
			MethodParameter methodParameter = new MethodParameter(method, i);
			if (isPayloadAssignable(methodParameter, payloadClass)) {
				if (foundCandidate != null) {
					throw new IllegalArgumentException("Ambiguous payload parameter for " + method.toGenericString());
				}
				foundCandidate = methodParameter;
			}
		}
		return foundCandidate;
	}

	/**
	 * Checks if the given method parameter is assignable from the payload type.
	 *
	 * @param methodParameter the method parameter to check
	 * @param payloadClass the class of the payload
	 * @return true if the parameter type is assignable from the payload type, false otherwise
	 */
	private boolean isPayloadAssignable(MethodParameter methodParameter, Class<?> payloadClass) {
		return methodParameter.getParameterType().isAssignableFrom(payloadClass);
	}
}
