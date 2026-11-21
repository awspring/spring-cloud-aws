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

import org.springframework.messaging.Message;

/**
 * Checkpoints the progress of a shard, exposed to listeners through the
 * {@link io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders#CHECKPOINTER} header.
 * <p>
 * What {@link #checkpoint()} covers depends on the listener mode. In
 * {@link io.awspring.cloud.kinesis.listener.KclListenerMode#SINGLE_RECORD SINGLE_RECORD} mode each message carries a
 * checkpointer bound to that record, so {@code checkpoint()} commits exactly that record's position and never a record
 * that has not been delivered yet. In {@link io.awspring.cloud.kinesis.listener.KclListenerMode#BATCH BATCH} mode the
 * message carries a batch checkpointer, so {@code checkpoint()} commits the latest position of the whole delivered
 * batch. {@link #checkpoint(Message)} always commits the position of the given message.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public interface KclCheckpointer {

	/**
	 * Checkpoint the position this checkpointer is bound to: the single record it was attached to in
	 * {@link io.awspring.cloud.kinesis.listener.KclListenerMode#SINGLE_RECORD SINGLE_RECORD} mode, or the latest
	 * position of the delivered batch in {@link io.awspring.cloud.kinesis.listener.KclListenerMode#BATCH BATCH} mode.
	 */
	void checkpoint();

	/**
	 * Checkpoint the position of the given message, read from its
	 * {@link io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders#SEQUENCE_NUMBER sequence number} and,
	 * for an aggregated record, its
	 * {@link io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders#SUBSEQUENCE_NUMBER subsequence number}.
	 * @param message the message whose position is checkpointed.
	 */
	void checkpoint(Message<?> message);

	/**
	 * A view of this checkpointer bound to a single record, whose {@link #checkpoint()} commits that record's position
	 * instead of the latest position of the batch. Used in
	 * {@link io.awspring.cloud.kinesis.listener.KclListenerMode#SINGLE_RECORD SINGLE_RECORD} mode so a listener
	 * checkpointing one record never commits records that have not been delivered yet.
	 * @param record the record the returned checkpointer is bound to.
	 * @return the record-bound checkpointer.
	 */
	default KclCheckpointer boundTo(Message<?> record) {
		return new RecordBoundKclCheckpointer(this, record);
	}

}
