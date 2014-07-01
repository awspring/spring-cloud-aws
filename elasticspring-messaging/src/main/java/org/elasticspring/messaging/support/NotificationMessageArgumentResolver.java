/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.support;

import org.elasticspring.messaging.config.annotation.NotificationMessage;
import org.elasticspring.messaging.support.converter.NotificationRequestConverter;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.ClassUtils;

/**
 * @author Alain Sahli
 */
public class NotificationMessageArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;

	public NotificationMessageArgumentResolver() {
		this.converter = new NotificationRequestConverter();
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(NotificationMessage.class) &&
				ClassUtils.isAssignable(String.class, parameter.getParameterType()));
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		return ((NotificationRequestConverter.NotificationRequest) this.converter.fromMessage(message, String.class)).getMessage();
	}
}
