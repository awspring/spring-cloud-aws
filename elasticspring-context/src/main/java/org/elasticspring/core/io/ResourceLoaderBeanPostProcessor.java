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

package org.elasticspring.core.io;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 *
 */
public class ResourceLoaderBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware, BeanFactoryPostProcessor {

	private final ResourceLoader resourceLoader;
	private ApplicationContext applicationContext;

	public ResourceLoaderBeanPostProcessor(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = decorateApplicationContext(applicationContext);
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ResourceLoaderAware) {
			((ResourceLoaderAware) bean).setResourceLoader(this.resourceLoader);
		}

		if (bean instanceof ApplicationContextAware) {
			((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
		}
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}


	protected ApplicationContext decorateApplicationContext(ApplicationContext target) {
		Class[] interfaces = ClassUtils.getAllInterfaces(target);
		return (ApplicationContext) Proxy.newProxyInstance(target.getClassLoader(), interfaces, new ResourceLoaderInvocationHandler(this.resourceLoader, target));
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		beanFactory.registerResolvableDependency(ApplicationContext.class, this.applicationContext);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this.applicationContext);
		beanFactory.registerResolvableDependency(BeanFactory.class, this.applicationContext);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this.resourceLoader);
	}

	private static class ResourceLoaderInvocationHandler implements InvocationHandler {

		private final ResourceLoader resourceLoader;
		private final ApplicationContext delegate;

		public ResourceLoaderInvocationHandler(ResourceLoader resourceLoader, ApplicationContext delegate) {
			this.resourceLoader = resourceLoader;
			this.delegate = delegate;
		}

		public Object invoke(Object target, Method method, Object[] arguments) throws Throwable {
			if (method.getDeclaringClass() == ResourceLoader.class) {
				return ReflectionUtils.invokeMethod(method, this.resourceLoader, arguments);
			}

			return ReflectionUtils.invokeMethod(method, this.delegate, arguments);
		}
	}
}
