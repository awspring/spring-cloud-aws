/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.kinesis.listener.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.kinesis.listener.ListenerExecutionFailedException;
import io.awspring.cloud.kinesis.listener.ReplyPublicationFailedException;
import io.awspring.cloud.kinesis.listener.errorhandler.BackOffErrorHandler;
import io.awspring.cloud.kinesis.listener.errorhandler.FailedDelivery;
import io.awspring.cloud.kinesis.operations.KinesisOperations;
import io.awspring.cloud.kinesis.operations.SendResult;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.backoff.FixedBackOff;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
class MessagingMessageListenerAdapterTests {

	private static final SendResult RESULT = new SendResult("seq", "shard");

	private final AtomicInteger handlerCalls = new AtomicInteger();

	private final Message<String> input = MessageBuilder.withPayload("in")
			.setHeader(KinesisMessageHeaders.PARTITION_KEY, "input-key").build();

	@Test
	@DisplayName("a payload reply is sent with the input partition key")
	void payloadReplyUsesInputKey() throws Exception {
		KinesisOperations operations = mock(KinesisOperations.class);
		adapter(operations, () -> "out").onMessage(this.input);

		verify(operations).send("reply", "input-key", (Object) "out");
	}

	@Test
	@DisplayName("a Message reply keeps its headers and falls back to the input partition key")
	void messageReplyFallsBackToInputKey() throws Exception {
		KinesisOperations operations = mock(KinesisOperations.class);
		Message<String> reply = MessageBuilder.withPayload("out").setHeader("custom", "v").build();
		adapter(operations, () -> reply).onMessage(this.input);

		ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
		verify(operations).send(eq("reply"), eq("input-key"), captor.capture());
		assertThat(captor.getValue().getHeaders()).containsEntry("custom", "v");
		assertThat(captor.getValue().getPayload()).isEqualTo("out");
	}

	@Test
	@DisplayName("a Message reply's partition key header wins over the input key")
	void messageReplyPartitionKeyHeaderWins() throws Exception {
		KinesisOperations operations = mock(KinesisOperations.class);
		Message<String> reply = MessageBuilder.withPayload("out")
				.setHeader(KinesisMessageHeaders.PARTITION_KEY, "reply-key").build();
		adapter(operations, () -> reply).onMessage(this.input);

		verify(operations).send(eq("reply"), eq("reply-key"), any(Message.class));
	}

	@Test
	@DisplayName("a failed reply is retried by resending the cached result without invoking the handler again")
	void replyOnlyRetry() throws Exception {
		KinesisOperations operations = mock(KinesisOperations.class);
		when(operations.send(eq("reply"), eq("input-key"), any(Object.class)))
				.thenThrow(new IllegalStateException("kinesis down")).thenThrow(new IllegalStateException("still down"))
				.thenReturn(RESULT);
		MessagingMessageListenerAdapter adapter = adapter(operations,
				() -> "out-" + this.handlerCalls.incrementAndGet());

		ReplyPublicationFailedException failure = null;
		try {
			adapter.onMessage(this.input);
		}
		catch (ReplyPublicationFailedException ex) {
			failure = ex;
		}
		assertThat(failure).isNotNull();

		FailedDelivery delivery = new FailedDelivery(this.input, failure, "in", "shard-1", failure::resend);
		new BackOffErrorHandler(new FixedBackOff(0, 3), (d, last) -> {
			throw new AssertionError("must recover by retrying");
		}).handle(delivery);

		assertThat(this.handlerCalls).hasValue(1);
		verify(operations, times(3)).send("reply", "input-key", (Object) "out-1");
	}

	@Test
	@DisplayName("a listener retry that reaches reply failure switches later attempts to reply-only retry")
	void listenerRetrySwitchesToReplyOnlyRetry() throws Exception {
		KinesisOperations operations = mock(KinesisOperations.class);
		when(operations.send(eq("reply"), eq("input-key"), any(Object.class)))
				.thenThrow(new IllegalStateException("kinesis down")).thenReturn(RESULT);
		AtomicInteger calls = new AtomicInteger();
		MessagingMessageListenerAdapter adapter = adapter(operations, () -> {
			if (calls.incrementAndGet() == 1) {
				throw new IllegalStateException("listener failed");
			}
			return "out";
		});

		Throwable original = catchThrowable(() -> adapter.onMessage(this.input));
		assertThat(original).isInstanceOf(ListenerExecutionFailedException.class)
				.isNotInstanceOf(ReplyPublicationFailedException.class);
		FailedDelivery delivery = new FailedDelivery(this.input, original, "in", "shard-1",
				() -> adapter.onMessage(this.input));

		new BackOffErrorHandler(new FixedBackOff(0, 3), (d, last) -> {
			throw new AssertionError("must recover by retrying");
		}).handle(delivery);

		assertThat(calls).hasValue(2);
		verify(operations, times(2)).send("reply", "input-key", (Object) "out");
	}

	@Test
	@DisplayName("a handler failure is not a reply failure")
	void handlerFailureIsNotReplyFailure() {
		KinesisOperations operations = mock(KinesisOperations.class);
		assertThatThrownBy(() -> adapter(operations, () -> {
			throw new IllegalArgumentException("boom");
		}).onMessage(this.input)).isNotInstanceOf(ReplyPublicationFailedException.class);
	}

	private MessagingMessageListenerAdapter adapter(KinesisOperations operations, Supplier<Object> handler)
			throws Exception {
		Handler bean = new Handler(handler);
		InvocableHandlerMethod method = new InvocableHandlerMethod(bean, Handler.class.getDeclaredMethod("handle"));
		return new MessagingMessageListenerAdapter(method, operations, "reply");
	}

	public static final class Handler {

		private final Supplier<Object> supplier;

		Handler(Supplier<Object> supplier) {
			this.supplier = supplier;
		}

		public Object handle() {
			return this.supplier.get();
		}

	}

}
