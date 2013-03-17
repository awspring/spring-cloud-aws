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

package org.elasticspring.context.config.xml;

import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import org.elasticspring.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.elasticspring.core.io.s3.support.EndpointRoutingS3Client;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleStorageLoaderBeanDefinitionParserTest {

	@Test
	public void testCreateBeanDefinition() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);
		Assert.assertTrue(PathMatchingSimpleStorageResourcePatternResolver.class.isInstance(resourceLoader));
	}

	@Test
	public void testCreateResourceLoaderWithSymmetricKeyEncryptionClient() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withSymmetricKeyEncryptionClient.xml", getClass());
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);

		Object amazonS3 = ReflectionTestUtils.getField(resourceLoader, "amazonS3");
		if (amazonS3 instanceof EndpointRoutingS3Client) {
			EndpointRoutingS3Client endpointRoutingS3Client = (EndpointRoutingS3Client) amazonS3;
			Object defaultClient = ReflectionTestUtils.getField(endpointRoutingS3Client, "defaultClient");
			Assert.assertTrue(AmazonS3EncryptionClient.class.isInstance(defaultClient));
		} else {
			Assert.fail("Resource loader uses not the expected AmazonS3 client.");
		}
	}

	@Test
	public void testCreateResourceLoaderWithKeyPairEncryptionClient() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withKeyPairEncryptionClient.xml", getClass());
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);

		Object amazonS3 = ReflectionTestUtils.getField(resourceLoader, "amazonS3");
		if (amazonS3 instanceof EndpointRoutingS3Client) {
			EndpointRoutingS3Client endpointRoutingS3Client = (EndpointRoutingS3Client) amazonS3;
			Object defaultClient = ReflectionTestUtils.getField(endpointRoutingS3Client, "defaultClient");
			Assert.assertTrue(AmazonS3EncryptionClient.class.isInstance(defaultClient));
		} else {
			Assert.fail("Resource loader uses not the expected AmazonS3 client.");
		}
	}
}
