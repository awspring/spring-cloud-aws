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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.*;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;

/**
 * Integration tests for loading configuration properties from AWS AppConfig.
 *
 * @author Matej Nedic
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@EnabledIfEnvironmentVariable(named = "LOCALSTACK_AUTH_TOKEN", matches = ".+", disabledReason = "Requires LocalStack Pro image")
class AppConfigConfigDataLoaderIntegrationTests {

	private static final String NEW_LINE_CHAR = System.lineSeparator();
	private static final String REGION = "eu-central-1";
	private static AppConfigClient appConfigClient;
	private static String APP_ID;
	private static String ENV_ID;
	private static String STRATEGY_ID;
	private static String PROFILE_ID_PROPERTIES;
	private static String PROFILE_ID_YAML;
	private static String PROFILE_ID_JSON;
	private static String IMPORT_PROPERTIES;
	private static String IMPORT_YAML;
	private static String IMPORT_JSON;

	private static final String api_key = System.getenv("LOCALSTACK_AUTH_TOKEN");

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack-pro:4.4.0")).withEnv("LOCALSTACK_AUTH_TOKEN", api_key)
			.withEnv("AWS_DEFAULT_REGION", REGION);

	@BeforeAll
	static void beforeAll() throws IOException {
		appConfigClient = AppConfigClient.builder().endpointOverride(localstack.getEndpoint()).region(Region.of(REGION))
				.credentialsProvider(StaticCredentialsProvider
						.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
				.build();

		CreateApplicationResponse appResponse = appConfigClient.createApplication(
				CreateApplicationRequest.builder().name("myApp").description("My Application").build());
		APP_ID = appResponse.id();

		CreateEnvironmentResponse envResponse = appConfigClient.createEnvironment(CreateEnvironmentRequest.builder()
				.applicationId(APP_ID).name("myEnv").description("My Environment").build());
		ENV_ID = envResponse.id();

		CreateDeploymentStrategyResponse strategyResponse = appConfigClient.createDeploymentStrategy(
				CreateDeploymentStrategyRequest.builder().name("myStrategy").description("My Strategy")
						.deploymentDurationInMinutes(0).growthFactor(100.0f).finalBakeTimeInMinutes(0).build());
		STRATEGY_ID = strategyResponse.id();

		PROFILE_ID_PROPERTIES = createProfileWithContent("propertiesProfile", "text/plain",
				"io/awspring/cloud/autoconfigure/config/appconfig/test-config.properties");

		PROFILE_ID_YAML = createProfileWithContent("yamlProfile", "application/x-yaml",
				"io/awspring/cloud/autoconfigure/config/appconfig/test-config.yaml");

		PROFILE_ID_JSON = createProfileWithContent("jsonProfile", "application/json",
				"io/awspring/cloud/autoconfigure/config/appconfig/test-config.json");

		IMPORT_PROPERTIES = "aws-appconfig:" + APP_ID + "#" + PROFILE_ID_PROPERTIES + "#" + ENV_ID;
		IMPORT_YAML = "aws-appconfig:" + APP_ID + "#" + PROFILE_ID_YAML + "#" + ENV_ID;
		IMPORT_JSON = "aws-appconfig:" + APP_ID + "#" + PROFILE_ID_JSON + "#" + ENV_ID;
	}

	private static String createProfileWithContent(String profileName, String contentType, String resourcePath)
			throws IOException {
		CreateConfigurationProfileResponse profileResponse = appConfigClient
				.createConfigurationProfile(CreateConfigurationProfileRequest.builder().applicationId(APP_ID)
						.name(profileName).locationUri("hosted").build());

		ClassPathResource resource = new ClassPathResource(resourcePath);
		String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		appConfigClient.createHostedConfigurationVersion(CreateHostedConfigurationVersionRequest.builder()
				.applicationId(APP_ID).configurationProfileId(profileResponse.id())
				.content(SdkBytes.fromUtf8String(content)).contentType(contentType).build());

		appConfigClient.startDeployment(StartDeploymentRequest.builder().applicationId(APP_ID).environmentId(ENV_ID)
				.deploymentStrategyId(STRATEGY_ID).configurationProfileId(profileResponse.id())
				.configurationVersion("1").build());

		return profileResponse.id();
	}

	@Test
	void resolvesPropertyFromAppConfig() {
		SpringApplication application = createApplication();

		try (ConfigurableApplicationContext context = runApplication(application, IMPORT_PROPERTIES)) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("some.property.to.be.checked")).isEqualTo("yes");
		}
	}

	@Test
	void whenKeysAreNotSpecifiedFailsWithHumanReadableFailureMessage(CapturedOutput output) {
		SpringApplication application = createApplication();

		assertThatThrownBy(() -> runApplication(application, "aws-appconfig:"))
				.isInstanceOf(AppConfigKeysMissingException.class);
		String errorMessage = "Description:%1$s%1$sCould not import properties from AWS App Config"
				.formatted(NEW_LINE_CHAR);
		assertThat(output.getOut()).contains(errorMessage);
	}

	@Test
	void whenKeysCannotBeFoundFailWithHumanReadableMessage(CapturedOutput output) {
		SpringApplication application = createApplication();

		assertThatThrownBy(() -> runApplication(application, "aws-appconfig:invalidApp#invalidProfile#invalidEnv"))
				.isInstanceOf(AwsAppConfigPropertySourceNotFoundException.class);
		String errorMessage = "Description:%1$s%1$sCould not import properties from App Config. Exception happened while trying to load the keys"
				.formatted(NEW_LINE_CHAR);
		assertThat(output.getOut()).contains(errorMessage);
	}

	@Test
	void propertyIsNotResolvedWhenIntegrationIsDisabled() {
		SpringApplication application = createApplication();

		try (ConfigurableApplicationContext context = runApplication(application, IMPORT_PROPERTIES,
				"spring.cloud.aws.endpoint", "--spring.cloud.aws.appconfig.enabled=false")) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isNull();
			assertThat(context.getBeanProvider(AppConfigDataClient.class).getIfAvailable()).isNull();
		}
	}

	@Test
	void customSeparatorIsRespected() {
		SpringApplication application = createApplication();

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:" + APP_ID + "/" + PROFILE_ID_PROPERTIES + "/" + ENV_ID, "spring.cloud.aws.endpoint",
				"--spring.cloud.aws.appconfig.separator=/")) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
		}
	}

	@Test
	void resolvesPropertiesFromYamlContentType() {
		SpringApplication application = createApplication();

		try (ConfigurableApplicationContext context = runApplication(application, IMPORT_YAML)) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("some.property.to.be.checked")).isEqualTo("no");
		}
	}

	@Test
	void resolvesPropertiesFromJsonContentType() {
		SpringApplication application = createApplication();

		try (ConfigurableApplicationContext context = runApplication(application, IMPORT_JSON)) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("some.property.to.be.checked")).isEqualTo("yes");
		}
	}

	@Nested
	class ReloadConfigurationTests {

		@AfterEach
		void resetConfiguration() throws IOException {
			updateAppConfigConfiguration(PROFILE_ID_PROPERTIES,
					"io/awspring/cloud/autoconfigure/config/appconfig/test-config.properties");
		}

		@Test
		void reloadsProperties() throws IOException {
			SpringApplication application = createApplication();

			try (ConfigurableApplicationContext context = runApplication(application, IMPORT_PROPERTIES,
					"spring.cloud.aws.appconfig.endpoint", "--spring.cloud.aws.appconfig.reload.strategy=refresh",
					"--spring.cloud.aws.appconfig.reload.period=PT1S")) {
				assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");

				updateAppConfigConfiguration(PROFILE_ID_PROPERTIES,
						"io/awspring/cloud/autoconfigure/config/appconfig/test-config-updated.properties");

				await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
					assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("false");
					assertThat(context.getEnvironment().getProperty("some.property.to.be.checked"))
							.isEqualTo("updated");
				});
			}
		}

		@Test
		void doesNotReloadPropertiesWhenMonitoringIsDisabled() throws IOException {
			SpringApplication application = createApplication();

			try (ConfigurableApplicationContext context = runApplication(application, IMPORT_PROPERTIES,
					"spring.cloud.aws.appconfig.endpoint", "--spring.cloud.aws.appconfig.reload.period=PT1S")) {
				assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");

				updateAppConfigConfiguration(PROFILE_ID_PROPERTIES,
						"io/awspring/cloud/autoconfigure/config/appconfig/test-config-updated.properties");

				await().during(Duration.ofSeconds(5)).untilAsserted(() -> {
					assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
				});
			}
		}

		@Test
		void reloadsPropertiesWithRestartContextStrategy() throws IOException {
			SpringApplication application = createApplication();

			try (ConfigurableApplicationContext context = runApplication(application, IMPORT_PROPERTIES,
					"spring.cloud.aws.appconfig.endpoint",
					"--spring.cloud.aws.appconfig.reload.strategy=RESTART_CONTEXT",
					"--spring.cloud.aws.appconfig.reload.period=PT1S",
					"--spring.cloud.aws.appconfig.reload.max-wait-for-restart=PT1S",
					"--management.endpoint.restart.enabled=true",
					"--management.endpoints.web.exposure.include=restart")) {
				assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");

				updateAppConfigConfiguration(PROFILE_ID_PROPERTIES,
						"io/awspring/cloud/autoconfigure/config/appconfig/test-config-updated.properties");

				await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
					assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("false");
					assertThat(context.getEnvironment().getProperty("some.property.to.be.checked"))
							.isEqualTo("updated");
				});
			}
		}

		private void updateAppConfigConfiguration(String profileId, String resourcePath) throws IOException {
			ClassPathResource resource = new ClassPathResource(resourcePath);
			String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

			// Create new version
			CreateHostedConfigurationVersionResponse versionResponse = appConfigClient
					.createHostedConfigurationVersion(CreateHostedConfigurationVersionRequest.builder()
							.applicationId(APP_ID).configurationProfileId(profileId)
							.content(SdkBytes.fromUtf8String(content)).contentType("text/plain").build());

			appConfigClient.startDeployment(StartDeploymentRequest.builder().applicationId(APP_ID).environmentId(ENV_ID)
					.deploymentStrategyId(STRATEGY_ID).configurationProfileId(profileId)
					.configurationVersion(String.valueOf(versionResponse.versionNumber())).build());
		}
	}

	private SpringApplication createApplication() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		return application;
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport) {
		return runApplication(application, springConfigImport, "spring.cloud.aws.appconfig.endpoint");
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport,
			String endpointProperty, String... extraArgs) {
		List<String> args = new ArrayList<>(List.of("--spring.config.import=" + springConfigImport,
				"--spring.cloud.aws.appconfig.region=" + REGION,
				"--" + endpointProperty + "=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=" + localstack.getAccessKey(),
				"--spring.cloud.aws.credentials.secret-key=" + localstack.getSecretKey(),
				"--spring.cloud.aws.region.static=" + REGION, "--logging.level.io.awspring.cloud.appconfig=debug"));
		args.addAll(List.of(extraArgs));
		return application.run(args.toArray(String[]::new));
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class App {
	}
}
