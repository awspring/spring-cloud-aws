/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.context.config.xml;

import com.amazonaws.services.ec2.AmazonEC2Client;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
		assertNotNull(postProcessor);
		assertEquals(1, beanFactory.getBeanDefinitionCount());
	}

	@Test
	public void parseInternal_singleElementWithUserTagsMapDefined_userTagMapCreatedAlongWithPostProcessor() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-userTagsMap.xml", getClass()));

		//Assert
		assertTrue(beanFactory.containsBeanDefinition("myUserTags"));
		assertTrue(beanFactory.containsBeanDefinition(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonEC2Client.class.getName())));
	}

	@Test
	public void parseInternal_singleElementWithCustomAmazonEc2Client_userTagMapCreatedWithCustomEc2Client() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-customEc2Client.xml", getClass()));

		//Assert
		assertTrue(beanFactory.containsBeanDefinition("myUserTags"));

		ConstructorArgumentValues.ValueHolder valueHolder = beanFactory.getBeanDefinition("myUserTags").
				getConstructorArgumentValues().getArgumentValue(0, BeanReference.class);
		BeanReference beanReference = (BeanReference) valueHolder.getValue();
		assertEquals("amazonEC2Client", beanReference.getBeanName());
		assertFalse(beanFactory.containsBeanDefinition(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonEC2Client.class.getName())));
	}
}
