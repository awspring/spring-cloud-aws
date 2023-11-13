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
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.FileCopyUtils;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.*;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

@ExtendWith(OutputCaptureExtension.class)
@Testcontainers
class AppConfigDataLoaderTest {

	private static final String YAML_TYPE = "application/x-yaml";
	private static final String YAML_TYPE_ALTERNATIVE = "text/yaml";
	private static final String TEXT_TYPE = "text/plain";
	private static final String JSON_TYPE = "application/json";
	
	private static String parameterStoreArn;
	private static String appId;
	private static String configId;
	private static String envId;

	private static AppConfigClient appConfigClient;


	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
		DockerImageName.parse("localstack/localstack-pro:latest")).withReuse(true).withEnv("LOCALSTACK_API_KEY", System.getenv("LOCALSTACK_API_KEY"));


	@BeforeAll
	public static void before() {
		appConfigClient = AppConfigClient.builder().endpointOverride(localstack.getEndpoint()).build();

	}
	@Test
	void resolvesYaml() throws IOException {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		byte[] yamlFile = FileCopyUtils.copyToByteArray(getClass().getClassLoader()
			.getResourceAsStream("io.awspring.cloud.autoconfigure.config.appconfig/working-spring-dynamodb.yaml"));
		prepareEnv(yamlFile,"YamlApplication", "test", "yaml-profile", YAML_TYPE_ALTERNATIVE);
		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:YamlApplication___yaml-profile___test")) {
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

		byte[] jsonFile = FileCopyUtils.copyToByteArray(getClass().getClassLoader()
			.getResourceAsStream("io.awspring.cloud.autoconfigure.config.appconfig/working-json-file.json"));

		prepareEnv(jsonFile,"JsonApplication", "test", "json-profile", JSON_TYPE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:JsonApplication___json-profile___test")) {
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

		byte[] textFile = FileCopyUtils.copyToByteArray(getClass().getClassLoader().getResourceAsStream(
			"io.awspring.cloud.autoconfigure.config.appconfig/working-properties-file.properties"));
		prepareEnv(textFile,"TextApplication", "test", "text_configuration", TEXT_TYPE);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-appconfig:TextApplication___text_configuration___test")) {
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
				"optional:aws-appconfig:RandApplication___rand-configuration___test")) {
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

		@Test
		void reloadsProperties() throws IOException {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			byte[] textFile = FileCopyUtils.copyToByteArray(getClass().getClassLoader().getResourceAsStream(
				"io.awspring.cloud.autoconfigure.config.appconfig/working-properties-file.properties"));
			prepareEnv(textFile,"my_app", "test", "reload-config", TEXT_TYPE);

			try (ConfigurableApplicationContext context = runApplication(application,
					"aws-appconfig:my_app___reload-config___test")) {
				Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("false");
				Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.dynamodb.region"))
						.isEqualTo("eu-west-2");
				Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled")).isEqualTo("false");

				// update parameter value
				byte[] newValues = FileCopyUtils.copyToByteArray(getClass().getClassLoader().getResourceAsStream(
						"io.awspring.cloud.autoconfigure.config.appconfig/changed-value.properties"));
				putParameter(localstack, "test", new String(newValues, StandardCharsets.UTF_8));

				appConfigClient.createHostedConfigurationVersion(CreateHostedConfigurationVersionRequest.builder().content(SdkBytes.fromByteArray(newValues)).contentType(TEXT_TYPE).configurationProfileId(configId).applicationId(appId).latestVersionNumber(2).build());

				await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled"))
							.isEqualTo("true");
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.dynamodb.region"))
							.isEqualTo("eu-west-2");
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.sns.enabled"))
							.isEqualTo("true");
					Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled"))
						.isNull();
				});
			}
		}

		@Test
		void doesNotReloadTextPropertiesWhenReloadStrategyIsNotSet() throws IOException {
			SpringApplication application = new SpringApplication(App.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			byte[] textFile = FileCopyUtils.copyToByteArray(getClass().getClassLoader().getResourceAsStream(
					"io.awspring.cloud.autoconfigure.config.appconfig/working-properties-file.properties"));
			prepareEnv(textFile,"MyApplication", "test", "text_configuration", TEXT_TYPE);

			try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-appconfig:MyApplication___text_configuration___test",
				"--spring.cloud.aws.appconfig.region=" + localstack.getRegion(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.endpoint=" + localstack.getEndpoint()
				)) {

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
			
			
			byte[] textFile = FileCopyUtils.copyToByteArray(getClass().getClassLoader().getResourceAsStream(
					"io.awspring.cloud.autoconfigure.config.appconfig/working-properties-file.properties"));
			prepareEnv(textFile,"testApp", "test", "config", TEXT_TYPE);

			try (ConfigurableApplicationContext context = runApplication(application,
					"aws-appconfig:testApp___config___test")) {
				Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.sqs.enabled")).isEqualTo("false");
				Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.dynamodb.region"))
						.isEqualTo("eu-west-2");
				Assertions.assertThat(context.getEnvironment().getProperty("cloud.aws.s3.enabled")).isEqualTo("false");

				// update parameter value
				byte[] newValues = FileCopyUtils.copyToByteArray(getClass().getClassLoader().getResourceAsStream(
						"io.awspring.cloud.autoconfigure.config.appconfig/removed_values.properties"));
				appConfigClient.createHostedConfigurationVersion(CreateHostedConfigurationVersionRequest.builder().content(SdkBytes.fromByteArray(newValues)).contentType(TEXT_TYPE).configurationProfileId(configId).applicationId(appId).latestVersionNumber(1).build());

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
				"--spring.cloud.aws.appconfig.reload.strategy=refresh",
			"--spring.cloud.aws.secretsmanager.region=" + localstack.getRegion(),
			"--spring.cloud.aws.endpoint=" + localstack.getEndpoint(),
			"--spring.cloud.aws.appconfig.endpoint="
				+ localstack.getEndpoint(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=" + localstack.getRegion(), "--logging.level.io.awspring.cloud.appconfig=debug",
				"--spring.cloud.aws.appconfig.reload.period=PT1S", "--spring.cloud.aws.s3.enabled=false");
	}


	private static void prepareEnv(byte[] content, String appName, String envName, String configName, String contentType) {
		AppConfigClient appConfigClient = AppConfigClient.builder().endpointOverride(localstack.getEndpoint()).build();
		putParameter(localstack, "test", new String(content, StandardCharsets.UTF_8));
		parameterStoreArn = SsmClient.builder().endpointOverride(localstack.getEndpoint()).build().getParameter(GetParameterRequest.builder().name("test").build()).parameter().arn();
		appId = appConfigClient.createApplication(CreateApplicationRequest.builder().name(appName).build()).id();
		envId = appConfigClient.createEnvironment(CreateEnvironmentRequest.builder().name(envName).applicationId(appId).build()).id();
		configId = appConfigClient.createConfigurationProfile(CreateConfigurationProfileRequest.builder().applicationId(appId).name(configName).locationUri(parameterStoreArn).build()).id();
		appConfigClient.createHostedConfigurationVersion(CreateHostedConfigurationVersionRequest.builder().content(SdkBytes.fromByteArray(content)).contentType(contentType).configurationProfileId(configId).applicationId(appId).latestVersionNumber(1).build());
		var depResponse = appConfigClient.createDeploymentStrategy(CreateDeploymentStrategyRequest.builder().name("myDeployment").build());
		appConfigClient.startDeployment(StartDeploymentRequest.builder().applicationId(appId).deploymentStrategyId(depResponse.id()).configurationProfileId(configId).environmentId(envId).build());
	}
	private static void putParameter(LocalStackContainer localstack, String parameterName, String parameterValue) {
		try {
			localstack.execInContainer("awslocal", "ssm", "put-parameter", "--name", parameterName, "--type", "String",
				"--value", parameterValue, "--region", localstack.getRegion(), "--overwrite");
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
