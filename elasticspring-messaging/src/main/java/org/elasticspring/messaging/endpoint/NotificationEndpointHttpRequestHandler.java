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

import org.elasticspring.messaging.StringMessage;
import org.elasticspring.messaging.support.converter.MessageConverter;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MethodInvoker;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class NotificationEndpointHttpRequestHandler implements HttpRequestHandler, ServletContextAware, InitializingBean, BeanNameAware {

	private static final String PAYLOAD_CHAR_SET = "UTF-8";
	private final MessageConverter messageConverter;
	private final Object target;
	private final String listenerMethod;

	private ServletContext servletContext;
	private String beanName;

	public NotificationEndpointHttpRequestHandler(MessageConverter messageConverter, Object target, String listenerMethod) {
		this.messageConverter = messageConverter;
		this.target = target;
		this.listenerMethod = listenerMethod;
	}

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(this.target);
		methodInvoker.setTargetMethod(this.listenerMethod);

		byte[] payload = FileCopyUtils.copyToByteArray(request.getInputStream());

		Object argument = this.messageConverter.fromMessage(new StringMessage(
				new String(payload, Charset.forName(PAYLOAD_CHAR_SET))));
		methodInvoker.setArguments(new Object[]{argument});
		try {
			methodInvoker.prepare();
		} catch (ClassNotFoundException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} catch (NoSuchMethodException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}

		try {
			methodInvoker.invoke();
		} catch (InvocationTargetException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getTargetException().getMessage());
		} catch (IllegalAccessException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.servletContext, "ServletContext must no be null, please make sure this class is " +
				"used inside a web application context");
		ServletRegistration.Dynamic dynamic = this.servletContext.addServlet(this.beanName, new HttpRequestHandlerServlet());
		dynamic.addMapping("/" + this.beanName);
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}
}