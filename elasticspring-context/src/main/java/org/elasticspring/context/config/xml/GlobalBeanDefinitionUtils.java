package org.elasticspring.context.config.xml;

import org.elasticspring.core.env.ResourceIdResolver;
import org.elasticspring.core.env.StackResourceRegistryDetectingResourceIdResolver;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Provides utility methods for registering globally used bean definitions.
 *
 * @author Christian Stettler
 */
public class GlobalBeanDefinitionUtils {

	public static final String RESOURCE_ID_RESOLVER_BEAN_NAME = ResourceIdResolver.class.getName() + ".BEAN_NAME";

	private GlobalBeanDefinitionUtils() {
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

}
