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

package io.awspring.cloud.messaging.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.Topic;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
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
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(amazonSns);
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:test";
		when(amazonSns.listTopics(new ListTopicsRequest(null)))
				.thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(physicalTopicName)));
		notificationMessagingTemplate.setDefaultDestinationName(physicalTopicName);

		// Act
		notificationMessagingTemplate.send(MessageBuilder.withPayload("Message content").build());

		// Assert
		verify(amazonSns).publish(
				new PublishRequest(physicalTopicName, "Message content", null).withMessageAttributes(isNotNull()));
	}

	@Test
	void send_validTextMessageWithCustomDestinationResolver_usesTopicChannel() throws Exception {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(amazonSns,
				(DestinationResolver<String>) name -> name.toUpperCase(Locale.ENGLISH), null);

		// Act
		notificationMessagingTemplate.send("test", MessageBuilder.withPayload("Message content").build());

		// Assert
		verify(amazonSns)
				.publish(new PublishRequest("TEST", "Message content", null).withMessageAttributes(isNotNull()));
	}

	@Test
	void convertAndSend_withDestinationPayloadAndSubject_shouldSetSubject() throws Exception {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(amazonSns);
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:test";
		when(amazonSns.listTopics(new ListTopicsRequest(null)))
				.thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(physicalTopicName)));

		// Act
		notificationMessagingTemplate.sendNotification(physicalTopicName, "My message", "My subject");

		// Assert
		verify(amazonSns).publish(
				new PublishRequest(physicalTopicName, "My message", "My subject").withMessageAttributes(isNotNull()));
	}

	@Test
	void convertAndSend_withPayloadAndSubject_shouldSetSubject() throws Exception {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(amazonSns);
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:test";
		when(amazonSns.listTopics(new ListTopicsRequest(null)))
				.thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(physicalTopicName)));
		notificationMessagingTemplate.setDefaultDestinationName(physicalTopicName);

		// Act
		notificationMessagingTemplate.sendNotification("My message", "My subject");

		// Assert
		verify(amazonSns).publish(
				new PublishRequest(physicalTopicName, "My message", "My subject").withMessageAttributes(isNotNull()));
	}

	@Test
	void convertAndSend_withPayloadAndMessageGroupIdHeader_shouldSetMessageGroupIdParameter() throws Exception {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(amazonSns);
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:test";
		when(amazonSns.listTopics(new ListTopicsRequest(null)))
				.thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(physicalTopicName)));
		ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
		when(amazonSns.publish(publishRequestArgumentCaptor.capture())).thenReturn(new PublishResult());

		// Act
		Map<String, Object> headers = new HashMap<>();
		headers.put(TopicMessageChannel.MESSAGE_GROUP_ID_HEADER, "id-5");
		notificationMessagingTemplate.convertAndSend(physicalTopicName, "My message", headers);

		// Assert
		PublishRequest publishRequest = publishRequestArgumentCaptor.getValue();
		assertThat(publishRequest.getMessage()).isEqualTo("My message");
		assertThat(publishRequest.getMessageGroupId()).isEqualTo("id-5");
	}

}
