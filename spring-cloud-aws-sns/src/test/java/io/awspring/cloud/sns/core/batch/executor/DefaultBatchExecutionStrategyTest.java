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
package io.awspring.cloud.sns.core.batch.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sns.core.batch.BatchResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sns.model.PublishBatchRequest;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;
import software.amazon.awssdk.services.sns.model.PublishBatchResponse;
import software.amazon.awssdk.services.sns.model.PublishBatchResultEntry;

/**
 *
 * @author Matej Nedic
 */
@ExtendWith(MockitoExtension.class)
class DefaultBatchExecutionStrategyTest {

	@Mock
	private SnsClient snsClient;

	@Captor
	private ArgumentCaptor<PublishBatchRequest> requestCaptor;

	@Test
	void throwsExceptionWhenSnsClientIsNull() {
		assertThatThrownBy(() -> new DefaultBatchExecutionStrategy(null)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("SnsClient cannot be null");
	}

	@Test
	void sendsSingleBatchSuccessfully() {
		DefaultBatchExecutionStrategy strategy = new DefaultBatchExecutionStrategy(snsClient);
		Arn topicArn = Arn.fromString("arn:aws:sns:us-east-1:00000000000:test-topic");

		List<PublishBatchRequestEntry> entries = createEntries(5);

		PublishBatchResponse response = PublishBatchResponse.builder()
				.successful(PublishBatchResultEntry.builder().id("1").messageId("msg-1").build(),
						PublishBatchResultEntry.builder().id("2").messageId("msg-2").build(),
						PublishBatchResultEntry.builder().id("3").messageId("msg-3").build(),
						PublishBatchResultEntry.builder().id("4").messageId("msg-4").build(),
						PublishBatchResultEntry.builder().id("5").messageId("msg-5").build())
				.build();

		when(snsClient.publishBatch(any(PublishBatchRequest.class))).thenReturn(response);

		BatchResult result = strategy.send(topicArn, entries);

		assertThat(result.results()).hasSize(5);
		assertThat(result.errors()).isEmpty();
		assertThat(result.isFullySuccessful()).isTrue();
		assertThat(result.hasErrors()).isFalse();
		verify(snsClient, times(1)).publishBatch(any(PublishBatchRequest.class));
	}

	@Test
	void sendsTwelveMessagesInTwoBatches() {
		DefaultBatchExecutionStrategy strategy = new DefaultBatchExecutionStrategy(snsClient);
		Arn topicArn = Arn.fromString("arn:aws:sns:us-east-1:123456789012:test-topic");

		List<PublishBatchRequestEntry> entries = createEntries(12);

		// First batch response (10 messages)
		PublishBatchResponse response1 = PublishBatchResponse.builder()
				.successful(PublishBatchResultEntry.builder().id("1").messageId("msg-1").build(),
						PublishBatchResultEntry.builder().id("2").messageId("msg-2").build(),
						PublishBatchResultEntry.builder().id("3").messageId("msg-3").build(),
						PublishBatchResultEntry.builder().id("4").messageId("msg-4").build(),
						PublishBatchResultEntry.builder().id("5").messageId("msg-5").build(),
						PublishBatchResultEntry.builder().id("6").messageId("msg-6").build(),
						PublishBatchResultEntry.builder().id("7").messageId("msg-7").build(),
						PublishBatchResultEntry.builder().id("8").messageId("msg-8").build(),
						PublishBatchResultEntry.builder().id("9").messageId("msg-9").build(),
						PublishBatchResultEntry.builder().id("10").messageId("msg-10").build())
				.build();

		// Second batch response (2 messages)
		PublishBatchResponse response2 = PublishBatchResponse.builder()
				.successful(PublishBatchResultEntry.builder().id("11").messageId("msg-11").build(),
						PublishBatchResultEntry.builder().id("12").messageId("msg-12").build())
				.build();

		when(snsClient.publishBatch(any(PublishBatchRequest.class))).thenReturn(response1).thenReturn(response2);

		BatchResult result = strategy.send(topicArn, entries);

		assertThat(result.results()).hasSize(12);
		assertThat(result.errors()).isEmpty();
		assertThat(result.isFullySuccessful()).isTrue();
		verify(snsClient, times(2)).publishBatch(requestCaptor.capture());

		List<PublishBatchRequest> capturedRequests = requestCaptor.getAllValues();
		assertThat(capturedRequests.get(0).publishBatchRequestEntries()).hasSize(10);
		assertThat(capturedRequests.get(1).publishBatchRequestEntries()).hasSize(2);
	}

	@Test
	void handlesPartialFailures() {
		DefaultBatchExecutionStrategy strategy = new DefaultBatchExecutionStrategy(snsClient);
		Arn topicArn = Arn.fromString("arn:aws:sns:us-east-1:000000000000:test-topic");

		List<PublishBatchRequestEntry> entries = createEntries(5);

		PublishBatchResponse response = PublishBatchResponse.builder()
				.successful(PublishBatchResultEntry.builder().id("1").messageId("msg-1").build(),
						PublishBatchResultEntry.builder().id("2").messageId("msg-2").build(),
						PublishBatchResultEntry.builder().id("3").messageId("msg-3").build())
				.failed(BatchResultErrorEntry.builder().id("4").code("InvalidParameter").message("Invalid message")
						.senderFault(true).build(),
						BatchResultErrorEntry.builder().id("5").code("ServiceError").message("Service unavailable")
								.senderFault(false).build())
				.build();

		when(snsClient.publishBatch(any(PublishBatchRequest.class))).thenReturn(response);

		BatchResult result = strategy.send(topicArn, entries);

		assertThat(result.results()).hasSize(3);
		assertThat(result.errors()).hasSize(2);
		assertThat(result.isFullySuccessful()).isFalse();
		assertThat(result.hasErrors()).isTrue();

		assertThat(result.errors()).extracting(BatchResult.SnsError::code).containsExactlyInAnyOrder("InvalidParameter",
				"ServiceError");
	}

	@Test
	void handlesAllFailures() {
		DefaultBatchExecutionStrategy strategy = new DefaultBatchExecutionStrategy(snsClient);
		Arn topicArn = Arn.fromString("arn:aws:sns:us-east-1:000000000000:test-topic");

		List<PublishBatchRequestEntry> entries = createEntries(3);

		PublishBatchResponse response = PublishBatchResponse.builder()
				.failed(BatchResultErrorEntry.builder().id("1").code("InvalidParameter").message("Invalid message 1")
						.senderFault(true).build(),
						BatchResultErrorEntry.builder().id("2").code("InvalidParameter").message("Invalid message 2")
								.senderFault(true).build(),
						BatchResultErrorEntry.builder().id("3").code("InvalidParameter").message("Invalid message 3")
								.senderFault(true).build())
				.build();

		when(snsClient.publishBatch(any(PublishBatchRequest.class))).thenReturn(response);

		BatchResult result = strategy.send(topicArn, entries);

		assertThat(result.results()).isEmpty();
		assertThat(result.errors()).hasSize(3);
		assertThat(result.isFullySuccessful()).isFalse();
		assertThat(result.hasErrors()).isTrue();
	}

	@Test
	void combinesResultsFromMultipleBatches() {
		DefaultBatchExecutionStrategy strategy = new DefaultBatchExecutionStrategy(snsClient);
		Arn topicArn = Arn.fromString("arn:aws:sns:us-east-1:123456789012:test-topic");

		List<PublishBatchRequestEntry> entries = createEntries(15);

		// First batch: 10 messages, 8 success, 2 failures
		PublishBatchResponse response1 = PublishBatchResponse.builder()
				.successful(PublishBatchResultEntry.builder().id("1").messageId("msg-1").build(),
						PublishBatchResultEntry.builder().id("2").messageId("msg-2").build(),
						PublishBatchResultEntry.builder().id("3").messageId("msg-3").build(),
						PublishBatchResultEntry.builder().id("4").messageId("msg-4").build(),
						PublishBatchResultEntry.builder().id("5").messageId("msg-5").build(),
						PublishBatchResultEntry.builder().id("6").messageId("msg-6").build(),
						PublishBatchResultEntry.builder().id("7").messageId("msg-7").build(),
						PublishBatchResultEntry.builder().id("8").messageId("msg-8").build())
				.failed(BatchResultErrorEntry.builder().id("9").code("Error1").message("Error 1").senderFault(true)
						.build(),
						BatchResultErrorEntry.builder().id("10").code("Error2").message("Error 2").senderFault(true)
								.build())
				.build();

		// Second batch: 5 messages, all success
		PublishBatchResponse response2 = PublishBatchResponse.builder()
				.successful(PublishBatchResultEntry.builder().id("11").messageId("msg-11").build(),
						PublishBatchResultEntry.builder().id("12").messageId("msg-12").build(),
						PublishBatchResultEntry.builder().id("13").messageId("msg-13").build(),
						PublishBatchResultEntry.builder().id("14").messageId("msg-14").build(),
						PublishBatchResultEntry.builder().id("15").messageId("msg-15").build())
				.build();

		when(snsClient.publishBatch(any(PublishBatchRequest.class))).thenReturn(response1).thenReturn(response2);

		BatchResult result = strategy.send(topicArn, entries);

		assertThat(result.results()).hasSize(13);
		assertThat(result.errors()).hasSize(2);
		assertThat(result.isFullySuccessful()).isFalse();
		assertThat(result.hasErrors()).isTrue();
		verify(snsClient, times(2)).publishBatch(any(PublishBatchRequest.class));
	}

	private List<PublishBatchRequestEntry> createEntries(int count) {
		List<PublishBatchRequestEntry> entries = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			entries.add(PublishBatchRequestEntry.builder().id(String.valueOf(i)).message("Message " + i).build());
		}
		return entries;
	}
}
