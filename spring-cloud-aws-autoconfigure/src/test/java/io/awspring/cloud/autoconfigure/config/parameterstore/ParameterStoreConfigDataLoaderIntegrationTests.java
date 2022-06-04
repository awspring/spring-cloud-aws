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
package io.awspring.cloud.autoconfigure.config.parameterstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SSM;

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
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

/**
 * Integration tests for loading configuration properties from AWS Parameter Store.
 *
 * @author Maciej Walkowiak
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class ParameterStoreConfigDataLoaderIntegrationTests {

	private static final String REGION = "us-east-1";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.2")).withServices(SSM).withReuse(true);

	@BeforeAll
	static void beforeAll() {
		putParameter(localstack, "/config/spring/message", "value from tests", REGION);
		putParameter(localstack, "/config/spring/another-parameter", "another parameter value", REGION);
	}

	@Test
	void resolvesPropertyFromParameterStore() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-parameterstore:/config/spring/")) {
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
			assertThat(context.getEnvironment().getProperty("another-parameter")).isEqualTo("another parameter value");
			assertThat(context.getEnvironment().getProperty("non-existing-parameter")).isNull();
		}
	}

	@Test
	void clientIsConfiguredWithConfigurerProvidedToBootstrapRegistry() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfiguration());

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-parameterstore:/config/spring/")) {
			ConfiguredAwsClient ssmClient = new ConfiguredAwsClient(context.getBean(SsmClient.class));
			assertThat(ssmClient.getApiCallTimeout()).isEqualTo(Duration.ofMillis(2828));
			assertThat(ssmClient.getSyncHttpClient()).isNotNull();
		}
	}

	@Test
	void whenKeysAreNotSpecifiedFailsWithHumanReadableFailureMessage(CapturedOutput output) {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application, "aws-parameterstore:")) {
			fail("Context without keys should fail to start");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(ParameterStoreKeysMissingException.class);
			// ensure that failure analyzer catches the exception and provides meaningful
			// error message
			assertThat(output.getOut())
					.contains("Description:\n" + "\n" + "Could not import properties from AWS Parameter Store");
		}
	}

	@Test
	void ssmClientCanBeOverwrittenInBootstrapConfig() {
		SsmClient mockClient = mock(SsmClient.class);
		when(mockClient.getParametersByPath(any(GetParametersByPathRequest.class)))
				.thenReturn(GetParametersByPathResponse.builder()
						.parameters(Parameter.builder().name("message").value("value from mock").build()).build());
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(registry -> {
			registry.register(SsmClient.class, ctx -> mockClient);
		});

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-parameterstore:/config/spring")) {
			SsmClient clientFromContext = context.getBean(SsmClient.class);
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
				"aws-parameterstore:/config/spring")) {
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
				"aws-parameterstore:/config/spring/")) {
			context.getEnvironment().getProperty("message");
			assertThat(output.getAll()).contains("Populating property retrieved from AWS Parameter Store: message");
		}
	}

	@Test
	void endpointCanBeOverwrittenWithGlobalAwsProperties() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application, "aws-parameterstore:/config/spring/",
				"spring.cloud.aws.endpoint")) {
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
		}
	}

	@Test
	void serviceSpecificEndpointTakesPrecedenceOverGlobalAwsRegion() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-parameterstore:/config/spring/",
				"--spring.cloud.aws.parameterstore.region=" + REGION,
				"--spring.cloud.aws.endpoint=http://non-existing-host/",
				"--spring.cloud.aws.parameterstore.endpoint=" + localstack.getEndpointOverride(SSM).toString(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=eu-west-1",
				"--logging.level.io.awspring.cloud.parameterstore=debug")) {
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
		}
	}

	@Test
	void parameterStoreClientUsesGlobalRegion() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-parameterstore:/config/spring/",
				"--spring.cloud.aws.endpoint=" + localstack.getEndpointOverride(SSM).toString(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=" + REGION,
				"--logging.level.io.awspring.cloud.parameterstore=debug")) {
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
		}
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport,
			String endpointProperty) {
		return application.run("--spring.config.import=" + springConfigImport,
				"--spring.cloud.aws.parameterstore.region=" + REGION,
				"--" + endpointProperty + "=" + localstack.getEndpointOverride(SSM).toString(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=eu-west-1", "--logging.level.io.awspring.cloud.parameterstore=debug");
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport) {
		return runApplication(application, springConfigImport, "spring.cloud.aws.parameterstore.endpoint");
	}

	private static void putParameter(LocalStackContainer localstack, String parameterName, String parameterValue,
			String region) {
		try {
			localstack.execInContainer("awslocal", "ssm", "put-parameter", "--name", parameterName, "--type", "String",
					"--value", parameterValue, "--region", region);
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
			registry.register(AwsParameterStoreClientCustomizer.class,
					context -> new AwsParameterStoreClientCustomizer() {

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
