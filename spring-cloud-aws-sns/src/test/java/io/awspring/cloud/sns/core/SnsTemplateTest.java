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

import static io.awspring.cloud.sns.Matchers.requestMatches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sns.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

/**
 * Tests for {@link SnsTemplate}.
 *
 * @author Alain Sahli
 * @author Mariusz Sondecki
 */
class SnsTemplateTest {
	private static final String TOPIC_ARN = "arn:aws:sns:eu-west:123456789012:test";

	private final SnsClient snsClient = mock(SnsClient.class);

	private SnsTemplate snsTemplate;

	@BeforeEach
	void init() {
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setSerializedPayloadClass(String.class);
		snsTemplate = new SnsTemplate(snsClient, new DefaultTopicArnResolver(snsClient), converter);

		when(snsClient.createTopic(CreateTopicRequest.builder().name("topic name").build()))
				.thenReturn(CreateTopicResponse.builder().topicArn(TOPIC_ARN).build());
	}

	@Test
	void sendsTextMessage() {
		snsTemplate.sendNotification("topic name", "message content", "subject");

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.topicArn()).isEqualTo(TOPIC_ARN);
			assertThat(r.message()).isEqualTo("message content");
			assertThat(r.subject()).isEqualTo("subject");
			assertThat(r.messageAttributes().keySet()).contains(MessageHeaders.ID);
			assertThat(r.messageAttributes().keySet()).contains(MessageHeaders.TIMESTAMP);
		}));
	}

	@Test
	void sendsTestMessageCacheHit() {
		CachingTopicArnResolver cachingTopicArnResolver = new CachingTopicArnResolver(
				new DefaultTopicArnResolver(snsClient));
		SnsTemplate snsTemplateTestCache = new SnsTemplate(snsClient, cachingTopicArnResolver, null);
		snsTemplateTestCache.sendNotification("topic name", "message content", "subject");
		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.topicArn()).isEqualTo(TOPIC_ARN);
			assertThat(r.message()).isEqualTo("message content");
			assertThat(r.subject()).isEqualTo("subject");
			assertThat(r.messageAttributes().keySet()).contains(MessageHeaders.ID);
			assertThat(r.messageAttributes().keySet()).contains(MessageHeaders.TIMESTAMP);
		}));
		assertThat(cachingTopicArnResolver.cacheSize()).isEqualTo(1);

		snsTemplateTestCache.sendNotification("topic name", "message content", "subject");
		assertThat(cachingTopicArnResolver.cacheSize()).isEqualTo(1);
	}

	@Test
	void sendsJsonMessage() {
		snsTemplate.sendNotification("topic name", new Person("John Doe"), "subject");

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.topicArn()).isEqualTo(TOPIC_ARN);
			assertThat(r.message()).isEqualTo("{\"name\":\"John Doe\"}");
			assertThat(r.subject()).isEqualTo("subject");
			assertThat(r.messageAttributes().keySet()).contains(MessageHeaders.ID);
			assertThat(r.messageAttributes().keySet()).contains(MessageHeaders.TIMESTAMP);
		}));
	}

	@Test
	void sendsMessageToDefaultDestination() {
		snsTemplate.setDefaultDestinationName("topic name");
		snsTemplate.sendNotification("message content", "subject");

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.topicArn()).isEqualTo(TOPIC_ARN);
		}));
	}

	@Test
	void sendsComplexSnsNotification() {
		snsTemplate.sendNotification("topic name", SnsNotification.builder(new Person("foo")).groupId("groupId")
				.deduplicationId("deduplicationId").header("header-1", "value-1").subject("subject").build());

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.topicArn()).isEqualTo(TOPIC_ARN);
			assertThat(r.message()).isEqualTo("{\"name\":\"foo\"}");
			assertThat(r.subject()).isEqualTo("subject");
			assertThat(r.messageGroupId()).isEqualTo("groupId");
			assertThat(r.messageDeduplicationId()).isEqualTo("deduplicationId");
			assertThat(r.messageAttributes()).containsEntry("header-1",
					MessageAttributeValue.builder().stringValue("value-1").dataType("String").build());
		}));
	}

	@Test
	void sendsSimpleSnsNotification() {
		snsTemplate.sendNotification("topic name", SnsNotification.of(new Person("bar")));

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.topicArn()).isEqualTo(TOPIC_ARN);
			assertThat(r.message()).isEqualTo("{\"name\":\"bar\"}");
			assertThat(r.subject()).isNull();
			assertThat(r.messageGroupId()).isNull();
			assertThat(r.messageDeduplicationId()).isNull();
		}));
	}

	@Test
	void sendsMessageProcessedByInterceptor() {
		// given
		ChannelInterceptor interceptor = mock(ChannelInterceptor.class);
		String originalMessage = "message content";
		String processedMessage = originalMessage + " modified by interceptor";
		snsTemplate.addChannelInterceptor(interceptor);
		when(interceptor.preSend(any(Message.class), any(MessageChannel.class))).thenAnswer(invocation -> {
			Object[] args = invocation.getArguments();
			Message message = (Message) args[0];
			return new GenericMessage<>(processedMessage, message.getHeaders());
		});

		// when
		snsTemplate.sendNotification("topic name", originalMessage, "subject");

		// then
		verify(snsClient).publish(requestMatches(r -> assertThat(r.message()).isEqualTo(processedMessage)));
		verify(interceptor).preSend(any(), any());
		verify(interceptor).postSend(any(), any(), anyBoolean());
	}
}
