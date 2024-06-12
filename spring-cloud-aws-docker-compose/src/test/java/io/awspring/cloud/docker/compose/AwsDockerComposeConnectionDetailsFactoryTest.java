package io.awspring.cloud.docker.compose;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

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
