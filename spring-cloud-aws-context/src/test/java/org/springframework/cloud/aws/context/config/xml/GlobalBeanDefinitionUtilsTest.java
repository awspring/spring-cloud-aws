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

import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalBeanDefinitionUtilsTest {

	// @checkstyle:off
	@Test
	public void retrieveResourceIdResolverBeanName_resourceIdResolverBeanNotYetRegistered_resourceIdResolverBeanIsRegistered() {
		// @checkstyle:on
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

		// Act
		GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(registry);

		// Assert
		assertThat(registry.getBeanNamesForType(ResourceIdResolver.class).length)
				.isEqualTo(1);
	}

	// @checkstyle:off
	@Test
	public void retrieveResourceIdResolverBeanName_resourceIdResolverBeanNotYetRegistered_resourceIdResolverBeanIsRegisteredUnderInternalName() {
		// @checkstyle:on
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

		// Act
		GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(registry);

		// Assert
		assertThat(registry.getBeanDefinition(
				GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME)).isNotNull();
	}

	@Test
	public void retrieveResourceIdResolverBeanName_resourceIdResolverBeanNotYetRegistered_returnsInternalBeanName() {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

		// Act
		String resourceIdResolverBeanName = GlobalBeanDefinitionUtils
				.retrieveResourceIdResolverBeanName(registry);

		// Assert
		assertThat(resourceIdResolverBeanName)
				.isEqualTo(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME);
	}

	// @checkstyle:off
	@Test
	public void retrieveResourceIdResolverBeanName_resourceIdResolverBeanAlreadyRegistered_resourceIdResolverBeanIsNotAgainRegistered() {
		// @checkstyle:on
		// Arrange
		BeanDefinition resourceIdResolverBeanDefinition = new GenericBeanDefinition();

		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		registry.registerBeanDefinition(
				GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME,
				resourceIdResolverBeanDefinition);

		// Act
		GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(registry);

		// Assert
		assertThat(registry.getBeanDefinition(
				GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME))
						.isEqualTo(resourceIdResolverBeanDefinition);
	}

	@Test
	public void retrieveResourceIdResolverBeanName_resourceIdResolverBeanAlreadyRegistered_returnsInternalBeanName() {
		// Arrange
		BeanDefinition resourceIdResolverBeanDefinition = new GenericBeanDefinition();

		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		registry.registerBeanDefinition(
				GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME,
				resourceIdResolverBeanDefinition);

		// Act
		String resourceIdResolverBeanName = GlobalBeanDefinitionUtils
				.retrieveResourceIdResolverBeanName(registry);

		// Assert
		assertThat(resourceIdResolverBeanName)
				.isEqualTo(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME);
	}

}
