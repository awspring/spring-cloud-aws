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
import org.elasticspring.messaging.config.annotation.QueueListenerBeanDefinitionRegistryPostProcessor;
import org.elasticspring.messaging.listener.SimpleMessageListenerContainer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class AnnotationDrivenQueueListenerBeanDefinitionParserTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testParseMinimalConfigWithDefaultContainer() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-minimal.xml", getClass()));

		BeanDefinition sqsDefinition = registry.getBeanDefinition(AmazonMessagingConfigurationUtils.SQS_CLIENT_BEAN_NAME);
		Assert.assertNotNull(sqsDefinition);

		BeanDefinition abstractContainerDefinition = registry.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		Assert.assertNotNull(abstractContainerDefinition);

		Assert.assertEquals(1, abstractContainerDefinition.getPropertyValues().size());
		Assert.assertEquals(AmazonMessagingConfigurationUtils.SQS_CLIENT_BEAN_NAME,
				((RuntimeBeanReference) abstractContainerDefinition.getPropertyValues().getPropertyValue("amazonSqs").getValue()).getBeanName());

		BeanDefinition registryProcessor = registry.getBeanDefinition(QueueListenerBeanDefinitionRegistryPostProcessor.class.getName() + "#0");
		Assert.assertNotNull(registryProcessor);
		Assert.assertEquals(1, registryProcessor.getPropertyValues().size());

		Assert.assertEquals(SimpleMessageListenerContainer.class.getName() + "#0",
				registryProcessor.getPropertyValues().getPropertyValue("parentBeanName").getValue());
	}

	@Test
	public void testParseCustomAmazonSqsClient() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-amazon-sqs.xml", getClass()));

		BeanDefinition sqsAsync = registry.getBeanDefinition("myClient");
		Assert.assertNotNull(sqsAsync);

		BeanDefinition abstractContainerDefinition = registry.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		Assert.assertNotNull(abstractContainerDefinition);

		Assert.assertEquals(1, abstractContainerDefinition.getPropertyValues().size());
		Assert.assertEquals("myClient",
				((RuntimeBeanReference) abstractContainerDefinition.getPropertyValues().getPropertyValue("amazonSqs").getValue()).getBeanName());
	}

	@Test
	public void testParseCustomTaskExecutor() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-task-executor.xml", getClass()));

		BeanDefinition executor = registry.getBeanDefinition("executor");
		Assert.assertNotNull(executor);

		BeanDefinition abstractContainerDefinition = registry.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		Assert.assertNotNull(abstractContainerDefinition);

		Assert.assertEquals(2, abstractContainerDefinition.getPropertyValues().size());
		Assert.assertEquals("executor",
				((RuntimeBeanReference) abstractContainerDefinition.getPropertyValues().getPropertyValue("taskExecutor").getValue()).getBeanName());
	}

	@Test
	public void testParseCustomProperties() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-properties.xml", getClass()));

		BeanDefinition abstractContainerDefinition = registry.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		Assert.assertNotNull(abstractContainerDefinition);

		Assert.assertEquals("false", abstractContainerDefinition.getPropertyValues().getPropertyValue("autoStartup").getValue());
		Assert.assertEquals("9", abstractContainerDefinition.getPropertyValues().getPropertyValue("maxNumberOfMessages").getValue());
		Assert.assertEquals("6", abstractContainerDefinition.getPropertyValues().getPropertyValue("visibilityTimeout").getValue());
		Assert.assertEquals("3", abstractContainerDefinition.getPropertyValues().getPropertyValue("waitTimeOut").getValue());
	}
}