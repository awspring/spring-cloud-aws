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
import org.elasticspring.messaging.support.NotificationMessage;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MethodInvoker;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

/**
 * {@link HttpRequestHandler} implementation that retrieves notification over the http protocol. This endpoint will
 * auto-register itself on the application context respecting the endpoint address configured in the subscription. This
 * endpoint supports message for confirmation subscription as well as notification messages. Unsubscribe message are
 * ignored by this endpoint as this endpoint will never modify any subscription on itself.
 * <p>
 * <b>Note:</b>This endpoint requires a servlet 3.0 compliant servlet container to aut-register a new {@link
 * HttpRequestHandlerServlet} for this endpoint inside the application context
 * </p>
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class NotificationEndpointHttpRequestHandler implements HttpRequestHandler, ServletContextAware, InitializingBean, BeanNameAware {

	/**
	 * The character set for the notification messages
	 */
	private static final String PAYLOAD_CHAR_SET = "UTF-8";

	/**
	 * The http request parameter name that contains the message type (e.g. Notification)
	 */
	public static final String MESSAGE_TYPE = "x-amz-sns-message-type";

	/**
	 * The topic arn to which the message is send.
	 */
	public static final String TOPIC_ARN_HEADER = "x-amz-sns-topic-arn";

	/**
	 * The notification message type which contains a message payload and an optional subject
	 */
	public static final String NOTIFICATION_MESSAGE_TYPE = "Notification";

	/**
	 * The subscription confirmation which is sent out to the endpoint to confirm the subscription
	 */
	public static final String SUBSCRIPTION_MESSAGE_TYPE = "SubscriptionConfirmation";

	/**
	 * The amazon sns client used to confirm subscription
	 */
	private final AmazonSNS amazonSNS;

	/**
	 * The message converter used to deserialize a notification message
	 */
	private final MessageConverter messageConverter;

	/**
	 * The target object which will be called on a notification message
	 */
	private final Object target;

	/**
	 * The listener method name that will be called
	 */
	private final String listenerMethod;

	/**
	 * The configured endpoint address for this endpoint
	 */
	private final String endpointAddress;

	/**
	 * The configured and resolved topic arn for this endpoint, will be used to check if the messages received are for
	 * this
	 * endpoint.
	 */
	private final String topicArn;

	/**
	 * The servlet context used to register the servlet
	 */
	private ServletContext servletContext;

	/**
	 * The bean name which will become the servlet name
	 */
	private String beanName;

	/**
	 * Constructs an endpoint with the specified parameters.
	 *
	 * @param amazonSns
	 * 		the amazon sns client
	 * @param messageConverter
	 * 		the message converter user
	 * @param target
	 * 		the target object that will be called
	 * @param listenerMethod
	 * 		the listener method that will be called
	 * @param endpointAddress
	 * 		the endpoint address that will be called
	 * @param topicArn
	 * 		the topic arn that will be called
	 */
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

	/**
	 * Handles all incoming request and check if the message type is configured and the topic arn of the message matches
	 * the configured topic arn. Dispatches the message to the particular message based method inside this class (e.g.
	 * {@link #handleNotificationMessage(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
	 *
	 * @param request
	 * 		- the http servlet request containing the message
	 * @param response
	 * 		- the response used to set the particular status code
	 * @throws IOException
	 * 		if there is an error reading the content of the message (e.g. stream closed)
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

	/**
	 * Handles notification messages by calling the target
	 *
	 * @param request
	 * 		the request used to read the notification message
	 * @param response
	 * 		the response that is used to set the status flag
	 * @throws IOException
	 * 		in case of any error during message read operation
	 */
	private void handleNotificationMessage(HttpServletRequest request, HttpServletResponse response) throws IOException {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(this.target);
		methodInvoker.setTargetMethod(this.listenerMethod);

		byte[] payload = FileCopyUtils.copyToByteArray(request.getInputStream());

		String payload1 = new String(payload, Charset.forName(PAYLOAD_CHAR_SET));
		NotificationMessage notificationMessage = (NotificationMessage)
				this.messageConverter.fromMessage(MessageBuilder.withPayload(payload1).build(),null);

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

	/**
	 * Handles the subscription message by reading the message and subscribing to the topic.
	 *
	 * @param request
	 * 		the request containing the message
	 * @param response
	 * 		the response used to set the status code in case of any error
	 * @throws IOException
	 * 		in case of any error during message read operation
	 */
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