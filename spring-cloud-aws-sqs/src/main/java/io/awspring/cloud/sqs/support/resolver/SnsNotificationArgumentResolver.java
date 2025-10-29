/*
 * Copyright 2013-2025 the original author or authors.
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
import io.awspring.cloud.sqs.support.converter.SnsNotification;
import io.awspring.cloud.sqs.support.converter.SnsNotificationConverter;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * Resolves method parameters with {@link SnsNotification} object.
 *
 * @author Damien Chomat
 * @since 3.4.1
 */
public class SnsNotificationArgumentResolver implements HandlerMethodArgumentResolver {

	private final SmartMessageConverter converter;

	/**
	 * Creates a new resolver with the given converter and JSON mapper.
	 * @param converter the message converter to use for the message payload
	 * @param jsonMapper the JSON mapper to use for parsing the SNS notification
	 */
	public SnsNotificationArgumentResolver(MessageConverter converter, ObjectMapper jsonMapper) {
		this.converter = new SnsNotificationConverter(converter, jsonMapper);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return SnsNotification.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		return this.converter.fromMessage(message, parameter.getParameterType(), parameter);
	}
}
