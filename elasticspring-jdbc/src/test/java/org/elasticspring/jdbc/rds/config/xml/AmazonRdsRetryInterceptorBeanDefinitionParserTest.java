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

package org.elasticspring.jdbc.rds.config.xml;

import com.amazonaws.services.rds.AmazonRDS;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

/**
 *
 */
public class AmazonRdsRetryInterceptorBeanDefinitionParserTest {

	@Test
	public void testCreateMinimalConfiguration() throws Exception {
		ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-minimal.xml", getClass());

		//Get bean so it will be initialized
		MethodInterceptor interceptor = classPathXmlApplicationContext.getBean(MethodInterceptor.class);

		Assert.assertNotNull(interceptor);

	}


	@Test
	public void testCreateCustomRegion() throws Exception {
		ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customRegion.xml", getClass());

		//Get bean so it will be initialized
		AmazonRDS amazonRDS = classPathXmlApplicationContext.getBean(AmazonRDS.class);

		//have to use reflection utils
		Assert.assertEquals("https://rds.eu-west-1.amazonaws.com", ReflectionTestUtils.getField(amazonRDS, "endpoint").toString());

	}


	@Test
	public void testCreateCustomRegionProvider() throws Exception {
		ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customRegionProvider.xml", getClass());

		//Get bean so it will be initialized
		AmazonRDS amazonRDS = classPathXmlApplicationContext.getBean(AmazonRDS.class);

		//have to use reflection utils
		Assert.assertEquals("https://rds.eu-west-1.amazonaws.com", ReflectionTestUtils.getField(amazonRDS, "endpoint").toString());

	}

	@Test
	public void testCustomBackoffPolicy() throws Exception {
		ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customBackOffPolicy.xml", getClass());
		BeanDefinition beanDefinition = classPathXmlApplicationContext.getBeanFactory().getBeanDefinition("interceptor");
		BeanDefinition retryOperations = (BeanDefinition) beanDefinition.getPropertyValues().getPropertyValue("retryOperations").getValue();
		Assert.assertEquals("policy", ((RuntimeBeanReference) retryOperations.getPropertyValues().getPropertyValue("backOffPolicy").getValue()).getBeanName());
	}

	@Test
	public void testCustomMaxNumberOfRetries() throws Exception {
		ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-maxNUmberOfRetries.xml", getClass());
		BeanDefinition beanDefinition = classPathXmlApplicationContext.getBeanFactory().getBeanDefinition("interceptor");
		BeanDefinition retryOperations = (BeanDefinition) beanDefinition.getPropertyValues().getPropertyValue("retryOperations").getValue();
		BeanDefinition compositeRetryPolicy = (BeanDefinition) retryOperations.getPropertyValues().getPropertyValue("retryPolicy").getValue();
		@SuppressWarnings("unchecked") List<BeanDefinition> policies = (List<BeanDefinition>) compositeRetryPolicy.getPropertyValues().getPropertyValue("policies").getValue();
		BeanDefinition sqlPolicy = policies.get(1);
		Assert.assertEquals("4", sqlPolicy.getPropertyValues().getPropertyValue("maxNumberOfRetries").getValue().toString());

	}
}