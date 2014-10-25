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
// Checkstyle is disabled because in test 'testUploadBigFileAndCompareChecksum'
// there is a needed while loop without a statement inside.


package org.springframework.cloud.aws.context.support.io;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;
import org.junit.After;
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
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Agim Emruli
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ResourceLoaderAwsTest {

	private static final String S3_PREFIX = "s3://";
	private final List<String> createdObjects = new ArrayList<String>();
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private ResourceLoader resourceLoader;
	@SuppressWarnings("SpringJavaAutowiringInspection")
	@Autowired
	private AmazonS3 amazonS3;
	@SuppressWarnings("SpringJavaAutowiringInspection")
	@Autowired
	private StackResourceRegistry stackResourceRegistry;

	@Test
	public void testUploadAndDownloadOfSmallFileWithInjectedApplicationContext() throws Exception {
		String bucketName = this.stackResourceRegistry.lookupPhysicalResourceId("EmptyBucket");
		uploadFileTestFile(bucketName, "testUploadAndDownloadOfSmallFileWithInjectedApplicationContext", "Test Content");
		Resource resource = this.applicationContext.getResource(S3_PREFIX + bucketName + "/testUploadAndDownloadOfSmallFileWithInjectedApplicationContext");
		assertTrue(resource.exists());
		InputStream inputStream = resource.getInputStream();
		assertNotNull(inputStream);
		assertEquals("Test Content", FileCopyUtils.copyToString(new InputStreamReader(inputStream, "UTF-8")));
		assertEquals("Test Content".length(), resource.contentLength());
	}

	@Test
	public void testUploadAndDownloadOfSmallFileWithInjectedResourceLoader() throws Exception {
		String bucketName = this.stackResourceRegistry.lookupPhysicalResourceId("EmptyBucket");
		uploadFileTestFile(bucketName, "testUploadAndDownloadOfSmallFileWithInjectedResourceLoader", "hello world");
		Resource resource = this.applicationContext.getResource(S3_PREFIX + bucketName + "/testUploadAndDownloadOfSmallFileWithInjectedResourceLoader");
		assertTrue(resource.exists());
		InputStream inputStream = resource.getInputStream();
		assertNotNull(inputStream);
		assertEquals("hello world", FileCopyUtils.copyToString(new InputStreamReader(inputStream, "UTF-8")));
		assertEquals("hello world".length(), resource.contentLength());
	}

	private void uploadFileTestFile(String bucketName, String objectKey, String content) throws UnsupportedEncodingException {
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(content.length());
		this.amazonS3.putObject(bucketName, objectKey, new ByteArrayInputStream(content.getBytes("UTF-8")), objectMetadata);
		this.createdObjects.add(objectKey);
	}

	@Test
	public void testUploadFileWithMoreThenFiveMegabytes() throws Exception {
		String bucketName = this.stackResourceRegistry.lookupPhysicalResourceId("EmptyBucket");
		Resource resource = this.resourceLoader.getResource(S3_PREFIX + bucketName + "/testUploadFileWithMoreThenFiveMegabytes");
		assertTrue(WritableResource.class.isInstance(resource));
		WritableResource writableResource = (WritableResource) resource;
		OutputStream outputStream = writableResource.getOutputStream();
		for (int i = 0; i < (1024 * 1024 * 6); i++) {
			outputStream.write("c".getBytes("UTF-8"));
		}
		outputStream.close();
		this.createdObjects.add("testUploadFileWithMoreThenFiveMegabytes");
	}

	@Test
	public void testUploadBigFileAndCompareChecksum() throws IOException, NoSuchAlgorithmException {
		String bucketName = this.stackResourceRegistry.lookupPhysicalResourceId("EmptyBucket");
		Resource resource = this.resourceLoader.getResource(S3_PREFIX + bucketName + "/test-file.jpg");
		assertTrue(WritableResource.class.isInstance(resource));

		WritableResource writableResource = (WritableResource) resource;
		OutputStream outputStream = writableResource.getOutputStream();
		ClassPathResource testFileResource = new ClassPathResource("test-file.jpg", getClass());
		InputStream inputStream = new FileInputStream(testFileResource.getFile());

		MessageDigest md = MessageDigest.getInstance("MD5");

		inputStream = new DigestInputStream(inputStream, md);
		FileCopyUtils.copy(inputStream, outputStream);

		byte[] originalMd5Checksum = md.digest();

		Resource downloadedResource = this.resourceLoader.getResource(S3_PREFIX + bucketName + "/test-file.jpg");
		InputStream downloadedInputStream = downloadedResource.getInputStream();

		md.reset();
		try {
			downloadedInputStream = new DigestInputStream(downloadedInputStream, md);
			//noinspection StatementWithEmptyBody
			while (downloadedInputStream.read() != -1) {
				// go through the input stream until EOF to compute MD5 checksum.
			}
		} finally {
			downloadedInputStream.close();
		}

		byte[] downloadedMd5Checksum = md.digest();

		assertEquals(DigestUtils.md5DigestAsHex(originalMd5Checksum), DigestUtils.md5DigestAsHex(downloadedMd5Checksum));
		this.createdObjects.add("test-file.jpg");
	}


	//Cleans up the bucket. Because if the bucket is not cleaned up, then the bucket will not be deleted after the test run.
	@After
	public void tearDown() {
		String bucketName = this.stackResourceRegistry.lookupPhysicalResourceId("EmptyBucket");
		for (String createdObject : this.createdObjects) {
			this.amazonS3.deleteObject(bucketName, createdObject);
		}

	}
}