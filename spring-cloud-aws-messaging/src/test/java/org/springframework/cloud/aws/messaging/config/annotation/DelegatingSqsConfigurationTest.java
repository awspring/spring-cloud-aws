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

package org.springframework.cloud.aws.messaging.config.annotation;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Alain Sahli
 */
public class DelegatingSqsConfigurationTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void configuration_withMinimalBeans_shouldStartSqsListenerContainer() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(MinimalConfiguration.class);
		SimpleMessageListenerContainer container = applicationContext.getBean(SimpleMessageListenerContainer.class);

		// Assert
		assertTrue(container.isRunning());
		QueueMessageHandler queueMessageHandler = (QueueMessageHandler) ReflectionTestUtils.getField(container, "messageHandler");
		assertTrue(QueueMessageHandler.class.isInstance(queueMessageHandler));

		HandlerMethodReturnValueHandler sendToReturnValueHandler = queueMessageHandler.getCustomReturnValueHandlers().get(0);
		QueueMessagingTemplate messagingTemplate = (QueueMessagingTemplate) ReflectionTestUtils.getField(sendToReturnValueHandler, "messageTemplate");
		AmazonSQSBufferedAsyncClient amazonBufferedSqsClient = (AmazonSQSBufferedAsyncClient) ReflectionTestUtils.getField(messagingTemplate, "amazonSqs");
		AmazonSQSAsyncClient amazonSqsClient = (AmazonSQSAsyncClient) ReflectionTestUtils.getField(amazonBufferedSqsClient, "realSQS");
		assertNotNull(ReflectionTestUtils.getField(amazonSqsClient, "awsCredentialsProvider"));
	}

	@Test
	public void configuration_withCustomAmazonClient_shouldBeUsedByTheContainer() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ConfigurationWithCustomAmazonClient.class);

		// Assert
		AmazonSQS amazonSqsClient = applicationContext.getBean(AmazonSQS.class);
		assertEquals(ConfigurationWithCustomAmazonClient.CUSTOM_SQS_CLIENT, amazonSqsClient);
	}

	@Test
	public void configuration_withRegisteredConfigurers_shouldBeCalled() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(MinimalConfiguration.class, ConfigurationWithConfigurer.class);
		SimpleMessageListenerContainer container = applicationContext.getBean(SimpleMessageListenerContainer.class);
		QueueMessageHandler messageHandler = (QueueMessageHandler) ReflectionTestUtils.getField(container, "messageHandler");

		// Assert
		assertEquals(1, messageHandler.getCustomArgumentResolvers().size());
		assertEquals(ConfigurationWithConfigurer.CUSTOM_ARGUMENT_RESOLVER, messageHandler.getCustomArgumentResolvers().get(0));

		assertEquals(2, messageHandler.getCustomReturnValueHandlers().size());
		assertEquals(ConfigurationWithConfigurer.CUSTOM_RETURN_VALUE_HANDLER, messageHandler.getCustomReturnValueHandlers().get(0));
	}

	@Test
	public void configuration_withMultipleRegisteredConfigurers_shouldAllBeCalled() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(MinimalConfiguration.class, ConfigurationWithConfigurer.class, AnotherConfigurationWithConfigurer.class);
		SimpleMessageListenerContainer container = applicationContext.getBean(SimpleMessageListenerContainer.class);
		QueueMessageHandler messageHandler = (QueueMessageHandler) ReflectionTestUtils.getField(container, "messageHandler");

		// Assert
		assertEquals(2, messageHandler.getCustomArgumentResolvers().size());
		assertEquals(3, messageHandler.getCustomReturnValueHandlers().size());
	}

	@Test
	public void configuration_withCustomConfigurationFactory_shouldBeUsedToCreateTheContainer() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ConfigurationWithCustomContainerFactory.class);
		SimpleMessageListenerContainer container = applicationContext.getBean(SimpleMessageListenerContainer.class);

		// Assert
		assertEquals(ConfigurationWithCustomContainerFactory.AMAZON_SQS, ReflectionTestUtils.getField(container, "amazonSqs"));
		assertEquals(ConfigurationWithCustomContainerFactory.AUTO_STARTUP, container.isAutoStartup());
		assertEquals(ConfigurationWithCustomContainerFactory.MAX_NUMBER_OF_MESSAGES, ReflectionTestUtils.getField(container, "maxNumberOfMessages"));
		assertEquals(ConfigurationWithCustomContainerFactory.MESSAGE_HANDLER, ReflectionTestUtils.getField(container, "messageHandler"));
		assertEquals(ConfigurationWithCustomContainerFactory.RESOURCE_ID_RESOLVER, ReflectionTestUtils.getField(container, "resourceIdResolver"));
		assertEquals(ConfigurationWithCustomContainerFactory.TASK_EXECUTOR, ReflectionTestUtils.getField(container, "taskExecutor"));
		assertEquals(ConfigurationWithCustomContainerFactory.VISIBILITY_TIMEOUT, ReflectionTestUtils.getField(container, "visibilityTimeout"));
		assertEquals(ConfigurationWithCustomContainerFactory.WAIT_TIME_OUT, ReflectionTestUtils.getField(container, "waitTimeOut"));
	}

	@Test
	public void configuration_withoutAwsCredentials_shouldCreateAClientWithDefaultCredentialsProvider() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ConfigurationWithMissingAwsCredentials.class);

		// Assert
		AmazonSQSBufferedAsyncClient bufferedAmazonSqsClient = applicationContext.getBean(AmazonSQSBufferedAsyncClient.class);
		AmazonSQSAsyncClient amazonSqsClient = (AmazonSQSAsyncClient) ReflectionTestUtils.getField(bufferedAmazonSqsClient, "realSQS");
		assertTrue(DefaultAWSCredentialsProviderChain.class.isInstance(ReflectionTestUtils.getField(amazonSqsClient, "awsCredentialsProvider")));
	}

	@Test
	public void configuration_withCustomFactoryAndMultipleRegisteredConfigurers_resolversShouldBeAddedToTheMessageHandler() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ConfigurationWithCustomContainerFactory.class,
				ConfigurationWithConfigurer.class, AnotherConfigurationWithConfigurer.class);
		SimpleMessageListenerContainer container = applicationContext.getBean(SimpleMessageListenerContainer.class);
		QueueMessageHandler messageHandler = (QueueMessageHandler) ReflectionTestUtils.getField(container, "messageHandler");

		// Assert
		assertEquals(2, messageHandler.getCustomArgumentResolvers().size());
		assertEquals(2, messageHandler.getCustomReturnValueHandlers().size());
	}

	@EnableSqs
	@Configuration
	public static class MinimalConfiguration {

		@Bean
		public AWSCredentialsProvider awsCredentials() {
			return mock(AWSCredentialsProvider.class);
		}

	}

	@EnableSqs
	@Configuration
	public static class ConfigurationWithCustomAmazonClient {

		public static final AmazonSQS CUSTOM_SQS_CLIENT = mock(AmazonSQS.class);

		@Bean
		public AWSCredentialsProvider awsCredentials() {
			return mock(AWSCredentialsProvider.class);
		}

		@Bean
		public AmazonSQS amazonSQS() {
			return CUSTOM_SQS_CLIENT;
		}

	}

	@EnableSqs
	@Configuration
	public static class ConfigurationWithConfigurer extends QueueMessageHandlerConfigurerAdapter {

		public static final HandlerMethodReturnValueHandler CUSTOM_RETURN_VALUE_HANDLER = mock(HandlerMethodReturnValueHandler.class);
		public static final HandlerMethodArgumentResolver CUSTOM_ARGUMENT_RESOLVER = mock(HandlerMethodArgumentResolver.class);

		@Override
		public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
			returnValueHandlers.add(CUSTOM_RETURN_VALUE_HANDLER);
		}

		@Override
		public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
			argumentResolvers.add(CUSTOM_ARGUMENT_RESOLVER);
		}
	}

	@EnableSqs
	@Configuration
	public static class AnotherConfigurationWithConfigurer extends QueueMessageHandlerConfigurerAdapter {

		@Override
		public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
			returnValueHandlers.add(mock(HandlerMethodReturnValueHandler.class));
		}

		@Override
		public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
			argumentResolvers.add(mock(HandlerMethodArgumentResolver.class));
		}
	}

	@EnableSqs
	@Configuration
	public static class ConfigurationWithCustomContainerFactory {

		public static final AmazonSQS AMAZON_SQS = mock(AmazonSQS.class);
		public static final boolean AUTO_STARTUP = true;
		public static final int MAX_NUMBER_OF_MESSAGES = 1456;
		public static final QueueMessageHandler MESSAGE_HANDLER = new QueueMessageHandler();
		public static final ResourceIdResolver RESOURCE_ID_RESOLVER = mock(ResourceIdResolver.class);
		public static final SimpleAsyncTaskExecutor TASK_EXECUTOR = new SimpleAsyncTaskExecutor();
		public static final int VISIBILITY_TIMEOUT = 1789;
		public static final int WAIT_TIME_OUT = 12;

		@Bean
		public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory() {
			SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory(amazonSQS());
			factory.setAutoStartup(AUTO_STARTUP);
			factory.setMaxNumberOfMessages(MAX_NUMBER_OF_MESSAGES);
			factory.setMessageHandler(MESSAGE_HANDLER);
			factory.setResourceIdResolver(RESOURCE_ID_RESOLVER);
			factory.setTaskExecutor(TASK_EXECUTOR);
			factory.setVisibilityTimeout(VISIBILITY_TIMEOUT);
			factory.setWaitTimeOut(WAIT_TIME_OUT);

			return factory;
		}

		@Bean
		public AmazonSQS amazonSQS() {
			return AMAZON_SQS;
		}

	}

	@EnableSqs
	@Configuration
	public static class ConfigurationWithMissingAwsCredentials {

	}

}