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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SECRETSMANAGER;

import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * Integration tests for loading configuration properties from AWS Secrets Manager.
 *
 * @author Maciej Walkowiak
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class SecretsManagerConfigDataLoaderIntegrationTests {

	private static final String REGION = "us-east-1";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.2")).withServices(SECRETSMANAGER).withReuse(true);

	@BeforeAll
	static void beforeAll() {
		createSecret(localstack, "/config/spring",
				"{\"message\":\"value from tests\", \"another-parameter\": \"another parameter value\"}", REGION);
		createSecret(localstack, "/certs/prod/fn_certificate", "=== my prod cert should be here", REGION);
		createSecret(localstack, "/certs/dev/fn_certificate/", "=== my dev cert should be here", REGION);
		createSecret(localstack, "fn_certificate", "=== my cert should be here", REGION);
		createSecret(localstack, "/config/second", "{\"secondMessage\":\"second value from tests\"}", REGION);
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
	void resolvesPropertyFromSecretsManager_PlainTextSecret_WithoutSlashes() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:fn_certificate")) {
			assertThat(context.getEnvironment().getProperty("fn_certificate")).isEqualTo("=== my cert should be here");
		}
	}

	@Test
	void clientIsConfiguredWithConfigurerProvidedToBootstrapRegistry() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfiguration());

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:/config/spring;/config/second")) {
			ConfiguredAwsClient ssmClient = new ConfiguredAwsClient(context.getBean(SecretsManagerClient.class));
			assertThat(ssmClient.getApiCallTimeout()).isEqualTo(Duration.ofMillis(2828));
			assertThat(ssmClient.getSyncHttpClient()).isNotNull();
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
			assertThat(output.getOut())
					.contains("Description:\n" + "\n" + "Could not import properties from AWS Secrets Manager");
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
		AwsCredentialsProvider mockCredentialsProvider = mock(AwsCredentialsProvider.class);
		when(mockCredentialsProvider.resolveCredentials())
				.thenReturn(AwsBasicCredentials.create("mock-key", "mock-secret"));
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(registry -> {
			registry.register(AwsCredentialsProvider.class, ctx -> mockCredentialsProvider);
		});

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-secretsmanager:/config/spring")) {
			// perhaps there is a better way to verify that correct credentials provider
			// is used by SSM client without using reflection?
			verify(mockCredentialsProvider).resolveCredentials();
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
	void serviceSpecificEndpointTakesPrecedenceOverGlobalAwsRegion() {
		SpringApplication application = new SpringApplication(SecretsManagerConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-secretsmanager:/config/spring;/config/second",
				"--spring.cloud.aws.secretsmanager.region=" + REGION,
				"--spring.cloud.aws.endpoint=http://non-existing-host/",
				"--spring.cloud.aws.secretsmanager.endpoint="
						+ localstack.getEndpointOverride(SECRETSMANAGER).toString(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=eu-west-1")) {
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
		}
	}

	@Test
	void secretsManagerClientUsesGlobalRegion() {
		SpringApplication application = new SpringApplication(SecretsManagerConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-secretsmanager:/config/spring;/config/second",
				"--spring.cloud.aws.endpoint=" + localstack.getEndpointOverride(SECRETSMANAGER).toString(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=" + REGION)) {
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
		}
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport) {
		return runApplication(application, springConfigImport, "spring.cloud.aws.secretsmanager.endpoint");
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport,
			String endpointProperty) {
		return application.run("--spring.config.import=" + springConfigImport,
				"--spring.cloud.aws.secretsmanager.region=" + REGION,
				"--" + endpointProperty + "=" + localstack.getEndpointOverride(SECRETSMANAGER).toString(),
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

	@SpringBootApplication
	static class App {

	}

	static class AwsConfigurerClientConfiguration implements BootstrapRegistryInitializer {

		@Override
		public void initialize(BootstrapRegistry registry) {
			registry.register(AwsSecretsManagerClientCustomizer.class,
					context -> new AwsSecretsManagerClientCustomizer() {

						@Override
						public ClientOverrideConfiguration overrideConfiguration() {
							return ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofMillis(2828))
									.build();
						}

						@Override
						public SdkHttpClient httpClient() {
							return ApacheHttpClient.builder().connectionTimeout(Duration.ofMillis(1542)).build();
						}
					});
		}
	}

}
