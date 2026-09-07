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
import io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import software.amazon.kinesis.leases.LeaseManagementConfig;
import software.amazon.kinesis.lifecycle.LifecycleConfig;
import software.amazon.kinesis.metrics.MetricsConfig;
import software.amazon.kinesis.processor.ProcessorConfig;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;
import software.amazon.kinesis.retrieval.RetrievalConfig;
import software.amazon.kinesis.retrieval.RetrievalSpecificConfig;

/**
 * @author Matej Nedic
 * @since 4.2.0
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

	private final KinesisMessagingMessageConverter messageConverter;

	private KclListenerMode listenerMode = KclListenerMode.SINGLE_RECORD;

	private KclCheckpointMode checkpointMode;

	private ErrorHandler errorHandler = new LoggingErrorHandler();

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
		this.messageConverter = new KinesisMessagingMessageConverter();
		if (options.getPayloadContentType() != null) {
			this.messageConverter.setContentType(options.getPayloadContentType());
		}
		this.checkpointMode = options.getCheckpointMode();
		this.id = String.join(",", this.streamNames);
	}

	public KclContainerOptions getContainerOptions() {
		return this.options;
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

	public void setListenerMode(KclListenerMode listenerMode) {
		Assert.notNull(listenerMode, "listenerMode must not be null");
		this.listenerMode = listenerMode;
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
			Assert.state(this.messageListener != null || this.batchMessageListener != null,
					"A messageListener or batchMessageListener must be set before starting the container");
			normalizeCheckpointMode();
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
			this.scheduler = null;
			this.executorService = null;
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
				return createRecordProcessor(KclMessageListenerContainer.this.streamNames.iterator().next());
			}

			@Override
			public ShardRecordProcessor shardRecordProcessor(StreamIdentifier streamIdentifier) {
				return createRecordProcessor(streamIdentifier.streamName());
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

	private KclShardRecordProcessor createRecordProcessor(String streamName) {
		return new KclShardRecordProcessor(this.messageConverter, this.listenerMode, this.checkpointMode,
				this.options.getCheckpointRecordCount(), this.options.getCheckpointInterval(), this.messageListener,
				this.batchMessageListener, this.errorHandler, streamName);
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
