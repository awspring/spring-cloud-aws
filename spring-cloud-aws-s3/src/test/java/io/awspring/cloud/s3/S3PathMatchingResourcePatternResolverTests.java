/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * @author Tobias Soloschenko
 * @author Artem Bilan
 */
class S3PathMatchingResourcePatternResolverTests implements LocalstackContainerTest {

	private static final RequestBody requestBody = RequestBody.fromString("test-file-content");

	private static ResourcePatternResolver resourceLoader;

	@BeforeAll
	static void beforeAll() {
		S3Client client = LocalstackContainerTest.s3Client();

		// prepare buckets and objects for tests
		client.createBucket(request -> request.bucket("my-bucket"));
		client.createBucket(request -> request.bucket("my-bucket2"));
		client.createBucket(request -> request.bucket("my-buckettwo"));

		client.putObject(PutObjectRequest.builder().bucket("my-bucket").key("test.txt").build(), requestBody);
		client.putObject(PutObjectRequest.builder().bucket("my-bucket").key("test2.txt").build(), requestBody);
		client.putObject(PutObjectRequest.builder().bucket("my-bucket").key("test3.txt").build(), requestBody);
		client.putObject(PutObjectRequest.builder().bucket("my-bucket").key("foo-test/test.txt").build(), requestBody);
		client.putObject(PutObjectRequest.builder().bucket("my-bucket").key("foo-test/test2.txt").build(), requestBody);
		client.putObject(PutObjectRequest.builder().bucket("my-bucket").key("bar-test/test.txt").build(), requestBody);
		client.putObject(PutObjectRequest.builder().bucket("my-bucket").key("nested/foo-test/test.txt").build(),
				requestBody);
		client.putObject(PutObjectRequest.builder().bucket("my-bucket").key("nested/foo-test/test2.txt").build(),
				requestBody);
		client.putObject(PutObjectRequest.builder().bucket("my-bucket").key("nested/bar-test/test.txt").build(),
				requestBody);

		client.putObject(PutObjectRequest.builder().bucket("my-bucket2").key("test.txt").build(), requestBody);
		client.putObject(PutObjectRequest.builder().bucket("my-bucket2").key("nested/foo-test/test.txt").build(),
				requestBody);

		client.putObject(PutObjectRequest.builder().bucket("my-buckettwo").key("test.txt").build(), requestBody);

		resourceLoader = getResourceLoader(client);
	}

	@Test
	void loadsObjectsWithWildcardsInBucketNames() throws Exception {
		assertThat(resourceLoader.getResources("s3://my-bucket*/test.txt").length).as("test the single '*' wildcard")
				.isEqualTo(3);
		assertThat(resourceLoader.getResources("s3://my-bucket?wo/test.txt").length).as("test the '?' wildcard")
				.isEqualTo(1);
		assertThat(resourceLoader.getResources("s3://**/test.txt").length).as("test the double '**' wildcard")
				.isEqualTo(3);
	}

	@Test
	void loadsObjectsWithWildcardsInBucketNamesAndKeys() throws IOException {
		assertThat(resourceLoader.getResources("s3://my-bucket/test*.txt").length).as("test the single '*' wildcard")
				.isEqualTo(3);
		assertThat(resourceLoader.getResources("s3://my-bucket/test?.txt").length).as("test the single '?' wildcard")
				.isEqualTo(2);
		assertThat(resourceLoader.getResources("s3://my-bucket/foo-test/test*.txt").length)
				.as("test the single '*' wildcard in nested folder").isEqualTo(2);
		assertThat(resourceLoader.getResources("s3://my-bucket/*-test/test.txt").length)
				.as("test the single '*' wildcard in the folder name").isEqualTo(2);
		assertThat(resourceLoader.getResources("s3://my-bucket/*-test/test*.txt").length)
				.as("test the single '*' wildcard in folder name and file name").isEqualTo(3);
		assertThat(resourceLoader.getResources("s3://my-bucket/*-test/test?.txt").length)
				.as("test the single '*' wildcard in folder name and '?' in file name").isEqualTo(1);
		assertThat(resourceLoader.getResources("s3://my-bucket/**/*-test/test?.txt").length)
				.as("test the '**' with '*' wildcard in folder name and '?' in file name").isEqualTo(2);
		assertThat(resourceLoader.getResources("s3://my-bucket/**/test?.txt").length)
				.as("test the '**' in folder name and '?' in file name").isEqualTo(4);
		assertThat(resourceLoader.getResources("s3://my-bucket*/**/test?.txt").length)
				.as("test the '*' in bucket name, '**' in folder name and '?' in file name").isEqualTo(4);
	}

	@Test
	void loadsClasspathFileWithoutWildcards() throws Exception {
		Resource[] resources = resourceLoader
				.getResources("classpath*:io/awspring/cloud/s3/S3PathMatchingResourcePatternResolverTests.class");
		assertThat(resources.length).isEqualTo(1);
		assertThat(resources[0].exists()).isTrue();
	}

	@Test
	void loadsClasspathFileWithWildcards() throws Exception {
		Resource[] resourcesWithFileNameWildcard = resourceLoader
				.getResources("classpath*:io/**/S3PathMatchingResourcePatternResolverTest?.class");
		assertThat(resourcesWithFileNameWildcard.length).isEqualTo(1);
		assertThat(resourcesWithFileNameWildcard[0].exists()).isTrue();
	}

	private static ResourcePatternResolver getResourceLoader(S3Client s3Client) {
		DefaultResourceLoader loader = new DefaultResourceLoader();
		loader.addProtocolResolver(new S3ProtocolResolver(s3Client));
		return new S3PathMatchingResourcePatternResolver(s3Client, new PathMatchingResourcePatternResolver(loader));
	}

}
