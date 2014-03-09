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
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} that inspects beans and sets the region for all
 * {@link com.amazonaws.AmazonWebServiceClient} instances. This post processor can reconfigure the default region to a
 * particular one.
 *
 * @author Agim Emruli
 */
public class RegionPostProcessor implements MergedBeanDefinitionPostProcessor {

	private final RegionProvider regionProvider;

	/**
	 * Configures the post processor with the respective class
	 *
	 * @param regionProvider
	 * 		- the region provider that will be used, might be a static or dynamic one.
	 */
	public RegionPostProcessor(RegionProvider regionProvider) {
		this.regionProvider = regionProvider;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		//Check if we the class to be created is a AmazonWebserviceClient
		if (AmazonWebServiceClient.class.isAssignableFrom(beanType)) {
			if (!(beanDefinition.getPropertyValues().contains("region") ||
					beanDefinition.getPropertyValues().contains("endpoint"))) {
				beanDefinition.getPropertyValues().add("region", this.regionProvider.getRegion());
			}
		}
	}
}