/*
 * Copyright 2013-2023 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SnsNotificationMessage;
import io.awspring.cloud.sqs.support.converter.SnsMessageConverter;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * @author Michael Sosa
 * @author gustavomonarin
 * @author Wei Jiang
 * @since 3.1.1
 */
public class NotificationMessageArgumentResolver implements HandlerMethodArgumentResolver {

	private final SmartMessageConverter converter;

	public NotificationMessageArgumentResolver(MessageConverter converter, ObjectMapper jsonMapper) {
		this.converter = new SnsMessageConverter(converter, jsonMapper);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(SnsNotificationMessage.class);
	}

	@Override
	public Object resolveArgument(MethodParameter par, Message<?> msg) {
		return this.converter.fromMessage(msg, par.getParameterType(), par);
	}

}
