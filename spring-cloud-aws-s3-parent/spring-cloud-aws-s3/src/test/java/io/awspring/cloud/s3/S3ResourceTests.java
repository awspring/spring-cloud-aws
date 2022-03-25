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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
class S3ResourceTests {

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.0")).withServices(Service.S3).withReuse(true);

	private static S3Client client;

	@BeforeAll
	static void beforeAll() {
		client = S3Client.builder().region(Region.US_EAST_1)
				.endpointOverride(localstack.getEndpointOverride(Service.S3)).build();
		client.createBucket(request -> request.bucket("first-bucket"));
	}

	@Test
	void readsFileFromS3() throws IOException {
		client.putObject(PutObjectRequest.builder().bucket("first-bucket").key("test-file.txt").build(),
				RequestBody.fromString("test-file-content"));

		S3Resource resource = new S3Resource("s3://first-bucket/test-file.txt", client);
		String content = retrieveContent(resource);
		assertThat(content).isEqualTo("test-file-content");
	}

	@NotNull
	private String retrieveContent(S3Resource resource) throws IOException {
		return new BufferedReader(new InputStreamReader(resource.getInputStream())).lines()
				.collect(Collectors.joining("\n"));
	}

}
