package org.elasticspring.context.config.xml;

import org.elasticspring.core.env.ResourceIdResolver;
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
	 * Configures and registers a bean of type {@link ResourceIdResolver} with the given bean definition registry. The
	 * bean is only registered once, and is registered under the name {@link GlobalBeanDefinitionUtils#RESOURCE_ID_RESOLVER_BEAN_NAME}.
	 * Subsequent calls to this method have no effect.
	 *
	 * @param registry
	 * 		the bean definition registry to register the resource id resolver with
	 */
	public static void configureResourceIdResolver(BeanDefinitionRegistry registry) {
		if (!(registry.containsBeanDefinition(RESOURCE_ID_RESOLVER_BEAN_NAME))) {
			registry.registerBeanDefinition(RESOURCE_ID_RESOLVER_BEAN_NAME, buildResourceIdResolverBeanDefinition());
		}
	}

	private static BeanDefinition buildResourceIdResolverBeanDefinition() {
		return BeanDefinitionBuilder.genericBeanDefinition(ResourceIdResolver.class).getBeanDefinition();
	}

}
