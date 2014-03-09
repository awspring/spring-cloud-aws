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

package org.elasticspring.messaging.config.annotation;

import org.elasticspring.messaging.config.AmazonMessagingConfigurationUtils;
import org.elasticspring.messaging.endpoint.HttpNotificationEndpointFactoryBean;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class TopicListenerBeanDefinitionRegistryPostProcessorTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testCreateHttpEndpoint() throws Exception {
		TopicListenerBeanDefinitionRegistryPostProcessor postProcessor = new TopicListenerBeanDefinitionRegistryPostProcessor();
		SimpleBeanDefinitionRegistry simpleBeanDefinitionRegistry = new SimpleBeanDefinitionRegistry();

		simpleBeanDefinitionRegistry.registerBeanDefinition("httpEndpoint",
				BeanDefinitionBuilder.rootBeanDefinition(HttpEndpoint.class).getBeanDefinition());
		postProcessor.postProcessBeanDefinitionRegistry(simpleBeanDefinitionRegistry);

		Assert.assertEquals(3, simpleBeanDefinitionRegistry.getBeanDefinitionCount());
		BeanDefinition definition = simpleBeanDefinitionRegistry.getBeanDefinition(
				HttpNotificationEndpointFactoryBean.class.getName() + "#0");

		Assert.assertEquals(AmazonMessagingConfigurationUtils.SNS_CLIENT_BEAN_NAME, ((RuntimeBeanReference) definition.getConstructorArgumentValues().
				getArgumentValue(0, RuntimeBeanReference.class).getValue()).getBeanName());
		Assert.assertEquals("test", definition.getConstructorArgumentValues().
				getArgumentValue(1, String.class).getValue());
		Assert.assertEquals(TopicListener.NotificationProtocol.HTTP, definition.getConstructorArgumentValues().
				getArgumentValue(2, TopicListener.NotificationProtocol.class).getValue());
		Assert.assertEquals("http://localhost/myapp/myLister", definition.getConstructorArgumentValues().
				getArgumentValue(3, String.class).getValue());
		Assert.assertEquals("httpEndpoint", ((RuntimeBeanReference) definition.getConstructorArgumentValues().
				getArgumentValue(4, RuntimeBeanReference.class).getValue()).getBeanName());
		Assert.assertEquals("doReceiveNotification", definition.getConstructorArgumentValues().
				getArgumentValue(5, String.class).getValue());

		Assert.assertTrue(simpleBeanDefinitionRegistry.containsBeanDefinition(AmazonMessagingConfigurationUtils.SNS_CLIENT_BEAN_NAME));
	}

	@Test
	public void testCreateHttpsEndpoint() throws Exception {
		TopicListenerBeanDefinitionRegistryPostProcessor postProcessor = new TopicListenerBeanDefinitionRegistryPostProcessor();
		SimpleBeanDefinitionRegistry simpleBeanDefinitionRegistry = new SimpleBeanDefinitionRegistry();

		simpleBeanDefinitionRegistry.registerBeanDefinition("httpsEndpoint",
				BeanDefinitionBuilder.rootBeanDefinition(HttpsEndpoint.class).getBeanDefinition());
		postProcessor.postProcessBeanDefinitionRegistry(simpleBeanDefinitionRegistry);

		Assert.assertEquals(3, simpleBeanDefinitionRegistry.getBeanDefinitionCount());
		BeanDefinition definition = simpleBeanDefinitionRegistry.getBeanDefinition(
				HttpNotificationEndpointFactoryBean.class.getName() + "#0");

		Assert.assertEquals(AmazonMessagingConfigurationUtils.SNS_CLIENT_BEAN_NAME, ((RuntimeBeanReference) definition.getConstructorArgumentValues().
				getArgumentValue(0, RuntimeBeanReference.class).getValue()).getBeanName());
		Assert.assertEquals("test", definition.getConstructorArgumentValues().
				getArgumentValue(1, String.class).getValue());
		Assert.assertEquals(TopicListener.NotificationProtocol.HTTPS, definition.getConstructorArgumentValues().
				getArgumentValue(2, TopicListener.NotificationProtocol.class).getValue());
		Assert.assertEquals("https://localhost/myapp/myLister", definition.getConstructorArgumentValues().
				getArgumentValue(3, String.class).getValue());
		Assert.assertEquals("httpsEndpoint", ((RuntimeBeanReference) definition.getConstructorArgumentValues().
				getArgumentValue(4, RuntimeBeanReference.class).getValue()).getBeanName());
		Assert.assertEquals("doReceiveNotification", definition.getConstructorArgumentValues().
				getArgumentValue(5, String.class).getValue());

		Assert.assertTrue(simpleBeanDefinitionRegistry.containsBeanDefinition(AmazonMessagingConfigurationUtils.SNS_CLIENT_BEAN_NAME));
	}

	@Test
	public void testCreateHttpEndpointWithCustomSns() throws Exception {
		TopicListenerBeanDefinitionRegistryPostProcessor postProcessor = new TopicListenerBeanDefinitionRegistryPostProcessor();
		SimpleBeanDefinitionRegistry simpleBeanDefinitionRegistry = new SimpleBeanDefinitionRegistry();

		simpleBeanDefinitionRegistry.registerBeanDefinition("httpEndpoint",
				BeanDefinitionBuilder.rootBeanDefinition(HttpEndpoint.class).getBeanDefinition());

		postProcessor.setAmazonSnsBeanName("customSns");
		postProcessor.postProcessBeanDefinitionRegistry(simpleBeanDefinitionRegistry);


		Assert.assertEquals(2, simpleBeanDefinitionRegistry.getBeanDefinitionCount());
		BeanDefinition definition = simpleBeanDefinitionRegistry.getBeanDefinition(
				HttpNotificationEndpointFactoryBean.class.getName() + "#0");

		Assert.assertEquals("customSns", ((RuntimeBeanReference) definition.getConstructorArgumentValues().
				getArgumentValue(0, RuntimeBeanReference.class).getValue()).getBeanName());
		Assert.assertEquals("test", definition.getConstructorArgumentValues().
				getArgumentValue(1, String.class).getValue());
		Assert.assertEquals(TopicListener.NotificationProtocol.HTTP, definition.getConstructorArgumentValues().
				getArgumentValue(2, TopicListener.NotificationProtocol.class).getValue());
		Assert.assertEquals("http://localhost/myapp/myLister", definition.getConstructorArgumentValues().
				getArgumentValue(3, String.class).getValue());
		Assert.assertEquals("httpEndpoint", ((RuntimeBeanReference) definition.getConstructorArgumentValues().
				getArgumentValue(4, RuntimeBeanReference.class).getValue()).getBeanName());
		Assert.assertEquals("doReceiveNotification", definition.getConstructorArgumentValues().
				getArgumentValue(5, String.class).getValue());
	}

	static class HttpEndpoint {

		@TopicListener(protocol = TopicListener.NotificationProtocol.HTTP, topicName = "test",
				endpoint = "http://localhost/myapp/myLister")
		public void doReceiveNotification(String message) {
			LoggerFactory.getLogger(getClass()).debug("Received message {}", message);
		}
	}

	static class HttpsEndpoint {

		@TopicListener(protocol = TopicListener.NotificationProtocol.HTTPS, topicName = "test",
				endpoint = "https://localhost/myapp/myLister")
		public void doReceiveNotification(String message) {
			LoggerFactory.getLogger(getClass()).debug("Received message {}", message);
		}
	}

}
