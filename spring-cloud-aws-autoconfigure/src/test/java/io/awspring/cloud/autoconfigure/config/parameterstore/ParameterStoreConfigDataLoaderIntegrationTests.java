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
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterType;

/**
 * Integration tests for loading configuration properties from AWS Parameter Store.
 *
 * @author Maciej Walkowiak
 * @author Matej Nedic
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class ParameterStoreConfigDataLoaderIntegrationTests {

	private static final String REGION = "us-east-1";
	private static final String NEW_LINE_CHAR = System.lineSeparator();

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:3.2.0"));

	@BeforeAll
	static void beforeAll() {
		putParameter(localstack, "/config/spring/message", "value from tests", REGION);
		putParameter(localstack, "/config/spring/another-parameter", "another parameter value", REGION);
		putParameter(localstack, "/config/second/secondMessage", "second value from tests", REGION);
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
	void propertyIsNotResolvedWhenIntegrationIsDisabled() {
		SpringApplication application = new SpringApplication(ParameterStoreConfigDataLoaderIntegrationTests.App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-parameterstore:/config/spring/",
				"--spring.cloud.aws.parameterstore.enabled=false", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=eu-west-1")) {
			assertThat(context.getEnvironment().getProperty("message")).isNull();
			assertThat(context.getBeanProvider(SsmClient.class).getIfAvailable()).isNull();
		}
	}

	@Test
	void resolvesPropertiesWithPrefixes() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-parameterstore:/config/spring/?prefix=first.;/config/second/?prefix=second.")) {
			assertThat(context.getEnvironment().getProperty("first.message")).isEqualTo("value from tests");
			assertThat(context.getEnvironment().getProperty("first.another-parameter"))
					.isEqualTo("another parameter value");
			assertThat(context.getEnvironment().getProperty("second.secondMessage"))
					.isEqualTo("second value from tests");
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
	void clientIsConfiguredWithCustomizerProvidedToBootstrapRegistry() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new CustomizerConfiguration());

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-parameterstore:/config/spring/")) {
			ConfiguredAwsClient ssmClient = new ConfiguredAwsClient(context.getBean(SsmClient.class));
			assertThat(ssmClient.getApiCallTimeout()).isEqualTo(Duration.ofMillis(2001));
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
			// Ensure that new line character should be platform independent
			String errorMessage = "Description:%1$s%1$sCould not import properties from AWS Parameter Store"
					.formatted(NEW_LINE_CHAR);
			assertThat(output.getOut()).contains(errorMessage);
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
		AwsCredentialsProvider bootstrapCredentialsProvider = StaticCredentialsProvider
				.create(AwsBasicCredentials.create("mock-key", "mock-secret"));
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(registry -> {
			registry.register(AwsCredentialsProvider.class, ctx -> bootstrapCredentialsProvider);
		});

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-parameterstore:/config/spring")) {
			ConfiguredAwsClient ssmClient = new ConfiguredAwsClient(context.getBean(SsmClient.class));
			assertThat(ssmClient.getAwsCredentialsProvider()).isEqualTo(bootstrapCredentialsProvider);
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
				"--spring.cloud.aws.parameterstore.endpoint=" + localstack.getEndpoint(),
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
				"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=" + REGION,
				"--logging.level.io.awspring.cloud.parameterstore=debug")) {
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
		}
	}

	@Test
	void arrayParameterNames() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		putParameter(localstack, "/config/myservice/key_0_.value", "value1", REGION);
		putParameter(localstack, "/config/myservice/key_0_.nested_0_.nestedValue", "key_nestedValue1", REGION);
		putParameter(localstack, "/config/myservice/key_0_.nested_1_.nestedValue", "key_nestedValue2", REGION);
		putParameter(localstack, "/config/myservice/key_1_.value", "value2", REGION);
		putParameter(localstack, "/config/myservice/key_1_.nested_0_.nestedValue", "key_nestedValue3", REGION);
		putParameter(localstack, "/config/myservice/key_1_.nested_1_.nestedValue", "key_nestedValue4", REGION);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-parameterstore:/config/myservice/")) {
			assertThat(context.getEnvironment().getProperty("key[0].value")).isEqualTo("value1");
			assertThat(context.getEnvironment().getProperty("key[0].nested[0].nestedValue"))
					.isEqualTo("key_nestedValue1");
			assertThat(context.getEnvironment().getProperty("key[0].nested[1].nestedValue"))
					.isEqualTo("key_nestedValue2");
			assertThat(context.getEnvironment().getProperty("key[1].value")).isEqualTo("value2");
			assertThat(context.getEnvironment().getProperty("key[1].nested[0].nestedValue"))
					.isEqualTo("key_nestedValue3");
			assertThat(context.getEnvironment().getProperty("key[1].nested[1].nestedValue"))
					.isEqualTo("key_nestedValue4");
		}
	}

	@Nested
	class ReloadConfigurationTests {

		@AfterEach
		void resetParameterValue() {
			overwriteParameter(localstack, "/config/spring/message", "value from tests", REGION);
		}

		@Test
		void reloadsPropertiesWhenPropertyValueChanges() {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			try (ConfigurableApplicationContext context = application.run(
					"--spring.config.import=aws-parameterstore:/config/spring/",
					"--spring.cloud.aws.parameterstore.reload.strategy=refresh",
					"--spring.cloud.aws.parameterstore.reload.period=PT1S",
					"--spring.cloud.aws.parameterstore.region=" + REGION,
					"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
					"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
					"--spring.cloud.aws.region.static=eu-west-1")) {
				assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");

				// update parameter value
				SsmClient ssmClient = context.getBean(SsmClient.class);
				ssmClient.putParameter(r -> r.name("/config/spring/message").value("new value")
						.type(ParameterType.STRING).overwrite(true).build());

				await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
					assertThat(context.getEnvironment().getProperty("message")).isEqualTo("new value");
				});
			}
		}

		@Test
		void reloadsPropertiesWhenNewPropertyIsAdded() {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			try (ConfigurableApplicationContext context = application.run(
					"--spring.config.import=aws-parameterstore:/config/spring/",
					"--spring.cloud.aws.parameterstore.reload.strategy=refresh",
					"--spring.cloud.aws.parameterstore.reload.period=PT1S",
					"--spring.cloud.aws.parameterstore.region=" + REGION,
					"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
					"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
					"--spring.cloud.aws.region.static=eu-west-1")) {
				assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");

				// update parameter value
				SsmClient ssmClient = context.getBean(SsmClient.class);
				ssmClient.putParameter(r -> r.name("/config/spring/new-property").value("new value")
						.type(ParameterType.STRING).overwrite(true).build());

				await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
					assertThat(context.getEnvironment().getProperty("new-property")).isEqualTo("new value");
				});
			}
		}

		@Test
		void doesNotReloadPropertiesWhenReloadStrategyIsNotSet() {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			try (ConfigurableApplicationContext context = application.run(
					"--spring.config.import=aws-parameterstore:/config/spring/",
					"--spring.cloud.aws.parameterstore.reload.period=PT1S",
					"--spring.cloud.aws.parameterstore.region=" + REGION,
					"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
					"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
					"--spring.cloud.aws.region.static=eu-west-1")) {
				assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");

				// update parameter value
				SsmClient ssmClient = context.getBean(SsmClient.class);
				ssmClient.putParameter(r -> r.name("/config/spring/message").value("new value")
						.type(ParameterType.STRING).overwrite(true).build());

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
					"--spring.config.import=aws-parameterstore:/config/spring/",
					"--spring.cloud.aws.parameterstore.reload.strategy=restart_context",
					"--spring.cloud.aws.parameterstore.reload.period=PT1S",
					"--spring.cloud.aws.parameterstore.region=" + REGION,
					"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
					"--management.endpoint.restart.enabled=true", "--management.endpoints.web.exposure.include=restart",
					"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
					"--spring.cloud.aws.region.static=eu-west-1")) {
				assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");

				// update parameter value
				SsmClient ssmClient = context.getBean(SsmClient.class);
				ssmClient.putParameter(r -> r.name("/config/spring/message").value("new value")
						.type(ParameterType.STRING).overwrite(true).build());

				await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
					assertThat(context.getEnvironment().getProperty("message")).isEqualTo("new value");
				});
			}
		}
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport,
			String endpointProperty) {
		return application.run("--spring.config.import=" + springConfigImport,
				"--spring.cloud.aws.parameterstore.region=" + REGION,
				"--" + endpointProperty + "=" + localstack.getEndpoint(),
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

	private static void overwriteParameter(LocalStackContainer localstack, String parameterName, String parameterValue,
			String region) {
		try {
			localstack.execInContainer("awslocal", "ssm", "put-parameter", "--name", parameterName, "--type", "String",
					"--value", parameterValue, "--region", region, "--overwrite");
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
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

	static class CustomizerConfiguration implements BootstrapRegistryInitializer {

		@Override
		public void initialize(BootstrapRegistry registry) {
			registry.register(SsmClientCustomizer.class, context -> (builder -> {
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
