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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import net.bytebuddy.utility.RandomString;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Integration tests for {@link S3Template}.
 *
 * @author Maciej Walkowiak
 * @author Yuki Yoshida
 * @author Ziemowit Stolarczyk
 * @author Hardik Singh Behl
 */
@Testcontainers
class S3TemplateIntegrationTests {

	private static final String BUCKET_NAME = "test-bucket";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:3.8.1")).withEnv("S3_SKIP_SIGNATURE_VALIDATION", "0");

	private static S3Client client;

	private static S3Presigner presigner;
	private S3Template s3Template;

	@BeforeAll
	static void beforeAll() {
		// region and credentials are irrelevant for test, but must be added to make
		// test work on environments without AWS cli configured
		StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider
				.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));
		client = S3Client.builder().region(Region.of(localstack.getRegion())).credentialsProvider(credentialsProvider)
				.endpointOverride(localstack.getEndpoint()).build();
		presigner = S3Presigner.builder().region(Region.of(localstack.getRegion()))
				.credentialsProvider(credentialsProvider).endpointOverride(localstack.getEndpoint()).build();
	}

	@BeforeEach
	void init() {
		this.s3Template = new S3Template(client,
				new DiskBufferingS3OutputStreamProvider(client, new PropertiesS3ObjectContentTypeResolver()),
				new Jackson2JsonS3ObjectConverter(new ObjectMapper()), presigner);

		client.createBucket(r -> r.bucket(BUCKET_NAME));
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
		String location = s3Template.createBucket(BUCKET_NAME);

		assertThat(location).isNotNull();
		assertThat(client.listBuckets()).satisfies(r -> this.bucketExists(r, BUCKET_NAME));
	}

	@Test
	void deletesBucket() {
		client.createBucket(r -> r.bucket(BUCKET_NAME));
		assertThat(client.listBuckets()).satisfies(r -> this.bucketExists(r, BUCKET_NAME));

		s3Template.deleteBucket(BUCKET_NAME);

		assertThat(client.listBuckets()).satisfies(r -> this.bucketDoesNotExist(r, BUCKET_NAME));
	}

	@Test
	void whenBucketExistsShouldReturnTrue() {
		final boolean existsBucket = s3Template.bucketExists(BUCKET_NAME);

		assertThat(existsBucket).isTrue();
		assertThat(client.listBuckets()).satisfies(r -> this.bucketExists(r, BUCKET_NAME));
	}

	@Test
	void whenBucketNotExistsShouldReturnFalse() {
		destroyBuckets();

		final boolean existsBucket = s3Template.bucketExists(BUCKET_NAME);

		assertThat(existsBucket).isFalse();
	}

	@Test
	void deletesObject() {
		client.createBucket(r -> r.bucket(BUCKET_NAME));
		client.putObject(r -> r.bucket(BUCKET_NAME).key("key.txt"), RequestBody.fromString("foo"));
		assertThatNoException().isThrownBy(() -> client.headObject(r -> r.bucket(BUCKET_NAME).key("key.txt")));

		s3Template.deleteObject(BUCKET_NAME, "key.txt");

		assertThatExceptionOfType(NoSuchKeyException.class)
				.isThrownBy(() -> client.headObject(r -> r.bucket(BUCKET_NAME).key("key.txt")));
	}

	@Test
	void deletesObjectByS3Url() {
		client.putObject(r -> r.bucket(BUCKET_NAME).key("key.txt"), RequestBody.fromString("foo"));
		assertThatNoException().isThrownBy(() -> client.headObject(r -> r.bucket(BUCKET_NAME).key("key.txt")));

		s3Template.deleteObject("s3://test-bucket/key.txt");

		assertThatExceptionOfType(NoSuchKeyException.class)
				.isThrownBy(() -> client.headObject(r -> r.bucket(BUCKET_NAME).key("key.txt")));
	}

	@Test
	void whenObjectExistsShouldReturnTrue() {
		client.putObject(r -> r.bucket(BUCKET_NAME).key("key.txt"), RequestBody.fromString("hello"));

		final boolean existsObject = s3Template.objectExists(BUCKET_NAME, "key.txt");

		assertThat(existsObject).isTrue();
	}

	@Test
	void whenObjectNotExistsShouldReturnFalse() {
		client.putObject(r -> r.bucket(BUCKET_NAME).key("key.txt"), RequestBody.fromString("hello"));

		final boolean existsObject1 = s3Template.objectExists(BUCKET_NAME, "other-key.txt");
		final boolean existsObject2 = s3Template.objectExists("other-bucket", "key.txt");

		assertThat(existsObject1).isFalse();
		assertThat(existsObject2).isFalse();
	}

	@Test
	void listObjects() throws IOException {
		client.putObject(r -> r.bucket(BUCKET_NAME).key("hello-en.txt"), RequestBody.fromString("hello"));
		client.putObject(r -> r.bucket(BUCKET_NAME).key("hello-fr.txt"), RequestBody.fromString("bonjour"));
		client.putObject(r -> r.bucket(BUCKET_NAME).key("bye.txt"), RequestBody.fromString("bye"));

		List<S3Resource> resources = s3Template.listObjects(BUCKET_NAME, "hello");
		assertThat(resources.size()).isEqualTo(2);

		// According to the S3Client doc : "Objects are returned sorted in an ascending order of the respective key
		// names in the list."
		assertThat(resources).extracting(S3Resource::getInputStream)
				.map(is -> new String(is.readAllBytes(), StandardCharsets.UTF_8)).containsExactly("hello", "bonjour");
	}

	@Test
	void storesObject() throws IOException {
		S3Resource storedObject = s3Template.store(BUCKET_NAME, "person.json", new Person("John", "Doe"));

		ResponseInputStream<GetObjectResponse> response = client
				.getObject(r -> r.bucket(BUCKET_NAME).key("person.json"));
		String result = StreamUtils.copyToString(response, StandardCharsets.UTF_8);

		assertThat(storedObject).isNotNull();
		assertThat(result).isEqualTo("{\"firstName\":\"John\",\"lastName\":\"Doe\"}");
		assertThat(response.response().contentType()).isEqualTo("application/json");
	}

	@Test
	void readsObject() {
		client.putObject(r -> r.bucket(BUCKET_NAME).key("person.json"),
				RequestBody.fromString("{\"firstName\":\"John\",\"lastName\":\"Doe\"}"));

		Person person = s3Template.read(BUCKET_NAME, "person.json", Person.class);

		assertThat(person.firstName).isEqualTo("John");
		assertThat(person.lastName).isEqualTo("Doe");
	}

	@Test
	void uploadsFile() throws IOException {
		try (InputStream is = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8))) {
			S3Resource uploadedResource = s3Template.upload(BUCKET_NAME, "file.txt", is,
					ObjectMetadata.builder().contentType("text/plain").build());
			assertThat(uploadedResource).isNotNull();
		}

		ResponseInputStream<GetObjectResponse> response = client.getObject(r -> r.bucket(BUCKET_NAME).key("file.txt"));
		String result = StreamUtils.copyToString(response, StandardCharsets.UTF_8);
		assertThat(result).isEqualTo("hello");
		assertThat(response.response().contentType()).isEqualTo("text/plain");
	}

	@Test
	void downloadsFile() throws IOException {
		client.putObject(r -> r.bucket(BUCKET_NAME).key("file.txt"), RequestBody.fromString("hello"));
		S3Resource resource = (S3Resource) s3Template.download(BUCKET_NAME, "file.txt");
		assertThat(resource.contentLength()).isEqualTo(5);
		assertThat(resource.getDescription()).isNotNull();

		try (InputStream is = resource.getInputStream()) {
			String result = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
			assertThat(result).isEqualTo("hello");
		}
	}

	@Test
	void createsWorkingSignedGetURL() throws IOException {
		client.putObject(r -> r.bucket(BUCKET_NAME).key("file.txt"), RequestBody.fromString("hello"));
		URL signedGetUrl = s3Template.createSignedGetURL(BUCKET_NAME, "file.txt", Duration.ofMinutes(1));

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(signedGetUrl.toString());
		HttpResponse response = httpClient.execute(httpGet);

		try (InputStream content = response.getEntity().getContent()) {
			String result = StreamUtils.copyToString(content, StandardCharsets.UTF_8);
			assertThat(result).isEqualTo("hello");
		}
	}

	@Test
	void createsWorkingSignedPutURL() throws IOException {
		String fileContent = RandomString.make();
		long contentLength = fileContent.length();
		String contentMD5 = calculateContentMD5(fileContent);

		ObjectMetadata metadata = ObjectMetadata.builder().metadata("testkey", "testvalue").contentLength(contentLength)
				.contentMD5(contentMD5).build();
		URL signedPutUrl = s3Template.createSignedPutURL(BUCKET_NAME, "file.txt", Duration.ofMinutes(1), metadata,
				"text/plain");

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPut httpPut = new HttpPut(signedPutUrl.toString());
		httpPut.setHeader("x-amz-meta-testkey", "testvalue");
		httpPut.setHeader("Content-Type", "text/plain");
		httpPut.setHeader("Content-MD5", contentMD5);
		HttpEntity body = new StringEntity(fileContent);
		httpPut.setEntity(body);

		HttpResponse response = httpClient.execute(httpPut);
		httpClient.close();

		HeadObjectResponse headObjectResponse = client
				.headObject(HeadObjectRequest.builder().bucket(BUCKET_NAME).key("file.txt").build());

		assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatusCode.OK);
		assertThat(headObjectResponse.contentLength()).isEqualTo(contentLength);
		assertThat(headObjectResponse.metadata().containsKey("testkey")).isTrue();
		assertThat(headObjectResponse.metadata().get("testkey")).isEqualTo("testvalue");
	}

	@Test
	void signedPutURLFailsForNonMatchingSignature() throws IOException {
		String fileContent = RandomString.make();
		long contentLength = fileContent.length();
		String contentMD5 = calculateContentMD5(fileContent);
		String maliciousContent = RandomString.make();

		ObjectMetadata metadata = ObjectMetadata.builder().contentLength(contentLength).contentMD5(contentMD5).build();
		URL signedPutUrl = s3Template.createSignedPutURL(BUCKET_NAME, "file.txt", Duration.ofMinutes(1), metadata,
				"text/plain");

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPut httpPut = new HttpPut(signedPutUrl.toString());
		httpPut.setHeader("Content-Type", "text/plain");
		httpPut.setHeader("Content-MD5", contentMD5);
		HttpEntity body = new StringEntity(fileContent + maliciousContent);
		httpPut.setEntity(body);

		HttpResponse response = httpClient.execute(httpPut);
		httpClient.close();

		assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatusCode.FORBIDDEN);
	}

	private void bucketDoesNotExist(ListBucketsResponse r, String bucketName) {
		assertThat(r.buckets().stream().filter(b -> b.name().equals(bucketName)).findAny()).isEmpty();
	}

	private void bucketExists(ListBucketsResponse r, String bucketName) {
		assertThat(r.buckets().stream().filter(b -> b.name().equals(bucketName)).findAny()).isPresent();
	}

	private String calculateContentMD5(String content) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
			byte[] mdBytes = md.digest(contentBytes);
			return Base64.getEncoder().encodeToString(mdBytes);
		}
		catch (Exception exception) {
			throw new RuntimeException("Failed to calculate Content-MD5", exception);
		}
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
