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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.sqs.listener.BatchVisibility;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.Visibility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Tests for {@link ErrorHandlerVisibilityHelper}.
 *
 * @author Bruno Garcia
 * @author Rafael Pavarini
 */
class ErrorHandlerVisibilityHelperTest {

	@Test
	void getReceiveMessageCount() {
		Message<Object> message = mock(Message.class);
		MessageHeaders headers = mock(MessageHeaders.class);

		given(message.getHeaders()).willReturn(headers);
		given(headers.get(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class))
				.willReturn("30");

		long receiveMessageCount = ErrorHandlerVisibilityHelper.getReceiveMessageCount(message);

		assertThat(receiveMessageCount).isEqualTo(30L);
	}

	@Test
	void getVisibility() {
		Message<Object> message = mock(Message.class);
		MessageHeaders headers = mock(MessageHeaders.class);
		Visibility visibilityMock = mock(Visibility.class);

		given(message.getHeaders()).willReturn(headers);
		given(headers.get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class)).willReturn(visibilityMock);

		Visibility visibility = ErrorHandlerVisibilityHelper.getVisibility(message);

		assertThat(visibility).isSameAs(visibilityMock);

	}

	@Test
	void getVisibilityBatch() {
		QueueMessageVisibility queueVisibility = mock(QueueMessageVisibility.class);
		BatchVisibility expectedBatchVisibility = mock(BatchVisibility.class);
		Message<Object> m1 = mock(Message.class);
		Message<Object> m2 = mock(Message.class);
		Message<Object> m3 = mock(Message.class);
		MessageHeaders headers = mock(MessageHeaders.class);

		given(m1.getHeaders()).willReturn(headers);
		given(m2.getHeaders()).willReturn(headers);
		given(m3.getHeaders()).willReturn(headers);

		List<Message<Object>> batch = List.of(m1, m2, m3);
		Collection<Message<?>> castMessages = new ArrayList<>(batch);

		given(headers.get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class)).willReturn(queueVisibility);
		given(queueVisibility.toBatchVisibility(castMessages)).willReturn(expectedBatchVisibility);

		BatchVisibility batchVisibility = ErrorHandlerVisibilityHelper.getVisibility(batch);

		assertThat(batchVisibility).isSameAs(expectedBatchVisibility);
	}

	@Test
	void castMessages() {
		Message<Object> m1 = mock(Message.class);
		Message<Object> m2 = mock(Message.class);
		Message<Object> m3 = mock(Message.class);
		List<Message<Object>> original = List.of(m1, m2, m3);

		Collection<Message<?>> cast = ErrorHandlerVisibilityHelper.castMessages(original);

		assertThat(cast).containsExactlyElementsOf(original);
	}

	@Test
	void groupMessagesByReceiveMessageCount() {
		Message<Object> lowMsg = mock(Message.class);
		Message<Object> highMsg1 = mock(Message.class);
		Message<Object> highMsg2 = mock(Message.class);

		MessageHeaders lowHeaders = mock(MessageHeaders.class);
		MessageHeaders highHeaders = mock(MessageHeaders.class);

		given(lowMsg.getHeaders()).willReturn(lowHeaders);
		given(highMsg1.getHeaders()).willReturn(highHeaders);
		given(highMsg2.getHeaders()).willReturn(highHeaders);

		given(lowHeaders.get(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class))
				.willReturn("1");
		given(highHeaders.get(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class))
				.willReturn("30");

		List<Message<Object>> batch = List.of(lowMsg, highMsg1, highMsg2);

		Map<Long, List<Message<Object>>> grouped = ErrorHandlerVisibilityHelper
				.groupMessagesByReceiveMessageCount(batch);

		assertThat(grouped).hasSize(2);
		assertThat(grouped.get(1L)).containsExactly(lowMsg);
		assertThat(grouped.get(30L)).containsExactly(highMsg1, highMsg2);
	}
}
