package io.awspring.cloud.sqs.operations;


import io.awspring.cloud.sqs.QueueAttributesResolvingException;
import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
class SqsTemplateTests {

	@Test
	void shouldSendWithOptions() {
		String queue = "test-queue";
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString()).sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class))).willReturn(CompletableFuture.completedFuture(response));

		SqsOperations<Object> template = SqsTemplate.newTemplate(mockClient);
		String payload = "test-payload";
		String testHeaderName = "testHeaderName";
		String testHeaderValue = "testHeaderValue";
		String testHeaderName2 = "testHeaderName2";
		String testHeaderValue2 = "testHeaderValue2";
		Integer delaySeconds = 5;
		SendResult<Object> result = template.send(to -> to
			.delaySeconds(delaySeconds)
			.queue(queue)
			.payload(payload)
			.header(testHeaderName, testHeaderValue)
			.headers(Map.of(testHeaderName2, testHeaderValue2)));
		assertThat(result.endpoint()).isEqualTo(queue);
		assertThat(result.message().getHeaders())
			.containsKeys(testHeaderName, testHeaderName2)
			.containsValues(testHeaderValue, testHeaderValue2);
		assertThat(result.message().getPayload()).isEqualTo(payload);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.delaySeconds()).isEqualTo(delaySeconds);
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		assertThat(capturedRequest.messageBody()).isEqualTo(payload);
		assertThat(capturedRequest.messageAttributes())
			.containsKeys(testHeaderName, testHeaderName2);
	}

	@Test
	void shouldSendFifoWithOptions() {
		String queue = "test-queue";
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString()).sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class))).willReturn(CompletableFuture.completedFuture(response));

		UUID messageGroupId = UUID.randomUUID();
		UUID messageDeduplicationId = UUID.randomUUID();

		SqsOperations<Object> template = SqsTemplate.newTemplate(mockClient);
		String payload = "test-payload";
		SendResult<Object> result = template.sendFifo(to -> to
			.queue(queue)
			.messageGroupId(messageGroupId)
			.messageDeduplicationId(messageDeduplicationId)
			.payload(payload));
		assertThat(result.endpoint()).isEqualTo(queue);
		assertThat(result.message().getHeaders())
			.containsAllEntriesOf(Map.of(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, messageDeduplicationId.toString(),
				SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, messageGroupId.toString()));
		assertThat(result.message().getPayload()).isEqualTo(payload);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
		assertThat(capturedRequest.messageBody()).isEqualTo(payload);
	}

	@Test
	void shouldSendWithDefaultEndpoint() {
		String queue = "test-queue";
		String payload = "test-payload";
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString()).sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class))).willReturn(CompletableFuture.completedFuture(response));
		SqsOperations<Object> template = SqsTemplate
			.builder()
			.sqsAsyncClient(mockClient)
			.configure(options -> options.defaultEndpointName(queue))
			.buildSyncTemplate();
		SendResult<Object> result = template.send(payload);
		assertThat(result.endpoint()).isEqualTo(queue);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		then(mockClient).should().sendMessage(captor.capture());
		SendMessageRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.queueUrl()).isEqualTo(queue);
	}

	@Test
	void shouldSendWithQueueAndPayload() {
		String queue = "test-queue";
		String payload = "test-payload";
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString()).sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class))).willReturn(CompletableFuture.completedFuture(response));
		SqsOperations<Object> template = SqsTemplate.newSyncTemplate(mockClient);
		SendResult<Object> result = template.send(queue, payload);
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
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString()).sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class))).willReturn(CompletableFuture.completedFuture(response));
		SqsOperations<String> template = SqsTemplate.newSyncTemplate(mockClient);
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
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		SendMessageBatchResponse response = SendMessageBatchResponse.builder()
			.successful(builder -> builder.id(message1.getHeaders().getId().toString()).messageId(UUID.randomUUID().toString()),
				builder -> builder.id(message2.getHeaders().getId().toString()).messageId(UUID.randomUUID().toString()))
			.build();
		given(mockClient.sendMessageBatch(any(SendMessageBatchRequest.class))).willReturn(CompletableFuture.completedFuture(response));
		SqsOperations<String> template = SqsTemplate.newSyncTemplate(mockClient);
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
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		String testErrorMessage = "test error message";
		String code = "BC01";
		boolean senderFault = true;
		SendMessageBatchResponse response = SendMessageBatchResponse.builder()
			.successful(builder -> builder.id(message1.getHeaders().getId().toString()).messageId(UUID.randomUUID().toString()))
			.failed(builder -> builder.id(message2.getHeaders().getId().toString()).message(testErrorMessage).code(code).senderFault(senderFault))
			.build();
		given(mockClient.sendMessageBatch(any(SendMessageBatchRequest.class))).willReturn(CompletableFuture.completedFuture(response));
		SqsOperations<String> template = SqsTemplate.<String>builder().configure(options -> options.sendBatchFailureStrategy(SendBatchFailureStrategy.RETURN_RESULT))
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
		assertThat(failedResult.additionalInformation().get(SqsTemplateParameters.CODE_PARAMETER_NAME)).isEqualTo(code);
		assertThat(failedResult.additionalInformation().get(SqsTemplateParameters.SENDER_FAULT_PARAMETER_NAME)).isEqualTo(senderFault);
	}

	@Test
	void shouldThrowIfHasFailedMessagesInBatchByDefault() {
		String queue = "test-queue";
		String payload1 = "test-payload-1";
		String payload2 = "test-payload-2";
		Message<String> message1 = MessageBuilder.withPayload(payload1).build();
		Message<String> message2 = MessageBuilder.withPayload(payload2).build();
		List<Message<String>> messages = List.of(message1, message2);
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		String testErrorMessage = "test error message";
		String code = "BC01";
		boolean senderFault = true;
		SendMessageBatchResponse response = SendMessageBatchResponse.builder()
			.successful(builder -> builder.id(message1.getHeaders().getId().toString()).messageId(UUID.randomUUID().toString()))
			.failed(builder -> builder.id(message2.getHeaders().getId().toString()).message(testErrorMessage).code(code).senderFault(senderFault))
			.build();
		given(mockClient.sendMessageBatch(any(SendMessageBatchRequest.class))).willReturn(CompletableFuture.completedFuture(response));
		SqsOperations<String> template = SqsTemplate.newSyncTemplate(mockClient);
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
				assertThat(failedResult.additionalInformation().get(SqsTemplateParameters.CODE_PARAMETER_NAME)).isEqualTo(code);
				assertThat(failedResult.additionalInformation().get(SqsTemplateParameters.SENDER_FAULT_PARAMETER_NAME)).isEqualTo(senderFault);
			});
	}

	@Test
	void shouldCreateByDefaultIfQueueNotFound() {
		String queue = "test-queue";
		String payload = "test-payload";
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.failedFuture(QueueDoesNotExistException.builder().message("test queue not found").build()));
		given(mockClient.createQueue(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(CreateQueueResponse.builder().queueUrl(queue).build()));
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString()).sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class))).willReturn(CompletableFuture.completedFuture(response));
		SqsOperations<Object> template = SqsTemplate.newSyncTemplate(mockClient);
		SendResult<Object> result = template.send(to -> to.queue(queue).payload(payload));
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
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.failedFuture(QueueDoesNotExistException.builder().message("test queue not found").build()));
		UUID uuid = UUID.randomUUID();
		String sequenceNumber = "1234";
		SendMessageResponse response = SendMessageResponse.builder().messageId(uuid.toString()).sequenceNumber(sequenceNumber).build();
		given(mockClient.sendMessage(any(SendMessageRequest.class))).willReturn(CompletableFuture.completedFuture(response));
		SqsOperations<Object> template = SqsTemplate.builder()
			.sqsAsyncClient(mockClient)
			.configure(options -> options.queueNotFoundStrategy(QueueNotFoundStrategy.FAIL))
			.buildSyncTemplate();
		assertThatThrownBy(() -> template.send(to -> to.queue(queue).payload(payload)))
			.isInstanceOf(CompletionException.class)
			.cause()
			.isInstanceOf(MessagingOperationFailedException.class)
			.cause()
			.isInstanceOf(CompletionException.class)
			.cause()
			.isInstanceOf(QueueAttributesResolvingException.class)
			.cause()
			.isInstanceOf(QueueDoesNotExistException.class);
	}

	@Test
	void shouldReceiveEmpty() {
		String queue = "test-queue";
		String payload = "test-payload";
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse
			.builder()
			.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		SqsOperations<Object> template = SqsTemplate
			.builder()
			.sqsAsyncClient(mockClient)
			.configure(options -> options.defaultEndpointName(queue))
			.buildSyncTemplate();
		Optional<Message<Object>> receivedMessage = template.receive();
		assertThat(receivedMessage).isEmpty();
	}

	@Test
	void shouldReceiveFromDefaultEndpoint() {
		String queue = "test-queue";
		String payload = "test-payload";
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse
			.builder()
			.messages(builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build())
			.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse
			.builder().successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations<Object> template = SqsTemplate
			.builder()
			.sqsAsyncClient(mockClient)
			.configure(options -> options.defaultEndpointName(queue))
			.buildSyncTemplate();
		Optional<Message<Object>> receivedMessage = template.receive();
		assertThat(receivedMessage).isPresent().hasValueSatisfying(message -> assertThat(message.getPayload()).isEqualTo(payload));
	}

	@Test
	void shouldReceiveAndNotAcknowledge() {
		String queue = "test-queue";
		String payload = "test-payload";
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse
			.builder()
			.messages(builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build())
			.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		SqsOperations<Object> template = SqsTemplate
			.builder()
			.sqsAsyncClient(mockClient)
			.configure(options -> options.defaultEndpointName(queue)
				.acknowledgementMode(TemplateAcknowledgementMode.DO_NOT_ACKNOWLEDGE))
			.buildSyncTemplate();
		Optional<Message<Object>> receivedMessage = template.receive();
		assertThat(receivedMessage).isPresent().hasValueSatisfying(message -> assertThat(message.getPayload()).isEqualTo(payload));
		then(mockClient).should(never()).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
	}

	@Test
	void shouldReceiveFromDefaultSettings() {
		String queue = "test-queue";
		String payload = "test-payload";
		String headerName1 = "headerName";
		String headerValue1 = "headerValue";
		String headerName2 = "headerName2";
		String headerValue2 = "headerValue2";
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse
			.builder()
			.messages(builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build())
			.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse
			.builder().successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations<String> template = SqsTemplate
			.<String>builder()
			.sqsAsyncClient(mockClient)
			.configure(options -> options.defaultEndpointName(queue)
				.defaultPollTimeout(Duration.ofSeconds(1))
				.additionalHeaderForReceive(headerName1, headerValue1)
				.additionalHeadersForReceive(Map.of(headerName2, headerValue2)))
			.buildSyncTemplate();
		Optional<Message<String>> receivedMessage = template.receive(null, null, null, null);
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
	}

	@Test
	void shouldReceiveFromOptions() {
		String queue = "test-queue";
		String payload = "test-payload";
		String headerName1 = "headerName";
		String headerValue1 = "headerValue";
		String headerName2 = "headerName2";
		String headerValue2 = "headerValue2";
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse
			.builder()
			.messages(builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build())
			.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse
			.builder().successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations<String> template = SqsTemplate.newSyncTemplate(mockClient);
		Optional<Message<String>> receivedMessage = template.receive(from -> from
				.queue(queue)
				.pollTimeout(Duration.ofSeconds(1))
				.visibilityTimeout(Duration.ofSeconds(5))
				.additionalHeader(headerName1, headerValue1)
				.additionalHeaders(Map.of(headerName2, headerValue2)));
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
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		String messageGroupId = UUID.randomUUID().toString();
		String deduplicationId = UUID.randomUUID().toString();
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse
			.builder()
			.messages(builder -> builder.messageId(UUID.randomUUID().toString())
				.attributes(Map.of(MessageSystemAttributeName.MESSAGE_GROUP_ID, messageGroupId,
					MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID, deduplicationId))
				.receiptHandle("test-receipt-handle").body(payload).build())
			.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse
			.builder().successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations<String> template = SqsTemplate.newSyncTemplate(mockClient);
		UUID attemptId = UUID.randomUUID();
		Optional<Message<String>> receivedMessage = template.receiveFifo(from -> from
				.queue(queue).receiveRequestAttemptId(attemptId));
		assertThat(receivedMessage).isPresent().hasValueSatisfying(message -> {
			assertThat(message.getPayload()).isEqualTo(payload);
			assertThat(message.getHeaders()).containsEntry(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, messageGroupId);
			assertThat(message.getHeaders()).containsEntry(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, deduplicationId);
		});
		ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
		then(mockClient).should().receiveMessage(captor.capture());
		ReceiveMessageRequest request = captor.getValue();
		assertThat(request.receiveRequestAttemptId()).isEqualTo(attemptId.toString());
		assertThat(request.maxNumberOfMessages()).isEqualTo(1);
	}

	@Test
	void shouldReceiveBatchWithDefaultValues() {
		String queue = "test-queue";
		String payload = "test-payload";
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse
			.builder()
			.messages(builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build(),
				builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build(),
				builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build(),
				builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build(),
				builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build())
			.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse
			.builder().successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations<Object> template = SqsTemplate
			.builder()
			.sqsAsyncClient(mockClient)
			.configure(options -> options.defaultEndpointName(queue)
				.defaultPollTimeout(Duration.ofSeconds(5))
				.defaultMaxNumberOfMessages(6))
			.buildSyncTemplate();
		Collection<Message<Object>> receivedMessages = template.receiveMany();
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
	void shouldReceiveBatchWithOptions() {
		String queue = "test-queue";
		String payload = "test-payload";
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse
			.builder()
			.messages(builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build(),
				builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build(),
				builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build(),
				builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build(),
				builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build())
			.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse
			.builder().successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations<Object> template = SqsTemplate.newSyncTemplate(mockClient);
		Collection<Message<Object>> receivedMessages = template.receiveMany(from -> from
			.queue(queue)
			.maxNumberOfMessages(6)
			.visibilityTimeout(Duration.ofSeconds(3))
			.pollTimeout(Duration.ofSeconds(5))
		);
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
		SqsAsyncClient mockClient = mock(SqsAsyncClient.class);
		GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queue).build();
		given(mockClient.getQueueUrl(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(urlResponse));
		ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse
			.builder()
			.messages(builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build(),
				builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build(),
				builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build(),
				builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build(),
				builder -> builder.messageId(UUID.randomUUID().toString()).receiptHandle("test-receipt-handle").body(payload).build())
			.build();
		given(mockClient.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(CompletableFuture.completedFuture(receiveMessageResponse));
		DeleteMessageBatchResponse deleteResponse = DeleteMessageBatchResponse
			.builder().successful(builder -> builder.id(UUID.randomUUID().toString())).build();
		given(mockClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).willReturn(CompletableFuture.completedFuture(deleteResponse));
		SqsOperations<Object> template = SqsTemplate.newSyncTemplate(mockClient);
		UUID attemptId = UUID.randomUUID();
		Collection<Message<Object>> receivedMessages = template.receiveManyFifo(from -> from
			.queue(queue)
			.receiveRequestAttemptId(attemptId)
		);
		assertThat(receivedMessages).hasSize(5);
		ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
		then(mockClient).should().receiveMessage(captor.capture());
		ReceiveMessageRequest request = captor.getValue();
		assertThat(request.receiveRequestAttemptId()).isEqualTo(attemptId.toString());
		assertThat(request.queueUrl()).isEqualTo(queue);
	}

}
