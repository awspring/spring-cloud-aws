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
import java.nio.charset.StandardCharsets;
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
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResultEntry;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Matej Nedic
 * @since 4.2.0
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
	@DisplayName("sendBatch sends all records in a single PutRecords call without chunking")
	void sendBatchSendsSingleCall() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		when(client.putRecords(any(PutRecordsRequest.class))).thenAnswer(invocation -> CompletableFuture
				.completedFuture(successResponse(((PutRecordsRequest) invocation.getArgument(0)).records().size())));
		KinesisTemplate template = new KinesisTemplate(client, new JsonMapper());

		List<SendRequest> requests = IntStream.range(0, 750).mapToObj(
				i -> SendRequest.builder().streamName("orders").partitionKey("pk-" + i).payload("p-" + i).build())
				.collect(Collectors.toList());

		BatchSendResult result = template.sendBatch("orders", requests);

		assertThat(result.hasFailures()).isFalse();
		assertThat(result.successful()).hasSize(750);
		ArgumentCaptor<PutRecordsRequest> captor = ArgumentCaptor.forClass(PutRecordsRequest.class);
		verify(client, org.mockito.Mockito.times(1)).putRecords(captor.capture());
		assertThat(captor.getValue().records()).hasSize(750);
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

	private static PutRecordsResponse successResponse(int count) {
		PutRecordsResultEntry[] entries = IntStream.range(0, count).mapToObj(i -> ok("seq-" + i, "shard-0"))
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
