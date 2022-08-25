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
package io.awspring.cloud.sqs.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.sqs.CompletableFutures;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class ListenerExecutionFailedExceptionTests {

	@Test
	void shouldUnwrapMessage() {
		Throwable throwable = mock(Throwable.class);
		Message<Object> message = mock(Message.class);
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException("Expected error",
				throwable, message);
		assertThat(listenerException).extracting(ListenerExecutionFailedException::getFailedMessage).isEqualTo(message);
		assertThat(ListenerExecutionFailedException.unwrapMessage(listenerException)).isEqualTo(message);
	}

	@Test
	void shouldUnwrapNestedMessage() {
		Throwable throwable = mock(Throwable.class);
		Message<Object> message = mock(Message.class);
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException("Expected error",
				throwable, message);
		RuntimeException oneMoreException = new RuntimeException(listenerException);
		CompletableFuture<Object> failedFuture = CompletableFutures.failedFuture(oneMoreException);
		assertThatThrownBy(failedFuture::join).extracting(ListenerExecutionFailedException::unwrapMessage)
				.isEqualTo(message);
	}

	@Test
	void shouldThrowIfMoreThanOneMessage() {
		Throwable throwable = mock(Throwable.class);
		Message<Object> message = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message, message, message);
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException("Expected error",
				throwable, messages);
		assertThatThrownBy(listenerException::getFailedMessage).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldUnwrapMessages() {
		Throwable throwable = mock(Throwable.class);
		Message<Object> message = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message, message, message);
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException("Expected error",
				throwable, messages);
		assertThat(listenerException).extracting(ListenerExecutionFailedException::getFailedMessages)
				.isEqualTo(messages);
		assertThat(ListenerExecutionFailedException.unwrapMessages(listenerException)).isEqualTo(messages);
	}

	@Test
	void shouldUnwrapNestedMessages() {
		Throwable throwable = mock(Throwable.class);
		Message<Object> message = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message, message, message);
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException("Expected error",
				throwable, messages);
		RuntimeException oneMoreException = new RuntimeException(listenerException);
		CompletableFuture<Object> failedFuture = CompletableFutures.failedFuture(oneMoreException);
		assertThatThrownBy(failedFuture::join).extracting(ListenerExecutionFailedException::unwrapMessages)
				.isEqualTo(messages);
	}

	@Test
	void shouldThrowIfListenerExceptionNotFoundForMessage() {
		Throwable throwable = mock(Throwable.class);
		RuntimeException oneMoreException = new RuntimeException(throwable);
		assertThatThrownBy(() -> ListenerExecutionFailedException.unwrapMessage(oneMoreException))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldThrowIfListenerExceptionNotFoundForMessages() {
		Throwable throwable = mock(Throwable.class);
		RuntimeException oneMoreException = new RuntimeException(throwable);
		assertThatThrownBy(() -> ListenerExecutionFailedException.unwrapMessages(oneMoreException))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldFindListenerException() {
		Throwable throwable = mock(Throwable.class);
		Message<Object> message = mock(Message.class);
		ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException("Expected error",
				throwable, message);
		RuntimeException oneMoreException = new RuntimeException(listenerException);
		assertThat(ListenerExecutionFailedException.hasListenerException(oneMoreException)).isTrue();
	}

	@Test
	void shouldNotFindListenerException() {
		Throwable throwable = mock(Throwable.class);
		RuntimeException oneMoreException = new RuntimeException(throwable);
		assertThat(ListenerExecutionFailedException.hasListenerException(oneMoreException)).isFalse();
	}

}
