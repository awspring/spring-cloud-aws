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

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ConfirmSubscriptionRequest;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticspring.messaging.StringMessage;
import org.elasticspring.messaging.support.converter.MessageConverter;
import org.elasticspring.messaging.support.converter.NotificationMessageConverter;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class NotificationEndpointHttpRequestHandler implements HttpRequestHandler, ServletContextAware, InitializingBean, BeanNameAware {

	private static final String PAYLOAD_CHAR_SET = "UTF-8";
	public static final String MESSAGE_TYPE = "x-amz-sns-message-type";
	public static final String TOPIC_ARN_HEADER = "x-amz-sns-topic-arn";

	public static final String NOTIFICATION_MESSAGE_TYPE = "Notification";
	public static final String SUBSCRIPTION_MESSAGE_TYPE = "SubscriptionConfirmation";

	private final AmazonSNS amazonSNS;
	private final MessageConverter messageConverter;
	private final Object target;
	private final String listenerMethod;
	private final String endpointAddress;
	private final String topicArn;

	private ServletContext servletContext;
	private String beanName;

	public NotificationEndpointHttpRequestHandler(AmazonSNS amazonSns, MessageConverter messageConverter, Object target,
												  String listenerMethod, String endpointAddress, String topicArn) {
		this.amazonSNS = amazonSns;
		this.messageConverter = messageConverter;
		this.target = target;
		this.listenerMethod = listenerMethod;
		this.endpointAddress = endpointAddress;
		this.topicArn = topicArn;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getHeader(MESSAGE_TYPE) == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No mandatory request header with name:'" + MESSAGE_TYPE + "'");
			return;
		}

		if (request.getHeader(TOPIC_ARN_HEADER) == null || !request.getHeader(TOPIC_ARN_HEADER).equals(this.topicArn)) {
			// Do not send the configured topic arn as that contains sensitive information like the account no
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The topic arn in the message:'" +
					(request.getHeader(TOPIC_ARN_HEADER) != null ? request.getHeader(TOPIC_ARN_HEADER) : "") +
					"' does not match the expected configured topic arn");
			return;
		}

		if (NOTIFICATION_MESSAGE_TYPE.equals(request.getHeader(MESSAGE_TYPE))) {
			handleNotificationMessage(request, response);
		} else if (SUBSCRIPTION_MESSAGE_TYPE.equals(request.getHeader(MESSAGE_TYPE))) {
			handleSubscription(request, response);
		}
	}

	private void handleNotificationMessage(HttpServletRequest request, HttpServletResponse response) throws IOException {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(this.target);
		methodInvoker.setTargetMethod(this.listenerMethod);

		byte[] payload = FileCopyUtils.copyToByteArray(request.getInputStream());

		NotificationMessageConverter.NotificationMessage notificationMessage = (NotificationMessageConverter.NotificationMessage)
				this.messageConverter.fromMessage(new StringMessage(
						new String(payload, Charset.forName(PAYLOAD_CHAR_SET))));

		if (ClassUtils.hasMethod(AopUtils.getTargetClass(this.target), this.listenerMethod, String.class, String.class)) {
			methodInvoker.setArguments(new Object[]{notificationMessage.getBody(), notificationMessage.getSubject()});
		} else {
			methodInvoker.setArguments(new Object[]{notificationMessage.getBody()});
		}

		try {
			methodInvoker.prepare();
		} catch (ClassNotFoundException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		} catch (NoSuchMethodException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The configured endpoint method:'" + this.listenerMethod + "' does not exist");
			return;
		}

		try {
			methodInvoker.invoke();
		} catch (InvocationTargetException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getTargetException().getMessage());
		} catch (IllegalAccessException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	private void handleSubscription(HttpServletRequest request, HttpServletResponse response) throws IOException {
		byte[] payload = FileCopyUtils.copyToByteArray(request.getInputStream());
		try {
			JSONObject jsonObject = new JSONObject(new String(payload, PAYLOAD_CHAR_SET));
			this.amazonSNS.confirmSubscription(new ConfirmSubscriptionRequest(jsonObject.getString("TopicArn"), jsonObject.getString("Token")));
		} catch (JSONException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.servletContext, "ServletContext must no be null, please make sure this class is " +
				"used inside a web application context");
		Assert.notNull(this.beanName, "bean name must not be null");

		ServletRegistration.Dynamic dynamic = this.servletContext.addServlet(this.beanName, new HttpRequestHandlerServlet());

		Assert.notNull(dynamic, "Error registering servlet to handle notification request. Please make sure to run in a servlet 3.0 compliant servlet container");

		dynamic.addMapping(getRelativeUrlInContextPath(this.servletContext.getContextPath(), this.endpointAddress));
	}

	private static String getRelativeUrlInContextPath(String contextPath, String endpointAddress) throws URISyntaxException {
		Assert.notNull(contextPath, "contextPath must not be null");

		URI uri = new URI(endpointAddress);
		String query = uri.getPath();

		Assert.isTrue(query.contains(contextPath), "The endpoint:'" + endpointAddress + "' " +
				"does not contain the context path:'" + contextPath + "' where the application is running");

		String relativePath = query.substring(contextPath.length());
		return toContextPath(relativePath);
	}

	private static String toContextPath(String input) {
		return input.startsWith("/") ? input : "/" + input;
	}
}