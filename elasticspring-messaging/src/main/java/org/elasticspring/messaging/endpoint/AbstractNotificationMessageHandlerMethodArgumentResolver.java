/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.messaging.endpoint;

import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * @author Agim Emruli
 */
public abstract class AbstractNotificationMessageHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String NOTIFICATION_REQUEST_ATTRIBUTE_NAME = "NOTIFICATION_REQUEST";
	private final MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();


	@SuppressWarnings("unchecked")
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		if (attributes.getAttribute(NOTIFICATION_REQUEST_ATTRIBUTE_NAME, RequestAttributes.SCOPE_REQUEST) == null) {
			attributes.setAttribute(NOTIFICATION_REQUEST_ATTRIBUTE_NAME, this.messageConverter.read(HashMap.class, createInputMessage(webRequest)),RequestAttributes.SCOPE_REQUEST);
		}

		HashMap<String, String> content = (HashMap<String, String>) attributes.getAttribute(NOTIFICATION_REQUEST_ATTRIBUTE_NAME, RequestAttributes.SCOPE_REQUEST);
		return doResolverArgumentFromNotificationMessage(content);
	}

	protected abstract Object doResolverArgumentFromNotificationMessage(HashMap<String, String> content);

	private ServletServerHttpRequest createInputMessage(NativeWebRequest webRequest) {
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		return new ServletServerHttpRequest(servletRequest);
	}
}
