/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.junit.Test;
import org.springframework.core.io.Resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class SimpleStorageResourceLoaderTest {


	@Test
	public void testGetResourceWithExistingResource() throws Exception {

		String accessKey = "access";
		String secretKey = "secret";

		AmazonS3 amazonS3 = mock(AmazonS3.class);

		SimpleStorageResourceLoader resourceLoader = getResourceLoader(accessKey, secretKey, amazonS3);

		ObjectMetadata metadata = new ObjectMetadata();
		when(amazonS3.getObjectMetadata("bucket", "object")).thenReturn(metadata);

		@SuppressWarnings({"HardcodedFileSeparator"})
		String resourceName = "s3://bucket/object/";
		Resource resource = resourceLoader.getResource(resourceName);
		assertNotNull(resource);

	}

	@Test
	public void testGetResourceWithNonExistingResource() throws Exception {

		String accessKey = "access";
		String secretKey = "secret";

		AmazonS3 amazonS3 = mock(AmazonS3.class);

		SimpleStorageResourceLoader resourceLoader = getResourceLoader(accessKey, secretKey, amazonS3);

		when(amazonS3.getObjectMetadata("bucket", "object")).thenReturn(null);

		@SuppressWarnings({"HardcodedFileSeparator"})
		String resourceName = "s3://bucket/object/";
		Resource resource = resourceLoader.getResource(resourceName);
		assertNotNull(resource);
	}

	@Test
	public void testGetResourceWithDifferentPatterns() throws Exception {

		String accessKey = "access";
		String secretKey = "secret";

		AmazonS3 amazonS3 = mock(AmazonS3.class);

		SimpleStorageResourceLoader resourceLoader = getResourceLoader(accessKey, secretKey, amazonS3);


		assertNotNull(resourceLoader.getResource("s3://bucket/object/"));

		assertNotNull(resourceLoader.getResource("s3://bucket/object"));

		assertNotNull(resourceLoader.getResource("s3://prefix.bucket/object.suffix"));

		verify(amazonS3,times(2)).getObjectMetadata("bucket", "object");
	}


	@Test
	public void testGetResourceWithMalFormedUrl() throws Exception {

		String accessKey = "access";
		String secretKey = "secret";

		AmazonS3 amazonS3 = mock(AmazonS3.class);

		SimpleStorageResourceLoader resourceLoader = getResourceLoader(accessKey, secretKey, amazonS3);

		try {
			assertNotNull(resourceLoader.getResource("s3://bucket/object/asd/"));
			fail("expected exception due to path after object");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("not a valid s3 location"));
		}


		try {
			assertNotNull(resourceLoader.getResource("s3://bucketsAndObject"));
			fail("expected exception due to missing object");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("not a valid s3 location"));
		}


		verify(amazonS3,times(0)).getObjectMetadata("bucket", "object");
	}

	@Test
	public void testWithCustomClassLoader() throws Exception {
		ClassLoader classLoader = mock(ClassLoader.class);
		SimpleStorageResourceLoader simpleStorageResourceLoader = new SimpleStorageResourceLoader("access","secret",classLoader);
		assertSame(classLoader,simpleStorageResourceLoader.getClassLoader());
	}

	@Test
	public void testWithDefaultClassLoader() throws Exception {
		SimpleStorageResourceLoader simpleStorageResourceLoader = new SimpleStorageResourceLoader("access","secret");
		assertSame(SimpleStorageResourceLoader.class.getClassLoader(),simpleStorageResourceLoader.getClassLoader());
	}

	private SimpleStorageResourceLoader getResourceLoader(String accessKey, String secretKey, final AmazonS3 amazonS3) {
		return new SimpleStorageResourceLoader(accessKey, secretKey) {

			@Override
			public AmazonS3 getAmazonS3() {
				return amazonS3;
			}
		};
	}
}
