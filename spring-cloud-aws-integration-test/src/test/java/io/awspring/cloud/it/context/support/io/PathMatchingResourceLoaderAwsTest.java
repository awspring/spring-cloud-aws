/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.it.context.support.io;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;
import org.springframework.cloud.aws.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.springframework.cloud.aws.it.AWSIntegration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alain Sahli
 * @author Agim Emruli
 * @since 1.0
 */
@SuppressWarnings("SpringJavaAutowiringInspection")
@ExtendWith(SpringExtension.class)
@AWSIntegration
abstract class PathMatchingResourceLoaderAwsTest {

	private static final List<String> FILES_FOR_HIERARCHY = Arrays.asList("foo1/bar1/baz1/test1.txt",
			"foo1/bar1/test1.txt", "foo1/test1.txt", "test1.txt", "foo2/bar2/test2.txt", "foo2/bar2/baz2/test2.txt");

	private final ExecutorService executor = Executors.newFixedThreadPool(10);

	private ResourcePatternResolver resourceLoader;

	@Autowired
	private AmazonS3 amazonS3;

	@Autowired
	private StackResourceRegistry stackResourceRegistry;

	@Autowired
	public void setupResolver(ApplicationContext applicationContext, AmazonS3 amazonS3) {
		this.resourceLoader = new PathMatchingSimpleStorageResourcePatternResolver(amazonS3, applicationContext);
	}

	@Test
	void testWildcardsInKey() throws Exception {
		String bucketName = this.stackResourceRegistry.lookupPhysicalResourceId("PathMatcherBucket");
		createTestFiles(bucketName);

		String protocolAndBucket = "s3://" + bucketName;
		try {
			assertThat(this.resourceLoader.getResources(protocolAndBucket + "/foo1/bar?/test1.txt").length)
					.as("test the '?' wildcard").isEqualTo(1);
			assertThat(this.resourceLoader.getResources(protocolAndBucket + "/foo*/bar2/test2.txt").length)
					.as("test the '*' wildcard").isEqualTo(1);
			assertThat(this.resourceLoader.getResources(protocolAndBucket + "/**/test1.txt").length)
					.as("test the '**' wildcard").isEqualTo(4);
			assertThat(this.resourceLoader.getResources(protocolAndBucket + "/**/test?.txt").length)
					.as("test a mix of '**' and '?'").isEqualTo(6);
			assertThat(this.resourceLoader.getResources(protocolAndBucket + "/**/baz*/test?.txt").length)
					.as("test all together").isEqualTo(2);
		}
		finally {
			deleteTestFiles(bucketName);
		}
	}

	@Test
	void testWildcardsInBucket() throws Exception {
		String firstBucket = this.stackResourceRegistry.lookupPhysicalResourceId("PathMatcherBucket01");
		String secondBucket = this.stackResourceRegistry.lookupPhysicalResourceId("PathMatcherBucket02");
		String thirdBucket = this.stackResourceRegistry.lookupPhysicalResourceId("PathMatcherBucket03");
		String bucketPrefix = firstBucket.substring(0, firstBucket.lastIndexOf("-") - 2);
		try {
			createTestFiles(firstBucket, secondBucket, thirdBucket);
			assertThat(this.resourceLoader.getResources("s3://" + bucketPrefix + "??"
					+ firstBucket.substring(firstBucket.lastIndexOf("-")) + "/test1.txt").length)
							.as("test the '?' wildcard").isEqualTo(1);
			assertThat(this.resourceLoader.getResources("s3://" + bucketPrefix + "*/test1.txt").length)
					.as("test the '*' wildcard").isEqualTo(3);
			assertThat(this.resourceLoader.getResources("s3://**/test1.txt").length).as("test the '**' wildcard")
					.isEqualTo(4 * 3);
		}
		finally {
			deleteTestFiles(firstBucket, secondBucket, thirdBucket);
		}
	}

	private void createTestFiles(String... bucketNames) throws InterruptedException {
		List<CreateFileCallable> callables = new ArrayList<>();

		for (String bucketName : bucketNames) {
			for (String fileName : FILES_FOR_HIERARCHY) {
				callables.add(new CreateFileCallable(bucketName, fileName, this.amazonS3));
			}
		}
		this.executor.invokeAll(callables);
	}

	private void deleteTestFiles(String... bucketNames) throws InterruptedException {
		List<DeleteFileCallable> deleteFileCallables = new ArrayList<>();
		for (String bucketName : bucketNames) {
			for (String fileName : FILES_FOR_HIERARCHY) {
				deleteFileCallables.add(new DeleteFileCallable(bucketName, fileName, this.amazonS3));
			}
		}
		this.executor.invokeAll(deleteFileCallables);
	}

	private static final class CreateFileCallable implements Callable<String> {

		private final String fileName;

		private final AmazonS3 amazonS3;

		private final String bucketName;

		private CreateFileCallable(String bucketName, String fileName, AmazonS3 amazonS3) {
			this.fileName = fileName;
			this.amazonS3 = amazonS3;
			this.bucketName = bucketName;
		}

		@Override
		public String call() {
			this.amazonS3.putObject(this.bucketName, this.fileName, new ByteArrayInputStream(this.fileName.getBytes()),
					new ObjectMetadata());
			return this.fileName;
		}

	}

	private static final class DeleteFileCallable implements Callable<String> {

		private final String fileName;

		private final AmazonS3 amazonS3;

		private final String bucketName;

		private DeleteFileCallable(String bucketName, String fileName, AmazonS3 amazonS3) {
			this.fileName = fileName;
			this.amazonS3 = amazonS3;
			this.bucketName = bucketName;
		}

		@Override
		public String call() {
			this.amazonS3.deleteObject(this.bucketName, this.fileName);
			return this.fileName;
		}

	}

}
