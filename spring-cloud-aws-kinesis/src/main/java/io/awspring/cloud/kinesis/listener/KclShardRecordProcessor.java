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
import io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders;
import io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
class KclShardRecordProcessor implements ShardRecordProcessor {

	private static final Logger logger = LoggerFactory.getLogger(KclShardRecordProcessor.class);

	private final KinesisMessagingMessageConverter converter;

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

	@Nullable
	private String shardId;

	private long recordsSinceCheckpoint;

	private long lastCheckpointEpochMillis;

	KclShardRecordProcessor(KinesisMessagingMessageConverter converter, KclListenerMode listenerMode,
			KclCheckpointMode checkpointMode, long checkpointRecordCount, Duration checkpointInterval,
			@Nullable MessageListener messageListener, @Nullable BatchMessageListener batchMessageListener,
			ErrorHandler errorHandler, String streamName) {
		this.converter = converter;
		this.listenerMode = listenerMode;
		this.checkpointMode = checkpointMode;
		this.checkpointRecordCount = checkpointRecordCount;
		this.checkpointInterval = checkpointInterval;
		this.messageListener = messageListener;
		this.batchMessageListener = batchMessageListener;
		this.errorHandler = errorHandler;
		this.streamName = streamName;
	}

	@Override
	public void initialize(InitializationInput initializationInput) {
		this.shardId = initializationInput.shardId();
		this.lastCheckpointEpochMillis = System.currentTimeMillis();
		logger.debug("Initializing record processor for shard {} at {}", this.shardId,
				initializationInput.extendedSequenceNumber());
	}

	@Override
	public void processRecords(ProcessRecordsInput processRecordsInput) {
		List<KinesisClientRecord> records = processRecordsInput.records();
		if (records.isEmpty()) {
			checkpointIdlePeriodicIfDue(processRecordsInput);
			return;
		}
		KclCheckpointer checkpointer = new DefaultKclCheckpointer(processRecordsInput.checkpointer());
		List<Message<?>> messages = new ArrayList<>(records.size());
		for (KinesisClientRecord record : records) {
			try {
				messages.add(withCheckpointer(this.converter.toMessagingMessage(record, this.shardId, this.streamName),
						checkpointer));
			}
			catch (Exception ex) {
				this.errorHandler.handle(withCheckpointer(MessageBuilder.withPayload(record).build(), checkpointer),
						ex);
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
		try {
			requireBatchListener().onMessage(messages);
		}
		catch (Exception ex) {
			this.errorHandler.handle(MessageBuilder.withPayload(messages).build(), ex);
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
		for (Message<?> message : messages) {
			try {
				requireMessageListener().onMessage(message);
			}
			catch (Exception ex) {
				this.errorHandler.handle(message, ex);
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

	@Override
	public void leaseLost(LeaseLostInput leaseLostInput) {
		logger.debug("Lease lost for shard {}", this.shardId);
	}

	@Override
	public void shardEnded(ShardEndedInput shardEndedInput) {
		logger.debug("Shard {} ended, checkpointing", this.shardId);
		mandatoryCheckpoint(new DefaultKclCheckpointer(shardEndedInput.checkpointer()));
	}

	@Override
	public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
		logger.debug("Shutdown requested for shard {}", this.shardId);
		if (this.checkpointMode != KclCheckpointMode.MANUAL) {
			autoCheckpoint(new DefaultKclCheckpointer(shutdownRequestedInput.checkpointer()), null);
		}
	}

	private void checkpointIdlePeriodicIfDue(ProcessRecordsInput processRecordsInput) {
		if (this.checkpointMode == KclCheckpointMode.PERIODIC && this.recordsSinceCheckpoint > 0
				&& periodicCheckpointDue()) {
			autoCheckpoint(new DefaultKclCheckpointer(processRecordsInput.checkpointer()), null);
		}
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

}
