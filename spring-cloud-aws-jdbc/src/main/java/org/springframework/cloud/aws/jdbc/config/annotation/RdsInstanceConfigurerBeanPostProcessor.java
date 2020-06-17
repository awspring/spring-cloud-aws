/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.jdbc.config.annotation;

import java.util.Collection;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceFactoryBean;

/**
 * Bean post processor for RDS instance configurer.
 *
 * @author Agim Emruli
 */
public class RdsInstanceConfigurerBeanPostProcessor
		implements BeanPostProcessor, BeanFactoryAware {

	private RdsInstanceConfigurer rdsInstanceConfigurer;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof AmazonRdsDataSourceFactoryBean
				&& this.rdsInstanceConfigurer != null) {
			((AmazonRdsDataSourceFactoryBean) bean).setDataSourceFactory(
					this.rdsInstanceConfigurer.getDataSourceFactory());
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof ListableBeanFactory) {
			Collection<RdsInstanceConfigurer> configurer = ((ListableBeanFactory) beanFactory)
					.getBeansOfType(RdsInstanceConfigurer.class).values();

			if (configurer.isEmpty()) {
				return;
			}

			if (configurer.size() > 1) {
				throw new IllegalStateException(
						"Only one RdsInstanceConfigurer may exist");
			}

			this.rdsInstanceConfigurer = configurer.iterator().next();
		}
	}

}
