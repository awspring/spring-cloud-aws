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
package io.awspring.cloud.sqs.listener.errorhandler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

/**
 * Tests for {@link ErrorHandler}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings("unchecked")
class ErrorHandlerTests {

	@Test
	void shouldReturnError() {
		Message<Object> message = mock(Message.class);
		RuntimeException exception = new RuntimeException("Expected exception from shouldReturnError");
		ErrorHandler<Object> handler = new ErrorHandler<Object>() {
		};
		assertThatThrownBy(() -> handler.handle(message, exception)).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void shouldReturnErrorBatch() {
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);
		RuntimeException exception = new RuntimeException("Expected exception from shouldReturnErrorBatch");
		ErrorHandler<Object> handler = new ErrorHandler<Object>() {
		};
		assertThatThrownBy(() -> handler.handle(batch, exception)).isInstanceOf(UnsupportedOperationException.class);
	}

}
