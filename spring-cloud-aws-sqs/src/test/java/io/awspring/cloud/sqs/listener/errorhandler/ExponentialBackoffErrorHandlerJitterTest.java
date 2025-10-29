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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import io.awspring.cloud.sqs.listener.BatchVisibility;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.Visibility;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Tests for {@link ExponentialBackoffErrorHandler} with different {@link Jitter} implementations.
 *
 * @author Bruno Garcia
 * @author Rafael Pavarini
 */
class ExponentialBackoffErrorHandlerJitterTest extends BaseExponentialBackoffErrorHandlerJitterTest {
	static Supplier<Random> midRandomSupplier = () -> new MockedRandomNextInt(timeout -> timeout / 2);
	static Supplier<Random> maxRandomSupplier = () -> new MockedRandomNextInt(timeout -> timeout - 1);

	@ParameterizedTest
	@MethodSource("testCases")
	void calculateExponentialFullJitter(BaseTestCase baseTestCase) {
		Message<Object> message = mock(Message.class);
		RuntimeException exception = new RuntimeException("Expected exception from shouldChangeVisibilityToZero");
		MessageHeaders headers = mock(MessageHeaders.class);
		Visibility visibility = mock(Visibility.class);
		given(message.getHeaders()).willReturn(headers);
		given(headers.get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class)).willReturn(visibility);
		given(headers.get(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class))
				.willReturn(baseTestCase.sqsApproximateReceiveCount);
		given(visibility.changeToAsync(anyInt())).willReturn(CompletableFuture.completedFuture(null));

		assertThat(baseTestCase.calculateWithVisibilityTimeoutExpectedFullJitter(message, exception))
				.isCompletedExceptionally();
		then(visibility).should().changeToAsync(baseTestCase.VisibilityTimeoutExpectedFullJitter);
	}

	@ParameterizedTest
	@MethodSource("testCases")
	void calculateExponentialFullJitterCollection(BaseTestCase baseTestCase) {
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message1, message2, message3);
		RuntimeException exception = new RuntimeException("Expected exception from shouldChangeVisibilityToZero");
		MessageHeaders headers = mock(MessageHeaders.class);
		QueueMessageVisibility visibility = mock(QueueMessageVisibility.class);
		BatchVisibility batchvisibility = mock(BatchVisibility.class);
		given(batchvisibility.changeToAsync(anyInt())).willReturn(CompletableFuture.completedFuture(null));
		given(batchvisibility.changeToAsync(anyInt())).willReturn(CompletableFuture.completedFuture(null));
		given(visibility.toBatchVisibility(any())).willReturn(batchvisibility);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(headers.getId()).willReturn(UUID.randomUUID());
		given(headers.get("id", UUID.class)).willReturn(UUID.randomUUID());
		given(headers.get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class)).willReturn(visibility);
		given(headers.get(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class))
				.willReturn(baseTestCase.sqsApproximateReceiveCount);
		given(batchvisibility.changeToAsync(anyInt())).willReturn(CompletableFuture.completedFuture(null));

		assertThat(baseTestCase.calculateWithVisibilityTimeoutExpectedFullJitter(messages, exception))
				.isCompletedExceptionally();
		then(batchvisibility).should(times(1)).changeToAsync(baseTestCase.VisibilityTimeoutExpectedFullJitter);
	}

	@ParameterizedTest
	@MethodSource("testCases")
	void calculateExponentialHalfJitterCollection(BaseTestCase baseTestCase) {
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message1, message2, message3);
		RuntimeException exception = new RuntimeException("Expected exception from shouldChangeVisibilityToZero");
		MessageHeaders headers = mock(MessageHeaders.class);
		QueueMessageVisibility visibility = mock(QueueMessageVisibility.class);
		BatchVisibility batchvisibility = mock(BatchVisibility.class);
		given(batchvisibility.changeToAsync(anyInt())).willReturn(CompletableFuture.completedFuture(null));
		given(batchvisibility.changeToAsync(anyInt())).willReturn(CompletableFuture.completedFuture(null));
		given(visibility.toBatchVisibility(any())).willReturn(batchvisibility);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);
		given(headers.getId()).willReturn(UUID.randomUUID());
		given(headers.get("id", UUID.class)).willReturn(UUID.randomUUID());
		given(headers.get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class)).willReturn(visibility);
		given(headers.get(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class))
				.willReturn(baseTestCase.sqsApproximateReceiveCount);
		given(batchvisibility.changeToAsync(anyInt())).willReturn(CompletableFuture.completedFuture(null));

		assertThat(baseTestCase.calculateWithVisibilityTimeoutExpectedHalfJitter(messages, exception))
				.isCompletedExceptionally();
		then(batchvisibility).should(times(1)).changeToAsync(baseTestCase.VisibilityTimeoutExpectedHalfJitter);
	}

	@ParameterizedTest
	@MethodSource("testCases")
	void calculateExponentialHalfJitter(BaseTestCase baseTestCase) {
		Message<Object> message = mock(Message.class);
		RuntimeException exception = new RuntimeException("Expected exception from shouldChangeVisibilityToZero");
		MessageHeaders headers = mock(MessageHeaders.class);
		Visibility visibility = mock(Visibility.class);
		given(message.getHeaders()).willReturn(headers);
		given(headers.get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class)).willReturn(visibility);
		given(headers.get(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, String.class))
				.willReturn(baseTestCase.sqsApproximateReceiveCount);
		given(visibility.changeToAsync(anyInt())).willReturn(CompletableFuture.completedFuture(null));

		assertThat(baseTestCase.calculateWithVisibilityTimeoutExpectedHalfJitter(message, exception))
				.isCompletedExceptionally();
		then(visibility).should().changeToAsync(baseTestCase.VisibilityTimeoutExpectedHalfJitter);

	}

	private static Collection<BaseTestCase> testCases() {
		return List.of(
				baseTestCaseMidRandomSupplier().sqsApproximateReceiveCount("1").VisibilityTimeoutExpectedHalfJitter(75)
						.VisibilityTimeoutExpectedFullJitter(50),

				baseTestCaseMidRandomSupplier().sqsApproximateReceiveCount("2").VisibilityTimeoutExpectedHalfJitter(150)
						.VisibilityTimeoutExpectedFullJitter(100),

				baseTestCaseMidRandomSupplier().sqsApproximateReceiveCount("3").VisibilityTimeoutExpectedHalfJitter(300)
						.VisibilityTimeoutExpectedFullJitter(200),

				baseTestCaseMidRandomSupplier().sqsApproximateReceiveCount("5")
						.VisibilityTimeoutExpectedHalfJitter(1200).VisibilityTimeoutExpectedFullJitter(800),

				baseTestCaseMidRandomSupplier().sqsApproximateReceiveCount("7")
						.VisibilityTimeoutExpectedHalfJitter(4800).VisibilityTimeoutExpectedFullJitter(3200),

				baseTestCaseMidRandomSupplier().sqsApproximateReceiveCount("11")
						.VisibilityTimeoutExpectedHalfJitter(32400).VisibilityTimeoutExpectedFullJitter(21600),

				baseTestCaseMidRandomSupplier().sqsApproximateReceiveCount("13")
						.VisibilityTimeoutExpectedHalfJitter(32400).VisibilityTimeoutExpectedFullJitter(21600),

				baseTestCaseMaxRandomSupplier().sqsApproximateReceiveCount("1").VisibilityTimeoutExpectedHalfJitter(100)
						.VisibilityTimeoutExpectedFullJitter(100),

				baseTestCaseMaxRandomSupplier().sqsApproximateReceiveCount("2").VisibilityTimeoutExpectedHalfJitter(200)
						.VisibilityTimeoutExpectedFullJitter(200),

				baseTestCaseMaxRandomSupplier().sqsApproximateReceiveCount("3").VisibilityTimeoutExpectedHalfJitter(400)
						.VisibilityTimeoutExpectedFullJitter(400),

				baseTestCaseMaxRandomSupplier().sqsApproximateReceiveCount("5")
						.VisibilityTimeoutExpectedHalfJitter(1600).VisibilityTimeoutExpectedFullJitter(1600),

				baseTestCaseMaxRandomSupplier().sqsApproximateReceiveCount("7")
						.VisibilityTimeoutExpectedHalfJitter(6400).VisibilityTimeoutExpectedFullJitter(6400),

				baseTestCaseMaxRandomSupplier().sqsApproximateReceiveCount("11")
						.VisibilityTimeoutExpectedHalfJitter(43200).VisibilityTimeoutExpectedFullJitter(43200),

				baseTestCaseMaxRandomSupplier().sqsApproximateReceiveCount("13")
						.VisibilityTimeoutExpectedHalfJitter(43200).VisibilityTimeoutExpectedFullJitter(43200));
	}

	private static BaseTestCase baseTestCaseMidRandomSupplier() {
		return new BaseTestCase()
				.initialVisibilityTimeoutSeconds(BackoffVisibilityConstants.DEFAULT_INITIAL_VISIBILITY_TIMEOUT_SECONDS)
				.randomSupplier(midRandomSupplier).multiplier(BackoffVisibilityConstants.DEFAULT_MULTIPLIER);
	}

	private static BaseTestCase baseTestCaseMaxRandomSupplier() {
		return new BaseTestCase()
				.initialVisibilityTimeoutSeconds(BackoffVisibilityConstants.DEFAULT_INITIAL_VISIBILITY_TIMEOUT_SECONDS)
				.randomSupplier(maxRandomSupplier).multiplier(BackoffVisibilityConstants.DEFAULT_MULTIPLIER);
	}
}
