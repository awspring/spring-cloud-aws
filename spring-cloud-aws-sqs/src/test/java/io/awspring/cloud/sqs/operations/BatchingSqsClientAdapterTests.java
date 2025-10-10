/*
 * Copyright 2013-2024 the original author or authors.
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
import static org.mockito.Mockito.mock;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.*;

/**
 * @author Heechul Kang
 */
@ExtendWith(MockitoExtension.class)
class BatchingSqsClientAdapterTests {
	SqsAsyncBatchManager mockBatchManager;

	BatchingSqsClientAdapter mockAdapter;

	@BeforeEach
	void beforeEach() {
		mockBatchManager = mock(SqsAsyncBatchManager.class);
		mockAdapter = new BatchingSqsClientAdapter(mockBatchManager);
	}

	@Test
	void shouldThrowExceptionWhenBatchManagerIsNull() {
		assertThatThrownBy(() -> new BatchingSqsClientAdapter(null)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("batchManager cannot be null");
	}

	@Test
	void shouldReturnCorrectServiceName() {
		String serviceName = mockAdapter.serviceName();
		assertThat(serviceName).isEqualTo(SqsAsyncClient.SERVICE_NAME);
	}

	@Test
	void shouldDelegateBatchManagerClose() {
		mockAdapter.close();
		then(mockBatchManager).should().close();
	}

	@Test
	void shouldDelegateSendMessageWithRequest() {
		String queueUrl = "test-queue";
		String messageBody = "test-message";

		SendMessageRequest request = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody).build();
		SendMessageResponse expectedResponse = SendMessageResponse.builder().messageId(UUID.randomUUID().toString())
				.build();
		given(mockBatchManager.sendMessage(request)).willReturn(CompletableFuture.completedFuture(expectedResponse));

		CompletableFuture<SendMessageResponse> result = mockAdapter.sendMessage(request);

		assertThat(result).isCompletedWithValue(expectedResponse);
		then(mockBatchManager).should().sendMessage(request);
	}

	@Test
	void shouldDelegateSendMessageWithConsumer() {
		String queueUrl = "test-queue";
		String messageBody = "test-message";

		Consumer<SendMessageRequest.Builder> requestConsumer = builder -> builder.queueUrl(queueUrl)
				.messageBody(messageBody);
		SendMessageResponse expectedResponse = SendMessageResponse.builder().messageId(UUID.randomUUID().toString())
				.build();
		given(mockBatchManager.sendMessage(any(Consumer.class)))
				.willReturn(CompletableFuture.completedFuture(expectedResponse));

		CompletableFuture<SendMessageResponse> result = mockAdapter.sendMessage(requestConsumer);

		assertThat(result).isCompletedWithValue(expectedResponse);
		ArgumentCaptor<Consumer<SendMessageRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);
		then(mockBatchManager).should().sendMessage(captor.capture());
		assertThat(captor.getValue()).isEqualTo(requestConsumer);
	}

	@Test
	void shouldDelegateReceiveMessageWithRequest() {
		String queueUrl = "test-queue";
		String body = "test-body";

		ReceiveMessageRequest request = ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(10)
				.build();
		Message message = Message.builder().messageId(UUID.randomUUID().toString()).body(body).build();
		ReceiveMessageResponse expectedResponse = ReceiveMessageResponse.builder().messages(message).build();
		given(mockBatchManager.receiveMessage(request)).willReturn(CompletableFuture.completedFuture(expectedResponse));

		CompletableFuture<ReceiveMessageResponse> result = mockAdapter.receiveMessage(request);

		assertThat(result).isCompletedWithValue(expectedResponse);
		then(mockBatchManager).should().receiveMessage(request);
	}

	@Test
	void shouldDelegateReceiveMessageWithConsumer() {
		String queueUrl = "test-queue";
		String body = "test-body";

		Consumer<ReceiveMessageRequest.Builder> requestConsumer = builder -> builder.queueUrl(queueUrl)
				.maxNumberOfMessages(10);
		Message message = Message.builder().messageId(UUID.randomUUID().toString()).body(body).build();
		ReceiveMessageResponse expectedResponse = ReceiveMessageResponse.builder().messages(message).build();
		given(mockBatchManager.receiveMessage(any(Consumer.class)))
				.willReturn(CompletableFuture.completedFuture(expectedResponse));

		CompletableFuture<ReceiveMessageResponse> result = mockAdapter.receiveMessage(requestConsumer);

		assertThat(result).isCompletedWithValue(expectedResponse);
		ArgumentCaptor<Consumer<ReceiveMessageRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);
		then(mockBatchManager).should().receiveMessage(captor.capture());
		assertThat(captor.getValue()).isEqualTo(requestConsumer);
	}

	@Test
	void shouldDelegateDeleteMessageWithRequest() {
		String queueUrl = "test-queue";
		String receiptHandle = "test-receipt-handle";

		DeleteMessageRequest request = DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle)
				.build();
		DeleteMessageResponse expectedResponse = DeleteMessageResponse.builder().build();
		given(mockBatchManager.deleteMessage(request)).willReturn(CompletableFuture.completedFuture(expectedResponse));

		CompletableFuture<DeleteMessageResponse> result = mockAdapter.deleteMessage(request);

		assertThat(result).isCompletedWithValue(expectedResponse);
		then(mockBatchManager).should().deleteMessage(request);
	}

	@Test
	void shouldDelegateDeleteMessageWithConsumer() {
		String queueUrl = "test-queue";
		String receiptHandle = "test-receipt-handle";

		Consumer<DeleteMessageRequest.Builder> requestConsumer = builder -> builder.queueUrl(queueUrl)
				.receiptHandle(receiptHandle);
		DeleteMessageResponse expectedResponse = DeleteMessageResponse.builder().build();
		given(mockBatchManager.deleteMessage(any(Consumer.class)))
				.willReturn(CompletableFuture.completedFuture(expectedResponse));

		CompletableFuture<DeleteMessageResponse> result = mockAdapter.deleteMessage(requestConsumer);

		assertThat(result).isCompletedWithValue(expectedResponse);
		ArgumentCaptor<Consumer<DeleteMessageRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);
		then(mockBatchManager).should().deleteMessage(captor.capture());
		assertThat(captor.getValue()).isEqualTo(requestConsumer);
	}

	@Test
	void shouldDelegateChangeMessageVisibilityWithRequest() {
		String queueUrl = "test-queue";
		String receiptHandle = "test-receipt-handle";

		ChangeMessageVisibilityRequest request = ChangeMessageVisibilityRequest.builder().queueUrl(queueUrl)
				.receiptHandle(receiptHandle).visibilityTimeout(30).build();
		ChangeMessageVisibilityResponse expectedResponse = ChangeMessageVisibilityResponse.builder().build();
		given(mockBatchManager.changeMessageVisibility(request))
				.willReturn(CompletableFuture.completedFuture(expectedResponse));

		CompletableFuture<ChangeMessageVisibilityResponse> result = mockAdapter.changeMessageVisibility(request);

		assertThat(result).isCompletedWithValue(expectedResponse);
		then(mockBatchManager).should().changeMessageVisibility(request);
	}

	@Test
	void shouldDelegateChangeMessageVisibilityWithConsumer() {
		String queueUrl = "test-queue";
		String receiptHandle = "test-receipt-handle";

		Consumer<ChangeMessageVisibilityRequest.Builder> requestConsumer = builder -> builder.queueUrl(queueUrl)
				.receiptHandle(receiptHandle).visibilityTimeout(30);
		ChangeMessageVisibilityResponse expectedResponse = ChangeMessageVisibilityResponse.builder().build();
		given(mockBatchManager.changeMessageVisibility(any(Consumer.class)))
				.willReturn(CompletableFuture.completedFuture(expectedResponse));

		CompletableFuture<ChangeMessageVisibilityResponse> result = mockAdapter
				.changeMessageVisibility(requestConsumer);

		assertThat(result).isCompletedWithValue(expectedResponse);
		ArgumentCaptor<Consumer<ChangeMessageVisibilityRequest.Builder>> captor = ArgumentCaptor
				.forClass(Consumer.class);
		then(mockBatchManager).should().changeMessageVisibility(captor.capture());
		assertThat(captor.getValue()).isEqualTo(requestConsumer);
	}

	@Test
	void shouldHandleExceptionalCompletionInSendMessage() {
		String queueUrl = "test-queue";
		String body = "test-message";

		SendMessageRequest request = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(body).build();
		RuntimeException exception = new RuntimeException("Batch manager error");
		CompletableFuture<SendMessageResponse> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(exception);
		given(mockBatchManager.sendMessage(request)).willReturn(failedFuture);

		CompletableFuture<SendMessageResponse> result = mockAdapter.sendMessage(request);

		assertThat(result).isCompletedExceptionally();
		then(mockBatchManager).should().sendMessage(request);
	}

	@Test
	void shouldHandleExceptionalCompletionInReceiveMessage() {
		String queueUrl = "test-queue";

		ReceiveMessageRequest request = ReceiveMessageRequest.builder().queueUrl(queueUrl).build();
		RuntimeException exception = new RuntimeException("Batch manager error");
		CompletableFuture<ReceiveMessageResponse> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(exception);
		given(mockBatchManager.receiveMessage(request)).willReturn(failedFuture);

		CompletableFuture<ReceiveMessageResponse> result = mockAdapter.receiveMessage(request);

		assertThat(result).isCompletedExceptionally();
		then(mockBatchManager).should().receiveMessage(request);
	}

	@Test
	void shouldHandleExceptionalCompletionInDeleteMessage() {
		String queueUrl = "test-queue";
		String receiptHandle = "test-receipt-handle";

		DeleteMessageRequest request = DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle)
				.build();
		RuntimeException exception = new RuntimeException("Batch manager error");
		CompletableFuture<DeleteMessageResponse> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(exception);
		given(mockBatchManager.deleteMessage(request)).willReturn(failedFuture);

		CompletableFuture<DeleteMessageResponse> result = mockAdapter.deleteMessage(request);

		assertThat(result).isCompletedExceptionally();
		then(mockBatchManager).should().deleteMessage(request);
	}

	@Test
	void shouldHandleExceptionalCompletionInChangeMessageVisibility() {
		String queueUrl = "test-queue";
		String receiptHandle = "test-receipt-handle";

		ChangeMessageVisibilityRequest request = ChangeMessageVisibilityRequest.builder().queueUrl(queueUrl)
				.receiptHandle(receiptHandle).visibilityTimeout(30).build();
		RuntimeException exception = new RuntimeException("Batch manager error");
		CompletableFuture<ChangeMessageVisibilityResponse> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(exception);
		given(mockBatchManager.changeMessageVisibility(request)).willReturn(failedFuture);

		CompletableFuture<ChangeMessageVisibilityResponse> result = mockAdapter.changeMessageVisibility(request);

		assertThat(result).isCompletedExceptionally();
		then(mockBatchManager).should().changeMessageVisibility(request);
	}
}