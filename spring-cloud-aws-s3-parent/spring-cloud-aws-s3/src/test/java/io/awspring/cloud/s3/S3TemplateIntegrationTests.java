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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.amazonaws.auth.AWSCredentials;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Integration tests for {@link S3Template}.
 *
 * @author Maciej Walkowiak
 * @author Yuki Yoshida
 */
@Testcontainers
class S3TemplateIntegrationTests {

	private static final String BUCKET_NAME = "test-bucket";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.2")).withServices(LocalStackContainer.Service.S3)
					.withReuse(true);

	private static S3Client client;

	private S3Template s3Template;

	@BeforeAll
	static void beforeAll() {
		// region and credentials are irrelevant for test, but must be added to make
		// test work on environments without AWS cli configured
		AWSCredentials localstackCredentials = localstack.getDefaultCredentialsProvider().getCredentials();
		StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials
				.create(localstackCredentials.getAWSAccessKeyId(), localstackCredentials.getAWSSecretKey()));
		client = S3Client.builder().region(Region.of(localstack.getRegion())).credentialsProvider(credentialsProvider)
				.endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3)).build();
	}

	@BeforeEach
	void init() {
		this.s3Template = new S3Template(client,
				new DiskBufferingS3OutputStreamProvider(client, new PropertiesS3ObjectContentTypeResolver()),
				new Jackson2JsonS3ObjectConverter(new ObjectMapper()));

		client.createBucket(r -> r.bucket("test-bucket"));
	}

	@AfterEach
	void destroyBuckets() {
		client.listBuckets().buckets().forEach(b -> {
			client.listObjects(r -> r.bucket(b.name())).contents()
					.forEach(s3Object -> client.deleteObject(r -> r.bucket(b.name()).key(s3Object.key())));
			client.deleteBucket(r -> r.bucket(b.name()));
		});
	}

	@Test
	void createsBucket() {
		String location = s3Template.createBucket("test-bucket");

		assertThat(location).isNotNull();
		assertThat(client.listBuckets()).satisfies(r -> this.bucketExists(r, "test-bucket"));
	}

	@Test
	void deletesBucket() {
		client.createBucket(r -> r.bucket("test-bucket"));
		assertThat(client.listBuckets()).satisfies(r -> this.bucketExists(r, "test-bucket"));

		s3Template.deleteBucket("test-bucket");

		assertThat(client.listBuckets()).satisfies(r -> this.bucketDoesNotExist(r, "test-bucket"));
	}

	@Test
	void deletesObject() {
		client.createBucket(r -> r.bucket("test-bucket"));
		client.putObject(r -> r.bucket("test-bucket").key("key.txt"), RequestBody.fromString("foo"));
		assertThatNoException().isThrownBy(() -> client.headObject(r -> r.bucket("test-bucket").key("key.txt")));

		s3Template.deleteObject("test-bucket", "key.txt");

		assertThatExceptionOfType(NoSuchKeyException.class)
				.isThrownBy(() -> client.headObject(r -> r.bucket("test-bucket").key("key.txt")));
	}

	@Test
	void deletesObjectByS3Url() {
		client.putObject(r -> r.bucket("test-bucket").key("key.txt"), RequestBody.fromString("foo"));
		assertThatNoException().isThrownBy(() -> client.headObject(r -> r.bucket("test-bucket").key("key.txt")));

		s3Template.deleteObject("s3://test-bucket/key.txt");

		assertThatExceptionOfType(NoSuchKeyException.class)
				.isThrownBy(() -> client.headObject(r -> r.bucket("test-bucket").key("key.txt")));
	}

	@Test
	void storesObject() throws IOException {
		S3Resource storedObject = s3Template.store("test-bucket", "person.json", new Person("John", "Doe"));

		ResponseInputStream<GetObjectResponse> response = client
				.getObject(r -> r.bucket("test-bucket").key("person.json"));
		String result = StreamUtils.copyToString(response, StandardCharsets.UTF_8);

		assertThat(storedObject).isNotNull();
		assertThat(result).isEqualTo("{\"firstName\":\"John\",\"lastName\":\"Doe\"}");
		assertThat(response.response().contentType()).isEqualTo("application/json");
	}

	@Test
	void readsObject() {
		client.putObject(r -> r.bucket("test-bucket").key("person.json"),
				RequestBody.fromString("{\"firstName\":\"John\",\"lastName\":\"Doe\"}"));

		Person person = s3Template.read("test-bucket", "person.json", Person.class);

		assertThat(person.firstName).isEqualTo("John");
		assertThat(person.lastName).isEqualTo("Doe");
	}

	@Test
	void uploadsFile() throws IOException {
		try (InputStream is = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8))) {
			S3Resource uploadedResource = s3Template.upload("test-bucket", "file.txt", is,
					ObjectMetadata.builder().contentType("text/plain").build());
			assertThat(uploadedResource).isNotNull();
		}

		ResponseInputStream<GetObjectResponse> response = client
				.getObject(r -> r.bucket("test-bucket").key("file.txt"));
		String result = StreamUtils.copyToString(response, StandardCharsets.UTF_8);
		assertThat(result).isEqualTo("hello");
		assertThat(response.response().contentType()).isEqualTo("text/plain");
	}

	@Test
	void downloadsFile() throws IOException {
		client.putObject(r -> r.bucket("test-bucket").key("file.txt"), RequestBody.fromString("hello"));
		S3Resource resource = (S3Resource) s3Template.download("test-bucket", "file.txt");
		assertThat(resource.contentLength()).isEqualTo(5);
		assertThat(resource.getDescription()).isNotNull();

		try (InputStream is = resource.getInputStream()) {
			String result = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
			assertThat(result).isEqualTo("hello");
		}
	}

	@Test
	void retrieveMetadata() throws IOException {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("key", "keyValue");
		metadata.put("camelCaseKey", "camelCaseKeyValue");

		client.putObject(r -> r.bucket("test-bucket").key("metadata.txt").metadata(metadata), RequestBody.fromString("hello"));
		S3Resource resource = s3Template.download("test-bucket", "metadata.txt");

		assertThat(resource.metadata())
			.containsEntry("key", "keyValue")
			// retrieved as lower case
			.containsEntry("camelcasekey", "camelCaseKeyValue")
			.doesNotContainKey("camelCaseKey");
	}

	private void bucketDoesNotExist(ListBucketsResponse r, String bucketName) {
		assertThat(r.buckets().stream().filter(b -> b.name().equals(bucketName)).findAny()).isEmpty();
	}

	private void bucketExists(ListBucketsResponse r, String bucketName) {
		assertThat(r.buckets().stream().filter(b -> b.name().equals(bucketName)).findAny()).isPresent();
	}

	static class Person {
		private String firstName;
		private String lastName;

		public Person() {
		}

		public Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
	}

}
