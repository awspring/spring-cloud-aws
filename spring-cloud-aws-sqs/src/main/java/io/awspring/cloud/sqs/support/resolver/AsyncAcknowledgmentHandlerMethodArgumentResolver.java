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

import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.ClassUtils;

/**
 * {@link HandlerMethodArgumentResolver} for {@link Acknowledgement} method parameters.
 *
 * @author Alain Sahli
 * @author Tomaz Fernandes
 * @since 1.1
 */
public class AsyncAcknowledgmentHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final String acknowledgmentHeaderName;

	public AsyncAcknowledgmentHandlerMethodArgumentResolver(String acknowledgmentHeaderName) {
		this.acknowledgmentHeaderName = acknowledgmentHeaderName;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return ClassUtils.isAssignable(Acknowledgement.class, parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		if (!message.getHeaders().containsKey(this.acknowledgmentHeaderName)
				|| message.getHeaders().get(this.acknowledgmentHeaderName) == null) {
			throw new IllegalArgumentException(
					"No acknowledgment object found for message header: '" + this.acknowledgmentHeaderName + "'");
		}
		return message.getHeaders().get(this.acknowledgmentHeaderName);
	}

}
