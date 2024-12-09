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
import static org.mockito.Mockito.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.s3.S3ClientCustomizer;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.builder.SdkDefaultClientBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
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
	private static final String YAML_TYPE = "application/x-yaml";
	private static final String YAML_TYPE_ALTERNATIVE = "text/yaml";
	private static final String TEXT_TYPE = "text/plain";
	private static final String JSON_TYPE = "application/json";
	private static String BUCKET = "test-bucket";
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
		uploadFileToBucket("key1=value1", "application.properties", TEXT_TYPE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-s3:test-bucket/application.properties")) {
			assertThat(context.getEnvironment().getProperty("key1")).isEqualTo("value1");
		}
	}

	@Test
	void resolvesPropertyFromS3ComplexPath() {
		SpringApplication application = new SpringApplication(S3ConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		uploadFileToBucket("key1=value1", "myPath/unusual/application.properties", TEXT_TYPE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-s3:test-bucket/myPath/unusual/application.properties")) {
			assertThat(context.getEnvironment().getProperty("key1")).isEqualTo("value1");
		}
	}

	@Test
	void resolvesYamlFromS3() {
		SpringApplication application = new SpringApplication(S3ConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		uploadFileToBucket("key1: value1", "application.yaml", YAML_TYPE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-s3:test-bucket/application.yaml")) {
			assertThat(context.getEnvironment().getProperty("key1")).isEqualTo("value1");
		}
	}

	@Test
	void resolvesYamlAlternative() {
		SpringApplication application = new SpringApplication(S3ConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		uploadFileToBucket("key1: value1", "test.yaml", YAML_TYPE_ALTERNATIVE);

		try (ConfigurableApplicationContext context = runApplication(application, "aws-s3:test-bucket/test.yaml")) {
			assertThat(context.getEnvironment().getProperty("key1")).isEqualTo("value1");
		}
	}

	@Test
	void resolveJson() throws JsonProcessingException {
		SpringApplication application = new SpringApplication(S3ConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		uploadFileToBucket(new ObjectMapper().writeValueAsString(Map.of("key1", "value1")), "application.json",
				JSON_TYPE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-s3:test-bucket/application.json")) {
			assertThat(context.getEnvironment().getProperty("key1")).isEqualTo("value1");
		}
	}

	@Test
	void clientIsConfiguredWithCustomizerProvidedToBootstrapRegistry() throws JsonProcessingException {
		SpringApplication application = new SpringApplication(S3ConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new S3ConfigDataLoaderIntegrationTests.CustomizerConfiguration());
		uploadFileToBucket(new ObjectMapper().writeValueAsString(Map.of("key1", "value1")), "application.json",
				JSON_TYPE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-s3:test-bucket/application.json")) {
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(S3Client.class));
			assertThat(client.getApiCallTimeout()).isEqualTo(Duration.ofMillis(2001));
			assertThat(client.getSyncHttpClient()).isInstanceOf(SdkDefaultClientBuilder.NonManagedSdkHttpClient.class);
			assertThat(client.getSyncHttpClient().clientName()).isEqualTo("mock-client");
		}
	}

	@Test
	void reloadPropertiesFromS3() {
		SpringApplication application = new SpringApplication(S3ConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		uploadFileToBucket("key1=value1", "reload.properties", TEXT_TYPE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-s3:test-bucket/reload.properties",
				"--spring.cloud.aws.s3.config.reload.strategy=refresh",
				"--spring.cloud.aws.s3.config.reload.period=PT1S",
				"--spring.cloud.aws.s3.region=" + localstack.getRegion(),
				"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=" + localstack.getRegion())) {

			assertThat(context.getEnvironment().getProperty("key1")).isEqualTo("value1");

			uploadFileToBucket("key1=value2", "reload.properties", TEXT_TYPE);

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

	private static void uploadFileToBucket(String content, String key, String contentType) {
		S3Client s3Client = S3Client.builder().region(Region.of(localstack.getRegion()))
				.endpointOverride(localstack.getEndpoint())
				.credentialsProvider(StaticCredentialsProvider
						.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
				.build();
		s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET).contentType(contentType).key(key).build(),
				RequestBody.fromBytes(content.getBytes()));
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport) {
		return runApplication(application, springConfigImport, "spring.cloud.aws.s3.endpoint");
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport,
			String endpointProperty) {
		return application.run("--spring.config.import=" + springConfigImport,
				"--spring.cloud.aws.s3.region=" + localstack.getRegion(),
				"--" + endpointProperty + "=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=eu-west-1", "--logging.level.io.awspring.cloud.s3=debug");
	}

	static class CustomizerConfiguration implements BootstrapRegistryInitializer {

		@Override
		public void initialize(BootstrapRegistry registry) {
			registry.register(S3ClientCustomizer.class, context -> (builder -> {
				builder.overrideConfiguration(builder.overrideConfiguration().copy(c -> {
					c.apiCallTimeout(Duration.ofMillis(2001));
				}));
			}));

			SdkHttpClient mock = spy(ApacheHttpClient.builder().build());
			when(mock.clientName()).thenReturn("mock-client");

			registry.register(SdkHttpClient.class, context -> mock);

			registry.register(AwsSyncClientCustomizer.class, context -> (builder -> {
				builder.httpClient(mock);
			}));
		}
	}

	@SpringBootApplication
	@EnableAutoConfiguration
	static class App {

	}
}
