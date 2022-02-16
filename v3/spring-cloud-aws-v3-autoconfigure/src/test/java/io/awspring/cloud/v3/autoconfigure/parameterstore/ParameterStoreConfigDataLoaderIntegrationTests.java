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

package io.awspring.cloud.v3.autoconfigure.parameterstore;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SSM;

@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class ParameterStoreConfigDataLoaderIntegrationTests {

	private static final String REGION = "us-east-1";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.0")).withServices(SSM).withReuse(true);

	@BeforeAll
	static void beforeAll() {
		putParameter(localstack, "/config/spring/message", "value from tests", REGION);
		putParameter(localstack, "/config/spring/another-parameter", "another parameter value", REGION);
	}

	@Test
	void resolvesPropertyFromParameterStore() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.import=aws-parameterstore:/config/spring/",
				"--spring.cloud.aws.parameterstore.region=" + REGION,
				"--spring.cloud.aws.parameterstore.endpoint=" + localstack.getEndpointOverride(SSM).toString())) {
			assertThat(context.getEnvironment().getProperty("message")).isEqualTo("value from tests");
			assertThat(context.getEnvironment().getProperty("another-parameter")).isEqualTo("another parameter value");
			assertThat(context.getEnvironment().getProperty("non-existing-parameter")).isNull();
		}
	}

	@Test
	void whenKeysAreNotSpecifiedFailsWithHumanReadableFailureMessage(CapturedOutput output) {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run("--spring.config.import=aws-parameterstore:",
				"--spring.cloud.aws.parameterstore.region=" + REGION,
				"--spring.cloud.aws.parameterstore.endpoint=" + localstack.getEndpointOverride(SSM).toString())) {
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

}
