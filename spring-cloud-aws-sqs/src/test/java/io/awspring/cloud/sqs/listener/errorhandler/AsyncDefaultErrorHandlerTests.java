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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.listener.BatchVisibility;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.Visibility;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Tests for {@link AsyncDefaultErrorHandler}.
 *
 * @author Bruno Garcia
 * @author Rafael Pavarini
 */
@SuppressWarnings("unchecked")
class AsyncDefaultErrorHandlerTests {

	@Test
	void shouldChangeVisibilityToZero() {
		Message<Object> message = mock(Message.class);
		RuntimeException exception = new RuntimeException("Expected exception from shouldChangeVisibilityToZero");
		MessageHeaders headers = mock(MessageHeaders.class);
		Visibility visibility = mock(Visibility.class);
		when(message.getHeaders()).thenReturn(headers);
		when(headers.get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class)).thenReturn(visibility);
		when(visibility.changeToAsync(0)).thenReturn(CompletableFuture.completedFuture(null));

		AsyncDefaultErrorHandler<Object> handler = new AsyncDefaultErrorHandler<>();

		assertThat(handler.handle(message, exception)).isCompletedExceptionally();
		verify(visibility).changeToAsync(0);
	}

	@Test
	void shouldReturnError() {
		Message<Object> message = mock(Message.class);
		RuntimeException exception = new RuntimeException("Expected exception from shouldReturnError");
		MessageHeaders headers = mock(MessageHeaders.class);
		when(headers.get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class)).thenReturn(null);
		when(message.getHeaders()).thenReturn(headers);

		AsyncDefaultErrorHandler<Object> handler = new AsyncDefaultErrorHandler<>();

		assertThatThrownBy(() -> handler.handle(message, exception));
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
		when(batchvisibility.changeToAsync(0)).thenReturn(CompletableFuture.completedFuture(null));
		when(visibility.toBatchVisibility(any())).thenReturn(batchvisibility);
		when(headers.get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class)).thenReturn(visibility);
		when(message1.getHeaders()).thenReturn(headers);
		when(message2.getHeaders()).thenReturn(headers);
		when(message3.getHeaders()).thenReturn(headers);

		AsyncDefaultErrorHandler<Object> handler = new AsyncDefaultErrorHandler<>();

		assertThat(handler.handle(batch, exception)).isCompletedExceptionally();
		verify(batchvisibility, times(1)).changeToAsync(0);
	}
}
