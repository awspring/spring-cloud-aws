package io.awspring.cloud.sns.core;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

/**
 * @author Alain Sahli
 */
class NotificationMessagingTemplateTest {

	@Test
	void send_validTextMessage_usesTopicChannel() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(snsClient);
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
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(snsClient,
				(DestinationResolver<String>) name -> name.toUpperCase(Locale.ENGLISH), null);

		// Act
		notificationMessagingTemplate.send("test", MessageBuilder.withPayload("Message content").build());

		// Assert
		verify(snsClient).publish(PublishRequest.builder().topicArn("TEST").message("Message content").subject(null)
				.messageAttributes(isNotNull()).build());
	}

	@Test
	void convertAndSend_withDestinationPayloadAndSubject_shouldSetSubject() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(snsClient);
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
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(snsClient);
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
		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(snsClient);
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:test";
		when(snsClient.listTopics(ListTopicsRequest.builder().build())).thenReturn(
				ListTopicsResponse.builder().topics(Topic.builder().topicArn(physicalTopicName).build()).build());
		ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
		when(snsClient.publish(publishRequestArgumentCaptor.capture())).thenReturn(PublishResponse.builder().build());

		// Act
		Map<String, Object> headers = new HashMap<>();
		headers.put(TopicMessageChannel.MESSAGE_GROUP_ID_HEADER, "id-5");
		notificationMessagingTemplate.convertAndSend(physicalTopicName, "My message", headers);

		// Assert
		PublishRequest publishRequest = publishRequestArgumentCaptor.getValue();
		assertThat(publishRequest.message()).isEqualTo("My message");
		assertThat(publishRequest.messageGroupId()).isEqualTo("id-5");
	}

}
