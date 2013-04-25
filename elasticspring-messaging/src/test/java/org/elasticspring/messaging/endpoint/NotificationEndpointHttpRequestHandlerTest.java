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
import org.codehaus.jettison.json.JSONObject;
import org.elasticspring.messaging.config.annotation.TopicListener;
import org.elasticspring.messaging.support.converter.NotificationMessageConverter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class NotificationEndpointHttpRequestHandlerTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testSimpleNotificationRequest() throws Exception {
		SimpleHttpRequestHandler target = new SimpleHttpRequestHandler();

		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), target, "handleNotification", "http://localhost:8080/first", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");


		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");

		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("testBean"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("/");

		handler.afterPropertiesSet();

		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.addHeader(NotificationEndpointHttpRequestHandler.MESSAGE_TYPE, NotificationEndpointHttpRequestHandler.NOTIFICATION_MESSAGE_TYPE);
		mockHttpServletRequest.addHeader(NotificationEndpointHttpRequestHandler.TOPIC_ARN_HEADER, "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");

		MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();


		JSONObject jsonObject = new JSONObject();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Message", "Hello World");

		mockHttpServletRequest.setContent(jsonObject.toString().getBytes());

		handler.handleRequest(mockHttpServletRequest, mockHttpServletResponse);
		Assert.assertEquals("Hello World", target.getLastMessage());
	}

	@Test
	public void testRegisterInRootContextWithRootMapping() throws Exception {
		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), new SimpleHttpRequestHandler(), "handleNotification", "http://localhost:8080/first", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");


		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");

		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("testBean"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("/");

		handler.afterPropertiesSet();

		Mockito.verify(dynamic, Mockito.times(1)).addMapping("/first");
	}

	@Test
	public void testRegisterInRootContextWithNestedMapping() throws Exception {
		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), new SimpleHttpRequestHandler(), "handleNotification", "http://localhost:8080/first/second/third", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");


		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");

		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("testBean"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("/");

		handler.afterPropertiesSet();

		Mockito.verify(dynamic, Mockito.times(1)).addMapping("/first/second/third");
	}

	@Test
	public void testRegisterInRootContextWithEmptyContext() throws Exception {
		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), new SimpleHttpRequestHandler(), "handleNotification", "http://localhost:8080/first/second/third", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");


		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");

		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("testBean"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("");

		handler.afterPropertiesSet();

		Mockito.verify(dynamic, Mockito.times(1)).addMapping("/first/second/third");
	}

	@Test
	public void testRegisterInSubContextWithNestedMapping() throws Exception {
		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), new SimpleHttpRequestHandler(), "handleNotification", "http://localhost:8080/myApp/first/second", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");


		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");

		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("testBean"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("/myApp");

		handler.afterPropertiesSet();

		Mockito.verify(dynamic, Mockito.times(1)).addMapping("/first/second");
	}

	@Test
	public void testContextIsNotInEndpoint() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("does not contain the context path");

		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), new SimpleHttpRequestHandler(), "handleNotification", "http://localhost:8080/myApp/first/second", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");


		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");

		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("testBean"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("/differentApp");

		handler.afterPropertiesSet();
	}

	@Test
	public void testNoServletContextSet() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("ServletContext must no be null, please make sure this class is used inside a web application context");

		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), new SimpleHttpRequestHandler(), "handleNotification", "http://localhost:8080/myApp/first/second", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");

		handler.setBeanName("testBean");
		handler.afterPropertiesSet();
	}

	@Test
	public void testRegisterBeanNameNotSet() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("bean name must not be null");
		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), new SimpleHttpRequestHandler(), "handleNotification", "http://localhost:8080/myApp/first/second", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");

		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);

		handler.afterPropertiesSet();
	}

	@Test //e.g. Jetty 8
	public void testNoValidServlet30ServletContainer() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("Error registering servlet to handle notification request. Please make sure to run in a servlet 3.0 compliant servlet container");
		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), new SimpleHttpRequestHandler(), "handleNotification", "http://localhost:8080/myApp/first/second", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");

		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");
		handler.afterPropertiesSet();
	}

	@Test
	public void testInvalidClass() throws Exception {
		SimpleHttpRequestHandler target = new SimpleHttpRequestHandler();

		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), target, "notExistingMethod", "http://localhost:8080/first", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");


		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");

		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("testBean"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("/");

		handler.afterPropertiesSet();

		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.addHeader(NotificationEndpointHttpRequestHandler.MESSAGE_TYPE, NotificationEndpointHttpRequestHandler.NOTIFICATION_MESSAGE_TYPE);
		mockHttpServletRequest.addHeader(NotificationEndpointHttpRequestHandler.TOPIC_ARN_HEADER, "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");

		MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();


		JSONObject jsonObject = new JSONObject();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Message", "Hello World");

		mockHttpServletRequest.setContent(jsonObject.toString().getBytes());

		handler.handleRequest(mockHttpServletRequest, mockHttpServletResponse);
		Assert.assertEquals(500, mockHttpServletResponse.getStatus());
		Assert.assertEquals("The configured endpoint method:'notExistingMethod' does not exist", mockHttpServletResponse.getErrorMessage());
	}

	@Test
	public void testExceptionThrowingMethod() throws Exception {
		SimpleHttpRequestHandler target = new SimpleHttpRequestHandler();

		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), target, "exceptionThrowingMethod", "http://localhost:8080/first", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");


		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");

		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("testBean"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("/");

		handler.afterPropertiesSet();

		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.addHeader(NotificationEndpointHttpRequestHandler.MESSAGE_TYPE, NotificationEndpointHttpRequestHandler.NOTIFICATION_MESSAGE_TYPE);
		mockHttpServletRequest.addHeader(NotificationEndpointHttpRequestHandler.TOPIC_ARN_HEADER, "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");

		MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();


		JSONObject jsonObject = new JSONObject();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Message", "Hello World");

		mockHttpServletRequest.setContent(jsonObject.toString().getBytes());

		handler.handleRequest(mockHttpServletRequest, mockHttpServletResponse);
		Assert.assertEquals(500, mockHttpServletResponse.getStatus());
		Assert.assertEquals("Application Error", mockHttpServletResponse.getErrorMessage());
	}

	@Test
	public void testNoMessageTypeSend() throws Exception {
		SimpleHttpRequestHandler target = new SimpleHttpRequestHandler();

		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), target, "handleNotification", "http://localhost:8080/first", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");


		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");

		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("testBean"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("/");

		handler.afterPropertiesSet();

		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.addHeader(NotificationEndpointHttpRequestHandler.TOPIC_ARN_HEADER, "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");

		MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

		handler.handleRequest(mockHttpServletRequest, mockHttpServletResponse);
		Assert.assertEquals(400, mockHttpServletResponse.getStatus());
		Assert.assertTrue(mockHttpServletResponse.getErrorMessage().startsWith("No mandatory request header with name"));
	}

	@Test
	public void testNoTopicArn() throws Exception {
		SimpleHttpRequestHandler target = new SimpleHttpRequestHandler();

		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), target, "handleNotification", "http://localhost:8080/first", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");


		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");

		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("testBean"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("/");

		handler.afterPropertiesSet();

		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.addHeader(NotificationEndpointHttpRequestHandler.MESSAGE_TYPE, NotificationEndpointHttpRequestHandler.NOTIFICATION_MESSAGE_TYPE);

		MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

		handler.handleRequest(mockHttpServletRequest, mockHttpServletResponse);
		Assert.assertEquals(400, mockHttpServletResponse.getStatus());
		Assert.assertTrue(mockHttpServletResponse.getErrorMessage().startsWith("The topic arn in the message:'' does not match the expected configured topic arn"));
	}

	@Test
	public void testWrongTopicArn() throws Exception {
		SimpleHttpRequestHandler target = new SimpleHttpRequestHandler();

		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				Mockito.mock(AmazonSNS.class), new NotificationMessageConverter(), target, "handleNotification", "http://localhost:8080/first", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");


		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");

		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("testBean"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("/");

		handler.afterPropertiesSet();

		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.addHeader(NotificationEndpointHttpRequestHandler.MESSAGE_TYPE, NotificationEndpointHttpRequestHandler.NOTIFICATION_MESSAGE_TYPE);
		mockHttpServletRequest.addHeader(NotificationEndpointHttpRequestHandler.TOPIC_ARN_HEADER, "arn:aws:sns:us-east-1:123456789012:notExistingTopic");

		MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

		handler.handleRequest(mockHttpServletRequest, mockHttpServletResponse);
		Assert.assertEquals(400, mockHttpServletResponse.getStatus());
		Assert.assertTrue(mockHttpServletResponse.getErrorMessage().startsWith("The topic arn in the message:'arn:aws:sns:us-east-1:123456789012:notExistingTopic' " +
				"does not match the expected configured topic arn"));
	}

	@Test
	public void testSubscriptionRequest() throws Exception {
		SimpleHttpRequestHandler target = new SimpleHttpRequestHandler();

		AmazonSNS amazonSns = Mockito.mock(AmazonSNS.class);
		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				amazonSns, new NotificationMessageConverter(), target, "handleNotification", "http://localhost:8080/first", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");


		ServletContext servletContext = Mockito.mock(ServletContext.class);
		handler.setServletContext(servletContext);
		handler.setBeanName("testBean");

		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("testBean"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("/");

		handler.afterPropertiesSet();

		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.addHeader(NotificationEndpointHttpRequestHandler.MESSAGE_TYPE, NotificationEndpointHttpRequestHandler.SUBSCRIPTION_MESSAGE_TYPE);
		mockHttpServletRequest.addHeader(NotificationEndpointHttpRequestHandler.TOPIC_ARN_HEADER, "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");

		MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();


		JSONObject jsonObject = new JSONObject();
		jsonObject.put("Token", "1234");
		jsonObject.put("TopicArn", "arn:aws:sns:us-east-1:123456789012:my_corporate_topic");

		mockHttpServletRequest.setContent(jsonObject.toString().getBytes());

		handler.handleRequest(mockHttpServletRequest, mockHttpServletResponse);
		Mockito.verify(amazonSns, Mockito.times(1)).confirmSubscription(new ConfirmSubscriptionRequest("arn:aws:sns:us-east-1:123456789012:my_corporate_topic", "1234"));
	}


	static class SimpleHttpRequestHandler {

		private String lastMessage;

		@TopicListener(topicName = "test", protocol = TopicListener.NotificationProtocol.HTTP, endpoint = "foo")
		public void handleNotification(String message) {
			this.lastMessage = message;
		}

		@TopicListener(topicName = "test", protocol = TopicListener.NotificationProtocol.HTTP, endpoint = "bar")
		public void exceptionThrowingMethod(String message) {
			throw new RuntimeException("Application Error");
		}

		public String getLastMessage() {
			return this.lastMessage;
		}
	}
}
