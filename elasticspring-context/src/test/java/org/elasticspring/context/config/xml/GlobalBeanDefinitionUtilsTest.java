package org.elasticspring.context.config.xml;

import org.elasticspring.core.env.ResourceIdResolver;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class GlobalBeanDefinitionUtilsTest {

	@Test
	public void configureResourceIdResolver_resourceIdResolverBeanNotYetRegistered_resourceIdResolverBeanIsRegistered() {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

		// Act
		GlobalBeanDefinitionUtils.configureResourceIdResolver(registry);

		// Assert
		assertThat(registry.getBeanNamesForType(ResourceIdResolver.class).length, is(1));
	}

	@Test
	public void configureResourceIdResolver_resourceIdResolverBeanNotYetRegistered_resourceIdResolverBeanIsRegisteredUnderInternalName() {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

		// Act
		GlobalBeanDefinitionUtils.configureResourceIdResolver(registry);

		// Assert
		assertThat(registry.getBeanDefinition(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME), is(not(nullValue())));
	}

	@Test
	public void configureResourceIdResolver_resourceIdResolverBeanAlreadyRegistered_resourceIdResolverBeanIsNotAgainRegistered() {
		// Arrange
		BeanDefinition resourceIdResolverBeanDefinition = new GenericBeanDefinition();

		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		registry.registerBeanDefinition(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME, resourceIdResolverBeanDefinition);

		// Act
		GlobalBeanDefinitionUtils.configureResourceIdResolver(registry);

		// Assert
		assertThat(registry.getBeanDefinition(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME), is(resourceIdResolverBeanDefinition));
	}

}
