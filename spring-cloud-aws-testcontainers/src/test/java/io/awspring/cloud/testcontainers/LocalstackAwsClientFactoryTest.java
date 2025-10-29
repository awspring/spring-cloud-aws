/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Tests for {@link LocalstackAwsClientFactory}.
 *
 * @author Maciej Walkowiak
 */
@Testcontainers
class LocalstackAwsClientFactoryTest {

	@Container
	private LocalStackContainer localStackContainer = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:4.4.0"));

	@Test
	void createsClient() {
		var factory = new LocalstackAwsClientFactory(localStackContainer);
		try (var s3Client = factory.create(S3Client.builder())) {
			s3Client.createBucket(r -> r.bucket("my-bucket"));
			assertThat(s3Client.listBuckets().buckets()).hasSize(1);
		}
	}
}
