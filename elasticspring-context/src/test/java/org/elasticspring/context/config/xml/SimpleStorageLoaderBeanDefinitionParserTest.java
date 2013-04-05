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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleStorageLoaderBeanDefinitionParserTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testCreateBeanDefinition() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);
		Assert.assertTrue(PathMatchingSimpleStorageResourcePatternResolver.class.isInstance(resourceLoader));
	}

	@Test
	public void testCreateResourceLoaderWithSecretKeyEncryptionClient() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withSecretKeyEncryptionClient.xml", getClass());
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);

		assertThatClientIsEncryptionClient(resourceLoader);
	}

	@Test
	public void testCreateResourceLoaderWithSecretKeyRef() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withSecretKeyRef.xml", getClass());
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);

		assertThatClientIsEncryptionClient(resourceLoader);
	}

	@Test
	public void testCreateResourceLoaderWithKeyPairRef() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withKeyPairRef.xml", getClass());
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);

		assertThatClientIsEncryptionClient(resourceLoader);
	}

	@Test
	public void testCreateResourceLoaderWithKeyPairEncryptionClient() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withKeyPairEncryptionClient.xml", getClass());
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);

		assertThatClientIsEncryptionClient(resourceLoader);
	}

	@Test
	public void testCreateResourceLoaderWithAnonymousFlag() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withAnonymousFlag.xml", getClass());
		ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoader.class);

		assertThatClientIsEncryptionClient(resourceLoader);
	}

	@Test
	public void testCreateResourceLoaderWithAnonymousFlagOnly() throws Exception {
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("When attribute 'anonymous' is set to 'true' either 'key-pair' or 'secret-key' must be set.");

		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withAnonymousFlagOnly.xml", getClass());
	}

	@Test
	public void testCreateResourceLoaderWithTwoKeyPair() throws Exception {
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("'ref' and 'public-key-resource' with 'private-key-resource' are not allowed together in the same 'key-pair' element.");

		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withTwoKeyPair.xml", getClass());
	}

	@Test
	public void testCreateResourceLoaderWithTwoSecretKey() throws Exception {
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("'ref' and 'password' with 'salt' are not allowed together in the same 'secret-key' element.");

		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withTwoSecretKey.xml", getClass());
	}

	private void assertThatClientIsEncryptionClient(ResourceLoader resourceLoader) {
		Object amazonS3 = ReflectionTestUtils.getField(resourceLoader, "amazonS3");
		if (!(amazonS3 instanceof AmazonS3EncryptionClient)) {
			Assert.fail("Resource loader uses not the expected AmazonS3 client.");
		}
	}
}
