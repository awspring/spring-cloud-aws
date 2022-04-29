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
package io.awspring.cloud.autoconfigure.sns.it;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;

import io.awspring.cloud.sns.core.SnsTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;

/**
 * Integration tests for {@link SnsTemplate}.
 *
 * @author Matej Nedic
 * @since 3.0
 */
@Testcontainers
class SnsTemplateIntegrationTest {

	private static final String REGION = "eu-west-1";

	private static final String TOPIC_NAME = "my_topic_name";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.2")).withServices(SNS).withReuse(true);

	@Test
	void send_validTextMessage_usesTopicChannel_auto_create() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application)) {
			SnsTemplate snsTemplate = context.getBean(SnsTemplate.class);

			assertThatCode(() -> snsTemplate.convertAndSend(TOPIC_NAME, "message")).doesNotThrowAnyException();
		}
	}

	@Test
	void send_validTextMessage_usesTopicChannel_send_arn() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application)) {
			SnsTemplate snsTemplate = context.getBean(SnsTemplate.class);
			SnsClient client = context.getBean(SnsClient.class);
			String topic_arn = client.createTopic(CreateTopicRequest.builder().name(TOPIC_NAME).build()).topicArn();

			assertThatCode(() -> snsTemplate.convertAndSend(topic_arn, "message")).doesNotThrowAnyException();
		}
	}

	private ConfigurableApplicationContext runApplication(SpringApplication application) {
		return application.run("--spring.cloud.aws.sns.region=" + REGION,
				"--spring.cloud.aws.sns.endpoint=" + localstack.getEndpointOverride(SNS).toString(),
				"--spring.cloud.aws.credentials.access-key=noop", "--spring.cloud.aws.credentials.secret-key=noop",
				"--spring.cloud.aws.region.static=eu-west-1");
	}

	@SpringBootApplication
	static class App {

	}

}
