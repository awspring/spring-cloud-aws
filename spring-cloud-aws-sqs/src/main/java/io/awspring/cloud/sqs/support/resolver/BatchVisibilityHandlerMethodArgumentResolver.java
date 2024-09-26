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
import io.awspring.cloud.sqs.listener.BatchVisibility;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import java.util.Collection;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link HandlerMethodArgumentResolver} for {@link BatchVisibility} method parameters.
 *
 * @author Clement Denis
 * @author Tomaz Fernandes
 * @since 3.3
 */
public class BatchVisibilityHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final String visibilityHeaderName;

	public BatchVisibilityHandlerMethodArgumentResolver(String visibilityHeaderName) {
		this.visibilityHeaderName = visibilityHeaderName;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return ClassUtils.isAssignable(BatchVisibility.class, parameter.getParameterType());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {

		Object payloadObject = message.getPayload();
		Assert.isInstanceOf(Collection.class, payloadObject, "Payload must be instance of Collection");
		Collection<Message<?>> messages = (Collection<Message<?>>) payloadObject;

		QueueMessageVisibility visibilityObject = MessageHeaderUtils.getHeader(messages.iterator().next(),
				visibilityHeaderName, QueueMessageVisibility.class);

		return visibilityObject.toBatchVisibility(messages);
	}

}
