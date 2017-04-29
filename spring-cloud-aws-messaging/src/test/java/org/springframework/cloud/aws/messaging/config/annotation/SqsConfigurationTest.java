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
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.aws.context.config.annotation.EnableContextRegion;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.cloud.aws.messaging.support.destination.DynamicQueueUrlDestinationResolver;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * @author Alain Sahli
 */
public class SqsConfigurationTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void configuration_withMinimalBeans_shouldStartSqsListenerContainer() throws Exception {
        // Arrange & Act
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(MinimalConfiguration.class);
        SimpleMessageListenerContainer container = applicationContext.getBean(SimpleMessageListenerContainer.class);

        // Assert
        assertTrue(container.isRunning());
        QueueMessageHandler queueMessageHandler = applicationContext.getBean(QueueMessageHandler.class);
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
        AmazonSQSAsync amazonSqsClient = applicationContext.getBean(AmazonSQSAsync.class);
        assertEquals(ConfigurationWithCustomAmazonClient.CUSTOM_SQS_CLIENT, amazonSqsClient);
    }

    @Test
    public void messageHandler_withFactoryConfiguration_shouldUseCustomValues() throws Exception {
        // Arrange & Act
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ConfigurationWithCustomizedMessageHandler.class);
        QueueMessageHandler messageHandler = applicationContext.getBean(QueueMessageHandler.class);

        // Assert
        assertEquals(1, messageHandler.getCustomArgumentResolvers().size());
        assertEquals(ConfigurationWithCustomizedMessageHandler.CUSTOM_ARGUMENT_RESOLVER, messageHandler.getCustomArgumentResolvers().get(0));

        assertEquals(2, messageHandler.getCustomReturnValueHandlers().size());
        assertEquals(ConfigurationWithCustomizedMessageHandler.CUSTOM_RETURN_VALUE_HANDLER, messageHandler.getCustomReturnValueHandlers().get(0));

        Object sendToMessageTemplate = ReflectionTestUtils.getField(messageHandler.getReturnValueHandlers().get(1), "messageTemplate");
        assertEquals(ConfigurationWithCustomizedMessageHandler.CUSTOM_AMAZON_SQS, ReflectionTestUtils.getField(sendToMessageTemplate, "amazonSqs"));

        Object destinationResolver = ReflectionTestUtils.getField(sendToMessageTemplate, "destinationResolver");
        Object targetDestinationResolver = ReflectionTestUtils.getField(destinationResolver, "targetDestinationResolver");
        assertEquals(ConfigurationWithCustomizedMessageHandler.CUSTOM_RESOURCE_ID_RESOLVER, ReflectionTestUtils.getField(targetDestinationResolver, "resourceIdResolver"));
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
        assertTrue(ConfigurationWithCustomContainerFactory.DESTINATION_RESOLVER == ReflectionTestUtils.getField(container, "destinationResolver"));
        assertEquals(ConfigurationWithCustomContainerFactory.BACK_OFF_TIME, container.getBackOffTime());
    }

    @Test
    public void configuration_withCustomSendToMessageTemplate_shouldUseTheConfiguredTemplate() throws Exception {
        // Arrange & Act
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ConfigurationWithCustomSendToMessageTemplate.class);
        QueueMessageHandler queueMessageHandler = applicationContext.getBean(QueueMessageHandler.class);

        // Assert
        assertEquals(1, queueMessageHandler.getReturnValueHandlers().size());
        assertTrue(ConfigurationWithCustomSendToMessageTemplate.SEND_TO_MESSAGE_TEMPLATE ==
                ReflectionTestUtils.getField(queueMessageHandler.getReturnValueHandlers().get(0), "messageTemplate"));
    }

    @Test
    public void queueMessageHandlerBeanMustBeSetOnContainer() throws Exception {
        // Arrange & Act
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(MinimalConfiguration.class);
        SimpleMessageListenerContainer simpleMessageListenerContainer = applicationContext.getBean(SimpleMessageListenerContainer.class);
        QueueMessageHandler queueMessageHandler = applicationContext.getBean(QueueMessageHandler.class);

        // Assert
        assertEquals(queueMessageHandler, ReflectionTestUtils.getField(simpleMessageListenerContainer, "messageHandler"));
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
    public void configuration_withRegionProvider_shouldUseItForClient() throws Exception {
        // Arrange & Act
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ConfigurationWithRegionProvider.class);
        AmazonSQSAsync bufferedAmazonSqsClient = applicationContext.getBean(AmazonSQSAsync.class);
        AmazonSQSAsyncClient amazonSqs = (AmazonSQSAsyncClient) ReflectionTestUtils.getField(bufferedAmazonSqsClient, "realSQS");

        // Assert
        assertEquals("https://" + Region.getRegion(Regions.EU_WEST_1).getServiceEndpoint("sqs"), ReflectionTestUtils.getField(amazonSqs, "endpoint").toString());
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

        public static final AmazonSQSAsync CUSTOM_SQS_CLIENT = mock(AmazonSQSAsync.class, withSettings().stubOnly());

        @Bean
        public AWSCredentialsProvider awsCredentials() {
            return mock(AWSCredentialsProvider.class);
        }

        @Bean
        public AmazonSQSAsync amazonSQS() {
            return CUSTOM_SQS_CLIENT;
        }

    }

    @EnableSqs
    @Configuration
    public static class ConfigurationWithCustomizedMessageHandler extends MinimalConfiguration {

        public static final HandlerMethodReturnValueHandler CUSTOM_RETURN_VALUE_HANDLER = mock(HandlerMethodReturnValueHandler.class);
        public static final HandlerMethodArgumentResolver CUSTOM_ARGUMENT_RESOLVER = mock(HandlerMethodArgumentResolver.class);
        public static final AmazonSQSAsync CUSTOM_AMAZON_SQS = mock(AmazonSQSAsync.class, withSettings().stubOnly());
        public static final ResourceIdResolver CUSTOM_RESOURCE_ID_RESOLVER = mock(ResourceIdResolver.class);

        @Bean
        public QueueMessageHandlerFactory queueMessageHandlerFactory() {
            QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory();
            factory.setArgumentResolvers(Collections.singletonList(CUSTOM_ARGUMENT_RESOLVER));
            factory.setReturnValueHandlers(Collections.singletonList(CUSTOM_RETURN_VALUE_HANDLER));
            factory.setAmazonSqs(CUSTOM_AMAZON_SQS);
            factory.setResourceIdResolver(CUSTOM_RESOURCE_ID_RESOLVER);

            return factory;
        }

    }

    @EnableSqs
    @Configuration
    public static class ConfigurationWithCustomContainerFactory {

        public static final AmazonSQSAsync AMAZON_SQS = mock(AmazonSQSAsync.class, withSettings().stubOnly());
        public static final boolean AUTO_STARTUP = true;
        public static final int MAX_NUMBER_OF_MESSAGES = 1456;
        public static final QueueMessageHandler MESSAGE_HANDLER;
        public static final ResourceIdResolver RESOURCE_ID_RESOLVER = mock(ResourceIdResolver.class);
        public static final SimpleAsyncTaskExecutor TASK_EXECUTOR = new SimpleAsyncTaskExecutor();
        public static final int VISIBILITY_TIMEOUT = 1789;
        public static final int WAIT_TIME_OUT = 12;
        public static final DestinationResolver<String> DESTINATION_RESOLVER = new DynamicQueueUrlDestinationResolver(mock(AmazonSQSAsync.class, withSettings().stubOnly()));
        public static final long BACK_OFF_TIME = 5000;

        static {
            QueueMessageHandler queueMessageHandler = new QueueMessageHandler();
            queueMessageHandler.setApplicationContext(new StaticApplicationContext());
            MESSAGE_HANDLER = queueMessageHandler;
        }

        @Bean
        public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory() {
            SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
            factory.setAmazonSqs(amazonSQS());
            factory.setAutoStartup(AUTO_STARTUP);
            factory.setMaxNumberOfMessages(MAX_NUMBER_OF_MESSAGES);
            factory.setQueueMessageHandler(MESSAGE_HANDLER);
            factory.setResourceIdResolver(RESOURCE_ID_RESOLVER);
            factory.setTaskExecutor(TASK_EXECUTOR);
            factory.setVisibilityTimeout(VISIBILITY_TIMEOUT);
            factory.setWaitTimeOut(WAIT_TIME_OUT);
            factory.setDestinationResolver(DESTINATION_RESOLVER);
            factory.setBackOffTime(BACK_OFF_TIME);

            return factory;
        }

        @Bean
        public AmazonSQSAsync amazonSQS() {
            return AMAZON_SQS;
        }

    }

    @EnableSqs
    @Configuration
    public static class ConfigurationWithCustomSendToMessageTemplate {

        public static final DestinationResolvingMessageSendingOperations<?> SEND_TO_MESSAGE_TEMPLATE = mock(DestinationResolvingMessageSendingOperations.class);

        @Bean
        public QueueMessageHandlerFactory queueMessageHandlerFactory() {
            QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory();
            factory.setSendToMessagingTemplate(SEND_TO_MESSAGE_TEMPLATE);

            return factory;
        }

    }

    @EnableSqs
    @Configuration
    public static class ConfigurationWithMissingAwsCredentials {

    }

    @EnableSqs
    @EnableContextRegion(region = "eu-west-1")
    @Configuration
    public static class ConfigurationWithRegionProvider {

    }

}
