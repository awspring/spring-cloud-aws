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
package io.awspring.cloud.s3.crossregion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;

/**
 * Integration tests for {@link CrossRegionS3Client}.
 *
 * @author Maciej Walkowiak
 */
@Testcontainers
class CrossRegionS3ClientIntegrationTests {

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:1.4.0"));

	private static S3Client client;

	@BeforeAll
	static void beforeAll() {
		// region and credentials are irrelevant for test, but must be added to make
		// test work on environments without AWS cli configured
		StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider
				.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));
		client = new CrossRegionS3Client(
				S3Client.builder().region(Region.of(localstack.getRegion())).credentialsProvider(credentialsProvider)
						.endpointOverride(localstack.getEndpoint()));
	}

	@Test
	void utilitiesDelegateToDefaultClient() {
		URL url = client.utilities().getUrl(r -> r.key("key").bucket("foo"));
		assertThat(url).isNotNull();
	}

	@Test
	void waiterDelegateToDefaultClient() {
		AtomicReference<ResponseOrException<HeadBucketResponse>> result = new AtomicReference<>();
		CompletableFuture.runAsync(() -> {
			WaiterResponse<HeadBucketResponse> bucket = client.waiter().waitUntilBucketExists(r -> r.bucket("bucket"));
			result.set(bucket.matched());
		});

		client.createBucket(r -> r.bucket("bucket"));

		await().untilAsserted(() -> {
			assertThat(result.get()).isNotNull();
			assertThat(result.get().response()).satisfies(it -> {
				assertThat(it).isPresent();
			});
		});
	}

}
