/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.s3;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.io.ResourceLoader;

/**
 *
 */
public class SimpleStoreResourceLoaderBeanFactoryPostProcessor implements BeanFactoryPostProcessor{

	private final ResourceLoader resourceLoader;

	public SimpleStoreResourceLoaderBeanFactoryPostProcessor(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		beanFactory.registerResolvableDependency(ResourceLoader.class, this.resourceLoader);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this.resourceLoader);
	}
}
