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
 * Abstract base class that provides support to inspect annotated beans and process them in the subclass. This class
 * uses a {@link MetadataReader} to read the meta data (typically done with ASM) from the classes and provide them to
 * the sub classes without actually loading the class.
 * <p>
 * Subclasses can inspect the {@link AnnotationMetadata} and process the bean or create a new bean definition at all.
 * </p>
 *
 * @author Agim Emruli
 * @see TopicListenerBeanDefinitionRegistryPostProcessor
 * @since 1.0
 */
public abstract class AbstractMessagingBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	/**
	 * {@link MetadataReaderFactory} use by this instance.
	 */
	private final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// Do nothing, as we interested in modify the bean registry to register new bean definitions
	}

	/**
	 * Provides the {@link MetadataReaderFactory} to sub classes if needed by them.
	 *
	 * @return - a {@link CachingMetadataReaderFactory} instantiated and maintained by this class.
	 */
	protected MetadataReaderFactory getMetadataReaderFactory() {
		return this.metadataReaderFactory;
	}

	/**
	 * Iterates through every bean inside the {@code BeanDefinitionRegistry} and parses the {@link AnnotationMetadata} for
	 * every bean. Regardless if there are any {@link AnnotationMetadata} available, this class will call the subclasses
	 * through {@link #processBeanDefinition(org.springframework.beans.factory.support.BeanDefinitionRegistry, String,
	 * org.springframework.core.type.AnnotationMetadata)}
	 *
	 * @param registry
	 * 		- the registry containing all bean definition which will be processes
	 */
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
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

	/**
	 * Template method that must be implemented by subclasses. This method will be called on each bean with or without
	 * annotation metadata. Subclasses may inspect the {@link AnnotationMetadata} and register another bean inside the
	 * {@link BeanDefinitionRegistry}
	 *
	 * @param registry
	 * 		- the bean definition registry which may be used to inspect bean definition or register new ones
	 * @param beanDefinitionName
	 * 		-  the name of bean definition for which the {@link AnnotationMetadata} are provided
	 * @param annotationMetadata
	 * 		-  the annotation metadata which may contain annotation information
	 */
	protected abstract void processBeanDefinition(BeanDefinitionRegistry registry, String beanDefinitionName, AnnotationMetadata annotationMetadata);
}
