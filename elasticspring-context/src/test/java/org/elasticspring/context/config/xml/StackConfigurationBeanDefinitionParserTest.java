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

package org.elasticspring.context.config.xml;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import org.elasticspring.core.env.stack.StackResourceRegistry;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import static org.elasticspring.config.AmazonWebserviceClientConfigurationUtils.getBeanName;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 */
public class StackConfigurationBeanDefinitionParserTest {

	@Test
	public void parseInternal_stackConfigurationWithExternallyConfiguredCloudFormationClient_returnsConfiguredStackWithExternallyConfiguredClient() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-withCustomCloudFormationClient.xml", getClass()));

		AmazonCloudFormation amazonCloudFormationMock = beanFactory.getBean(AmazonCloudFormation.class);
		when(amazonCloudFormationMock.listStackResources(new ListStackResourcesRequest().withStackName("test"))).
				thenReturn(new ListStackResourcesResult().withStackResourceSummaries(new StackResourceSummary()));
		when(amazonCloudFormationMock.describeStacks(new DescribeStacksRequest().withStackName("test"))).
				thenReturn(new DescribeStacksResult().withStacks(new Stack()));


		//Act
		StackResourceRegistry stackResourceRegistry = beanFactory.getBean(StackResourceRegistry.class);

		//Assert
		assertNotNull(stackResourceRegistry);
		assertFalse(beanFactory.containsBeanDefinition(getBeanName(AmazonCloudFormationClient.class.getName())));
		verify(amazonCloudFormationMock, times(1)).listStackResources(new ListStackResourcesRequest().withStackName("test"));
		beanFactory.getBean("customStackTags");
		verify(amazonCloudFormationMock, times(1)).describeStacks(new DescribeStacksRequest().withStackName("test"));
	}
}