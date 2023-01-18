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
package io.awspring.cloud.sqs.listener.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

/**
 * Tests for {@link AsyncMessageInterceptor}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings("unchecked")
class AsyncMessageInterceptorTests {

	@Test
	void shouldReturnMessage() {
		Message<Object> message = mock(Message.class);
		AsyncMessageInterceptor<Object> interceptor = new AsyncMessageInterceptor<Object>() {
		};
		CompletableFuture<Message<Object>> future = interceptor.intercept(message);
		assertThat(future).isCompletedWithValue(message);
	}

	@Test
	void shouldReturnMessageBatch() {
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		AsyncMessageInterceptor<Object> interceptor = new AsyncMessageInterceptor<Object>() {
		};
		CompletableFuture<Collection<Message<Object>>> future = interceptor.intercept(batch);
		assertThat(future).isCompletedWithValue(batch);
	}

}
