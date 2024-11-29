/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sqs.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.QueueAttributesResolvingException;
import io.awspring.cloud.sqs.SqsAcknowledgementException;
import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.support.converter.ContextAwareMessagingMessageConverter;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

/**
 * @author Tomaz Fernandes
 */
@SuppressWarnings("unchecked")
class SqsTemplateTests {

	SqsAsyncClient mockClient;

	@BeforeEach
	void beforeEach() {
		mockClient = mock(SqsAsyncClient.class);
	}

	@Test
	void shouldSendWithOptions() {
		String queue = "test-queue";
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString())
				.sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		SqsOperations template = SqsTemplate.newTemplate(mockClient);
		String payload = "test-payload";
		String testHeaderName = "testHeaderName";
		String testHeaderValue = "testHeaderValue";
		String testHeaderName2 = "testHeaderName2";
		String testHeaderValue2 = "testHeaderValue2";
		Integer delaySeconds = 5;
		SendResult<String> result = template.send(to -> to.delaySeconds(delaySeconds).queue(queue).payload(payload)
				.header(testHeaderName, testHeaderValue).headers(Map.of(testHeaderName2, testHeaderValue2)));
		assertThat(result.endpoint()).isEqualTo(queue);
		assertThat(result.message().getHeaders()).containsKeys(testHeaderName, testHeaderName2)
				.containsValues(testHeaderValue, testHeaderValue2);
		assertThat(result.message().getPayload()).isEqualTo(payload);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.delaySeconds()).isEqualTo(delaySeconds);
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		assertThat(capturedRequest.messageBody()).isEqualTo(payload);
		assertThat(capturedRequest.messageAttributes()).containsKeys(testHeaderName, testHeaderName2);
	}

	@Test
	void shouldSendFifoWithOptions() {
		String queue = "test-queue";
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString())
				.sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		var messageGroupId = UUID.randomUUID().toString();
		var messageDeduplicationId = UUID.randomUUID().toString();
		SqsOperations template = SqsTemplate.newTemplate(mockClient);
		String payload = "test-payload";
		SendResult<String> result = template.send(to -> to.queue(queue).messageGroupId(messageGroupId)
				.messageDeduplicationId(messageDeduplicationId).payload(payload));
		assertThat(result.endpoint()).isEqualTo(queue);
		assertThat(result.message().getHeaders()).containsAllEntriesOf(
				Map.of(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, messageDeduplicationId,
						SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, messageGroupId));
		assertThat(result.message().getPayload()).isEqualTo(payload);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		assertThat(capturedRequest.messageBody()).isEqualTo(payload);
		assertThat(capturedRequest.messageGroupId()).isEqualTo(messageGroupId);
		assertThat(capturedRequest.messageDeduplicationId()).isEqualTo(messageDeduplicationId);
	}

	@Test
	void shouldAddFifoHeadersToSend() {
		String queue = "test-queue.fifo";
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString())
				.sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		mockQueueAttributes(mockClient, Map.of(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "false"));
		SqsOperations template = SqsTemplate.newTemplate(mockClient);
		String payload = "test-payload";
		SendResult<String> result = template.send(queue, payload);
		assertThat(result.endpoint()).isEqualTo(queue);
		MessageHeaders resultHeaders = result.message().getHeaders();
		assertThat(resultHeaders).containsKeys(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER,
				SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER);
		String messageDeduplicationId = resultHeaders
				.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, String.class);
		String messageGroupId = resultHeaders.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER,
				String.class);
		assertThat(result.message().getPayload()).isEqualTo(payload);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		assertThat(capturedRequest.messageBody()).isEqualTo(payload);
		assertThat(capturedRequest.messageGroupId()).isEqualTo(messageGroupId);
		assertThat(capturedRequest.messageDeduplicationId()).isEqualTo(messageDeduplicationId);
	}

	private static void mockQueueAttributes(SqsAsyncClient mockClient, Map<QueueAttributeName, String> attributes) {
		GetQueueAttributesResponse queueAttributesResponse = GetQueueAttributesResponse.builder().attributes(attributes)
				.build();
		given(mockClient.getQueueAttributes(any(Consumer.class)))
				.willReturn(CompletableFuture.completedFuture(queueAttributesResponse));
	}

	@Test
	void shouldAddFifoHeadersToSendWithContentBasedDeduplicationQueueConfig() {
		String queue = "test-queue.fifo";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString())
				.sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		mockQueueAttributes(mockClient, Map.of(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "true"));
		SqsOperations template = SqsTemplate.newTemplate(mockClient);
		String payload = "test-payload";
		SendResult<String> result = template.send(queue, payload);
		assertThat(result.endpoint()).isEqualTo(queue);
		MessageHeaders resultHeaders = result.message().getHeaders();
		assertThat(resultHeaders).containsKey(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER)
				.doesNotContainKey(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER);
		String messageDeduplicationId = resultHeaders
				.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, String.class);
		String messageGroupId = resultHeaders.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER,
				String.class);
		assertThat(result.message().getPayload()).isEqualTo(payload);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		assertThat(capturedRequest.messageBody()).isEqualTo(payload);
		assertThat(capturedRequest.messageGroupId()).isEqualTo(messageGroupId);
		assertThat(capturedRequest.messageDeduplicationId()).isEqualTo(messageDeduplicationId);
	}

	@Test
	void shouldSendWithDefaultEndpoint() {
		String queue = "test-queue";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());

		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString())
				.sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient)
				.configure(options -> options.defaultQueue(queue)).buildSyncTemplate();
		SendResult<String> result = template.send(payload);
		assertThat(result.endpoint()).isEqualTo(queue);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
	}

	@Test
	void shouldSendWithDeduplicationIdWhenContentBasedDeduplicationSetToAutoAndIsDisabled() {
		String queue = "test-queue.fifo";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());

		var queueAttributesResponse = GetQueueAttributesResponse.builder()
				.attributes(Map.of(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "false")).build();

		given(mockClient.getQueueAttributes(any(Consumer.class)))
				.willReturn(CompletableFuture.completedFuture(queueAttributesResponse));

		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString())
				.sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient).configure(options -> options
				.defaultQueue(queue).contentBasedDeduplication(TemplateContentBasedDeduplication.AUTO))
				.buildSyncTemplate();

		SendResult<String> result = template.send(payload);

		assertThat(result.endpoint()).isEqualTo(queue);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		assertThat(capturedRequest.messageDeduplicationId()).isNotNull();
	}

	@Test
	void shouldSendWithoutDeduplicationIdWhenContentBasedDeduplicationSetToAutoAndEnabled() {
		String queue = "test-queue.fifo";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());

		var queueAttributesResponse = GetQueueAttributesResponse.builder()
				.attributes(Map.of(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "true")).build();

		given(mockClient.getQueueAttributes(any(Consumer.class)))
				.willReturn(CompletableFuture.completedFuture(queueAttributesResponse));

		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString())
				.sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient).configure(options -> options
				.defaultQueue(queue).contentBasedDeduplication(TemplateContentBasedDeduplication.AUTO))
				.buildSyncTemplate();

		SendResult<String> result = template.send(payload);

		assertThat(result.endpoint()).isEqualTo(queue);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		assertThat(capturedRequest.messageDeduplicationId()).isNull();
	}

	@Test
	void shouldSendWithDeduplicationIdWhenContentBasedDeduplicationSetToDisabled() {
		String queue = "test-queue.fifo";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());

		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString())
				.sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient).configure(options -> options
				.defaultQueue(queue).contentBasedDeduplication(TemplateContentBasedDeduplication.DISABLED))
				.buildSyncTemplate();

		SendResult<String> result = template.send(payload);

		assertThat(result.endpoint()).isEqualTo(queue);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		assertThat(capturedRequest.messageDeduplicationId()).isNotNull();
	}

	@Test
	void shouldSendWithoutDeduplicationIdWhenContentBasedDeduplicationSetToEnabled() {
		String queue = "test-queue.fifo";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());

		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString())
				.sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient).configure(options -> options
				.defaultQueue(queue).contentBasedDeduplication(TemplateContentBasedDeduplication.ENABLED))
				.buildSyncTemplate();

		SendResult<String> result = template.send(payload);

		assertThat(result.endpoint()).isEqualTo(queue);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		assertThat(capturedRequest.messageDeduplicationId()).isNull();
	}

	@Test
	void shouldWrapSendError() {
		String queue = "test-queue";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.failedFuture(new RuntimeException("Expected send error")));
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient)
				.configure(options -> options.defaultQueue(queue)).buildSyncTemplate();
		assertThatThrownBy(() -> template.send(payload)).isInstanceOf(MessagingOperationFailedException.class)
				.isInstanceOfSatisfying(MessagingOperationFailedException.class, ex -> {
					assertThat(ex.getEndpoint()).isEqualTo(queue);
					assertThat(ex.getFailedMessage()).isPresent().hasValueSatisfying(msg -> {
						assertThat(msg.getPayload()).isEqualTo(payload);
					});
				});
	}

	@Test
	void shouldSendWithQueueAndPayload() {
		String queue = "test-queue";
		String payload = "test-payload";
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString())
				.sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));

		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		SendResult<String> result = template.send(queue, payload);
		assertThat(result.endpoint()).isEqualTo(queue);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		assertThat(capturedRequest.messageBody()).isEqualTo(payload);
	}

	@Test
	void shouldSendWithQueueAndMessageAndHeaders() {
		String queue = "test-queue";
		String payload = "test-payload";
		String headerName = "headerName";
		String headerValue = "headerValue";
		Message<String> message = MessageBuilder.withPayload(payload).setHeader(headerName, headerValue).build();

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString())
				.sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		SendResult<String> result = template.send(queue, message);
		assertThat(result.endpoint()).isEqualTo(queue);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		assertThat(capturedRequest.messageBody()).isEqualTo(payload);
		assertThat(capturedRequest.messageAttributes().get(headerName).stringValue()).isEqualTo(headerValue);
	}

	@Test
	void shouldSendBatch() {
		String queue = "test-queue";
		String payload1 = "test-payload-1";
		String payload2 = "test-payload-2";
		String headerName1 = "headerName";
		String headerValue1 = "headerValue";
		String headerName2 = "headerName2";
		String headerValue2 = "headerValue2";
		Message<String> message1 = MessageBuilder.withPayload(payload1).setHeader(headerName1, headerValue1).build();
		Message<String> message2 = MessageBuilder.withPayload(payload2).setHeader(headerName2, headerValue2).build();
		List<Message<String>> messages = List.of(message1, message2);

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());

		SendMessageBatchResponse response = SendMessageBatchResponse.builder().successful(
				builder -> builder.id(message1.getHeaders().getId().toString()).messageId(UUID.randomUUID().toString()),
				builder -> builder.id(message2.getHeaders().getId().toString()).messageId(UUID.randomUUID().toString()))
				.build();
		given(mockClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		SendResult.Batch<String> results = template.sendMany(queue, messages);
		assertThat(results.successful()).hasSize(2);
		Iterator<SendResult<String>> iterator = results.successful().iterator();
		SendResult<String> firstResult = iterator.next();
		SendResult<String> secondResult = iterator.next();
		assertThat(firstResult.endpoint()).isEqualTo(queue);
		assertThat(firstResult.message().getPayload()).isEqualTo(payload1);
		assertThat(secondResult.endpoint()).isEqualTo(queue);
		assertThat(secondResult.message().getPayload()).isEqualTo(payload2);
		ArgumentCaptor<SendMessageBatchRequest> captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
		then(mockClient).should().sendMessageBatch(captor.capture());
		SendMessageBatchRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		Iterator<SendMessageBatchRequestEntry> requestIterator = capturedRequest.entries().iterator();
		SendMessageBatchRequestEntry firstEntry = requestIterator.next();
		SendMessageBatchRequestEntry secondEntry = requestIterator.next();
		assertThat(firstEntry.messageBody()).isEqualTo(payload1);
		assertThat(firstEntry.messageAttributes().get(headerName1).stringValue()).isEqualTo(headerValue1);
		assertThat(secondEntry.messageBody()).isEqualTo(payload2);
		assertThat(secondEntry.messageAttributes().get(headerName2).stringValue()).isEqualTo(headerValue2);
	}

	@Test
	void shouldAddFailedToTheBatchResult() {
		String queue = "test-queue";
		String payload1 = "test-payload-1";
		String payload2 = "test-payload-2";
		Message<String> message1 = MessageBuilder.withPayload(payload1).build();
		Message<String> message2 = MessageBuilder.withPayload(payload2).build();
		List<Message<String>> messages = List.of(message1, message2);

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		String testErrorMessage = "test error message";
		String code = "BC01";
		boolean senderFault = true;
		SendMessageBatchResponse response = SendMessageBatchResponse.builder().successful(
				builder -> builder.id(message1.getHeaders().getId().toString()).messageId(UUID.randomUUID().toString()))
				.failed(builder -> builder.id(message2.getHeaders().getId().toString()).message(testErrorMessage)
						.code(code).senderFault(senderFault))
				.build();
		given(mockClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		SqsOperations template = SqsTemplate.builder().configure(
				options -> options.sendBatchFailureHandlingStrategy(SendBatchFailureHandlingStrategy.DO_NOT_THROW))
				.sqsAsyncClient(mockClient).buildSyncTemplate();
		SendResult.Batch<String> results = template.sendMany(queue, messages);
		assertThat(results.successful()).isNotEmpty();
		assertThat(results.failed()).isNotEmpty();
		SendResult<String> successfulResult = results.successful().iterator().next();
		assertThat(successfulResult.endpoint()).isEqualTo(queue);
		assertThat(successfulResult.message().getPayload()).isEqualTo(payload1);
		SendResult.Failed<String> failedResult = results.failed().iterator().next();
		assertThat(failedResult.errorMessage()).isEqualTo(testErrorMessage);
		assertThat(failedResult.message().getPayload()).isEqualTo(payload2);
		assertThat(failedResult.additionalInformation().get(SqsTemplateParameters.ERROR_CODE_PARAMETER_NAME))
				.isEqualTo(code);
		assertThat(failedResult.additionalInformation().get(SqsTemplateParameters.SENDER_FAULT_PARAMETER_NAME))
				.isEqualTo(senderFault);
	}

	@Test
	void shouldThrowIfHasFailedMessagesInBatchByDefault() {
		String queue = "test-queue";
		String payload1 = "test-payload-1";
		String payload2 = "test-payload-2";
		Message<String> message1 = MessageBuilder.withPayload(payload1).build();
		Message<String> message2 = MessageBuilder.withPayload(payload2).build();
		List<Message<String>> messages = List.of(message1, message2);

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		String testErrorMessage = "test error message";
		String code = "BC01";
		boolean senderFault = true;
		SendMessageBatchResponse response = SendMessageBatchResponse.builder().successful(
				builder -> builder.id(message1.getHeaders().getId().toString()).messageId(UUID.randomUUID().toString()))
				.failed(builder -> builder.id(message2.getHeaders().getId().toString()).message(testErrorMessage)
						.code(code).senderFault(senderFault))
				.build();
		given(mockClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		assertThatThrownBy(() -> template.sendMany(queue, messages))
				.isInstanceOf(SendBatchOperationFailedException.class)
				.isInstanceOfSatisfying(SendBatchOperationFailedException.class, ex -> {
					assertThat(ex.getFailedMessages().iterator().next().getPayload()).isEqualTo(payload2);
					assertThat(ex.getEndpoint()).isEqualTo(queue);
					SendResult.Batch<String> sendBatchResult = ex.getSendBatchResult(String.class);
					SendResult<String> successful = sendBatchResult.successful().iterator().next();
					assertThat(successful.message().getPayload()).isEqualTo(payload1);
					assertThat(successful.endpoint()).isEqualTo(queue);
					SendResult.Failed<String> failedResult = sendBatchResult.failed().iterator().next();
					assertThat(failedResult.errorMessage()).isEqualTo(testErrorMessage);
					assertThat(failedResult.message().getPayload()).isEqualTo(payload2);
					assertThat(
							failedResult.additionalInformation().get(SqsTemplateParameters.ERROR_CODE_PARAMETER_NAME))
							.isEqualTo(code);
					assertThat(
							failedResult.additionalInformation().get(SqsTemplateParameters.SENDER_FAULT_PARAMETER_NAME))
							.isEqualTo(senderFault);
				});
	}

	@Test
	void shouldCreateByDefaultIfQueueNotFound() {
		String queue = "test-queue";
		String payload = "test-payload";

		mockQueueAttributes(mockClient, Map.of());
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class))).willReturn(CompletableFuture
				.failedFuture(QueueDoesNotExistException.builder().message("test queue not found").build()));
		given(mockClient.createQueue(any(Consumer.class)))
				.willReturn(CompletableFuture.completedFuture(CreateQueueResponse.builder().queueUrl(queue).build()));
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString())
				.sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		SendResult<String> result = template.send(to -> to.queue(queue).payload(payload));
		assertThat(result.endpoint()).isEqualTo(queue);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
	}

	@Test
	void shouldThrowIfQueueNotFound() {
		String queue = "test-queue";
		String payload = "test-payload";

		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class))).willReturn(CompletableFuture
				.failedFuture(QueueDoesNotExistException.builder().message("test queue not found").build()));

		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient)
				.configure(options -> options.queueNotFoundStrategy(QueueNotFoundStrategy.FAIL)).buildSyncTemplate();
		assertThatThrownBy(() -> template.send(to -> to.queue(queue).payload(payload)))
				.isInstanceOf(MessagingOperationFailedException.class).cause().isInstanceOf(CompletionException.class)
				.cause().isInstanceOf(QueueAttributesResolvingException.class).cause()
				.isInstanceOf(QueueDoesNotExistException.class);
	}

	@Test
	void shouldReceiveEmpty() {
		String queue = "test-queue";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder().build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient)
				.configure(options -> options.defaultQueue(queue)).buildSyncTemplate();
		Optional<Message<?>> receivedMessage = template.receive();
		assertThat(receivedMessage).isEmpty();
	}

	@Test
	void shouldReceiveFromDefaultEndpoint() {
		String queue = "test-queue";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder().messages(builder -> builder
				.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient)
				.configure(options -> options.defaultQueue(queue)).buildSyncTemplate();
		Optional<Message<?>> receivedMessage = template.receive();
		assertThat(receivedMessage).isPresent()
				.hasValueSatisfying(message -> assertThat(message.getPayload()).isEqualTo(payload));
	}

	@Test
	void shouldConvertToPayloadClass() throws Exception {
		String queue = "test-queue";
		SampleRecord payload = new SampleRecord("first-prop", "second-prop");
		String payloadString = new ObjectMapper().writeValueAsString(payload);

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());

		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder()
				.messages(builder -> builder.messageId(UUID.randomUUID().toString())
						.receiptHandle("test-receipt-handle").body(payloadString).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		Optional<Message<SampleRecord>> receivedMessage = template.receive(from -> from.queue(queue),
				SampleRecord.class);
		assertThat(receivedMessage).isPresent()
				.hasValueSatisfying(message -> assertThat(message.getPayload()).isEqualTo(payload));
	}

	@Test
	void shouldConvertToDefaultClass() throws Exception {
		String queue = "test-queue";
		SampleRecord payload = new SampleRecord("first-prop", "second-prop");
		String payloadString = new ObjectMapper().writeValueAsString(payload);

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder()
				.messages(builder -> builder.messageId(UUID.randomUUID().toString())
						.receiptHandle("test-receipt-handle").body(payloadString).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient)
				.configure(options -> options.defaultPayloadClass(SampleRecord.class)).buildSyncTemplate();
		Optional<Message<?>> receivedMessage = template.receive(from -> from.queue(queue));
		assertThat(receivedMessage).isPresent().hasValueSatisfying(message -> {
			SampleRecord receivedPayload = (SampleRecord) message.getPayload();
			assertThat(receivedPayload).isEqualTo(payload);
			assertThat(receivedPayload.firstProperty).isEqualTo(payload.firstProperty);
			assertThat(receivedPayload.secondProperty).isEqualTo(payload.secondProperty);
		});
	}

	record SampleRecord(String firstProperty, String secondProperty) {
	}

	@SuppressWarnings("rawtypes")
	@Test
	void shouldUseCustomConverter() {
		String queue = "test-queue";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		ContextAwareMessagingMessageConverter<software.amazon.awssdk.services.sqs.model.Message> converter = mock(
				ContextAwareMessagingMessageConverter.class);
		String receiptHandle = "test-receipt-handle";
		Message message = MessageBuilder.withPayload(payload)
				.setHeader(SqsHeaders.SQS_RECEIPT_HANDLE_HEADER, receiptHandle).build();
		given(converter.toMessagingMessage(any(software.amazon.awssdk.services.sqs.model.Message.class), any()))
				.willReturn(message);
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder().messages(builder -> builder
				.messageId(UUID.randomUUID().toString()).receiptHandle(receiptHandle).body(payload).build()).build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient).messageConverter(converter)
				.configure(options -> options.defaultQueue(queue)).buildSyncTemplate();
		Optional<Message<String>> receivedMessage = template.receive(queue, String.class);
		assertThat(receivedMessage).isPresent()
				.hasValueSatisfying(msg -> assertThat(msg.getPayload()).isEqualTo(payload));
	}

	@Test
	void shouldReceiveAndNotAcknowledge() {
		String queue = "test-queue";
		String payload = "test-payload";
		String receiptHandle = "test-receipt-handle";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder().messages(builder -> builder
				.messageId(UUID.randomUUID().toString()).receiptHandle(receiptHandle).body(payload).build()).build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient)
				.configure(
						options -> options.defaultQueue(queue).acknowledgementMode(TemplateAcknowledgementMode.MANUAL))
				.buildSyncTemplate();
		Optional<Message<?>> receivedMessage = template.receive();
		assertThat(receivedMessage).isPresent()
				.hasValueSatisfying(message -> assertThat(message.getPayload()).isEqualTo(payload));
		then(mockClient).should(never()).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
	}

	@Test
	void shouldWrapFullAcknowledgementError() {
		String queue = "test-queue";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder().messages(builder -> builder
				.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.failedFuture(new RuntimeException("Expected ack error")));
		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		assertThatThrownBy(() -> template.receive(from -> from.queue(queue)))
				.isInstanceOf(MessagingOperationFailedException.class).cause()
				.isInstanceOf(SqsAcknowledgementException.class)
				.isInstanceOfSatisfying(SqsAcknowledgementException.class, ex -> {
					assertThat(ex.getFailedAcknowledgementMessages()).hasSize(1).allSatisfy(message -> {
						assertThat(message.getPayload()).isEqualTo(payload);
					});
				});
	}

	@Test
	void shouldWrapPartialAcknowledgementError() {
		String queue = "test-queue";
		String payload1 = "test-payload-1";
		String payload2 = "test-payload-2";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		String messageId1 = UUID.randomUUID().toString();
		String messageId2 = UUID.randomUUID().toString();
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder().messages(
				builder -> builder.messageId(messageId1).receiptHandle("test-receipt-handle").body(payload1),
				builder -> builder.messageId(messageId2).receiptHandle("test-receipt-handle").body(payload2).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(messageId1)).failed(builder -> builder.id(messageId2)).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		assertThatThrownBy(() -> template.receive(from -> from.queue(queue)))
				.isInstanceOf(MessagingOperationFailedException.class).cause()
				.isInstanceOf(SqsAcknowledgementException.class)
				.isInstanceOfSatisfying(SqsAcknowledgementException.class, ex -> {
					assertThat(ex.getSuccessfullyAcknowledgedMessages()).hasSize(1)
							.allSatisfy(message -> assertThat(message.getPayload()).isEqualTo(payload1));
					assertThat(ex.getFailedAcknowledgementMessages()).hasSize(1)
							.allSatisfy(message -> assertThat(message.getPayload()).isEqualTo(payload2));
				});
	}

	@Test
	void shouldReceiveFromDefaultSettings() {
		String queue = "test-queue";
		String payload = "test-payload";
		String headerName1 = "headerName";
		String headerValue1 = "headerValue";
		String headerName2 = "headerName2";
		String headerValue2 = "headerValue2";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder().messages(builder -> builder
				.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		mockQueueAttributes(mockClient, Map.of(QueueAttributeName.QUEUE_ARN, "queue-arn"));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		String testMessageAttributes = "test-message-attributes";
		MessageSystemAttributeName systemAttribute = MessageSystemAttributeName.MESSAGE_GROUP_ID;
		QueueAttributeName queueAttribute = QueueAttributeName.QUEUE_ARN;
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient)
				.configure(options -> options.defaultQueue(queue).defaultPollTimeout(Duration.ofSeconds(1))
						.messageAttributeNames(Collections.singletonList(testMessageAttributes))
						.messageSystemAttributeNames(Collections.singletonList(systemAttribute))
						.queueAttributeNames(Collections.singletonList(queueAttribute))
						.additionalHeaderForReceive(headerName1, headerValue1)
						.additionalHeadersForReceive(Map.of(headerName2, headerValue2)))
				.buildSyncTemplate();
		Optional<Message<?>> receivedMessage = template.receive();
		assertThat(receivedMessage).isPresent().hasValueSatisfying(message -> {
			assertThat(message.getPayload()).isEqualTo(payload);
			assertThat(message.getHeaders()).containsEntry(headerName1, headerValue1);
			assertThat(message.getHeaders()).containsEntry(headerName2, headerValue2);
		});
		ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
		then(mockClient).should().receiveMessage(captor.capture());
		ReceiveMessageRequest request = captor.getValue();
		assertThat(request.maxNumberOfMessages()).isEqualTo(1);
		assertThat(request.queueUrl()).isEqualTo(queue);
		assertThat(request.messageAttributeNames()).containsExactly(testMessageAttributes);
		assertThat(request.attributeNamesAsStrings().get(0)).isEqualTo(systemAttribute.name());
		ArgumentCaptor<GetQueueUrlRequest> queueCaptor = ArgumentCaptor.forClass(GetQueueUrlRequest.class);
		then(mockClient).should().getQueueUrl(queueCaptor.capture());
		GetQueueUrlRequest capturedUrlRequest = queueCaptor.getValue();
		assertThat(capturedUrlRequest.queueName()).isEqualTo(queue);
		ArgumentCaptor<Consumer<GetQueueAttributesRequest.Builder>> queueAttributesCaptor = ArgumentCaptor
				.forClass(Consumer.class);
		then(mockClient).should().getQueueAttributes(queueAttributesCaptor.capture());
		GetQueueAttributesRequest.Builder getAttributesBuilder = GetQueueAttributesRequest.builder();
		queueAttributesCaptor.getValue().accept(getAttributesBuilder);
		GetQueueAttributesRequest attributesRequest = getAttributesBuilder.build();
		assertThat(attributesRequest.attributeNamesAsStrings()).hasSize(1).first().isEqualTo(queueAttribute.toString());
		assertThat(attributesRequest.queueUrl()).isEqualTo(queue);

	}

	@Test
	void shouldReceiveFromOptions() {
		String queue = "test-queue";
		String payload = "test-payload";
		String headerName1 = "headerName";
		String headerValue1 = "headerValue";
		String headerName2 = "headerName2";
		String headerValue2 = "headerValue2";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());

		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder().messages(builder -> builder
				.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		Optional<Message<String>> receivedMessage = template.receive(from -> from.queue(queue)
				.pollTimeout(Duration.ofSeconds(1)).visibilityTimeout(Duration.ofSeconds(5))
				.additionalHeader(headerName1, headerValue1).additionalHeaders(Map.of(headerName2, headerValue2)),
				String.class);
		assertThat(receivedMessage).isPresent().hasValueSatisfying(message -> {
			assertThat(message.getPayload()).isEqualTo(payload);
			assertThat(message.getHeaders()).containsEntry(headerName1, headerValue1);
			assertThat(message.getHeaders()).containsEntry(headerName2, headerValue2);
		});
		ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
		then(mockClient).should().receiveMessage(captor.capture());
		ReceiveMessageRequest request = captor.getValue();
		assertThat(request.maxNumberOfMessages()).isEqualTo(1);
		assertThat(request.visibilityTimeout()).isEqualTo(5);
		assertThat(request.waitTimeSeconds()).isEqualTo(1);
	}

	@Test
	void shouldReceiveFifoWithGivenAttemptId() {
		String queue = "test-queue";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		String messageGroupId = UUID.randomUUID().toString();
		String deduplicationId = UUID.randomUUID().toString();
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder()
				.messages(builder -> builder.messageId(UUID.randomUUID().toString())
						.attributes(Map.of(MessageSystemAttributeName.MESSAGE_GROUP_ID, messageGroupId,
								MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID, deduplicationId))
						.receiptHandle("test-receipt-handle").body(payload).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		UUID attemptId = UUID.randomUUID();
		Optional<Message<String>> receivedMessage = template
				.receive(from -> from.queue(queue).receiveRequestAttemptId(attemptId), String.class);
		assertThat(receivedMessage).isPresent().hasValueSatisfying(message -> {
			assertThat(message.getPayload()).isEqualTo(payload);
			assertThat(message.getHeaders())
					.containsEntry(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, messageGroupId);
			assertThat(message.getHeaders()).containsEntry(
					SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, deduplicationId);
		});
		ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
		then(mockClient).should().receiveMessage(captor.capture());
		ReceiveMessageRequest request = captor.getValue();
		assertThat(request.receiveRequestAttemptId()).isEqualTo(attemptId.toString());
		assertThat(request.maxNumberOfMessages()).isEqualTo(1);
	}

	@Test
	void shouldReceiveFifoWithRandomAttemptId() {
		String queue = "test-queue.fifo";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "false"));
		String messageGroupId = UUID.randomUUID().toString();
		String deduplicationId = UUID.randomUUID().toString();
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder()
				.messages(builder -> builder.messageId(UUID.randomUUID().toString())
						.attributes(Map.of(MessageSystemAttributeName.MESSAGE_GROUP_ID, messageGroupId,
								MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID, deduplicationId))
						.receiptHandle("test-receipt-handle").body(payload).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		Optional<Message<String>> receivedMessage = template.receive(from -> from.queue(queue), String.class);
		assertThat(receivedMessage).isPresent().hasValueSatisfying(message -> {
			assertThat(message.getPayload()).isEqualTo(payload);
			assertThat(message.getHeaders())
					.containsEntry(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, messageGroupId);
			assertThat(message.getHeaders()).containsEntry(
					SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, deduplicationId);
		});
		ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
		then(mockClient).should().receiveMessage(captor.capture());
		ReceiveMessageRequest request = captor.getValue();
		assertThat(request.receiveRequestAttemptId()).isNotNull();
		assertThat(request.maxNumberOfMessages()).isEqualTo(1);
	}

	@Test
	void shouldReceiveBatchWithDefaultValues() {
		String queue = "test-queue";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder()
				.messages(
						builder -> builder.messageId(UUID.randomUUID().toString())
								.receiptHandle("test-receipt-handle-1").body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString())
								.receiptHandle("test-receipt-handle-2").body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString())
								.receiptHandle("test-receipt-handle-3").body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString())
								.receiptHandle("test-receipt-handle-4").body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString())
								.receiptHandle("test-receipt-handle-5").body(payload).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient).configure(options -> options
				.defaultQueue(queue).defaultPollTimeout(Duration.ofSeconds(5)).defaultMaxNumberOfMessages(6))
				.buildSyncTemplate();
		Collection<Message<?>> receivedMessages = template.receiveMany();
		assertThat(receivedMessages).hasSize(5);
		ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
		then(mockClient).should().receiveMessage(captor.capture());
		ReceiveMessageRequest request = captor.getValue();
		assertThat(request.visibilityTimeout()).isNull();
		assertThat(request.queueUrl()).isEqualTo(queue);
		assertThat(request.maxNumberOfMessages()).isEqualTo(6);
		assertThat(request.waitTimeSeconds()).isEqualTo(5);
	}

	@Test
	void shouldReceiveBatchWithQueueAndPayload() {
		String queue = "test-queue";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());

		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder()
				.messages(
						builder -> builder.messageId(UUID.randomUUID().toString())
								.receiptHandle("test-receipt-handle-1").body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString())
								.receiptHandle("test-receipt-handle-2").body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString())
								.receiptHandle("test-receipt-handle-3").body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString())
								.receiptHandle("test-receipt-handle-4").body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString())
								.receiptHandle("test-receipt-handle-5").body(payload).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		int defaultPollTimeout = 5;
		int defaultMaxNumberOfMessages = 6;
		SqsAsyncOperations template = SqsTemplate.builder().sqsAsyncClient(mockClient)
				.configure(options -> options.defaultQueue(queue)
						.defaultPollTimeout(Duration.ofSeconds(defaultPollTimeout))
						.defaultMaxNumberOfMessages(defaultMaxNumberOfMessages))
				.buildAsyncTemplate();
		Collection<Message<String>> receivedMessages = template.receiveManyAsync(queue, String.class).join();
		assertThat(receivedMessages).hasSize(5);
		ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
		then(mockClient).should().receiveMessage(captor.capture());
		ReceiveMessageRequest request = captor.getValue();
		assertThat(request.visibilityTimeout()).isNull();
		assertThat(request.queueUrl()).isEqualTo(queue);
		assertThat(request.maxNumberOfMessages()).isEqualTo(defaultMaxNumberOfMessages);
		assertThat(request.waitTimeSeconds()).isEqualTo(defaultPollTimeout);
		assertThat(receivedMessages).hasSize(5).allSatisfy(message -> {
			assertThat(message.getPayload()).isEqualTo(payload);
		});
	}

	@Test
	void shouldReceiveBatchWithOptions() {
		String queue = "test-queue";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder()
				.messages(
						builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle")
								.body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle")
								.body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle")
								.body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle")
								.body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle")
								.body(payload).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		Collection<Message<String>> receivedMessages = template.receiveMany(from -> from.queue(queue)
				.maxNumberOfMessages(6).visibilityTimeout(Duration.ofSeconds(3)).pollTimeout(Duration.ofSeconds(5)),
				String.class);
		assertThat(receivedMessages).hasSize(5);
		ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
		then(mockClient).should().receiveMessage(captor.capture());
		ReceiveMessageRequest request = captor.getValue();
		assertThat(request.visibilityTimeout()).isEqualTo(3);
		assertThat(request.queueUrl()).isEqualTo(queue);
		assertThat(request.maxNumberOfMessages()).isEqualTo(6);
		assertThat(request.waitTimeSeconds()).isEqualTo(5);
	}

	@Test
	void shouldReceiveBatchFifo() {
		String queue = "test-queue";
		String payload = "test-payload";

		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder()
				.messages(
						builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle")
								.body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle")
								.body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle")
								.body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle")
								.body(payload).build(),
						builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle")
								.body(payload).build())
				.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse.builder()
				.successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations template = SqsTemplate.newSyncTemplate(mockClient);
		UUID attemptId = UUID.randomUUID();
		Collection<Message<?>> receivedMessages = template
				.receiveMany(from -> from.queue(queue).receiveRequestAttemptId(attemptId));
		assertThat(receivedMessages).hasSize(5);
		ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
		then(mockClient).should().receiveMessage(captor.capture());
		ReceiveMessageRequest request = captor.getValue();
		assertThat(request.receiveRequestAttemptId()).isEqualTo(attemptId.toString());
		assertThat(request.queueUrl()).isEqualTo(queue);

	}

	@Test
	void shouldPropagateTracingAsMessageSystemAttribute() {
		String queue = "test-queue";
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(GetQueueUrlRequest.class)))
			.willReturn(CompletableFuture.completedFuture(urlResponse));
		mockQueueAttributes(mockClient, Map.of());
		SendMessageResponse response = SendMessageResponse.builder().messageId(UUID.randomUUID().toString())
			.sequenceNumber("123").build();
		given(mockClient.sendMessage(any(SendMessageRequest.class)))
			.willReturn(CompletableFuture.completedFuture(response));

		SqsOperations sqsOperations = SqsTemplate.newSyncTemplate(mockClient);
		SendResult<Object> result = sqsOperations.send(options -> options
			.queue(queue)
			.header(SqsHeaders.MessageSystemAttributes.SQS_AWS_TRACE_HEADER, "abc")
			.payload("test")
		);

		assertThat(result).isNotNull();

		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest sendMessageRequest = captor.getValue();

		assertThat(sendMessageRequest.messageSystemAttributes()).hasEntrySatisfying(
			MessageSystemAttributeNameForSends.AWS_TRACE_HEADER,
			value -> assertThat(value.stringValue()).isEqualTo("abc")
		);
	}

}
