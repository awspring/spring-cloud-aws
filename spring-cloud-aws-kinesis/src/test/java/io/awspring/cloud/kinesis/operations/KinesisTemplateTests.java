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
package io.awspring.cloud.kinesis.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.kinesis.support.converter.KinesisMessageConverter;
import io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResultEntry;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
class KinesisTemplateTests {

	@Test
	@DisplayName("send serializes a String payload and returns the sequence number and shard id")
	@SuppressWarnings("unchecked")
	void sendStringPayload() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		when(client.putRecord(any(Consumer.class))).thenReturn(CompletableFuture
				.completedFuture(PutRecordResponse.builder().sequenceNumber("seq-1").shardId("shard-1").build()));
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());

		SendResult result = template.send("orders", "pk-1", "hello");

		assertThat(result.sequenceNumber()).isEqualTo("seq-1");
		assertThat(result.shardId()).isEqualTo("shard-1");

		ArgumentCaptor<Consumer<PutRecordRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);
		verify(client).putRecord(captor.capture());
		PutRecordRequest.Builder builder = PutRecordRequest.builder();
		captor.getValue().accept(builder);
		PutRecordRequest request = builder.build();
		assertThat(request.streamName()).isEqualTo("orders");
		assertThat(request.partitionKey()).isEqualTo("pk-1");
		assertThat(request.data().asUtf8String()).isEqualTo("hello");
	}

	@Test
	@DisplayName("a template built with a bare KinesisMessagingMessageConverter uses the default payload converter")
	@SuppressWarnings("unchecked")
	void sendWithBareMessagingConverter() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		when(client.putRecord(any(Consumer.class))).thenReturn(CompletableFuture
				.completedFuture(PutRecordResponse.builder().sequenceNumber("seq-3").shardId("shard-1").build()));
		KinesisTemplate template = new KinesisTemplate(client, new KinesisMessagingMessageConverter());

		template.send("orders", "pk-1", new Order("o-1"));

		ArgumentCaptor<Consumer<PutRecordRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);
		verify(client).putRecord(captor.capture());
		PutRecordRequest.Builder builder = PutRecordRequest.builder();
		captor.getValue().accept(builder);
		assertThat(builder.build().data().asUtf8String()).isEqualTo("{\"id\":\"o-1\"}");
	}

	@Test
	@DisplayName("send serializes a POJO payload as JSON")
	@SuppressWarnings("unchecked")
	void sendPojoPayload() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		when(client.putRecord(any(Consumer.class))).thenReturn(CompletableFuture
				.completedFuture(PutRecordResponse.builder().sequenceNumber("seq-2").shardId("shard-1").build()));
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());

		template.send("orders", "pk-1", new Order("o-1"));

		ArgumentCaptor<Consumer<PutRecordRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);
		verify(client).putRecord(captor.capture());
		PutRecordRequest.Builder builder = PutRecordRequest.builder();
		captor.getValue().accept(builder);
		assertThat(builder.build().data().asUtf8String()).isEqualTo("{\"id\":\"o-1\"}");
	}

	@Test
	@DisplayName("send uses a custom KinesisMessageConverter when one is provided")
	@SuppressWarnings("unchecked")
	void sendUsesCustomMessageConverter() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		when(client.putRecord(any(Consumer.class))).thenReturn(CompletableFuture
				.completedFuture(PutRecordResponse.builder().sequenceNumber("seq-c").shardId("shard-1").build()));
		KinesisMessageConverter converter = new KinesisMessageConverter() {
			@Override
			public Message<byte[]> toMessagingMessage(KinesisClientRecord record, String shardId, String streamName) {
				throw new UnsupportedOperationException();
			}

			@Override
			public byte[] fromMessagingMessage(Message<?> message) {
				return ("converted:" + message.getPayload()).getBytes(StandardCharsets.UTF_8);
			}
		};
		KinesisTemplate template = new KinesisTemplate(client, converter);

		template.send("orders", "pk-1", "hello");

		ArgumentCaptor<Consumer<PutRecordRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);
		verify(client).putRecord(captor.capture());
		PutRecordRequest.Builder builder = PutRecordRequest.builder();
		captor.getValue().accept(builder);
		assertThat(builder.build().data().asUtf8String()).isEqualTo("converted:hello");
	}

	@Test
	@DisplayName("send with a SendRequest carries explicitHashKey and sequenceNumberForOrdering to the SDK")
	@SuppressWarnings("unchecked")
	void sendRequestCarriesOptionalParameters() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		when(client.putRecord(any(Consumer.class))).thenReturn(CompletableFuture
				.completedFuture(PutRecordResponse.builder().sequenceNumber("seq-9").shardId("shard-9").build()));
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());

		SendRequest request = SendRequest.builder().streamName("orders").partitionKey("pk-1").payload("hello")
				.explicitHashKey("123").sequenceNumberForOrdering("42").build();
		template.send(request);

		ArgumentCaptor<Consumer<PutRecordRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);
		verify(client).putRecord(captor.capture());
		PutRecordRequest.Builder builder = PutRecordRequest.builder();
		captor.getValue().accept(builder);
		PutRecordRequest built = builder.build();
		assertThat(built.explicitHashKey()).isEqualTo("123");
		assertThat(built.sequenceNumberForOrdering()).isEqualTo("42");
	}

	@Test
	@DisplayName("SendRequest accepts a partition key of exactly 256 characters")
	void partitionKeyAtBoundaryIsAccepted() {
		String boundary = "a".repeat(256);
		SendRequest request = SendRequest.builder().streamName("orders").partitionKey(boundary).payload("x").build();
		assertThat(request.partitionKey()).hasSize(256);
	}

	@Test
	@DisplayName("sendBatch splits 1,200 records into calls of 500/500/200 in order and merges results in request order")
	void sendBatchSplitsByRecordCount() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		List<Integer> callSizes = new ArrayList<>();
		when(client.putRecords(any(PutRecordsRequest.class))).thenAnswer(invocation -> {
			List<PutRecordsRequestEntry> records = ((PutRecordsRequest) invocation.getArgument(0)).records();
			callSizes.add(records.size());
			return CompletableFuture.completedFuture(echoResponse(records));
		});
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());

		List<SendRequest> requests = IntStream.range(0, 1200).mapToObj(
				i -> SendRequest.builder().streamName("orders").partitionKey("pk-" + i).payload("p-" + i).build())
				.collect(Collectors.toList());

		BatchSendResult result = template.sendBatch("orders", requests);

		assertThat(callSizes).containsExactly(500, 500, 200);
		assertThat(result.hasFailures()).isFalse();
		assertThat(result.successful()).hasSize(1200);
		assertThat(result.successful()).extracting(SendResult::sequenceNumber).containsExactlyElementsOf(
				IntStream.range(0, 1200).mapToObj(i -> "pk-" + i).collect(Collectors.toList()));
	}

	@Test
	@DisplayName("sendBatch keeps each PutRecords call within 5 MiB and the records in input order")
	void sendBatchSplitsBySize() {
		int maxRequestSizeBytes = 5 * 1024 * 1024;
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		List<List<PutRecordsRequestEntry>> calls = new ArrayList<>();
		when(client.putRecords(any(PutRecordsRequest.class))).thenAnswer(invocation -> {
			List<PutRecordsRequestEntry> records = ((PutRecordsRequest) invocation.getArgument(0)).records();
			calls.add(records);
			return CompletableFuture.completedFuture(echoResponse(records));
		});
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());

		// Each record is a 2 MiB byte[], so at most 2 fit in one 5 MiB request: 5 records -> 2/2/1.
		byte[] twoMiB = new byte[2 * 1024 * 1024];
		List<SendRequest> requests = IntStream.range(0, 5)
				.mapToObj(
						i -> SendRequest.builder().streamName("orders").partitionKey("pk-" + i).payload(twoMiB).build())
				.collect(Collectors.toList());

		BatchSendResult result = template.sendBatch("orders", requests);

		assertThat(result.successful()).hasSize(5);
		assertThat(calls.stream().map(List::size)).containsExactly(2, 2, 1);

		// Every request stays at or below the 5 MiB call size, measured as data bytes plus UTF-8 partition-key bytes.
		for (List<PutRecordsRequestEntry> call : calls) {
			long size = call.stream().mapToLong(entry -> entry.data().asByteArray().length
					+ entry.partitionKey().getBytes(StandardCharsets.UTF_8).length).sum();
			assertThat(size).isLessThanOrEqualTo(maxRequestSizeBytes);
		}

		// The records keep their input order when flattened across the calls.
		List<String> sentOrder = calls.stream().flatMap(List::stream).map(PutRecordsRequestEntry::partitionKey)
				.collect(Collectors.toList());
		assertThat(sentOrder).containsExactly("pk-0", "pk-1", "pk-2", "pk-3", "pk-4");
	}

	@Test
	@DisplayName("a failing second chunk fails the returned future")
	void sendBatchFailingSecondChunkFailsFuture() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		when(client.putRecords(any(PutRecordsRequest.class))).thenAnswer(invocation -> {
			List<PutRecordsRequestEntry> records = ((PutRecordsRequest) invocation.getArgument(0)).records();
			if (records.size() == 500) {
				return CompletableFuture.completedFuture(echoResponse(records));
			}
			return CompletableFuture.failedFuture(new IllegalStateException("chunk failed"));
		});
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());

		List<SendRequest> requests = IntStream.range(0, 700).mapToObj(
				i -> SendRequest.builder().streamName("orders").partitionKey("pk-" + i).payload("p-" + i).build())
				.collect(Collectors.toList());

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> template.sendBatch("orders", requests))
				.isInstanceOf(IllegalStateException.class).hasMessage("chunk failed");
	}

	@Test
	@DisplayName("retry resends only retryable failures and merges the results")
	void retryResendsOnlyRetryableFailures() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		// First call: pk-0 ok, pk-1 retryable failure. Second call (resend of pk-1): ok.
		when(client.putRecords(any(PutRecordsRequest.class)))
				.thenReturn(
						CompletableFuture.completedFuture(PutRecordsResponse.builder().failedRecordCount(1)
								.records(ok("seq-0", "shard-0"),
										failure("ProvisionedThroughputExceededException", "slow down"))
								.build()))
				.thenReturn(CompletableFuture.completedFuture(
						PutRecordsResponse.builder().failedRecordCount(0).records(ok("seq-1", "shard-0")).build()));
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());
		template.setBatchRetryBackOff(new org.springframework.util.backoff.FixedBackOff(1, 3));

		List<SendRequest> requests = List.of(
				SendRequest.builder().streamName("orders").partitionKey("pk-0").payload("v0").build(),
				SendRequest.builder().streamName("orders").partitionKey("pk-1").payload("v1").build());

		BatchSendResult result = template.sendBatch("orders", requests);

		verify(client, org.mockito.Mockito.times(2)).putRecords(any(PutRecordsRequest.class));
		assertThat(result.hasFailures()).isFalse();
		assertThat(result.successful()).extracting(SendResult::sequenceNumber).containsExactly("seq-0", "seq-1");
	}

	@Test
	@DisplayName("non-retryable failures are not retried")
	void nonRetryableFailuresAreNotRetried() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		when(client.putRecords(any(PutRecordsRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(PutRecordsResponse.builder().failedRecordCount(1)
						.records(ok("seq-0", "shard-0"), failure("ValidationException", "bad")).build()));
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());
		template.setBatchRetryBackOff(new org.springframework.util.backoff.FixedBackOff(1, 3));

		List<SendRequest> requests = List.of(
				SendRequest.builder().streamName("orders").partitionKey("pk-0").payload("v0").build(),
				SendRequest.builder().streamName("orders").partitionKey("pk-1").payload("v1").build());

		BatchSendResult result = template.sendBatch("orders", requests);

		verify(client, org.mockito.Mockito.times(1)).putRecords(any(PutRecordsRequest.class));
		assertThat(result.failed()).singleElement().satisfies(failed -> {
			assertThat(failed.partitionKey()).isEqualTo("pk-1");
			assertThat(failed.errorCode()).isEqualTo("ValidationException");
		});
	}

	@Test
	@DisplayName("retries stop when the back-off stops, reporting the last error")
	void retriesStopWhenBackOffStops() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		when(client.putRecords(any(PutRecordsRequest.class))).thenReturn(CompletableFuture.completedFuture(
				PutRecordsResponse.builder().failedRecordCount(1).records(failure("InternalFailure", "boom")).build()));
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());
		// One retry allowed: initial call + one resend = 2 calls, then the back-off stops.
		template.setBatchRetryBackOff(new org.springframework.util.backoff.FixedBackOff(1, 1));

		List<SendRequest> requests = List
				.of(SendRequest.builder().streamName("orders").partitionKey("pk-0").payload("v0").build());

		BatchSendResult result = template.sendBatch("orders", requests);

		verify(client, org.mockito.Mockito.times(2)).putRecords(any(PutRecordsRequest.class));
		assertThat(result.failed()).singleElement()
				.satisfies(failed -> assertThat(failed.errorCode()).isEqualTo("InternalFailure"));
	}

	@Test
	@DisplayName("without a back-off a retryable failure is reported without retrying")
	void noRetryWithoutBackOff() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		when(client.putRecords(any(PutRecordsRequest.class))).thenReturn(CompletableFuture.completedFuture(
				PutRecordsResponse.builder().failedRecordCount(1).records(failure("InternalFailure", "boom")).build()));
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());

		List<SendRequest> requests = List
				.of(SendRequest.builder().streamName("orders").partitionKey("pk-0").payload("v0").build());

		BatchSendResult result = template.sendBatch("orders", requests);

		verify(client, org.mockito.Mockito.times(1)).putRecords(any(PutRecordsRequest.class));
		assertThat(result.failed()).hasSize(1);
	}

	@Test
	@DisplayName("sendBatch reports partial failures without retrying")
	void sendBatchReportsPartialFailuresWithoutRetrying() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		PutRecordsResponse response = PutRecordsResponse.builder().failedRecordCount(1)
				.records(ok("seq-a", "shard-0"), failure("InternalFailure", "boom")).build();
		when(client.putRecords(any(PutRecordsRequest.class))).thenReturn(CompletableFuture.completedFuture(response));

		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());

		List<SendRequest> requests = List.of(
				SendRequest.builder().streamName("orders").partitionKey("pk-0").payload("v0").build(),
				SendRequest.builder().streamName("orders").partitionKey("pk-1").payload("v1").build());

		BatchSendResult result = template.sendBatch("orders", requests);

		verify(client, org.mockito.Mockito.times(1)).putRecords(any(PutRecordsRequest.class));
		assertThat(result.successful()).extracting(SendResult::sequenceNumber).containsExactly("seq-a");
		assertThat(result.hasFailures()).isTrue();
		assertThat(result.failed()).hasSize(1);
		assertThat(result.failed().get(0).partitionKey()).isEqualTo("pk-1");
		assertThat(result.failed().get(0).errorCode()).isEqualTo("InternalFailure");
		assertThat(result.failed().get(0).payload()).isEqualTo("v1");
	}

	@Test
	void sendBatchAsyncSnapshotsRequests() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		CompletableFuture<PutRecordsResponse> responseFuture = new CompletableFuture<>();
		when(client.putRecords(any(PutRecordsRequest.class))).thenReturn(responseFuture);
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());
		SendRequest original = SendRequest.builder().streamName("orders").partitionKey("original-pk")
				.payload("original-payload").build();
		List<SendRequest> requests = new ArrayList<>(List.of(original));

		CompletableFuture<BatchSendResult> resultFuture = template.sendBatchAsync("orders", requests);
		requests.clear();
		requests.add(SendRequest.builder().streamName("orders").partitionKey("replacement-pk")
				.payload("replacement-payload").build());
		responseFuture.complete(
				PutRecordsResponse.builder().failedRecordCount(1).records(failure("InternalFailure", "boom")).build());

		BatchSendResult result = resultFuture.join();
		assertThat(result.failed()).singleElement().satisfies(failed -> {
			assertThat(failed.partitionKey()).isEqualTo("original-pk");
			assertThat(failed.payload()).isEqualTo("original-payload");
		});
	}

	@Test
	@DisplayName("entries() keeps every request in order and marks success or failure across two chunks")
	void entriesPreserveRequestOrderAcrossChunks() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		// 700 records split into chunks of 500 and 200. In each chunk fail the records at even
		// within-chunk positions, so successes and failures interleave across both chunks.
		when(client.putRecords(any(PutRecordsRequest.class))).thenAnswer(invocation -> {
			List<PutRecordsRequestEntry> records = ((PutRecordsRequest) invocation.getArgument(0)).records();
			PutRecordsResultEntry[] entries = new PutRecordsResultEntry[records.size()];
			int failedCount = 0;
			for (int i = 0; i < records.size(); i++) {
				if (i % 2 == 0) {
					entries[i] = failure("InternalFailure", "boom");
					failedCount++;
				}
				else {
					entries[i] = ok(records.get(i).partitionKey(), "shard-0");
				}
			}
			return CompletableFuture.completedFuture(
					PutRecordsResponse.builder().failedRecordCount(failedCount).records(entries).build());
		});
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());

		List<SendRequest> requests = IntStream.range(0, 700).mapToObj(
				i -> SendRequest.builder().streamName("orders").partitionKey("pk-" + i).payload("p-" + i).build())
				.collect(Collectors.toList());

		BatchSendResult result = template.sendBatch("orders", requests);

		assertThat(result.entries()).hasSize(700);
		for (int i = 0; i < 700; i++) {
			BatchSendResult.Entry entry = result.entries().get(i);
			assertThat(entry.index()).isEqualTo(i);
			assertThat(entry.request().partitionKey()).isEqualTo("pk-" + i);
			// position within its chunk (500/200 split) decides success: even -> failure.
			int withinChunk = (i < 500) ? i : (i - 500);
			if (withinChunk % 2 == 0) {
				assertThat(entry.isSuccessful()).isFalse();
				assertThat(entry.result()).isNull();
				assertThat(entry.failure()).isNotNull();
				assertThat(entry.failure().partitionKey()).isEqualTo("pk-" + i);
			}
			else {
				assertThat(entry.isSuccessful()).isTrue();
				assertThat(entry.failure()).isNull();
				assertThat(entry.result().sequenceNumber()).isEqualTo("pk-" + i);
			}
		}
		assertThat(result.successful()).hasSize(350);
		assertThat(result.failed()).hasSize(350);
		assertThat(result.hasFailures()).isTrue();
	}

	@Test
	@DisplayName("a retry that turns a failure into a success keeps its request-order index and is reported once")
	void retriedSuccessKeepsIndex() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		// First call: pk-0 ok, pk-1 retryable failure, pk-2 ok. Resend of pk-1: ok.
		when(client.putRecords(any(PutRecordsRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(PutRecordsResponse.builder().failedRecordCount(1)
						.records(ok("seq-0", "shard-0"), failure("ProvisionedThroughputExceededException", "slow down"),
								ok("seq-2", "shard-0"))
						.build()))
				.thenReturn(CompletableFuture.completedFuture(
						PutRecordsResponse.builder().failedRecordCount(0).records(ok("seq-1", "shard-0")).build()));
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());
		template.setBatchRetryBackOff(new org.springframework.util.backoff.FixedBackOff(1, 3));

		List<SendRequest> requests = List.of(
				SendRequest.builder().streamName("orders").partitionKey("pk-0").payload("v0").build(),
				SendRequest.builder().streamName("orders").partitionKey("pk-1").payload("v1").build(),
				SendRequest.builder().streamName("orders").partitionKey("pk-2").payload("v2").build());

		BatchSendResult result = template.sendBatch("orders", requests);

		assertThat(result.entries()).hasSize(3);
		assertThat(result.entries()).extracting(BatchSendResult.Entry::index).containsExactly(0, 1, 2);
		assertThat(result.entries()).allMatch(BatchSendResult.Entry::isSuccessful);
		BatchSendResult.Entry retried = result.entries().get(1);
		assertThat(retried.request().partitionKey()).isEqualTo("pk-1");
		assertThat(retried.result().sequenceNumber()).isEqualTo("seq-1");
		assertThat(result.hasFailures()).isFalse();
		assertThat(result.successful()).extracting(SendResult::sequenceNumber).containsExactly("seq-0", "seq-1",
				"seq-2");
	}

	/**
	 * Builds a success response that echoes each entry's partition key as its sequence number, so that merged results
	 * can be asserted to be in request order.
	 */
	private static PutRecordsResponse echoResponse(List<PutRecordsRequestEntry> records) {
		PutRecordsResultEntry[] entries = records.stream().map(record -> ok(record.partitionKey(), "shard-0"))
				.toArray(PutRecordsResultEntry[]::new);
		return PutRecordsResponse.builder().failedRecordCount(0).records(entries).build();
	}

	private static PutRecordsResultEntry ok(String sequenceNumber, String shardId) {
		return PutRecordsResultEntry.builder().sequenceNumber(sequenceNumber).shardId(shardId).build();
	}

	private static PutRecordsResultEntry failure(String errorCode, String errorMessage) {
		return PutRecordsResultEntry.builder().errorCode(errorCode).errorMessage(errorMessage).build();
	}

	record Order(String id) {
	}

}
