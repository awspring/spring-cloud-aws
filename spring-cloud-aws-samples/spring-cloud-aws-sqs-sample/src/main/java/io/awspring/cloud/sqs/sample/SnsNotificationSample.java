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
import io.awspring.cloud.sqs.support.converter.SnsNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	 * Receives SNS notifications from the "sns-notification-queue" SQS queue. The message payload is automatically
	 * converted to a String.
	 *
	 * @param notification the SNS notification wrapper containing the message and metadata
	 */
	@SqsListener("sns-notification-queue")
	public void receiveStringMessage(SnsNotification<String> notification) {
		LOGGER.info("Received SNS notification with ID: {}", notification.getMessageId());
		LOGGER.info("From topic: {}", notification.getTopicArn());
		notification.getSubject().ifPresent(subject -> LOGGER.info("Subject: {}", subject));
		LOGGER.info("Message: {}", notification.getMessage());
		LOGGER.info("Timestamp: {}", notification.getTimestamp());
		LOGGER.info("Message attributes: {}", notification.getMessageAttributes());
	}

	/**
	 * Receives SNS notifications from the "sns-notification-queue" SQS queue. The message payload is automatically
	 * converted to a CustomMessage object.
	 *
	 * @param notification the SNS notification wrapper containing the message and metadata
	 */
	@SqsListener("sns-notification-custom-queue")
	public void receiveCustomMessage(SnsNotification<CustomMessage> notification) {
		LOGGER.info("Received SNS notification with ID: {}", notification.getMessageId());
		LOGGER.info("From topic: {}", notification.getTopicArn());
		notification.getSubject().ifPresent(subject -> LOGGER.info("Subject: {}", subject));

		CustomMessage message = notification.getMessage();
		LOGGER.info("Message content: {}", message.getContent());
		LOGGER.info("Message timestamp: {}", message.getTimestamp());

		LOGGER.info("Notification timestamp: {}", notification.getTimestamp());
		LOGGER.info("Message attributes: {}", notification.getMessageAttributes());
	}

	/**
	 * A custom message class for demonstration purposes.
	 */
	public static class CustomMessage {
		private String content;
		private long timestamp;

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}
	}
}
