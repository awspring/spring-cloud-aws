/*
 * Copyright 2013-2021 the original author or authors.
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

import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.operations.TemplateAcknowledgementMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.util.Collection;
import java.util.UUID;

@Configuration
public class SqsManualContainerInstantiationSample {


	public static final String NEW_USER_QUEUE = "new-user-queue";

	private static final Logger LOGGER = LoggerFactory.getLogger(SqsManualContainerInstantiationSample.class);

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
			.configure(options -> options.acknowledgementMode(TemplateAcknowledgementMode.MANUAL))
			.build();
	}

	@Bean
	SqsMessageListenerContainer<User> sqsMessageListenerContainer(SqsAsyncClient sqsAsyncClient) {
		SqsMessageListenerContainer<User> container = new SqsMessageListenerContainer<>(sqsAsyncClient);
		container.setMessageListener((message) -> {
			LOGGER.info("Received message {}", message);
			Acknowledgement.acknowledge(message);
		});
		container.setQueueNames(NEW_USER_QUEUE);
		container.setAcknowledgementResultCallback(new AckResultCallback());
		container.configure(sqsContainerOptionsBuilder ->
			sqsContainerOptionsBuilder
				.acknowledgementMode(AcknowledgementMode.MANUAL));
		return container;
	}

	static class AckResultCallback implements AcknowledgementResultCallback<User> {
		@Override
		public void onSuccess(Collection<Message<User>> messages) {
			LOGGER.info("Ack with success");
		}

		@Override
		public void onFailure(Collection<Message<User>> messages, Throwable t) {
			LOGGER.error("Ack with fail", t);
		}
	}

	public record User(UUID id, String name) {
	}
}
