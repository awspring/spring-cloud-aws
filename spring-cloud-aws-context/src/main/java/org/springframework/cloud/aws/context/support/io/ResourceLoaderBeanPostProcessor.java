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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * {@link BeanPostProcessor} implementation which decorates the ApplicationContext in order to set
 * a specialized {@link ResourceLoader} that can handle S3 resources.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class ResourceLoaderBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware, BeanFactoryPostProcessor {

	private final ResourceLoader resourceLoader;
	private ApplicationContext applicationContextProxy;

	public ResourceLoaderBeanPostProcessor(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (applicationContext instanceof GenericApplicationContext) {
			((GenericApplicationContext) applicationContext).setResourceLoader(this.resourceLoader);
		} else {
			this.applicationContextProxy = getApplicationContextProxy(applicationContext);
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ResourceLoaderAware) {
			((ResourceLoaderAware) bean).setResourceLoader(this.resourceLoader);
		}

		if (this.applicationContextProxy != null && bean instanceof ApplicationContextAware &&
				!(bean instanceof ApplicationObjectSupport)) {
			((ApplicationContextAware) bean).setApplicationContext(this.applicationContextProxy);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}


	protected ApplicationContext getApplicationContextProxy(ApplicationContext target) {
		Class<?>[] interfaces = ClassUtils.getAllInterfaces(target);
		return (ApplicationContext) Proxy.newProxyInstance(target.getClassLoader(), interfaces,
				new ResourceLoaderInvocationHandler(this.resourceLoader, target));
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.applicationContextProxy != null) {
			beanFactory.registerResolvableDependency(ApplicationContext.class, this.applicationContextProxy);
		}
		beanFactory.registerResolvableDependency(ResourceLoader.class, this.resourceLoader);
	}

	private static class ResourceLoaderInvocationHandler implements InvocationHandler {

		private final ResourceLoader resourceLoader;
		private final ApplicationContext delegate;

		private ResourceLoaderInvocationHandler(ResourceLoader resourceLoader, ApplicationContext delegate) {
			this.resourceLoader = resourceLoader;
			this.delegate = delegate;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getDeclaringClass() == ResourceLoader.class) {
				return ReflectionUtils.invokeMethod(method, this.resourceLoader, args);
			}

			return ReflectionUtils.invokeMethod(method, this.delegate, args);
		}
	}
}