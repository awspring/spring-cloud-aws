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
package io.awspring.cloud.autoconfigure.config.appconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import java.io.IOException;
import java.time.Duration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.FileCopyUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;

@ExtendWith(OutputCaptureExtension.class)
class AppConfigDataLoaderTest {

	static final AppConfigDataClient appConfigDataClient = Mockito.mock(AppConfigDataClient.class);

	@BeforeEach
	public void reset() {
		Mockito.reset(appConfigDataClient);
	}

	@Test
	void resolvesYaml() throws IOException {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new AppConfigDataClientInitializer());

		when(appConfigDataClient.startConfigurationSession(
				StartConfigurationSessionRequest.builder().applicationIdentifier("MyApplication")
						.configurationProfileIdentifier("yaml_configuration").environmentIdentifier("test").build()))
								.thenReturn(StartConfigurationSessionResponse.builder().initialConfigurationToken("20")
										.build());
		byte[] yamlFile = FileCopyUtils.copyToByteArray(getClass().getClassLoader()
				.getResourceAsStream("io.awspring.cloud.autoconfigure.config.appconfig/working-spring-dynamodb.yaml"));
		when(appConfigDataClient.getLatestConfiguration((GetLatestConfigurationRequest) any()))
				.thenReturn(GetLatestConfigurationResponse.builder().configuration(SdkBytes.fromByteArray(yamlFile))
						.contentType("application/x-yaml").build());

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:MyApplication___yaml_configuration___test")) {
			Assertions.assertThat(context.getEnvironment().getProperty("spring.cloud.aws.dynamodb.enabled"))
					.isEqualTo("true");
			Assertions.assertThat(context.getEnvironment().getProperty("spring.cloud.aws.dynamodb.endpoint"))
					.isEqualTo("localhost://123123");
		}
	}

	@Test
	void resolvesJson() throws IOException {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new AppConfigDataClientInitializer());

		when(appConfigDataClient.startConfigurationSession(
				StartConfigurationSessionRequest.builder().applicationIdentifier("MyApplication")
						.configurationProfileIdentifier("json_configuration").environmentIdentifier("test").build()))
								.thenReturn(StartConfigurationSessionResponse.builder().initialConfigurationToken("20")
										.build());
		byte[] jsonFile = FileCopyUtils.copyToByteArray(getClass().getClassLoader()
				.getResourceAsStream("io.awspring.cloud.autoconfigure.config.appconfig/working-json-file.json"));
		when(appConfigDataClient.getLatestConfiguration((GetLatestConfigurationRequest) any()))
				.thenReturn(GetLatestConfigurationResponse.builder().configuration(SdkBytes.fromByteArray(jsonFile))
						.contentType("application/json").build());

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:MyApplication___json_configuration___test")) {
			Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.dynamodb.enabled")).isEqualTo("true");
			Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.dynamodb.region"))
					.isEqualTo("eu-west-2");
			Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled")).isEqualTo("false");
			Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.s3.region")).isEqualTo("eu-west-2");
			Assertions.assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8089");
		}
	}

	@Test
	void resolvesTextProperties() throws IOException {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new AppConfigDataClientInitializer());

		when(appConfigDataClient.startConfigurationSession(
				StartConfigurationSessionRequest.builder().applicationIdentifier("MyApplication")
						.configurationProfileIdentifier("text_configuration").environmentIdentifier("test").build()))
								.thenReturn(StartConfigurationSessionResponse.builder().initialConfigurationToken("20")
										.build());
		byte[] textFile = FileCopyUtils.copyToByteArray(getClass().getClassLoader().getResourceAsStream(
				"io.awspring.cloud.autoconfigure.config.appconfig/working-properties-file.properties"));
		when(appConfigDataClient.getLatestConfiguration((GetLatestConfigurationRequest) any()))
				.thenReturn(GetLatestConfigurationResponse.builder().configuration(SdkBytes.fromByteArray(textFile))
						.contentType("text/plain").build());

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:MyApplication___text_configuration___test")) {
			Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("false");
			Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.dynamodb.region"))
					.isEqualTo("eu-west-2");
			Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled")).isEqualTo("false");
		}
	}

	@Test
	void customClientConfiguration() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfiguration());

		try (ConfigurableApplicationContext context = runApplication(application,
				"optional:aws-appconfig:MyApplication___text_configuration___test")) {
			ConfiguredAwsClient appConfigDataClient = new ConfiguredAwsClient(
					context.getBean(AppConfigDataClient.class));
			assertThat(appConfigDataClient.getApiCallTimeout()).isEqualTo(Duration.ofMillis(2828));
			assertThat(appConfigDataClient.getSyncHttpClient()).isNotNull();
		}
	}

	@Test
	void resolveFail_NoKeysFound(CapturedOutput output) {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new AppConfigDataClientInitializer());

		try (ConfigurableApplicationContext context = runApplication(application, "aws-appconfig:")) {
			fail("Context without keys should fail to start");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(AppConfigKeysMissingException.class);
			// ensure that failure analyzer catches the exception and provides meaningful
			// error message
			assertThat(output.getOut())
					.contains("Description:\n" + "\n" + "Could not import properties from AWS AppConfig");
		}
	}

	@Nested
	class ReloadConfigurationTests {

		@BeforeEach
		public void reset() {
			Mockito.reset(appConfigDataClient);
		}

		@Test
		void reloadsProperties() throws IOException {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);
			application.addBootstrapRegistryInitializer(new AppConfigDataClientInitializer());

			when(appConfigDataClient.startConfigurationSession(StartConfigurationSessionRequest.builder()
					.applicationIdentifier("MyApplication").configurationProfileIdentifier("text_configuration")
					.environmentIdentifier("test").build())).thenReturn(
							StartConfigurationSessionResponse.builder().initialConfigurationToken("20").build());
			byte[] textFile = FileCopyUtils.copyToByteArray(getClass().getClassLoader().getResourceAsStream(
					"io.awspring.cloud.autoconfigure.config.appconfig/working-properties-file.properties"));
			when(appConfigDataClient.getLatestConfiguration((GetLatestConfigurationRequest) any()))
					.thenReturn(GetLatestConfigurationResponse.builder().configuration(SdkBytes.fromByteArray(textFile))
							.contentType("text/plain").build());

			try (ConfigurableApplicationContext context = runApplication(application,
					"aws-appconfig:MyApplication___text_configuration___test")) {
				Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("false");
				Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.dynamodb.region"))
						.isEqualTo("eu-west-2");
				Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled")).isEqualTo("false");

				// update parameter value
				byte[] newValues = FileCopyUtils.copyToByteArray(getClass().getClassLoader().getResourceAsStream(
						"io.awspring.cloud.autoconfigure.config.appconfig/changed-value.properties"));
				when(appConfigDataClient.getLatestConfiguration((GetLatestConfigurationRequest) any()))
						.thenReturn(GetLatestConfigurationResponse.builder()
								.configuration(SdkBytes.fromByteArray(newValues)).contentType("text/plain").build());

				await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled"))
							.isEqualTo("true");
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.dynamodb.region"))
							.isEqualTo("eu-central-1");
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled"))
							.isEqualTo("true");
				});
			}
		}

		@Test
		void doesNotReloadTextPropertiesWhenReloadStrategyIsNotSet() throws IOException {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);
			application.addBootstrapRegistryInitializer(new AppConfigDataClientInitializer());

			when(appConfigDataClient.startConfigurationSession(StartConfigurationSessionRequest.builder()
					.applicationIdentifier("MyApplication").configurationProfileIdentifier("text_configuration")
					.environmentIdentifier("test").build())).thenReturn(
							StartConfigurationSessionResponse.builder().initialConfigurationToken("20").build());
			byte[] textFile = FileCopyUtils.copyToByteArray(getClass().getClassLoader().getResourceAsStream(
					"io.awspring.cloud.autoconfigure.config.appconfig/working-properties-file.properties"));
			when(appConfigDataClient.getLatestConfiguration((GetLatestConfigurationRequest) any()))
					.thenReturn(GetLatestConfigurationResponse.builder().configuration(SdkBytes.fromByteArray(textFile))
							.contentType("text/plain").build());

			try (ConfigurableApplicationContext context = application.run(
					"--spring.config.import=aws-appconfig:MyApplication___text_configuration___test",
					"--spring.cloud.aws.appconfig.region=eu-central-1",
					"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
					"--spring.cloud.aws.region.static=eu-west-1")) {

				await().during(Duration.ofSeconds(5)).untilAsserted(() -> {
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled"))
							.isEqualTo("false");
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.dynamodb.region"))
							.isEqualTo("eu-west-2");
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled"))
							.isEqualTo("false");
				});
			}
		}

		@Test
		void reloadsPropertiesWithNewValues() throws IOException {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);
			application.addBootstrapRegistryInitializer(new AppConfigDataClientInitializer());

			when(appConfigDataClient.startConfigurationSession(StartConfigurationSessionRequest.builder()
					.applicationIdentifier("MyApplication").configurationProfileIdentifier("text_configuration")
					.environmentIdentifier("test").build())).thenReturn(
							StartConfigurationSessionResponse.builder().initialConfigurationToken("20").build());
			byte[] textFile = FileCopyUtils.copyToByteArray(getClass().getClassLoader().getResourceAsStream(
					"io.awspring.cloud.autoconfigure.config.appconfig/working-properties-file.properties"));
			when(appConfigDataClient.getLatestConfiguration((GetLatestConfigurationRequest) any()))
					.thenReturn(GetLatestConfigurationResponse.builder().configuration(SdkBytes.fromByteArray(textFile))
							.contentType("text/plain").build());

			try (ConfigurableApplicationContext context = runApplication(application,
					"aws-appconfig:MyApplication___text_configuration___test")) {
				Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("false");
				Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.dynamodb.region"))
						.isEqualTo("eu-west-2");
				Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled")).isEqualTo("false");

				// update parameter value
				byte[] newValues = FileCopyUtils.copyToByteArray(getClass().getClassLoader().getResourceAsStream(
						"io.awspring.cloud.autoconfigure.config.appconfig/removed_values.properties"));
				when(appConfigDataClient.getLatestConfiguration((GetLatestConfigurationRequest) any()))
						.thenReturn(GetLatestConfigurationResponse.builder()
								.configuration(SdkBytes.fromByteArray(newValues)).contentType("text/plain").build());

				await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled"))
							.isEqualTo("true");
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.dynamodb.region"))
							.isEqualTo(null);
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled"))
							.isEqualTo("true");
					Assertions.assertThat(context.getEnvironment().getProperty("some.property.to.be.checked"))
							.isEqualTo("yes");
				});
			}
		}

	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport) {
		return application.run("--spring.config.import=" + springConfigImport,
				"--spring.cloud.aws.appconfig.region=" + "eu-west-2",
				"--spring.cloud.aws.appconfig.reload.strategy=refresh",
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=eu-west-2", "--logging.level.io.awspring.cloud.appconfig=debug",
				"--spring.cloud.aws.appconfig.reload.period=PT1S", "--spring.cloud.aws.s3.enabled=false");
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class App {

	}

	static class AppConfigDataClientInitializer implements BootstrapRegistryInitializer {

		@Override
		public void initialize(BootstrapRegistry registry) {
			registry.register(AppConfigDataClient.class, context -> appConfigDataClient);

		}
	}

	static class AwsConfigurerClientConfiguration implements BootstrapRegistryInitializer {

		@Override
		public void initialize(BootstrapRegistry registry) {
			registry.register(AwsAppConfigDataClientCustomizer.class,
					context -> new AwsAppConfigDataClientCustomizer() {

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
