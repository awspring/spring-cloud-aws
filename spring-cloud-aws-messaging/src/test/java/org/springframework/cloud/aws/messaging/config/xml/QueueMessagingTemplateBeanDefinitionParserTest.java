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

package org.springframework.cloud.aws.messaging.config.xml;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.support.converter.ObjectMessageConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Alain Sahli
 */
public class QueueMessagingTemplateBeanDefinitionParserTest {

    @Test
    public void parseInternal_withMinimalConfig_shouldProduceAQueueMessagingTemplateWithDefaults() throws Exception {
        //Arrange
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

        //Act
        reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-minimal.xml", getClass()));

        //Assert
        QueueMessagingTemplate queueMessagingTemplate = registry.getBean(QueueMessagingTemplate.class);
        assertSame(registry.getBean(AmazonSQSAsync.class), ReflectionTestUtils.getField(queueMessagingTemplate, "amazonSqs"));
        Object cachingDestinationResolverProxy = ReflectionTestUtils.getField(queueMessagingTemplate, "destinationResolver");
        Object targetDestinationResolver = ReflectionTestUtils.getField(cachingDestinationResolverProxy, "targetDestinationResolver");
        assertEquals(registry.getBean(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME), ReflectionTestUtils.getField(targetDestinationResolver, "resourceIdResolver"));
        assertTrue(CompositeMessageConverter.class.isInstance(queueMessagingTemplate.getMessageConverter()));

        assertTrue(CompositeMessageConverter.class.isInstance(queueMessagingTemplate.getMessageConverter()));
        @SuppressWarnings("unchecked")
        List<MessageConverter> messageConverters = (List<MessageConverter>) ReflectionTestUtils.getField(queueMessagingTemplate.getMessageConverter(), "converters");
        assertEquals(2, messageConverters.size());
        assertTrue(StringMessageConverter.class.isInstance(messageConverters.get(0)));
        assertTrue(MappingJackson2MessageConverter.class.isInstance(messageConverters.get(1)));

        StringMessageConverter stringMessageConverter = (StringMessageConverter) messageConverters.get(0);
        assertSame(String.class, stringMessageConverter.getSerializedPayloadClass());
        assertEquals(false, ReflectionTestUtils.getField(stringMessageConverter, "strictContentTypeMatch"));

        MappingJackson2MessageConverter jackson2MessageConverter = (MappingJackson2MessageConverter) messageConverters.get(1);
        assertSame(String.class, jackson2MessageConverter.getSerializedPayloadClass());
        assertEquals(false, ReflectionTestUtils.getField(jackson2MessageConverter, "strictContentTypeMatch"));
    }

    @Test
    public void parseInternal_withCustomAmazonSqsClient_shouldPassItAsConstructorArg() throws Exception {
        //Arrange
        SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

        //Act
        reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-amazon-sqs.xml", getClass()));

        //Assert
        BeanDefinition queueMessagingTemplateBeanDefinition = registry.getBeanDefinition("queueMessagingTemplate");
        assertEquals("myClient", ((RuntimeBeanReference) queueMessagingTemplateBeanDefinition.getConstructorArgumentValues()
                .getArgumentValue(0, RuntimeBeanReference.class).getValue()).getBeanName());
    }

    @Test
    public void parseInternal_withCustomConverter_mustBeSetOnTemplate() throws Exception {
        //Arrange
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

        //Act
        reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-converter.xml", getClass()));

        //Assert
        QueueMessagingTemplate queueMessagingTemplateBeanDefinition = registry.getBean(QueueMessagingTemplate.class);
        MessageConverter messageConverter = queueMessagingTemplateBeanDefinition.getMessageConverter();
        assertTrue(CompositeMessageConverter.class.isInstance(messageConverter));
        CompositeMessageConverter compositeMessageConverter = (CompositeMessageConverter) messageConverter;
        assertEquals(2, compositeMessageConverter.getConverters().size());
        assertTrue(ObjectMessageConverter.class.isInstance(compositeMessageConverter.getConverters().get(1)));
    }

    @Test
    public void parseInternal_withDefaultDestination_mustBeSetOnTemplate() throws Exception {
        //Arrange
        SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

        //Act
        reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-with-default-destination.xml", getClass()));

        //Assert
        BeanDefinition queueMessagingTemplateBeanDefinition = registry.getBeanDefinition("queueMessagingTemplate");
        assertEquals("myDefaultDestination", queueMessagingTemplateBeanDefinition.getPropertyValues()
                .getPropertyValue("defaultDestinationName").getValue());
    }

    @Test
    public void parseInternal_withCustomRegion_shouldConfigureDefaultClientWithCustomRegion() throws Exception {
        //Arrange
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

        //Act
        reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-region.xml", getClass()));

        //Assert
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

        //Assert
        AmazonSQSBufferedAsyncClient amazonSqs = registry.getBean(AmazonSQSBufferedAsyncClient.class);
        Object amazonSqsAsyncClient = ReflectionTestUtils.getField(amazonSqs, "realSQS");
        assertEquals("https://" + Region.getRegion(Regions.AP_SOUTHEAST_2).getServiceEndpoint("sqs"), ReflectionTestUtils.getField(amazonSqsAsyncClient, "endpoint").toString());
    }

    @Test
    public void parseInternal_withMultipleMessagingTemplatesDefined_shouldConfigureOnlyOneSqsClientAndDecorateOnlyOnce() throws Exception {
        //Arrange
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

        //Act
        reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-multiple-templates.xml", getClass()));

        //Assert
        AmazonSQSBufferedAsyncClient amazonSqs = registry.getBean(AmazonSQSBufferedAsyncClient.class);
        assertTrue(ReflectionTestUtils.getField(amazonSqs, "realSQS") instanceof AmazonSQSAsyncClient);
    }
}
