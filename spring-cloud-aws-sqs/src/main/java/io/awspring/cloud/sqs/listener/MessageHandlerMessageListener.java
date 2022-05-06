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

import io.awspring.cloud.messaging.support.listener.AsyncMessageListener;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessageHandlerMessageListener<T> implements AsyncMessageListener<T> {

	private static final Logger logger = LoggerFactory.getLogger(MessageHandlerMessageListener.class);

	private final MessageHandler messageHandler;

	private TaskExecutor taskExecutor;

	public MessageHandlerMessageListener(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

	@Override
	public CompletableFuture<Void> onMessage(Message<T> message) {
		logger.trace("Handling message {} in thread {}", message.getPayload(), Thread.currentThread().getName());
		return this.taskExecutor != null ? CompletableFuture.supplyAsync(handleMessage(message), this.taskExecutor)
				: CompletableFuture.supplyAsync(handleMessage(message));
	}

	private Supplier<Void> handleMessage(Message<T> message) {
		return () -> {
			this.messageHandler.handleMessage(message);
			return null;
		};
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
}
