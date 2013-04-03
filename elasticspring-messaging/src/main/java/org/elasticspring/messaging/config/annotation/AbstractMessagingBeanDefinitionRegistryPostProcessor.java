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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public abstract class AbstractMessagingBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// Do nothing, as we interested in modify the bean registry to register new bean definitions
	}

	protected MetadataReaderFactory getMetadataReaderFactory() {
		return this.metadataReaderFactory;
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		for (String beanDefinitionName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
			MetadataReader reader;
			try {
				reader = getMetadataReaderFactory().getMetadataReader(beanDefinition.getBeanClassName());
			} catch (IOException e) {
				throw new RuntimeException("Error reading class metadata for class:'" +
						beanDefinition.getBeanClassName() + "' and bean definition name:'" + beanDefinitionName + "'");
			}

			AnnotationMetadata annotationMetadata = reader.getAnnotationMetadata();
			processBeanDefinition(registry, beanDefinitionName, annotationMetadata);
		}
	}

	protected abstract void processBeanDefinition(BeanDefinitionRegistry registry, String beanDefinitionName, AnnotationMetadata annotationMetadata);
}
