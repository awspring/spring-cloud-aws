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

// CHECKSTYLE:OFF
// Checkstyle is disabled because in test 'testWriteFileAndCheckChecksum'
// there is a needed while loop without a statement inside.


package org.elasticspring.context.support.io;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.elasticspring.support.TestStackEnvironment;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Agim Emruli
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
	public void testWriteFileAndCheckChecksum() throws IOException, NoSuchAlgorithmException {
		String bucketName = this.testStackEnvironment.getByLogicalId("EmptyBucket");
		Resource resource = this.resourceLoader.getResource(S3_PREFIX + bucketName + "/test-file.pdf");
		Assert.assertTrue(WritableResource.class.isInstance(resource));

		WritableResource writableResource = (WritableResource) resource;
		OutputStream outputStream = writableResource.getOutputStream();
		ClassPathResource testFileResource = new ClassPathResource("/org/elasticspring/context/support/io/test-file.pdf");
		InputStream inputStream = new FileInputStream(testFileResource.getFile());

		byte[] buffer = new byte[1024];
		MessageDigest md = MessageDigest.getInstance("MD5");
		try {
			inputStream = new DigestInputStream(inputStream, md);
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
		} finally {
			inputStream.close();
			outputStream.close();
		}

		byte[] originalMd5Checksum = md.digest();

		Resource downloadedResource = this.resourceLoader.getResource(S3_PREFIX + bucketName + "/test-file.pdf");
		InputStream downloadedInputStream = downloadedResource.getInputStream();

		md.reset();
		try {
			downloadedInputStream = new DigestInputStream(downloadedInputStream, md);
			//noinspection StatementWithEmptyBody
			while (downloadedInputStream.read(buffer) != -1) {
				// go through the input stream until EOF to compute MD5 checksum.
			}
		} finally {
			downloadedInputStream.close();
		}

		byte[] downloadedMd5Checksum = md.digest();

		Assert.assertEquals(DigestUtils.md5DigestAsHex(originalMd5Checksum), DigestUtils.md5DigestAsHex(downloadedMd5Checksum));
		this.createdObjects.add("test-file.pdf");
	}


	//Cleans up the bucket. Because if the bucket is not cleaned up, then the bucket will not be deleted after the test run.
	@After
	public void tearDown() {
		String bucketName = this.testStackEnvironment.getByLogicalId("EmptyBucket");
		for (String createdObject : this.createdObjects) {
			this.amazonS3.deleteObject(bucketName, createdObject);
		}

	}
}