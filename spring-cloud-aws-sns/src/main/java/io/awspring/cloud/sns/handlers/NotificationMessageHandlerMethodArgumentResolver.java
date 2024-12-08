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
import io.awspring.cloud.sns.annotation.handlers.NotificationMessage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StringUtils;

/**
 * Handles conversion of SNS notification value to a variable that is annotated with {@link NotificationMessage}.
 * Validation is not implemented in SDKv2. <a href="https://github.com/aws/aws-sdk-java-v2/issues/1302">...</a>
 *
 * @author Agim Emruli
 * @author Manuel Wessner
 * @author Matej Nedic
 */
public class NotificationMessageHandlerMethodArgumentResolver
		extends AbstractNotificationMessageHandlerMethodArgumentResolver {

	private final List<HttpMessageConverter<?>> messageConverter;

	public NotificationMessageHandlerMethodArgumentResolver() {
		this(Arrays.asList(new MappingJackson2HttpMessageConverter(), new StringHttpMessageConverter()));
	}

	public NotificationMessageHandlerMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverter) {
		this.messageConverter = messageConverter;
	}

	private static MediaType getMediaType(JsonNode content) {
		JsonNode contentTypeNode = content.findPath("MessageAttributes").findPath("contentType");
		if (contentTypeNode.isObject()) {
			String contentType = contentTypeNode.findPath("Value").asText();
			if (StringUtils.hasText(contentType)) {
				return MediaType.parseMediaType(contentType);
			}
		}

		return MediaType.TEXT_PLAIN;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(NotificationMessage.class));
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Object doResolveArgumentFromNotificationMessage(JsonNode content, HttpInputMessage request,
			Class<?> parameterType) {
		if (!"Notification".equals(content.get("Type").asText())) {
			throw new IllegalArgumentException(
					"@NotificationMessage annotated parameters are only allowed for method that receive a notification message.");
		}

		MediaType mediaType = getMediaType(content);
		String messageContent = content.findPath("Message").asText();
		for (HttpMessageConverter<?> converter : this.messageConverter) {
			if (converter.canRead(parameterType, mediaType)) {
				try {
					return converter.read((Class) parameterType,
							new ByteArrayHttpInputMessage(messageContent, mediaType, request));
				}
				catch (Exception e) {
					throw new HttpMessageNotReadableException(
							"Error converting notification message with payload:" + messageContent, e, request);
				}
			}
		}

		throw new HttpMessageNotReadableException(
				"Error converting notification message with payload:" + messageContent, request);
	}

	public static final class ByteArrayHttpInputMessage implements HttpInputMessage {

		private final String content;

		private final MediaType mediaType;

		private final HttpInputMessage request;

		private ByteArrayHttpInputMessage(String content, MediaType mediaType, HttpInputMessage request) {
			this.content = content;
			this.mediaType = mediaType;
			this.request = request;
		}

		@Override
		public InputStream getBody() {
			return new ByteArrayInputStream(this.content.getBytes(getCharset()));
		}

		private Charset getCharset() {
			return this.mediaType.getCharset() != null ? this.mediaType.getCharset()
					: Charset.forName(StandardCharsets.UTF_8.name());
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.request.getHeaders();
		}

	}

}
