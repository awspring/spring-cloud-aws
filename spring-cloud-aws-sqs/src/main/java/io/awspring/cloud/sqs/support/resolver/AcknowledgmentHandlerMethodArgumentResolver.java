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
import io.awspring.cloud.sqs.MessagingHeaders;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import java.util.concurrent.CompletableFuture;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link HandlerMethodArgumentResolver} implementation for resolving {@link Acknowledgement} arguments.
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class AcknowledgmentHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return ClassUtils.isAssignable(Acknowledgement.class, parameter.getParameterType());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		AcknowledgementCallback<Object> callback = message.getHeaders()
				.get(MessagingHeaders.ACKNOWLEDGMENT_CALLBACK_HEADER, AcknowledgementCallback.class);
		Assert.notNull(callback, "No acknowledgement found for message " + MessageHeaderUtils.getId(message)
				+ ". AcknowledgeMode should be MANUAL.");
		return new Acknowledgement() {
			@Override
			public void acknowledge() {
				acknowledgeAsync().join();
			}

			@Override
			public CompletableFuture<Void> acknowledgeAsync() {
				return callback.onAcknowledge((Message<Object>) message);
			}
		};
	}

}
