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
package io.awspring.cloud.kinesis.listener.checkpoint;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.exceptions.ThrottlingException;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
class DefaultKclCheckpointerTests {

	private final RecordProcessorCheckpointer delegate = mock(RecordProcessorCheckpointer.class);

	private DefaultKclCheckpointer checkpointer(int maxRetries) {
		return new DefaultKclCheckpointer(this.delegate, maxRetries, Duration.ofMillis(1));
	}

	@Test
	@DisplayName("checkpoint() delegates to the KCL checkpointer")
	void checkpointLatestDelegates() throws Exception {
		checkpointer(3).checkpoint();
		verify(this.delegate).checkpoint();
	}

	@Test
	@DisplayName("checkpoint(message) uses the sequence number when no subsequence is present")
	void checkpointMessageUsesSequenceNumber() throws Exception {
		Message<String> message = MessageBuilder.withPayload("p")
				.setHeader(KinesisMessageHeaders.SEQUENCE_NUMBER, "seq-1").build();
		checkpointer(3).checkpoint(message);
		verify(this.delegate).checkpoint("seq-1");
	}

	@Test
	@DisplayName("checkpoint(message) uses sequence and subsequence when subsequence > 0")
	void checkpointMessageUsesSubsequenceNumber() throws Exception {
		Message<String> message = MessageBuilder.withPayload("p")
				.setHeader(KinesisMessageHeaders.SEQUENCE_NUMBER, "seq-1")
				.setHeader(KinesisMessageHeaders.SUBSEQUENCE_NUMBER, 5L).build();
		checkpointer(3).checkpoint(message);
		verify(this.delegate).checkpoint("seq-1", 5L);
	}

	@Test
	@DisplayName("checkpoint(message) fails fast when the sequence number header is missing")
	void checkpointMessageMissingSequenceNumber() {
		Message<String> message = MessageBuilder.withPayload("p").build();
		assertThatThrownBy(() -> checkpointer(3).checkpoint(message)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("retries throttling exceptions and succeeds")
	void retriesOnThrottlingThenSucceeds() throws Exception {
		doThrow(new ThrottlingException("throttled")).doThrow(new ThrottlingException("throttled")).doNothing()
				.when(this.delegate).checkpoint();

		checkpointer(3).checkpoint();

		verify(this.delegate, times(3)).checkpoint();
	}

	@Test
	@DisplayName("throws CheckpointException when throttling retries are exhausted")
	void throttlingExhaustionThrowsCheckpointException() throws Exception {
		doThrow(new ThrottlingException("throttled")).when(this.delegate).checkpoint();

		assertThatThrownBy(() -> checkpointer(2).checkpoint()).isInstanceOf(CheckpointException.class)
				.hasCauseInstanceOf(ThrottlingException.class);

		verify(this.delegate, times(3)).checkpoint();
	}

	@Test
	@DisplayName("maps ShutdownException to LeaseLostCheckpointException without retrying")
	void shutdownMapsToLeaseLost() throws Exception {
		doThrow(new ShutdownException("lease lost")).when(this.delegate).checkpoint();

		assertThatThrownBy(() -> checkpointer(3).checkpoint()).isInstanceOf(LeaseLostCheckpointException.class)
				.hasCauseInstanceOf(ShutdownException.class);

		verify(this.delegate, times(1)).checkpoint();
	}

	@Test
	@DisplayName("maps InvalidStateException to CheckpointException without retrying")
	void invalidStateMapsToCheckpointException() throws Exception {
		doThrow(new InvalidStateException("bad table")).when(this.delegate).checkpoint();

		assertThatThrownBy(() -> checkpointer(3).checkpoint()).isInstanceOf(CheckpointException.class)
				.hasCauseInstanceOf(InvalidStateException.class);

		verify(this.delegate, times(1)).checkpoint();
	}

}
