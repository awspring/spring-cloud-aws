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

package org.springframework.cloud.aws.messaging.endpoint;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Agim Emruli
 */
public abstract class AbstractNotificationMessageHandlerMethodArgumentResolver
		implements HandlerMethodArgumentResolver {

	private static final String NOTIFICATION_REQUEST_ATTRIBUTE_NAME = "NOTIFICATION_REQUEST";

	private final MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();

	@SuppressWarnings("unchecked")
	@Override
	public Object resolveArgument(MethodParameter parameter,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory) throws Exception {
		Assert.notNull(parameter, "Parameter must not be null");
		if (webRequest.getAttribute(NOTIFICATION_REQUEST_ATTRIBUTE_NAME,
				RequestAttributes.SCOPE_REQUEST) == null) {
			webRequest.setAttribute(NOTIFICATION_REQUEST_ATTRIBUTE_NAME,
					this.messageConverter.read(JsonNode.class,
							createInputMessage(webRequest)),
					RequestAttributes.SCOPE_REQUEST);
		}

		JsonNode content = (JsonNode) webRequest.getAttribute(
				NOTIFICATION_REQUEST_ATTRIBUTE_NAME, RequestAttributes.SCOPE_REQUEST);
		return doResolveArgumentFromNotificationMessage(content,
				createInputMessage(webRequest), parameter.getParameterType());
	}

	protected abstract Object doResolveArgumentFromNotificationMessage(JsonNode content,
			HttpInputMessage request, Class<?> parameterType);

	private HttpInputMessage createInputMessage(NativeWebRequest webRequest)
			throws IOException {
		HttpServletRequest servletRequest = webRequest
				.getNativeRequest(HttpServletRequest.class);
		return new ServletServerHttpRequest(servletRequest);
	}

}
