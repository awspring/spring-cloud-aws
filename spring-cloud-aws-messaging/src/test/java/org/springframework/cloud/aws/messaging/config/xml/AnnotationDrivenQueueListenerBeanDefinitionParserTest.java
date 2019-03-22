/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.messaging.config.xml;

import java.util.List;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.cloud.aws.core.env.StackResourceRegistryDetectingResourceIdResolver;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SendToHandlerMethodReturnValueHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class AnnotationDrivenQueueListenerBeanDefinitionParserTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void parseInternal_minimalConfiguration_shouldProduceContainerWithDefaultAmazonSqsBean()
			throws Exception {
		// Act
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-minimal.xml", getClass());

		// Assert
		AmazonSQSAsync amazonSqsClient = applicationContext.getBean(AmazonSQSAsync.class);
		assertThat(amazonSqsClient).isNotNull();

		SimpleMessageListenerContainer container = applicationContext
				.getBean(SimpleMessageListenerContainer.class);
		assertThat(container).isNotNull();

		assertThat(ReflectionTestUtils.getField(container, "amazonSqs"))
				.isSameAs(amazonSqsClient);
		assertThat(ReflectionTestUtils.getField(container, "resourceIdResolver"))
				.isSameAs(applicationContext
						.getBean(StackResourceRegistryDetectingResourceIdResolver.class));

		QueueMessageHandler queueMessageHandler = (QueueMessageHandler) ReflectionTestUtils
				.getField(container, "messageHandler");
		HandlerMethodReturnValueHandler sendToReturnValueHandler = queueMessageHandler
				.getReturnValueHandlers().get(0);
		assertThat(SendToHandlerMethodReturnValueHandler.class
				.isInstance(sendToReturnValueHandler)).isTrue();
		QueueMessagingTemplate queueMessagingTemplate = (QueueMessagingTemplate) ReflectionTestUtils
				.getField(sendToReturnValueHandler, "messageTemplate");

		assertThat(CompositeMessageConverter.class
				.isInstance(queueMessagingTemplate.getMessageConverter())).isTrue();

		@SuppressWarnings("unchecked")
		List<MessageConverter> messageConverters = (List<MessageConverter>) ReflectionTestUtils
				.getField(queueMessagingTemplate.getMessageConverter(), "converters");
		assertThat(messageConverters.size()).isEqualTo(2);
		assertThat(StringMessageConverter.class.isInstance(messageConverters.get(0)))
				.isTrue();
		assertThat(MappingJackson2MessageConverter.class
				.isInstance(messageConverters.get(1))).isTrue();

		StringMessageConverter stringMessageConverter = (StringMessageConverter) messageConverters
				.get(0);
		assertThat(stringMessageConverter.getSerializedPayloadClass())
				.isSameAs(String.class);
		assertThat(ReflectionTestUtils.getField(stringMessageConverter,
				"strictContentTypeMatch")).isEqualTo(false);

		MappingJackson2MessageConverter jackson2MessageConverter = (MappingJackson2MessageConverter) messageConverters
				.get(1);
		assertThat(jackson2MessageConverter.getSerializedPayloadClass())
				.isSameAs(String.class);
		assertThat(ReflectionTestUtils.getField(jackson2MessageConverter,
				"strictContentTypeMatch")).isEqualTo(false);
	}

	@Test
	public void parseInternal_customSqsClient_shouldProduceContainerWithCustomSqsClientUsed()
			throws Exception {
		// Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-amazon-sqs.xml", getClass()));

		// Assert
		BeanDefinition sqsAsync = registry.getBeanDefinition("myClient");
		assertThat(sqsAsync).isNotNull();

		BeanDefinition abstractContainerDefinition = registry
				.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		assertThat(abstractContainerDefinition).isNotNull();

		assertThat(abstractContainerDefinition.getPropertyValues().size()).isEqualTo(3);
		assertThat(((RuntimeBeanReference) abstractContainerDefinition.getPropertyValues()
				.getPropertyValue("amazonSqs").getValue()).getBeanName())
						.isEqualTo("myClient");
	}

	@Test
	public void parseInternal_customTaskExecutor_shouldCreateContainerAndClientWithCustomTaskExecutor()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-task-executor.xml", getClass()));

		// Assert
		BeanDefinition executor = beanFactory.getBeanDefinition("executor");
		assertThat(executor).isNotNull();

		BeanDefinition abstractContainerDefinition = beanFactory
				.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		assertThat(abstractContainerDefinition).isNotNull();

		assertThat(abstractContainerDefinition.getPropertyValues().size()).isEqualTo(4);
		assertThat(((RuntimeBeanReference) abstractContainerDefinition.getPropertyValues()
				.getPropertyValue("taskExecutor").getValue()).getBeanName())
						.isEqualTo("executor");

		AmazonSQSBufferedAsyncClient bufferedAsyncClient = beanFactory
				.getBean(AmazonSQSBufferedAsyncClient.class);
		assertThat(bufferedAsyncClient).isNotNull();
	}

	@Test
	public void parseInternal_withSendToMessageTemplateAttribute_mustBeSetOnTheBeanDefinition()
			throws Exception {
		// Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-with-send-to-message-template.xml",
				getClass()));

		// Assert
		BeanDefinition queueMessageHandler = registry
				.getBeanDefinition(QueueMessageHandler.class.getName() + "#0");
		assertThat(queueMessageHandler).isNotNull();

		assertThat(queueMessageHandler.getPropertyValues().size()).isEqualTo(1);
		ManagedList<?> returnValueHandlers = (ManagedList<?>) queueMessageHandler
				.getPropertyValues().getPropertyValue("customReturnValueHandlers")
				.getValue();
		assertThat(returnValueHandlers.size()).isEqualTo(1);
		RootBeanDefinition sendToReturnValueHandler = (RootBeanDefinition) returnValueHandlers
				.get(0);

		assertThat(((RuntimeBeanReference) sendToReturnValueHandler
				.getConstructorArgumentValues()
				.getArgumentValue(0, RuntimeBeanReference.class).getValue())
						.getBeanName()).isEqualTo("messageTemplate");
	}

	@Test
	public void parseInternal_withCustomProperties_customPropertiesConfiguredOnContainer()
			throws Exception {
		// Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-properties.xml", getClass()));

		// Assert
		BeanDefinition abstractContainerDefinition = registry
				.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		assertThat(abstractContainerDefinition).isNotNull();

		assertThat(abstractContainerDefinition.getPropertyValues()
				.getPropertyValue("autoStartup").getValue()).isEqualTo("false");
		assertThat(abstractContainerDefinition.getPropertyValues()
				.getPropertyValue("maxNumberOfMessages").getValue()).isEqualTo("9");
		assertThat(abstractContainerDefinition.getPropertyValues()
				.getPropertyValue("visibilityTimeout").getValue()).isEqualTo("6");
		assertThat(abstractContainerDefinition.getPropertyValues()
				.getPropertyValue("waitTimeOut").getValue()).isEqualTo("3");
	}

	@Test
	public void parseInternal_customArgumentResolvers_parsedAndConfiguredInQueueMessageHandler()
			throws Exception {
		// Arrange
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-argument-resolvers.xml",
				getClass()));

		// Act
		applicationContext.refresh();

		// Assert
		assertThat(applicationContext.getBean(QueueMessageHandler.class)).isNotNull();
		assertThat(applicationContext.getBean(QueueMessageHandler.class)
				.getCustomArgumentResolvers().size()).isEqualTo(1);
		assertThat(TestHandlerMethodArgumentResolver.class.isInstance(applicationContext
				.getBean(QueueMessageHandler.class).getCustomArgumentResolvers().get(0)))
						.isTrue();
	}

	@Test
	public void parseInternal_customReturnValueHandlers_parsedAndConfiguredInQueueMessageHandler()
			throws Exception {
		// Arrange
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-return-value-handlers.xml",
				getClass()));

		// Act
		applicationContext.refresh();

		// Assert
		assertThat(applicationContext.getBean(QueueMessageHandler.class)).isNotNull();
		assertThat(applicationContext.getBean(QueueMessageHandler.class)
				.getCustomReturnValueHandlers().size()).isEqualTo(2);
		assertThat(TestHandlerMethodReturnValueHandler.class
				.isInstance(applicationContext.getBean(QueueMessageHandler.class)
						.getCustomReturnValueHandlers().get(0))).isTrue();
	}

	@Test
	public void parseInternal_customerRegionConfigured_regionConfiguredAndParsedForInternalCreatedSqsClient()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-region.xml", getClass()));

		// Assert
		AmazonSQSBufferedAsyncClient amazonSQSBufferedAsyncClient = registry
				.getBean(AmazonSQSBufferedAsyncClient.class);
		Object amazonSqsAsyncClient = ReflectionTestUtils
				.getField(amazonSQSBufferedAsyncClient, "realSQS");

		assertThat(
				ReflectionTestUtils.getField(amazonSqsAsyncClient, "endpoint").toString())
						.isEqualTo("https://" + Region.getRegion(Regions.EU_WEST_1)
								.getServiceEndpoint("sqs"));
	}

	// @checkstyle:off
	@Test
	public void parseInternal_customerRegionProviderConfigured_regionProviderConfiguredAndParsedForInternalCreatedSqsClient()
			throws Exception {
		// @checkstyle:on
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-region-provider.xml", getClass()));

		// Assert
		AmazonSQSBufferedAsyncClient amazonSQSBufferedAsyncClient = registry
				.getBean(AmazonSQSBufferedAsyncClient.class);
		Object amazonSqsAsyncClient = ReflectionTestUtils
				.getField(amazonSQSBufferedAsyncClient, "realSQS");

		assertThat(
				ReflectionTestUtils.getField(amazonSqsAsyncClient, "endpoint").toString())
						.isEqualTo("https://" + Region.getRegion(Regions.AP_SOUTHEAST_2)
								.getServiceEndpoint("sqs"));
	}

	@Test
	public void contextRegion_clientWithoutRegion_shouldHaveTheRegionGloballyDefined()
			throws Exception {
		// Arrange & Act
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-context-region.xml", getClass());

		// Assert
		AmazonSQSBufferedAsyncClient amazonSQSBufferedAsyncClient = applicationContext
				.getBean(AmazonSQSBufferedAsyncClient.class);
		Object amazonSqsAsyncClient = ReflectionTestUtils
				.getField(amazonSQSBufferedAsyncClient, "realSQS");

		assertThat(
				ReflectionTestUtils.getField(amazonSqsAsyncClient, "endpoint").toString())
						.isEqualTo("https://" + Region.getRegion(Regions.AP_SOUTHEAST_1)
								.getServiceEndpoint("sqs"));
	}

	@Test
	public void parseInternal_customDestinationResolver_isUsedOnTheContainer()
			throws Exception {
		// Arrange & Act
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-custom-destination-resolver.xml",
				getClass());

		// Assert
		SimpleMessageListenerContainer container = applicationContext
				.getBean(SimpleMessageListenerContainer.class);
		DestinationResolver<?> customDestinationResolver = applicationContext
				.getBean(DestinationResolver.class);
		assertThat(customDestinationResolver == ReflectionTestUtils.getField(container,
				"destinationResolver")).isTrue();
	}

	@Test
	public void parseInternal_definedBackOffTime_shouldBeSetOnContainer()
			throws Exception {
		// Arrange & Act
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-back-off-time.xml", getClass());

		// Assert
		SimpleMessageListenerContainer container = applicationContext
				.getBean(SimpleMessageListenerContainer.class);
		assertThat(container.getBackOffTime()).isEqualTo(5000L);
	}

	private static class TestHandlerMethodArgumentResolver
			implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return false;
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, Message<?> message)
				throws Exception {
			return null;
		}

	}

	private static class TestHandlerMethodReturnValueHandler
			implements HandlerMethodReturnValueHandler {

		@Override
		public boolean supportsReturnType(MethodParameter returnType) {
			return false;
		}

		@Override
		public void handleReturnValue(Object returnValue, MethodParameter returnType,
				Message<?> message) throws Exception {

		}

	}

}
