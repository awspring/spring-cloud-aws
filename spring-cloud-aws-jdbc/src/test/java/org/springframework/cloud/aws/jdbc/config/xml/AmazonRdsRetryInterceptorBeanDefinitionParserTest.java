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

package org.springframework.cloud.aws.jdbc.config.xml;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Agim Emruli
 */
public class AmazonRdsRetryInterceptorBeanDefinitionParserTest {

    @Test
    public void parseInternal_minimalConfiguration_createsRetryInterceptor() throws Exception {
        //Arrange
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-minimal.xml", getClass());

        //Act
        MethodInterceptor interceptor = classPathXmlApplicationContext.getBean(MethodInterceptor.class);

        //Assert
        assertNotNull(interceptor);
    }


    @Test
    public void parseInternal_customRegionConfigured_createsAmazonRdsClientWithCustomRegionConfigured() throws Exception {
        //Arrange
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customRegion.xml", getClass());

        //Act
        AmazonRDS amazonRDS = classPathXmlApplicationContext.getBean(AmazonRDS.class);

        //Assert
        assertEquals("https://rds.eu-west-1.amazonaws.com", ReflectionTestUtils.getField(amazonRDS, "endpoint").toString());
    }


    @Test
    public void parseInternal_customRegionProviderConfigured_createAmazonRdsClientWithCustomRegionConfigured() throws Exception {
        //Arrange
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customRegionProvider.xml", getClass());

        //Act
        AmazonRDS amazonRDS = classPathXmlApplicationContext.getBean(AmazonRDS.class);

        //Assert
        assertEquals("https://rds.eu-west-1.amazonaws.com", ReflectionTestUtils.getField(amazonRDS, "endpoint").toString());
    }

    @Test
    public void parseInternal_customRDsClientConfigured_createInterceptorWithCustomRdsClient() throws Exception {
        //Arrange
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customRdsClient.xml", getClass());

        //Act
        classPathXmlApplicationContext.getBean(MethodInterceptor.class);

        //Assert
        assertFalse(classPathXmlApplicationContext.containsBean(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonRDSClient.class.getName())));
    }

    @Test
    public void parseInternal_customBackOffPolicy_createInterceptorWithCustomBackOffPolicy() throws Exception {
        //Arrange
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customBackOffPolicy.xml", getClass());
        BeanDefinition beanDefinition = classPathXmlApplicationContext.getBeanFactory().getBeanDefinition("interceptor");

        //Act
        BeanDefinition retryOperations = (BeanDefinition) beanDefinition.getPropertyValues().getPropertyValue("retryOperations").getValue();

        //Assert
        assertEquals("policy", ((RuntimeBeanReference) retryOperations.getPropertyValues().getPropertyValue("backOffPolicy").getValue()).getBeanName());
    }

    @Test
    public void parseInternal_customNumberOfRetiresConfigured_createRetryPolicyWithCustomNumberOfRetriesConfigured() throws Exception {
        //Arrange
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-maxNumberOfRetries.xml", getClass());

        //Act
        BeanDefinition beanDefinition = classPathXmlApplicationContext.getBeanFactory().getBeanDefinition("interceptor");

        //Assert
        BeanDefinition retryOperations = (BeanDefinition) beanDefinition.getPropertyValues().getPropertyValue("retryOperations").getValue();
        BeanDefinition compositeRetryPolicy = (BeanDefinition) retryOperations.getPropertyValues().getPropertyValue("retryPolicy").getValue();
        @SuppressWarnings("unchecked") List<BeanDefinition> policies = (List<BeanDefinition>) compositeRetryPolicy.getPropertyValues().getPropertyValue("policies").getValue();
        BeanDefinition sqlPolicy = policies.get(1);
        assertEquals("4", sqlPolicy.getPropertyValues().getPropertyValue("maxNumberOfRetries").getValue().toString());
    }
}
