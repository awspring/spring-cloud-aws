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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class AbstractNotificationEndpointFactoryBeanTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testTopicDoesNotExist() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("No topic found for name :'test'");
		DestinationResolver<MessageChannel> destinationResolver = Mockito.mock(MockDestinationResolver.class);


		AmazonSNS amazonSns = Mockito.mock(AmazonSNS.class);
		Mockito.when(amazonSns.listTopics(new ListTopicsRequest(null))).thenReturn(new ListTopicsResult());

		AbstractNotificationEndpointFactoryBean<Subscription> factoryBean = new StubNotificationEndpointFactoryBean(amazonSns, "test",
				TopicListener.NotificationProtocol.SQS, "testQueue", new Object(), "notImportant");
		factoryBean.setDestinationResolver(destinationResolver);
		factoryBean.afterPropertiesSet();
	}

	@Test
	public void testSubscriptionNotFound() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("No subscription found for topic arn:'arn:aws:sns:eu-west:123456789012:test'");
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		Mockito.when(sns.listTopics(new ListTopicsRequest(null))).thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(topicArn)));

		Mockito.when(sns.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn))).thenReturn(
				new ListSubscriptionsByTopicResult());

		AbstractNotificationEndpointFactoryBean<Subscription> factoryBean = new StubNotificationEndpointFactoryBean(sns, "test",
				TopicListener.NotificationProtocol.SQS, "testQueue", new Object(), "notImportant");
		factoryBean.afterPropertiesSet();
	}

	@Test
	public void testSubscriptionNotFoundSecondRun() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("No subscription found for topic arn:'arn:aws:sns:eu-west:123456789012:test'");
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		Mockito.when(sns.listTopics(new ListTopicsRequest(null))).thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(topicArn)));

		Mockito.when(sns.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn))).thenReturn(
				new ListSubscriptionsByTopicResult().withNextToken("foo"));
		Mockito.when(sns.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn, "foo"))).thenReturn(
				new ListSubscriptionsByTopicResult());

		AbstractNotificationEndpointFactoryBean<Subscription> factoryBean = new StubNotificationEndpointFactoryBean(sns, "test",
				TopicListener.NotificationProtocol.SQS, "testQueue", new Object(), "notImportant");
		factoryBean.afterPropertiesSet();
	}

	@Test
	public void testSubscriptionFoundInSecondRun() throws Exception {
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);

		Mockito.when(sns.listTopics(new ListTopicsRequest(null))).thenReturn(new ListTopicsResult().withNextToken("mark"));

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		Mockito.when(sns.listTopics(new ListTopicsRequest("mark"))).thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(topicArn)));

		Subscription subscription = new Subscription().withEndpoint("testQueue").
				withProtocol(TopicListener.NotificationProtocol.SQS.getCanonicalName()).withTopicArn(topicArn);

		Mockito.when(sns.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn))).thenReturn(
				new ListSubscriptionsByTopicResult().withNextToken("foo"));
		Mockito.when(sns.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn, "foo"))).thenReturn(
				new ListSubscriptionsByTopicResult().withSubscriptions(subscription));

		AbstractNotificationEndpointFactoryBean<Subscription> factoryBean = new StubNotificationEndpointFactoryBean(sns, "test",
				TopicListener.NotificationProtocol.SQS, "testQueue", new Object(), "notImportant");
		factoryBean.afterPropertiesSet();

		Assert.assertNotNull(factoryBean.getObject());
		Assert.assertSame(subscription, factoryBean.getObject());
	}

	@Test
	public void testInitializingBeanCallbackCalled() throws Exception {
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		Mockito.when(sns.listTopics(new ListTopicsRequest(null))).thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(topicArn)));
		Mockito.when(sns.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn))).thenReturn(
				new ListSubscriptionsByTopicResult().withSubscriptions(new Subscription().withEndpoint("testQueue").
						withProtocol(TopicListener.NotificationProtocol.SQS.getCanonicalName()).withTopicArn(topicArn)));

		AbstractNotificationEndpointFactoryBean<InitAndDestroyBean> factoryBean = new AbstractNotificationEndpointFactoryBean<InitAndDestroyBean>(sns, "test",
				TopicListener.NotificationProtocol.SQS, "testQueue", new Object(), "notImportant") {

			@Override
			protected InitAndDestroyBean doCreateEndpointInstance(Subscription subscription) {
				return new InitAndDestroyBean();
			}

			@Override
			public Class<?> getObjectType() {
				return InitAndDestroyBean.class;
			}
		};

		factoryBean.afterPropertiesSet();

		Assert.assertNotNull(factoryBean.getObject());
		InitAndDestroyBean initAndDestroyBean = factoryBean.getObject();
		Assert.assertTrue(initAndDestroyBean.isInitialized());
		factoryBean.destroy();
		Assert.assertFalse(initAndDestroyBean.isInitialized());

	}

	private static class StubNotificationEndpointFactoryBean extends AbstractNotificationEndpointFactoryBean<Subscription> {

		private StubNotificationEndpointFactoryBean(AmazonSNS amazonSns, String topicName, TopicListener.NotificationProtocol protocol, String endpoint, Object target, String method) {
			super(amazonSns, topicName, protocol, endpoint, target, method);
		}

		@Override
		protected Subscription doCreateEndpointInstance(Subscription subscription) {
			return subscription;
		}

		@Override
		public Class<?> getObjectType() {
			return Object.class;
		}
	}

	static class InitAndDestroyBean implements InitializingBean, DisposableBean {

		private boolean initialized;

		@Override
		public void afterPropertiesSet() throws Exception {
			this.initialized = true;
		}

		@Override
		public void destroy() throws Exception {
			this.initialized = false;
		}

		boolean isInitialized() {
			return this.initialized;
		}
	}

	interface MockDestinationResolver extends DestinationResolver<MessageChannel>{

	}

}
