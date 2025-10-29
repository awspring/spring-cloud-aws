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

import static org.assertj.core.api.Assertions.*;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Tests for {@link ErrorHandlerVisibilityHelper}.
 *
 * @author Bruno Garcia
 * @author Rafael Pavarini
 */
@SuppressWarnings("unchecked")
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

	@Test
	void checkVisibilityTimeout() {
		assertThatNoException().isThrownBy(() -> ErrorHandlerVisibilityHelper.checkVisibilityTimeout(1));
		assertThatThrownBy(() -> ErrorHandlerVisibilityHelper.checkVisibilityTimeout(0))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Should be greater than 0");

		assertThatThrownBy(() -> ErrorHandlerVisibilityHelper
				.checkVisibilityTimeout(Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS + 1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Should be less than or equal to " + Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS);
	}

	@ParameterizedTest
	@MethodSource("testCases")
	void calculateVisibilityTimeoutExponentially(VisibilityTimeoutTestCase testCase) {
		assertThat(testCase.calculateVisibilityTimeout(CalculationType.EXPONENTIAL))
				.isEqualTo(testCase.timeoutExpectedExponentially);
	}

	@ParameterizedTest
	@MethodSource("testCases")
	void calculateVisibilityTimeoutLinearly(VisibilityTimeoutTestCase testCase) {
		assertThat(testCase.calculateVisibilityTimeout(CalculationType.LINEAR))
				.isEqualTo(testCase.timeoutExpectedLinearly);
	}

	private static Collection<VisibilityTimeoutTestCase> testCases() {
		return List.of(minTestCase(), maxTestCase(), defaultTestCase(),
				defaultVisibilityTimeoutSetup().receiveMessageCount(2).timeoutExpectedExponentially(200)
						.timeoutExpectedLinearly(102),
				defaultVisibilityTimeoutSetup().receiveMessageCount(3).timeoutExpectedExponentially(400)
						.timeoutExpectedLinearly(104),
				defaultVisibilityTimeoutSetup().receiveMessageCount(5).timeoutExpectedExponentially(1600)
						.timeoutExpectedLinearly(108),
				defaultVisibilityTimeoutSetup().receiveMessageCount(7).timeoutExpectedExponentially(6400)
						.timeoutExpectedLinearly(112),
				defaultVisibilityTimeoutSetup().receiveMessageCount(11)
						.timeoutExpectedExponentially(Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS)
						.timeoutExpectedLinearly(120),
				defaultVisibilityTimeoutSetup().receiveMessageCount(13)
						.timeoutExpectedExponentially(Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS)
						.timeoutExpectedLinearly(124),
				defaultVisibilityTimeoutSetup().receiveMessageCount(21551)
						.timeoutExpectedExponentially(Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS)
						.timeoutExpectedLinearly(Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS));
	}

	private static VisibilityTimeoutTestCase minTestCase() {
		return new VisibilityTimeoutTestCase().initialVisibilityTimeoutSeconds(1)
				.maxVisibilityTimeoutSeconds(Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS).multiplier(2.0)
				.receiveMessageCount(1).timeoutExpectedExponentially(1).timeoutExpectedLinearly(1);
	}

	private static VisibilityTimeoutTestCase defaultTestCase() {
		return defaultVisibilityTimeoutSetup().receiveMessageCount(1)
				.timeoutExpectedExponentially(BackoffVisibilityConstants.DEFAULT_INITIAL_VISIBILITY_TIMEOUT_SECONDS)
				.timeoutExpectedLinearly(BackoffVisibilityConstants.DEFAULT_INITIAL_VISIBILITY_TIMEOUT_SECONDS);
	}

	private static VisibilityTimeoutTestCase defaultVisibilityTimeoutSetup() {
		return new VisibilityTimeoutTestCase()
				.initialVisibilityTimeoutSeconds(BackoffVisibilityConstants.DEFAULT_INITIAL_VISIBILITY_TIMEOUT_SECONDS)
				.maxVisibilityTimeoutSeconds(BackoffVisibilityConstants.DEFAULT_MAX_VISIBILITY_TIMEOUT_SECONDS)
				.multiplier(BackoffVisibilityConstants.DEFAULT_MULTIPLIER);
	}

	private static VisibilityTimeoutTestCase maxTestCase() {
		return new VisibilityTimeoutTestCase().initialVisibilityTimeoutSeconds(Integer.MAX_VALUE)
				.maxVisibilityTimeoutSeconds(Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS).multiplier(2.0)
				.receiveMessageCount(1).timeoutExpectedExponentially(Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS)
				.timeoutExpectedLinearly(Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS);
	}

	private static class VisibilityTimeoutTestCase {
		int receiveMessageCount;
		int initialVisibilityTimeoutSeconds;
		double multiplier;
		int maxVisibilityTimeoutSeconds;
		int timeoutExpectedExponentially;
		int timeoutExpectedLinearly;

		VisibilityTimeoutTestCase receiveMessageCount(int receiveMessageCount) {
			this.receiveMessageCount = receiveMessageCount;
			return this;
		}

		VisibilityTimeoutTestCase timeoutExpectedLinearly(int timeoutExpected) {
			this.timeoutExpectedLinearly = timeoutExpected;
			return this;
		}

		VisibilityTimeoutTestCase timeoutExpectedExponentially(int timeoutExpected) {
			this.timeoutExpectedExponentially = timeoutExpected;
			return this;
		}

		VisibilityTimeoutTestCase initialVisibilityTimeoutSeconds(int initialVisibilityTimeoutSeconds) {
			this.initialVisibilityTimeoutSeconds = initialVisibilityTimeoutSeconds;
			return this;
		}

		VisibilityTimeoutTestCase maxVisibilityTimeoutSeconds(int maxVisibilityTimeoutSeconds) {
			this.maxVisibilityTimeoutSeconds = maxVisibilityTimeoutSeconds;
			return this;
		}

		VisibilityTimeoutTestCase multiplier(double multiplier) {
			this.multiplier = multiplier;
			return this;
		}

		int calculateVisibilityTimeout(CalculationType calculationType) {
			return switch (calculationType) {
			case EXPONENTIAL ->
				ErrorHandlerVisibilityHelper.calculateVisibilityTimeoutExponentially(this.receiveMessageCount,
						this.initialVisibilityTimeoutSeconds, this.multiplier, this.maxVisibilityTimeoutSeconds);
			case LINEAR -> ErrorHandlerVisibilityHelper.calculateVisibilityTimeoutLinearly(this.receiveMessageCount,
					this.initialVisibilityTimeoutSeconds, (int) this.multiplier, this.maxVisibilityTimeoutSeconds);
			};
		}

	}

	private enum CalculationType {
		EXPONENTIAL, LINEAR

	}
}
