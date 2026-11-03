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
package io.awspring.cloud.kinesis.listener;

import io.awspring.cloud.kinesis.listener.checkpoint.CheckpointException;
import io.awspring.cloud.kinesis.listener.checkpoint.DefaultKclCheckpointer;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointer;
import io.awspring.cloud.kinesis.listener.checkpoint.LeaseLostCheckpointException;
import io.awspring.cloud.kinesis.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.kinesis.listener.errorhandler.FailedDelivery;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageConverter;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders;
import io.awspring.cloud.kinesis.support.observation.KclListenerObservation;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

/**
 * KCL {@link ShardRecordProcessor} dispatching the records of one shard to the configured listener.
 * <p>
 * Failures are never propagated to KCL, because KCL logs an exception thrown from {@code processRecords} and moves on
 * to the next batch, silently skipping the failed records. Instead every failure is handed to the {@link ErrorHandler}:
 * if it returns normally the failure is considered resolved and processing continues; if it throws, the shard is halted
 * - no further records are delivered and no further checkpoint is taken - so the records after the last checkpoint are
 * redelivered once the lease is acquired again.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
class KclShardRecordProcessor implements ShardRecordProcessor {

	private static final Logger logger = LoggerFactory.getLogger(KclShardRecordProcessor.class);

	private static final KclListenerObservation.DefaultConvention DEFAULT_OBSERVATION_CONVENTION = new KclListenerObservation.DefaultConvention();

	private final KinesisMessageConverter converter;

	@Nullable
	private final MimeType payloadContentType;

	private final KclListenerMode listenerMode;

	private final KclCheckpointMode checkpointMode;

	private final long checkpointRecordCount;

	private final Duration checkpointInterval;

	@Nullable
	private final MessageListener messageListener;

	@Nullable
	private final BatchMessageListener batchMessageListener;

	private final ErrorHandler errorHandler;

	private final String streamName;

	private final ObservationRegistry observationRegistry;

	private final KclListenerObservation.@Nullable Convention observationConvention;

	@Nullable
	private final String leaseKeyPrefix;

	@Nullable
	private final Duration haltedShardReleaseDelay;

	@Nullable
	private final ShardReleaser shardReleaser;

	@Nullable
	private final ScheduledExecutorService releaseScheduler;

	@Nullable
	private final ShardHaltCallback haltCallback;

	@Nullable
	private String shardId;

	private long recordsSinceCheckpoint;

	private long lastCheckpointEpochMillis;

	@Nullable
	private volatile Long millisBehindLatest;

	private volatile boolean halted;

	@Nullable
	private volatile ScheduledFuture<?> pendingRelease;

	KclShardRecordProcessor(KinesisMessageConverter converter, KclListenerMode listenerMode,
			KclCheckpointMode checkpointMode, long checkpointRecordCount, Duration checkpointInterval,
			@Nullable MessageListener messageListener, @Nullable BatchMessageListener batchMessageListener,
			ErrorHandler errorHandler, String streamName) {
		this(converter, null, listenerMode, checkpointMode, checkpointRecordCount, checkpointInterval, messageListener,
				batchMessageListener, errorHandler, streamName);
	}

	/**
	 * @param converter converts each record into the message handed to the listener.
	 * @param payloadContentType added as {@code contentType} header to converted messages that have none, or
	 *     {@code null} to leave the headers as the converter produced them.
	 */
	KclShardRecordProcessor(KinesisMessageConverter converter, @Nullable MimeType payloadContentType,
			KclListenerMode listenerMode, KclCheckpointMode checkpointMode, long checkpointRecordCount,
			Duration checkpointInterval, @Nullable MessageListener messageListener,
			@Nullable BatchMessageListener batchMessageListener, ErrorHandler errorHandler, String streamName) {
		this(converter, payloadContentType, listenerMode, checkpointMode, checkpointRecordCount, checkpointInterval,
				messageListener, batchMessageListener, errorHandler, streamName, ObservationRegistry.NOOP, null, null,
				null, null, null, null);
	}

	/**
	 * Convenience overload without observation: defaults to {@link ObservationRegistry#NOOP} and the default
	 * convention.
	 * @param leaseKeyPrefix the serialized {@code StreamIdentifier} for a multi-stream application, prepended with
	 *     {@code ':'} to the shard id to form the lease key, or {@code null} for a single-stream application.
	 * @param haltedShardReleaseDelay how long after a halt the lease is released, or {@code null} to keep the lease.
	 * @param shardReleaser releases the lease, or {@code null} when releasing is disabled.
	 * @param releaseScheduler schedules the release off the shard thread, or {@code null} when releasing is disabled.
	 * @param haltCallback notified when the shard halts, or {@code null} to ignore halts.
	 */
	KclShardRecordProcessor(KinesisMessageConverter converter, @Nullable MimeType payloadContentType,
			KclListenerMode listenerMode, KclCheckpointMode checkpointMode, long checkpointRecordCount,
			Duration checkpointInterval, @Nullable MessageListener messageListener,
			@Nullable BatchMessageListener batchMessageListener, ErrorHandler errorHandler, String streamName,
			@Nullable String leaseKeyPrefix, @Nullable Duration haltedShardReleaseDelay,
			@Nullable ShardReleaser shardReleaser, @Nullable ScheduledExecutorService releaseScheduler,
			@Nullable ShardHaltCallback haltCallback) {
		this(converter, payloadContentType, listenerMode, checkpointMode, checkpointRecordCount, checkpointInterval,
				messageListener, batchMessageListener, errorHandler, streamName, ObservationRegistry.NOOP, null,
				leaseKeyPrefix, haltedShardReleaseDelay, shardReleaser, releaseScheduler, haltCallback);
	}

	/**
	 * @param observationRegistry the registry used to observe each listener invocation, or
	 *     {@link ObservationRegistry#NOOP} to disable observation.
	 * @param observationConvention a convention overriding the default observation name and key values, or {@code null}
	 *     to use the default.
	 * @param leaseKeyPrefix the serialized {@code StreamIdentifier} for a multi-stream application, prepended with
	 *     {@code ':'} to the shard id to form the lease key, or {@code null} for a single-stream application whose
	 *     lease key is the shard id.
	 * @param haltedShardReleaseDelay how long after a halt the lease is released, or {@code null} to keep the lease.
	 * @param shardReleaser releases the lease, or {@code null} when releasing is disabled.
	 * @param releaseScheduler schedules the release off the shard thread, or {@code null} when releasing is disabled.
	 * @param haltCallback notified when the shard halts, or {@code null} to ignore halts.
	 */
	KclShardRecordProcessor(KinesisMessageConverter converter, @Nullable MimeType payloadContentType,
			KclListenerMode listenerMode, KclCheckpointMode checkpointMode, long checkpointRecordCount,
			Duration checkpointInterval, @Nullable MessageListener messageListener,
			@Nullable BatchMessageListener batchMessageListener, ErrorHandler errorHandler, String streamName,
			ObservationRegistry observationRegistry, KclListenerObservation.@Nullable Convention observationConvention,
			@Nullable String leaseKeyPrefix, @Nullable Duration haltedShardReleaseDelay,
			@Nullable ShardReleaser shardReleaser, @Nullable ScheduledExecutorService releaseScheduler,
			@Nullable ShardHaltCallback haltCallback) {
		this.converter = converter;
		this.payloadContentType = payloadContentType;
		this.listenerMode = listenerMode;
		this.checkpointMode = checkpointMode;
		this.checkpointRecordCount = checkpointRecordCount;
		this.checkpointInterval = checkpointInterval;
		this.messageListener = messageListener;
		this.batchMessageListener = batchMessageListener;
		this.errorHandler = errorHandler;
		this.streamName = streamName;
		this.observationRegistry = observationRegistry;
		this.observationConvention = observationConvention;
		this.leaseKeyPrefix = leaseKeyPrefix;
		this.haltedShardReleaseDelay = haltedShardReleaseDelay;
		this.shardReleaser = shardReleaser;
		this.releaseScheduler = releaseScheduler;
		this.haltCallback = haltCallback;
	}

	@Override
	public void initialize(InitializationInput initializationInput) {
		this.shardId = initializationInput.shardId();
		this.lastCheckpointEpochMillis = System.currentTimeMillis();
		if (this.haltCallback != null) {
			this.haltCallback.onProcessorInitialized(this.shardId);
		}
		logger.debug("Initializing record processor for shard {} at {}", this.shardId,
				initializationInput.extendedSequenceNumber());
	}

	/**
	 * Whether an unresolved failure halted this shard.
	 * @return {@code true} once the shard is halted.
	 */
	boolean isHalted() {
		return this.halted;
	}

	/**
	 * This shard's id, once {@link #initialize(InitializationInput)} has run.
	 * @return the shard id, or {@code null} before initialization.
	 */
	@Nullable
	String getShardId() {
		return this.shardId;
	}

	@Override
	public void processRecords(ProcessRecordsInput processRecordsInput) {
		this.millisBehindLatest = processRecordsInput.millisBehindLatest();
		if (this.halted) {
			logger.debug("Shard {} of stream {} is halted, ignoring {} records", this.shardId, this.streamName,
					processRecordsInput.records().size());
			return;
		}
		try {
			doProcessRecords(processRecordsInput);
		}
		catch (Throwable ex) {
			// Never let an exception reach KCL: it would skip the records and continue with the next batch.
			halt(ex);
		}
	}

	private void doProcessRecords(ProcessRecordsInput processRecordsInput) {
		List<KinesisClientRecord> records = processRecordsInput.records();
		if (records.isEmpty()) {
			checkpointIdlePeriodicIfDue(processRecordsInput);
			return;
		}
		boolean singleRecord = this.listenerMode != KclListenerMode.BATCH;
		KclCheckpointer checkpointer = new DefaultKclCheckpointer(processRecordsInput.checkpointer());
		List<Message<?>> messages = new ArrayList<>(records.size());
		for (KinesisClientRecord record : records) {
			try {
				messages.add(toListenerMessage(record, checkpointer, singleRecord));
			}
			catch (Exception ex) {
				Message<?> rawMessage = failedConversionMessage(record, checkpointer, singleRecord);
				if (!resolve(new FailedDelivery(rawMessage, ex, this.streamName, this.shardId, null))) {
					return;
				}
			}
		}
		if (this.listenerMode == KclListenerMode.BATCH) {
			processBatch(messages, checkpointer);
		}
		else {
			processSingle(messages, checkpointer);
		}
	}

	private void processBatch(List<Message<?>> messages, KclCheckpointer checkpointer) {
		if (!messages.isEmpty()) {
			BatchMessageListener listener = requireBatchListener();
			Observation observation = startObservation(KclListenerObservation.Context.forBatch(this.streamName,
					this.shardId, messages.size(), this.millisBehindLatest));
			boolean resolved = observe(observation, () -> listener.onMessage(messages),
					ex -> new FailedDelivery(
							withCheckpointer(MessageBuilder.withPayload(messages).build(), checkpointer), ex,
							this.streamName, this.shardId, () -> listener.onMessage(messages)));
			if (!resolved) {
				return;
			}
		}
		switch (this.checkpointMode) {
		case BATCH, RECORD -> autoCheckpoint(checkpointer, null);
		case PERIODIC -> {
			this.recordsSinceCheckpoint += messages.size();
			if (periodicCheckpointDue()) {
				autoCheckpoint(checkpointer, null);
			}
		}
		case MANUAL -> {
		}
		}
	}

	private void processSingle(List<Message<?>> messages, KclCheckpointer checkpointer) {
		MessageListener listener = requireMessageListener();
		for (Message<?> message : messages) {
			Observation observation = startObservation(singleRecordContext(message));
			boolean resolved = observe(observation, () -> listener.onMessage(message), ex -> new FailedDelivery(message,
					ex, this.streamName, this.shardId, () -> listener.onMessage(message)));
			if (!resolved) {
				return;
			}
			if (this.checkpointMode == KclCheckpointMode.RECORD) {
				autoCheckpoint(checkpointer, message);
			}
			else if (this.checkpointMode == KclCheckpointMode.PERIODIC) {
				this.recordsSinceCheckpoint++;
				if (periodicCheckpointDue()) {
					autoCheckpoint(checkpointer, message);
				}
			}
		}
		if (this.checkpointMode == KclCheckpointMode.BATCH) {
			autoCheckpoint(checkpointer, null);
		}
	}

	/**
	 * Starts the listener observation for a listener invocation. With {@link ObservationRegistry#NOOP} (the default)
	 * this is a no-op and costs nothing beyond building the context.
	 */
	private Observation startObservation(KclListenerObservation.Context context) {
		return KclListenerObservation.Documentation.LISTENER_OBSERVATION
				.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> context,
						this.observationRegistry)
				.parentObservation(this.observationRegistry.getCurrentObservation()).start();
	}

	/**
	 * Builds the single-record observation context from the converted message, reading the shard, sequence number and
	 * partition key from its Kinesis headers. The message is the propagation carrier, so trace context in embedded
	 * headers continues the producer trace.
	 */
	private KclListenerObservation.Context singleRecordContext(Message<?> message) {
		MessageHeaders headers = message.getHeaders();
		return KclListenerObservation.Context.forRecord(message, this.streamName,
				headers.get(KinesisMessageHeaders.SHARD_ID, String.class),
				headers.get(KinesisMessageHeaders.SEQUENCE_NUMBER, String.class),
				headers.get(KinesisMessageHeaders.PARTITION_KEY, String.class), this.millisBehindLatest);
	}

	/**
	 * Runs a listener invocation inside the given observation. The observation is in scope on the shard thread while
	 * the listener and, on failure, the error handler run, so calls they make (including {@code @SendTo} replies)
	 * become child observations. An error is recorded only when the failure stays unresolved; a resolved failure (the
	 * error handler returned, possibly after a retry that succeeded) is a successful observation.
	 * @param observation the started observation.
	 * @param invocation the listener invocation.
	 * @param failureFactory builds the {@link FailedDelivery} from the listener exception.
	 * @return {@code true} if the invocation succeeded or the failure was resolved, {@code false} if the shard halted.
	 */
	private boolean observe(Observation observation, ListenerInvocation invocation,
			Function<Exception, FailedDelivery> failureFactory) {
		try (Observation.Scope scope = observation.openScope()) {
			try {
				invocation.invoke();
				return true;
			}
			catch (Exception ex) {
				return resolveObserved(failureFactory.apply(ex), observation);
			}
		}
		finally {
			observation.stop();
		}
	}

	/**
	 * Hands the failure to the error handler from within an observation scope, recording the original failure on the
	 * observation when it stays unresolved.
	 */
	private boolean resolveObserved(FailedDelivery failedDelivery, Observation observation) {
		if (resolve(failedDelivery)) {
			return true;
		}
		observation.error(unwrapListenerFailure(failedDelivery.getCause()));
		return false;
	}

	/**
	 * Hands the failure to the error handler.
	 * @return {@code true} if the handler resolved the failure, {@code false} if the shard was halted.
	 */
	private boolean resolve(FailedDelivery failedDelivery) {
		try {
			this.errorHandler.handle(failedDelivery);
			return true;
		}
		catch (Throwable ex) {
			logger.warn("Halted shard {} of stream {}: the error handler left the failure unresolved; "
					+ "records after the last checkpoint are redelivered once the lease is released or reacquired",
					this.shardId, this.streamName);
			markHalted(ex);
			return false;
		}
	}

	private static Throwable unwrapListenerFailure(Throwable cause) {
		return cause instanceof ListenerExecutionFailedException && cause.getCause() != null ? cause.getCause() : cause;
	}

	/**
	 * A listener invocation that may throw a checked exception, used to wrap the single-record and batch listener calls
	 * uniformly for observation.
	 */
	@FunctionalInterface
	private interface ListenerInvocation {

		void invoke() throws Exception;

	}

	private void halt(Throwable ex) {
		logger.error(
				"Halted shard {} of stream {} after an unexpected failure; "
						+ "records after the last checkpoint are redelivered once the lease is released or reacquired",
				this.shardId, this.streamName, ex);
		markHalted(ex);
	}

	/**
	 * Marks the shard halted, notifies the halt callback once and, when a release delay is configured, schedules the
	 * lease release off the shard thread. Scheduling (rather than releasing from {@code processRecords}) is deliberate:
	 * a halted and idle shard may never receive another batch to trigger the release from.
	 */
	private void markHalted(Throwable cause) {
		if (this.halted) {
			return;
		}
		this.halted = true;
		if (this.haltCallback != null) {
			this.haltCallback.onShardHalted(this.shardId, cause, this.millisBehindLatest);
		}
		scheduleRelease();
	}

	private void scheduleRelease() {
		if (this.haltedShardReleaseDelay == null || this.shardReleaser == null || this.releaseScheduler == null
				|| this.shardId == null) {
			return;
		}
		ShardReleaser releaser = this.shardReleaser;
		// Single-stream lease key is the shard id; multi-stream is '<serialized StreamIdentifier>:<shardId>'.
		String key = this.leaseKeyPrefix != null ? this.leaseKeyPrefix + ":" + this.shardId : this.shardId;
		try {
			this.pendingRelease = this.releaseScheduler.schedule(() -> {
				logger.info("Releasing the lease of halted shard {} of stream {} so it can be reprocessed from its "
						+ "last checkpoint", this.shardId, this.streamName);
				releaser.releaseLease(key);
			}, this.haltedShardReleaseDelay.toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			logger.debug("Lease release for shard {} not scheduled, the container is stopping", this.shardId);
		}
	}

	private void cancelPendingRelease() {
		ScheduledFuture<?> release = this.pendingRelease;
		if (release != null) {
			release.cancel(false);
			this.pendingRelease = null;
		}
	}

	@Override
	public void leaseLost(LeaseLostInput leaseLostInput) {
		cancelPendingRelease();
		notifyProcessorEnded();
		logger.debug("Lease lost for shard {}", this.shardId);
	}

	@Override
	public void shardEnded(ShardEndedInput shardEndedInput) {
		if (this.halted) {
			// A halted shard reached its end. Checkpointing SHARD_END would mark the unprocessed records as done, so
			// KCL fails the shutdown task and retries it. The pending release is kept scheduled (not cancelled here):
			// releasing the lease shuts the halted consumer down and KCL rebuilds a fresh one that replays from the
			// last checkpoint, so the halted records are redelivered and their SHARD_END checkpoint is taken then.
			// Cancelling the release, as before, left a halted shard-ended consumer stuck and blocked its child shards.
			logger.error(
					"Shard {} of stream {} ended while halted; not checkpointing the end of the shard, "
							+ "the lease is released so the shard is reprocessed from its last checkpoint and its "
							+ "child shards stay blocked until the halted records are redelivered",
					this.shardId, this.streamName);
			return;
		}
		cancelPendingRelease();
		notifyProcessorEnded();
		logger.debug("Shard {} ended, checkpointing", this.shardId);
		mandatoryCheckpoint(new DefaultKclCheckpointer(shardEndedInput.checkpointer()));
	}

	@Override
	public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
		cancelPendingRelease();
		notifyProcessorEnded();
		logger.debug("Shutdown requested for shard {}", this.shardId);
		if (this.checkpointMode != KclCheckpointMode.MANUAL && !this.halted) {
			autoCheckpoint(new DefaultKclCheckpointer(shutdownRequestedInput.checkpointer()), null);
		}
	}

	private void notifyProcessorEnded() {
		if (this.haltCallback != null) {
			this.haltCallback.onProcessorEnded(this.shardId);
		}
	}

	private void checkpointIdlePeriodicIfDue(ProcessRecordsInput processRecordsInput) {
		if (this.checkpointMode == KclCheckpointMode.PERIODIC && this.recordsSinceCheckpoint > 0
				&& periodicCheckpointDue()) {
			autoCheckpoint(new DefaultKclCheckpointer(processRecordsInput.checkpointer()), null);
		}
	}

	/**
	 * Converts a record into the listener message, then overlays the canonical Kinesis headers read straight from the
	 * raw record (overriding any value a custom converter produced) and attaches the checkpointer. In single-record
	 * mode the checkpointer is bound to this record, so a manual {@code checkpoint()} commits this record's position
	 * and never a record that has not been delivered yet.
	 */
	private Message<?> toListenerMessage(KinesisClientRecord record, KclCheckpointer checkpointer,
			boolean singleRecord) {
		Message<byte[]> converted = this.converter.toMessagingMessage(record, this.shardId, this.streamName);
		Assert.state(converted != null, () -> this.converter.getClass().getName()
				+ " returned a null message for record " + record.sequenceNumber());
		MessageBuilder<byte[]> builder = MessageBuilder.fromMessage(converted);
		overlayCanonicalHeaders(builder, record);
		if (this.payloadContentType != null) {
			builder.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, this.payloadContentType);
		}
		Message<?> message = builder.build();
		return withCheckpointer(message, singleRecord ? checkpointer.boundTo(message) : checkpointer);
	}

	/**
	 * Builds the message handed to the error handler for a record that failed conversion: the raw record as payload,
	 * carrying the canonical Kinesis headers and a working checkpointer, so the {@link FailedDelivery} can be
	 * checkpointed past even though the record never became a listener message.
	 */
	private Message<?> failedConversionMessage(KinesisClientRecord record, KclCheckpointer checkpointer,
			boolean singleRecord) {
		MessageBuilder<KinesisClientRecord> builder = MessageBuilder.withPayload(record);
		overlayCanonicalHeaders(builder, record);
		Message<?> message = builder.build();
		return withCheckpointer(message, singleRecord ? checkpointer.boundTo(message) : checkpointer);
	}

	/**
	 * Overlays the canonical Kinesis headers from the raw record, overriding whatever a custom converter set, so the
	 * checkpointer and observations read the record's true position. A custom converter controls the message body and
	 * its own headers, but must not be able to spoof or drop the headers checkpointing depends on.
	 */
	private void overlayCanonicalHeaders(MessageBuilder<?> builder, KinesisClientRecord record) {
		builder.setHeader(KinesisMessageHeaders.PARTITION_KEY, record.partitionKey());
		builder.setHeader(KinesisMessageHeaders.SEQUENCE_NUMBER, record.sequenceNumber());
		builder.setHeader(KinesisMessageHeaders.SUBSEQUENCE_NUMBER, record.subSequenceNumber());
		builder.setHeader(KinesisMessageHeaders.AGGREGATED, record.aggregated());
		if (record.approximateArrivalTimestamp() != null) {
			builder.setHeader(KinesisMessageHeaders.APPROXIMATE_ARRIVAL_TIMESTAMP,
					record.approximateArrivalTimestamp());
		}
		if (this.shardId != null) {
			builder.setHeader(KinesisMessageHeaders.SHARD_ID, this.shardId);
		}
		builder.setHeader(KinesisMessageHeaders.STREAM_NAME, this.streamName);
	}

	private Message<?> withCheckpointer(Message<?> message, KclCheckpointer checkpointer) {
		return MessageBuilder.fromMessage(message).setHeader(KinesisMessageHeaders.CHECKPOINTER, checkpointer).build();
	}

	private void mandatoryCheckpoint(KclCheckpointer checkpointer) {
		try {
			checkpointer.checkpoint();
			resetPeriodicState();
		}
		catch (LeaseLostCheckpointException ex) {
			logger.debug("Lease for shard {} was lost before the end-of-shard checkpoint", this.shardId);
		}
		catch (CheckpointException ex) {
			logger.error("Failed the mandatory end-of-shard checkpoint for shard {}; "
					+ "child shard leases cannot be created until it succeeds", this.shardId, ex);
			throw ex;
		}
	}

	private void autoCheckpoint(KclCheckpointer checkpointer, @Nullable Message<?> message) {
		try {
			if (message != null) {
				checkpointer.checkpoint(message);
			}
			else {
				checkpointer.checkpoint();
			}
			resetPeriodicState();
		}
		catch (LeaseLostCheckpointException ex) {
			logger.debug("Lease for shard {} was lost before checkpointing", this.shardId);
		}
		catch (CheckpointException ex) {
			logger.warn("Unable to checkpoint shard {}: {}", this.shardId, ex.getMessage());
		}
	}

	private boolean periodicCheckpointDue() {
		return this.recordsSinceCheckpoint >= this.checkpointRecordCount
				|| System.currentTimeMillis() - this.lastCheckpointEpochMillis >= this.checkpointInterval.toMillis();
	}

	private void resetPeriodicState() {
		this.recordsSinceCheckpoint = 0;
		this.lastCheckpointEpochMillis = System.currentTimeMillis();
	}

	private MessageListener requireMessageListener() {
		if (this.messageListener == null) {
			throw new IllegalStateException("No MessageListener configured for SINGLE_RECORD mode");
		}
		return this.messageListener;
	}

	private BatchMessageListener requireBatchListener() {
		if (this.batchMessageListener == null) {
			throw new IllegalStateException("No BatchMessageListener configured for BATCH mode");
		}
		return this.batchMessageListener;
	}

	/**
	 * Notified of the halt and the lifecycle boundaries of one shard's processor, so the container can track the set of
	 * halted shards and publish a signal. The halt notification is the single abstract method, so a callback that only
	 * cares about halts can still be a lambda; {@link #onProcessorInitialized(String)} and
	 * {@link #onProcessorEnded(String)} let the container clear a halted shard once it recovers.
	 */
	@FunctionalInterface
	interface ShardHaltCallback {

		/**
		 * @param shardId the halted shard, or {@code null} if not known yet.
		 * @param cause the failure that halted the shard.
		 * @param millisBehindLatest the lag of the last processed batch, or {@code null} if none was processed.
		 */
		void onShardHalted(@Nullable String shardId, Throwable cause, @Nullable Long millisBehindLatest);

		/**
		 * Called when a processor for the shard initializes, so the container can clear a stale halted entry left by a
		 * previous processor of the same shard.
		 * @param shardId the shard, or {@code null} if not known yet.
		 */
		default void onProcessorInitialized(@Nullable String shardId) {
		}

		/**
		 * Called when the processor ends cleanly (its lease is lost, the shard ended after a successful end-of-shard
		 * checkpoint, or a shutdown was requested), so the container can clear the shard from the halted set. A halted
		 * shard whose end is reached does not end here: it is reprocessed first.
		 * @param shardId the shard, or {@code null} if not known yet.
		 */
		default void onProcessorEnded(@Nullable String shardId) {
		}

	}

}
