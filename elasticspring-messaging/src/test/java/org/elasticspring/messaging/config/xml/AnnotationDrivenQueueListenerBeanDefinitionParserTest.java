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
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import org.elasticspring.config.AmazonWebserviceClientConfigurationUtils;
import org.elasticspring.messaging.listener.QueueMessageHandler;
import org.elasticspring.messaging.listener.SimpleMessageListenerContainer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class AnnotationDrivenQueueListenerBeanDefinitionParserTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void parseInternal_minimalConfiguration_shouldProduceContainerWithDefaultAmazonSqsBean() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-minimal.xml", getClass()));

		//Assert
		BeanDefinition sqsDefinition = registry.getBeanDefinition(AmazonWebserviceClientConfigurationUtils.
				getBeanName(AnnotationDrivenQueueListenerBeanDefinitionParser.AMAZON_BUFFER_CLIENT_CLASS_NAME));
		assertNotNull(sqsDefinition);

		BeanDefinition abstractContainerDefinition = registry.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		assertNotNull(abstractContainerDefinition);

		assertEquals(3, abstractContainerDefinition.getPropertyValues().size());
		assertEquals(AmazonWebserviceClientConfigurationUtils.getBeanName(AnnotationDrivenQueueListenerBeanDefinitionParser.AMAZON_BUFFER_CLIENT_CLASS_NAME),
				((RuntimeBeanReference) abstractContainerDefinition.getPropertyValues().getPropertyValue("amazonSqs").getValue()).getBeanName());
	}

	@Test
	public void parseInternal_customSqsClient_shouldProduceContainerWithCustomSqsClientUsed() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-amazon-sqs.xml", getClass()));

		//Assert
		BeanDefinition sqsAsync = registry.getBeanDefinition("myClient");
		assertNotNull(sqsAsync);

		BeanDefinition abstractContainerDefinition = registry.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		assertNotNull(abstractContainerDefinition);

		assertEquals(3, abstractContainerDefinition.getPropertyValues().size());
		assertEquals("myClient",
				((RuntimeBeanReference) abstractContainerDefinition.getPropertyValues().getPropertyValue("amazonSqs").getValue()).getBeanName());
	}

	@Test
	public void parseInternal_customTaskExecutor_shouldCreateContainerAndClientWithCustomTaskExecutor() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-task-executor.xml", getClass()));

		//Assert
		BeanDefinition executor = registry.getBeanDefinition("executor");
		assertNotNull(executor);

		BeanDefinition abstractContainerDefinition = registry.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		assertNotNull(abstractContainerDefinition);

		assertEquals(4, abstractContainerDefinition.getPropertyValues().size());
		assertEquals("executor",
				((RuntimeBeanReference) abstractContainerDefinition.getPropertyValues().getPropertyValue("taskExecutor").getValue()).getBeanName());
	}

	@Test
	public void parseInternal_withSendToMessageTemplateAttribute_mustBeSetOnTheBeanDefinition() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-with-send-to-message-template.xml", getClass()));

		//Assert
		BeanDefinition queueMessageHandler = registry.getBeanDefinition(QueueMessageHandler.class.getName() + "#0");
		assertNotNull(queueMessageHandler);

		assertEquals(1, queueMessageHandler.getPropertyValues().size());
		AbstractBeanDefinition returnValueHandler = (AbstractBeanDefinition) queueMessageHandler.getPropertyValues().getPropertyValue("defaultReturnValueHandler").getValue();
		assertEquals("messageTemplate",
				((RuntimeBeanReference) returnValueHandler.getConstructorArgumentValues().getArgumentValue(0, RuntimeBeanReference.class).getValue()).getBeanName());
	}

	@Test
	public void parseInternal_withCustomProperties_customPropertiesConfiguredOnContainer() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-properties.xml", getClass()));

		//Assert
		BeanDefinition abstractContainerDefinition = registry.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		assertNotNull(abstractContainerDefinition);

		assertEquals("false", abstractContainerDefinition.getPropertyValues().getPropertyValue("autoStartup").getValue());
		assertEquals("9", abstractContainerDefinition.getPropertyValues().getPropertyValue("maxNumberOfMessages").getValue());
		assertEquals("6", abstractContainerDefinition.getPropertyValues().getPropertyValue("visibilityTimeout").getValue());
		assertEquals("3", abstractContainerDefinition.getPropertyValues().getPropertyValue("waitTimeOut").getValue());
	}

	@Test
	public void parseInternal_customArgumentResolvers_parsedAndConfiguredInQueueMessageHandler() throws Exception {
		//Arrange
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-argument-resolvers.xml", getClass()));

		//Act
		applicationContext.refresh();

		//Assert
		assertNotNull(applicationContext.getBean(QueueMessageHandler.class));
		assertEquals(1, applicationContext.getBean(QueueMessageHandler.class).getCustomArgumentResolvers().size());
		assertTrue(TestHandlerMethodArgumentResolver.class.isInstance(applicationContext.getBean(QueueMessageHandler.class).getCustomArgumentResolvers().get(0)));
	}

	@Test
	public void parseInternal_customReturnValueHandlers_parsedAndConfiguredInQueueMessageHandler() throws Exception {
		//Arrange
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-return-value-handlers.xml", getClass()));

		//Act
		applicationContext.refresh();

		//Assert
		assertNotNull(applicationContext.getBean(QueueMessageHandler.class));
		assertEquals(1, applicationContext.getBean(QueueMessageHandler.class).getCustomReturnValueHandlers().size());
		assertTrue(TestHandlerMethodReturnValueHandler.class.isInstance(applicationContext.getBean(QueueMessageHandler.class).getCustomReturnValueHandlers().get(0)));
	}

	@Test
	public void parseInternal_customerRegionConfigured_regionConfiguredAndParsedForInternalCreatedSqsClient() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-region.xml", getClass()));

		//Assert
		BeanDefinition sqsDefinition = registry.getBeanDefinition(AmazonWebserviceClientConfigurationUtils.
				getBeanName(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonSQSAsyncClient.class.getName())));
		assertNotNull(sqsDefinition);

		BeanDefinition regionProviderDefinition = (BeanDefinition) sqsDefinition.getPropertyValues().getPropertyValue("region").getValue();

		assertEquals(Region.class.getName(), regionProviderDefinition.getBeanClassName());
		assertEquals("EU_WEST_1", regionProviderDefinition.getConstructorArgumentValues().getArgumentValue(0, String.class).getValue());
	}

	@Test
	public void parseInternal_customerRegionProviderConfigured_regionProviderConfiguredAndParsedForInternalCreatedSqsClient() throws Exception {
		//Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-region-provider.xml", getClass()));

		//Assert
		BeanDefinition sqsDefinition = registry.getBeanDefinition(AmazonWebserviceClientConfigurationUtils.
				getBeanName(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonSQSAsyncClient.class.getName())));
		assertNotNull(sqsDefinition);

		BeanDefinition regionProviderDefinition = (BeanDefinition) sqsDefinition.getPropertyValues().getPropertyValue("region").getValue();

		assertEquals("provider", ((RuntimeBeanReference) regionProviderDefinition.getPropertyValues().getPropertyValue("targetObject").getValue()).getBeanName());
	}

	private static class TestHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return false;
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
			return null;
		}
	}

	private static class TestHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

		@Override
		public boolean supportsReturnType(MethodParameter returnType) {
			return false;
		}

		@Override
		public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message) throws Exception {

		}
	}
}