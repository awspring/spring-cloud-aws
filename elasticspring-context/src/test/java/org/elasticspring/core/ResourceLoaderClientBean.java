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

package org.elasticspring.core;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

/**
 *
 */
public class ResourceLoaderClientBean implements ResourceLoaderAware,ApplicationContextAware  {

	private ResourceLoader resourceLoaderAwareResourceLoader;
	private ApplicationContext applicationContext;

	@Autowired
	private ResourceLoader annotationResourceLoader;

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoaderAwareResourceLoader = resourceLoader;
	}

	public ResourceLoader getResourceLoaderAwareResourceLoader() {
		return this.resourceLoaderAwareResourceLoader;
	}

	public ResourceLoader getAnnotationResourceLoader() {
		return this.annotationResourceLoader;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
}
