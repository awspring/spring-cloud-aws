/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.kinesis.config;

import io.awspring.cloud.kinesis.annotation.KclListenerAnnotationBeanPostProcessor;
import io.awspring.cloud.kinesis.listener.DefaultListenerContainerRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
public class KclBootstrapConfiguration implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(KclBeanNames.KCL_LISTENER_ANNOTATION_BEAN_POST_PROCESSOR_BEAN_NAME)) {
			registry.registerBeanDefinition(KclBeanNames.KCL_LISTENER_ANNOTATION_BEAN_POST_PROCESSOR_BEAN_NAME,
					new RootBeanDefinition(KclListenerAnnotationBeanPostProcessor.class));
		}
		if (!registry.containsBeanDefinition(KclBeanNames.CONTAINER_REGISTRY_BEAN_NAME)) {
			registry.registerBeanDefinition(KclBeanNames.CONTAINER_REGISTRY_BEAN_NAME,
					new RootBeanDefinition(DefaultListenerContainerRegistry.class));
		}
	}

}
