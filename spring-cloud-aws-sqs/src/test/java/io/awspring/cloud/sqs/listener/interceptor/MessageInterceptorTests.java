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
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

/**
 * Tests for {@link MessageInterceptor}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings("unchecked")
class MessageInterceptorTests {

	@Test
	void shouldReturnMessage() {
		MessageInterceptor<Object> interceptor = new MessageInterceptor<Object>() {
		};
		Message<Object> message = mock(Message.class);
		Message<Object> interceptResult = interceptor.intercept(message);
		assertThat(interceptResult).isEqualTo(message);
	}

	@Test
	void shouldReturnMessageBatch() {
		MessageInterceptor<Object> interceptor = new MessageInterceptor<Object>() {
		};
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		Collection<Message<Object>> interceptorResult = interceptor.intercept(batch);
		assertThat(interceptorResult).isEqualTo(batch);
	}

}
