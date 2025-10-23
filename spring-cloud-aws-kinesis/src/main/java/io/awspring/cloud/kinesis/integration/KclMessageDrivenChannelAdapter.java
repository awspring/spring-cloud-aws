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

import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryDeserializer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.log.LogMessage;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.common.StreamConfig;
import software.amazon.kinesis.common.StreamIdentifier;
import software.amazon.kinesis.coordinator.CoordinatorConfig;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.exceptions.ThrottlingException;
import software.amazon.kinesis.leases.LeaseManagementConfig;
import software.amazon.kinesis.lifecycle.LifecycleConfig;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.metrics.MetricsConfig;
import software.amazon.kinesis.metrics.MetricsLevel;
import software.amazon.kinesis.metrics.NullMetricsFactory;
import software.amazon.kinesis.processor.FormerStreamsLeasesDeletionStrategy;
import software.amazon.kinesis.processor.MultiStreamTracker;
import software.amazon.kinesis.processor.ProcessorConfig;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;
import software.amazon.kinesis.processor.SingleStreamTracker;
import software.amazon.kinesis.processor.StreamTracker;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import software.amazon.kinesis.retrieval.RetrievalConfig;
import software.amazon.kinesis.retrieval.RetrievalSpecificConfig;
import software.amazon.kinesis.retrieval.fanout.FanOutConfig;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

/**
 * The {@link MessageProducerSupport} implementation for receiving data from Amazon Kinesis stream(s) using AWS KCL.
 *
 * @author Hervé Fortin
 * @author Artem Bilan
 * @author Dirk Bonhomme
 * @author Siddharth Jain
 * @author Minkyu Moon
 *
 * @since 4.0
 */
@ManagedResource
@IntegrationManagedResource
public class KclMessageDrivenChannelAdapter extends MessageProducerSupport implements ApplicationEventPublisherAware {

	private static final ThreadLocal<AttributeAccessor> attributesHolder = new ThreadLocal<>();

	private final ShardRecordProcessorFactory recordProcessorFactory = new RecordProcessorFactory();

	private final String[] streams;

	private final KinesisAsyncClient kinesisClient;

	private final CloudWatchAsyncClient cloudWatchClient;

	private final DynamoDbAsyncClient dynamoDBClient;

	private TaskExecutor executor = new SimpleAsyncTaskExecutor();

	private String consumerGroup = "SpringIntegration";

	@Nullable
	private String leaseTableName;

	private InboundMessageMapper<byte[]> embeddedHeadersMapper;

	private ConfigsBuilder config;

	private InitialPositionInStreamExtended streamInitialSequence = InitialPositionInStreamExtended
			.newInitialPosition(InitialPositionInStream.LATEST);

	private int consumerBackoff = 1000;

	private Converter<byte[], Object> converter = new DeserializingConverter();

	private ListenerMode listenerMode = ListenerMode.record;

	private long checkpointsInterval = 5_000L;

	private CheckpointMode checkpointMode = CheckpointMode.batch;

	private String workerId = UUID.randomUUID().toString();

	private GlueSchemaRegistryDeserializer glueSchemaRegistryDeserializer;

	private boolean bindSourceRecord;

	private boolean fanOut = true;

	private ApplicationEventPublisher applicationEventPublisher;

	private volatile Scheduler scheduler;

	private MetricsLevel metricsLevel = MetricsLevel.DETAILED;

	private Consumer<CoordinatorConfig> coordinatorConfigCustomizer = (config) -> {
	};

	private Consumer<LifecycleConfig> lifecycleConfigCustomizer = (config) -> {
	};

	private Consumer<MetricsConfig> metricsConfigCustomizer = (config) -> {
	};

	private Consumer<LeaseManagementConfig> leaseManagementConfigCustomizer = (config) -> {
	};

	private boolean emptyRecordList;

	private int pollingMaxRecords = PollingConfig.DEFAULT_MAX_RECORDS;

	private long pollingIdleTime = 1500L;

	private long gracefulShutdownTimeout;

	public KclMessageDrivenChannelAdapter(String... streams) {
		this(KinesisAsyncClient.create(), CloudWatchAsyncClient.create(), DynamoDbAsyncClient.create(), streams);
	}

	public KclMessageDrivenChannelAdapter(Region region, String... streams) {
		this(KinesisAsyncClient.builder().region(region).build(),
				CloudWatchAsyncClient.builder().region(region).build(),
				DynamoDbAsyncClient.builder().region(region).build(), streams);
	}

	public KclMessageDrivenChannelAdapter(KinesisAsyncClient kinesisClient, CloudWatchAsyncClient cloudWatchClient,
			DynamoDbAsyncClient dynamoDBClient, String... streams) {

		Assert.notNull(kinesisClient, "'kinesisClient' must not be null.");
		Assert.notNull(cloudWatchClient, "'cloudWatchClient' must not be null.");
		Assert.notNull(dynamoDBClient, "'dynamoDBClient' must not be null.");
		Assert.notEmpty(streams, "'streams' must not be empty.");
		this.streams = streams;
		this.kinesisClient = kinesisClient;
		this.cloudWatchClient = cloudWatchClient;
		this.dynamoDBClient = dynamoDBClient;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void setExecutor(TaskExecutor executor) {
		Assert.notNull(executor, "'executor' must not be null.");
		this.executor = executor;
	}

	public void setConsumerGroup(String consumerGroup) {
		Assert.hasText(consumerGroup, "'consumerGroup' must not be empty");
		this.consumerGroup = consumerGroup;
	}

	public String getConsumerGroup() {
		return this.consumerGroup;
	}

	/**
	 * Set a name of the DynamoDB table name for leases. Defaults to {@link #consumerGroup}.
	 * @param leaseTableName the DynamoDB table name for leases.
	 */
	public void setLeaseTableName(String leaseTableName) {
		this.leaseTableName = leaseTableName;
	}

	/**
	 * Specify an {@link InboundMessageMapper} to extract message headers embedded into the record data.
	 * @param embeddedHeadersMapper the {@link InboundMessageMapper} to use.
	 */
	public void setEmbeddedHeadersMapper(InboundMessageMapper<byte[]> embeddedHeadersMapper) {
		this.embeddedHeadersMapper = embeddedHeadersMapper;
	}

	public void setStreamInitialSequence(InitialPositionInStreamExtended streamInitialSequence) {
		Assert.notNull(streamInitialSequence, "'streamInitialSequence' must not be null");
		this.streamInitialSequence = streamInitialSequence;
	}

	public void setConsumerBackoff(int consumerBackoff) {
		this.consumerBackoff = Math.max(1000, consumerBackoff);
	}

	/**
	 * Specify a {@link Converter} to deserialize the {@code byte[]} from record's body. Can be {@code null} meaning no
	 * deserialization.
	 * @param converter the {@link Converter} to use or null
	 */
	public void setConverter(Converter<byte[], Object> converter) {
		this.converter = converter;
	}

	public void setListenerMode(ListenerMode listenerMode) {
		Assert.notNull(listenerMode, "'listenerMode' must not be null");
		this.listenerMode = listenerMode;
	}

	/**
	 * Sets the interval between 2 checkpoints.
	 * @param checkpointsInterval interval between 2 checkpoints (in milliseconds)
	 */
	public void setCheckpointsInterval(long checkpointsInterval) {
		this.checkpointsInterval = checkpointsInterval;
	}

	public void setCheckpointMode(CheckpointMode checkpointMode) {
		Assert.notNull(checkpointMode, "'checkpointMode' must not be null");
		this.checkpointMode = checkpointMode;
	}

	/**
	 * Sets the worker identifier used to distinguish different workers/processes of a Kinesis application.
	 * @param workerId the worker identifier to use
	 */
	public void setWorkerId(String workerId) {
		Assert.hasText(workerId, "'workerId' must not be null or empty");
		this.workerId = workerId;
	}

	public void setGlueSchemaRegistryDeserializer(GlueSchemaRegistryDeserializer glueSchemaRegistryDeserializer) {
		this.glueSchemaRegistryDeserializer = glueSchemaRegistryDeserializer;
	}

	/**
	 * Set to true to bind the source consumer record in the header named
	 * {@link IntegrationMessageHeaderAccessor#SOURCE_DATA}. Does not apply to batch listeners.
	 * @param bindSourceRecord true to bind.
	 */
	public void setBindSourceRecord(boolean bindSourceRecord) {
		this.bindSourceRecord = bindSourceRecord;
	}

	/**
	 * Specify a retrieval strategy: fan-out (true; default) or polling (false).
	 * @param fanOut false for a polling retrieval strategy.
	 */
	public void setFanOut(boolean fanOut) {
		this.fanOut = fanOut;
	}

	/**
	 * Specify a metrics level to emit. Defaults to {@link MetricsLevel#DETAILED}.
	 * @param metricsLevel the {@link MetricsLevel} for emitting (or not) metrics into Cloud Watch.
	 */
	public void setMetricsLevel(MetricsLevel metricsLevel) {
		Assert.notNull(metricsLevel, "'metricsLevel' must not be null");
		this.metricsLevel = metricsLevel;
	}

	/**
	 * Set a {@link Consumer} to configure a {@link CoordinatorConfig}.
	 * @param coordinatorConfigCustomizer the {@link Consumer} to configure a {@link CoordinatorConfig}.
	 * @see CoordinatorConfig
	 */
	public void setCoordinatorConfigCustomizer(Consumer<CoordinatorConfig> coordinatorConfigCustomizer) {
		Assert.notNull(coordinatorConfigCustomizer, "'coordinatorConfigCustomizer' must not be null");
		this.coordinatorConfigCustomizer = coordinatorConfigCustomizer;
	}

	/**
	 * Set a {@link Consumer} to configure a {@link LifecycleConfig}.
	 * @param lifecycleConfigCustomizer the {@link Consumer} to configure a {@link LifecycleConfig}.
	 * @see LifecycleConfig
	 */
	public void setLifecycleConfigCustomizer(Consumer<LifecycleConfig> lifecycleConfigCustomizer) {
		Assert.notNull(lifecycleConfigCustomizer, "'lifecycleConfigCustomizer' must not be null");
		this.lifecycleConfigCustomizer = lifecycleConfigCustomizer;
	}

	/**
	 * Set a {@link Consumer} to configure a {@link MetricsConfig}. May override whatever could be set individually,
	 * like {@link #setMetricsLevel(MetricsLevel)}.
	 * @param metricsConfigCustomizer the {@link Consumer} to configure a {@link MetricsConfig}.
	 * @see MetricsConfig
	 */
	public void setMetricsConfigCustomizer(Consumer<MetricsConfig> metricsConfigCustomizer) {
		Assert.notNull(metricsConfigCustomizer, "'metricsConfigCustomizer' must not be null");
		this.metricsConfigCustomizer = metricsConfigCustomizer;
	}

	/**
	 * Set a {@link Consumer} to configure a {@link LeaseManagementConfig}.
	 * @param leaseManagementConfigCustomizer the {@link Consumer} to configure a {@link LeaseManagementConfig}.
	 * @see LeaseManagementConfig
	 */
	public void setLeaseManagementConfigCustomizer(Consumer<LeaseManagementConfig> leaseManagementConfigCustomizer) {
		Assert.notNull(leaseManagementConfigCustomizer, "'leaseManagementConfigCustomizer' must not be null");
		this.leaseManagementConfigCustomizer = leaseManagementConfigCustomizer;
	}

	/**
	 * Whether to return an empty record list from the consumer to the processor. Works only in
	 * {@link ListenerMode#batch} mode. The message will be sent into the output channel with an empty {@link List} as a
	 * payload.
	 * @param emptyRecordList true to return an empty record list.
	 * @see ProcessorConfig#callProcessRecordsEvenForEmptyRecordList(boolean)
	 */
	public void setEmptyRecordList(boolean emptyRecordList) {
		this.emptyRecordList = emptyRecordList;
	}

	/**
	 * The number of records to poll from Kinesis when using {@link PollingConfig}.
	 * @param pollingMaxRecords the number of records to poll from Kinesis.
	 * @see PollingConfig#maxRecords(int)
	 */
	public void setPollingMaxRecords(int pollingMaxRecords) {
		this.pollingMaxRecords = pollingMaxRecords;
	}

	/**
	 * The idle timeout between polls when using {@link PollingConfig}.
	 * @param pollingIdleTime idle timeout between polls.
	 * @see PollingConfig#idleTimeBetweenReadsInMillis(long)
	 */
	public void setPollingIdleTime(long pollingIdleTime) {
		this.pollingIdleTime = pollingIdleTime;
	}

	/**
	 * The timeout for {@link Scheduler#startGracefulShutdown()}. Defaults to {@code 0} with the meaning to call
	 * {@link Scheduler#shutdown()}.
	 * @param gracefulShutdownTimeout the timeout for {@link Scheduler#startGracefulShutdown()}.
	 * @see Scheduler#startGracefulShutdown()
	 */
	public void setGracefulShutdownTimeout(long gracefulShutdownTimeout) {
		this.gracefulShutdownTimeout = gracefulShutdownTimeout;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.listenerMode.equals(ListenerMode.record) && this.emptyRecordList) {
			this.emptyRecordList = false;
			logger.warn("The 'emptyRecordList' is processed only in the [ListenerMode.batch].");
		}
		this.config = new ConfigsBuilder(buildStreamTracker(), this.consumerGroup, this.kinesisClient,
				this.dynamoDBClient, this.cloudWatchClient, this.workerId, this.recordProcessorFactory);

		if (this.leaseTableName != null) {
			this.config.tableName(this.leaseTableName);
		}
	}

	private StreamTracker buildStreamTracker() {
		if (this.streams.length == 1) {
			return new SingleStreamTracker(StreamIdentifier.singleStreamInstance(this.streams[0]),
					this.streamInitialSequence);
		}
		else {
			return new StreamsTracker();
		}
	}

	@Override
	protected void doStart() {
		super.doStart();

		if (ListenerMode.batch.equals(this.listenerMode) && CheckpointMode.record.equals(this.checkpointMode)) {
			this.checkpointMode = CheckpointMode.batch;
			logger.warn("The 'checkpointMode' is overridden from [CheckpointMode.record] to [CheckpointMode.batch] "
					+ "because it does not make sense in case of [ListenerMode.batch].");
		}

		LifecycleConfig lifecycleConfig = this.config.lifecycleConfig();
		lifecycleConfig.taskBackoffTimeMillis(this.consumerBackoff);
		this.lifecycleConfigCustomizer.accept(lifecycleConfig);

		RetrievalSpecificConfig retrievalSpecificConfig;
		String singleStreamName = this.streams.length == 1 ? this.streams[0] : null;
		if (this.fanOut) {
			retrievalSpecificConfig = new FanOutConfig(this.kinesisClient).applicationName(this.consumerGroup)
					.streamName(singleStreamName);
		}
		else {
			retrievalSpecificConfig = new PollingConfig(this.kinesisClient).streamName(singleStreamName)
					.maxRecords(this.pollingMaxRecords).idleTimeBetweenReadsInMillis(this.pollingIdleTime);
		}

		RetrievalConfig retrievalConfig = this.config.retrievalConfig()
				.glueSchemaRegistryDeserializer(this.glueSchemaRegistryDeserializer)
				.retrievalSpecificConfig(retrievalSpecificConfig);

		MetricsConfig metricsConfig = this.config.metricsConfig();
		metricsConfig.metricsLevel(this.metricsLevel);
		if (MetricsLevel.NONE.equals(this.metricsLevel)) {
			metricsConfig.metricsFactory(new NullMetricsFactory());
		}
		this.metricsConfigCustomizer.accept(metricsConfig);

		CoordinatorConfig coordinatorConfig = this.config.coordinatorConfig();
		this.coordinatorConfigCustomizer.accept(coordinatorConfig);

		LeaseManagementConfig leaseManagementConfig = this.config.leaseManagementConfig();
		this.leaseManagementConfigCustomizer.accept(leaseManagementConfig);

		ProcessorConfig processorConfig = this.config.processorConfig()
				.callProcessRecordsEvenForEmptyRecordList(this.emptyRecordList);

		this.scheduler = new Scheduler(this.config.checkpointConfig(), coordinatorConfig, leaseManagementConfig,
				lifecycleConfig, metricsConfig, processorConfig, retrievalConfig);

		this.executor.execute(this.scheduler);
	}

	/**
	 * Takes no action by default. Subclasses may override this if they need lifecycle-managed behavior.
	 */
	@Override
	protected void doStop() {
		super.doStop();
		if (this.gracefulShutdownTimeout == 0) {
			this.scheduler.shutdown();
		}
		else {
			try {
				logger.info("Start graceful shutdown for KCL...");
				this.scheduler.startGracefulShutdown().get(this.gracefulShutdownTimeout, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException | ExecutionException | TimeoutException ex) {
				throw new RuntimeException("Graceful shutdown for KCL has failed.", ex);
			}
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		if (isRunning()) {
			stop();
		}
	}

	@Override
	protected AttributeAccessor getErrorMessageAttributes(Message<?> message) {
		AttributeAccessor attributes = attributesHolder.get();
		if (attributes == null) {
			return super.getErrorMessageAttributes(message);
		}
		else {
			return attributes;
		}
	}

	@Override
	public String toString() {
		return "KclMessageDrivenChannelAdapter{consumerGroup='" + this.consumerGroup + '\'' + ", stream(s)='"
				+ Arrays.toString(this.streams) + "'}";
	}

	private final class RecordProcessorFactory implements ShardRecordProcessorFactory {

		RecordProcessorFactory() {
		}

		@Override
		public ShardRecordProcessor shardRecordProcessor() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ShardRecordProcessor shardRecordProcessor(StreamIdentifier streamIdentifier) {
			return new RecordProcessor(streamIdentifier.streamName());
		}

	}

	private final class StreamsTracker implements MultiStreamTracker {

		private final FormerStreamsLeasesDeletionStrategy formerStreamsLeasesDeletionStrategy = new FormerStreamsLeasesDeletionStrategy.AutoDetectionAndDeferredDeletionStrategy() {

			@Override
			public Duration waitPeriodToDeleteFormerStreams() {
				return Duration.ZERO;
			}

		};

		private final Flux<StreamConfig> streamConfigs = Flux.fromArray(KclMessageDrivenChannelAdapter.this.streams)
				.flatMap((streamName) -> Mono.fromFuture(KclMessageDrivenChannelAdapter.this.kinesisClient
						.describeStreamSummary(request -> request.streamName(streamName))))
				.map(DescribeStreamSummaryResponse::streamDescriptionSummary)
				.map((summary) -> StreamIdentifier.multiStreamInstance(Arn.fromString(summary.streamARN()),
						summary.streamCreationTimestamp().getEpochSecond()))
				.map((streamIdentifier) -> new StreamConfig(streamIdentifier,
						KclMessageDrivenChannelAdapter.this.streamInitialSequence))
				.cache();

		StreamsTracker() {
		}

		@Override
		public List<StreamConfig> streamConfigList() {
			return this.streamConfigs.collectList().block();
		}

		@Override
		public FormerStreamsLeasesDeletionStrategy formerStreamsLeasesDeletionStrategy() {
			return this.formerStreamsLeasesDeletionStrategy;
		}

	}

	/**
	 * Processes records and checkpoints progress.
	 */
	private final class RecordProcessor implements ShardRecordProcessor {

		private final String stream;

		private String shardId;

		private long nextCheckpointTimeInMillis;

		RecordProcessor(String stream) {
			this.stream = stream;
		}

		@Override
		public void initialize(InitializationInput initializationInput) {
			this.shardId = initializationInput.shardId();
			logger.info(() -> "Initializing record processor for shard: " + this.shardId);
		}

		@Override
		public void leaseLost(LeaseLostInput leaseLostInput) {

		}

		@Override
		public void shardEnded(ShardEndedInput shardEndedInput) {
			logger.info(LogMessage.format("Shard [%s] ended; checkpointing...", this.shardId));
			try {
				shardEndedInput.checkpointer().checkpoint();
			}
			catch (ShutdownException | InvalidStateException ex) {
				logger.error(ex, "Exception while checkpointing at requested shutdown. Giving up");
			}

			if (KclMessageDrivenChannelAdapter.this.applicationEventPublisher != null) {
				KclMessageDrivenChannelAdapter.this.applicationEventPublisher
						.publishEvent(new KinesisShardEndedEvent(KclMessageDrivenChannelAdapter.this, this.shardId));
			}
		}

		@Override
		public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
			logger.info("Scheduler is shutting down; checkpointing...");
			try {
				shutdownRequestedInput.checkpointer().checkpoint();
			}
			catch (ShutdownException | InvalidStateException ex) {
				logger.error(ex, "Exception while checkpointing at requested shutdown. Giving up");
			}
		}

		@Override
		public void processRecords(ProcessRecordsInput processRecordsInput) {
			List<KinesisClientRecord> records = processRecordsInput.records();
			RecordProcessorCheckpointer checkpointer = processRecordsInput.checkpointer();
			logger.debug(() -> "Processing " + records.size() + " records from " + this.shardId);

			try {
				if (ListenerMode.record.equals(KclMessageDrivenChannelAdapter.this.listenerMode)) {
					for (KinesisClientRecord record : records) {
						processSingleRecord(record, checkpointer);
						checkpointIfRecordMode(checkpointer, record);
						checkpointIfPeriodicMode(checkpointer, record);
					}
				}
				else if (ListenerMode.batch.equals(KclMessageDrivenChannelAdapter.this.listenerMode)) {
					processMultipleRecords(records, checkpointer);
					checkpointIfPeriodicMode(checkpointer, null);
				}
				checkpointIfBatchMode(checkpointer);
			}
			finally {
				attributesHolder.remove();
			}
		}

		private void processSingleRecord(KinesisClientRecord record, RecordProcessorCheckpointer checkpointer) {
			performSend(prepareMessageForRecord(record), record, checkpointer);
		}

		private void processMultipleRecords(List<KinesisClientRecord> records,
				RecordProcessorCheckpointer checkpointer) {

			AbstractIntegrationMessageBuilder<?> messageBuilder = getMessageBuilderFactory().withPayload(records);
			if (KclMessageDrivenChannelAdapter.this.embeddedHeadersMapper != null) {
				List<Message<Object>> payload = records.stream().map(this::prepareMessageForRecord)
						.map(AbstractIntegrationMessageBuilder::build).toList();

				messageBuilder = getMessageBuilderFactory().withPayload(payload);
			}
			else if (KclMessageDrivenChannelAdapter.this.converter != null) {
				final List<String> partitionKeys = new ArrayList<>();
				final List<String> sequenceNumbers = new ArrayList<>();

				List<Object> payload = records.stream().map(r -> {
					partitionKeys.add(r.partitionKey());
					sequenceNumbers.add(r.sequenceNumber());

					return KclMessageDrivenChannelAdapter.this.converter
							.convert(BinaryUtils.copyAllBytesFrom(r.data()));
				}).toList();

				messageBuilder = getMessageBuilderFactory().withPayload(payload)
						.setHeader(KinesisHeaders.RECEIVED_PARTITION_KEY, partitionKeys)
						.setHeader(KinesisHeaders.RECEIVED_SEQUENCE_NUMBER, sequenceNumbers);
			}

			performSend(messageBuilder, records, checkpointer);
		}

		private AbstractIntegrationMessageBuilder<Object> prepareMessageForRecord(KinesisClientRecord record) {
			Object payload = BinaryUtils.copyAllBytesFrom(record.data());
			Message<?> messageToUse = null;

			if (KclMessageDrivenChannelAdapter.this.embeddedHeadersMapper != null) {
				try {
					messageToUse = KclMessageDrivenChannelAdapter.this.embeddedHeadersMapper
							.toMessage((byte[]) payload);
					if (messageToUse == null) {
						throw new IllegalStateException("The 'embeddedHeadersMapper' returned null for payload: "
								+ Arrays.toString((byte[]) payload));
					}
					payload = messageToUse.getPayload();
				}
				catch (Exception ex) {
					logger.warn(ex, "Could not parse embedded headers. Remain payload untouched.");
				}
			}

			if (payload instanceof byte[] && KclMessageDrivenChannelAdapter.this.converter != null) {
				payload = KclMessageDrivenChannelAdapter.this.converter.convert((byte[]) payload);
			}

			AbstractIntegrationMessageBuilder<Object> messageBuilder = getMessageBuilderFactory().withPayload(payload)
					.setHeader(KinesisHeaders.RECEIVED_PARTITION_KEY, record.partitionKey())
					.setHeader(KinesisHeaders.RECEIVED_SEQUENCE_NUMBER, record.sequenceNumber());

			if (KclMessageDrivenChannelAdapter.this.bindSourceRecord) {
				messageBuilder.setHeader(IntegrationMessageHeaderAccessor.SOURCE_DATA, record);
			}

			if (messageToUse != null) {
				messageBuilder.copyHeadersIfAbsent(messageToUse.getHeaders());
			}

			return messageBuilder;
		}

		private void performSend(AbstractIntegrationMessageBuilder<?> messageBuilder, Object rawRecord,
				RecordProcessorCheckpointer checkpointer) {

			messageBuilder.setHeader(KinesisHeaders.RECEIVED_STREAM, this.stream).setHeader(KinesisHeaders.SHARD,
					this.shardId);

			if (CheckpointMode.manual.equals(KclMessageDrivenChannelAdapter.this.checkpointMode)) {
				messageBuilder.setHeader(KinesisHeaders.CHECKPOINTER, checkpointer);
			}

			Message<?> messageToSend = messageBuilder.build();
			setAttributesIfNecessary(rawRecord, messageToSend);
			try {
				sendMessage(messageToSend);
			}
			catch (Exception ex) {
				logger.error(ex, () -> "Got an exception during sending a '" + messageToSend + "'" + "\nfor the '"
						+ rawRecord + "'.\n" + "Consider to use 'errorChannel' flow for the compensation logic.");
			}
		}

		/**
		 * If there's an error channel, we create a new attributes holder here. Then set the attributes for use by the
		 * {@link ErrorMessageStrategy}.
		 * @param record the Kinesis record to use.
		 * @param message the Spring Messaging message to use.
		 */
		private void setAttributesIfNecessary(Object record, Message<?> message) {
			if (getErrorChannel() != null) {
				AttributeAccessor attributes = ErrorMessageUtils.getAttributeAccessor(message, null);
				attributesHolder.set(attributes);
				attributes.setAttribute(KinesisHeaders.RAW_RECORD, record);
			}
		}

		/**
		 * Checkpoint with retries.
		 * @param checkpointer checkpointer
		 * @param record last processed record
		 */
		private void checkpoint(RecordProcessorCheckpointer checkpointer, @Nullable KinesisClientRecord record) {
			logger.info(() -> "Checkpointing shard " + this.shardId);
			try {
				if (record == null) {
					checkpointer.checkpoint();
				}
				else {
					checkpointer.checkpoint(record.sequenceNumber());
				}
			}
			catch (ShutdownException se) {
				// Ignore the checkpoint if the processor instance has been shutdown (fail over).
				logger.info(se, "Caught shutdown exception, skipping checkpoint.");
			}
			catch (ThrottlingException ex) {
				logger.info(ex, "Transient issue when checkpointing");
			}
			catch (InvalidStateException ex) {
				// This indicates an issue with the DynamoDB table (check for table, provisioned IOPS).
				logger.error(ex, "Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client.");
			}
		}

		private void checkpointIfBatchMode(RecordProcessorCheckpointer checkpointer) {
			if (CheckpointMode.batch.equals(KclMessageDrivenChannelAdapter.this.checkpointMode)) {
				checkpoint(checkpointer, null);
			}
		}

		private void checkpointIfRecordMode(RecordProcessorCheckpointer checkpointer, KinesisClientRecord record) {
			if (CheckpointMode.record.equals(KclMessageDrivenChannelAdapter.this.checkpointMode)) {
				checkpoint(checkpointer, record);
			}
		}

		private void checkpointIfPeriodicMode(RecordProcessorCheckpointer checkpointer,
				@Nullable KinesisClientRecord record) {

			if (CheckpointMode.periodic.equals(KclMessageDrivenChannelAdapter.this.checkpointMode)
					&& System.currentTimeMillis() > this.nextCheckpointTimeInMillis) {
				checkpoint(checkpointer, record);
				this.nextCheckpointTimeInMillis = System.currentTimeMillis()
						+ KclMessageDrivenChannelAdapter.this.checkpointsInterval;
			}
		}

	}

}
