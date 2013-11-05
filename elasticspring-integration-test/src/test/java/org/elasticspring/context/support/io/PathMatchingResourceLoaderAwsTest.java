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
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.elasticspring.support.TestStackEnvironment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

/**
 * @author Alain Sahli
 * @since 1.0
 */
@SuppressWarnings("SpringJavaAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("PathMatchingResourceLoaderAwsTest-context.xml")
public class PathMatchingResourceLoaderAwsTest {

	@Autowired
	private ResourcePatternResolver resourceLoader;

	@Autowired
	private AmazonS3 amazonS3;

	@Autowired
	private TestStackEnvironment testStackEnvironment;

	private final CompletionService<String> completionService = new ExecutorCompletionService<String>(Executors.newFixedThreadPool(10));

	private static final List<String> FILES_FOR_HIERARCHY = Arrays.asList("foo1/bar1/baz1/test1.txt", "foo1/bar1/test1.txt",
			"foo1/test1.txt", "test1.txt", "foo2/bar2/test2.txt", "foo2/bar2/baz2/test2.txt");

	@Test
	public void testWildcardsInKey() throws Exception {
		String bucketName = this.testStackEnvironment.getByLogicalId("PathMatcherBucket");
		createTestFiles(bucketName);

		String protocolAndBucket = "s3://" + bucketName;
		try {
			Assert.assertEquals("test the '?' wildcard", 1, this.resourceLoader.getResources(protocolAndBucket + "/foo1/bar?/test1.txt").length);
			Assert.assertEquals("test the '*' wildcard", 1, this.resourceLoader.getResources(protocolAndBucket + "/foo*/bar2/test2.txt").length);
			Assert.assertEquals("test the '**' wildcard", 4, this.resourceLoader.getResources(protocolAndBucket + "/**/test1.txt").length);
			Assert.assertEquals("test a mix of '**' and '?'", 6, this.resourceLoader.getResources(protocolAndBucket + "/**/test?.txt").length);
			Assert.assertEquals("test all together", 2, this.resourceLoader.getResources(protocolAndBucket + "/**/baz*/test?.txt").length);
		} finally {
			deleteTestFiles(bucketName);
		}
	}

	@Test
	public void testWildcardsInBucket() throws Exception {
		String firstBucket = this.testStackEnvironment.getByLogicalId("PathMatcherBucket01");
		String secondBucket = this.testStackEnvironment.getByLogicalId("PathMatcherBucket02");
		String thirdBucket = this.testStackEnvironment.getByLogicalId("PathMatcherBucket03");
		String bucketPrefix = firstBucket.substring(0, firstBucket.lastIndexOf("-") - 2);
		try {
			createTestFiles(firstBucket, secondBucket, thirdBucket);
			Assert.assertEquals("test the '?' wildcard", 1, this.resourceLoader.getResources("s3://" + bucketPrefix + "??" + firstBucket.substring(firstBucket.lastIndexOf("-")) + "/test1.txt").length);
			Assert.assertEquals("test the '*' wildcard", 3, this.resourceLoader.getResources("s3://" + bucketPrefix + "*/test1.txt").length);
			Assert.assertEquals("test the '**' wildcard", 4 * 3, this.resourceLoader.getResources("s3://**/test1.txt").length);
		} finally {
			deleteTestFiles(firstBucket, secondBucket, thirdBucket);
		}
	}

	private void createTestFiles(String... bucketNames) throws InterruptedException {
		int createdFiles = 0;
		for (String bucketName : bucketNames) {
			for (String fileName : FILES_FOR_HIERARCHY) {
				this.completionService.submit(new CreateFileCallable(bucketName, fileName, this.amazonS3));
				createdFiles++;
			}
		}

		for (int i = 0; i < createdFiles; i++) {
			this.completionService.take();
		}
	}

	private void deleteTestFiles(String... bucketNames) throws InterruptedException {
		int createdFiles = 0;
		for (String bucketName : bucketNames) {
			for (String fileName : FILES_FOR_HIERARCHY) {
				this.completionService.submit(new DeleteFileCallable(bucketName, fileName, this.amazonS3));
				createdFiles++;
			}
		}
		for (int i = 0; i < createdFiles; i++) {
			this.completionService.take();
		}
	}

	private static class CreateFileCallable implements Callable<String> {

		private final String fileName;
		private final AmazonS3 amazonS3;
		private final String bucketName;

		private CreateFileCallable(String bucketName, String fileName, AmazonS3 amazonS3) {
			this.fileName = fileName;
			this.amazonS3 = amazonS3;
			this.bucketName = bucketName;
		}


		@Override
		public String call() throws Exception {
			this.amazonS3.putObject(this.bucketName, this.fileName, new ByteArrayInputStream(this.fileName.getBytes()), new ObjectMetadata());
			return this.fileName;
		}
	}

	private static class DeleteFileCallable implements Callable<String> {

		private final String fileName;
		private final AmazonS3 amazonS3;
		private final String bucketName;

		private DeleteFileCallable(String bucketName, String fileName, AmazonS3 amazonS3) {
			this.fileName = fileName;
			this.amazonS3 = amazonS3;
			this.bucketName = bucketName;
		}


		@Override
		public String call() throws Exception {
			this.amazonS3.deleteObject(this.bucketName, this.fileName);
			return this.fileName;
		}
	}
}