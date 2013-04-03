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

import org.apache.catalina.Context;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.startup.Tomcat;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.elasticspring.messaging.config.annotation.TopicListener;
import org.elasticspring.messaging.support.converter.NotificationMessageConverter;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.ContextLoaderListener;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class NotificationEndpointHttpRequestHandlerTest {

	@Test
	public void testSimpleRequest() throws Exception {

		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
		SimpleHttpRequestHandler target = new SimpleHttpRequestHandler();
		NotificationEndpointHttpRequestHandler handler = new NotificationEndpointHttpRequestHandler(
				new NotificationMessageConverter(), target, "handleNotification", "http://localhost:8080/first");

		String message = "{ \"Type\" : \"Notification\"," +
				" \"Message\" : \"world\"}";
		mockHttpServletRequest.setContent(message.getBytes("UTF-8"));

		handler.handleRequest(mockHttpServletRequest, mockHttpServletResponse);
		Assert.assertEquals("world", target.getLastMessage());
	}

	@Test
	public void testRegisterClass() throws Exception {
		Tomcat tomcat = new Tomcat();
		tomcat.setBaseDir(System.getProperty("java.io.tmpdir"));
		tomcat.setPort(0);
		tomcat.setHostname("localhost");

		Context context = tomcat.addWebapp("/test", System.getProperty("java.io.tmpdir"));
		context.setApplicationLifecycleListeners(new Object[]{new ContextLoaderListener()});
		ApplicationParameter parameter = new ApplicationParameter();
		parameter.setName(ContextLoaderListener.CONFIG_LOCATION_PARAM);
		parameter.setValue("classpath:org/elasticspring/messaging/endpoint/web-app-config.xml");
		context.addApplicationParameter(parameter);
		tomcat.start();

		Assert.assertTrue(context.getState().isAvailable());

		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost("http://localhost:" + tomcat.getConnector().getLocalPort() + "/test/first");

		String message = "{ \"Type\" : \"Notification\"," +
				" \"Message\" : \"world\"}";
		httpPost.setEntity(new StringEntity(message));
		HttpResponse response = httpClient.execute(httpPost);
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		tomcat.stop();
	}

	static class SimpleHttpRequestHandler {

		private String lastMessage;

		@TopicListener(topicName = "test", protocol = TopicListener.NotificationProtocol.HTTP, endpoint = "foo")
		public void handleNotification(String message) {
			this.lastMessage = message;
		}

		public String getLastMessage() {
			return this.lastMessage;
		}
	}
}
