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

import org.elasticspring.messaging.config.annotation.TopicListenerBeanDefinitionRegistryPostProcessor;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class AnnotationDrivenTopicListenerBeanDefinitionParserTest {

	@Test
	public void testWithDefaultConfiguration() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-minimal.xml", getClass()));
		Assert.assertEquals(1, registry.getBeanDefinitionCount());
		BeanDefinition beanDefinition = registry.getBeanDefinition(TopicListenerBeanDefinitionRegistryPostProcessor.class.getName() + "#0");
		Assert.assertEquals(0, beanDefinition.getPropertyValues().size());
	}

	@Test
	public void testWithCustomSqs() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-amazon-sqs.xml", getClass()));
		Assert.assertEquals(2, registry.getBeanDefinitionCount());
		BeanDefinition beanDefinition = registry.getBeanDefinition(TopicListenerBeanDefinitionRegistryPostProcessor.class.getName() + "#0");
		Assert.assertEquals(1, beanDefinition.getPropertyValues().size());
		Assert.assertEquals("customSqs", beanDefinition.getPropertyValues().getPropertyValue("amazonSqsBeanName").getValue());
	}

	@Test
	public void testWithCustomSns() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-amazon-sns.xml", getClass()));
		Assert.assertEquals(2, registry.getBeanDefinitionCount());
		BeanDefinition beanDefinition = registry.getBeanDefinition(TopicListenerBeanDefinitionRegistryPostProcessor.class.getName() + "#0");
		Assert.assertEquals(1, beanDefinition.getPropertyValues().size());
		Assert.assertEquals("customSns", beanDefinition.getPropertyValues().getPropertyValue("amazonSnsBeanName").getValue());
	}
}
