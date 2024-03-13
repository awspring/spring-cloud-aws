/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.autoconfigure.config.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Integration tests for loading configuration properties from AWS S3.
 *
 * @author Kunal Varpe
 * @author Matej Nedic
 */

@Testcontainers
public class S3ConfigDataLoaderIntegrationTests {

	private static String BUCKET = "test-bucket";
	private static String FILE_NAME = "/application.properties";
	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:1.4.0")).withServices(S3).withReuse(true);

	@BeforeAll
	static void beforeAll() {
		createBucket();
	}

	@Test
	void resolvesPropertyFromS3() {
		SpringApplication application = new SpringApplication(S3ConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		uploadFileToBucket("key1=value1");

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-s3:test-bucket/application.properties")) {
			assertThat(context.getEnvironment().getProperty("key1")).isEqualTo("value1");
		}
	}

	@Test
	void reloadPropertiesFromS3() {
		SpringApplication application = new SpringApplication(S3ConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		uploadFileToBucket("key1=value1");

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-s3:test-bucket/application.properties",
				"--spring.cloud.aws.s3.reload.strategy=refresh", "--spring.cloud.aws.s3.reload.period=PT1S",
				"--spring.cloud.aws.s3.region=" + localstack.getRegion(),
				"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=" + localstack.getRegion())) {

			assertThat(context.getEnvironment().getProperty("key1")).isEqualTo("value1");

			uploadFileToBucket("key1=value2");

			await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
				assertThat(context.getEnvironment().getProperty("key1")).isEqualTo("value2");
			});
		}
	}

	private static void createBucket() {
		try {
			localstack.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET, "--region", localstack.getRegion());
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static void uploadFileToBucket(String content) {
		S3Client s3Client = S3Client.builder().region(Region.of(localstack.getRegion()))
				.endpointOverride(localstack.getEndpoint()).build();
		s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET).key(FILE_NAME).build(),
				RequestBody.fromBytes(content.getBytes()));
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport) {
		return runApplication(application, springConfigImport, "spring.cloud.aws.s3.endpoint");
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport,
			String endpointProperty) {
		System.out.println(localstack.getEndpointOverride(S3));
		return application.run("--spring.config.import=" + springConfigImport,
				"--spring.cloud.aws.s3.region=" + localstack.getRegion(),
				"--" + endpointProperty + "=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=eu-west-1", "--logging.level.io.awspring.cloud.s3=debug");
	}

	@SpringBootApplication
	@EnableAutoConfiguration
	static class App {

	}
}
