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
import java.util.Collections;
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
 * Tests for {@link SequentialBatchExecutionStrategy}.
 *
 * @author Matej Nedic
 */
@ExtendWith(MockitoExtension.class)
class SequentialBatchExecutionStrategyTest {

	private static final Arn TOPIC_ARN = Arn.fromString("arn:aws:sns:us-east-1:000000000000:test-topic");

	@Mock
	private SnsClient snsClient;

	@Captor
	private ArgumentCaptor<PublishBatchRequest> requestCaptor;

	@Test
	void returnsEmptyResultForEmptyEntries() {
		var strategy = new SequentialBatchExecutionStrategy(snsClient);

		BatchResult result = strategy.send(TOPIC_ARN, Collections.emptyList());

		assertThat(result.results()).isEmpty();
		assertThat(result.errors()).isEmpty();
	}

	@Test
	void sendsSingleBatchSuccessfully() {
		var strategy = new SequentialBatchExecutionStrategy(snsClient);
		var entries = createEntries(5);

		when(snsClient.publishBatch(any(PublishBatchRequest.class)))
				.thenReturn(successResponse("msg-1", "msg-2", "msg-3", "msg-4", "msg-5"));

		BatchResult result = strategy.send(TOPIC_ARN, entries);

		assertThat(result.results()).extracting(BatchResult.SnsResult::messageId)
				.containsExactlyInAnyOrder("msg-1", "msg-2", "msg-3", "msg-4", "msg-5");
		assertThat(result.errors()).isEmpty();
		verify(snsClient, times(1)).publishBatch(any(PublishBatchRequest.class));
	}

	@Test
	void sendsExactlyTenMessagesInSingleBatch() {
		var strategy = new SequentialBatchExecutionStrategy(snsClient);
		var entries = createEntries(10);

		when(snsClient.publishBatch(any(PublishBatchRequest.class)))
				.thenReturn(successResponse("m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9", "m10"));

		BatchResult result = strategy.send(TOPIC_ARN, entries);

		assertThat(result.results()).hasSize(10);
		assertThat(result.errors()).isEmpty();
		verify(snsClient, times(1)).publishBatch(requestCaptor.capture());
		assertThat(requestCaptor.getValue().publishBatchRequestEntries()).hasSize(10);
	}

	@Test
	void splitsTwelveMessagesIntoTwoBatches() {
		var strategy = new SequentialBatchExecutionStrategy(snsClient);
		var entries = createEntries(12);

		when(snsClient.publishBatch(any(PublishBatchRequest.class)))
				.thenReturn(successResponse("m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9", "m10"))
				.thenReturn(successResponse("m11", "m12"));

		BatchResult result = strategy.send(TOPIC_ARN, entries);

		assertThat(result.results()).hasSize(12);
		assertThat(result.errors()).isEmpty();
		verify(snsClient, times(2)).publishBatch(requestCaptor.capture());

		List<PublishBatchRequest> captured = requestCaptor.getAllValues();
		assertThat(captured.get(0).publishBatchRequestEntries()).hasSize(10);
		assertThat(captured.get(1).publishBatchRequestEntries()).hasSize(2);
		//Verify that first and second batch are sent properly
		assertThat(captured.get(0).publishBatchRequestEntries().get(0).message()).isEqualTo("Message 1");
		assertThat(captured.get(1).publishBatchRequestEntries().get(0).message()).isEqualTo("Message 11");
	}

	@Test
	void passesCorrectTopicArnInRequest() {
		var strategy = new SequentialBatchExecutionStrategy(snsClient);
		var entries = createEntries(1);

		when(snsClient.publishBatch(any(PublishBatchRequest.class)))
				.thenReturn(successResponse("msg-1"));

		strategy.send(TOPIC_ARN, entries);

		verify(snsClient).publishBatch(requestCaptor.capture());
		assertThat(requestCaptor.getValue().topicArn()).isEqualTo(TOPIC_ARN.toString());
	}

	@Test
	void handlesPartialFailures() {
		var strategy = new SequentialBatchExecutionStrategy(snsClient);
		var entries = createEntries(5);

		PublishBatchResponse response = PublishBatchResponse.builder()
				.successful(
						PublishBatchResultEntry.builder().id("1").messageId("msg-1").build(),
						PublishBatchResultEntry.builder().id("2").messageId("msg-2").build(),
						PublishBatchResultEntry.builder().id("3").messageId("msg-3").build())
				.failed(
						BatchResultErrorEntry.builder().id("4").code("InvalidParameter")
								.message("Invalid").senderFault(true).build(),
						BatchResultErrorEntry.builder().id("5").code("ServiceError")
								.message("Unavailable").senderFault(false).build())
				.build();

		when(snsClient.publishBatch(any(PublishBatchRequest.class))).thenReturn(response);

		BatchResult result = strategy.send(TOPIC_ARN, entries);

		assertThat(result.results()).extracting(BatchResult.SnsResult::messageId)
				.containsExactlyInAnyOrder("msg-1", "msg-2", "msg-3");
		assertThat(result.errors()).extracting(BatchResult.SnsError::code)
				.containsExactlyInAnyOrder("InvalidParameter", "ServiceError");
	}

	@Test
	void handlesAllFailures() {
		var strategy = new SequentialBatchExecutionStrategy(snsClient);
		var entries = createEntries(3);

		PublishBatchResponse response = PublishBatchResponse.builder()
				.failed(
						BatchResultErrorEntry.builder().id("1").code("Err").message("e1").senderFault(true).build(),
						BatchResultErrorEntry.builder().id("2").code("Err").message("e2").senderFault(true).build(),
						BatchResultErrorEntry.builder().id("3").code("Err").message("e3").senderFault(true).build())
				.build();

		when(snsClient.publishBatch(any(PublishBatchRequest.class))).thenReturn(response);

		BatchResult result = strategy.send(TOPIC_ARN, entries);

		assertThat(result.results()).isEmpty();
		assertThat(result.errors()).extracting(BatchResult.SnsError::message)
				.containsExactlyInAnyOrder("e1", "e2", "e3");
	}

	@Test
	void combinesResultsAcrossMultipleBatches() {
		var strategy = new SequentialBatchExecutionStrategy(snsClient);
		var entries = createEntries(15);

		// First batch: 8 success, 2 failures
		PublishBatchResponse response1 = PublishBatchResponse.builder()
				.successful(
						PublishBatchResultEntry.builder().id("1").messageId("m1").build(),
						PublishBatchResultEntry.builder().id("2").messageId("m2").build(),
						PublishBatchResultEntry.builder().id("3").messageId("m3").build(),
						PublishBatchResultEntry.builder().id("4").messageId("m4").build(),
						PublishBatchResultEntry.builder().id("5").messageId("m5").build(),
						PublishBatchResultEntry.builder().id("6").messageId("m6").build(),
						PublishBatchResultEntry.builder().id("7").messageId("m7").build(),
						PublishBatchResultEntry.builder().id("8").messageId("m8").build())
				.failed(
						BatchResultErrorEntry.builder().id("9").code("E1").message("err1").senderFault(true).build(),
						BatchResultErrorEntry.builder().id("10").code("E2").message("err2").senderFault(true).build())
				.build();

		// Second batch: all 5 success
		when(snsClient.publishBatch(any(PublishBatchRequest.class)))
				.thenReturn(response1)
				.thenReturn(successResponse("m11", "m12", "m13", "m14", "m15"));

		BatchResult result = strategy.send(TOPIC_ARN, entries);

		assertThat(result.results()).hasSize(13);
		assertThat(result.errors()).extracting(BatchResult.SnsError::code)
				.containsExactlyInAnyOrder("E1", "E2");
		verify(snsClient, times(2)).publishBatch(any(PublishBatchRequest.class));
	}

	private List<PublishBatchRequestEntry> createEntries(int count) {
		List<PublishBatchRequestEntry> entries = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			entries.add(PublishBatchRequestEntry.builder()
					.id(String.valueOf(i)).message("Message " + i).build());
		}
		return entries;
	}

	private PublishBatchResponse successResponse(String... messageIds) {
		var builder = PublishBatchResponse.builder();
		List<PublishBatchResultEntry> results = new ArrayList<>();
		for (int i = 0; i < messageIds.length; i++) {
			results.add(PublishBatchResultEntry.builder()
					.id(String.valueOf(i + 1)).messageId(messageIds[i]).build());
		}
		return builder.successful(results).build();
	}
}
