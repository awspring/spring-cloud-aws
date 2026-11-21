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

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

/**
 * Published when a shard is halted because the {@link io.awspring.cloud.kinesis.listener.errorhandler.ErrorHandler}
 * left a failure unresolved, or an unexpected failure occurred while processing. A halted shard delivers no further
 * records and takes no further checkpoint until its lease is reacquired, so operators can alert on this event rather
 * than parsing logs.
 * <p>
 * The {@link #getSource() source} is the {@link KclMessageListenerContainer} that owns the shard.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
@SuppressWarnings("serial")
public class KclShardHaltedEvent extends ApplicationEvent {

	private final String containerId;

	private final String streamName;

	@Nullable
	private final String shardId;

	private final Throwable cause;

	@Nullable
	private final Long millisBehindLatest;

	/**
	 * Create the event.
	 * @param source the container that owns the halted shard.
	 * @param containerId the id of that container.
	 * @param streamName the stream the halted shard belongs to.
	 * @param shardId the halted shard, or {@code null} if it is not known yet.
	 * @param cause the failure that halted the shard.
	 * @param millisBehindLatest how far behind the tip of the shard the last processed batch was, or {@code null} if no
	 *     batch has been processed.
	 */
	public KclShardHaltedEvent(Object source, String containerId, String streamName, @Nullable String shardId,
			Throwable cause, @Nullable Long millisBehindLatest) {
		super(source);
		this.containerId = containerId;
		this.streamName = streamName;
		this.shardId = shardId;
		this.cause = cause;
		this.millisBehindLatest = millisBehindLatest;
	}

	/**
	 * The id of the container that owns the halted shard.
	 * @return the container id.
	 */
	public String getContainerId() {
		return this.containerId;
	}

	/**
	 * The stream the halted shard belongs to.
	 * @return the stream name.
	 */
	public String getStreamName() {
		return this.streamName;
	}

	/**
	 * The halted shard.
	 * @return the shard id, or {@code null} if it is not known yet.
	 */
	@Nullable
	public String getShardId() {
		return this.shardId;
	}

	/**
	 * The failure that halted the shard.
	 * @return the cause.
	 */
	public Throwable getCause() {
		return this.cause;
	}

	/**
	 * How far behind the tip of the shard the last processed batch was, from
	 * {@link software.amazon.kinesis.lifecycle.events.ProcessRecordsInput#millisBehindLatest()}.
	 * @return the lag in milliseconds, or {@code null} if no batch has been processed.
	 */
	@Nullable
	public Long getMillisBehindLatest() {
		return this.millisBehindLatest;
	}

}
