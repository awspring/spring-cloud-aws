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

package org.elasticspring.messaging;

import org.apache.catalina.Context;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.startup.Tomcat;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.elasticspring.messaging.config.annotation.TopicListener;
import org.elasticspring.messaging.endpoint.NotificationEndpointHttpRequestHandler;
import org.elasticspring.support.TestStackEnvironment;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class HttpNotificationServiceTest {

	@Test
	public void testMessageHttpRequest() throws Exception {

		Tomcat tomcat = new Tomcat();
		tomcat.setBaseDir(System.getProperty("java.io.tmpdir"));
		tomcat.setPort(0);
		tomcat.setHostname("localhost");

		Context context = tomcat.addContext("", System.getProperty("java.io.tmpdir"));
		context.setApplicationLifecycleListeners(new Object[]{new ContextLoaderListener()});
		ApplicationParameter parameter = new ApplicationParameter();
		parameter.setName(ContextLoaderListener.CONFIG_LOCATION_PARAM);
		parameter.setValue("classpath:org/elasticspring/messaging/HttpNotificationServiceTest-web-context.xml");
		context.addApplicationParameter(parameter);

		tomcat.start();

		ServletContext servletContext = context.getServletContext();
		WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
		SimpleHttpEndpoint bean = applicationContext.getBean(SimpleHttpEndpoint.class);
		TestStackEnvironment testStackEnvironment = applicationContext.getBean(TestStackEnvironment.class);

		Assert.assertTrue(context.getState().isAvailable());

		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost("http://localhost:" + tomcat.getConnector().getLocalPort() + "/endpoint");

		String message = "{ \"Type\" : \"Notification\"," +
				" \"Message\" : \"world\", \"Subject\" : \"Hello\"}";
		httpPost.setEntity(new StringEntity(message));
		httpPost.setHeader(NotificationEndpointHttpRequestHandler.MESSAGE_TYPE, NotificationEndpointHttpRequestHandler.NOTIFICATION_MESSAGE_TYPE);
		httpPost.setHeader(NotificationEndpointHttpRequestHandler.TOPIC_ARN_HEADER, testStackEnvironment.getByLogicalId("HttpReceivingSnsTopic"));
		HttpResponse response = httpClient.execute(httpPost);
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		tomcat.stop();

		Assert.assertEquals("Hello", bean.getLastSubject());
		Assert.assertEquals("world", bean.getLastMessage());
	}

	static class SimpleHttpEndpoint {

		private String lastMessage;
		private String lastSubject;

		String getLastMessage() {
			return this.lastMessage;
		}

		String getLastSubject() {
			return this.lastSubject;
		}

		// TODO Alain: use @MessageMapping and create a new annotation @TopicAttributes for the protocol and endpoint
		@TopicListener(topicName = "#{testStackEnvironment.getByLogicalId('HttpReceivingSnsTopic')}",
				protocol = TopicListener.NotificationProtocol.HTTP,
				endpoint = "http://notimportant.elasticspring.com/endpoint")
		public void receiveNotification(String body, String subject) {
			this.lastMessage = body;
			this.lastSubject = subject;
		}
	}
}