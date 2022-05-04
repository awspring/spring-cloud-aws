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
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
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
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * Integration tests for {@link S3Resource}.
 *
 * @author Maciej Walkowiak
 * @author Anton Perez
 */
@Testcontainers
class S3ResourceIntegrationTests {

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.2")).withServices(Service.S3).withReuse(true);

	private static S3Client client;
	private static S3TransferManager s3TransferManager;

	// Required for the @S3ResourceIntegrationTest annotation
	private static Stream<S3OutputStreamProvider> availableS3OutputStreamProviders() {
		return Stream.of(new DiskBufferingS3OutputStreamProvider(client, new PropertiesS3ObjectContentTypeResolver()),
				new TransferManagerS3OutputStreamProvider(s3TransferManager,
						new PropertiesS3ObjectContentTypeResolver()));
	}

	@BeforeAll
	static void beforeAll() {
		// region and credentials are irrelevant for test, but must be added to make
		// test work on environments without AWS cli configured
		AWSCredentials localstackCredentials = localstack.getDefaultCredentialsProvider().getCredentials();
		StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials
				.create(localstackCredentials.getAWSAccessKeyId(), localstackCredentials.getAWSSecretKey()));
		client = S3Client.builder().region(Region.of(localstack.getRegion())).credentialsProvider(credentialsProvider)
				.endpointOverride(localstack.getEndpointOverride(Service.S3)).build();
		s3TransferManager = S3TransferManager.builder()
				.s3ClientConfiguration(
						b -> b.region(Region.of(localstack.getRegion())).credentialsProvider(credentialsProvider)
								.endpointOverride(localstack.getEndpointOverride(Service.S3)).build())
				.build();
		client.createBucket(request -> request.bucket("first-bucket"));
	}

	@S3ResourceIntegrationTest
	void readsFileFromS3(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString("test-file-content"));

		S3Resource resource = s3Resource("s3://first-bucket/test-file.txt", s3OutputStreamProvider);
		String content = retrieveContent(resource);
		assertThat(content).isEqualTo("test-file-content");
	}

	@S3ResourceIntegrationTest
	void existsReturnsTrueWhenKeyExists(S3OutputStreamProvider s3OutputStreamProvider) {
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString("test-file-content"));
		S3Resource resource = s3Resource("s3://first-bucket/test-file.txt", s3OutputStreamProvider);
		assertThat(resource.exists()).isTrue();
	}

	@S3ResourceIntegrationTest
	void existsReturnsFalseWhenObjectDoesNotExist(S3OutputStreamProvider s3OutputStreamProvider) {
		S3Resource resource = s3Resource("s3://first-bucket/non-existing-file.txt", s3OutputStreamProvider);
		assertThat(resource.exists()).isFalse();
	}

	@S3ResourceIntegrationTest
	void objectHasContentLength(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		String contents = "test-file-content";
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString(contents));
		S3Resource resource = s3Resource("s3://first-bucket/test-file.txt", s3OutputStreamProvider);
		assertThat(resource.contentLength()).isEqualTo(contents.length());
	}

	@S3ResourceIntegrationTest
	void objectHasContentType(S3OutputStreamProvider s3OutputStreamProvider) {
		String contents = "{\"foo\":\"bar\"}";
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.json")
				.contentType("application/json").build(), RequestBody.fromString(contents));
		S3Resource resource = s3Resource("s3://first-bucket/test-file.json", s3OutputStreamProvider);
		assertThat(resource.contentType()).isEqualTo("application/json");
	}

	@S3ResourceIntegrationTest
	void contentLengthThrowsWhenResourceDoesNotExist(S3OutputStreamProvider s3OutputStreamProvider) {
		S3Resource resource = s3Resource("s3://first-bucket/non-existing-file.txt", s3OutputStreamProvider);
		assertThatThrownBy(resource::contentLength).isInstanceOf(NoSuchKeyException.class);
	}

	@S3ResourceIntegrationTest
	void returnsResourceUrl(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		S3Resource resource = s3Resource("s3://first-bucket/a-file.txt", s3OutputStreamProvider);
		assertThat(resource.getURL().toString()).isEqualTo("https://first-bucket.s3.amazonaws.com/a-file.txt");
	}

	@S3ResourceIntegrationTest
	void returnsEncodedResourceUrlAndUri(S3OutputStreamProvider s3OutputStreamProvider)
			throws IOException, URISyntaxException {
		S3Resource resource = s3Resource("s3://first-bucket/some/[objectName]", s3OutputStreamProvider);
		assertThat(resource.getURL().toString())
				.isEqualTo("https://first-bucket.s3.amazonaws.com/some/%5BobjectName%5D");
		assertThat(resource.getURI()).isEqualTo(new URI("https://first-bucket.s3.amazonaws.com/some/%5BobjectName%5D"));
	}

	@S3ResourceIntegrationTest
	void resourceIsWritableWithDiskBuffering(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString("test-file-content"));
		S3Resource resource = s3Resource("s3://first-bucket/test-file.txt", s3OutputStreamProvider);

		try (OutputStream outputStream = resource.getOutputStream()) {
			outputStream.write("overwritten with buffering".getBytes(StandardCharsets.UTF_8));
		}
		assertThat(retrieveContent(resource)).isEqualTo("overwritten with buffering");
	}

	@S3ResourceIntegrationTest
	void objectMetadataCanBeSetOnWriting(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		S3Resource resource = s3Resource("s3://first-bucket/new-file.txt", s3OutputStreamProvider);

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

	@S3ResourceIntegrationTest
	void contentTypeCanBeResolved(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		S3Resource resource = s3Resource("s3://first-bucket/new-file.txt", s3OutputStreamProvider);

		try (OutputStream outputStream = resource.getOutputStream()) {
			outputStream.write("content".getBytes(StandardCharsets.UTF_8));
		}
		GetObjectResponse result = client
				.getObject(request -> request.bucket("first-bucket").key("new-file.txt").build()).response();
		assertThat(result.contentType()).isEqualTo("text/plain");
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
