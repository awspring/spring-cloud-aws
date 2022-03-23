package io.awspring.cloud.autoconfigure.sns.configuration;

import io.awspring.cloud.sns.core.NotificationMessagingTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;

@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
public class NotificationMessageTemplateTest {

	private static String TOPIC_ARN;

	private static final String REGION = "eu-west-1";

	private static final String TOPIC_NAME = "my_topic_name";

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.0")).withServices(SNS).withReuse(true);

	@BeforeAll
	static void beforeAll() {
		SnsClient client = SnsClient.builder().endpointOverride(localstack.getEndpointOverride(SNS))
				.region(Region.of(REGION)).build();
		TOPIC_ARN = client.createTopic(CreateTopicRequest.builder().name(TOPIC_NAME).build()).topicArn();
	}

	@Test
	void send_validTextMessage_usesTopicChannel() {
		SpringApplication application = new SpringApplication(App.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = runApplication(application)) {
			NotificationMessagingTemplate notificationMessagingTemplate = context
					.getBean(NotificationMessagingTemplate.class);
			assertThatCode(() -> notificationMessagingTemplate.convertAndSend(TOPIC_NAME, "message"))
					.doesNotThrowAnyException();
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
