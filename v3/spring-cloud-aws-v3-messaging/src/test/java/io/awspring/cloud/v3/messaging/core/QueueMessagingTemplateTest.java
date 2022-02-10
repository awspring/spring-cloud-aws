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

import java.time.LocalDate;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.awspring.cloud.v3.core.env.ResourceIdResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alain Sahli
 */
class QueueMessagingTemplateTest {

	@Test
	void send_withoutDefaultDestination_throwAnException() {
		SqsClient amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);

		Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
		assertThatThrownBy(() -> queueMessagingTemplate.send(stringMessage)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void send_withDefaultDestination_usesDefaultDestination() {
		SqsClient amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);
		queueMessagingTemplate.setDefaultDestinationName("my-queue");

		Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
		queueMessagingTemplate.send(stringMessage);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		verify(amazonSqs).sendMessage(sendMessageRequestArgumentCaptor.capture());
		assertThat(sendMessageRequestArgumentCaptor.getValue().queueUrl()).isEqualTo("https://queue-url.com");
	}

	@Test
	void send_withDestination_usesDestination() {
		SqsClient amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);

		Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
		queueMessagingTemplate.send("my-queue", stringMessage);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		verify(amazonSqs).sendMessage(sendMessageRequestArgumentCaptor.capture());
		assertThat(sendMessageRequestArgumentCaptor.getValue().queueUrl()).isEqualTo("https://queue-url.com");
	}

	@Test
	void send_withCustomDestinationResolveAndDestination_usesDestination() {
		SqsClient amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs,
				(DestinationResolver<String>) name -> name.toUpperCase(Locale.ENGLISH), null);

		Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
		queueMessagingTemplate.send("myqueue", stringMessage);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		verify(amazonSqs).sendMessage(sendMessageRequestArgumentCaptor.capture());
		assertThat(sendMessageRequestArgumentCaptor.getValue().queueUrl()).isEqualTo("MYQUEUE");
	}

	@Test
	void receive_withoutDefaultDestination_throwsAnException() {
		SqsClient amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);

		assertThatThrownBy(queueMessagingTemplate::receive).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void receive_withDefaultDestination_useDefaultDestination() {
		SqsClient amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);
		queueMessagingTemplate.setDefaultDestinationName("my-queue");

		queueMessagingTemplate.receive();

		ArgumentCaptor<ReceiveMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(ReceiveMessageRequest.class);
		verify(amazonSqs).receiveMessage(sendMessageRequestArgumentCaptor.capture());
		assertThat(sendMessageRequestArgumentCaptor.getValue().queueUrl()).isEqualTo("https://queue-url.com");
	}

	@Test
	void receive_withDestination_usesDestination() {
		SqsClient amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);

		queueMessagingTemplate.receive("my-queue");

		ArgumentCaptor<ReceiveMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(ReceiveMessageRequest.class);
		verify(amazonSqs).receiveMessage(sendMessageRequestArgumentCaptor.capture());
		assertThat(sendMessageRequestArgumentCaptor.getValue().queueUrl()).isEqualTo("https://queue-url.com");
	}

	@Test
	void receiveAndConvert_withoutDefaultDestination_throwsAnException() {
		SqsClient amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);

		assertThatThrownBy(() -> queueMessagingTemplate.receiveAndConvert(String.class))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void receiveAndConvert_withDefaultDestination_usesDefaultDestinationAndConvertsMessage() {
		SqsClient amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);
		queueMessagingTemplate.setDefaultDestinationName("my-queue");

		String message = queueMessagingTemplate.receiveAndConvert(String.class);

		assertThat(message).isEqualTo("My message");
	}

	@Test
	void receiveAndConvert_withDestination_usesDestinationAndConvertsMessage() {
		SqsClient amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);

		String message = queueMessagingTemplate.receiveAndConvert("my-queue", String.class);

		assertThat(message).isEqualTo("My message");
	}

	@Test
	void instantiation_withConverter_shouldAddItToTheCompositeConverter() {
		// Arrange
		SimpleMessageConverter simpleMessageConverter = new SimpleMessageConverter();

		// Act
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(createAmazonSqs(),
				(ResourceIdResolver) null, simpleMessageConverter);

		// Assert
		assertThat(((CompositeMessageConverter) queueMessagingTemplate.getMessageConverter()).getConverters())
				.hasSize(2);
		assertThat(((CompositeMessageConverter) queueMessagingTemplate.getMessageConverter()).getConverters().get(1))
				.isEqualTo(simpleMessageConverter);
	}

	@Test
	void instantiation_withoutConverter_shouldAddDefaultJacksonConverterToTheCompositeConverter() {
		// Act
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(createAmazonSqs(),
				(ResourceIdResolver) null, null);

		// Assert
		assertThat(((CompositeMessageConverter) queueMessagingTemplate.getMessageConverter()).getConverters())
				.hasSize(2);
		assertThat(((CompositeMessageConverter) queueMessagingTemplate.getMessageConverter()).getConverters().get(1))
				.isInstanceOf(MappingJackson2MessageConverter.class);
	}

	private SqsClient createAmazonSqs() {
		SqsClient amazonSqs = mock(SqsClient.class);


		GetQueueUrlResponse queueUrl = GetQueueUrlResponse.builder()
			.queueUrl("https://queue-url.com")
			.build();

		when(amazonSqs.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrl);

		ReceiveMessageResponse receiveMessageResult = ReceiveMessageResponse.builder()
			.messages(software.amazon.awssdk.services.sqs.model.Message.builder()
				.body("My message")
				.build())
			.build();

		when(amazonSqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(receiveMessageResult);

		return amazonSqs;
	}

	private static class TestPerson {

		private String firstName;

		private String lastName;

		private LocalDate activeSince;

		private TestPerson(String firstName, @JsonProperty String lastName, @JsonProperty LocalDate activeSince) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.activeSince = activeSince;
		}

		protected TestPerson() {
		}

		public String getFirstName() {
			return this.firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return this.lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public LocalDate getActiveSince() {
			return this.activeSince;
		}

		public void setActiveSince(LocalDate activeSince) {
			this.activeSince = activeSince;
		}

	}

}
