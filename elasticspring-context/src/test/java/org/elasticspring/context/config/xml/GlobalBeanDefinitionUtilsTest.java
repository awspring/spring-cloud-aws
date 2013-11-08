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
	public void retrieveResourceIdResolverBeanName_resourceIdResolverBeanNotYetRegistered_resourceIdResolverBeanIsRegistered() {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

		// Act
		GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(registry);

		// Assert
		assertThat(registry.getBeanNamesForType(ResourceIdResolver.class).length, is(1));
	}

	@Test
	public void retrieveResourceIdResolverBeanName_resourceIdResolverBeanNotYetRegistered_resourceIdResolverBeanIsRegisteredUnderInternalName() {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

		// Act
		GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(registry);

		// Assert
		assertThat(registry.getBeanDefinition(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME), is(not(nullValue())));
	}

	@Test
	public void retrieveResourceIdResolverBeanName_resourceIdResolverBeanNotYetRegistered_returnsInternalBeanName() {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

		// Act
		String resourceIdResolverBeanName = GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(registry);

		// Assert
		assertThat(resourceIdResolverBeanName, is(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME));
	}

	@Test
	public void retrieveResourceIdResolverBeanName_resourceIdResolverBeanAlreadyRegistered_resourceIdResolverBeanIsNotAgainRegistered() {
		// Arrange
		BeanDefinition resourceIdResolverBeanDefinition = new GenericBeanDefinition();

		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		registry.registerBeanDefinition(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME, resourceIdResolverBeanDefinition);

		// Act
		GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(registry);

		// Assert
		assertThat(registry.getBeanDefinition(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME), is(resourceIdResolverBeanDefinition));
	}

	@Test
	public void retrieveResourceIdResolverBeanName_resourceIdResolverBeanAlreadyRegistered_returnsInternalBeanName() {
		// Arrange
		BeanDefinition resourceIdResolverBeanDefinition = new GenericBeanDefinition();

		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		registry.registerBeanDefinition(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME, resourceIdResolverBeanDefinition);

		// Act
		String resourceIdResolverBeanName = GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(registry);

		// Assert
		assertThat(resourceIdResolverBeanName, is(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME));
	}

}
