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

package org.springframework.cloud.aws.context.support.io;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ResourceLoaderTest {


	@Test
	public void testInjectionForFields() throws Exception {

		StaticApplicationContext staticApplicationContext = new StaticApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(staticApplicationContext);

		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		Resource resource = mock(Resource.class);
		when(resourceLoader.getResource("s3://bucket/object")).thenReturn(resource);

		BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(ResourceLoaderBeanPostProcessor.class);
		beanDefinition.addConstructorArgValue(resourceLoader);

		staticApplicationContext.registerBeanDefinition("beanPostProcessor", beanDefinition.getBeanDefinition());
		staticApplicationContext.registerSingleton("client", FieldInjectionTarget.class);
		staticApplicationContext.refresh();

		FieldInjectionTarget fieldInjectionTarget = staticApplicationContext.getBean(FieldInjectionTarget.class);

		assertNotNull(fieldInjectionTarget.getResourceLoader());
		Resource resourceLoaderResource = fieldInjectionTarget.getResourceLoader().getResource("s3://bucket/object");
		assertSame(resource, resourceLoaderResource);
	}

	@Test
	public void testInjectionForMethods() throws Exception {

		StaticApplicationContext staticApplicationContext = new StaticApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(staticApplicationContext);

		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		Resource resource = mock(Resource.class);
		when(resourceLoader.getResource("s3://bucket/object")).thenReturn(resource);

		BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(ResourceLoaderBeanPostProcessor.class);
		beanDefinition.addConstructorArgValue(resourceLoader);

		staticApplicationContext.registerBeanDefinition("beanPostProcessor", beanDefinition.getBeanDefinition());
		staticApplicationContext.registerSingleton("client", MethodInjectionTarget.class);
		staticApplicationContext.refresh();

		MethodInjectionTarget methodInjectionTarget = staticApplicationContext.getBean(MethodInjectionTarget.class);

		assertNotNull(methodInjectionTarget.getResourceLoader());
		Resource resourceLoaderResource = methodInjectionTarget.getResourceLoader().getResource("s3://bucket/object");
		assertSame(resource, resourceLoaderResource);
	}

	@Test
	public void testInjectionForConstructor() throws Exception {

		StaticApplicationContext staticApplicationContext = new StaticApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(staticApplicationContext);

		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		Resource resource = mock(Resource.class);
		when(resourceLoader.getResource("s3://bucket/object")).thenReturn(resource);

		BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(ResourceLoaderBeanPostProcessor.class);
		beanDefinition.addConstructorArgValue(resourceLoader);

		staticApplicationContext.registerBeanDefinition("beanPostProcessor", beanDefinition.getBeanDefinition());
		staticApplicationContext.registerSingleton("client", ConstructorInjectionTarget.class);
		staticApplicationContext.refresh();

		ConstructorInjectionTarget constructorInjectionTarget = staticApplicationContext.getBean(ConstructorInjectionTarget.class);

		assertNotNull(constructorInjectionTarget.getResourceLoader());
		Resource resourceLoaderResource = constructorInjectionTarget.getResourceLoader().getResource("s3://bucket/object");
		assertSame(resource, resourceLoaderResource);
	}

	@Test
	public void testResourceLoaderAwareBean() throws Exception {
		StaticApplicationContext staticApplicationContext = new StaticApplicationContext();

		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		Resource resource = mock(Resource.class);
		when(resourceLoader.getResource("s3://bucket/object")).thenReturn(resource);

		BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(ResourceLoaderBeanPostProcessor.class);
		beanDefinition.addConstructorArgValue(resourceLoader);

		staticApplicationContext.registerBeanDefinition("beanPostProcessor", beanDefinition.getBeanDefinition());
		staticApplicationContext.registerSingleton("client", ResourceLoaderAwareBean.class);
		staticApplicationContext.refresh();

		ResourceLoaderAwareBean resourceLoaderAwareBean = staticApplicationContext.getBean(ResourceLoaderAwareBean.class);
		assertNotNull(resourceLoaderAwareBean.getResourceLoader());
		Resource resourceLoaderResource = resourceLoaderAwareBean.getResourceLoader().getResource("s3://bucket/object");
		assertSame(resource, resourceLoaderResource);
	}

	private static final class FieldInjectionTarget {

		@SuppressWarnings("SpringJavaAutowiringInspection")
		@Autowired
		private ResourceLoader resourceLoader;

		public ResourceLoader getResourceLoader() {
			return this.resourceLoader;
		}
	}

	private static final class MethodInjectionTarget {

		private ResourceLoader resourceLoader;

		public ResourceLoader getResourceLoader() {
			return this.resourceLoader;
		}

		@Autowired
		public void setResourceLoader(@SuppressWarnings("SpringJavaAutowiringInspection") ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}
	}

	private static final class ConstructorInjectionTarget {

		private final ResourceLoader resourceLoader;

		@Autowired
		private ConstructorInjectionTarget(@SuppressWarnings("SpringJavaAutowiringInspection") ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

		public ResourceLoader getResourceLoader() {
			return this.resourceLoader;
		}
	}


	private static final class ResourceLoaderAwareBean implements ResourceLoaderAware {

		private ResourceLoader resourceLoader;

		public ResourceLoader getResourceLoader() {
			return this.resourceLoader;
		}

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}
	}
}
