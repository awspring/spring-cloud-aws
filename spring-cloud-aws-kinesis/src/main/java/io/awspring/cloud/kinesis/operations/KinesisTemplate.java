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

import io.awspring.cloud.kinesis.operations.BatchSendResult.Entry;
import io.awspring.cloud.kinesis.operations.BatchSendResult.FailedRecord;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageConverter;
import io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter;
import io.awspring.cloud.kinesis.support.observation.KinesisTemplateObservation;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResultEntry;
import tools.jackson.databind.json.JsonMapper;

/**
 * Default {@link KinesisOperations} implementation, sending records through a {@link KinesisAsyncClient}.
 * <p>
 * Synchronous methods delegate to their asynchronous counterparts and unwrap the {@link CompletionException} so callers
 * see the original AWS exception. A batch is split into {@code PutRecords} calls of at most 500 records and 5 MiB each,
 * a conservative size below the current 10 MiB {@code PutRecords} limit; the chunks are sent sequentially and their
 * results merged in request order.
 * <p>
 * When an {@link ObservationRegistry} is set, every send and batch send is recorded as a
 * {@link KinesisTemplateObservation} named {@code spring.aws.kinesis.template}. A split batch is still recorded as one
 * observation carrying the total record and failed counts.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class KinesisTemplate implements KinesisOperations {

	/**
	 * Maximum number of records a single {@code PutRecords} call accepts.
	 */
	private static final int MAX_RECORDS_PER_REQUEST = 500;

	/**
	 * Maximum aggregate size of a single {@code PutRecords} request, counted as the sum over every entry of the data
	 * bytes plus the UTF-8 bytes of its partition key (5 MiB).
	 */
	private static final int MAX_REQUEST_SIZE_BYTES = 5 * 1024 * 1024;

	/**
	 * Error codes that a configured back-off resends; other error codes are reported without a retry.
	 */
	private static final Set<String> RETRYABLE_ERROR_CODES = Set.of("ProvisionedThroughputExceededException",
			"InternalFailure");

	private final KinesisAsyncClient kinesisAsyncClient;

	private final KinesisMessageConverter messageConverter;

	private static final KinesisTemplateObservation.DefaultConvention DEFAULT_OBSERVATION_CONVENTION = new KinesisTemplateObservation.DefaultConvention();

	private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

	private KinesisTemplateObservation.@Nullable Convention observationConvention;

	private @Nullable BackOff batchRetryBackOff;

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

	/**
	 * Set the {@link ObservationRegistry} send operations are observed with. Defaults to
	 * {@link ObservationRegistry#NOOP}.
	 * @param observationRegistry the registry.
	 */
	public void setObservationRegistry(ObservationRegistry observationRegistry) {
		Assert.notNull(observationRegistry, "observationRegistry must not be null");
		this.observationRegistry = observationRegistry;
	}

	/**
	 * Set a custom {@link KinesisTemplateObservation.Convention} overriding the default observation name and key
	 * values.
	 * @param observationConvention the convention.
	 */
	public void setObservationConvention(KinesisTemplateObservation.Convention observationConvention) {
		Assert.notNull(observationConvention, "observationConvention must not be null");
		this.observationConvention = observationConvention;
	}

	/**
	 * Set a {@link BackOff} used to resend batch entries that failed with a retryable error code
	 * ({@code ProvisionedThroughputExceededException}, {@code InternalFailure}). The failed entries are resent in a new
	 * {@code PutRecords} call after each back-off interval, without blocking a thread, until none remain or the
	 * back-off stops; entries with other error codes are never retried. The merged {@link BatchSendResult} reports each
	 * record once: successful if any attempt succeeded, otherwise with its last error. Defaults to {@code null}, which
	 * disables retrying.
	 * <p>
	 * Retried records can be written after records that followed them in the batch, so per-shard order is not kept for
	 * retried records. The back-off must be bounded.
	 * @param batchRetryBackOff the back-off, or {@code null} to disable retrying.
	 */
	public void setBatchRetryBackOff(@Nullable BackOff batchRetryBackOff) {
		this.batchRetryBackOff = batchRetryBackOff;
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
		KinesisTemplateObservation.Context context = KinesisTemplateObservation.Context.forRecord(request.streamName(),
				request.partitionKey());
		return observe(context, () -> {
			SdkBytes data = SdkBytes.fromByteArray(serialize(request.payload(), context));
			return this.kinesisAsyncClient
					.putRecord(builder -> builder.streamName(request.streamName()).partitionKey(request.partitionKey())
							.explicitHashKey(request.explicitHashKey())
							.sequenceNumberForOrdering(request.sequenceNumberForOrdering()).data(data))
					.thenApply(response -> {
						context.setRecordResult(response.sequenceNumber(), response.shardId());
						return new SendResult(response.sequenceNumber(), response.shardId());
					});
		});
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
		List<SendRequest> requestSnapshot = new ArrayList<>(requests);
		for (SendRequest request : requestSnapshot) {
			Assert.notNull(request, "request must not be null");
			Assert.isTrue(request.streamName().equals(streamName),
					"every request must target the batch streamName " + streamName);
		}
		KinesisTemplateObservation.Context context = KinesisTemplateObservation.Context.forBatch(streamName,
				requestSnapshot.size());
		return observe(context, () -> {
			List<RecordEntry> records = new ArrayList<>(requestSnapshot.size());
			for (int i = 0; i < requestSnapshot.size(); i++) {
				SendRequest request = requestSnapshot.get(i);
				records.add(toRecordEntry(i, request, serialize(request.payload(), context)));
			}
			List<List<RecordEntry>> chunks = splitIntoChunks(records);
			return sendChunksSequentially(streamName, chunks).thenApply(outcomes -> {
				BatchSendResult result = mergeOutcomes(outcomes);
				context.setFailedRecordCount(result.failed().size());
				return result;
			});
		});
	}

	/**
	 * Splits the records into chunks of at most {@value #MAX_RECORDS_PER_REQUEST} records and
	 * {@value #MAX_REQUEST_SIZE_BYTES} bytes (data plus partition-key bytes of each entry), a conservative size below
	 * the current 10 MiB {@code PutRecords} limit. A single record on its own exceeding that chunk size is left in its
	 * own chunk rather than being split; a record above Kinesis' record size limit is rejected by Kinesis as a failed
	 * record.
	 */
	private static List<List<RecordEntry>> splitIntoChunks(List<RecordEntry> records) {
		List<List<RecordEntry>> chunks = new ArrayList<>();
		List<RecordEntry> current = new ArrayList<>();
		long currentSize = 0;
		for (RecordEntry record : records) {
			boolean wouldExceed = current.size() >= MAX_RECORDS_PER_REQUEST
					|| (!current.isEmpty() && currentSize + record.sizeBytes() > MAX_REQUEST_SIZE_BYTES);
			if (wouldExceed) {
				chunks.add(current);
				current = new ArrayList<>();
				currentSize = 0;
			}
			current.add(record);
			currentSize += record.sizeBytes();
		}
		if (!current.isEmpty()) {
			chunks.add(current);
		}
		return chunks;
	}

	/**
	 * Sends the chunks one after another, starting the next chunk only once the previous future has completed, so that
	 * the records of a shard keep their request order. If a chunk call fails as a whole, the returned future fails with
	 * that exception; chunks already sent may have been written.
	 */
	private CompletableFuture<List<RecordOutcome>> sendChunksSequentially(String streamName,
			List<List<RecordEntry>> chunks) {
		CompletableFuture<List<RecordOutcome>> future = CompletableFuture.completedFuture(new ArrayList<>());
		for (List<RecordEntry> chunk : chunks) {
			future = future.thenCompose(acc -> sendChunkWithRetries(streamName, chunk).thenApply(outcomes -> {
				acc.addAll(outcomes);
				return acc;
			}));
		}
		return future;
	}

	/**
	 * Sends one chunk and, when a back-off is configured, resends the entries that failed with a retryable error code
	 * after each back-off interval, without blocking a thread, until none remain or the back-off stops.
	 */
	private CompletableFuture<List<RecordOutcome>> sendChunkWithRetries(String streamName, List<RecordEntry> chunk) {
		return putRecords(streamName, chunk).thenCompose(outcomes -> {
			if (this.batchRetryBackOff == null) {
				return CompletableFuture.completedFuture(outcomes);
			}
			return retryFailures(streamName, outcomes, this.batchRetryBackOff.start());
		});
	}

	private CompletableFuture<List<RecordOutcome>> retryFailures(String streamName, List<RecordOutcome> outcomes,
			BackOffExecution execution) {
		List<RecordOutcome> retryable = new ArrayList<>();
		for (RecordOutcome outcome : outcomes) {
			if (outcome.isRetryable()) {
				retryable.add(outcome);
			}
		}
		if (retryable.isEmpty()) {
			return CompletableFuture.completedFuture(outcomes);
		}
		long waitMillis = execution.nextBackOff();
		if (waitMillis == BackOffExecution.STOP) {
			return CompletableFuture.completedFuture(outcomes);
		}
		Executor delayed = CompletableFuture.delayedExecutor(waitMillis, TimeUnit.MILLISECONDS);
		List<RecordEntry> resend = new ArrayList<>(retryable.size());
		for (RecordOutcome outcome : retryable) {
			resend.add(outcome.entry());
		}
		return CompletableFuture.supplyAsync(() -> resend, delayed)
				.thenCompose(entries -> putRecords(streamName, entries)).thenCompose(resendOutcomes -> {
					for (int i = 0; i < retryable.size(); i++) {
						retryable.get(i).updateFrom(resendOutcomes.get(i));
					}
					return retryFailures(streamName, outcomes, execution);
				});
	}

	/**
	 * Sends the entries in a single {@code PutRecords} call and pairs each result with its request entry in order.
	 */
	private CompletableFuture<List<RecordOutcome>> putRecords(String streamName, List<RecordEntry> entries) {
		List<PutRecordsRequestEntry> putEntries = new ArrayList<>(entries.size());
		for (RecordEntry entry : entries) {
			putEntries.add(entry.toPutRecordsRequestEntry());
		}
		PutRecordsRequest putRecordsRequest = PutRecordsRequest.builder().streamName(streamName).records(putEntries)
				.build();
		return this.kinesisAsyncClient.putRecords(putRecordsRequest).thenApply(response -> {
			List<RecordOutcome> outcomes = new ArrayList<>(entries.size());
			List<PutRecordsResultEntry> results = response.records();
			for (int i = 0; i < entries.size(); i++) {
				outcomes.add(RecordOutcome.from(entries.get(i), results.get(i)));
			}
			return outcomes;
		});
	}

	private static BatchSendResult mergeOutcomes(List<RecordOutcome> outcomes) {
		List<RecordOutcome> ordered = new ArrayList<>(outcomes);
		ordered.sort((left, right) -> Integer.compare(left.entry().index(), right.entry().index()));
		List<Entry> entries = new ArrayList<>(ordered.size());
		for (RecordOutcome outcome : ordered) {
			RecordEntry entry = outcome.entry();
			if (outcome.isSuccessful()) {
				entries.add(new Entry(entry.index(), entry.request(),
						new SendResult(outcome.sequenceNumber(), outcome.shardId()), null));
			}
			else {
				FailedRecord failure = new FailedRecord(entry.payload(), entry.partitionKey(), outcome.errorCode(),
						outcome.errorMessage());
				entries.add(new Entry(entry.index(), entry.request(), null, failure));
			}
		}
		return new BatchSendResult(entries);
	}

	private RecordEntry toRecordEntry(int index, SendRequest request, byte[] serialized) {
		int size = serialized.length + request.partitionKey().getBytes(StandardCharsets.UTF_8).length;
		return new RecordEntry(index, request, serialized, size);
	}

	/**
	 * One request entry: its request-order index, the original request (for the result), the serialized data
	 * (serialized once and reused across retries) and its counted size.
	 */
	private record RecordEntry(int index, SendRequest request, byte[] data, int sizeBytes) {

		Object payload() {
			return this.request.payload();
		}

		String partitionKey() {
			return this.request.partitionKey();
		}

		@Nullable
		String explicitHashKey() {
			return this.request.explicitHashKey();
		}

		PutRecordsRequestEntry toPutRecordsRequestEntry() {
			return PutRecordsRequestEntry.builder().partitionKey(partitionKey()).explicitHashKey(explicitHashKey())
					.data(SdkBytes.fromByteArray(this.data)).build();
		}
	}

	/**
	 * The latest result for one request entry across attempts. Mutable so a retry overwrites the previous result while
	 * the entry keeps its request-order position.
	 */
	private static final class RecordOutcome {

		private final RecordEntry entry;

		@Nullable
		private String sequenceNumber;

		@Nullable
		private String shardId;

		@Nullable
		private String errorCode;

		@Nullable
		private String errorMessage;

		private RecordOutcome(RecordEntry entry) {
			this.entry = entry;
		}

		static RecordOutcome from(RecordEntry entry, PutRecordsResultEntry result) {
			RecordOutcome outcome = new RecordOutcome(entry);
			outcome.apply(result);
			return outcome;
		}

		void updateFrom(RecordOutcome other) {
			this.sequenceNumber = other.sequenceNumber;
			this.shardId = other.shardId;
			this.errorCode = other.errorCode;
			this.errorMessage = other.errorMessage;
		}

		private void apply(PutRecordsResultEntry result) {
			if (result.errorCode() != null) {
				this.errorCode = result.errorCode();
				this.errorMessage = result.errorMessage();
				this.sequenceNumber = null;
				this.shardId = null;
			}
			else {
				this.sequenceNumber = result.sequenceNumber();
				this.shardId = result.shardId();
				this.errorCode = null;
				this.errorMessage = null;
			}
		}

		boolean isSuccessful() {
			return this.errorCode == null;
		}

		boolean isRetryable() {
			return this.errorCode != null && RETRYABLE_ERROR_CODES.contains(this.errorCode);
		}

		RecordEntry entry() {
			return this.entry;
		}

		@Nullable
		String sequenceNumber() {
			return this.sequenceNumber;
		}

		@Nullable
		String shardId() {
			return this.shardId;
		}

		@Nullable
		String errorCode() {
			return this.errorCode;
		}

		@Nullable
		String errorMessage() {
			return this.errorMessage;
		}

	}

	/**
	 * Runs the send inside an observation, stopping it when the returned future completes. A synchronous failure (for
	 * example during serialization) is recorded and rethrown.
	 */
	private <T> CompletableFuture<T> observe(KinesisTemplateObservation.Context context,
			Supplier<CompletableFuture<T>> send) {
		Observation observation = KinesisTemplateObservation.Documentation.TEMPLATE_OBSERVATION
				.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> context,
						this.observationRegistry)
				.parentObservation(this.observationRegistry.getCurrentObservation()).start();
		CompletableFuture<T> future;
		try (Observation.Scope scope = observation.openScope()) {
			future = send.get();
		}
		catch (RuntimeException ex) {
			observation.error(ex);
			observation.stop();
			throw ex;
		}
		return future.whenComplete((result, ex) -> {
			if (ex != null) {
				observation.error(ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex);
			}
			observation.stop();
		});
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

	/**
	 * Serializes the payload, adding the trace-propagation headers the observation injected so that a converter which
	 * embeds headers (see {@code EmbeddedHeadersKinesisMessageConverter}) carries them to the consumer. Converters that
	 * ignore headers drop them, so plain records are unaffected.
	 */
	private byte[] serialize(Object payload, KinesisTemplateObservation.Context context) {
		MessageBuilder<Object> builder = MessageBuilder.withPayload(payload);
		context.getPropagationHeaders().forEach(builder::setHeaderIfAbsent);
		return this.messageConverter.fromMessagingMessage(builder.build());
	}

}
