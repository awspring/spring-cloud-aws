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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import org.elasticspring.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ResourceLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alain Sahli
 * @author Agim Emruli
 * @since 1.0
 */
public class ContextResourceLoaderBeanDefinitionParserTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void parseInternal_defaultConfiguration_createsAmazonS3ClientWithoutRegionConfigured() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		//Act
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);

		//Assert
		assertTrue(PathMatchingSimpleStorageResourcePatternResolver.class.isInstance(resourceLoader));
	}

	@Test
	public void parseInternal_configurationWithRegion_createsAmazonS3ClientWithRegionConfigured() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withRegionConfigured.xml", getClass());

		//Act
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);
		AmazonS3Client webServiceClient = applicationContext.getBean(AmazonS3Client.class);

		//Assert
		assertTrue(PathMatchingSimpleStorageResourcePatternResolver.class.isInstance(resourceLoader));
		assertEquals(Region.getRegion(Regions.EU_WEST_1), webServiceClient.getRegion().toAWSRegion());
	}

	@Test
	public void parseInternal_configurationWithCustomRegionProvider_createsAmazonS3ClientWithRegionConfigured() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withCustomRegionProvider.xml", getClass());

		//Act
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);
		AmazonS3Client webServiceClient = applicationContext.getBean(AmazonS3Client.class);

		//Assert
		assertTrue(PathMatchingSimpleStorageResourcePatternResolver.class.isInstance(resourceLoader));
		assertEquals(Region.getRegion(Regions.US_WEST_2), webServiceClient.getRegion().toAWSRegion());
	}

	@Test
	public void parseInternal_configurationWithCustomTaskExecutor_createsResourceLoaderWithCustomTaskExecutor() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withCustomTaskExecutor.xml", getClass());

		//Act
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);


		//Assert
		assertTrue(PathMatchingSimpleStorageResourcePatternResolver.class.isInstance(resourceLoader));

	}
}