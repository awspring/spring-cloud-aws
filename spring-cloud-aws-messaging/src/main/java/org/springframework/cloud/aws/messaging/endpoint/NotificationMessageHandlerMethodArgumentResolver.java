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

package org.springframework.cloud.aws.messaging.endpoint;

import org.springframework.cloud.aws.messaging.config.annotation.NotificationMessage;
import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;

import java.util.HashMap;

/**
 * @author Agim Emruli
 */
public class NotificationMessageHandlerMethodArgumentResolver extends AbstractNotificationMessageHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(NotificationMessage.class) &&
				ClassUtils.isAssignable(String.class, parameter.getParameterType()));
	}

	@Override
	protected Object doResolverArgumentFromNotificationMessage(HashMap<String, String> content) {
		if (!"Notification".equals(content.get("Type"))) {
			throw new IllegalArgumentException("@NotificationMessage annotated parameters are only allowed for method that receive a notification message.");
		}
		return content.get("Message");
	}
}
