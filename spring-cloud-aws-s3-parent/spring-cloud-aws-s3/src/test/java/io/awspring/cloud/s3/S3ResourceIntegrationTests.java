/*
 * Copyright 2013-2022 the original author or authors.
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazonaws.auth.AWSCredentials;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;

/**
 * Integration tests for {@link S3Resource}.
 *
 * @author Maciej Walkowiak
 */
@Testcontainers
class S3ResourceIntegrationTests {

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.0")).withServices(Service.S3).withReuse(true);

	private static S3Client client;

	@BeforeAll
	static void beforeAll() {
		// region and credentials are irrelevant for test, but must be added to make
		// test work on environments without AWS cli configured
		AWSCredentials localstackCredentials = localstack.getDefaultCredentialsProvider().getCredentials();
		StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials
				.create(localstackCredentials.getAWSAccessKeyId(), localstackCredentials.getAWSSecretKey()));
		client = S3Client.builder().region(Region.of(localstack.getRegion())).credentialsProvider(credentialsProvider)
				.endpointOverride(localstack.getEndpointOverride(Service.S3)).build();
		client.createBucket(request -> request.bucket("first-bucket"));
	}

	@Test
	void readsFileFromS3() throws IOException {
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString("test-file-content"));

		S3Resource resource = s3Resource("s3://first-bucket/test-file.txt");
		String content = retrieveContent(resource);
		assertThat(content).isEqualTo("test-file-content");
	}

	@Test
	void existsReturnsTrueWhenKeyExists() {
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString("test-file-content"));
		S3Resource resource = s3Resource("s3://first-bucket/test-file.txt");
		assertThat(resource.exists()).isTrue();
	}

	@Test
	void existsReturnsFalseWhenObjectDoesNotExist() {
		S3Resource resource = s3Resource("s3://first-bucket/non-existing-file.txt");
		assertThat(resource.exists()).isFalse();
	}

	@Test
	void objectHasContentLength() throws IOException {
		String contents = "test-file-content";
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString(contents));
		S3Resource resource = s3Resource("s3://first-bucket/test-file.txt");
		assertThat(resource.contentLength()).isEqualTo(contents.length());
	}

	@Test
	void contentLengthThrowsWhenResourceDoesNotExist() {
		S3Resource resource = s3Resource("s3://first-bucket/non-existing-file.txt");
		assertThatThrownBy(resource::contentLength).isInstanceOf(NoSuchKeyException.class);
	}

	@Test
	void returnsResourceUrl() throws IOException {
		S3Resource resource = s3Resource("s3://first-bucket/a-file.txt");
		assertThat(resource.getURL().toString()).isEqualTo("https://first-bucket.s3.amazonaws.com/a-file.txt");
	}

	@Test
	void returnsEncodedResourceUrlAndUri() throws IOException, URISyntaxException {
		S3Resource resource = s3Resource("s3://first-bucket/some/[objectName]");
		assertThat(resource.getURL().toString())
				.isEqualTo("https://first-bucket.s3.amazonaws.com/some%2F%5BobjectName%5D");
		assertThat(resource.getURI())
				.isEqualTo(new URI("https://first-bucket.s3.amazonaws.com/some%2F%5BobjectName%5D"));
	}

	@Test
	void resourceIsWritableWithDiskBuffering() throws IOException {
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString("test-file-content"));
		S3Resource resource = s3Resource("s3://first-bucket/test-file.txt",
				new DiskBufferingS3OutputStreamProvider(client));

		try (OutputStream outputStream = resource.getOutputStream()) {
			outputStream.write("overwritten with buffering".getBytes(StandardCharsets.UTF_8));
		}
		assertThat(retrieveContent(resource)).isEqualTo("overwritten with buffering");
	}

	@Test
	void objectMetadataCanBeSetOnWriting() throws IOException {
		S3Resource resource = s3Resource("s3://first-bucket/new-file.txt",
				new DiskBufferingS3OutputStreamProvider(client));

		ObjectMetadata objectMetadata = ObjectMetadata.builder().storageClass(StorageClass.ONEZONE_IA.name())
				.metadata("key", "value").contentLanguage("en").build();
		resource.setObjectMetadata(objectMetadata);

		try (OutputStream outputStream = resource.getOutputStream()) {
			outputStream.write("content".getBytes(StandardCharsets.UTF_8));
		}
		GetObjectResponse result = client
				.getObject(request -> request.bucket("first-bucket").key("new-file.txt").build()).response();
		assertThat(result.storageClass()).isEqualTo(StorageClass.ONEZONE_IA);
		assertThat(result.contentLanguage()).isEqualTo("en");
		assertThat(result.metadata()).containsEntry("key", "value");
	}

	@NotNull
	private S3Resource s3Resource(String location) {
		return new S3Resource(location, client, new DiskBufferingS3OutputStreamProvider(client));
	}

	@NotNull
	private S3Resource s3Resource(String location, S3OutputStreamProvider s3OutputStreamProvider) {
		return new S3Resource(location, client, s3OutputStreamProvider);
	}

	@NotNull
	private String retrieveContent(S3Resource resource) throws IOException {
		return new BufferedReader(new InputStreamReader(resource.getInputStream())).lines()
				.collect(Collectors.joining("\n"));
	}

	static class Person {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "Person{" + "name='" + name + '\'' + '}';
		}
	}

}
