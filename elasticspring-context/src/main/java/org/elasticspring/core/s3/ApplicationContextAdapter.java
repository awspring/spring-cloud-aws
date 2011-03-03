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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;

/**
 *
 */
public class ApplicationContextAdapter implements ApplicationContext {

	private final ApplicationContext delegate;
	private final ResourceLoader resourceLoader;

	public ApplicationContextAdapter(ApplicationContext delegate, ResourceLoader resourceLoader) {
		this.delegate = delegate;
		this.resourceLoader = resourceLoader;
	}

	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return delegate.getAutowireCapableBeanFactory();
	}

	public String getDisplayName() {
		return delegate.getDisplayName();
	}

	public String getId() {
		return delegate.getId();
	}

	public ApplicationContext getParent() {
		return delegate.getParent();
	}

	public long getStartupDate() {
		return delegate.getStartupDate();
	}

	public boolean containsBeanDefinition(String beanName) {
		return delegate.containsBeanDefinition(beanName);
	}

	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
		return delegate.findAnnotationOnBean(beanName, annotationType);
	}

	public int getBeanDefinitionCount() {
		return delegate.getBeanDefinitionCount();
	}

	public String[] getBeanDefinitionNames() {
		return delegate.getBeanDefinitionNames();
	}

	public String[] getBeanNamesForType(Class type) {
		return delegate.getBeanNamesForType(type);
	}

	public String[] getBeanNamesForType(Class type, boolean includeNonSingletons, boolean allowEagerInit) {
		return delegate.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		return delegate.getBeansOfType(type);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
		return delegate.getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}

	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
		return delegate.getBeansWithAnnotation(annotationType);
	}

	public boolean containsBean(String name) {
		return delegate.containsBean(name);
	}

	public String[] getAliases(String name) {
		return delegate.getAliases(name);
	}

	public Object getBean(String name) throws BeansException {
		return delegate.getBean(name);
	}

	public Object getBean(String name, Object... args) throws BeansException {
		return delegate.getBean(name, args);
	}

	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return delegate.getBean(name, requiredType);
	}

	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return delegate.getBean(requiredType);
	}

	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return delegate.getType(name);
	}

	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		return delegate.isPrototype(name);
	}

	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return delegate.isSingleton(name);
	}

	public boolean isTypeMatch(String name, Class targetType) throws NoSuchBeanDefinitionException {
		return delegate.isTypeMatch(name, targetType);
	}

	public boolean containsLocalBean(String name) {
		return delegate.containsLocalBean(name);
	}

	public BeanFactory getParentBeanFactory() {
		return delegate.getParentBeanFactory();
	}

	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		return delegate.getMessage(code, args, defaultMessage, locale);
	}

	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		return delegate.getMessage(code, args, locale);
	}

	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return delegate.getMessage(resolvable, locale);
	}

	public void publishEvent(ApplicationEvent event) {
		delegate.publishEvent(event);
	}

	public Resource[] getResources(String locationPattern) throws IOException {
		return delegate.getResources(locationPattern);
	}

	public ClassLoader getClassLoader() {
		return delegate.getClassLoader();
	}

	public Resource getResource(String location) {
		return resourceLoader.getResource(location);
	}
}
