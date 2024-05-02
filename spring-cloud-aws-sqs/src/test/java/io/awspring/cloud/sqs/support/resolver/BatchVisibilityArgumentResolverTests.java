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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.sqs.listener.BatchVisibility;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.Visibility;
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
 * Tests for {@link BatchVisibilityHandlerMethodArgumentResolver}.
 *
 * @author Clement Denis
 */
@SuppressWarnings("unchecked")
class BatchVisibilityArgumentResolverTests {

	@Test
	void shouldConvertAndAcknowledge() throws Exception {
		QueueMessageVisibility visibility = mock(QueueMessageVisibility.class);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = new MessageHeaders(getMessageHeaders(visibility));
		Message<Collection<Message<Object>>> rootMessage = mock(Message.class);
		given(rootMessage.getPayload()).willReturn(batch);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(visibility.changeToAsyncBatch(anyInt(), anyList())).willReturn(CompletableFuture.completedFuture(null));
		HandlerMethodArgumentResolver resolver = new BatchVisibilityHandlerMethodArgumentResolver();
		Object result = resolver.resolveArgument(null, rootMessage);
		assertThat(result).isNotNull().isInstanceOf(BatchVisibility.class);
		((BatchVisibility<Object>) result).changeTo(10);
		then(visibility).should().changeToAsyncBatch(10, batch);
	}

	@Test
	void shouldConvertAndAcknowledgePartialBatch() throws Exception {
		QueueMessageVisibility visibility = mock(QueueMessageVisibility.class);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = new MessageHeaders(getMessageHeaders(visibility));
		Message<Collection<Message<Object>>> rootMessage = mock(Message.class);
		given(rootMessage.getPayload()).willReturn(batch);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(visibility.changeToAsyncBatch(anyInt(), anyList())).willReturn(CompletableFuture.completedFuture(null));
		HandlerMethodArgumentResolver resolver = new BatchVisibilityHandlerMethodArgumentResolver();
		Object result = resolver.resolveArgument(null, rootMessage);
		assertThat(result).isNotNull().isInstanceOf(BatchVisibility.class);
		List<Message<Object>> changeVisibilityOn = Collections.singletonList(message1);
		((BatchVisibility<Object>) result).changeTo(changeVisibilityOn, 10);
		then(visibility).should().changeToAsyncBatch(10, changeVisibilityOn);
	}

	@Test
	void shouldConvertAndAcknowledgeAsync() throws Exception {
		QueueMessageVisibility visibility = mock(QueueMessageVisibility.class);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = new MessageHeaders(getMessageHeaders(visibility));
		Message<Collection<Message<Object>>> rootMessage = mock(Message.class);
		given(rootMessage.getPayload()).willReturn(batch);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(visibility.changeToAsyncBatch(anyInt(), anyList())).willReturn(CompletableFuture.completedFuture(null));
		HandlerMethodArgumentResolver resolver = new BatchVisibilityHandlerMethodArgumentResolver();
		Object result = resolver.resolveArgument(null, rootMessage);
		assertThat(result).isNotNull().isInstanceOf(BatchVisibility.class);
		((BatchVisibility<Object>) result).changeToAsync(10);
		then(visibility).should().changeToAsyncBatch(10, batch);
	}

	@Test
	void shouldConvertAndAcknowledgePartialBatchAsync() throws Exception {
		QueueMessageVisibility visibility = mock(QueueMessageVisibility.class);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		MessageHeaders headers = new MessageHeaders(getMessageHeaders(visibility));
		Message<Collection<Message<Object>>> rootMessage = mock(Message.class);
		given(rootMessage.getPayload()).willReturn(batch);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(visibility.changeToAsyncBatch(anyInt(), anyList())).willReturn(CompletableFuture.completedFuture(null));
		HandlerMethodArgumentResolver resolver = new BatchVisibilityHandlerMethodArgumentResolver();
		Object result = resolver.resolveArgument(null, rootMessage);
		assertThat(result).isNotNull().isInstanceOf(BatchVisibility.class);
		List<Message<Object>> changeVisibilityOn = Collections.singletonList(message1);
		((BatchVisibility<Object>) result).changeToAsync(changeVisibilityOn, 10);
		then(visibility).should().changeToAsyncBatch(10, changeVisibilityOn);
	}

	@NotNull
	private MessageHeaders getMessageHeaders(Visibility visibility) {
		return new MessageHeaders(Collections.singletonMap(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, visibility));
	}

}
