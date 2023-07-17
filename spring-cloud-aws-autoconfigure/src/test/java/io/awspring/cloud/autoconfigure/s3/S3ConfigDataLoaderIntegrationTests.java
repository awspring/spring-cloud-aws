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
package io.awspring.cloud.autoconfigure.s3;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SECRETSMANAGER;

/**
 * Integration tests for loading configuration properties from AWS S3.
 *
 * @author Kunal Varpe
 */

@Testcontainers
public class S3ConfigDataLoaderIntegrationTests {
	private static final String REGION = "us-east-1";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:1.4.0")).withServices(S3).withReuse(true);

	@BeforeAll
	static void beforeAll() {
		createBucket(localstack, "test-bucket", REGION);
		uploadFileToBucket(localstack, "test-bucket", "application.properties", "key1=value1", REGION);
	}

	@Test
	void resolvesPropertyFromS3() {
		SpringApplication application = new SpringApplication(S3ConfigDataLoaderIntegrationTests.App. class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-s3:test-bucket/application.properties")) {
			assertThat(context.getEnvironment().getProperty("key1")).isEqualTo("value1");
		}
	}

	private static void createBucket(LocalStackContainer localstack, String bucket, String region) {
		try {
			localstack.execInContainer("awslocal", "s3", "mb", "s3://" + bucket, "--region", region);
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static void uploadFileToBucket(LocalStackContainer localstack, String bucket, String fileName, String conent, String region) {
		try {
			localstack.execInContainer("touch", fileName);
			localstack.execInContainer("echo", conent, ">>", fileName);
			String bucketWithKey = "s3://" + bucket + "/" + fileName;
			localstack.execInContainer("awslocal", "s3", "cp", fileName, bucketWithKey, "--region", region);
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport) {
		return runApplication(application, springConfigImport, "spring.cloud.aws.s3.endpoint");
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport,
			String endpointProperty) {
		System.out.println(localstack.getEndpointOverride(S3));
		return application.run("--spring.config.import=" + springConfigImport,
				"--spring.cloud.aws.s3.region=" + REGION,
				"--" + endpointProperty + "=" + localstack.getEndpointOverride(S3).toString(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=eu-west-1", "--logging.level.io.awspring.cloud.s3=debug");
	}

	@SpringBootApplication
	@EnableAutoConfiguration
	static class App {

	}
}
