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

package org.elasticspring.context.support.io;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Region;
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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

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

	private final CompletionService<String> completionService = new ExecutorCompletionService<String>(Executors.newSingleThreadExecutor());

	private static final String DEFAULT_FILENAME = "test.txt";

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
			while (downloadedInputStream.read(buffer) != -1) {
				// go through the input stream until EOF to compute MD5 checksum.
				// Dummy operation to avoid checkstyle error
				int a = 1;
			}
		} finally {
			downloadedInputStream.close();
		}

		byte[] downloadedMd5Checksum = md.digest();

		Assert.assertEquals(DigestUtils.md5DigestAsHex(originalMd5Checksum), DigestUtils.md5DigestAsHex(downloadedMd5Checksum));
		this.createdObjects.add("test-file.pdf");
	}

	@Test
	public void testBucketNamesWithDotsOnAllS3Regions() throws Exception {
		List<String> createdBuckets = null;
		try {
			createdBuckets = createBuckets(".");

			for (String bucketName : createdBuckets) {
				assertBucketContent(bucketName);
			}
		} finally {
			deleteBucket(createdBuckets);
		}
	}

	@Test
	public void testBucketNamesWithoutDotsOnAllS3Regions() throws Exception {
		List<String> createdBuckets = null;
		try {
			createdBuckets = createBuckets("-");

			for (String bucketName : createdBuckets) {
				assertBucketContent(bucketName);
			}
		} finally {
			deleteBucket(createdBuckets);
		}
	}


	private void assertBucketContent(String bucketName) throws IOException {
		InputStream inputStream = null;
		try {
			Resource resource = this.resourceLoader.getResource(S3_PREFIX + bucketName + "/test.txt");
			inputStream = resource.getInputStream();
			Assert.assertTrue(resource.contentLength() > 0);
			Assert.assertNotNull(inputStream);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}


	private List<String> createBuckets(String separator) throws Exception {
		List<String> createdBuckets = new ArrayList<String>(Region.values().length);
		for (Region region : Region.values()) {
			String bucketName = "test" + separator + "elasticspring" + separator + UUID.randomUUID().toString().replace("-", separator);
			this.completionService.submit(new CreateBucketCallable(region, this.amazonS3, bucketName));
		}

		for (Region ignore : Region.values()) {
			createdBuckets.add(this.completionService.take().get());
		}

		return createdBuckets;
	}

	private void deleteBucket(List<String> bucketNames) throws InterruptedException {
		if (bucketNames == null) {
			return;
		}
		for (String bucketName : bucketNames) {
			this.completionService.submit(new DeleteBucketCallable(this.amazonS3, bucketName));
		}

		for (String ignore : bucketNames) {
			this.completionService.take();
		}
	}


	//Cleans up the bucket. Because if the bucket is not cleaned up, then the bucket will not be deleted after the test run.
	@After
	public void tearDown() {
		String bucketName = this.testStackEnvironment.getByLogicalId("EmptyBucket");
		for (String createdObject : this.createdObjects) {
			this.amazonS3.deleteObject(bucketName, createdObject);
		}

	}

	private static class CreateBucketCallable implements Callable<String> {

		private final Region region;
		private final AmazonS3 amazonS3;
		private final String bucketName;

		private CreateBucketCallable(Region region, AmazonS3 amazonS3, String bucketName) {
			this.region = region;
			this.amazonS3 = amazonS3;
			this.bucketName = bucketName;
		}

		@Override
		public String call() throws Exception {
			Bucket bucket = this.amazonS3.createBucket(this.bucketName, this.region);
			ObjectMetadata objectMetadata = new ObjectMetadata();
			objectMetadata.setContentLength(this.region.name().getBytes().length);
			this.amazonS3.putObject(this.bucketName, DEFAULT_FILENAME, new ByteArrayInputStream(this.region.name().getBytes()), objectMetadata);
			return bucket.getName();
		}
	}

	private static class DeleteBucketCallable implements Callable<String> {

		private final AmazonS3 amazonS3;
		private final String bucketName;

		private DeleteBucketCallable(AmazonS3 amazonS3, String bucketName) {
			this.amazonS3 = amazonS3;
			this.bucketName = bucketName;
		}

		@Override
		public String call() throws Exception {
			this.amazonS3.deleteObject(this.bucketName, DEFAULT_FILENAME);
			this.amazonS3.deleteBucket(this.bucketName);
			return this.bucketName;
		}
	}

}