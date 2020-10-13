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

package org.springframework.cloud.aws.context.support.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.Ordered;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.ResourceLoader;

/**
 * Configurer that register the {@link SimpleStorageProtocolResolver} to the resource
 * resolver to allow resolving s3 based resources in Spring.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @since 1.0
 */
public class SimpleStorageProtocolResolverConfigurer implements BeanFactoryPostProcessor, Ordered, ResourceLoaderAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleStorageProtocolResolverConfigurer.class);

	private final ProtocolResolver protocolResolver;

	private ResourceLoader resourceLoader;

	public SimpleStorageProtocolResolverConfigurer(SimpleStorageProtocolResolver simpleStorageProtocolResolver) {
		this.protocolResolver = simpleStorageProtocolResolver;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (DefaultResourceLoader.class.isAssignableFrom(resourceLoader.getClass())) {
			((DefaultResourceLoader) resourceLoader).addProtocolResolver(this.protocolResolver);
		}
		else {
			LOGGER.warn("The provided delegate resource loader is not an implementation "
					+ "of DefaultResourceLoader. Custom Protocol using s3:// prefix will not be enabled.");
		}
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

}
