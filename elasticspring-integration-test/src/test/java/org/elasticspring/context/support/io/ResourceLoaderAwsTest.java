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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.elasticspring.core.io.s3.S3Region;
import org.elasticspring.support.TestStackEnvironment;
import org.junit.After;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("ResourceLoaderAwsTest-context.xml")
public class ResourceLoaderAwsTest {

	private static final String S3_PREFIX = "s3://";
	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private TestStackEnvironment testStackEnvironment;

	@SuppressWarnings("SpringJavaAutowiringInspection")
	@Autowired
	private AmazonS3 amazonS3;

	private final List<String> createdObjects = new ArrayList<String>();

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testWithInjectedApplicationContext() throws Exception {
		String bucketName = this.testStackEnvironment.getByLogicalId("EmptyBucket");
		uploadFile(bucketName, "test");
		Resource resource = this.applicationContext.getResource(S3_PREFIX + bucketName + "/test");
		Assert.assertTrue(resource.exists());
		InputStream inputStream = resource.getInputStream();
		Assert.assertNotNull(inputStream);
		Assert.assertTrue(resource.contentLength() > 0);
		inputStream.close();
	}

	private void uploadFile(String bucketName, String objectKey) {
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength("hello world".length());
		this.amazonS3.putObject(bucketName, objectKey, new ByteArrayInputStream("hello world".getBytes()), objectMetadata);
		this.createdObjects.add(objectKey);
	}

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testWithInjectedResourceLoader() throws Exception {
		String bucketName = this.testStackEnvironment.getByLogicalId("EmptyBucket");
		uploadFile(bucketName, "test");
		Resource resource = this.applicationContext.getResource(S3_PREFIX + bucketName + "/test");
		Assert.assertTrue(resource.exists());
		InputStream inputStream = resource.getInputStream();
		Assert.assertNotNull(inputStream);
		Assert.assertTrue(resource.contentLength() > 0);
		inputStream.close();
	}

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testWriteFile() throws Exception {
		String bucketName = this.testStackEnvironment.getByLogicalId("EmptyBucket");
		Resource resource = this.resourceLoader.getResource(S3_PREFIX + bucketName + "/newFile");
		Assert.assertTrue(WritableResource.class.isInstance(resource));
		WritableResource writableResource = (WritableResource) resource;
		OutputStream outputStream = writableResource.getOutputStream();
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < (1024 * 1024); j++) {
				outputStream.write("c".getBytes("UTF-8"));
			}
		}
		outputStream.close();
		this.createdObjects.add("newFile");
	}

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testBucketNamesWithDotsOnAllS3Regions() throws IOException {
		for (S3Region region : S3Region.values()) {
			InputStream inputStream = null;
			try {
				String bucketNameWithDots = region.getLocation().toLowerCase() + ".elasticspring.org";
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
				String bucketNameWithoutDots = region.getLocation().toLowerCase() + "-elasticspring-org";
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

	//Cleans up the bucket. Because if the bucket is not cleaned up, then the bucket will not be deleted after the test run.
	@After
	public void tearDown() throws Exception {
		String bucketName = this.testStackEnvironment.getByLogicalId("EmptyBucket");
		for (String createdObject : this.createdObjects) {
			this.amazonS3.deleteObject(bucketName, createdObject);
		}

	}
}