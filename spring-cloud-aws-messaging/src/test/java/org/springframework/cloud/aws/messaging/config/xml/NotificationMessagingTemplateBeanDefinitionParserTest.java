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
import com.amazonaws.services.sns.AmazonSNSClient;
import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alain Sahli
 */
public class NotificationMessagingTemplateBeanDefinitionParserTest {

	@Test
	public void parseInternal_withMinimalConfig_shouldCreateDefaultTemplate()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-minimal.xml", getClass()));

		// Assert
		NotificationMessagingTemplate notificationMessagingTemplate = registry
				.getBean(NotificationMessagingTemplate.class);
		assertThat(
				ReflectionTestUtils.getField(notificationMessagingTemplate, "amazonSns"))
						.isSameAs(registry.getBean(AmazonSNSClient.class));

		Object cachingDestinationResolverProxy = ReflectionTestUtils
				.getField(notificationMessagingTemplate, "destinationResolver");
		Object targetDestinationResolver = ReflectionTestUtils
				.getField(cachingDestinationResolverProxy, "targetDestinationResolver");
		assertThat(ReflectionTestUtils.getField(targetDestinationResolver,
				"resourceIdResolver")).isEqualTo(registry.getBean(
						GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME));

		assertThat(CompositeMessageConverter.class
				.isInstance(notificationMessagingTemplate.getMessageConverter()))
						.isTrue();
		@SuppressWarnings("unchecked")
		List<MessageConverter> messageConverters = (List<MessageConverter>) ReflectionTestUtils
				.getField(notificationMessagingTemplate.getMessageConverter(),
						"converters");
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
	public void parseInternal_withCustomAmazonSnsClient_shouldPassItAsConstructorArg()
			throws Exception {
		// Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-amazon-sns.xml", getClass()));

		// Assert
		BeanDefinition notificationMessagingTemplateBeanDefinition = registry
				.getBeanDefinition("notificationMessagingTemplate");
		assertThat(((RuntimeBeanReference) notificationMessagingTemplateBeanDefinition
				.getConstructorArgumentValues()
				.getArgumentValue(0, RuntimeBeanReference.class).getValue())
						.getBeanName()).isEqualTo("mySnsClient");
	}

	@Test
	public void parseInternal_withDefaultDestination_mustBeSetOnTemplate()
			throws Exception {
		// Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-with-default-destination.xml",
				getClass()));

		// Assert
		BeanDefinition notificationMessagingTemplateBeanDefinition = registry
				.getBeanDefinition("notificationMessagingTemplate");
		assertThat(notificationMessagingTemplateBeanDefinition.getPropertyValues()
				.getPropertyValue("defaultDestinationName").getValue())
						.isEqualTo("myDefaultDestination");
	}

	@Test
	public void parseInternal_withCustomRegion_shouldConfigureDefaultClientWithCustomRegion()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-region.xml", getClass()));

		// Assert
		AmazonSNSClient amazonSns = registry.getBean(AmazonSNSClient.class);
		assertThat(ReflectionTestUtils.getField(amazonSns, "endpoint").toString())
				.isEqualTo("https://"
						+ Region.getRegion(Regions.EU_WEST_1).getServiceEndpoint("sns"));
	}

	@Test
	public void parseInternal_withCustomRegionProvider_shouldConfigureDefaultClientWithCustomRegionReturnedByProvider()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-region-provider.xml", getClass()));

		// Assert
		AmazonSNSClient amazonSns = registry.getBean(AmazonSNSClient.class);
		assertThat(ReflectionTestUtils.getField(amazonSns, "endpoint").toString())
				.isEqualTo("https://"
						+ Region.getRegion(Regions.CN_NORTH_1).getServiceEndpoint("sns"));
	}

}
