/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sns.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import io.awspring.cloud.sns.annotation.handlers.NotificationSubject;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.util.ClassUtils;

/**
 *
 * Handles conversion of SNS subject value to a variable that is annotated with {@link NotificationSubject}.
 *
 * @author Agim Emruli
 */
public class NotificationSubjectHandlerMethodArgumentResolver
		extends AbstractNotificationMessageHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(NotificationSubject.class)
				&& ClassUtils.isAssignable(String.class, parameter.getParameterType()));
	}

	@Override
	protected Object doResolveArgumentFromNotificationMessage(JsonNode content, HttpInputMessage request,
			Class<?> parameterType) {
		if (!"Notification".equals(content.get("Type").asText())) {
			throw new IllegalArgumentException(
					"@NotificationMessage annotated parameters are only allowed for method that receive a notification message.");
		}
		return content.findPath("Subject").asText();
	}

}
