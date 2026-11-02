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

import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.retrieval.RetrievalMode;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageConverter;
import io.awspring.cloud.kinesis.support.observation.KclListenerObservation;
import io.micrometer.observation.ObservationRegistry;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.kinesis.checkpoint.CheckpointConfig;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.coordinator.CoordinatorConfig;
import software.amazon.kinesis.leases.LeaseManagementConfig;
import software.amazon.kinesis.lifecycle.LifecycleConfig;
import software.amazon.kinesis.metrics.MetricsConfig;
import software.amazon.kinesis.metrics.MetricsLevel;
import software.amazon.kinesis.processor.ProcessorConfig;
import software.amazon.kinesis.retrieval.RetrievalConfig;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
public final class KclContainerOptions {

	private final String workerIdentifier;

	private final int maxRecords;

	private final long idleTimeBetweenReadsInMillis;

	private final InitialPositionInStream initialPositionInStream;

	private final Duration gracefulShutdownTimeout;

	private final long checkpointRecordCount;

	private final Duration checkpointInterval;

	private final RetrievalMode retrievalMode;

	private final KclCheckpointMode checkpointMode;

	@Nullable
	private final Duration haltedShardReleaseDelay;

	private final boolean autoStartup;

	private final int phase;

	@Nullable
	private final MetricsLevel metricsLevel;

	@Nullable
	private final String metricsNamespace;

	@Nullable
	private final String consumerArn;

	@Nullable
	private final String consumerName;

	@Nullable
	private final Instant initialPositionTimestamp;

	@Nullable
	private final String leaseTableName;

	@Nullable
	private final BillingMode billingMode;

	@Nullable
	private final MimeType payloadContentType;

	@Nullable
	private final KinesisMessageConverter messageConverter;

	private final ObservationRegistry observationRegistry;

	private final KclListenerObservation.@Nullable Convention observationConvention;

	@Nullable
	private final Consumer<CheckpointConfig> checkpointConfigCustomizer;

	@Nullable
	private final Consumer<CoordinatorConfig> coordinatorConfigCustomizer;

	@Nullable
	private final Consumer<LeaseManagementConfig> leaseManagementConfigCustomizer;

	@Nullable
	private final Consumer<LifecycleConfig> lifecycleConfigCustomizer;

	@Nullable
	private final Consumer<MetricsConfig> metricsConfigCustomizer;

	@Nullable
	private final Consumer<ProcessorConfig> processorConfigCustomizer;

	@Nullable
	private final Consumer<RetrievalConfig> retrievalConfigCustomizer;

	private KclContainerOptions(Builder builder) {
		this.workerIdentifier = builder.workerIdentifier;
		this.maxRecords = builder.maxRecords;
		this.idleTimeBetweenReadsInMillis = builder.idleTimeBetweenReadsInMillis;
		this.initialPositionInStream = builder.initialPositionInStream;
		this.gracefulShutdownTimeout = builder.gracefulShutdownTimeout;
		this.checkpointRecordCount = builder.checkpointRecordCount;
		this.checkpointInterval = builder.checkpointInterval;
		this.retrievalMode = builder.retrievalMode;
		this.checkpointMode = builder.checkpointMode;
		this.haltedShardReleaseDelay = builder.haltedShardReleaseDelay;
		this.autoStartup = builder.autoStartup;
		this.phase = builder.phase;
		this.metricsLevel = builder.metricsLevel;
		this.metricsNamespace = builder.metricsNamespace;
		this.consumerArn = builder.consumerArn;
		this.consumerName = builder.consumerName;
		this.initialPositionTimestamp = builder.initialPositionTimestamp;
		this.leaseTableName = builder.leaseTableName;
		this.billingMode = builder.billingMode;
		this.payloadContentType = builder.payloadContentType;
		this.messageConverter = builder.messageConverter;
		this.observationRegistry = builder.observationRegistry;
		this.observationConvention = builder.observationConvention;
		this.checkpointConfigCustomizer = builder.checkpointConfigCustomizer;
		this.coordinatorConfigCustomizer = builder.coordinatorConfigCustomizer;
		this.leaseManagementConfigCustomizer = builder.leaseManagementConfigCustomizer;
		this.lifecycleConfigCustomizer = builder.lifecycleConfigCustomizer;
		this.metricsConfigCustomizer = builder.metricsConfigCustomizer;
		this.processorConfigCustomizer = builder.processorConfigCustomizer;
		this.retrievalConfigCustomizer = builder.retrievalConfigCustomizer;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		Builder builder = new Builder();
		builder.workerIdentifier = this.workerIdentifier;
		builder.maxRecords = this.maxRecords;
		builder.idleTimeBetweenReadsInMillis = this.idleTimeBetweenReadsInMillis;
		builder.initialPositionInStream = this.initialPositionInStream;
		builder.gracefulShutdownTimeout = this.gracefulShutdownTimeout;
		builder.checkpointRecordCount = this.checkpointRecordCount;
		builder.checkpointInterval = this.checkpointInterval;
		builder.retrievalMode = this.retrievalMode;
		builder.checkpointMode = this.checkpointMode;
		builder.haltedShardReleaseDelay = this.haltedShardReleaseDelay;
		builder.autoStartup = this.autoStartup;
		builder.phase = this.phase;
		builder.metricsLevel = this.metricsLevel;
		builder.metricsNamespace = this.metricsNamespace;
		builder.consumerArn = this.consumerArn;
		builder.consumerName = this.consumerName;
		builder.initialPositionTimestamp = this.initialPositionTimestamp;
		builder.leaseTableName = this.leaseTableName;
		builder.billingMode = this.billingMode;
		builder.payloadContentType = this.payloadContentType;
		builder.messageConverter = this.messageConverter;
		builder.observationRegistry = this.observationRegistry;
		builder.observationConvention = this.observationConvention;
		builder.checkpointConfigCustomizer = this.checkpointConfigCustomizer;
		builder.coordinatorConfigCustomizer = this.coordinatorConfigCustomizer;
		builder.leaseManagementConfigCustomizer = this.leaseManagementConfigCustomizer;
		builder.lifecycleConfigCustomizer = this.lifecycleConfigCustomizer;
		builder.metricsConfigCustomizer = this.metricsConfigCustomizer;
		builder.processorConfigCustomizer = this.processorConfigCustomizer;
		builder.retrievalConfigCustomizer = this.retrievalConfigCustomizer;
		return builder;
	}

	public String getWorkerIdentifier() {
		return this.workerIdentifier;
	}

	public int getMaxRecords() {
		return this.maxRecords;
	}

	public long getIdleTimeBetweenReadsInMillis() {
		return this.idleTimeBetweenReadsInMillis;
	}

	public InitialPositionInStream getInitialPositionInStream() {
		return this.initialPositionInStream;
	}

	public Duration getGracefulShutdownTimeout() {
		return this.gracefulShutdownTimeout;
	}

	public long getCheckpointRecordCount() {
		return this.checkpointRecordCount;
	}

	public Duration getCheckpointInterval() {
		return this.checkpointInterval;
	}

	public RetrievalMode getRetrievalMode() {
		return this.retrievalMode;
	}

	public KclCheckpointMode getCheckpointMode() {
		return this.checkpointMode;
	}

	/**
	 * How long after a shard halts the container releases its lease, so that KCL reassigns the shard and it is
	 * processed again from its last checkpoint without a restart, or {@code null} to keep the lease (the shard then
	 * stays halted until the application restarts or the lease moves on its own).
	 * @return the release delay, or {@code null} if releasing is disabled.
	 */
	@Nullable
	public Duration getHaltedShardReleaseDelay() {
		return this.haltedShardReleaseDelay;
	}

	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public int getPhase() {
		return this.phase;
	}

	@Nullable
	public MetricsLevel getMetricsLevel() {
		return this.metricsLevel;
	}

	@Nullable
	public String getMetricsNamespace() {
		return this.metricsNamespace;
	}

	@Nullable
	public String getConsumerArn() {
		return this.consumerArn;
	}

	@Nullable
	public String getConsumerName() {
		return this.consumerName;
	}

	@Nullable
	public Instant getInitialPositionTimestamp() {
		return this.initialPositionTimestamp;
	}

	@Nullable
	public String getLeaseTableName() {
		return this.leaseTableName;
	}

	@Nullable
	public BillingMode getBillingMode() {
		return this.billingMode;
	}

	@Nullable
	public MimeType getPayloadContentType() {
		return this.payloadContentType;
	}

	/**
	 * The converter turning each KCL record into the {@link org.springframework.messaging.Message} handed to the
	 * listener, or {@code null} for the default
	 * {@link io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter}.
	 * @return the message converter.
	 */
	@Nullable
	public KinesisMessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * The {@link ObservationRegistry} listener invocations are observed with, or {@link ObservationRegistry#NOOP} when
	 * observation is disabled.
	 * @return the observation registry.
	 */
	public ObservationRegistry getObservationRegistry() {
		return this.observationRegistry;
	}

	/**
	 * The custom {@link KclListenerObservation.Convention} overriding the default observation name and key values, or
	 * {@code null} to use the default convention.
	 * @return the observation convention.
	 */
	public KclListenerObservation.@Nullable Convention getObservationConvention() {
		return this.observationConvention;
	}

	@Nullable
	public Consumer<CheckpointConfig> getCheckpointConfigCustomizer() {
		return this.checkpointConfigCustomizer;
	}

	@Nullable
	public Consumer<CoordinatorConfig> getCoordinatorConfigCustomizer() {
		return this.coordinatorConfigCustomizer;
	}

	@Nullable
	public Consumer<LeaseManagementConfig> getLeaseManagementConfigCustomizer() {
		return this.leaseManagementConfigCustomizer;
	}

	@Nullable
	public Consumer<LifecycleConfig> getLifecycleConfigCustomizer() {
		return this.lifecycleConfigCustomizer;
	}

	@Nullable
	public Consumer<MetricsConfig> getMetricsConfigCustomizer() {
		return this.metricsConfigCustomizer;
	}

	@Nullable
	public Consumer<ProcessorConfig> getProcessorConfigCustomizer() {
		return this.processorConfigCustomizer;
	}

	@Nullable
	public Consumer<RetrievalConfig> getRetrievalConfigCustomizer() {
		return this.retrievalConfigCustomizer;
	}

	public static final class Builder {

		private String workerIdentifier = defaultWorkerIdentifier();

		private int maxRecords = 10000;

		private long idleTimeBetweenReadsInMillis = 1000L;

		private InitialPositionInStream initialPositionInStream = InitialPositionInStream.TRIM_HORIZON;

		private Duration gracefulShutdownTimeout = Duration.ofSeconds(20);

		private long checkpointRecordCount = 1000L;

		private Duration checkpointInterval = Duration.ofSeconds(60);

		private RetrievalMode retrievalMode = RetrievalMode.POLLING;

		private KclCheckpointMode checkpointMode = KclCheckpointMode.BATCH;

		@Nullable
		private Duration haltedShardReleaseDelay;

		private boolean autoStartup = true;

		private int phase = SmartLifecycle.DEFAULT_PHASE;

		@Nullable
		private MetricsLevel metricsLevel;

		@Nullable
		private String metricsNamespace;

		@Nullable
		private String consumerArn;

		@Nullable
		private String consumerName;

		@Nullable
		private Instant initialPositionTimestamp;

		@Nullable
		private String leaseTableName;

		@Nullable
		private BillingMode billingMode;

		@Nullable
		private MimeType payloadContentType;

		@Nullable
		private KinesisMessageConverter messageConverter;

		private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

		private KclListenerObservation.@Nullable Convention observationConvention;

		@Nullable
		private Consumer<CheckpointConfig> checkpointConfigCustomizer;

		@Nullable
		private Consumer<CoordinatorConfig> coordinatorConfigCustomizer;

		@Nullable
		private Consumer<LeaseManagementConfig> leaseManagementConfigCustomizer;

		@Nullable
		private Consumer<LifecycleConfig> lifecycleConfigCustomizer;

		@Nullable
		private Consumer<MetricsConfig> metricsConfigCustomizer;

		@Nullable
		private Consumer<ProcessorConfig> processorConfigCustomizer;

		@Nullable
		private Consumer<RetrievalConfig> retrievalConfigCustomizer;

		private Builder() {
		}

		public Builder workerIdentifier(String workerIdentifier) {
			Assert.hasText(workerIdentifier, "workerIdentifier must not be empty");
			this.workerIdentifier = workerIdentifier;
			return this;
		}

		public Builder maxRecords(int maxRecords) {
			Assert.isTrue(maxRecords > 0, "maxRecords must be greater than zero");
			this.maxRecords = maxRecords;
			return this;
		}

		public Builder idleTimeBetweenReadsInMillis(long idleTimeBetweenReadsInMillis) {
			Assert.isTrue(idleTimeBetweenReadsInMillis > 0, "idleTimeBetweenReadsInMillis must be greater than zero");
			this.idleTimeBetweenReadsInMillis = idleTimeBetweenReadsInMillis;
			return this;
		}

		public Builder initialPositionInStream(InitialPositionInStream initialPositionInStream) {
			Assert.notNull(initialPositionInStream, "initialPositionInStream must not be null");
			this.initialPositionInStream = initialPositionInStream;
			return this;
		}

		public Builder gracefulShutdownTimeout(Duration gracefulShutdownTimeout) {
			Assert.notNull(gracefulShutdownTimeout, "gracefulShutdownTimeout must not be null");
			this.gracefulShutdownTimeout = gracefulShutdownTimeout;
			return this;
		}

		public Builder checkpointRecordCount(long checkpointRecordCount) {
			Assert.isTrue(checkpointRecordCount > 0, "checkpointRecordCount must be greater than zero");
			this.checkpointRecordCount = checkpointRecordCount;
			return this;
		}

		public Builder checkpointInterval(Duration checkpointInterval) {
			Assert.notNull(checkpointInterval, "checkpointInterval must not be null");
			this.checkpointInterval = checkpointInterval;
			return this;
		}

		public Builder retrievalMode(RetrievalMode retrievalMode) {
			Assert.notNull(retrievalMode, "retrievalMode must not be null");
			this.retrievalMode = retrievalMode;
			return this;
		}

		public Builder checkpointMode(KclCheckpointMode checkpointMode) {
			Assert.notNull(checkpointMode, "checkpointMode must not be null");
			this.checkpointMode = checkpointMode;
			return this;
		}

		/**
		 * Set how long after a shard halts the container gives up the shard's lease, so that KCL reassigns it and the
		 * shard is processed again from its last checkpoint without restarting the application. The delay also spaces
		 * out releases of a shard that keeps failing, since each release restarts the processor from the checkpoint
		 * with fresh state. Defaults to {@code null}: the lease is kept and the shard stays halted until the
		 * application restarts or the lease moves on its own. When set it must be strictly positive: a zero delay would
		 * release a shard that keeps failing in a tight loop.
		 * @param haltedShardReleaseDelay the delay between a halt and the lease release, or {@code null} to disable
		 *     releasing.
		 * @return this builder.
		 */
		public Builder haltedShardReleaseDelay(@Nullable Duration haltedShardReleaseDelay) {
			Assert.isTrue(
					haltedShardReleaseDelay == null
							|| (!haltedShardReleaseDelay.isNegative() && !haltedShardReleaseDelay.isZero()),
					"haltedShardReleaseDelay must be positive");
			this.haltedShardReleaseDelay = haltedShardReleaseDelay;
			return this;
		}

		public Builder autoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
			return this;
		}

		public Builder phase(int phase) {
			this.phase = phase;
			return this;
		}

		public Builder metricsNamespace(String metricsNamespace) {
			Assert.hasText(metricsNamespace, "metricsNamespace must not be empty");
			this.metricsNamespace = metricsNamespace;
			return this;
		}

		public Builder consumerArn(String consumerArn) {
			Assert.hasText(consumerArn, "consumerArn must not be empty");
			this.consumerArn = consumerArn;
			return this;
		}

		public Builder consumerName(String consumerName) {
			Assert.hasText(consumerName, "consumerName must not be empty");
			this.consumerName = consumerName;
			return this;
		}

		public Builder metricsLevel(MetricsLevel metricsLevel) {
			this.metricsLevel = metricsLevel;
			return this;
		}

		public Builder initialPositionTimestamp(Instant initialPositionTimestamp) {
			this.initialPositionTimestamp = initialPositionTimestamp;
			return this;
		}

		public Builder leaseTableName(String leaseTableName) {
			this.leaseTableName = leaseTableName;
			return this;
		}

		public Builder billingMode(BillingMode billingMode) {
			this.billingMode = billingMode;
			return this;
		}

		/**
		 * Set the converter turning each KCL record into the {@link org.springframework.messaging.Message} handed to
		 * the listener, for example to decompress records or unwrap an envelope. The resulting {@code byte[]} payload
		 * is then converted to the listener's parameter type by the handler method's message converters. Defaults to
		 * {@link io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter}.
		 * @param messageConverter the message converter.
		 * @return this builder.
		 */
		public Builder messageConverter(KinesisMessageConverter messageConverter) {
			Assert.notNull(messageConverter, "messageConverter must not be null");
			this.messageConverter = messageConverter;
			return this;
		}

		/**
		 * Set the content type added to every converted message that has no {@code contentType} header yet, whatever
		 * converter produced it.
		 * @param payloadContentType the content type.
		 * @return this builder.
		 */
		public Builder payloadContentType(MimeType payloadContentType) {
			this.payloadContentType = payloadContentType;
			return this;
		}

		/**
		 * Set the {@link ObservationRegistry} listener invocations are observed with: one observation per record in
		 * {@link KclListenerMode#SINGLE_RECORD} mode and one per batch in {@link KclListenerMode#BATCH} mode. Defaults
		 * to {@link ObservationRegistry#NOOP}, which makes observation a no-op.
		 * @param observationRegistry the registry.
		 * @return this builder.
		 */
		public Builder observationRegistry(ObservationRegistry observationRegistry) {
			Assert.notNull(observationRegistry, "observationRegistry must not be null");
			this.observationRegistry = observationRegistry;
			return this;
		}

		/**
		 * Set a custom {@link KclListenerObservation.Convention} overriding the default observation name and key
		 * values. Defaults to {@code null}, which uses {@link KclListenerObservation.DefaultConvention}.
		 * @param observationConvention the convention.
		 * @return this builder.
		 */
		public Builder observationConvention(KclListenerObservation.Convention observationConvention) {
			Assert.notNull(observationConvention, "observationConvention must not be null");
			this.observationConvention = observationConvention;
			return this;
		}

		public Builder checkpointConfigCustomizer(Consumer<CheckpointConfig> checkpointConfigCustomizer) {
			this.checkpointConfigCustomizer = checkpointConfigCustomizer;
			return this;
		}

		public Builder coordinatorConfigCustomizer(Consumer<CoordinatorConfig> coordinatorConfigCustomizer) {
			this.coordinatorConfigCustomizer = coordinatorConfigCustomizer;
			return this;
		}

		public Builder leaseManagementConfigCustomizer(
				Consumer<LeaseManagementConfig> leaseManagementConfigCustomizer) {
			this.leaseManagementConfigCustomizer = leaseManagementConfigCustomizer;
			return this;
		}

		public Builder lifecycleConfigCustomizer(Consumer<LifecycleConfig> lifecycleConfigCustomizer) {
			this.lifecycleConfigCustomizer = lifecycleConfigCustomizer;
			return this;
		}

		public Builder metricsConfigCustomizer(Consumer<MetricsConfig> metricsConfigCustomizer) {
			this.metricsConfigCustomizer = metricsConfigCustomizer;
			return this;
		}

		public Builder processorConfigCustomizer(Consumer<ProcessorConfig> processorConfigCustomizer) {
			this.processorConfigCustomizer = processorConfigCustomizer;
			return this;
		}

		public Builder retrievalConfigCustomizer(Consumer<RetrievalConfig> retrievalConfigCustomizer) {
			this.retrievalConfigCustomizer = retrievalConfigCustomizer;
			return this;
		}

		public KclContainerOptions build() {
			Assert.state(
					this.initialPositionInStream != InitialPositionInStream.AT_TIMESTAMP
							|| this.initialPositionTimestamp != null,
					"initialPositionTimestamp must be set when initialPositionInStream is AT_TIMESTAMP");
			return new KclContainerOptions(this);
		}

		private static String defaultWorkerIdentifier() {
			try {
				return InetAddress.getLocalHost().getHostName() + ":" + UUID.randomUUID();
			}
			catch (UnknownHostException ex) {
				return UUID.randomUUID().toString();
			}
		}

	}

}
