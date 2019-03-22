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

package org.springframework.cloud.aws.context.config.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.env.StackResourceRegistryDetectingResourceIdResolver;

/**
 * Provides utility methods for registering globally used bean definitions.
 *
 * @author Christian Stettler
 * @author Alain Sahli
 * @author Agim Emruli
 */
public final class GlobalBeanDefinitionUtils {

	/**
	 * Bean name of the resource id resolver.
	 */
	public static final String RESOURCE_ID_RESOLVER_BEAN_NAME = ResourceIdResolver.class
			.getName() + ".BEAN_NAME";

	private GlobalBeanDefinitionUtils() {
		// Avoid instantiation
	}

	/**
	 * Returns the name of the resource id resolver bean. This method is provided as
	 * utility method for bean definition parsers that create bean definitions with a
	 * dependency to the global resource id resolver bean. If the resource id resolver
	 * bean of type {@link ResourceIdResolver} has not yet been registered with the
	 * provided bean definition registry, it is automatically registered.
	 * @param registry the bean definition registry to check for an existing resource id
	 * resolver bean definition, and to register the resource id resolver bean definition
	 * with, if needed
	 * @return the bean name of the resource id resolver bean
	 */
	@SuppressWarnings("SameReturnValue")
	public static String retrieveResourceIdResolverBeanName(
			BeanDefinitionRegistry registry) {
		registerResourceIdResolverBeanIfNeeded(registry);

		return RESOURCE_ID_RESOLVER_BEAN_NAME;
	}

	static void registerResourceIdResolverBeanIfNeeded(BeanDefinitionRegistry registry) {
		if (!(registry.containsBeanDefinition(RESOURCE_ID_RESOLVER_BEAN_NAME))) {
			registry.registerBeanDefinition(RESOURCE_ID_RESOLVER_BEAN_NAME,
					buildResourceIdResolverBeanDefinition());
		}
	}

	private static BeanDefinition buildResourceIdResolverBeanDefinition() {
		return BeanDefinitionBuilder
				.genericBeanDefinition(
						StackResourceRegistryDetectingResourceIdResolver.class)
				.getBeanDefinition();
	}

}
