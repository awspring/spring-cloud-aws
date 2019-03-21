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

package org.springframework.cloud.aws.messaging.support;

import org.springframework.cloud.aws.messaging.config.annotation.NotificationMessage;
import org.springframework.cloud.aws.messaging.support.converter.NotificationRequestConverter;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * @author Alain Sahli
 */
public class NotificationMessageArgumentResolver
		implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;

	public NotificationMessageArgumentResolver(MessageConverter converter) {
		this.converter = new NotificationRequestConverter(converter);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(NotificationMessage.class);
	}

	@Override
	public Object resolveArgument(MethodParameter par, Message<?> msg) throws Exception {
		Object object = this.converter.fromMessage(msg, par.getParameterType());
		NotificationRequestConverter.NotificationRequest nr = (NotificationRequestConverter.NotificationRequest) object;
		return nr.getMessage();
	}

}
