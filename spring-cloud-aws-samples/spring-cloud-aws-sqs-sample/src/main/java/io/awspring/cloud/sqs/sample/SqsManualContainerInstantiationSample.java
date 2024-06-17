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
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsManualContainerInstantiationSample {

	public static final String NEW_USER_QUEUE = "new-user-queue";

	private static final Logger LOGGER = LoggerFactory.getLogger(SqsManualContainerInstantiationSample.class);

	@Bean
	public ApplicationRunner sendMessageToQueueManualContainerInstantiation(SqsTemplate sqsTemplate) {
		LOGGER.info("Sending message");
		return args -> sqsTemplate.send(to -> to.queue(NEW_USER_QUEUE).payload(new User(UUID.randomUUID(), "John")));
	}

	@Bean
	public SqsTemplate sqsTemplateManualContainerInstantiation(SqsAsyncClient sqsAsyncClient) {
		return SqsTemplate.builder().sqsAsyncClient(sqsAsyncClient).build();
	}

	@Bean
	SqsMessageListenerContainer<User> sqsMessageListenerContainer(SqsAsyncClient sqsAsyncClient) {
		return new SqsMessageListenerContainer.Builder<User>().sqsAsyncClient(sqsAsyncClient).queueNames(NEW_USER_QUEUE)
				.messageListener((message) -> {
					LOGGER.info("Received message {}", message);
				}).build();
	}

	public record User(UUID id, String name) {
	}
}
