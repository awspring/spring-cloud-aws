/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.docker.compose;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

class AwsDockerComposeConnectionDetailsFactoryTest {

	private final Resource dockerComposeResource = new ClassPathResource("docker-compose.yaml");

	@AfterAll
	static void shutDown() {
		var shutdownHandlers = SpringApplication.getShutdownHandlers();
		((Runnable) shutdownHandlers).run();
	}

	@Test
	void runCreatesConnectionDetailsThatCanAccessLocalStack() throws IOException {
		var application = new SpringApplication(Config.class);
		var properties = new LinkedHashMap<String, Object>();
		properties.put("spring.docker.compose.skip.in-tests", "false");
		properties.put("spring.docker.compose.file", dockerComposeResource.getFile());
		properties.put("spring.docker.compose.stop.command", "down");
		application.setDefaultProperties(properties);
		var connectionDetails = application.run().getBean(AwsConnectionDetails.class);

		assertThat(connectionDetails.getAccessKey()).isEqualTo("noop");
		assertThat(connectionDetails.getSecretKey()).isEqualTo("noop");
		assertThat(connectionDetails.getRegion()).isEqualTo("eu-central-1");
		assertThat(connectionDetails.getEndpoint()).satisfiesAnyOf(
				endpoint -> assertThat(endpoint).isEqualTo(URI.create("http://localhost:4566")),
				endpoint -> assertThat(endpoint).isEqualTo(URI.create("http://127.0.0.1:4566")));
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}
}
