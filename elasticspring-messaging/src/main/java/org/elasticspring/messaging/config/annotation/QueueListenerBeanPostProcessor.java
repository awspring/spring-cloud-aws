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

package org.elasticspring.messaging.config.annotation;

import org.elasticspring.messaging.listener.MessageListenerAdapter;
import org.elasticspring.messaging.listener.SimpleMessageListenerContainer;
import org.elasticspring.messaging.support.converter.StringMessageConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueListenerBeanPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private static final String ANNOTATION_TYPE = QueueListener.class.getName();
	private static final String DESTINATION_ATTRIBUTE_NAME = "queueName";
	private static final String VALUE_ATTRIBUTE_NAME = "value";
	private final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	private Map<String, Object> messageListenerContainerConfiguration = new HashMap<String, Object>();

	public void setMessageListenerContainerConfiguration(Map<String, Object> messageListenerContainerConfiguration) {
		this.messageListenerContainerConfiguration = messageListenerContainerConfiguration;
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		for (String beanDefinitionName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
			MetadataReader reader;
			try {
				reader = this.metadataReaderFactory.getMetadataReader(beanDefinition.getBeanClassName());
			} catch (IOException e) {
				throw new RuntimeException("Error reading class metadata for class:'" +
						beanDefinition.getBeanClassName() + "' and bean definition name:'" + beanDefinitionName + "'");
			}

			AnnotationMetadata annotationMetadata = reader.getAnnotationMetadata();
			Set<MethodMetadata> methods = annotationMetadata.getAnnotatedMethods(ANNOTATION_TYPE);
			for (MethodMetadata method : methods) {
				BeanDefinition definition = getContainerBeanDefinition(beanDefinitionName, method);
				String beanName = BeanDefinitionReaderUtils.generateBeanName(definition, registry);
				registry.registerBeanDefinition(beanName, definition);
			}
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// Do nothing, as we interested in modify the bean registry to register new bean definitions
	}

	private BeanDefinition getContainerBeanDefinition(String beanName, MethodMetadata methodMetadata) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SimpleMessageListenerContainer.class);

		for (Map.Entry<String, Object> entry : this.messageListenerContainerConfiguration.entrySet()) {
			builder.addPropertyValue(entry.getKey(), entry.getValue());
		}

		builder.addPropertyValue("destinationName", getDestinationName(methodMetadata));
		builder.addPropertyValue("messageListener", getMessageListenerBeanDefinition(beanName, methodMetadata));
		return builder.getBeanDefinition();
	}

	private BeanDefinition getMessageListenerBeanDefinition(String target, MethodMetadata methodMetadata) {
		BeanDefinitionBuilder listenerBuilder = BeanDefinitionBuilder.rootBeanDefinition(MessageListenerAdapter.class);

		//TODO: Add support for custom message converter
		listenerBuilder.addConstructorArgValue(BeanDefinitionBuilder.rootBeanDefinition(StringMessageConverter.class).getBeanDefinition());
		listenerBuilder.addConstructorArgReference(target);
		listenerBuilder.addConstructorArgValue(methodMetadata.getMethodName());
		return listenerBuilder.getBeanDefinition();
	}


	private static String getDestinationName(MethodMetadata methodMetadata) {
		if (StringUtils.hasText(getNullSafeAnnotationAttribute(methodMetadata, VALUE_ATTRIBUTE_NAME)) &&
				StringUtils.hasText(getNullSafeAnnotationAttribute(methodMetadata, DESTINATION_ATTRIBUTE_NAME))) {
			throw new IllegalStateException("The annotation " + ANNOTATION_TYPE + " on type:'" + methodMetadata.getDeclaringClassName() + "' and method:'"
					+ methodMetadata.getMethodName() + "must contain either " + VALUE_ATTRIBUTE_NAME + " or " + DESTINATION_ATTRIBUTE_NAME + " attribute but not both!");
		}

		if (StringUtils.hasText(getNullSafeAnnotationAttribute(methodMetadata, VALUE_ATTRIBUTE_NAME))) {
			return getNullSafeAnnotationAttribute(methodMetadata, VALUE_ATTRIBUTE_NAME);
		}

		if (StringUtils.hasText(getNullSafeAnnotationAttribute(methodMetadata, DESTINATION_ATTRIBUTE_NAME))) {
			return getNullSafeAnnotationAttribute(methodMetadata, DESTINATION_ATTRIBUTE_NAME);
		}

		throw new IllegalStateException("The annotation " + ANNOTATION_TYPE + " on type:'" + methodMetadata.getDeclaringClassName() + "' and method:'"
				+ methodMetadata.getMethodName() + "' must contain either " + VALUE_ATTRIBUTE_NAME + " or " + DESTINATION_ATTRIBUTE_NAME + " attribute!");
	}

	private static String getNullSafeAnnotationAttribute(MethodMetadata methodMetadata, String attributeName) {
		if (methodMetadata.getAnnotationAttributes(ANNOTATION_TYPE).containsKey(attributeName)) {
			return methodMetadata.getAnnotationAttributes(ANNOTATION_TYPE).get(attributeName).toString();
		} else {
			return null;
		}
	}
}