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
package io.awspring.cloud.samples.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SpringBootTest
class AppTest {

	@TestConfiguration
	static class AppConfiguration {

		@Bean
		@ServiceConnection
		LocalStackContainer localStackContainer() {
			return new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8.1"));
		}
	}

	@Test
	void connectsToContainerThroughServiceConnection(@Autowired DynamoDbClient dynamoDbClient) {
		assertThat(dynamoDbClient.listTables().tableNames()).containsExactly("department");
	}

	public static void main(String[] args) {
		SpringApplication.from(App::main).with(AppConfiguration.class).run(args);
	}
}
