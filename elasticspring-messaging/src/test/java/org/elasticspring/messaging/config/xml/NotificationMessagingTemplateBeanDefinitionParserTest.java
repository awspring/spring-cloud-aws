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

package org.elasticspring.messaging.config.xml;

import org.elasticspring.config.AmazonWebserviceClientConfigurationUtils;
import org.elasticspring.context.config.xml.GlobalBeanDefinitionUtils;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.assertEquals;

/**
 * @author Alain Sahli
 */
public class NotificationMessagingTemplateBeanDefinitionParserTest {

	@Test
	public void parseInternal_withMinimalConfig_shouldCreateDefaultTemplate() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-minimal.xml", getClass()));

		// Assert
		BeanDefinition notificationMessagingTemplateBeanDefinition = registry.getBeanDefinition("notificationMessagingTemplate");
		assertEquals(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonWebserviceClientConfigurationUtils.getBeanName("com.amazonaws.services.sns.AmazonSNSClient")),
				((RuntimeBeanReference) notificationMessagingTemplateBeanDefinition.getConstructorArgumentValues().getArgumentValue(0, RuntimeBeanReference.class).getValue()).getBeanName());
		assertEquals(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME, ((RuntimeBeanReference) notificationMessagingTemplateBeanDefinition
				.getConstructorArgumentValues().getArgumentValue(1, RuntimeBeanReference.class).getValue()).getBeanName());
	}

	@Test
	public void parseInternal_withCustomAmazonSnsClient_shouldPassItAsConstructorArg() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-amazon-sns.xml", getClass()));

		// Assert
		BeanDefinition notificationMessagingTemplateBeanDefinition = registry.getBeanDefinition("notificationMessagingTemplate");
		assertEquals("mySnsClient", ((RuntimeBeanReference) notificationMessagingTemplateBeanDefinition.getConstructorArgumentValues()
				.getArgumentValue(0, RuntimeBeanReference.class).getValue()).getBeanName());
	}

	@Test
	public void parseInternal_withCustomConverter_mustBeSetOnTemplate() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-converter.xml", getClass()));

		// Assert
		BeanDefinition notificationMessagingTemplateBeanDefinition = registry.getBeanDefinition("notificationMessagingTemplate");
		assertEquals("myCustomConverter", ((RuntimeBeanReference) notificationMessagingTemplateBeanDefinition.getPropertyValues()
				.getPropertyValue("messageConverter").getValue()).getBeanName());
	}

	@Test
	public void parseInternal_withDefaultDestination_mustBeSetOnTemplate() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-with-default-destination.xml", getClass()));

		// Assert
		BeanDefinition notificationMessagingTemplateBeanDefinition = registry.getBeanDefinition("notificationMessagingTemplate");
		assertEquals("myDefaultDestination", ((RuntimeBeanReference) notificationMessagingTemplateBeanDefinition.getPropertyValues()
				.getPropertyValue("defaultDestination").getValue()).getBeanName());
	}

}