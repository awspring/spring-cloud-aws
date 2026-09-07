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

import io.awspring.cloud.kinesis.operations.BatchSendResult.FailedRecord;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageConverter;
import io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResultEntry;
import tools.jackson.databind.json.JsonMapper;

/**
 * Default {@link KinesisOperations} implementation, sending records through a {@link KinesisAsyncClient}.
 * <p>
 * Synchronous methods delegate to their asynchronous counterparts and unwrap the {@link CompletionException} so callers
 * see the original AWS exception. Batches are sent with a single {@code PutRecords} call.
 *
 * @author Matej Nedic
 * @since 4.2.0
 */
public class KinesisTemplate implements KinesisOperations {

	private final KinesisAsyncClient kinesisAsyncClient;

	private final KinesisMessageConverter messageConverter;

	/**
	 * Creates a template converting payloads with the default converter backed by the given {@link JsonMapper}.
	 * @param kinesisAsyncClient the client to send records with.
	 * @param jsonMapper the mapper used to serialize non-{@code byte[]}/{@code String} payloads.
	 */
	public KinesisTemplate(KinesisAsyncClient kinesisAsyncClient, JsonMapper jsonMapper) {
		this(kinesisAsyncClient, defaultMessageConverter(jsonMapper));
	}

	/**
	 * Creates a template converting payloads with the given converter.
	 * @param kinesisAsyncClient the client to send records with.
	 * @param messageConverter the converter used to serialize payloads.
	 */
	public KinesisTemplate(KinesisAsyncClient kinesisAsyncClient, KinesisMessageConverter messageConverter) {
		Assert.notNull(kinesisAsyncClient, "kinesisAsyncClient must not be null");
		Assert.notNull(messageConverter, "messageConverter must not be null");
		this.kinesisAsyncClient = kinesisAsyncClient;
		this.messageConverter = messageConverter;
	}

	private static KinesisMessageConverter defaultMessageConverter(JsonMapper jsonMapper) {
		Assert.notNull(jsonMapper, "jsonMapper must not be null");
		KinesisMessagingMessageConverter converter = new KinesisMessagingMessageConverter();
		converter.setPayloadMessageConverter(
				KinesisMessagingMessageConverter.defaultPayloadMessageConverter(jsonMapper));
		return converter;
	}

	@Override
	public SendResult send(String streamName, Object payload) {
		return send(streamName, UUID.randomUUID().toString(), payload);
	}

	@Override
	public SendResult send(String streamName, String partitionKey, Object payload) {
		return send(SendRequest.builder().streamName(streamName).partitionKey(partitionKey).payload(payload).build());
	}

	@Override
	public SendResult send(SendRequest request) {
		try {
			return sendAsync(request).join();
		}
		catch (CompletionException ex) {
			throw unwrap(ex, request.streamName());
		}
	}

	@Override
	public CompletableFuture<SendResult> sendAsync(String streamName, String partitionKey, Object payload) {
		return sendAsync(
				SendRequest.builder().streamName(streamName).partitionKey(partitionKey).payload(payload).build());
	}

	@Override
	public CompletableFuture<SendResult> sendAsync(SendRequest request) {
		Assert.notNull(request, "request must not be null");
		SdkBytes data = SdkBytes.fromByteArray(serialize(request.payload()));
		return this.kinesisAsyncClient
				.putRecord(builder -> builder.streamName(request.streamName()).partitionKey(request.partitionKey())
						.explicitHashKey(request.explicitHashKey())
						.sequenceNumberForOrdering(request.sequenceNumberForOrdering()).data(data))
				.thenApply(response -> new SendResult(response.sequenceNumber(), response.shardId()));
	}

	@Override
	public BatchSendResult sendBatch(String streamName, List<SendRequest> requests) {
		try {
			return sendBatchAsync(streamName, requests).join();
		}
		catch (CompletionException ex) {
			throw unwrap(ex, streamName);
		}
	}

	@Override
	public CompletableFuture<BatchSendResult> sendBatchAsync(String streamName, List<SendRequest> requests) {
		Assert.hasText(streamName, "streamName must not be empty");
		Assert.notEmpty(requests, "requests must not be empty");
		List<PutRecordsRequestEntry> entries = new ArrayList<>(requests.size());
		for (SendRequest request : requests) {
			Assert.notNull(request, "request must not be null");
			Assert.isTrue(request.streamName().equals(streamName),
					"every request must target the batch streamName " + streamName);
			entries.add(toEntry(request, serialize(request.payload())));
		}
		PutRecordsRequest putRecordsRequest = PutRecordsRequest.builder().streamName(streamName).records(entries)
				.build();
		return this.kinesisAsyncClient.putRecords(putRecordsRequest)
				.thenApply(response -> toBatchResult(response, requests));
	}

	private static BatchSendResult toBatchResult(PutRecordsResponse response, List<SendRequest> requests) {
		List<SendResult> successful = new ArrayList<>();
		List<FailedRecord> failed = new ArrayList<>();
		List<PutRecordsResultEntry> results = response.records();
		for (int i = 0; i < results.size(); i++) {
			PutRecordsResultEntry result = results.get(i);
			SendRequest request = requests.get(i);
			if (result.errorCode() != null) {
				failed.add(new FailedRecord(request.payload(), request.partitionKey(), result.errorCode(),
						result.errorMessage()));
			}
			else {
				successful.add(new SendResult(result.sequenceNumber(), result.shardId()));
			}
		}
		return new BatchSendResult(successful, failed);
	}

	private PutRecordsRequestEntry toEntry(SendRequest request, byte[] serialized) {
		return PutRecordsRequestEntry.builder().partitionKey(request.partitionKey())
				.explicitHashKey(request.explicitHashKey()).data(SdkBytes.fromByteArray(serialized)).build();
	}

	private RuntimeException unwrap(CompletionException ex, String streamName) {
		Throwable cause = ex.getCause();
		if (cause instanceof RuntimeException runtimeException) {
			return runtimeException;
		}
		if (cause == null) {
			return ex;
		}
		return new IllegalStateException("Failed to send record to stream " + streamName, cause);
	}

	private byte[] serialize(Object payload) {
		return this.messageConverter.fromMessagingMessage(MessageBuilder.withPayload(payload).build());
	}

}
