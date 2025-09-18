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

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * The base contract for JUnit tests based on the container for Localstack. The Testcontainers 'reuse' option must be
 * disabled, so Ryuk container is started and will clean all the containers up from this test suite after JVM exit.
 * Since the Localstack container instance is shared via static property, it is going to be started only once per JVM;
 * therefore, the target Docker container is reused automatically.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
@Testcontainers(disabledWithoutDocker = true)
public interface LocalstackContainerTest {

	LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:4.4.0")).withEnv("S3_SKIP_SIGNATURE_VALIDATION", "0");

	@BeforeAll
	static void startContainer() {
		LOCAL_STACK_CONTAINER.start();
	}

	static S3AsyncClient s3AsyncClient() {
		return S3AsyncClient.crtBuilder().region(Region.of(LOCAL_STACK_CONTAINER.getRegion()))
				.credentialsProvider(credentialsProvider()).endpointOverride(LOCAL_STACK_CONTAINER.getEndpoint())
				.build();
	}

	static S3Client s3Client() {
		return applyAwsClientOptions(S3Client.builder());
	}

	static AwsCredentialsProvider credentialsProvider() {
		return StaticCredentialsProvider.create(
				AwsBasicCredentials.create(LOCAL_STACK_CONTAINER.getAccessKey(), LOCAL_STACK_CONTAINER.getSecretKey()));
	}

	private static <B extends AwsClientBuilder<B, T>, T> T applyAwsClientOptions(B clientBuilder) {
		return clientBuilder.region(Region.of(LOCAL_STACK_CONTAINER.getRegion()))
				.credentialsProvider(credentialsProvider()).endpointOverride(LOCAL_STACK_CONTAINER.getEndpoint())
				.build();
	}

}
