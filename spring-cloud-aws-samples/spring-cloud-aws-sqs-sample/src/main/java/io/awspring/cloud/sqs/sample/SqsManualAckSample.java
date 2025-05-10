/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sqs.sample;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsManualAckSample {

	public static final String NEW_USER_QUEUE = "new-user-queue";

	private static final Logger LOGGER = LoggerFactory.getLogger(SqsManualAckSample.class);

	@Bean
	public ApplicationRunner sendMessageToManualAckQueue(SqsTemplate sqsTemplate) {
		LOGGER.info("Sending message");
		return args -> sqsTemplate.send(to -> to.queue(NEW_USER_QUEUE).payload(new User(UUID.randomUUID(), "John")));
	}

	@Bean
	public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
		return SqsTemplate.builder().sqsAsyncClient(sqsAsyncClient).build();
	}

	@SqsListener(NEW_USER_QUEUE)
	public void listen(Message<User> message) {
		LOGGER.info("Message received on listen method at {}", OffsetDateTime.now());
		Acknowledgement.acknowledge(message);
	}

	@Bean
	SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
		return SqsMessageListenerContainerFactory.builder()
				.configure(options -> options.acknowledgementMode(AcknowledgementMode.MANUAL)
						.acknowledgementInterval(Duration.ofSeconds(3)) // NOTE: With acknowledgementInterval 3 seconds,
																		// we can batch and ack async.
						.acknowledgementThreshold(0))
				.acknowledgementResultCallback(new AckResultCallback()).sqsAsyncClient(sqsAsyncClient).build();
	}

	public record User(UUID id, String name) {
	}

	static class AckResultCallback implements AcknowledgementResultCallback<Object> {
		@Override
		public void onSuccess(Collection<Message<Object>> messages) {
			LOGGER.info("Ack with success at {}", OffsetDateTime.now());
		}

		@Override
		public void onFailure(Collection<Message<Object>> messages, Throwable t) {
			LOGGER.error("Ack with fail", t);
		}
	}
}
