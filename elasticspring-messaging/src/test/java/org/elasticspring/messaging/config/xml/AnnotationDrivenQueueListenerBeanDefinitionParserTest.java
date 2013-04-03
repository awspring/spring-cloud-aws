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

package org.elasticspring.messaging.config.xml;

import org.elasticspring.messaging.config.AmazonMessagingConfigurationUtils;
import org.elasticspring.messaging.config.annotation.QueueListenerBeanPostProcessor;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class AnnotationDrivenQueueListenerBeanDefinitionParserTest {

	@Test
	public void testParseMinimalConfig() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-minimal.xml", getClass()));

		BeanDefinition sqsAsync = registry.getBeanDefinition(AmazonMessagingConfigurationUtils.SQS_CLIENT_BEAN_NAME);
		Assert.assertNotNull(sqsAsync);

		BeanDefinition beanDefinition = registry.getBeanDefinition(QueueListenerBeanPostProcessor.class.getName() + "#0");
		@SuppressWarnings("unchecked") Map<String, Object> configuration =
				(Map<String, Object>) beanDefinition.getPropertyValues().getPropertyValue("messageListenerContainerConfiguration").getValue();
		RuntimeBeanReference reference = (RuntimeBeanReference) configuration.get("amazonSqs");
		Assert.assertEquals(AmazonMessagingConfigurationUtils.SQS_CLIENT_BEAN_NAME, reference.getBeanName());
	}

	@Test
	public void testParseCustomAmazonSqsClient() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-amazon-sqs.xml", getClass()));

		BeanDefinition sqsAsync = registry.getBeanDefinition("myClient");
		Assert.assertNotNull(sqsAsync);

		BeanDefinition beanDefinition = registry.getBeanDefinition(QueueListenerBeanPostProcessor.class.getName() + "#0");
		@SuppressWarnings("unchecked") Map<String, Object> configuration =
				(Map<String, Object>) beanDefinition.getPropertyValues().getPropertyValue("messageListenerContainerConfiguration").getValue();
		RuntimeBeanReference reference = (RuntimeBeanReference) configuration.get("amazonSqs");
		Assert.assertEquals("myClient", reference.getBeanName());
	}

	@Test
	public void testParseCustomTaskManager() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-task-executor.xml", getClass()));

		BeanDefinition sqsAsync = registry.getBeanDefinition("executor");
		Assert.assertNotNull(sqsAsync);

		BeanDefinition beanDefinition = registry.getBeanDefinition(QueueListenerBeanPostProcessor.class.getName() + "#0");
		@SuppressWarnings("unchecked") Map<String, Object> configuration =
				(Map<String, Object>) beanDefinition.getPropertyValues().getPropertyValue("messageListenerContainerConfiguration").getValue();
		RuntimeBeanReference reference = (RuntimeBeanReference) configuration.get("taskManager");
		Assert.assertEquals("executor", reference.getBeanName());
	}

	@Test
	public void testParseCustomProperties() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-properties.xml", getClass()));

		BeanDefinition beanDefinition = registry.getBeanDefinition(QueueListenerBeanPostProcessor.class.getName() + "#0");
		@SuppressWarnings("unchecked") Map<String, Object> configuration =
				(Map<String, Object>) beanDefinition.getPropertyValues().getPropertyValue("messageListenerContainerConfiguration").getValue();
		Assert.assertEquals("false", configuration.get("autoStartup"));
		Assert.assertEquals("9", configuration.get("maxNumberOfMessages"));
		Assert.assertEquals("6", configuration.get("visibilityTimeout"));
		Assert.assertEquals("3", configuration.get("waitTimeOut"));
	}
}