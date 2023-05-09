package io.awspring.cloud.sqs.sample;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.time.Duration;
import java.util.UUID;

@Configuration
public class SqsManualAckSample {

	public static final String NEW_USER_QUEUE = "new-user-queue";

	private static final Logger LOGGER = LoggerFactory.getLogger(SqsManualAckSample.class);

	@Bean
	public ApplicationRunner sendMessageToQueue(SqsTemplate sqsTemplate) {
		LOGGER.info("Sending message");
		return args -> sqsTemplate.send(to -> to.queue(NEW_USER_QUEUE)
			.payload(new User(UUID.randomUUID(), "John"))
		);
	}

	@Bean
	public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
		return SqsTemplate.builder()
			.sqsAsyncClient(sqsAsyncClient)
			.build();
	}

	@SqsListener(NEW_USER_QUEUE)
	public void listen(Message<User> message) {
		Acknowledgement.acknowledge(message);
		LOGGER.info("Message {} acknowledged", message.getPayload());
	}

	@Bean
	SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
		return SqsMessageListenerContainerFactory
			.builder()
			.configure(options -> options
				.acknowledgementMode(AcknowledgementMode.MANUAL)
				.acknowledgementInterval(Duration.ZERO) // Set to Duration.ZERO along with
				.acknowledgementThreshold(0)            // acknowledgementThreshold to zero to enable immediate acknowledgement.
			)
			.sqsAsyncClient(sqsAsyncClient)
			.build();
	}

	public record User(UUID id, String name) {
	}
}
