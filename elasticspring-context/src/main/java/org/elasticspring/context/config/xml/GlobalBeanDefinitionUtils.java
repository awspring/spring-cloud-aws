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

package org.elasticspring.context.config.xml;

import com.amazonaws.regions.Regions;
import org.elasticspring.core.env.ResourceIdResolver;
import org.elasticspring.core.env.StackResourceRegistryDetectingResourceIdResolver;
import org.elasticspring.core.region.RegionProvider;
import org.elasticspring.core.region.StaticRegionProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Provides utility methods for registering globally used bean definitions.
 *
 * @author Christian Stettler
 * @author Alain Sahli
 * @author Agim Emruli
 */
public final class GlobalBeanDefinitionUtils {

	public static final String RESOURCE_ID_RESOLVER_BEAN_NAME = ResourceIdResolver.class.getName() + ".BEAN_NAME";
	public static final String REGION_PROVIDER_BEAN_NAME = RegionProvider.class.getName() + ".BEAN_NAME";

	private GlobalBeanDefinitionUtils() {
		// Avoid instantiation
	}

	/**
	 * Returns the name of the resource id resolver bean. This method is provided as utility method for bean definition
	 * parsers that create bean definitions with a dependency to the global resource id resolver bean. If the resource
	 * id resolver bean of type {@link ResourceIdResolver} has not yet been registered with the provided bean definition
	 * registry, it is automatically registered.
	 *
	 * @param registry
	 * 		the bean definition registry to check for an existing resource id resolver bean definition, and to register the
	 * 		resource id resolver bean definition with, if needed
	 * @return the bean name of the resource id resolver bean
	 */
	@SuppressWarnings("SameReturnValue")
	public static String retrieveResourceIdResolverBeanName(BeanDefinitionRegistry registry) {
		registerResourceIdResolverBeanIfNeeded(registry);

		return RESOURCE_ID_RESOLVER_BEAN_NAME;
	}

	static void registerResourceIdResolverBeanIfNeeded(BeanDefinitionRegistry registry) {
		if (!(registry.containsBeanDefinition(RESOURCE_ID_RESOLVER_BEAN_NAME))) {
			registry.registerBeanDefinition(RESOURCE_ID_RESOLVER_BEAN_NAME, buildResourceIdResolverBeanDefinition());
		}
	}

	private static BeanDefinition buildResourceIdResolverBeanDefinition() {
		return BeanDefinitionBuilder.genericBeanDefinition(StackResourceRegistryDetectingResourceIdResolver.class).getBeanDefinition();
	}

	public static String retrieveRegionProviderBeanName(BeanDefinitionRegistry registry) {
		registerRegionProviderBeanIfNeeded(registry);
		return REGION_PROVIDER_BEAN_NAME;
	}

	public static void registerOrReplaceRegionProvider(BeanDefinitionRegistry registry, String customGlobalRegionProvider) {
		if (registry.containsBeanDefinition(REGION_PROVIDER_BEAN_NAME)) {
			registry.removeBeanDefinition(REGION_PROVIDER_BEAN_NAME);
		}
		registry.registerAlias(customGlobalRegionProvider, REGION_PROVIDER_BEAN_NAME);
	}

	private static void registerRegionProviderBeanIfNeeded(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(REGION_PROVIDER_BEAN_NAME)) {
			registry.registerBeanDefinition(REGION_PROVIDER_BEAN_NAME, buildDefaultRegionProviderBeanDefinition().getBeanDefinition());
		}
	}

	private static BeanDefinitionBuilder buildDefaultRegionProviderBeanDefinition() {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(StaticRegionProvider.class);
		builder.addConstructorArgValue(Regions.DEFAULT_REGION);
		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		return builder;
	}
}