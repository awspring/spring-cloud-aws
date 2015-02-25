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

package org.springframework.cloud.aws.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.junit.Test;
import org.springframework.core.io.Resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleStorageResourceLoaderTest {


	@Test
	public void testGetResourceWithExistingResource() throws Exception {

		AmazonS3 amazonS3 = mock(AmazonS3.class);

		SimpleStorageResourceLoader resourceLoader = new SimpleStorageResourceLoader(amazonS3);

		ObjectMetadata metadata = new ObjectMetadata();
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(metadata);

		String resourceName = "s3://bucket/object/";
		Resource resource = resourceLoader.getResource(resourceName);
		assertNotNull(resource);
	}

	@Test
	public void testGetResourceWithNonExistingResource() throws Exception {

		AmazonS3 amazonS3 = mock(AmazonS3.class);

		SimpleStorageResourceLoader resourceLoader = new SimpleStorageResourceLoader(amazonS3);

		String resourceName = "s3://bucket/object/";
		Resource resource = resourceLoader.getResource(resourceName);
		assertNotNull(resource);
	}
	
	@Test
	public void testGetResourceWithVersionId() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);

		SimpleStorageResourceLoader resourceLoader = new SimpleStorageResourceLoader(amazonS3);

		ObjectMetadata metadata = new ObjectMetadata();
		
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(metadata);

		String resourceName = "s3://bucket/object^versionIdValue";
		Resource resource = resourceLoader.getResource(resourceName);
		assertNotNull(resource);
	}

	@Test
	public void testGetResourceWithDifferentPatterns() throws Exception {

		AmazonS3 amazonS3 = mock(AmazonS3.class);

		SimpleStorageResourceLoader resourceLoader = new SimpleStorageResourceLoader(amazonS3);


		assertNotNull(resourceLoader.getResource("s3://bucket/object/"));

		assertNotNull(resourceLoader.getResource("s3://bucket/object"));

		assertNotNull(resourceLoader.getResource("s3://prefix.bucket/object.suffix"));

		verify(amazonS3, times(0)).getObjectMetadata("bucket", "object");
	}

	@Test
	public void testGetResourceWithMalFormedUrl() throws Exception {

		AmazonS3 amazonS3 = mock(AmazonS3.class);

		SimpleStorageResourceLoader resourceLoader = new SimpleStorageResourceLoader(amazonS3);

		try {
			assertNotNull(resourceLoader.getResource("s3://bucketsAndObject"));
			fail("expected exception due to missing object");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("valid bucket name"));
		}


		verify(amazonS3, times(0)).getObjectMetadata("bucket", "object");
	}

	@Test
	public void testWithCustomClassLoader() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		ClassLoader classLoader = mock(ClassLoader.class);
		SimpleStorageResourceLoader simpleStorageResourceLoader = new SimpleStorageResourceLoader(amazonS3, classLoader);
		assertSame(classLoader, simpleStorageResourceLoader.getClassLoader());
	}

	@Test
	public void testWithDefaultClassLoader() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		SimpleStorageResourceLoader simpleStorageResourceLoader = new SimpleStorageResourceLoader(amazonS3);
		assertSame(SimpleStorageResourceLoader.class.getClassLoader(), simpleStorageResourceLoader.getClassLoader());
	}

	@Test
	public void testValidS3Pattern() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		SimpleStorageResourceLoader resourceLoader = new SimpleStorageResourceLoader(amazonS3);

		// None of the patterns below should throw an exception
		resourceLoader.getResource("s3://bucket/key");
		resourceLoader.getResource("S3://BuCket/key");
		resourceLoader.getResource("s3://bucket/folder1/folder2/key");
		resourceLoader.getResource("s3://bucket/folder1/folder2/key^versionIdValue");
	}

}
