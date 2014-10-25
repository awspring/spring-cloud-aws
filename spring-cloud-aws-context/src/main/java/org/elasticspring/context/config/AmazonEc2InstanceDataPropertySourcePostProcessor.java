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

package org.elasticspring.context.config;

import org.elasticspring.core.env.ec2.AmazonEc2InstanceDataPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * @author Agim Emruli
 */
public class AmazonEc2InstanceDataPropertySourcePostProcessor implements PriorityOrdered, EnvironmentAware, BeanFactoryPostProcessor {

	static final String INSTANCE_DATA_PROPERTY_SOURCE_NAME = "InstanceData";
	private static final Logger LOGGER = LoggerFactory.getLogger(AmazonEc2InstanceDataPropertySourcePostProcessor.class);

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.environment instanceof ConfigurableEnvironment) {
			((ConfigurableEnvironment) this.environment).getPropertySources().addLast(new AmazonEc2InstanceDataPropertySource(INSTANCE_DATA_PROPERTY_SOURCE_NAME));
		} else {
			LOGGER.warn("Environment is not of type '{}' property source with instance data is not available", ConfigurableEnvironment.class.getName());
		}
	}
}