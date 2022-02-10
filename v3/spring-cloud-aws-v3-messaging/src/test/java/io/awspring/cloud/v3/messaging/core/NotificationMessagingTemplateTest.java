/*
 * Copyright 2013-2019 the original author or authors.
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

package io.awspring.cloud.v3.messaging.core;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.Topic;

import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alain Sahli
 */
class NotificationMessagingTemplateTest {

	@Test
	void send_validTextMessage_usesTopicChannel() throws Exception {
		// Arrange
		SnsClient amazonSns = mock(SnsClient.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(amazonSns);
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:test";
		when(amazonSns.listTopics(ListTopicsRequest.builder().build()))
				.thenReturn(ListTopicsResponse.builder()
					.topics(Topic.builder()
						.topicArn(physicalTopicName)
						.build())
					.build());
		notificationMessagingTemplate.setDefaultDestinationName(physicalTopicName);

		// Act
		notificationMessagingTemplate.send(MessageBuilder.withPayload("Message content").build());

		// Assert
		verify(amazonSns).publish(PublishRequest.builder()
			.topicArn(physicalTopicName)
			.message("Message content")
			.messageAttributes(isNotNull())
			.build());
	}

	@Test
	void send_validTextMessageWithCustomDestinationResolver_usesTopicChannel() throws Exception {
		// Arrange
		SnsClient amazonSns = mock(SnsClient.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(amazonSns,
				(DestinationResolver<String>) name -> name.toUpperCase(Locale.ENGLISH), null);

		// Act
		notificationMessagingTemplate.send("test", MessageBuilder.withPayload("Message content").build());

		// Assert
		verify(amazonSns)
				.publish(PublishRequest.builder()
					.topicArn("TEST")
					.message("Message content")
					.messageAttributes(isNotNull())
					.build());
	}

	@Test
	void convertAndSend_withDestinationPayloadAndSubject_shouldSetSubject() throws Exception {
		// Arrange
		SnsClient amazonSns = mock(SnsClient.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(amazonSns);
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:test";
		when(amazonSns.listTopics(ListTopicsRequest.builder().build()))
				.thenReturn(ListTopicsResponse.builder()
					.topics(Topic.builder()
						.topicArn(physicalTopicName)
						.build())
					.build());

		// Act
		notificationMessagingTemplate.sendNotification(physicalTopicName, "My message", "My subject");

		// Assert
		verify(amazonSns).publish(PublishRequest.builder()
				.topicArn(physicalTopicName)
				.message("My message")
				.subject("My subject")
				.messageAttributes(isNotNull())
			.build());
	}

	@Test
	void convertAndSend_withPayloadAndSubject_shouldSetSubject() throws Exception {
		// Arrange
		SnsClient amazonSns = mock(SnsClient.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(amazonSns);
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:test";
		when(amazonSns.listTopics(ListTopicsRequest.builder().build()))
				.thenReturn(ListTopicsResponse.builder()
					.topics(Topic.builder()
						.topicArn(physicalTopicName)
						.build())
					.build());
		notificationMessagingTemplate.setDefaultDestinationName(physicalTopicName);

		// Act
		notificationMessagingTemplate.sendNotification("My message", "My subject");

		// Assert
		verify(amazonSns).publish(PublishRequest.builder()
			.topicArn(physicalTopicName)
			.message("My message")
			.subject("My subject")
			.messageAttributes(isNotNull())
			.build());
	}

}
