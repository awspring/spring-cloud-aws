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
package io.awspring.cloud.autoconfigure.config.secretsmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.sts.auth.StsWebIdentityTokenFileCredentialsProvider;

/**
 * Integration tests for loading configuration properties from AWS Secrets Manager.
 *
 * @author Maciej Walkowiak
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class SecretsManagerConfigDataLoaderIntegrationTests {

	private static final String REGION = "us-east-1";
	private static final String NEW_LINE_CHAR = System.lineSeparator();

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:4.4.0"));

	@TempDir
	static Path tokenTempDir;

	@BeforeAll
	static void beforeAll() {
		createSecret(localstack, "/config/spring",
				"{\"message\":\"value from tests\", \"another-parameter\": \"another parameter value\"}", REGION);
		createSecret(localstack, "/certs/prod/fn_certificate", "=== my prod cert should be here", REGION);
		createSecret(localstack, "/certs/dev/fn_certificate/", "=== my dev cert should be here", REGION);
		createSecret(localstack, "fn_certificate", "=== my cert should be here", REGION);
		createSecret(localstack, "/config/second", "{\"secondMessage\":\"second value from tests\"}", REGION);
		createSecretBlob(localstack, "/blob/byte_certificate",
				SdkBytes.fromString("My certificate", Charset.defaultCharset()), REGION);

	}

	@Test
	void resolvesPropertyFromSecretsManager() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:/config/spring;/config/second")) {
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
			assertThat(context.getEnvironment().getProperty("another-parameter")).isEqualTo("another parameter value");
			assertThat(context.getEnvironment().getProperty("secondMessage")).isEqualTo("second value from tests");
			assertThat(context.getEnvironment().getProperty("non-existing-parameter")).isNull();
		}
	}

	@Test
	void resolvesPropertiesWithPrefixes() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:/config/spring?prefix=first.;/config/second?prefix=second.")) {
			assertThat(context.getEnvironment().getProperty("first.message")).isEqualTo("value from tests");
			assertThat(context.getEnvironment().getProperty("first.another-parameter"))
					.isEqualTo("another parameter value");
			assertThat(context.getEnvironment().getProperty("second.secondMessage"))
					.isEqualTo("second value from tests");
			assertThat(context.getEnvironment().getProperty("non-existing-parameter")).isNull();
		}
	}

	@Test
	void resolvesPropertyFromSecretsManager_PlainTextSecret() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:/certs/prod/fn_certificate")) {
			assertThat(context.getEnvironment().getProperty("fn_certificate"))
					.isEqualTo("=== my prod cert should be here");
		}
	}

	@Test
	void resolvesPropertyFromSecretsManager_PlainTextSecret_endingWithSlash() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:/certs/dev/fn_certificate/")) {
			assertThat(context.getEnvironment().getProperty("fn_certificate"))
					.isEqualTo("=== my dev cert should be here");
		}
	}

	@Test
	void resolvesPropertyFromSecretsManager_SecretBinary() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:/blob/byte_certificate")) {
			assertThat(context.getEnvironment().getProperty("byte_certificate", byte[].class))
					.isEqualTo("My certificate".getBytes(StandardCharsets.UTF_8));
		}
	}

	@Test
	void resolvesPropertyFromSecretsManager_PlainTextSecret_WithoutSlashes() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:fn_certificate")) {
			assertThat(context.getEnvironment().getProperty("fn_certificate")).isEqualTo("=== my cert should be here");
		}
	}

	@Test
	void respectsImportOrder() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application, "classpath:config.properties")) {
			assertThat(context.getEnvironment().getProperty("another-parameter")).isEqualTo("from properties file");
		}
	}

	@Test
	void clientIsConfiguredWithCustomizerProvidedToBootstrapRegistry() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new CustomizerConfiguration());

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:/config/spring;/config/second")) {
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(SecretsManagerClient.class));
			assertThat(client.getApiCallTimeout()).isEqualTo(Duration.ofMillis(2001));
			assertThat(client.getSyncHttpClient()).isNotNull();
		}
	}

	@Test
	void whenKeysAreNotSpecifiedFailsWithHumanReadableFailureMessage(CapturedOutput output) {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application, "aws-secretsmanager:")) {
			fail("Context without keys should fail to start");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(SecretsManagerKeysMissingException.class);
			// ensure that failure analyzer catches the exception and provides meaningful
			// error message
			// Ensure that new line character should be platform independent
			String errorMessage = "Description:%1$s%1$sCould not import properties from AWS Secrets Manager"
					.formatted(NEW_LINE_CHAR);
			assertThat(output.getOut()).contains(errorMessage);
		}
	}

	@Test
	void secretsManagerClientCanBeOverwrittenInBootstrapConfig() {
		SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
		when(mockClient.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(GetSecretValueResponse.builder()
				.name("secrets").secretString("{\"message\":\"value from mock\"}").build());
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(registry -> {
			registry.register(SecretsManagerClient.class, ctx -> mockClient);
		});

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:/config/spring")) {
			SecretsManagerClient clientFromContext = context.getBean(SecretsManagerClient.class);
			assertThat(clientFromContext).isEqualTo(mockClient);
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from mock");
		}
	}

	@Test
	void credentialsProviderCanBeOverwrittenInBootstrapConfig() {
		AwsCredentialsProvider bootstrapCredentialsProvider = StaticCredentialsProvider
				.create(AwsBasicCredentials.create("mock-key", "mock-secret"));
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(registry -> {
			registry.register(AwsCredentialsProvider.class, ctx -> bootstrapCredentialsProvider);
		});

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:/config/spring")) {
			ConfiguredAwsClient secretsManagerClient = new ConfiguredAwsClient(
					context.getBean(SecretsManagerClient.class));
			assertThat(secretsManagerClient.getAwsCredentialsProvider()).isEqualTo(bootstrapCredentialsProvider);
		}
	}

	@Test
	void outputsDebugLogs(CapturedOutput output) {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:/config/spring;/config/second")) {
			context.getEnvironment().getProperty("message");
			assertThat(output.getAll()).contains("Populating property retrieved from AWS Secrets Manager: message");
		}
	}

	@Test
	void endpointCanBeOverwrittenWithGlobalAwsProperties() {
		SpringApplication application = new SpringApplication(SecretsManagerConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:/config/spring;/config/second", "spring.cloud.aws.endpoint")) {
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
		}
	}

	@Test
	void propertyIsNotResolvedWhenIntegrationIsDisabled() {
		SpringApplication application = new SpringApplication(SecretsManagerConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-secretsmanager:/config/spring;/config/second",
				"--spring.cloud.aws.secretsmanager.enabled=false", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=eu-west-1")) {
			assertThat(context.getEnvironment().getProperty("message")).isNull();
			assertThat(context.getBeanProvider(SecretsManagerClient.class).getIfAvailable()).isNull();
		}
	}

	@Test
	void serviceSpecificEndpointTakesPrecedenceOverGlobalAwsRegion() {
		SpringApplication application = new SpringApplication(SecretsManagerConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-secretsmanager:/config/spring;/config/second",
				"--spring.cloud.aws.secretsmanager.region=" + REGION,
				"--spring.cloud.aws.endpoint=http://non-existing-host/",
				"--spring.cloud.aws.secretsmanager.endpoint=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=eu-west-1")) {
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
			assertThat(context.getBean(AwsCredentialsProvider.class)).isInstanceOf(StaticCredentialsProvider.class);
		}
	}

	@Test
	void secretsManagerClientUsesStsCredentials() throws IOException {
		File tempFile = tokenTempDir.resolve("token-file.txt").toFile();
		tempFile.createNewFile();
		SpringApplication application = new SpringApplication(SecretsManagerConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=optional:aws-secretsmanager:/config/spring;/config/second",
				"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(), "--spring.cloud.aws.region.static=" + REGION,
				"--spring.cloud.aws.credentials.sts.role-arn=develop",
				"--spring.cloud.aws.credentials.sts.enabled=true",
				"--spring.cloud.aws.credentials.sts.web-identity-token-file=" + tempFile.getAbsolutePath())) {
			assertThat(context.getBean(AwsCredentialsProvider.class))
					.isInstanceOf(StsWebIdentityTokenFileCredentialsProvider.class);
		}
	}

	@Test
	void secretsManagerClientUsesGlobalRegion() {
		SpringApplication application = new SpringApplication(SecretsManagerConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-secretsmanager:/config/spring;/config/second",
				"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=" + REGION)) {
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
		}
	}

	@Nested
	class ReloadConfigurationTests {

		@AfterEach
		void resetSecretValue() {
			putSecretValue(localstack, "/config/spring",
					"{\"message\":\"value from tests\", \"another-parameter\": \"another parameter value\"}", REGION);
		}

		@Test
		void reloadsProperties() {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			try (ConfigurableApplicationContext context = application.run(
					"--spring.config.import=aws-secretsmanager:/config/spring;/config/second",
					"--spring.cloud.aws.secretsmanager.region=" + REGION,
					"--spring.cloud.aws.secretsmanager.reload.strategy=refresh",
					"--spring.cloud.aws.secretsmanager.reload.period=PT1S",
					"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
					"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
					"--spring.cloud.aws.region.static=eu-west-1",
					"--logging.level.io.awspring.cloud.secretsmanager=debug")) {
				assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");

				// update secret value
				SecretsManagerClient smClient = context.getBean(SecretsManagerClient.class);
				smClient.putSecretValue(
						r -> r.secretId("/config/spring").secretString("{\"message\":\"new value\"}").build());

				await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
					assertThat(context.getEnvironment().getProperty("message")).isEqualTo("new value");
				});
			}
		}

		@Test
		void doesNotReloadPropertiesWhenMonitoringIsDisabled() {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			try (ConfigurableApplicationContext context = application.run(
					"--spring.config.import=aws-secretsmanager:/config/spring;/config/second",
					"--spring.cloud.aws.secretsmanager.region=" + REGION,
					"--spring.cloud.aws.secretsmanager.reload.period=PT1S",
					"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
					"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
					"--spring.cloud.aws.region.static=eu-west-1",
					"--logging.level.io.awspring.cloud.secretsmanager=debug")) {
				assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");

				// update secret value
				SecretsManagerClient smClient = context.getBean(SecretsManagerClient.class);
				smClient.putSecretValue(
						r -> r.secretId("/config/spring").secretString("{\"message\":\"new value\"}").build());

				await().during(Duration.ofSeconds(5)).untilAsserted(() -> {
					assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
				});
			}
		}

		@Test
		void reloadsPropertiesWithRestartContextStrategy() {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			try (ConfigurableApplicationContext context = application.run(
					"--spring.config.import=aws-secretsmanager:/config/spring;/config/second",
					"--spring.cloud.aws.secretsmanager.region=" + REGION,
					"--spring.cloud.aws.secretsmanager.reload.strategy=RESTART_CONTEXT",
					"--spring.cloud.aws.secretsmanager.reload.period=PT1S",
					"--spring.cloud.aws.secretsmanager.reload.max-wait-for-restart=PT1S",
					"--management.endpoint.restart.enabled=true", "--management.endpoints.web.exposure.include=restart",
					"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
					"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
					"--spring.cloud.aws.region.static=eu-west-1",
					"--logging.level.io.awspring.cloud.secretsmanager=debug")) {
				assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");

				// update secret value
				SecretsManagerClient smClient = context.getBean(SecretsManagerClient.class);
				smClient.putSecretValue(
						r -> r.secretId("/config/spring").secretString("{\"message\":\"new value\"}").build());

				await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
					assertThat(context.getEnvironment().getProperty("message")).isEqualTo("new value");
				});
			}
		}
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport) {
		return runApplication(application, springConfigImport, "spring.cloud.aws.secretsmanager.endpoint");
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport,
			String endpointProperty) {
		return application.run("--spring.config.import=" + springConfigImport,
				"--spring.cloud.aws.secretsmanager.region=" + REGION,
				"--" + endpointProperty + "=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=eu-west-1", "--logging.level.io.awspring.cloud.secretsmanager=debug");
	}

	private static void createSecret(LocalStackContainer localstack, String secretName, String parameterValue,
			String region) {
		try {
			localstack.execInContainer("awslocal", "secretsmanager", "create-secret", "--name", secretName,
					"--secret-string", parameterValue, "--region", region);
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static void createSecretBlob(LocalStackContainer localstack, String secretName, SdkBytes byteValue,
			String region) {
		try {
			localstack.execInContainer("awslocal", "secretsmanager", "create-secret", "--name", secretName,
					"--secret-binary", byteValue.asUtf8String(), "--region", region);
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static void putSecretValue(LocalStackContainer localstack, String secretName, String parameterValue,
			String region) {
		try {
			localstack.execInContainer("awslocal", "secretsmanager", "put-secret-value", "--secret-id", secretName,
					"--secret-string", parameterValue, "--region", region);
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class App {
	}

	static class CustomizerConfiguration implements BootstrapRegistryInitializer {

		@Override
		public void initialize(BootstrapRegistry registry) {
			registry.register(SecretsManagerClientCustomizer.class, context -> (builder -> {
				builder.overrideConfiguration(builder.overrideConfiguration().copy(c -> {
					c.apiCallTimeout(Duration.ofMillis(2001));
				}));
			}));
			registry.register(AwsSyncClientCustomizer.class, context -> (builder -> {
				builder.httpClient(ApacheHttpClient.builder().connectionTimeout(Duration.ofMillis(1542)).build());
			}));
		}
	}

}
