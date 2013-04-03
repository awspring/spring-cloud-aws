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
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import org.elasticspring.messaging.config.annotation.TopicListener;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class HttpNotificationEndpointFactoryBeanTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testCreateEndpoint() throws Exception {
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		Mockito.when(sns.listTopics(new ListTopicsRequest(null))).thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(topicArn)));

		Mockito.when(sns.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn))).thenReturn(
				new ListSubscriptionsByTopicResult().withNextToken("foo"));
		Mockito.when(sns.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn, "foo"))).thenReturn(
				new ListSubscriptionsByTopicResult().withSubscriptions(new Subscription().withProtocol("http").
						withTopicArn(topicArn).withEndpoint("/test/myEndpoint")));

		HttpNotificationEndpointFactoryBean httpNotificationEndpointFactoryBean =
				new HttpNotificationEndpointFactoryBean(sns, "test", TopicListener.NotificationProtocol.HTTP, "/test/myEndpoint", new Object(), "myMethod");

		ServletContext servletContext = Mockito.mock(ServletContext.class);
		ServletRegistration.Dynamic dynamic = Mockito.mock(ServletRegistration.Dynamic.class);
		Mockito.when(servletContext.addServlet(Mockito.eq("endPointBeanName"), Mockito.isA(HttpRequestHandlerServlet.class))).thenReturn(dynamic);
		Mockito.when(servletContext.getContextPath()).thenReturn("/test");
		httpNotificationEndpointFactoryBean.setServletContext(servletContext);
		httpNotificationEndpointFactoryBean.setBeanName("endPointBeanName");
		httpNotificationEndpointFactoryBean.afterPropertiesSet();
		Object object = httpNotificationEndpointFactoryBean.getObject();
		Assert.assertNotNull(object);
	}

	@Test
	public void testCreateEndpointWrongProtocol() throws Exception {
		this.expectedException.expectMessage("This endpoint only support http and https endpoints");
		this.expectedException.expect(IllegalArgumentException.class);
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);
		//noinspection ResultOfObjectAllocationIgnored
		new HttpNotificationEndpointFactoryBean(sns, "test",
				TopicListener.NotificationProtocol.SQS, "test", new Object(), "test");
	}
}
