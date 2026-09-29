/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.kinesis.listener.adapter;

import io.awspring.cloud.kinesis.listener.ListenerExecutionFailedException;
import io.awspring.cloud.kinesis.operations.KinesisOperations;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
abstract class AbstractMethodInvokingListenerAdapter {

	private final InvocableHandlerMethod handlerMethod;

	@Nullable
	private final KinesisOperations kinesisOperations;

	@Nullable
	private final String replyStream;

	protected AbstractMethodInvokingListenerAdapter(InvocableHandlerMethod handlerMethod,
			@Nullable KinesisOperations kinesisOperations, @Nullable String replyStream) {
		Assert.notNull(handlerMethod, "handlerMethod must not be null");
		this.handlerMethod = handlerMethod;
		this.kinesisOperations = kinesisOperations;
		this.replyStream = replyStream;
	}

	protected void invoke(Message<?> message) {
		Object result;
		try {
			result = this.handlerMethod.invoke(message);
		}
		catch (Exception ex) {
			throw new ListenerExecutionFailedException("Failed to invoke @KclListener method", ex);
		}
		sendReply(result);
	}

	private void sendReply(@Nullable Object result) {
		if (result != null && this.replyStream != null && this.kinesisOperations != null) {
			this.kinesisOperations.send(this.replyStream, result);
		}
	}

}
