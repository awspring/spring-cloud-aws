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
import io.awspring.cloud.kinesis.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.kinesis.listener.errorhandler.LoggingErrorHandler;
import io.awspring.cloud.kinesis.listener.retrieval.FanOutRetrievalConfigurer;
import io.awspring.cloud.kinesis.listener.retrieval.KclRetrievalConfigurer;
import io.awspring.cloud.kinesis.listener.retrieval.PollingRetrievalConfigurer;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageConverter;
import io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.checkpoint.CheckpointConfig;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.common.StreamIdentifier;
import software.amazon.kinesis.coordinator.CoordinatorConfig;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.leases.Lease;
import software.amazon.kinesis.leases.LeaseManagementConfig;
import software.amazon.kinesis.leases.ShardInfo;
import software.amazon.kinesis.lifecycle.LifecycleConfig;
import software.amazon.kinesis.lifecycle.ShardConsumer;
import software.amazon.kinesis.metrics.MetricsConfig;
import software.amazon.kinesis.processor.ProcessorConfig;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;
import software.amazon.kinesis.retrieval.RetrievalConfig;
import software.amazon.kinesis.retrieval.RetrievalSpecificConfig;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
public class KclMessageListenerContainer implements MessageListenerContainer {

	private static final Logger logger = LoggerFactory.getLogger(KclMessageListenerContainer.class);

	private final KinesisAsyncClient kinesisClient;

	private final DynamoDbAsyncClient dynamoDbClient;

	private final CloudWatchAsyncClient cloudWatchClient;

	private final Collection<String> streamNames;

	private final String applicationName;

	private final KclContainerOptions options;

	private final Object lifecycleMonitor = new Object();

	private final KinesisMessageConverter messageConverter;

	private KclListenerMode listenerMode = KclListenerMode.SINGLE_RECORD;

	private KclCheckpointMode checkpointMode;

	private ErrorHandler errorHandler = new LoggingErrorHandler();

	private final Set<HaltedShard> haltedShards = ConcurrentHashMap.newKeySet();

	@Nullable
	private ApplicationEventPublisher applicationEventPublisher;

	@Nullable
	private MessageListener messageListener;

	@Nullable
	private BatchMessageListener batchMessageListener;

	@Nullable
	private String id;

	private volatile boolean running;

	@Nullable
	private Scheduler scheduler;

	@Nullable
	private ExecutorService executorService;

	@Nullable
	private ScheduledExecutorService releaseScheduler;

	public KclMessageListenerContainer(KinesisAsyncClient kinesisClient, DynamoDbAsyncClient dynamoDbClient,
			CloudWatchAsyncClient cloudWatchClient, String streamName, String applicationName,
			KclContainerOptions options) {
		this(kinesisClient, dynamoDbClient, cloudWatchClient, List.of(streamName), applicationName, options);
	}

	/**
	 * Creates a container consuming the given streams. Several streams are handled by one KCL application, and
	 * therefore share its lease table and workers.
	 * @param kinesisClient the Kinesis client.
	 * @param dynamoDbClient the DynamoDB client holding the lease table.
	 * @param cloudWatchClient the CloudWatch client KCL publishes metrics with.
	 * @param streamNames the streams to consume, must not be empty.
	 * @param applicationName the KCL application name.
	 * @param options the container options.
	 */
	public KclMessageListenerContainer(KinesisAsyncClient kinesisClient, DynamoDbAsyncClient dynamoDbClient,
			CloudWatchAsyncClient cloudWatchClient, Collection<String> streamNames, String applicationName,
			KclContainerOptions options) {
		Assert.notNull(kinesisClient, "kinesisClient must not be null");
		Assert.notNull(dynamoDbClient, "dynamoDbClient must not be null");
		Assert.notNull(cloudWatchClient, "cloudWatchClient must not be null");
		Assert.notEmpty(streamNames, "streamNames must not be empty");
		streamNames.forEach(streamName -> Assert.hasText(streamName, "streamName must not be empty"));
		Assert.hasText(applicationName, "applicationName must not be empty");
		Assert.notNull(options, "options must not be null");
		this.kinesisClient = kinesisClient;
		this.dynamoDbClient = dynamoDbClient;
		this.cloudWatchClient = cloudWatchClient;
		this.streamNames = List.copyOf(streamNames);
		this.applicationName = applicationName;
		this.options = options;
		KinesisMessageConverter configuredConverter = options.getMessageConverter();
		this.messageConverter = configuredConverter != null ? configuredConverter
				: new KinesisMessagingMessageConverter();
		this.checkpointMode = options.getCheckpointMode();
		this.id = String.join(",", this.streamNames);
	}

	public KclContainerOptions getContainerOptions() {
		return this.options;
	}

	/**
	 * The converter turning each KCL record into the message handed to the listener: the one set through
	 * {@link KclContainerOptions.Builder#messageConverter(KinesisMessageConverter)}, or the default
	 * {@link KinesisMessagingMessageConverter}.
	 * @return the message converter.
	 */
	public KinesisMessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * The streams this container consumes. More than one stream means the container runs KCL in multi-stream mode under
	 * a single application.
	 * @return the stream names.
	 */
	public Collection<String> getStreamNames() {
		return this.streamNames;
	}

	public void setMessageListener(MessageListener messageListener) {
		Assert.notNull(messageListener, "messageListener must not be null");
		this.messageListener = messageListener;
	}

	@Nullable
	public MessageListener getMessageListener() {
		return this.messageListener;
	}

	public void setBatchMessageListener(BatchMessageListener batchMessageListener) {
		Assert.notNull(batchMessageListener, "batchMessageListener must not be null");
		this.batchMessageListener = batchMessageListener;
	}

	@Nullable
	public BatchMessageListener getBatchMessageListener() {
		return this.batchMessageListener;
	}

	/**
	 * Set whether records are delivered one at a time to the {@link #setMessageListener(MessageListener) message
	 * listener} ({@link KclListenerMode#SINGLE_RECORD}, the default) or as a batch to the
	 * {@link #setBatchMessageListener(BatchMessageListener) batch message listener} ({@link KclListenerMode#BATCH}).
	 * The listener matching the mode must be set before the container starts.
	 * @param listenerMode the listener mode.
	 */
	public void setListenerMode(KclListenerMode listenerMode) {
		Assert.notNull(listenerMode, "listenerMode must not be null");
		this.listenerMode = listenerMode;
	}

	public KclListenerMode getListenerMode() {
		return this.listenerMode;
	}

	public void setCheckpointMode(KclCheckpointMode checkpointMode) {
		Assert.notNull(checkpointMode, "checkpointMode must not be null");
		this.checkpointMode = checkpointMode;
	}

	public KclCheckpointMode getCheckpointMode() {
		return this.checkpointMode;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "errorHandler must not be null");
		this.errorHandler = errorHandler;
	}

	public ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	/**
	 * Set the publisher the container uses to emit a {@link KclShardHaltedEvent} whenever a shard halts. When unset, no
	 * event is published and {@link #getHaltedShards()} is still kept up to date.
	 * @param applicationEventPublisher the event publisher.
	 */
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * The shards this container has halted and not yet recovered, each as a {@link HaltedShard} of its stream and shard
	 * id. A shard is removed when its processor ends (its lease is lost, the shard ends after a successful end-of-shard
	 * checkpoint, or a shutdown is requested) or when a new processor for the same stream and shard initializes, so a
	 * shard that was released and reacquired is no longer reported. The stream is part of the key, so shards of
	 * different streams in a multi-stream application never collide.
	 * @return an immutable snapshot of the halted shards.
	 */
	public Set<HaltedShard> getHaltedShards() {
		return Set.copyOf(this.haltedShards);
	}

	@Override
	public String getId() {
		return this.id != null ? this.id : String.join(",", this.streamNames);
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				return;
			}
			assertListenerMatchesMode();
			normalizeCheckpointMode();
			this.haltedShards.clear();
			if (this.options.getHaltedShardReleaseDelay() != null) {
				this.releaseScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
					Thread thread = new Thread(runnable, "kcl-release-" + getId());
					thread.setDaemon(true);
					return thread;
				});
			}
			Scheduler currentScheduler = createScheduler();
			this.scheduler = currentScheduler;
			this.executorService = Executors.newSingleThreadExecutor(runnable -> {
				Thread thread = new Thread(runnable, "kcl-" + getId());
				thread.setDaemon(true);
				return thread;
			});
			this.executorService.submit(() -> runScheduler(currentScheduler));
			this.running = true;
			logger.info("Started KCL container '{}' for streams {} (application '{}')", getId(), this.streamNames,
					this.applicationName);
		}
	}

	/**
	 * Fails the start when the listener for the configured {@link KclListenerMode} is missing. Without this check the
	 * container would start and then halt every shard on its first batch.
	 */
	private void assertListenerMatchesMode() {
		switch (this.listenerMode) {
		case SINGLE_RECORD -> Assert.state(this.messageListener != null,
				() -> "Container '" + getId() + "' is in SINGLE_RECORD listener mode but no messageListener is set; "
						+ "call setMessageListener(...), or setListenerMode(KclListenerMode.BATCH) to use the "
						+ "batchMessageListener");
		case BATCH -> Assert.state(this.batchMessageListener != null,
				() -> "Container '" + getId() + "' is in BATCH listener mode but no batchMessageListener is set; "
						+ "call setBatchMessageListener(...), or setListenerMode(KclListenerMode.SINGLE_RECORD) to "
						+ "use the messageListener");
		}
	}

	/**
	 * Downgrades {@link KclCheckpointMode#RECORD} to {@link KclCheckpointMode#BATCH} for batch listeners: a batch
	 * listener has no per-record completion boundary, so there is nothing to checkpoint a single record against.
	 */
	private void normalizeCheckpointMode() {
		if (this.listenerMode == KclListenerMode.BATCH && this.checkpointMode == KclCheckpointMode.RECORD) {
			logger.warn("Checkpoint mode of container '{}' is overridden from RECORD to BATCH, "
					+ "because RECORD has no meaning for a batch listener", getId());
			this.checkpointMode = KclCheckpointMode.BATCH;
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				return;
			}
			this.running = false;
			Scheduler currentScheduler = this.scheduler;
			if (currentScheduler != null) {
				gracefulShutdown(currentScheduler);
			}
			ExecutorService currentExecutor = this.executorService;
			if (currentExecutor != null) {
				currentExecutor.shutdownNow();
			}
			ScheduledExecutorService currentReleaseScheduler = this.releaseScheduler;
			if (currentReleaseScheduler != null) {
				currentReleaseScheduler.shutdownNow();
			}
			this.scheduler = null;
			this.executorService = null;
			this.releaseScheduler = null;
			logger.info("Stopped KCL container '{}' for streams {}", getId(), this.streamNames);
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return this.options.getPhase();
	}

	@Override
	public boolean isAutoStartup() {
		return this.options.isAutoStartup();
	}

	private void gracefulShutdown(Scheduler currentScheduler) {
		try {
			Future<Boolean> shutdownComplete = currentScheduler.startGracefulShutdown();
			Boolean completed = shutdownComplete.get(this.options.getGracefulShutdownTimeout().toMillis(),
					TimeUnit.MILLISECONDS);
			if (!Boolean.TRUE.equals(completed)) {
				logger.warn("Graceful shutdown of KCL container '{}' did not complete cleanly", getId());
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			currentScheduler.shutdown();
		}
		catch (Exception ex) {
			logger.warn("Error during graceful shutdown of KCL container '{}': {}", getId(), ex.getMessage());
			currentScheduler.shutdown();
		}
	}

	private void runScheduler(Scheduler schedulerToRun) {
		try {
			schedulerToRun.run();
		}
		catch (Throwable ex) {
			logger.error("KCL scheduler for container '{}' terminated abnormally", getId(), ex);
		}
		finally {
			synchronized (this.lifecycleMonitor) {
				if (this.scheduler == schedulerToRun) {
					this.running = false;
					this.scheduler = null;
					ExecutorService currentExecutor = this.executorService;
					this.executorService = null;
					ScheduledExecutorService currentReleaseScheduler = this.releaseScheduler;
					this.releaseScheduler = null;
					if (currentExecutor != null) {
						currentExecutor.shutdown();
					}
					if (currentReleaseScheduler != null) {
						currentReleaseScheduler.shutdownNow();
					}
				}
			}
		}
	}

	private Scheduler createScheduler() {
		InitialPositionInStreamExtended initialPosition = resolveInitialPosition();
		ConfigsBuilder configsBuilder = getConfigsBuilder(initialPosition);

		RetrievalSpecificConfig retrievalSpecificConfig = createRetrievalConfigurer()
				.createRetrievalConfig(this.streamNames, this.applicationName, this.kinesisClient, this.options);

		CheckpointConfig checkpointConfig = configsBuilder.checkpointConfig();
		CoordinatorConfig coordinatorConfig = configsBuilder.coordinatorConfig();
		LeaseManagementConfig leaseManagementConfig = configsBuilder.leaseManagementConfig()
				.initialPositionInStream(initialPosition);
		LifecycleConfig lifecycleConfig = configsBuilder.lifecycleConfig();
		MetricsConfig metricsConfig = configsBuilder.metricsConfig();
		ProcessorConfig processorConfig = configsBuilder.processorConfig();
		RetrievalConfig retrievalConfig = configsBuilder.retrievalConfig()
				.retrievalSpecificConfig(retrievalSpecificConfig);

		if (this.options.getMetricsLevel() != null) {
			metricsConfig.metricsLevel(this.options.getMetricsLevel());
		}
		if (this.options.getBillingMode() != null) {
			leaseManagementConfig.billingMode(this.options.getBillingMode());
		}
		if (this.checkpointMode == KclCheckpointMode.PERIODIC) {
			processorConfig.callProcessRecordsEvenForEmptyRecordList(true);
		}

		applyCustomizer(this.options.getCheckpointConfigCustomizer(), checkpointConfig);
		applyCustomizer(this.options.getCoordinatorConfigCustomizer(), coordinatorConfig);
		applyCustomizer(this.options.getLeaseManagementConfigCustomizer(), leaseManagementConfig);
		applyCustomizer(this.options.getLifecycleConfigCustomizer(), lifecycleConfig);
		applyCustomizer(this.options.getMetricsConfigCustomizer(), metricsConfig);
		applyCustomizer(this.options.getProcessorConfigCustomizer(), processorConfig);
		applyCustomizer(this.options.getRetrievalConfigCustomizer(), retrievalConfig);

		return new Scheduler(checkpointConfig, coordinatorConfig, leaseManagementConfig, lifecycleConfig, metricsConfig,
				processorConfig, retrievalConfig);
	}

	private ConfigsBuilder getConfigsBuilder(InitialPositionInStreamExtended initialPosition) {
		ShardRecordProcessorFactory processorFactory = new ShardRecordProcessorFactory() {

			@Override
			public ShardRecordProcessor shardRecordProcessor() {
				return createRecordProcessor(KclMessageListenerContainer.this.streamNames.iterator().next(), null);
			}

			@Override
			public ShardRecordProcessor shardRecordProcessor(StreamIdentifier streamIdentifier) {
				// KCL prefixes the lease key with the serialized StreamIdentifier only in multi-stream mode; a
				// single-stream application keeps the bare shard id as its lease key even though this overload runs.
				String leaseKeyPrefix = KclMessageListenerContainer.this.streamNames.size() > 1
						? streamIdentifier.serialize()
						: null;
				return createRecordProcessor(streamIdentifier.streamName(), leaseKeyPrefix);
			}

		};
		ConfigsBuilder configsBuilder = new ConfigsBuilder(
				StreamTrackerFactory.createStreamTracker(this.streamNames, initialPosition, this.kinesisClient),
				this.applicationName, this.kinesisClient, this.dynamoDbClient, this.cloudWatchClient,
				this.options.getWorkerIdentifier(), processorFactory);
		if (this.options.getLeaseTableName() != null) {
			configsBuilder.tableName(this.options.getLeaseTableName());
		}
		if (this.options.getMetricsNamespace() != null) {
			configsBuilder.namespace(this.options.getMetricsNamespace());
		}
		return configsBuilder;
	}

	private KclShardRecordProcessor createRecordProcessor(String streamName, @Nullable String leaseKeyPrefix) {
		boolean releaseEnabled = this.options.getHaltedShardReleaseDelay() != null;
		ShardReleaser releaser = releaseEnabled ? this::releaseLease : null;
		KclShardRecordProcessor.ShardHaltCallback haltCallback = new KclShardRecordProcessor.ShardHaltCallback() {
			@Override
			public void onShardHalted(@Nullable String shardId, Throwable cause, @Nullable Long millisBehindLatest) {
				KclMessageListenerContainer.this.onShardHalted(streamName, shardId, cause, millisBehindLatest);
			}

			@Override
			public void onProcessorInitialized(@Nullable String shardId) {
				removeHaltedShard(streamName, shardId);
			}

			@Override
			public void onProcessorEnded(@Nullable String shardId) {
				removeHaltedShard(streamName, shardId);
			}
		};
		return new KclShardRecordProcessor(this.messageConverter, this.options.getPayloadContentType(),
				this.listenerMode, this.checkpointMode, this.options.getCheckpointRecordCount(),
				this.options.getCheckpointInterval(), this.messageListener, this.batchMessageListener,
				this.errorHandler, streamName, this.options.getObservationRegistry(),
				this.options.getObservationConvention(), leaseKeyPrefix, this.options.getHaltedShardReleaseDelay(),
				releaser, this.releaseScheduler, haltCallback);
	}

	private void onShardHalted(String streamName, @Nullable String shardId, Throwable cause,
			@Nullable Long millisBehindLatest) {
		if (shardId != null) {
			this.haltedShards.add(new HaltedShard(streamName, shardId));
		}
		ApplicationEventPublisher publisher = this.applicationEventPublisher;
		if (publisher != null) {
			publisher.publishEvent(
					new KclShardHaltedEvent(this, getId(), streamName, shardId, cause, millisBehindLatest));
		}
	}

	private void removeHaltedShard(String streamName, @Nullable String shardId) {
		if (shardId != null) {
			this.haltedShards.remove(new HaltedShard(streamName, shardId));
		}
	}

	/**
	 * Gives up a halted shard so KCL processes it again from its last checkpoint, without restarting the application. A
	 * scheduler that has already stopped is a no-op.
	 * <p>
	 * The shard's {@link ShardConsumer} is shut down with {@code leaseLost()}: its processing loop
	 * ({@link Scheduler#runProcessLoop()}) then rebuilds a fresh consumer for the shard the worker still holds the
	 * lease for, replaying the records after the last checkpoint. Dropping only the lease is not enough on a single
	 * worker: the worker reacquires the lease it still owns before the consumer is cleaned up, so the halted consumer
	 * keeps its position and nothing is redelivered.
	 * <p>
	 * The lease is also dropped so that, when other workers are running, one of them can take the shard. The drop is by
	 * key rather than through {@code getCurrentlyHeldLease(leaseKey)}: that lookup returns a copy only while it is
	 * still within the lease duration, so a short failover time or a release delay longer than the lease duration would
	 * make it return {@code null}. {@code dropLease} reads only the key, so a {@link Lease} carrying just the key
	 * removes it from the renewer's held leases either way.
	 */
	private void releaseLease(String leaseKey) {
		Scheduler currentScheduler = this.scheduler;
		if (currentScheduler == null) {
			return;
		}
		try {
			boolean consumerShutDown = shutDownShardConsumer(currentScheduler, leaseKey);
			Lease lease = new Lease();
			lease.leaseKey(leaseKey);
			currentScheduler.leaseCoordinator().dropLease(lease);
			if (consumerShutDown) {
				logger.info(
						"Released lease {} of container '{}'; the shard will be reprocessed from its last checkpoint",
						leaseKey, getId());
			}
			else {
				logger.debug("No active shard consumer found for lease {} of container '{}'; dropped the lease only",
						leaseKey, getId());
			}
		}
		catch (RuntimeException ex) {
			logger.warn("Failed to release lease {} of container '{}': {}", leaseKey, getId(), ex.getMessage());
		}
	}

	/**
	 * Shuts down the {@link ShardConsumer} whose lease key matches, with {@code leaseLost()}, so the scheduler rebuilds
	 * a fresh consumer that replays from the last checkpoint.
	 * @return {@code true} if a matching consumer was found and shut down.
	 */
	private boolean shutDownShardConsumer(Scheduler currentScheduler, String leaseKey) {
		for (Map.Entry<ShardInfo, ShardConsumer> entry : currentScheduler.shardInfoShardConsumerMap().entrySet()) {
			if (leaseKey.equals(ShardInfo.getLeaseKey(entry.getKey()))) {
				entry.getValue().leaseLost();
				return true;
			}
		}
		return false;
	}

	private InitialPositionInStreamExtended resolveInitialPosition() {
		if (this.options.getInitialPositionInStream() == InitialPositionInStream.AT_TIMESTAMP) {
			Assert.state(this.options.getInitialPositionTimestamp() != null,
					"initialPositionTimestamp must be set when initialPositionInStream is AT_TIMESTAMP");
			return InitialPositionInStreamExtended
					.newInitialPositionAtTimestamp(Date.from(this.options.getInitialPositionTimestamp()));
		}
		return InitialPositionInStreamExtended.newInitialPosition(this.options.getInitialPositionInStream());
	}

	private static <T> void applyCustomizer(@Nullable Consumer<T> customizer, T config) {
		if (customizer != null) {
			customizer.accept(config);
		}
	}

	private KclRetrievalConfigurer createRetrievalConfigurer() {
		return switch (this.options.getRetrievalMode()) {
		case POLLING -> new PollingRetrievalConfigurer();
		case ENHANCED_FAN_OUT -> new FanOutRetrievalConfigurer();
		};
	}

}
