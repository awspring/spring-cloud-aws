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

import io.awspring.cloud.sqs.listener.BatchVisibility;
import io.awspring.cloud.sqs.listener.QueueMessageBatchVisibility;
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
 * @since 3.3
 */
public class BatchVisibilityHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return ClassUtils.isAssignable(BatchVisibility.class, parameter.getParameterType());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		Object payloadObject = message.getPayload();
		Assert.isInstanceOf(Collection.class, payloadObject, "Payload must be instance of Collection");
		return new QueueMessageBatchVisibility<>((Collection<Message<Object>>) payloadObject);
	}

}
