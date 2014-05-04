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

package org.elasticspring.core.region;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.net.URI;

public class RegionPostProcessorTest {

	@Test
	public void postProcessAfterInitialization_nonConfiguredBean_isRegionConfigured() throws Exception {
		//Arrange
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-testNonConfiguredBean.xml", getClass()));

		//Act
		applicationContext.refresh();

		//Assert
		SimpleWebserviceClient webserviceClient = applicationContext.getBean(SimpleWebserviceClient.class);
		Assert.assertEquals(Region.getRegion(Regions.SA_EAST_1), webserviceClient.getRegion());
	}

	@Test
	public void postProcessAfterInitialization_configuredRegionBean_isRegionNotReConfigured() throws Exception {
		//Arrange
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-testConfiguredBean.xml", getClass()));

		//Act
		applicationContext.refresh();

		//Assert
		SimpleWebserviceClient webserviceClient = applicationContext.getBean(SimpleWebserviceClient.class);
		Assert.assertEquals(Region.getRegion(Regions.US_WEST_2), webserviceClient.getRegion());
	}

	@Test
	public void postProcessAfterInitialization_configuredEndpointBean_isRegionNotReConfigured() throws Exception {
		//Arrange
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-testConfiguredEndpointBean.xml", getClass()));

		//Act
		applicationContext.refresh();

		//Assert
		SimpleWebserviceClient webserviceClient = applicationContext.getBean(SimpleWebserviceClient.class);
		Assert.assertNull(webserviceClient.getRegion());
		Assert.assertEquals("test.amazonaws.com", webserviceClient.getEndpoint());
	}

	@Test
	public void postProcessAfterInitialization_nonConfiguredNestedClient_isRegionConfigured() throws Exception {
		//Arrange
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-testWithNestedClient.xml", getClass()));

		//Act
		applicationContext.refresh();

		//Assert
		SimpleObjectHolder objectHolder = applicationContext.getBean(SimpleObjectHolder.class);
		Assert.assertEquals(Region.getRegion(Regions.SA_EAST_1), objectHolder.getSimpleWebserviceClient().getRegion());
	}

	static class SimpleWebserviceClient extends AmazonWebServiceClient {

		private Region region;

		SimpleWebserviceClient() {
			super(new ClientConfiguration());
		}

		Region getRegion() {
			return this.region;
		}

		@Override
		public void setRegion(Region region) throws IllegalArgumentException {
			this.region = region;
		}

		String getEndpoint() {
			return this.endpoint.toString();
		}

		@Override
		public void setEndpoint(String endpoint) throws IllegalArgumentException {
			this.endpoint = URI.create(endpoint);
		}
	}

	static class SimpleObjectHolder {

		private final SimpleWebserviceClient simpleWebserviceClient;

		SimpleObjectHolder(SimpleWebserviceClient simpleWebserviceClient) {
			this.simpleWebserviceClient = simpleWebserviceClient;
		}

		SimpleWebserviceClient getSimpleWebserviceClient() {
			return this.simpleWebserviceClient;
		}
	}
}