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
import static org.assertj.core.api.Assertions.fail;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.bootstrap.BootstrapRegistryInitializer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
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
class AppConfigConfigDataLoaderIntegrationTests {

	private static final String NEW_LINE_CHAR = System.lineSeparator();
	private static String APP_ID;
	private static String ENV_ID;
	private static String PROFILE_ID_PROPERTIES;
	private static String PROFILE_ID_YAML;
	private static String PROFILE_ID_JSON;

	private static String api_key = System.getenv("LOCALSTACK_AUTH_TOKEN");

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
		DockerImageName.parse("localstack/localstack-pro:latest"))
		.withEnv("LOCALSTACK_AUTH_TOKEN", api_key).withReuse(false);

	@BeforeAll
	static void beforeAll() throws IOException {

        try (AppConfigClient appConfigClient = AppConfigClient.builder()
                .endpointOverride(localstack.getEndpoint())
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .build()) {
            CreateApplicationResponse appResponse = appConfigClient.createApplication(
                    CreateApplicationRequest.builder()
                            .name("myApp")
                            .description("My Application")
                            .build());
            APP_ID = appResponse.id();

            CreateEnvironmentResponse envResponse = appConfigClient.createEnvironment(
                    CreateEnvironmentRequest.builder()
                            .applicationId(APP_ID)
                            .name("myEnv")
                            .description("My Environment")
                            .build());
            ENV_ID = envResponse.id();

            CreateDeploymentStrategyResponse strategyResponse = appConfigClient.createDeploymentStrategy(
                    CreateDeploymentStrategyRequest.builder()
                            .name("myStrategy")
                            .description("My Strategy")
                            .deploymentDurationInMinutes(0)
                            .growthFactor(100.0f)
                            .finalBakeTimeInMinutes(0)
                            .build());

            PROFILE_ID_PROPERTIES = createProfileWithContent(appConfigClient, APP_ID, ENV_ID, strategyResponse.id(),
                    "propertiesProfile", "text/plain",
                    "io/awspring/cloud/autoconfigure/config/appconfig/test-config.properties");

            PROFILE_ID_YAML = createProfileWithContent(appConfigClient, APP_ID, ENV_ID, strategyResponse.id(),
                    "yamlProfile", "application/x-yaml",
                    "io/awspring/cloud/autoconfigure/config/appconfig/test-config.yaml");

            PROFILE_ID_JSON = createProfileWithContent(appConfigClient, APP_ID, ENV_ID, strategyResponse.id(),
                    "jsonProfile", "application/json",
                    "io/awspring/cloud/autoconfigure/config/appconfig/test-config.json");
        }
	}

	private static String createProfileWithContent(AppConfigClient appConfigClient, String appId, String envId,
			String strategyId, String profileName, String contentType, String resourcePath) throws IOException {
		CreateConfigurationProfileResponse profileResponse = appConfigClient.createConfigurationProfile(
			CreateConfigurationProfileRequest.builder()
				.applicationId(appId)
				.name(profileName)
				.locationUri("hosted")
				.build());

		ClassPathResource resource = new ClassPathResource(resourcePath);
		String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		appConfigClient.createHostedConfigurationVersion(
			CreateHostedConfigurationVersionRequest.builder()
				.applicationId(appId)
				.configurationProfileId(profileResponse.id())
				.content(SdkBytes.fromUtf8String(content))
				.contentType(contentType)
				.build());

		appConfigClient.startDeployment(
			StartDeploymentRequest.builder()
				.applicationId(appId)
				.environmentId(envId)
				.deploymentStrategyId(strategyId)
				.configurationProfileId(profileResponse.id())
				.configurationVersion("1")
				.build());

		return profileResponse.id();
	}

	@Test
	void resolvesPropertyFromAppConfig() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:" + PROFILE_ID_PROPERTIES + "#" + ENV_ID + "#" + APP_ID)) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("some.property.to.be.checked")).isEqualTo("yes");
		}
	}

	@Test
	void clientIsConfiguredWithCustomizerProvidedToBootstrapRegistry() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(new CustomizerConfiguration());

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:" + PROFILE_ID_PROPERTIES + "#" + ENV_ID + "#" + APP_ID)) {
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(AppConfigDataClient.class));
			assertThat(client.getApiCallTimeout()).isEqualTo(Duration.ofMillis(2001));
			assertThat(client.getSyncHttpClient()).isNotNull();
		}
	}

	@Test
	void whenKeysAreNotSpecifiedFailsWithHumanReadableFailureMessage(CapturedOutput output) {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application, "aws-appconfig:")) {
			fail("Context without keys should fail to start");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(AppConfigKeysMissingException.class);
			String errorMessage = "Description:%1$s%1$sCould not import properties from AWS App Config"
					.formatted(NEW_LINE_CHAR);
			assertThat(output.getOut()).contains(errorMessage);
		}
	}

	@Test
	void whenKeysCannotBeFoundFailWithHumanReadableMessage(CapturedOutput output) {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:invalidApp#invalidEnv#invalidProfile")) {
			fail("Context with invalid keys should fail to start");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(AwsAppConfigPropertySourceNotFoundException.class);
			String errorMessage = "Description:%1$s%1$sCould not import properties from App Config. Exception happened while trying to load the keys"
					.formatted(NEW_LINE_CHAR);
			assertThat(output.getOut()).contains(errorMessage);
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
				"aws-appconfig:" + PROFILE_ID_PROPERTIES + "#" + ENV_ID + "#" + APP_ID)) {
			ConfiguredAwsClient appConfigDataClient = new ConfiguredAwsClient(
					context.getBean(AppConfigDataClient.class));
			assertThat(appConfigDataClient.getAwsCredentialsProvider()).isEqualTo(bootstrapCredentialsProvider);
		}
	}

	@Test
	void endpointCanBeOverwrittenWithGlobalAwsProperties() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:" + PROFILE_ID_PROPERTIES + "#" + ENV_ID + "#" + APP_ID, "spring.cloud.aws.endpoint")) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
		}
	}

	@Test
	void propertyIsNotResolvedWhenIntegrationIsDisabled() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-appconfig:" + PROFILE_ID_PROPERTIES + "#" + ENV_ID + "#" + APP_ID,
				"--spring.cloud.aws.appconfig.enabled=false",
				"--spring.cloud.aws.credentials.access-key=" + localstack.getAccessKey(),
				"--spring.cloud.aws.credentials.secret-key=" + localstack.getSecretKey(),
				"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
				"--spring.cloud.aws.region.static=eu-west-1")) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isNull();
			assertThat(context.getBeanProvider(AppConfigDataClient.class).getIfAvailable()).isNull();
		}
	}

	@Test
	void serviceSpecificEndpointTakesPrecedenceOverGlobalAwsRegion() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-appconfig:" + PROFILE_ID_PROPERTIES + "#" + ENV_ID + "#" + APP_ID,
				"--spring.cloud.aws.appconfig.region=" + localstack.getRegion(),
				"--spring.cloud.aws.endpoint=http://non-existing-host/",
				"--spring.cloud.aws.appconfig.endpoint=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=" + localstack.getAccessKey(),
				"--spring.cloud.aws.credentials.secret-key=" + localstack.getSecretKey(),
				"--spring.cloud.aws.region.static=eu-west-1")) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
			assertThat(context.getBean(AwsCredentialsProvider.class)).isInstanceOf(StaticCredentialsProvider.class);
		}
	}

	@Test
	void appConfigDataClientUsesGlobalRegion() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-appconfig:" + PROFILE_ID_PROPERTIES + "#" + ENV_ID + "#" + APP_ID,
				"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=" + localstack.getAccessKey(),
				"--spring.cloud.aws.credentials.secret-key=" + localstack.getSecretKey(),
				"--spring.cloud.aws.region.static=" + localstack.getRegion())) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
		}
	}

	@Test
	void customSeparatorIsRespected() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-appconfig:" + PROFILE_ID_PROPERTIES + "/" + ENV_ID + "/" + APP_ID,
				"--spring.cloud.aws.appconfig.separator=/",
				"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=" + localstack.getAccessKey(),
				"--spring.cloud.aws.credentials.secret-key=" + localstack.getSecretKey(),
				"--spring.cloud.aws.region.static=" + localstack.getRegion())) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
		}
	}

	@Test
	void resolvesPropertiesFromYamlContentType() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:" + PROFILE_ID_YAML + "#" + ENV_ID + "#" + APP_ID)) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("some.property.to.be.checked")).isEqualTo("no");
		}
	}

	@Test
	void resolvesPropertiesFromJsonContentType() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:" + PROFILE_ID_JSON + "#" + ENV_ID + "#" + APP_ID)) {
			assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("some.property.to.be.checked")).isEqualTo("yes");
		}
	}

	@Nested
	class ReloadConfigurationTests {

		@Test
		void reloadsProperties() throws IOException {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			try (ConfigurableApplicationContext context = application.run(
					"--spring.config.import=aws-appconfig:" + PROFILE_ID_PROPERTIES + "#" + ENV_ID + "#" + APP_ID,
					"--spring.cloud.aws.appconfig.reload.strategy=refresh",
					"--spring.cloud.aws.appconfig.reload.period=PT1S",
					"--spring.cloud.aws.appconfig.region=" + localstack.getRegion(),
					"--spring.cloud.aws.appconfig.endpoint=" + localstack.getEndpoint(),
					"--spring.cloud.aws.credentials.access-key=" + localstack.getAccessKey(),
					"--spring.cloud.aws.credentials.secret-key=" + localstack.getSecretKey(),
					"--spring.cloud.aws.region.static=" + localstack.getRegion(),
					"--logging.level.io.awspring.cloud.appconfig=debug")) {
				assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");

				updateAppConfigConfiguration(PROFILE_ID_PROPERTIES, 
					"io/awspring/cloud/autoconfigure/config/appconfig/test-config-updated.properties");

				await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
					assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("false");
					assertThat(context.getEnvironment().getProperty("some.property.to.be.checked")).isEqualTo("updated");
				});
			} finally {
				resetAppConfigConfiguration(PROFILE_ID_PROPERTIES, 
					"io/awspring/cloud/autoconfigure/config/appconfig/test-config.properties");
			}
		}


		@Test
		void doesNotReloadPropertiesWhenMonitoringIsDisabled() throws IOException {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			try (ConfigurableApplicationContext context = application.run(
					"--spring.config.import=aws-appconfig:" + PROFILE_ID_PROPERTIES + "#" + ENV_ID + "#" + APP_ID,
					"--spring.cloud.aws.appconfig.reload.period=PT1S",
					"--spring.cloud.aws.appconfig.region=" + localstack.getRegion(),
					"--spring.cloud.aws.appconfig.endpoint=" + localstack.getEndpoint(),
					"--spring.cloud.aws.credentials.access-key=" + localstack.getAccessKey(),
					"--spring.cloud.aws.credentials.secret-key=" + localstack.getSecretKey(),
					"--spring.cloud.aws.region.static=eu-west-1",
					"--logging.level.io.awspring.cloud.appconfig=debug")) {
				assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");

				updateAppConfigConfiguration(PROFILE_ID_PROPERTIES, 
					"io/awspring/cloud/autoconfigure/config/appconfig/test-config-updated.properties");

				await().during(Duration.ofSeconds(5)).untilAsserted(() -> {
					assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");
				});
			} finally {
			resetAppConfigConfiguration(PROFILE_ID_PROPERTIES,
				"io/awspring/cloud/autoconfigure/config/appconfig/test-config.properties");
		}

		}

		@Test
		void reloadsPropertiesWithRestartContextStrategy() throws IOException {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			try (ConfigurableApplicationContext context = application.run(
					"--spring.config.import=aws-appconfig:" + PROFILE_ID_PROPERTIES + "#" + ENV_ID + "#" + APP_ID,
					"--spring.cloud.aws.appconfig.reload.strategy=RESTART_CONTEXT",
					"--spring.cloud.aws.appconfig.reload.period=PT1S",
					"--spring.cloud.aws.appconfig.reload.max-wait-for-restart=PT1S",
					"--management.endpoint.restart.enabled=true",
					"--management.endpoints.web.exposure.include=restart",
					"--spring.cloud.aws.appconfig.region=" + localstack.getRegion(),
					"--spring.cloud.aws.appconfig.endpoint=" + localstack.getEndpoint(),
					"--spring.cloud.aws.credentials.access-key=" + localstack.getAccessKey(),
					"--spring.cloud.aws.credentials.secret-key=" + localstack.getSecretKey(),
					"--spring.cloud.aws.region.static=" + localstack.getRegion(),
					"--logging.level.io.awspring.cloud.appconfig=debug")) {
				assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("true");

				updateAppConfigConfiguration(PROFILE_ID_PROPERTIES, 
					"io/awspring/cloud/autoconfigure/config/appconfig/test-config-updated.properties");

				await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
					assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("false");
					assertThat(context.getEnvironment().getProperty("some.property.to.be.checked")).isEqualTo("updated");
				});
			} finally {
				resetAppConfigConfiguration(PROFILE_ID_PROPERTIES, 
					"io/awspring/cloud/autoconfigure/config/appconfig/test-config.properties");
			}
		}

		private void updateAppConfigConfiguration(String profileId, String resourcePath) throws IOException {

            try (AppConfigClient appConfigClient = AppConfigClient.builder()
                    .endpointOverride(localstack.getEndpoint())
                    .region(Region.of(localstack.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                    .build()) {

				ClassPathResource resource = new ClassPathResource(resourcePath);
				String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                CreateHostedConfigurationVersionResponse versionResponse = appConfigClient.createHostedConfigurationVersion(
                        CreateHostedConfigurationVersionRequest.builder()
                                .applicationId(APP_ID)
                                .configurationProfileId(profileId)
                                .content(SdkBytes.fromUtf8String(content))
                                .contentType("text/plain")
                                .build());

                ListDeploymentStrategiesResponse strategies = appConfigClient.listDeploymentStrategies(
                        ListDeploymentStrategiesRequest.builder().build());
                String strategyId = strategies.items().get(0).id();

                appConfigClient.startDeployment(
                        StartDeploymentRequest.builder()
                                .applicationId(APP_ID)
                                .environmentId(ENV_ID)
                                .deploymentStrategyId(strategyId)
                                .configurationProfileId(profileId)
                                .configurationVersion(String.valueOf(versionResponse.versionNumber()))
                                .build());
            }
		}

		private void resetAppConfigConfiguration(String profileId, String resourcePath) throws IOException {
			updateAppConfigConfiguration(profileId, resourcePath);
		}
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport) {
		return runApplication(application, springConfigImport, "spring.cloud.aws.appconfig.endpoint");
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport,
			String endpointProperty) {
		return application.run("--spring.config.import=" + springConfigImport,
				"--spring.cloud.aws.appconfig.region=" + localstack.getRegion(),
				"--" + endpointProperty + "=" + localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=" + localstack.getAccessKey(),
				"--spring.cloud.aws.credentials.secret-key=" + localstack.getSecretKey(),
				"--spring.cloud.aws.region.static=eu-west-1",
				"--logging.level.io.awspring.cloud.appconfig=debug");
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class App {
	}

	static class CustomizerConfiguration implements BootstrapRegistryInitializer {

		@Override
		public void initialize(BootstrapRegistry registry) {
			registry.register(AppConfigClientCustomizer.class, context -> (builder -> {
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
