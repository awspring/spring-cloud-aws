/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.kinesis.integration;

import java.time.Instant;
import java.util.Objects;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;

/**
 * A model to represent a sequence in the shard for particular {@link ShardIteratorType}.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class KinesisShardOffset {

	private ShardIteratorType iteratorType;

	private String sequenceNumber;

	private Instant timestamp;

	private String stream;

	private String shard;

	private boolean reset;

	public KinesisShardOffset(ShardIteratorType iteratorType) {
		Assert.notNull(iteratorType, "'iteratorType' must not be null.");
		this.iteratorType = iteratorType;
	}

	public KinesisShardOffset(KinesisShardOffset other) {
		this.iteratorType = other.getIteratorType();
		this.stream = other.getStream();
		this.shard = other.getShard();
		this.sequenceNumber = other.getSequenceNumber();
		this.timestamp = other.getTimestamp();
		this.reset = other.isReset();
	}

	public void setIteratorType(ShardIteratorType iteratorType) {
		this.iteratorType = iteratorType;
	}

	public ShardIteratorType getIteratorType() {
		return this.iteratorType;
	}

	public void setSequenceNumber(String sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public void setStream(String stream) {
		this.stream = stream;
	}

	public void setShard(String shard) {
		this.shard = shard;
	}

	public void setReset(boolean reset) {
		this.reset = reset;
	}

	public String getSequenceNumber() {
		return this.sequenceNumber;
	}

	public Instant getTimestamp() {
		return this.timestamp;
	}

	public String getStream() {
		return this.stream;
	}

	public String getShard() {
		return this.shard;
	}

	public boolean isReset() {
		return this.reset;
	}

	public KinesisShardOffset reset() {
		this.reset = true;
		return this;
	}

	public GetShardIteratorRequest toShardIteratorRequest() {
		Assert.state(this.stream != null && this.shard != null,
				"'stream' and 'shard' must not be null for conversion to the GetShardIteratorRequest.");
		return GetShardIteratorRequest.builder().streamName(this.stream).shardId(this.shard)
				.shardIteratorType(this.iteratorType).startingSequenceNumber(this.sequenceNumber)
				.timestamp(this.timestamp).build();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		KinesisShardOffset that = (KinesisShardOffset) o;
		return Objects.equals(this.stream, that.stream) && Objects.equals(this.shard, that.shard);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.stream, this.shard);
	}

	@Override
	public String toString() {
		return "KinesisShardOffset{" + "iteratorType=" + this.iteratorType + ", sequenceNumber='" + this.sequenceNumber
				+ '\'' + ", timestamp=" + this.timestamp + ", stream='" + this.stream + '\'' + ", shard='" + this.shard
				+ '\'' + ", reset=" + this.reset + '}';
	}

	public static KinesisShardOffset latest() {
		return latest(null, null);
	}

	public static KinesisShardOffset latest(String stream, String shard) {
		KinesisShardOffset kinesisShardOffset = new KinesisShardOffset(ShardIteratorType.LATEST);
		kinesisShardOffset.stream = stream;
		kinesisShardOffset.shard = shard;
		return kinesisShardOffset;
	}

	public static KinesisShardOffset trimHorizon() {
		return trimHorizon(null, null);
	}

	public static KinesisShardOffset trimHorizon(String stream, String shard) {
		KinesisShardOffset kinesisShardOffset = new KinesisShardOffset(ShardIteratorType.TRIM_HORIZON);
		kinesisShardOffset.stream = stream;
		kinesisShardOffset.shard = shard;
		return kinesisShardOffset;
	}

	public static KinesisShardOffset atSequenceNumber(String sequenceNumber) {
		return atSequenceNumber(null, null, sequenceNumber);
	}

	public static KinesisShardOffset atSequenceNumber(String stream, String shard, String sequenceNumber) {
		KinesisShardOffset kinesisShardOffset = new KinesisShardOffset(ShardIteratorType.AT_SEQUENCE_NUMBER);
		kinesisShardOffset.stream = stream;
		kinesisShardOffset.shard = shard;
		kinesisShardOffset.sequenceNumber = sequenceNumber;
		return kinesisShardOffset;
	}

	public static KinesisShardOffset afterSequenceNumber(String sequenceNumber) {
		return afterSequenceNumber(null, null, sequenceNumber);
	}

	public static KinesisShardOffset afterSequenceNumber(String stream, String shard, String sequenceNumber) {
		KinesisShardOffset kinesisShardOffset = new KinesisShardOffset(ShardIteratorType.AFTER_SEQUENCE_NUMBER);
		kinesisShardOffset.stream = stream;
		kinesisShardOffset.shard = shard;
		kinesisShardOffset.sequenceNumber = sequenceNumber;
		return kinesisShardOffset;
	}

	public static KinesisShardOffset atTimestamp(Instant timestamp) {
		return atTimestamp(null, null, timestamp);
	}

	public static KinesisShardOffset atTimestamp(String stream, String shard, Instant timestamp) {
		KinesisShardOffset kinesisShardOffset = new KinesisShardOffset(ShardIteratorType.AT_TIMESTAMP);
		kinesisShardOffset.stream = stream;
		kinesisShardOffset.shard = shard;
		kinesisShardOffset.timestamp = timestamp;
		return kinesisShardOffset;
	}

}
