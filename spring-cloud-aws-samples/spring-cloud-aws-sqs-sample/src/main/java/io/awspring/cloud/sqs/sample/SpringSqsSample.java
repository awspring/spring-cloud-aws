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

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@SpringBootApplication
public class SpringSqsSample {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringSqsSample.class);

	private static final String QUEUE_NAME = "test-queue";

	public static void main(String[] args) {
		SpringApplication.run(SpringSqsSample.class, args);
	}

	@SqsListener(queueNames = QUEUE_NAME)
	void listen(String message) {
		LOGGER.info("Received message {}", message);
	}

	@Bean
	public ApplicationRunner sendMessageToQueue(SqsAsyncClient client) {
		return args -> client.sendMessage(
				SendMessageRequest.builder().queueUrl(getQueueUrl(client)).messageBody("Hello from SQS!").build())
				.join();
	}

	private String getQueueUrl(SqsAsyncClient client) {
		return client.getQueueUrl(GetQueueUrlRequest.builder().queueName(QUEUE_NAME).build()).join().queueUrl();
	}

}
