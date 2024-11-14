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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.dynamodb.DynamoDbAutoConfiguration;
import io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration;
import io.awspring.cloud.autoconfigure.ses.SesAutoConfiguration;
import io.awspring.cloud.autoconfigure.sns.SnsAutoConfiguration;
import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class AwsContainerConnectionDetailsFactoryTest {

	@Container
	@ServiceConnection
	static final LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:3.8.1"));

	@Autowired(required = false)
	private AwsConnectionDetails connectionDetails;

	@Test
	void createsAwsConnection() {
		assertThat(connectionDetails).isNotNull().satisfies(it -> {
			assertThat(connectionDetails.getEndpoint().getHost()).isIn("127.0.0.1", "localhost");
		});
	}

	@Test
	void configuresDynamoDbClientWithServiceConnection(@Autowired DynamoDbClient client) {
		assertThatCode(client::listTables).doesNotThrowAnyException();
	}

	@Test
	void configuresSesClientWithServiceConnection(@Autowired SesClient client) {
		assertThatCode(client::listIdentities).doesNotThrowAnyException();
	}

	@Test
	void configuresSqsClientWithServiceConnection(@Autowired SqsAsyncClient client) {
		assertThatCode(() -> client.listQueues().join()).doesNotThrowAnyException();
	}

	@Test
	void configuresSnsClientWithServiceConnection(@Autowired SnsClient client) {
		assertThatCode(client::listTopics).doesNotThrowAnyException();
	}

	@Test
	void configuresS3ClientWithServiceConnection(@Autowired S3Client client) {
		assertThatCode(client::listBuckets).doesNotThrowAnyException();
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ AwsAutoConfiguration.class, CredentialsProviderAutoConfiguration.class,
			RegionProviderAutoConfiguration.class, DynamoDbAutoConfiguration.class, SesAutoConfiguration.class,
			SqsAutoConfiguration.class, SnsAutoConfiguration.class, S3AutoConfiguration.class })
	static class TestConfiguration {
	}

}
