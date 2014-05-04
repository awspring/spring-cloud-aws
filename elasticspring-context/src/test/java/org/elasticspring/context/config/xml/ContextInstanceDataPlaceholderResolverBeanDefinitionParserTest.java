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

package org.elasticspring.context.config.xml;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Agim Emruli
 */
public class ContextInstanceDataPlaceholderResolverBeanDefinitionParserTest {

	@Test
	public void parseInternal_singleElementDefined_beanDefinitionCreated() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-context.xml", getClass()));

		//Assert
		BeanFactoryPostProcessor postProcessor = beanFactory.getBean("AmazonEc2InstanceDataPropertySourcePostProcessor", BeanFactoryPostProcessor.class);
		Assert.assertNotNull(postProcessor);
	}

	@Test
	public void parseInternal_singleElementWithUserTagsMapDefined_userTagMapCreatedAlongWithPostProcessor() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-userTagsMap.xml", getClass()));

		//Assert
		Assert.assertTrue(beanFactory.containsBeanDefinition("myUserTags"));
		Assert.assertTrue(beanFactory.containsBeanDefinition("AmazonEC2"));
	}

}
