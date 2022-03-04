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

package io.awspring.cloud.autoconfigure.paramstore;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SSM;

/**
 * Integration tests for loading configuration properties as List from AWS Parameter
 * Store.
 *
 * @author Maciej Walkowiak
 * @author Matej Nedić
 */
@Testcontainers
class ParameterStoreConfigDataLoaderIntegrationTests {

	private static final String REGION = "us-east-1";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.0")).withServices(SSM).withReuse(true);

	@Test
	void followsNextToken() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		putParameter(localstack, "/config/myservice/key1", "value1", REGION);
		putParameter(localstack, "/config/myservice/key2", "value2", REGION);
		putParameter(localstack, "/config/myservice/key3", "value3", REGION);
		putParameter(localstack, "/config/myservice/key4", "value4", REGION);

		try (ConfigurableApplicationContext context = runApplication(application,
				"aws-parameterstore:/config/myservice/")) {
			assertThat(context.getEnvironment().getProperty("key1")).isEqualTo("value1");
			assertThat(context.getEnvironment().getProperty("key2")).isEqualTo("value2");
			assertThat(context.getEnvironment().getProperty("key3")).isEqualTo("value3");
			assertThat(context.getEnvironment().getProperty("key4")).isEqualTo("value4");
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

	private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport) {
		return application.run("--spring.config.import=" + springConfigImport,
				"--aws.paramstore.endpoint=" + localstack.getEndpointOverride(SSM).toString(),
				"--cloud.aws.credentials.accessKey=noop", "--cloud.aws.credentials.secretKey=noop");
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
