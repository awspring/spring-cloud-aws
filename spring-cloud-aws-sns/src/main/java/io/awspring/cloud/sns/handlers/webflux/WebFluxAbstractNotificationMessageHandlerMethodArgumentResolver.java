/*
 * Copyright 2013-2026 the original author or authors.
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

import java.util.Collections;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.util.Assert;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

/**
 * Base class for WebFlux argument resolvers that handle SNS HTTP notification messages. Reads and caches the request
 * body as a {@link JsonNode} in the {@link ServerWebExchange} attributes so that multiple resolvers can access the same
 * parsed notification without re-reading the body.
 *
 * <p>
 * This is the reactive counterpart of
 * {@link io.awspring.cloud.sns.handlers.AbstractNotificationMessageHandlerMethodArgumentResolver}.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public abstract class WebFluxAbstractNotificationMessageHandlerMethodArgumentResolver
		implements HandlerMethodArgumentResolver {

	private static final String NOTIFICATION_REQUEST_ATTRIBUTE_NAME = "NOTIFICATION_REQUEST";

	private final JacksonJsonDecoder decoder = new JacksonJsonDecoder();

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {
		Assert.notNull(parameter, "Parameter must not be null");

		Mono<JsonNode> contentMono = exchange.getAttribute(NOTIFICATION_REQUEST_ATTRIBUTE_NAME);
		if (contentMono == null) {
			contentMono = decoder.decodeToMono(exchange.getRequest().getBody(), ResolvableType.forClass(JsonNode.class),
					MediaType.APPLICATION_JSON, Collections.emptyMap()).cast(JsonNode.class).cache();
			exchange.getAttributes().put(NOTIFICATION_REQUEST_ATTRIBUTE_NAME, contentMono);
		}

		return contentMono.flatMap(
				content -> doResolveArgumentFromNotificationMessage(content, exchange, parameter.getParameterType()));
	}

	protected abstract Mono<Object> doResolveArgumentFromNotificationMessage(JsonNode content,
			ServerWebExchange exchange, Class<?> parameterType);

}
