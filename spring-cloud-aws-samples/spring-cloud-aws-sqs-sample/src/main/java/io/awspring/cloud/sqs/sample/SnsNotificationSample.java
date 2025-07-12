/*
 * Copyright 2013-2025 the original author or authors.
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
import io.awspring.cloud.sqs.support.converter.SnsNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Sample demonstrating how to receive SNS notifications from SQS.
 *
 * @author Damien Chomat
 */
@Component
public class SnsNotificationSample {

	private static final Logger LOGGER = LoggerFactory.getLogger(SnsNotificationSample.class);

	/**
	 * Receives SNS notifications from the "sns-notification-custom-queue" SQS queue. The message payload is
	 * automatically converted to a CustomMessage object.
	 *
	 * @param notification the SNS notification wrapper containing the message and metadata
	 */
	@SqsListener("sns-notification-custom-queue")
	public void receiveCustomMessage(SnsNotification<CustomMessage> notification) {
		LOGGER.info("Received SNS notification with ID: {}", notification.getMessageId());
		LOGGER.info("From topic: {}", notification.getTopicArn());
		notification.getSubject().ifPresent(subject -> LOGGER.info("Subject: {}", subject));

		CustomMessage message = notification.getMessage();
		LOGGER.info("Message content: {}", message.content());
		LOGGER.info("Message timestamp: {}", message.timestamp());

		LOGGER.info("Notification timestamp: {}", notification.getTimestamp());
		LOGGER.info("Message attributes: {}", notification.getMessageAttributes());
	}

	/**
	 * ApplicationRunner to send sample SNS messages to the queues for demonstration purposes. This simulates SNS
	 * notifications being sent to SQS queues that are subscribed to SNS topics.
	 */
	@Bean
	public ApplicationRunner sendSnsNotificationMessage(SqsTemplate sqsTemplate) {
		return args -> {
			// Simulate an SNS notification for an order processing topic
			String orderNotificationMessage = """
					{
						"Type": "Notification",
						"MessageId": "order-12345-notification",
						"TopicArn": "arn:aws:sns:us-east-1:123456789012:order-processing-topic",
						"Subject": "Order Processing Update",
						"Message": "{\\"content\\": \\"Order #12345 has been processed successfully\\", \\"timestamp\\": 1672531200000}",
						"Timestamp": "2023-01-01T12:00:00Z",
						"MessageAttributes": {
							"eventType": {
								"Type": "String",
								"Value": "ORDER_PROCESSED"
							},
							"priority": {
								"Type": "String",
								"Value": "high"
							}
						}
					}
					""";

			LOGGER.info("Sending SNS notification messages to subscribed SQS queue...");
			sqsTemplate.send("sns-notification-custom-queue", orderNotificationMessage);
		};
	}

	/**
	 * A custom message record for demonstration purposes.
	 */
	public record CustomMessage(String content, long timestamp) {
	}
}
