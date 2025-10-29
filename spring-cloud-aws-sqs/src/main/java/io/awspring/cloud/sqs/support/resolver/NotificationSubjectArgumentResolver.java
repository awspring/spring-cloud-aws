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
import io.awspring.cloud.sqs.annotation.SnsNotificationSubject;
import io.awspring.cloud.sqs.support.converter.SnsSubjectConverter;
import java.lang.reflect.Executable;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.ClassUtils;

/**
 * @author Alexander Nebel
 * @since 3.3.1
 */
public class NotificationSubjectArgumentResolver implements HandlerMethodArgumentResolver {

	private static final Logger logger = LoggerFactory.getLogger(NotificationSubjectArgumentResolver.class);

	private final MessageConverter converter;

	public NotificationSubjectArgumentResolver(ObjectMapper jsonMapper) {
		this.converter = new SnsSubjectConverter(jsonMapper);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (parameter.hasParameterAnnotation(SnsNotificationSubject.class)) {
			if (ClassUtils.isAssignable(parameter.getParameterType(), String.class)) {
				return true;
			}
			if (logger.isWarnEnabled()) {
				logger.warn(
						"Notification subject can only be injected into String assignable Types - No injection happening for {}#{}",
						parameter.getDeclaringClass().getName(), getMethodName(parameter));
			}
		}
		return false;
	}

	@Override
	public Object resolveArgument(MethodParameter par, Message<?> msg) {
		return converter.fromMessage(msg, par.getParameterType());
	}

	private String getMethodName(MethodParameter parameter) {
		var method = parameter.getMethod();
		var constructor = parameter.getConstructor();
		return Optional.ofNullable(method != null ? method : constructor).map(Executable::getName)
				.orElse("<Method name not resolvable>");
	}
}
