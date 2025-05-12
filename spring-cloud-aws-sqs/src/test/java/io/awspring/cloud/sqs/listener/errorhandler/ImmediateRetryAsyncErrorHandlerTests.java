/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.listener.errorhandler;

import io.awspring.cloud.sqs.listener.BatchVisibility;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.Visibility;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Tests for {@link ImmediateRetryAsyncErrorHandler}.
 *
 * @author Bruno Garcia
 * @author Rafael Pavarini
 */
@SuppressWarnings("unchecked")
class ImmediateRetryAsyncErrorHandlerTests {

	@Test
	void shouldChangeVisibilityToZero() {
		Message<Object> message = mock(Message.class);
		RuntimeException exception = new RuntimeException("Expected exception from shouldChangeVisibilityToZero");
		MessageHeaders headers = mock(MessageHeaders.class);
		Visibility visibility = mock(Visibility.class);
		given(message.getHeaders()).willReturn(headers);
		given(headers.get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class)).willReturn(visibility);
		given(visibility.changeToAsync(0)).willReturn(CompletableFuture.completedFuture(null));

		ImmediateRetryAsyncErrorHandler<Object> handler = new ImmediateRetryAsyncErrorHandler<>();

		assertThat(handler.handle(message, exception)).isCompletedExceptionally();
		then(visibility).should().changeToAsync(0);
	}

	@Test
	void shouldReturnError() {
		Message<Object> message = mock(Message.class);
		RuntimeException exception = new RuntimeException("Expected exception from shouldReturnError");
		MessageHeaders headers = mock(MessageHeaders.class);
		given(headers.get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class)).willReturn(null);
		given(message.getHeaders()).willReturn(headers);

		ImmediateRetryAsyncErrorHandler<Object> handler = new ImmediateRetryAsyncErrorHandler<>();

		assertThatThrownBy(() -> handler.handle(message, exception))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("Header Sqs_VisibilityTimeout not found in message");
	}

	@Test
	void shouldChangeVisibilityToZeroBatch() {
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		RuntimeException exception = new RuntimeException("Expected exception from shouldChangeVisibilityToZeroBatch");
		MessageHeaders headers = mock(MessageHeaders.class);
		QueueMessageVisibility visibility = mock(QueueMessageVisibility.class);
		BatchVisibility batchvisibility = mock(BatchVisibility.class);
		given(batchvisibility.changeToAsync(0)).willReturn(CompletableFuture.completedFuture(null));
		given(visibility.toBatchVisibility(any())).willReturn(batchvisibility);
		given(headers.get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class)).willReturn(visibility);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);

		ImmediateRetryAsyncErrorHandler<Object> handler = new ImmediateRetryAsyncErrorHandler<>();

		assertThat(handler.handle(batch, exception)).isCompletedExceptionally();
		then(batchvisibility).should(times(1)).changeToAsync(0);
	}
}
