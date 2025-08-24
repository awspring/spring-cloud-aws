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

import io.awspring.cloud.sqs.annotation.SqsHandler;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sample class to demonstrate how to handle multiple message types in a single listener with {@link SqsHandler}
 * annotation.
 *
 * @author José Iêdo
 */
@Configuration
@SqsListener(queueNames = SpringSqsHandlerSample.QUEUE_NAME)
public class SpringSqsHandlerSample {

	public static final String QUEUE_NAME = "multi-method-queue";
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringSqsHandlerSample.class);

	private interface BaseMessage {
	}

	private record SampleRecord(String propertyOne, String propertyTwo) {
	}

	private record AnotherSampleRecord(String propertyOne, String propertyTwo) implements BaseMessage { }

	@SqsHandler
	void handleMessage(SampleRecord message) {
		LOGGER.info("Received message of type SampleRecord: {}", message);
	}

	@SqsHandler
	void handleMessage(BaseMessage message) {
		LOGGER.info("Received message of type BaseMessage: {}", message);
	}

	@SqsHandler(isDefault = true)
	void handleMessage(Object message) {
		LOGGER.info("Received message of type Object: {}", message);
	}

	@Bean
	public ApplicationRunner sendMessageToQueueWithMultipleHandlers(SqsTemplate sqsTemplate) {
		return args -> {
			sqsTemplate.send(QUEUE_NAME, new SampleRecord("Hello!", "From SQS!"));
			sqsTemplate.send(QUEUE_NAME, new AnotherSampleRecord("Hello!", "From SQS!"));
			sqsTemplate.send(QUEUE_NAME, "Hello!");
		};
	}
}
