/*
 * Copyright 2013-2019 the original author or authors.
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
package io.awspring.cloud.sqs.support.resolver;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.BatchAcknowledgement;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link HandlerMethodArgumentResolver} implementation for resolving {@link BatchAcknowledgement} arguments.
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class BatchAcknowledgmentArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return ClassUtils.isAssignable(BatchAcknowledgement.class, parameter.getParameterType());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		Object payloadObject = message.getPayload();
		Assert.isInstanceOf(Collection.class, payloadObject, "Payload must be instance of Collection");
		Collection<Message<Object>> messages = (Collection<Message<Object>>) payloadObject;
		AcknowledgementCallback<Object> callback = messages.iterator().next().getHeaders()
				.get(SqsHeaders.SQS_ACKNOWLEDGMENT_CALLBACK_HEADER, AcknowledgementCallback.class);
		Assert.notNull(callback, "No acknowledgement found for messages " + MessageHeaderUtils.getId(messages)
				+ ". AcknowledgeMode should be MANUAL.");
		return createBatchAcknowledgement(messages, callback);
	}

	private <T> BatchAcknowledgement<T> createBatchAcknowledgement(Collection<Message<T>> messages,
			AcknowledgementCallback<T> callback) {
		return new BatchAcknowledgement<T>() {

			@Override
			public void acknowledge() {
				acknowledgeAsync().join();
			}

			@Override
			public CompletableFuture<Void> acknowledgeAsync() {
				return callback.onAcknowledge(messages);
			}

			@Override
			public void acknowledge(Collection<Message<T>> messagesToAcknowledge) {
				acknowledgeAsync(messagesToAcknowledge).join();
			}

			@Override
			public CompletableFuture<Void> acknowledgeAsync(Collection<Message<T>> messagesToAcknowledge) {
				return callback.onAcknowledge(messagesToAcknowledge);
			}
		};
	}

}
