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
package io.awspring.cloud.autoconfigure.kinesis;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.retrieval.RetrievalMode;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.metrics.MetricsLevel;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
@ConfigurationProperties(prefix = KinesisProperties.PREFIX)
public class KinesisProperties extends AwsClientProperties {

	public static final String PREFIX = "spring.cloud.aws.kinesis";

	private Listener listener = new Listener();

	public Listener getListener() {
		return this.listener;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public static class Listener {

		@Nullable
		private Integer maxRecords;

		@Nullable
		private Duration idleTimeBetweenReads;

		@Nullable
		private RetrievalMode retrievalMode;

		@Nullable
		private KclCheckpointMode checkpointMode;

		@Nullable
		private InitialPositionInStream initialPosition;

		@Nullable
		private Duration gracefulShutdownTimeout;

		@Nullable
		private Long checkpointRecordCount;

		@Nullable
		private Duration checkpointInterval;

		@Nullable
		private MetricsLevel metricsLevel;

		@Nullable
		private String metricsNamespace;

		@Nullable
		private Boolean autoStartup;

		@Nullable
		private Integer phase;

		@Nullable
		private Instant initialPositionTimestamp;

		@Nullable
		private BillingMode billingMode;

		@Nullable
		private String contentType;

		@Nullable
		public Integer getMaxRecords() {
			return this.maxRecords;
		}

		public void setMaxRecords(Integer maxRecords) {
			this.maxRecords = maxRecords;
		}

		@Nullable
		public Duration getIdleTimeBetweenReads() {
			return this.idleTimeBetweenReads;
		}

		public void setIdleTimeBetweenReads(Duration idleTimeBetweenReads) {
			this.idleTimeBetweenReads = idleTimeBetweenReads;
		}

		@Nullable
		public RetrievalMode getRetrievalMode() {
			return this.retrievalMode;
		}

		public void setRetrievalMode(RetrievalMode retrievalMode) {
			this.retrievalMode = retrievalMode;
		}

		@Nullable
		public KclCheckpointMode getCheckpointMode() {
			return this.checkpointMode;
		}

		public void setCheckpointMode(KclCheckpointMode checkpointMode) {
			this.checkpointMode = checkpointMode;
		}

		@Nullable
		public InitialPositionInStream getInitialPosition() {
			return this.initialPosition;
		}

		public void setInitialPosition(InitialPositionInStream initialPosition) {
			this.initialPosition = initialPosition;
		}

		@Nullable
		public Duration getGracefulShutdownTimeout() {
			return this.gracefulShutdownTimeout;
		}

		public void setGracefulShutdownTimeout(Duration gracefulShutdownTimeout) {
			this.gracefulShutdownTimeout = gracefulShutdownTimeout;
		}

		@Nullable
		public Long getCheckpointRecordCount() {
			return this.checkpointRecordCount;
		}

		public void setCheckpointRecordCount(Long checkpointRecordCount) {
			this.checkpointRecordCount = checkpointRecordCount;
		}

		@Nullable
		public Duration getCheckpointInterval() {
			return this.checkpointInterval;
		}

		public void setCheckpointInterval(Duration checkpointInterval) {
			this.checkpointInterval = checkpointInterval;
		}

		@Nullable
		public MetricsLevel getMetricsLevel() {
			return this.metricsLevel;
		}

		public void setMetricsLevel(MetricsLevel metricsLevel) {
			this.metricsLevel = metricsLevel;
		}

		@Nullable
		public String getMetricsNamespace() {
			return this.metricsNamespace;
		}

		public void setMetricsNamespace(String metricsNamespace) {
			this.metricsNamespace = metricsNamespace;
		}

		@Nullable
		public Boolean getAutoStartup() {
			return this.autoStartup;
		}

		public void setAutoStartup(Boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

		@Nullable
		public Integer getPhase() {
			return this.phase;
		}

		public void setPhase(Integer phase) {
			this.phase = phase;
		}

		@Nullable
		public Instant getInitialPositionTimestamp() {
			return this.initialPositionTimestamp;
		}

		public void setInitialPositionTimestamp(Instant initialPositionTimestamp) {
			this.initialPositionTimestamp = initialPositionTimestamp;
		}

		@Nullable
		public BillingMode getBillingMode() {
			return this.billingMode;
		}

		public void setBillingMode(BillingMode billingMode) {
			this.billingMode = billingMode;
		}

		@Nullable
		public String getContentType() {
			return this.contentType;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

	}

}
