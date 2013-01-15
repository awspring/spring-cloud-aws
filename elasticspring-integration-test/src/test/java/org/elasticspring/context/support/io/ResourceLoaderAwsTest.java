/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.context.support.io;

import org.elasticspring.core.region.S3Region;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ResourceLoaderAwsTest {

	public static final String S3_PREFIX = "s3://";
	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ResourceLoader resourceLoader;

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testWithInjectedApplicationContext() throws Exception {
		Resource resource = this.applicationContext.getResource("s3://test.elasticspring.org/test");
		Assert.assertTrue(resource.exists());
		InputStream inputStream = resource.getInputStream();
		Assert.assertNotNull(inputStream);
		Assert.assertTrue(resource.contentLength() > 0);
		inputStream.close();
	}

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testWithInjectedResourceLoader() throws Exception {
		Resource resource = this.resourceLoader.getResource("s3://test.elasticspring.org/test");
		Assert.assertTrue(resource.exists());
		InputStream inputStream = resource.getInputStream();
		Assert.assertNotNull(inputStream);
		Assert.assertTrue(resource.contentLength() > 0);
		inputStream.close();
	}

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testWriteFile() throws Exception {
		Resource resource = this.resourceLoader.getResource("s3://test.elasticspring.org/newFile");
		Assert.assertTrue(WritableResource.class.isInstance(resource));
		WritableResource writableResource = (WritableResource) resource;
		OutputStream outputStream = writableResource.getOutputStream();
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < (1024 * 1024); j++) {
				outputStream.write("c".getBytes("UTF-8"));
			}
		}
		outputStream.close();
	}

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testBucketNamesWithDotsOnAllS3Regions() throws IOException {
		for (S3Region region : S3Region.values()) {
			InputStream inputStream = null;
			try {
				String bucketNameWithDots = region.getRegion() + ".elasticspring.org";
				Resource resource = this.resourceLoader.getResource(S3_PREFIX + bucketNameWithDots + "/test.txt");
				inputStream = resource.getInputStream();
				Assert.assertTrue(resource.contentLength() > 0);
				Assert.assertNotNull(inputStream);
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}
			}
		}
	}

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testBucketNamesWithoutDotsOnAllS3Regions() throws IOException {
		for (S3Region region : S3Region.values()) {
			InputStream inputStream = null;
			try {
				String bucketNameWithoutDots = region.getRegion() + "-elasticspring-org";
				Resource resource = this.resourceLoader.getResource(S3_PREFIX + bucketNameWithoutDots + "/test.txt");
				inputStream = resource.getInputStream();
				Assert.assertTrue(resource.contentLength() > 0);
				Assert.assertNotNull(inputStream);
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}
			}
		}
	}
}