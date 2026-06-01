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

import io.awspring.cloud.sns.annotation.handlers.NotificationMessage;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import tools.jackson.databind.JsonNode;

/**
 * WebFlux argument resolver that handles conversion of SNS notification message payload to a method parameter annotated
 * with {@link NotificationMessage}. Uses reactive {@link Decoder} instances instead of
 * {@link org.springframework.http.converter.HttpMessageConverter}.
 *
 * <p>
 * This is the reactive counterpart of
 * {@link io.awspring.cloud.sns.handlers.NotificationMessageHandlerMethodArgumentResolver}.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public class WebFluxNotificationMessageHandlerMethodArgumentResolver
		extends WebFluxAbstractNotificationMessageHandlerMethodArgumentResolver {

	private final List<Decoder<?>> messageDecoders;

	@Nullable
	private final SnsMessageManager snsMessageManager;

	public static final List<Decoder<?>> defaultDecoders = Arrays.asList(new JacksonJsonDecoder(),
			StringDecoder.allMimeTypes());

	public WebFluxNotificationMessageHandlerMethodArgumentResolver() {
		this(defaultDecoders);
	}

	public WebFluxNotificationMessageHandlerMethodArgumentResolver(List<Decoder<?>> decoders) {
		this(decoders, null);
	}

	public WebFluxNotificationMessageHandlerMethodArgumentResolver(List<Decoder<?>> decoders,
			@Nullable SnsMessageManager snsMessageManager) {
		this.snsMessageManager = snsMessageManager;
		this.messageDecoders = decoders;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(NotificationMessage.class);
	}

	@Override
	protected Mono<Object> doResolveArgumentFromNotificationMessage(JsonNode content, ServerWebExchange exchange,
			Class<?> parameterType) {
		if (!"Notification".equals(content.get("Type").asString())) {
			return Mono.error(new IllegalArgumentException(
					"@NotificationMessage annotated parameters are only allowed for method that receive a notification message."));
		}

		MediaType mediaType = getMediaType(content);
		String messageContent = content.findPath("Message").asString();
		if (snsMessageManager != null) {
			verifySignature(content.toString());
		}

		for (Decoder<?> decoder : this.messageDecoders) {
			if (decoder.canDecode(ResolvableType.forClass(parameterType), mediaType)) {
				return decoder
						.decodeToMono(
								Mono.just(exchange.getResponse().bufferFactory()
										.wrap(messageContent.getBytes(getCharset(mediaType)))),
								ResolvableType.forClass(parameterType), mediaType, Collections.emptyMap())
						.cast(Object.class).onErrorMap(e -> new DecodingException(
								"Error converting notification message with payload:" + messageContent, e));
			}
		}

		return Mono
				.error(new DecodingException("Error converting notification message with payload:" + messageContent));
	}

	private static MediaType getMediaType(JsonNode content) {
		JsonNode contentTypeNode = content.findPath("MessageAttributes").findPath("contentType");
		if (contentTypeNode.isObject()) {
			String contentType = contentTypeNode.findPath("Value").asString();
			if (StringUtils.hasText(contentType)) {
				return MediaType.parseMediaType(contentType);
			}
		}
		return MediaType.TEXT_PLAIN;
	}

	private static Charset getCharset(MediaType mediaType) {
		return mediaType.getCharset() != null ? mediaType.getCharset() : StandardCharsets.UTF_8;
	}

	private void verifySignature(String payload) {
		snsMessageManager.parseMessage(payload);
	}

}
