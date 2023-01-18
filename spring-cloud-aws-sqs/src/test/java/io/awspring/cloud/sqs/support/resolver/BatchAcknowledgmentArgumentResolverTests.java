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
package io.awspring.cloud.sqs.support.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.BatchAcknowledgement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * Tests for {@link BatchAcknowledgmentArgumentResolver}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings("unchecked")
class BatchAcknowledgmentArgumentResolverTests {

	@Test
	void shouldConvertAndAcknowledge() throws Exception {
		AcknowledgementCallback<Object> callback = mock(AcknowledgementCallback.class);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = new MessageHeaders(getMessageHeaders(callback));
		Message<Collection<Message<Object>>> rootMessage = mock(Message.class);
		given(rootMessage.getPayload()).willReturn(batch);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(callback.onAcknowledge(any(Collection.class))).willReturn(CompletableFuture.completedFuture(null));
		HandlerMethodArgumentResolver resolver = new BatchAcknowledgmentArgumentResolver();
		Object result = resolver.resolveArgument(null, rootMessage);
		assertThat(result).isNotNull().isInstanceOf(BatchAcknowledgement.class);
		((BatchAcknowledgement<Object>) result).acknowledge();
		then(callback).should().onAcknowledge(batch);
	}

	@Test
	void shouldConvertAndAcknowledgePartialBatch() throws Exception {
		AcknowledgementCallback<Object> callback = mock(AcknowledgementCallback.class);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = new MessageHeaders(getMessageHeaders(callback));
		Message<Collection<Message<Object>>> rootMessage = mock(Message.class);
		given(rootMessage.getPayload()).willReturn(batch);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(callback.onAcknowledge(any(Collection.class))).willReturn(CompletableFuture.completedFuture(null));
		HandlerMethodArgumentResolver resolver = new BatchAcknowledgmentArgumentResolver();
		Object result = resolver.resolveArgument(null, rootMessage);
		assertThat(result).isNotNull().isInstanceOf(BatchAcknowledgement.class);
		List<Message<Object>> messagesToAcknowledge = Collections.singletonList(message2);
		((BatchAcknowledgement<Object>) result).acknowledge(messagesToAcknowledge);
		then(callback).should().onAcknowledge(messagesToAcknowledge);
	}

	@Test
	void shouldConvertAndAcknowledgeAsync() throws Exception {
		AcknowledgementCallback<Object> callback = mock(AcknowledgementCallback.class);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = new MessageHeaders(getMessageHeaders(callback));
		Message<Collection<Message<Object>>> rootMessage = mock(Message.class);
		given(rootMessage.getPayload()).willReturn(batch);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(callback.onAcknowledge(any(Collection.class))).willReturn(CompletableFuture.completedFuture(null));
		HandlerMethodArgumentResolver resolver = new BatchAcknowledgmentArgumentResolver();
		Object result = resolver.resolveArgument(null, rootMessage);
		assertThat(result).isNotNull().isInstanceOf(BatchAcknowledgement.class);
		((BatchAcknowledgement<Object>) result).acknowledgeAsync().join();
		then(callback).should().onAcknowledge(batch);
	}

	@Test
	void shouldConvertAndAcknowledgePartialBatchAsync() throws Exception {
		AcknowledgementCallback<Object> callback = mock(AcknowledgementCallback.class);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = getMessageHeaders(callback);
		Message<Collection<Message<Object>>> rootMessage = mock(Message.class);
		given(rootMessage.getPayload()).willReturn(batch);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(callback.onAcknowledge(any(Collection.class))).willReturn(CompletableFuture.completedFuture(null));
		HandlerMethodArgumentResolver resolver = new BatchAcknowledgmentArgumentResolver();
		Object result = resolver.resolveArgument(null, rootMessage);
		assertThat(result).isNotNull().isInstanceOf(BatchAcknowledgement.class);
		List<Message<Object>> messagesToAcknowledge = Collections.singletonList(message2);
		((BatchAcknowledgement<Object>) result).acknowledgeAsync(messagesToAcknowledge).join();
		then(callback).should().onAcknowledge(messagesToAcknowledge);
	}

	@NotNull
	private MessageHeaders getMessageHeaders(AcknowledgementCallback<Object> callback) {
		return new MessageHeaders(Collections.singletonMap(SqsHeaders.SQS_ACKNOWLEDGMENT_CALLBACK_HEADER, callback));
	}

}
