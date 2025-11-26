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
package io.awspring.cloud.kinesis.stream.binder.properties;

import io.awspring.cloud.kinesis.integration.CheckpointMode;
import io.awspring.cloud.kinesis.integration.ListenerMode;
import software.amazon.kinesis.metrics.MetricsLevel;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

/**
 * The Kinesis-specific consumer binding configuration properties.
 *
 * @author Peter Oates
 * @author Jacob Severson
 * @author Artem Bilan
 * @author Arnaud Lecollaire
 * @author Dmytro Danilenkov
 * @author Minkyu Moon
 *
 * @since 4.0
 */
public class KinesisConsumerProperties {

	private int startTimeout = 60000;

	private ListenerMode listenerMode = ListenerMode.record;

	private CheckpointMode checkpointMode = CheckpointMode.batch;

	/**
	 * Interval, in milliseconds, between two checkpoints when checkpoint mode is periodic.
	 */
	private Long checkpointInterval = 5_000L;

	private int recordsLimit = 10000;

	private int idleBetweenPolls = 1000;

	private int consumerBackoff = 1000;

	private String shardIteratorType;

	private String shardId;

	/**
	 * Worker identifier used to distinguish different workers/processes (only used when KCL is enabled).
	 */
	private String workerId;

	/**
	 * The KCL fan-out or polling retrieval mode.
	 */
	private boolean fanOut = true;

	/**
	 * The KCL emptyRecordList option for batch listener mode.
	 */
	private boolean emptyRecordList = false;

	/**
	 * The KCL table name for leases.
	 */
	private String leaseTableName;

	/**
	 * The KCL max records for request in polling mode.
	 */
	private int pollingMaxRecords = PollingConfig.DEFAULT_MAX_RECORDS;

	/**
	 * The KCL idle between requests in polling mode.
	 */
	private long pollingIdleTime = 1500L;

	/**
	 * The KCL graceful shutdown timeout in milliseconds.
	 */
	private long gracefulShutdownTimeout;

	private boolean embedHeaders = true;

	private boolean dynamoDbStreams;

	/**
	 * The {@link MetricsLevel} for emitting (or not) metrics into Cloud Watch.
	 */
	private MetricsLevel metricsLevel = MetricsLevel.DETAILED;

	public int getStartTimeout() {
		return this.startTimeout;
	}

	public void setStartTimeout(int startTimeout) {
		this.startTimeout = startTimeout;
	}

	public ListenerMode getListenerMode() {
		return this.listenerMode;
	}

	public void setListenerMode(ListenerMode listenerMode) {
		this.listenerMode = listenerMode;
	}

	public CheckpointMode getCheckpointMode() {
		return this.checkpointMode;
	}

	public void setCheckpointMode(CheckpointMode checkpointMode) {
		this.checkpointMode = checkpointMode;
	}

	public Long getCheckpointInterval() {
		return checkpointInterval;
	}

	public void setCheckpointInterval(Long checkpointInterval) {
		this.checkpointInterval = checkpointInterval;
	}

	public int getRecordsLimit() {
		return this.recordsLimit;
	}

	public void setRecordsLimit(int recordsLimit) {
		this.recordsLimit = recordsLimit;
	}

	public int getIdleBetweenPolls() {
		return this.idleBetweenPolls;
	}

	public void setIdleBetweenPolls(int idleBetweenPolls) {
		this.idleBetweenPolls = idleBetweenPolls;
	}

	public int getConsumerBackoff() {
		return this.consumerBackoff;
	}

	public void setConsumerBackoff(int consumerBackoff) {
		this.consumerBackoff = consumerBackoff;
	}

	public String getShardIteratorType() {
		return this.shardIteratorType;
	}

	public void setShardIteratorType(String shardIteratorType) {
		this.shardIteratorType = shardIteratorType;
	}

	public String getWorkerId() {
		return workerId;
	}

	public void setWorkerId(String workerId) {
		this.workerId = workerId;
	}

	public String getShardId() {
		return shardId;
	}

	public void setShardId(String shardId) {
		this.shardId = shardId;
	}

	public boolean isEmbedHeaders() {
		return this.embedHeaders;
	}

	public void setEmbedHeaders(boolean embedHeaders) {
		this.embedHeaders = embedHeaders;
	}

	public boolean isFanOut() {
		return this.fanOut;
	}

	public void setFanOut(boolean fanOut) {
		this.fanOut = fanOut;
	}

	public MetricsLevel getMetricsLevel() {
		return this.metricsLevel;
	}

	public void setMetricsLevel(MetricsLevel metricsLevel) {
		this.metricsLevel = metricsLevel;
	}

	public boolean isEmptyRecordList() {
		return this.emptyRecordList;
	}

	public void setEmptyRecordList(boolean emptyRecordList) {
		this.emptyRecordList = emptyRecordList;
	}

	public String getLeaseTableName() {
		return this.leaseTableName;
	}

	public void setLeaseTableName(String leaseTableName) {
		this.leaseTableName = leaseTableName;
	}

	public int getPollingMaxRecords() {
		return this.pollingMaxRecords;
	}

	public void setPollingMaxRecords(int pollingMaxRecords) {
		this.pollingMaxRecords = pollingMaxRecords;
	}

	public long getPollingIdleTime() {
		return this.pollingIdleTime;
	}

	public void setPollingIdleTime(long pollingIdleTime) {
		this.pollingIdleTime = pollingIdleTime;
	}

	public long getGracefulShutdownTimeout() {
		return this.gracefulShutdownTimeout;
	}

	public void setGracefulShutdownTimeout(long gracefulShutdownTimeout) {
		this.gracefulShutdownTimeout = gracefulShutdownTimeout;
	}

	public boolean isDynamoDbStreams() {
		return this.dynamoDbStreams;
	}

	public void setDynamoDbStreams(boolean dynamoDbStreams) {
		this.dynamoDbStreams = dynamoDbStreams;
	}

}
