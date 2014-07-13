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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import org.elasticspring.config.AmazonWebserviceClientConfigurationUtils;
import org.elasticspring.context.config.xml.GlobalBeanDefinitionUtils;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author Alain Sahli
 */
public class QueueMessagingTemplateBeanDefinitionParserTest {

	@Test
	public void parseInternal_withMinimalConfig_shouldProduceAQueueMessagingTemplateWithDefaults() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-minimal.xml", getClass()));

		// Assert
		BeanDefinition queueMessagingTemplateBeanDefinition = registry.getBeanDefinition("queueMessagingTemplate");
		assertEquals(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonSQSAsync.class.getName()),
				((RuntimeBeanReference) queueMessagingTemplateBeanDefinition.getConstructorArgumentValues().getArgumentValue(0, RuntimeBeanReference.class).getValue()).getBeanName());
		assertEquals(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME, ((RuntimeBeanReference) queueMessagingTemplateBeanDefinition
				.getConstructorArgumentValues().getArgumentValue(1, RuntimeBeanReference.class).getValue()).getBeanName());
		String jacksonConverter = "org.springframework.messaging.converter.MappingJackson2MessageConverter";
		assertEquals(jacksonConverter, ((RootBeanDefinition) queueMessagingTemplateBeanDefinition.getPropertyValues()
				.getPropertyValue("messageConverter").getValue()).getBeanClassName());
	}

	@Test
	public void parseInternal_withCustomAmazonSqsClient_shouldPassItAsConstructorArg() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-amazon-sqs.xml", getClass()));

		// Assert
		BeanDefinition queueMessagingTemplateBeanDefinition = registry.getBeanDefinition("queueMessagingTemplate");
		assertEquals("myClient", ((RuntimeBeanReference) queueMessagingTemplateBeanDefinition.getConstructorArgumentValues()
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
		BeanDefinition queueMessagingTemplateBeanDefinition = registry.getBeanDefinition("queueMessagingTemplate");
		assertEquals("myCustomConverter", ((RuntimeBeanReference) queueMessagingTemplateBeanDefinition.getPropertyValues()
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
		BeanDefinition queueMessagingTemplateBeanDefinition = registry.getBeanDefinition("queueMessagingTemplate");
		assertEquals("myDefaultDestination", ((RuntimeBeanReference) queueMessagingTemplateBeanDefinition.getPropertyValues()
				.getPropertyValue("defaultDestination").getValue()).getBeanName());
	}

	@Test
	public void parseInternal_withCustomRegion_shouldConfigureDefaultClientWithCustomRegion() throws Exception {
		//Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-region.xml", getClass()));

		// Assert
		AmazonSQSBufferedAsyncClient amazonSqs = registry.getBean(AmazonSQSBufferedAsyncClient.class);
		Object amazonSqsAsyncClient = ReflectionTestUtils.getField(amazonSqs, "realSQS");
		assertEquals("https://" + Region.getRegion(Regions.SA_EAST_1).getServiceEndpoint("sqs"), ReflectionTestUtils.getField(amazonSqsAsyncClient, "endpoint").toString());
	}

	@Test
	public void parseInternal_withCustomRegionProvider_shouldConfigureDefaultClientWithCustomRegionReturnedByProvider() throws Exception {
		//Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-region-provider.xml", getClass()));

		// Assert
		AmazonSQSBufferedAsyncClient amazonSqs = registry.getBean(AmazonSQSBufferedAsyncClient.class);
		Object amazonSqsAsyncClient = ReflectionTestUtils.getField(amazonSqs, "realSQS");
		assertEquals("https://" + Region.getRegion(Regions.AP_SOUTHEAST_2).getServiceEndpoint("sqs"), ReflectionTestUtils.getField(amazonSqsAsyncClient, "endpoint").toString());
	}

}