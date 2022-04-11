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

import static io.awspring.cloud.sns.core.Matchers.requestMatches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;

/**
 * @author Alain Sahli
 */
class SnsTemplateTest {

	private final SnsClient snsClient = mock(SnsClient.class);

	private SnsTemplate snsTemplate;

	@BeforeEach
	void init() {
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setSerializedPayloadClass(String.class);
		snsTemplate = new SnsTemplate(snsClient, new DefaultAutoTopicCreator(snsClient, true), converter);
	}

	@Test
	void sendsTextMessage() {
		when(snsClient.createTopic(CreateTopicRequest.builder().name("topic name").build()))
				.thenReturn(CreateTopicResponse.builder().topicArn("topic arn").build());

		snsTemplate.sendNotification("topic name", "message content", "subject");

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.topicArn()).isEqualTo("topic arn");
			assertThat(r.message()).isEqualTo("message content");
			assertThat(r.subject()).isEqualTo("subject");
			assertThat(r.messageAttributes().keySet()).contains(MessageHeaders.ID);
			assertThat(r.messageAttributes().keySet()).contains(MessageHeaders.TIMESTAMP);
		}));
	}

	@Test
	void sendsJsonMessage() {
		when(snsClient.createTopic(CreateTopicRequest.builder().name("topic name").build()))
				.thenReturn(CreateTopicResponse.builder().topicArn("topic arn").build());

		snsTemplate.sendNotification("topic name", new Person("John Doe"), "subject");

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.topicArn()).isEqualTo("topic arn");
			assertThat(r.message()).isEqualTo("{\"name\":\"John Doe\"}");
			assertThat(r.subject()).isEqualTo("subject");
			assertThat(r.messageAttributes().keySet()).contains(MessageHeaders.ID);
			assertThat(r.messageAttributes().keySet()).contains(MessageHeaders.TIMESTAMP);
		}));
	}

	@Test
	void sendsMessageToDefaultDestination() {
		when(snsClient.createTopic(CreateTopicRequest.builder().name("topic name").build()))
				.thenReturn(CreateTopicResponse.builder().topicArn("topic arn").build());

		snsTemplate.setDefaultDestinationName("topic name");
		snsTemplate.sendNotification("message content", "subject");

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.topicArn()).isEqualTo("topic arn");
		}));
	}

	static class Person {
		private final String name;

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

}
