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
package io.awspring.cloud.sns.handlers.webflux;

import io.awspring.cloud.sns.annotation.handlers.NotificationSubject;
import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

/**
 * WebFlux argument resolver that handles conversion of SNS notification subject value to a method parameter annotated
 * with {@link NotificationSubject}.
 *
 * <p>
 * This is the reactive counterpart of
 * {@link io.awspring.cloud.sns.handlers.NotificationSubjectHandlerMethodArgumentResolver}.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public class WebFluxNotificationSubjectHandlerMethodArgumentResolver
		extends WebFluxAbstractNotificationMessageHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(NotificationSubject.class)
				&& ClassUtils.isAssignable(String.class, parameter.getParameterType()));
	}

	@Override
	protected Mono<Object> doResolveArgumentFromNotificationMessage(JsonNode content, ServerWebExchange exchange,
			Class<?> parameterType) {
		if (!"Notification".equals(content.get("Type").asString())) {
			return Mono.error(new IllegalArgumentException(
					"@NotificationMessage annotated parameters are only allowed for method that receive a notification message."));
		}
		return Mono.just(content.findPath("Subject").asString());
	}
}
