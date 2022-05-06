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
package io.awspring.cloud.sqs.support;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class CallbackFutureReturnValueHandler<T> implements HandlerMethodReturnValueHandler {

	private static final Logger logger = LoggerFactory.getLogger(CallbackFutureReturnValueHandler.class);

	private final BiFunction<Message<T>, Throwable, CompletableFuture<?>> callback;

	public CallbackFutureReturnValueHandler(BiFunction<Message<T>, Throwable, CompletableFuture<?>> callback) {
		this.callback = callback;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return CompletionStage.class.isAssignableFrom(returnType.getParameterType());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message) {
		((CompletionStage<Void>) returnValue).handle((val, t) -> callback.apply(((Message<T>) message), t))
				.exceptionally(t -> {
					logger.error("Error executing callback for message {}", message, t);
					return null;
				});
	}
}
