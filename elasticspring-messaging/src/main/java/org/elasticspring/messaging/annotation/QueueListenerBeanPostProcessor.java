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

package org.elasticspring.messaging.annotation;

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

import java.io.IOException;
import java.util.Set;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueListenerBeanPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		for (String beanDefinitionName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
			try {
				MetadataReader reader = this.metadataReaderFactory.getMetadataReader(beanDefinition.getBeanClassName());
				AnnotationMetadata annotationMetadata = reader.getAnnotationMetadata();
				Set<MethodMetadata> methods = annotationMetadata.getAnnotatedMethods(QueueListener.class.getName());
				for (MethodMetadata method : methods) {
					BeanDefinition definition = getContainerBeanDefinition(beanDefinitionName, method);
					String beanName = BeanDefinitionReaderUtils.generateBeanName(definition, registry);
					registry.registerBeanDefinition(beanName, definition);
				}
			} catch (IOException e) {
				throw new RuntimeException("Error reading class metadata");
			}
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}

	private BeanDefinition getContainerBeanDefinition(String beanName, MethodMetadata methodMetadata) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SimpleMessageListenerContainer.class);
		builder.addPropertyReference("amazonSQS", "amazonSQS");
		builder.addPropertyValue("destinationName", methodMetadata.getAnnotationAttributes(QueueListener.class.getName()).get("value"));

		BeanDefinitionBuilder listenerBuilder = BeanDefinitionBuilder.rootBeanDefinition(MessageListenerAdapter.class);
		listenerBuilder.addConstructorArgValue(BeanDefinitionBuilder.rootBeanDefinition(StringMessageConverter.class).getBeanDefinition());
		listenerBuilder.addConstructorArgReference(beanName);
		listenerBuilder.addConstructorArgValue(methodMetadata.getMethodName());

		builder.addPropertyValue("messageListener", listenerBuilder.getBeanDefinition());
		return builder.getBeanDefinition();
	}
}
