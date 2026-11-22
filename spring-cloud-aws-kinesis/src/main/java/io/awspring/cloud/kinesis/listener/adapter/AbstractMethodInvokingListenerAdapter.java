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
import io.awspring.cloud.kinesis.listener.ReplyPublicationFailedException;
import io.awspring.cloud.kinesis.operations.KinesisOperations;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Invokes a listener method and forwards its return value to the reply stream, if one is declared.
 * <p>
 * A reply to a single record is sent with the partition key of that record, so replies keep the per-key order of the
 * input. A reply to a batch has no single input key and is sent with a random partition key. A returned {@link Message}
 * keeps its headers and uses its partition-key header when present.
 * <p>
 * If publishing the reply fails, a {@link ReplyPublicationFailedException} carrying the listener result is thrown so a
 * retry resends the reply without calling the listener method again.
 *
 * @author Matej Nedic
 * @since 4.3.0
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
		publishReply(result, message.getHeaders().get(KinesisMessageHeaders.PARTITION_KEY, String.class));
	}

	/**
	 * Publishes the reply. On failure the thrown exception carries the result, so a retry only resends the reply and
	 * never calls the listener method again. No state is kept on the adapter.
	 */
	private void publishReply(@Nullable Object result, @Nullable String inputPartitionKey) {
		if (result == null || this.replyStream == null || this.kinesisOperations == null) {
			return;
		}
		try {
			sendReply(this.kinesisOperations, this.replyStream, result, inputPartitionKey);
		}
		catch (RuntimeException ex) {
			throw new ReplyPublicationFailedException("Failed to publish @KclListener reply", ex,
					() -> publishReply(result, inputPartitionKey));
		}
	}

	private static void sendReply(KinesisOperations operations, String stream, Object result,
			@Nullable String inputPartitionKey) {
		if (result instanceof Message<?> reply) {
			// A partition key header of the reply wins over the input key; headers are
			// kept as they are.
			Object header = reply.getHeaders().get(KinesisMessageHeaders.PARTITION_KEY);
			String key = header != null && StringUtils.hasText(header.toString()) ? header.toString()
					: inputPartitionKey;
			if (key != null) {
				operations.send(stream, key, reply);
			}
			else {
				operations.send(stream, reply);
			}
		}
		else if (inputPartitionKey != null) {
			operations.send(stream, inputPartitionKey, result);
		}
		else {
			operations.send(stream, result);
		}
	}

}
