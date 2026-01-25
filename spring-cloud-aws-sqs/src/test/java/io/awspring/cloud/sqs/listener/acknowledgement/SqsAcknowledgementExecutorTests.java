/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sqs.listener.acknowledgement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.SqsAcknowledgementException;
import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;

/**
 * Tests for {@link SqsAcknowledgementExecutor}.
 *
 * @author Tomaz Fernandes
 */
@ExtendWith(MockitoExtension.class)
class SqsAcknowledgementExecutorTests {

	@Mock
	SqsAsyncClient sqsAsyncClient;

	@Mock
	QueueAttributes queueAttributes;

	@Mock
	Message<String> message;

	String queueName = "sqsAcknowledgementExecutorTestsQueueName";

	String queueUrl = "sqsAcknowledgementExecutorTestsQueueUrl";

	String receiptHandle = "sqsAcknowledgementExecutorTestsQueueReceiptHandle";

	MessageHeaders messageHeaders = new MessageHeaders(
			Collections.singletonMap(SqsHeaders.SQS_RECEIPT_HANDLE_HEADER, receiptHandle));

	@Test
	void shouldDeleteMessages() throws Exception {
		Collection<Message<String>> messages = Collections.singletonList(message);
		given(message.getHeaders()).willReturn(messageHeaders);
		given(queueAttributes.getQueueName()).willReturn(queueName);
		given(queueAttributes.getQueueUrl()).willReturn(queueUrl);
		given(sqsAsyncClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFuture.completedFuture(DeleteMessageBatchResponse.builder().build()));

		SqsAcknowledgementExecutor<String> executor = new SqsAcknowledgementExecutor<>();
		executor.setSqsAsyncClient(sqsAsyncClient);
		executor.setQueueAttributes(queueAttributes);
		executor.execute(messages).get();

		ArgumentCaptor<DeleteMessageBatchRequest> requestCaptor = ArgumentCaptor
				.forClass(DeleteMessageBatchRequest.class);
		verify(sqsAsyncClient).deleteMessageBatch(requestCaptor.capture());
		DeleteMessageBatchRequest request = requestCaptor.getValue();
		assertThat(request.queueUrl()).isEqualTo(queueUrl);
		DeleteMessageBatchRequestEntry entry = request.entries().get(0);
		assertThat(entry.receiptHandle()).isEqualTo(receiptHandle);
	}

	@Test
	void shouldWrapDeletionErrors() {
		IllegalArgumentException exception = new IllegalArgumentException(
				"Expected exception from shouldWrapDeletionErrors");
		Collection<Message<String>> messages = Collections.singletonList(message);
		given(message.getHeaders()).willReturn(messageHeaders);
		given(queueAttributes.getQueueName()).willReturn(queueName);
		given(queueAttributes.getQueueUrl()).willReturn(queueUrl);
		given(sqsAsyncClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class)))
				.willReturn(CompletableFutures.failedFuture(exception));
		SqsAcknowledgementExecutor<String> executor = new SqsAcknowledgementExecutor<>();
		executor.setSqsAsyncClient(sqsAsyncClient);
		executor.setQueueAttributes(queueAttributes);
		assertThatThrownBy(() -> executor.execute(messages).join()).isInstanceOf(CompletionException.class).getCause()
				.isInstanceOf(SqsAcknowledgementException.class).asInstanceOf(type(SqsAcknowledgementException.class))
				.extracting(SqsAcknowledgementException::getFailedAcknowledgementMessages).asList()
				.containsExactly(message);
	}

	@Test
	void shouldWrapIfErrorIsThrown() {
		IllegalArgumentException exception = new IllegalArgumentException(
				"Expected exception from shouldWrapIfErrorIsThrown");
		Collection<Message<String>> messages = Collections.singletonList(message);
		given(message.getHeaders()).willReturn(messageHeaders);
		given(queueAttributes.getQueueName()).willReturn(queueName);
		given(queueAttributes.getQueueUrl()).willReturn(queueUrl);
		given(sqsAsyncClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).willThrow(exception);

		SqsAcknowledgementExecutor<String> executor = new SqsAcknowledgementExecutor<>();
		executor.setSqsAsyncClient(sqsAsyncClient);
		executor.setQueueAttributes(queueAttributes);
		assertThatThrownBy(() -> executor.execute(messages).join()).isInstanceOf(CompletionException.class).getCause()
				.isInstanceOf(SqsAcknowledgementException.class).asInstanceOf(type(SqsAcknowledgementException.class))
				.extracting(SqsAcknowledgementException::getQueue).isEqualTo(queueUrl);
	}

}
