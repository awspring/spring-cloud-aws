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

package org.springframework.cloud.aws.messaging.core;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alain Sahli
 */
public class QueueMessagingTemplateTest {

	@Test(expected = IllegalStateException.class)
	public void send_withoutDefaultDestination_throwAnException() {
		AmazonSQSAsync amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				amazonSqs);

		Message<String> stringMessage = MessageBuilder.withPayload("message content")
				.build();
		queueMessagingTemplate.send(stringMessage);
	}

	@Test
	public void send_withDefaultDestination_usesDefaultDestination() {
		AmazonSQSAsync amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				amazonSqs);
		queueMessagingTemplate.setDefaultDestinationName("my-queue");

		Message<String> stringMessage = MessageBuilder.withPayload("message content")
				.build();
		queueMessagingTemplate.send(stringMessage);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		verify(amazonSqs).sendMessage(sendMessageRequestArgumentCaptor.capture());
		assertThat(sendMessageRequestArgumentCaptor.getValue().getQueueUrl())
				.isEqualTo("https://queue-url.com");
	}

	@Test
	public void send_withDestination_usesDestination() {
		AmazonSQSAsync amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				amazonSqs);

		Message<String> stringMessage = MessageBuilder.withPayload("message content")
				.build();
		queueMessagingTemplate.send("my-queue", stringMessage);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		verify(amazonSqs).sendMessage(sendMessageRequestArgumentCaptor.capture());
		assertThat(sendMessageRequestArgumentCaptor.getValue().getQueueUrl())
				.isEqualTo("https://queue-url.com");
	}

	@Test
	public void send_withCustomDestinationResolveAndDestination_usesDestination() {
		AmazonSQSAsync amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				amazonSqs,
				(DestinationResolver<String>) name -> name.toUpperCase(Locale.ENGLISH),
				null);

		Message<String> stringMessage = MessageBuilder.withPayload("message content")
				.build();
		queueMessagingTemplate.send("myqueue", stringMessage);

		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		verify(amazonSqs).sendMessage(sendMessageRequestArgumentCaptor.capture());
		assertThat(sendMessageRequestArgumentCaptor.getValue().getQueueUrl())
				.isEqualTo("MYQUEUE");
	}

	@Test(expected = IllegalStateException.class)
	public void receive_withoutDefaultDestination_throwsAnException() {
		AmazonSQSAsync amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				amazonSqs);

		queueMessagingTemplate.receive();
	}

	@Test
	public void receive_withDefaultDestination_useDefaultDestination() {
		AmazonSQSAsync amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				amazonSqs);
		queueMessagingTemplate.setDefaultDestinationName("my-queue");

		queueMessagingTemplate.receive();

		ArgumentCaptor<ReceiveMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(ReceiveMessageRequest.class);
		verify(amazonSqs).receiveMessage(sendMessageRequestArgumentCaptor.capture());
		assertThat(sendMessageRequestArgumentCaptor.getValue().getQueueUrl())
				.isEqualTo("https://queue-url.com");
	}

	@Test
	public void receive_withDestination_usesDestination() {
		AmazonSQSAsync amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				amazonSqs);

		queueMessagingTemplate.receive("my-queue");

		ArgumentCaptor<ReceiveMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(ReceiveMessageRequest.class);
		verify(amazonSqs).receiveMessage(sendMessageRequestArgumentCaptor.capture());
		assertThat(sendMessageRequestArgumentCaptor.getValue().getQueueUrl())
				.isEqualTo("https://queue-url.com");
	}

	@Test(expected = IllegalStateException.class)
	public void receiveAndConvert_withoutDefaultDestination_throwsAnException() {
		AmazonSQSAsync amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				amazonSqs);

		queueMessagingTemplate.receiveAndConvert(String.class);
	}

	@Test
	public void receiveAndConvert_withDefaultDestination_usesDefaultDestinationAndConvertsMessage() {
		AmazonSQSAsync amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				amazonSqs);
		queueMessagingTemplate.setDefaultDestinationName("my-queue");

		String message = queueMessagingTemplate.receiveAndConvert(String.class);

		assertThat(message).isEqualTo("My message");
	}

	@Test
	public void receiveAndConvert_withDestination_usesDestinationAndConvertsMessage() {
		AmazonSQSAsync amazonSqs = createAmazonSqs();
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				amazonSqs);

		String message = queueMessagingTemplate.receiveAndConvert("my-queue",
				String.class);

		assertThat(message).isEqualTo("My message");
	}

	@Test
	public void instantiation_withConverter_shouldAddItToTheCompositeConverter() {
		// Arrange
		SimpleMessageConverter simpleMessageConverter = new SimpleMessageConverter();

		// Act
		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				createAmazonSqs(), (ResourceIdResolver) null, simpleMessageConverter);

		// Assert
		assertThat(
				((CompositeMessageConverter) queueMessagingTemplate.getMessageConverter())
						.getConverters().size()).isEqualTo(2);
		assertThat(
				((CompositeMessageConverter) queueMessagingTemplate.getMessageConverter())
						.getConverters().get(1)).isEqualTo(simpleMessageConverter);
	}

	@Test
	public void instantiation_WithCustomJacksonConverterThatSupportsJava8Types_shouldConvertMessageToString()
			throws IOException {

		// Arrange
		AmazonSQSAsync amazonSqs = createAmazonSqs();

		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

		MappingJackson2MessageConverter simpleMessageConverter = new MappingJackson2MessageConverter();
		simpleMessageConverter.setSerializedPayloadClass(String.class);
		simpleMessageConverter.setObjectMapper(objectMapper);

		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				amazonSqs, (ResourceIdResolver) null, simpleMessageConverter);

		// Act
		queueMessagingTemplate.convertAndSend("test",
				new TestPerson("Agim", "Emruli", LocalDate.of(2017, 1, 1)));

		// Assert
		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		verify(amazonSqs).sendMessage(sendMessageRequestArgumentCaptor.capture());
		TestPerson testPerson = objectMapper.readValue(
				sendMessageRequestArgumentCaptor.getValue().getMessageBody(),
				TestPerson.class);

		assertThat(testPerson.getFirstName()).isEqualTo("Agim");
		assertThat(testPerson.getLastName()).isEqualTo("Emruli");
		assertThat(testPerson.getActiveSince()).isEqualTo(LocalDate.of(2017, 1, 1));
	}

	@Test
	public void instantiation_withDefaultMapping2JacksonConverter_shouldSupportJava8Types()
			throws IOException {

		// Arrange
		AmazonSQSAsync amazonSqs = createAmazonSqs();

		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

		QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(
				amazonSqs);

		// Act
		queueMessagingTemplate.convertAndSend("test",
				new TestPerson("Agim", "Emruli", LocalDate.of(2017, 1, 1)));

		// Assert
		ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(SendMessageRequest.class);
		verify(amazonSqs).sendMessage(sendMessageRequestArgumentCaptor.capture());
		TestPerson testPerson = objectMapper.readValue(
				sendMessageRequestArgumentCaptor.getValue().getMessageBody(),
				TestPerson.class);

		assertThat(testPerson.getFirstName()).isEqualTo("Agim");
		assertThat(testPerson.getLastName()).isEqualTo("Emruli");
		assertThat(testPerson.getActiveSince()).isEqualTo(LocalDate.of(2017, 1, 1));
	}

	private AmazonSQSAsync createAmazonSqs() {
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);

		GetQueueUrlResult queueUrl = new GetQueueUrlResult();
		queueUrl.setQueueUrl("https://queue-url.com");
		when(amazonSqs.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrl);

		ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult();
		com.amazonaws.services.sqs.model.Message message = new com.amazonaws.services.sqs.model.Message();
		message.setBody("My message");
		receiveMessageResult.withMessages(message);
		when(amazonSqs.receiveMessage(any(ReceiveMessageRequest.class)))
				.thenReturn(receiveMessageResult);

		return amazonSqs;
	}

	private static class TestPerson {

		private String firstName;

		private String lastName;

		private LocalDate activeSince;

		private TestPerson(String firstName, @JsonProperty String lastName,
				@JsonProperty LocalDate activeSince) {
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
