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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.io.Files;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
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
	private static final int DEFAULT_PART_SIZE = 5242880;

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:4.4.0"));

	private static S3Client client;
	private static S3AsyncClient asyncClient;
	private static S3TransferManager s3TransferManager;

	// Required for the @TestAvailableOutputStreamProviders annotation
	private static Stream<S3OutputStreamProvider> availableS3OutputStreamProviders() {
		return Stream.of(new DiskBufferingS3OutputStreamProvider(client, new PropertiesS3ObjectContentTypeResolver()),
				new TransferManagerS3OutputStreamProvider(s3TransferManager,
						new PropertiesS3ObjectContentTypeResolver()),
				new InMemoryBufferingS3OutputStreamProvider(client, new PropertiesS3ObjectContentTypeResolver()));
	}

	@BeforeAll
	static void beforeAll() {
		// region and credentials are irrelevant for test, but must be added to make
		// test work on environments without AWS cli configured
		StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider
				.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));
		asyncClient = S3AsyncClient.builder().region(Region.of(localstack.getRegion()))
				.credentialsProvider(credentialsProvider).endpointOverride(localstack.getEndpoint()).build();
		client = S3Client.builder().region(Region.of(localstack.getRegion())).credentialsProvider(credentialsProvider)
				.endpointOverride(localstack.getEndpoint()).build();
		s3TransferManager = S3TransferManager.builder().s3Client(asyncClient).build();
		client.createBucket(request -> request.bucket("first-bucket"));
	}

	@TestAvailableOutputStreamProviders
	void readsFileFromS3(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString("test-file-content"));

		S3Resource resource = s3Resource("s3://first-bucket/test-file.txt", s3OutputStreamProvider);
		String content = retrieveContent(resource);
		assertThat(content).isEqualTo("test-file-content");
	}

	@TestAvailableOutputStreamProviders
	void existsReturnsTrueWhenKeyExists(S3OutputStreamProvider s3OutputStreamProvider) {
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString("test-file-content"));
		S3Resource resource = s3Resource("s3://first-bucket/test-file.txt", s3OutputStreamProvider);
		assertThat(resource.exists()).isTrue();
	}

	@TestAvailableOutputStreamProviders
	void existsReturnsFalseWhenObjectDoesNotExist(S3OutputStreamProvider s3OutputStreamProvider) {
		S3Resource resource = s3Resource("s3://first-bucket/non-existing-file.txt", s3OutputStreamProvider);
		assertThat(resource.exists()).isFalse();
	}

	@TestAvailableOutputStreamProviders
	void objectHasContentLength(S3OutputStreamProvider s3OutputStreamProvider) {
		String contents = "test-file-content";
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString(contents));
		S3Resource resource = s3Resource("s3://first-bucket/test-file.txt", s3OutputStreamProvider);
		assertThat(resource.contentLength()).isEqualTo(contents.length());
	}

	@TestAvailableOutputStreamProviders
	void objectHasContentType(S3OutputStreamProvider s3OutputStreamProvider) {
		String contents = "{\"foo\":\"bar\"}";
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.json")
				.contentType("application/json").build(), RequestBody.fromString(contents));
		S3Resource resource = s3Resource("s3://first-bucket/test-file.json", s3OutputStreamProvider);
		assertThat(resource.contentType()).isEqualTo("application/json");
	}

	@TestAvailableOutputStreamProviders
	void contentLengthThrowsWhenResourceDoesNotExist(S3OutputStreamProvider s3OutputStreamProvider) {
		S3Resource resource = s3Resource("s3://first-bucket/non-existing-file.txt", s3OutputStreamProvider);
		assertThatThrownBy(resource::contentLength).isInstanceOf(NoSuchKeyException.class);
	}

	@TestAvailableOutputStreamProviders
	void returnsResourceUrl(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		S3Resource resource = s3Resource("s3://first-bucket/a-file.txt", s3OutputStreamProvider);
		assertThat(resource.getURL().toString())
				.isEqualTo("http://127.0.0.1:" + localstack.getFirstMappedPort() + "/first-bucket/a-file.txt");
	}

	@TestAvailableOutputStreamProviders
	void returnsEmptyUrlToBucketWhenObjectIsEmpty(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		S3Resource resource = s3Resource("s3://first-bucket/", s3OutputStreamProvider);
		assertThat(resource.getURL().toString()).isEqualTo("https://first-bucket.s3.amazonaws.com/");
	}

	@TestAvailableOutputStreamProviders
	void returnsEncodedResourceUrlAndUri(S3OutputStreamProvider s3OutputStreamProvider)
			throws IOException, URISyntaxException {
		S3Resource resource = s3Resource("s3://first-bucket/some/[objectName]", s3OutputStreamProvider);
		assertThat(resource.getURL().toString()).isEqualTo(
				"http://127.0.0.1:" + localstack.getFirstMappedPort() + "/first-bucket/some/%5BobjectName%5D");
		assertThat(resource.getURI()).isEqualTo(
				new URI("http://127.0.0.1:" + localstack.getFirstMappedPort() + "/first-bucket/some/%5BobjectName%5D"));
	}

	@TestAvailableOutputStreamProviders
	void resourceIsWritableWithDiskBuffering(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString("test-file-content"));
		S3Resource resource = s3Resource("s3://first-bucket/test-file.txt", s3OutputStreamProvider);

		try (OutputStream outputStream = resource.getOutputStream()) {
			outputStream.write("overwritten with buffering".getBytes(StandardCharsets.UTF_8));
		}
		assertThat(retrieveContent(resource)).isEqualTo("overwritten with buffering");
	}

	@TestAvailableOutputStreamProviders
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

	@TestAvailableOutputStreamProviders
	void contentTypeCanBeResolvedForLargeFiles(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		int i = new Random().nextInt();
		S3Resource resource = s3Resource("s3://first-bucket/new-file" + i + ".txt", s3OutputStreamProvider);

		// create file larger than single part size in multipart upload to make sure that file can be successfully
		// uploaded in parts
		File file = File.createTempFile("s3resource", "test");
		byte[] b = new byte[DEFAULT_PART_SIZE * 2];
		new Random().nextBytes(b);
		Files.write(b, file);

		try (OutputStream outputStream = resource.getOutputStream()) {
			outputStream.write(Files.toByteArray(file));
		}
		GetObjectResponse result = client
				.getObject(request -> request.bucket("first-bucket").key("new-file" + i + ".txt").build()).response();
		assertThat(result.contentType()).isEqualTo("text/plain");
	}

	@TestAvailableOutputStreamProviders
	void contentLengthCanBeSetForLargeFiles(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		int i = new Random().nextInt();
		S3Resource resource = s3Resource("s3://first-bucket/new-file" + i + ".txt", s3OutputStreamProvider);
		int fileSize = DEFAULT_PART_SIZE * 2;
		resource.setObjectMetadata(ObjectMetadata.builder().contentLength((long) fileSize).build());

		// create file larger than single part size in multipart upload to make sure that file can be successfully
		// uploaded in parts
		File file = File.createTempFile("s3resource", "test");
		byte[] b = new byte[fileSize];
		new Random().nextBytes(b);
		Files.write(b, file);

		try (OutputStream outputStream = resource.getOutputStream()) {
			outputStream.write(Files.toByteArray(file));
		}
		GetObjectResponse result = client
				.getObject(request -> request.bucket("first-bucket").key("new-file" + i + ".txt").build()).response();
		assertThat(result.contentType()).isEqualTo("text/plain");
	}

	@TestAvailableOutputStreamProviders
	void contentTypeCanBeResolvedForSmallFiles(S3OutputStreamProvider s3OutputStreamProvider) throws IOException {
		S3Resource resource = s3Resource("s3://first-bucket/new-file.txt", s3OutputStreamProvider);

		try (OutputStream outputStream = resource.getOutputStream()) {
			outputStream.write("content".getBytes(StandardCharsets.UTF_8));
		}

		GetObjectResponse result = client
				.getObject(request -> request.bucket("first-bucket").key("new-file.txt").build()).response();
		assertThat(result.contentType()).isEqualTo("text/plain");
	}

	@Test
	void retrievesMetadata() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("key", "keyValue");
		metadata.put("camelCaseKey", "camelCaseKeyValue");

		client.putObject(r -> r.bucket("first-bucket").key("metadata.txt").metadata(metadata),
				RequestBody.fromString("hello"));

		S3Resource resource = s3Resource("s3://first-bucket/metadata.txt",
				new InMemoryBufferingS3OutputStreamProvider(client, new PropertiesS3ObjectContentTypeResolver()));

		assertThat(resource.metadata()).containsEntry("key", "keyValue")
				// retrieved as lower case
				.containsEntry("camelcasekey", "camelCaseKeyValue").doesNotContainKey("camelCaseKey");
	}

	@Test
	void returnsLocationObject() {
		S3Resource resource = s3Resource("s3://first-bucket/new-file.txt",
				new InMemoryBufferingS3OutputStreamProvider(client, new PropertiesS3ObjectContentTypeResolver()));
		Location location = resource.getLocation();
		assertThat(location).isNotNull();
		assertThat(location.getBucket()).isEqualTo("first-bucket");
		assertThat(location.getObject()).isEqualTo("new-file.txt");
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

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ParameterizedTest
	@MethodSource("availableS3OutputStreamProviders")
	@interface TestAvailableOutputStreamProviders {
	}
}
