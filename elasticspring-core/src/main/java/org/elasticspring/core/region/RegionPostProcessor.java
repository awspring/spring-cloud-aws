/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.core.region;

import com.amazonaws.AmazonWebServiceClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.Assert;

public class RegionPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private final RegionProvider regionProvider;
	private ConfigurableListableBeanFactory listableBeanFactory;

	public RegionPostProcessor(RegionProvider regionProvider) {
		this.regionProvider = regionProvider;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class,beanFactory);
		this.listableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		BeanDefinition beanDefinition = this.listableBeanFactory.getBeanDefinition(beanName);
		if (bean instanceof AmazonWebServiceClient &&  !(beanDefinition.getPropertyValues().contains("region") ||
				beanDefinition.getPropertyValues().contains("endpoint"))) {
			AmazonWebServiceClient webServiceClient = (AmazonWebServiceClient) bean;
			webServiceClient.setRegion(this.regionProvider.getRegion());
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}