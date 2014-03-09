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
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

public class RegionPostProcessorTest {

	@Test
	public void postProcessBeforeInitialization_nonConfiguredBean_isRegionConfigured() throws Exception {
		//Arrange
		RegionPostProcessor regionPostProcessor = new RegionPostProcessor(new StaticRegionProvider(Regions.EU_WEST_1));
		AmazonWebServiceClient webServiceClient = Mockito.mock(AmazonWebServiceClient.class);
		ConfigurableListableBeanFactory beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition();
		Mockito.when(beanFactory.getBeanDefinition("testClient")).thenReturn(beanDefinitionBuilder.getBeanDefinition());
		regionPostProcessor.setBeanFactory(beanFactory);

		//Act
		regionPostProcessor.postProcessBeforeInitialization(webServiceClient, "testClient");

		//Assert
		Mockito.verify(webServiceClient, Mockito.times(1)).setRegion(Region.getRegion(Regions.EU_WEST_1));
	}

	@Test
	public void postProcessBeforeInitialization_configuredRegionBean_isRegionConfigured() throws Exception {
		//Arrange
		RegionPostProcessor regionPostProcessor = new RegionPostProcessor(new StaticRegionProvider(Regions.EU_WEST_1));
		AmazonWebServiceClient webServiceClient = Mockito.mock(AmazonWebServiceClient.class);
		ConfigurableListableBeanFactory beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition();
		beanDefinitionBuilder.addPropertyValue("region", Region.getRegion(Regions.AP_NORTHEAST_1));
		Mockito.when(beanFactory.getBeanDefinition("testClient")).thenReturn(beanDefinitionBuilder.getBeanDefinition());
		regionPostProcessor.setBeanFactory(beanFactory);

		//Act
		regionPostProcessor.postProcessBeforeInitialization(webServiceClient, "testClient");

		//Assert
		Mockito.verify(webServiceClient,Mockito.times(0)).setRegion(Mockito.any(Region.class));
	}

	@Test
	public void postProcessBeforeInitialization_configuredEndpointBean_isRegionConfigured() throws Exception {
		//Arrange
		RegionPostProcessor regionPostProcessor = new RegionPostProcessor(new StaticRegionProvider(Regions.EU_WEST_1));
		AmazonWebServiceClient webServiceClient = Mockito.mock(AmazonWebServiceClient.class);
		ConfigurableListableBeanFactory beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition();
		beanDefinitionBuilder.addPropertyValue("endpoint", Region.getRegion(Regions.AP_NORTHEAST_1).getServiceEndpoint("sqs"));
		Mockito.when(beanFactory.getBeanDefinition("testClient")).thenReturn(beanDefinitionBuilder.getBeanDefinition());
		regionPostProcessor.setBeanFactory(beanFactory);

		//Act
		regionPostProcessor.postProcessBeforeInitialization(webServiceClient, "testClient");

		//Assert
		Mockito.verify(webServiceClient,Mockito.times(0)).setEndpoint(Mockito.any(String.class));
	}
}
