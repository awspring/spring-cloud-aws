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

import io.awspring.cloud.messaging.support.listener.CallbackMessageListener;
import io.awspring.cloud.sqs.support.CallbackFutureReturnValueHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class AsyncMessageHandlerMessageListener<T> implements CallbackMessageListener<T> {

	private static final Logger logger = LoggerFactory.getLogger(AsyncMessageHandlerMessageListener.class);

	private final MessageHandler messageHandler;

	public AsyncMessageHandlerMessageListener(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addResultCallback(BiFunction<Message<T>, Throwable, CompletableFuture<?>> callback) {
		AbstractMethodMessageHandler<T> messageHandler = (AbstractMethodMessageHandler<T>) this.messageHandler;
		List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<>(
				messageHandler.getReturnValueHandlers());
		returnValueHandlers.add(new CallbackFutureReturnValueHandler<>(callback));
		messageHandler.setReturnValueHandlers(returnValueHandlers);
	}

	@Override
	public CompletableFuture<Void> onMessage(Message<T> message) {
		logger.trace("Handling message {} in thread {}", message.getPayload(), Thread.currentThread().getName());
		return CompletableFuture.supplyAsync(() -> {
			this.messageHandler.handleMessage(message);
			return null;
		});
	}

}
