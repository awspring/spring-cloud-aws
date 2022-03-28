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

package io.awspring.cloud.sns.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.Topic;

import org.springframework.messaging.support.MessageBuilder;

import static io.awspring.cloud.sns.core.MessageHeaderCodes.MESSAGE_GROUP_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
		SnsClient snsClient = mock(SnsClient.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(snsClient,
				false, new ObjectMapper());
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:test";
		when(snsClient.listTopics(ListTopicsRequest.builder().build())).thenReturn(
				ListTopicsResponse.builder().topics(Topic.builder().topicArn(physicalTopicName).build()).build());
		notificationMessagingTemplate.setDefaultDestinationName(physicalTopicName);

		// Act
		notificationMessagingTemplate.send(MessageBuilder.withPayload("Message content").build());

		// Assert
		verify(snsClient).publish(PublishRequest.builder().message("Message content").topicArn(physicalTopicName)
				.messageAttributes(isNotNull()).build());
	}

	@Test
	void send_validTextMessageWithCustomDestinationResolver_usesTopicChannel() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(snsClient, true,
				new ObjectMapper());
		when(snsClient.createTopic(any(Consumer.class)))
				.thenReturn(CreateTopicResponse.builder().topicArn("arn:aws:sns:eu-west:123456789012:test").build());

		// Act
		notificationMessagingTemplate.send("test", MessageBuilder.withPayload("Message content").build());

		// Assert
		verify(snsClient).publish(PublishRequest.builder().topicArn("TEST").message("Message content").subject(null)
				.messageAttributes(isNotNull()).build());
	}

	@Test
	void send_validTextMessageWithCustomDestinationResolver_failsForAutoCreateFalse() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(snsClient,
				false, new ObjectMapper());
		// Act
		Assertions.assertThrows(IllegalArgumentException.class, () -> notificationMessagingTemplate.send("test",
				MessageBuilder.withPayload("Message content").build()));
	}

	@Test
	void convertAndSend_withDestinationPayloadAndSubject_shouldSetSubject() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(snsClient,
				false, new ObjectMapper());
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:test";
		when(snsClient.listTopics(ListTopicsRequest.builder().build())).thenReturn(
				ListTopicsResponse.builder().topics(Topic.builder().topicArn(physicalTopicName).build()).build());

		// Act
		notificationMessagingTemplate.sendNotification(physicalTopicName, "My message", "My subject");

		// Assert
		verify(snsClient).publish(PublishRequest.builder().topicArn(physicalTopicName).message("My message")
				.subject("My subject").messageAttributes(isNotNull()).build());
	}

	@Test
	void convertAndSend_withPayloadAndSubject_shouldSetSubject() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(snsClient,
				false, new ObjectMapper());
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:test";
		when(snsClient.listTopics(ListTopicsRequest.builder().build())).thenReturn(
				ListTopicsResponse.builder().topics(Topic.builder().topicArn(physicalTopicName).build()).build());
		notificationMessagingTemplate.setDefaultDestinationName(physicalTopicName);

		// Act
		notificationMessagingTemplate.sendNotification("My message", "My subject");

		// Assert
		verify(snsClient).publish(PublishRequest.builder().topicArn(physicalTopicName).message("My message")
				.subject("My subject").messageAttributes(isNotNull()).build());
	}

	@Test
	void convertAndSend_withPayloadAndMessageGroupIdHeader_shouldSetMessageGroupIdParameter() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(snsClient,
				false, new ObjectMapper());
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:test";
		when(snsClient.listTopics(ListTopicsRequest.builder().build())).thenReturn(
				ListTopicsResponse.builder().topics(Topic.builder().topicArn(physicalTopicName).build()).build());
		ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
		when(snsClient.publish(publishRequestArgumentCaptor.capture())).thenReturn(PublishResponse.builder().build());

		// Act
		Map<String, Object> headers = new HashMap<>();
		headers.put(MESSAGE_GROUP_ID_HEADER, "id-5");
		notificationMessagingTemplate.convertAndSend(physicalTopicName, "My message", headers);

		// Assert
		PublishRequest publishRequest = publishRequestArgumentCaptor.getValue();
		assertThat(publishRequest.message()).isEqualTo("My message");
		assertThat(publishRequest.messageGroupId()).isEqualTo("id-5");
	}

}
