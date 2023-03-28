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
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.operations.TemplateAcknowledgementMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import java.util.UUID;

@SpringBootApplication
public class SpringSqsListenMultipleQueues {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringSqsListenMultipleQueues.class);

	private static final String ORDER_QUEUE = "order-queue";
	private static final String WITHDRAWAL_QUEUE = "withdrawal-queue";

	public static void main(String[] args) {
		SpringApplication.run(SpringSqsListenMultipleQueues.class, args);
	}

	// application.properties
	// spring.cloud.aws.credentials.access-key=${AWS_ACCESS_KEY}
	// spring.cloud.aws.credentials.secret-key==${AWS_SECRET_KEY}
	@Bean
	public SqsTemplate sqsTemplate(AwsCredentialsProvider credentialsProvider) {
		SqsAsyncClient client = SqsAsyncClient.builder()
			.region(Region.US_EAST_1)
			.credentialsProvider(credentialsProvider)
			.build();

		return SqsTemplate.builder()
			.sqsAsyncClient(client)
			.configure(options -> options
				.acknowledgementMode(TemplateAcknowledgementMode.ACKNOWLEDGE))
			.build();
	}

	@SqsListener(queueNames = {ORDER_QUEUE, WITHDRAWAL_QUEUE})
	void listen(Message message) {
		LOGGER.info("Received message {}", message);
	}

	@Bean
	public ApplicationRunner sendMessageToQueues(SqsTemplate sqsTemplate) {
		return args -> {
			sqsTemplate.sendAsync(ORDER_QUEUE, new OrderMessage(
				UUID.randomUUID().toString(),
				"john@awsspringcloud.com"
			));

			sqsTemplate.sendAsync(WITHDRAWAL_QUEUE, new WithdrawalMessage(
				UUID.randomUUID().toString(),
				"Mary",
				"mary@awsspringcloud.com"
			));
		};
	}

	private record WithdrawalMessage(String transaction, String customerName, String customerEmail) {
	}

	private record OrderMessage(String orderId, String customerEmail) {
	}
}
