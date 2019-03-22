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

package org.springframework.cloud.aws.messaging.listener.support;

import org.springframework.cloud.aws.messaging.listener.Visibility;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.ClassUtils;

/**
 * @author Szymon Dembek
 * @since 1.3
 */
public class VisibilityHandlerMethodArgumentResolver
		implements HandlerMethodArgumentResolver {

	private final String visibilityHeaderName;

	public VisibilityHandlerMethodArgumentResolver(String visibilityHeaderName) {
		this.visibilityHeaderName = visibilityHeaderName;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return ClassUtils.isAssignable(Visibility.class, parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message)
			throws Exception {
		if (!message.getHeaders().containsKey(this.visibilityHeaderName)
				|| message.getHeaders().get(this.visibilityHeaderName) == null) {
			throw new IllegalArgumentException(
					"No visibility object found for message header: '"
							+ this.visibilityHeaderName + "'");
		}
		return message.getHeaders().get(this.visibilityHeaderName);
	}

}
