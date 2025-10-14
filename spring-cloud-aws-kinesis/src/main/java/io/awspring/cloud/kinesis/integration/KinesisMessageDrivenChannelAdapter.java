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

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.locks.RenewableLockRegistry;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.ExpiredIteratorException;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.kinesis.model.InvalidArgumentException;
import software.amazon.awssdk.services.kinesis.model.LimitExceededException;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsResponse;
import software.amazon.awssdk.services.kinesis.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;

/**
 * The {@link MessageProducerSupport} implementation for receiving data from Amazon Kinesis stream(s).
 *
 * @author Artem Bilan
 * @author Krzysztof Witkowski
 * @author Hervé Fortin
 * @author Dirk Bonhomme
 * @author Greg Eales
 * @author Asiel Caballero
 * @author Jonathan Nagayoshi
 *
 * @since 4.0
 */
@ManagedResource
@IntegrationManagedResource
public class KinesisMessageDrivenChannelAdapter extends MessageProducerSupport
		implements DisposableBean, ApplicationEventPublisherAware {

	private static final ThreadLocal<AttributeAccessor> attributesHolder = new ThreadLocal<>();

	private final KinesisAsyncClient amazonKinesis;

	private final String[] streams;

	private final Set<KinesisShardOffset> shardOffsets = new HashSet<>();

	private final Map<KinesisShardOffset, ShardConsumer> shardConsumers = new ConcurrentHashMap<>();

	private final Set<String> inResharding = new ConcurrentSkipListSet<>();

	private final List<ConsumerInvoker> consumerInvokers = new ArrayList<>();

	private final ShardConsumerManager shardConsumerManager = new ShardConsumerManager();

	private final ExecutorService shardLocksExecutor = Executors
			.newSingleThreadExecutor(new CustomizableThreadFactory() {

				@Override
				protected String getDefaultThreadNamePrefix() {
					return (getComponentName() == null ? "" : getComponentName()) + "-kinesis-shard-locks-";
				}

			});

	private String consumerGroup = "SpringIntegration";

	private ConcurrentMetadataStore checkpointStore = new SimpleMetadataStore();

	private Executor dispatcherExecutor;

	private boolean dispatcherExecutorExplicitlySet;

	private Executor consumerExecutor;

	private boolean consumerExecutorExplicitlySet;

	private int maxConcurrency;

	private int concurrency;

	private KinesisShardOffset streamInitialSequence = KinesisShardOffset.latest();

	private Converter<byte[], Object> converter = new DeserializingConverter();

	private ListenerMode listenerMode = ListenerMode.record;

	private CheckpointMode checkpointMode = CheckpointMode.batch;

	private long checkpointsInterval = 5_000L;

	private int recordsLimit = 10000;

	private int idleBetweenPolls = 1000;

	private int consumerBackoff = 1000;

	private int startTimeout = 60 * 1000;

	private int describeStreamBackoff = 1000;

	private int describeStreamRetries = 50;

	private long lockRenewalTimeout = 10_000L;

	private boolean resetCheckpoints;

	private InboundMessageMapper<byte[]> embeddedHeadersMapper;

	private LockRegistry<?> lockRegistry;

	private boolean bindSourceRecord;

	private volatile boolean active;

	private volatile int consumerInvokerMaxCapacity;

	private volatile Future<?> shardConsumerManagerFuture;

	private ApplicationEventPublisher applicationEventPublisher;

	@Nullable
	private Function<List<Shard>, List<Shard>> shardListFilter;

	public KinesisMessageDrivenChannelAdapter(KinesisAsyncClient amazonKinesis, String... streams) {
		Assert.notNull(amazonKinesis, "'amazonKinesis' must not be null.");
		Assert.notEmpty(streams, "'streams' must not be null.");
		this.amazonKinesis = amazonKinesis;
		this.streams = Arrays.copyOf(streams, streams.length);
	}

	public KinesisMessageDrivenChannelAdapter(KinesisAsyncClient amazonKinesis, KinesisShardOffset... shardOffsets) {

		Assert.notNull(amazonKinesis, "'amazonKinesis' must not be null.");
		Assert.notEmpty(shardOffsets, "'shardOffsets' must not be null.");
		Assert.noNullElements(shardOffsets, "'shardOffsets' must not contain null elements.");
		for (KinesisShardOffset shardOffset : shardOffsets) {
			Assert.isTrue(StringUtils.hasText(shardOffset.getStream()) && StringUtils.hasText(shardOffset.getShard()),
					"The 'shardOffsets' must be provided with particular 'stream' and 'shard' values.");
			this.shardOffsets.add(new KinesisShardOffset(shardOffset));
		}
		this.amazonKinesis = amazonKinesis;
		this.streams = null;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void setConsumerGroup(String consumerGroup) {
		Assert.hasText(consumerGroup, "'consumerGroup' must not be empty");
		this.consumerGroup = consumerGroup;
	}

	public void setCheckpointStore(ConcurrentMetadataStore checkpointStore) {
		Assert.notNull(checkpointStore, "'checkpointStore' must not be null");
		this.checkpointStore = checkpointStore;
	}

	public void setConsumerExecutor(Executor executor) {
		Assert.notNull(executor, "'executor' must not be null");
		this.consumerExecutor = executor;
		this.consumerExecutorExplicitlySet = true;
	}

	public void setDispatcherExecutor(Executor dispatcherExecutor) {
		this.dispatcherExecutor = dispatcherExecutor;
		this.dispatcherExecutorExplicitlySet = true;
	}

	public void setStreamInitialSequence(KinesisShardOffset streamInitialSequence) {
		Assert.notNull(streamInitialSequence, "'streamInitialSequence' must not be null");
		this.streamInitialSequence = streamInitialSequence;
	}

	/**
	 * Specify a {@link Converter} to deserialize the {@code byte[]} from record's body. Can be {@code
	 * null} meaning no deserialization.
	 * @param converter the {@link Converter} to use or null
	 */
	public void setConverter(Converter<byte[], Object> converter) {
		this.converter = converter;
	}

	public void setListenerMode(ListenerMode listenerMode) {
		Assert.notNull(listenerMode, "'listenerMode' must not be null");
		this.listenerMode = listenerMode;
	}

	public void setCheckpointMode(CheckpointMode checkpointMode) {
		Assert.notNull(checkpointMode, "'checkpointMode' must not be null");
		this.checkpointMode = checkpointMode;
	}

	/**
	 * Sets the interval between 2 checkpoints. Only used when checkpointMode is periodic.
	 * @param checkpointsInterval interval between 2 checkpoints (in milliseconds)
	 */
	public void setCheckpointsInterval(long checkpointsInterval) {
		this.checkpointsInterval = checkpointsInterval;
	}

	/**
	 * The maximum record to poll per on get-records request. Not greater then {@code 10000}.
	 * @param recordsLimit the number of records to for per on get-records request.
	 * @see GetRecordsRequest.Builder#limit(Integer)
	 */
	public void setRecordsLimit(int recordsLimit) {
		Assert.isTrue(recordsLimit > 0, "'recordsLimit' must be more than 0");
		this.recordsLimit = Math.min(10000, recordsLimit);
	}

	public void setConsumerBackoff(int consumerBackoff) {
		this.consumerBackoff = Math.max(1000, consumerBackoff);
	}

	public void setDescribeStreamBackoff(int describeStreamBackoff) {
		this.describeStreamBackoff = Math.max(1000, describeStreamBackoff);
	}

	public void setDescribeStreamRetries(int describeStreamRetries) {
		Assert.isTrue(describeStreamRetries > 0, "'describeStreamRetries' must be more than 0");
		this.describeStreamRetries = describeStreamRetries;
	}

	public void setStartTimeout(int startTimeout) {
		Assert.isTrue(startTimeout > 0, "'startTimeout' must be more than 0");
		this.startTimeout = startTimeout;
	}

	/**
	 * Configure a timeout in milliseconds to wait for lock on shard renewal.
	 * @param lockRenewalTimeout the timeout to wait for lock renew in milliseconds.
	 */
	public void setLockRenewalTimeout(long lockRenewalTimeout) {
		Assert.isTrue(lockRenewalTimeout > 0, "'lockRenewalTimeout' must be more than 0");
		this.lockRenewalTimeout = lockRenewalTimeout;
	}

	/**
	 * The maximum number of concurrent {@link ConsumerInvoker}s running. The {@link ShardConsumer}s are evenly
	 * distributed between {@link ConsumerInvoker}s. Messages from within the same shard will be processed sequentially.
	 * In other words each shard is tied with the particular thread. By default, the concurrency is unlimited and shard
	 * is processed in the {@link #consumerExecutor} directly.
	 * @param concurrency the concurrency maximum number
	 */
	public void setConcurrency(int concurrency) {
		this.maxConcurrency = concurrency;
	}

	/**
	 * The sleep interval in milliseconds used in the main loop between shards polling cycles. Defaults to {@code 1000}l
	 * minimum {@code 250}.
	 * @param idleBetweenPolls the interval to sleep between shards polling cycles.
	 */
	public void setIdleBetweenPolls(int idleBetweenPolls) {
		this.idleBetweenPolls = Math.max(250, idleBetweenPolls);
	}

	/**
	 * Specify an {@link InboundMessageMapper} to extract message headers embedded into the record data.
	 * @param embeddedHeadersMapper the {@link InboundMessageMapper} to use.
	 */
	public void setEmbeddedHeadersMapper(InboundMessageMapper<byte[]> embeddedHeadersMapper) {
		this.embeddedHeadersMapper = embeddedHeadersMapper;
	}

	/**
	 * Specify a {@link LockRegistry} for exclusive access to provided streams. This is not used when shards-based
	 * configuration is provided.
	 * @param lockRegistry the {@link LockRegistry} to use.
	 */
	public void setLockRegistry(LockRegistry<?> lockRegistry) {
		this.lockRegistry = lockRegistry;
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
	 * Specify a {@link Function Function&lt;List&lt;Shard&gt;, List&lt;Shard&gt;&gt;} to filter the shards which will
	 * be read from.
	 * @param shardListFilter the filter {@link Function Function&lt;List&lt;Shard&gt;, List&lt;Shard&gt;&gt;}
	 */
	public void setShardListFilter(Function<List<Shard>, List<Shard>> shardListFilter) {
		this.shardListFilter = shardListFilter;
	}

	@Override
	protected void onInit() {
		super.onInit();

		final String componentName = getComponentName();
		if (this.consumerExecutor == null) {
			this.consumerExecutor = Executors.newCachedThreadPool(
					new CustomizableThreadFactory((componentName == null ? "" : componentName) + "-kinesis-consumer-"));
		}
		if (this.dispatcherExecutor == null) {
			this.dispatcherExecutor = Executors.newCachedThreadPool(new CustomizableThreadFactory(
					(componentName == null ? "" : componentName) + "-kinesis-dispatcher-"));
		}

		if (this.streams == null) {
			if (this.lockRegistry != null) {
				logger.warn("The LockRegistry is ignored when explicit shards configuration is used.");
			}
			this.lockRegistry = null;
		}
	}

	@Override
	public void destroy() {
		if (!this.consumerExecutorExplicitlySet) {
			((ExecutorService) this.consumerExecutor).shutdown();
		}
		if (!this.dispatcherExecutorExplicitlySet) {
			((ExecutorService) this.dispatcherExecutor).shutdown();
		}
	}

	@ManagedOperation
	public void stopConsumer(String stream, String shard) {
		ShardConsumer shardConsumer = this.shardConsumers.remove(KinesisShardOffset.latest(stream, shard));
		if (shardConsumer != null) {
			shardConsumer.stop();
		}
		else {
			this.logger.debug(
					() -> "There is no ShardConsumer for shard [" + shard + "] in stream [" + stream + "] to stop.");
		}
	}

	@ManagedOperation
	public void startConsumer(String stream, String shard) {
		KinesisShardOffset shardOffsetForSearch = KinesisShardOffset.latest(stream, shard);
		ShardConsumer shardConsumer = this.shardConsumers.get(shardOffsetForSearch);
		if (shardConsumer != null) {
			this.logger.debug(() -> "The [" + shardConsumer + "] has been started before.");
		}
		else {
			synchronized (this.shardOffsets) {
				for (KinesisShardOffset shardOffset : this.shardOffsets) {
					if (shardOffsetForSearch.equals(shardOffset)) {
						this.shardConsumerManager.addShardToConsume(shardOffset);
						break;
					}
				}
			}
		}
	}

	@ManagedOperation
	public void resetCheckpointForShardToLatest(String stream, String shard) {
		restartShardConsumerForOffset(KinesisShardOffset.latest(stream, shard));
	}

	@ManagedOperation
	public void resetCheckpointForShardToTrimHorizon(String stream, String shard) {
		restartShardConsumerForOffset(KinesisShardOffset.trimHorizon(stream, shard));
	}

	@ManagedOperation
	public void resetCheckpointForShardToSequenceNumber(String stream, String shard, String sequenceNumber) {
		restartShardConsumerForOffset(KinesisShardOffset.atSequenceNumber(stream, shard, sequenceNumber));
	}

	@ManagedOperation
	public void resetCheckpointForShardAtTimestamp(String stream, String shard, long timestamp) {
		restartShardConsumerForOffset(KinesisShardOffset.atTimestamp(stream, shard, Instant.ofEpochSecond(timestamp)));
	}

	private void restartShardConsumerForOffset(KinesisShardOffset shardOffset) {
		Assert.isTrue(this.shardOffsets.contains(shardOffset),
				"The [" + KinesisMessageDrivenChannelAdapter.this + "] doesn't operate shard [" + shardOffset.getShard()
						+ "] for stream [" + shardOffset.getStream() + "]");

		logger.debug(() -> "Resetting consumer for [" + shardOffset + "]...");
		shardOffset.reset();
		synchronized (this.shardOffsets) {
			this.shardOffsets.remove(shardOffset);
			this.shardOffsets.add(shardOffset);
		}
		if (this.active) {
			ShardConsumer oldShardConsumer = this.shardConsumers.remove(shardOffset);
			if (oldShardConsumer != null) {
				oldShardConsumer.close();
			}
			shardOffset.setReset(true);
			this.shardConsumerManager.addShardToConsume(shardOffset);
		}
	}

	@ManagedOperation
	public void resetCheckpoints() {
		this.resetCheckpoints = true;
		if (this.active) {
			stopConsumers();
			populateConsumers();
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

		if (this.streams != null) {
			populateShardsForStreams();
		}

		populateConsumers();

		this.active = true;

		this.concurrency = Math.min(this.maxConcurrency, this.shardOffsets.size());

		this.dispatcherExecutor.execute(new ConsumerDispatcher());

		this.shardConsumerManagerFuture = this.shardLocksExecutor.submit(this.shardConsumerManager);
	}

	private Collection<ShardConsumer> shardConsumerSubset(int i) {
		List<ShardConsumer> shardConsumers = new ArrayList<>(this.shardConsumers.values());
		if (this.concurrency == 1) {
			return shardConsumers;
		}
		else {
			int numConsumers = shardConsumers.size();
			if (numConsumers == this.concurrency) {
				return Collections.singleton(shardConsumers.get(i));
			}
			else {
				int perInvoker = numConsumers / this.concurrency;
				List<ShardConsumer> subset;
				if (i == this.concurrency - 1) {
					subset = shardConsumers.subList(i * perInvoker, numConsumers);
				}
				else {
					subset = shardConsumers.subList(i * perInvoker, (i + 1) * perInvoker);
				}
				return subset;
			}
		}
	}

	private List<Shard> readShardList(String stream) {
		return readShardList(stream, 0);
	}

	private List<Shard> readShardList(String stream, int retryCount) {
		List<Shard> shardList = new ArrayList<>();

		if (retryCount > this.describeStreamRetries) {
			throw new IllegalStateException("Kinesis could not read shards from stream with name [" + stream + "] ");
		}

		String nextToken = null;
		ListShardsRequest.Builder listShardsRequest = ListShardsRequest.builder().streamName(stream);

		try {
			do {
				ListShardsResponse listShardsResult = this.amazonKinesis
						.listShards(listShardsRequest.nextToken(nextToken).build()).join();
				shardList.addAll(listShardsResult.shards());
				nextToken = listShardsResult.nextToken();
			}
			while (nextToken != null);
		}
		catch (CompletionException ex) {
			if (ex.getCause() instanceof LimitExceededException) {
				logger.info(() -> "Got LimitExceededException when listing stream [" + stream + "]. "
						+ "Backing off for [" + this.describeStreamBackoff + "] millis.");

				try {
					Thread.sleep(this.describeStreamBackoff);
					readShardList(stream, retryCount + 1);
				}
				catch (InterruptedException interrupt) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(
							"The [describeStream] thread for the stream [" + stream + "] has been interrupted.",
							interrupt);
				}
			}
			else {
				throw ex;
			}
		}

		return shardList;
	}

	private void populateShardsForStreams() {
		this.shardOffsets.clear();
		final CountDownLatch shardsGatherLatch = new CountDownLatch(this.streams.length);
		for (final String stream : this.streams) {
			populateShardsForStream(stream, shardsGatherLatch);
		}
		try {
			if (!shardsGatherLatch.await(this.startTimeout, TimeUnit.MILLISECONDS)) {
				throw new IllegalStateException("The [ " + KinesisMessageDrivenChannelAdapter.this
						+ "] could not start during timeout: " + this.startTimeout);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(
					"The [ " + KinesisMessageDrivenChannelAdapter.this + "] has been interrupted from start.");
		}
	}

	private List<Shard> detectShardsToConsume(String stream) {
		return detectShardsToConsume(stream, 0);
	}

	private List<Shard> detectShardsToConsume(String stream, int retry) {
		List<Shard> shardsToConsume = new ArrayList<>();

		List<Shard> shards = readShardList(stream);

		try {
			for (Shard shard : shards) {
				String key = buildCheckpointKeyForShard(stream, shard.shardId());
				String endingSequenceNumber = shard.sequenceNumberRange().endingSequenceNumber();
				if (endingSequenceNumber != null) {
					String checkpoint = this.checkpointStore.get(key);

					boolean skipClosedAndExhaustedShard = checkpoint != null
							&& new BigInteger(endingSequenceNumber).compareTo(new BigInteger(checkpoint)) <= 0;

					logger.trace(() -> "The shard [" + shard + "] in stream [" + stream
							+ "] is closed CLOSED and exhausted with endingSequenceNumber [" + endingSequenceNumber
							+ "].\nThe last processed checkpoint is [" + checkpoint + "]."
							+ (skipClosedAndExhaustedShard ? "\nThe shard will be skipped." : ""));

					if (skipClosedAndExhaustedShard) {
						// Skip CLOSED shard which has been exhausted
						// according to the checkpoint
						continue;
					}
				}

				shardsToConsume.add(shard);
			}
		}
		catch (Exception ex) {
			String exceptionMessage = "Got an exception when processing shards in stream [" + stream + "]";
			logger.info(ex, () -> exceptionMessage + ".\n Retrying... ");
			if (retry > 5) {
				throw new IllegalStateException(exceptionMessage, ex);
			}
			// Retry
			detectShardsToConsume(stream, retry + 1);
			sleep(this.describeStreamBackoff, new IllegalStateException(exceptionMessage), false);
		}

		return this.shardListFilter != null ? this.shardListFilter.apply(shardsToConsume) : shardsToConsume;
	}

	private void sleep(long sleepAmount, RuntimeException error, boolean interruptThread) {
		try {
			Thread.sleep(sleepAmount);
		}
		catch (Exception ex) {
			if (interruptThread) {
				Thread.currentThread().interrupt();
			}

			if (this.active) {
				logger.error(ex, error.getMessage());
			}
			else {
				logger.debug(ex, () -> error.getMessage() + " while adapter was inactive");
			}

			throw error;
		}
	}

	private void populateShardsForStream(final String stream, final CountDownLatch shardsGatherLatch) {
		this.dispatcherExecutor.execute(() -> {
			try {
				List<Shard> shardsToConsume = detectShardsToConsume(stream);

				for (Shard shard : shardsToConsume) {
					KinesisShardOffset shardOffset = new KinesisShardOffset(this.streamInitialSequence);
					shardOffset.setShard(shard.shardId());
					shardOffset.setStream(stream);
					boolean addedOffset;
					synchronized (this.shardOffsets) {
						addedOffset = this.shardOffsets.add(shardOffset);
					}
					if (addedOffset && shardsGatherLatch == null && this.active) {
						this.shardConsumerManager.addShardToConsume(shardOffset);
					}
				}
			}
			catch (Exception ex) {
				logger.error(ex, () -> "Error population shards for stream: " + stream);
			}
			finally {
				if (shardsGatherLatch != null) {
					shardsGatherLatch.countDown();
				}
				this.inResharding.remove(stream);
			}
		});
	}

	private void populateConsumers() {
		synchronized (this.shardOffsets) {
			for (KinesisShardOffset shardOffset : this.shardOffsets) {
				this.shardConsumerManager.addShardToConsume(shardOffset);
			}
		}

		this.resetCheckpoints = false;
	}

	private void populateConsumer(KinesisShardOffset shardOffset) {
		shardOffset.setReset(this.resetCheckpoints);
		ShardConsumer shardConsumer = new ShardConsumer(shardOffset);

		if (this.active) {
			synchronized (this.consumerInvokers) {
				if (this.consumerInvokers.size() < this.maxConcurrency) {
					ConsumerInvoker consumerInvoker = new ConsumerInvoker(Collections.singleton(shardConsumer));
					this.consumerInvokers.add(consumerInvoker);
					this.consumerExecutor.execute(consumerInvoker);
				}
				else {
					boolean consumerAdded = false;
					for (ConsumerInvoker consumerInvoker : this.consumerInvokers) {
						if (consumerInvoker.consumers.size() < this.consumerInvokerMaxCapacity) {
							consumerInvoker.addConsumer(shardConsumer);
							consumerAdded = true;
							break;
						}
					}

					if (this.concurrency != 0 && !consumerAdded) {
						ConsumerInvoker firstConsumerInvoker = this.consumerInvokers.get(0);
						firstConsumerInvoker.addConsumer(shardConsumer);
						this.consumerInvokerMaxCapacity = firstConsumerInvoker.consumers.size();
					}
				}
			}
		}

		this.shardConsumers.put(shardOffset, shardConsumer);
	}

	private String buildCheckpointKeyForShard(String stream, String shardId) {
		return this.consumerGroup + ":" + stream + ":" + shardId;
	}

	@Override
	protected void doStop() {
		for (ConsumerInvoker consumerInvoker : this.consumerInvokers) {
			consumerInvoker.notifyBarrier();
		}
		super.doStop();
		stopConsumers();

		this.active = false;
		this.shardConsumerManagerFuture.cancel(true);
	}

	private void stopConsumers() {
		for (ShardConsumer shardConsumer : this.shardConsumers.values()) {
			shardConsumer.stop();
		}
		this.shardConsumers.clear();
	}

	/**
	 * If there's an error channel, we create a new attributes holder here. Then set the attributes for use by the
	 * {@link ErrorMessageStrategy}.
	 *
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
		return "KinesisMessageDrivenChannelAdapter{" + "shardOffsets=" + this.shardOffsets + ", consumerGroup='"
				+ this.consumerGroup + '\'' + '}';
	}

	private enum ConsumerState {
		NEW, EXPIRED, CONSUME, SLEEP, STOP
	}

	/**
	 * The {@link SchedulingAwareRunnable} implementation to iterate over {@link ShardConsumer} instances to move them
	 * to the next state according to the environment.
	 */
	private final class ConsumerDispatcher implements SchedulingAwareRunnable {

		private final Set<String> inReshardingProcess = new HashSet<>();

		@Override
		public void run() {
			// We can't rely on the 'isRunning()' because of race condition,
			// when 'running' is set after submitting this task
			while (KinesisMessageDrivenChannelAdapter.this.active) {
				for (String stream : KinesisMessageDrivenChannelAdapter.this.inResharding) {
					// Local store to avoid several tasks for the same 'stream'
					if (this.inReshardingProcess.add(stream)) {
						logger.debug(() -> "Resharding has happened for stream [" + stream + "]. Rebalancing...");
						populateShardsForStream(stream, null);
					}
				}

				Iterator<ShardConsumer> iterator = KinesisMessageDrivenChannelAdapter.this.shardConsumers.values()
						.iterator();
				while (iterator.hasNext()) {
					ShardConsumer shardConsumer = iterator.next();
					shardConsumer.execute();
					if (ConsumerState.STOP == shardConsumer.state) {
						iterator.remove();
						if (KinesisMessageDrivenChannelAdapter.this.streams != null
								&& shardConsumer.shardIterator == null) {

							// Shard is CLOSED and we are capable for resharding
							KinesisShardOffset shardOffset = shardConsumer.shardOffset;
							String stream = shardOffset.getStream();
							if (KinesisMessageDrivenChannelAdapter.this.inResharding.add(stream)) {
								this.inReshardingProcess.remove(stream);
								synchronized (KinesisMessageDrivenChannelAdapter.this.shardOffsets) {
									KinesisMessageDrivenChannelAdapter.this.shardOffsets.remove(shardOffset);
								}
							}
						}
					}
				}
				String errorMsg = "ConsumerDispatcher Thread [" + this + "] has been interrupted";
				sleep(KinesisMessageDrivenChannelAdapter.this.idleBetweenPolls, new IllegalStateException(errorMsg),
						true);
			}
		}

		@Override
		public boolean isLongLived() {
			return true;
		}

	}

	/**
	 * The state machine implementation for managing and consuming specific shard. This includes producing records
	 * downstream, checkpointing them, and handling errors.
	 */
	private final class ShardConsumer {

		private final KinesisShardOffset shardOffset;

		private final ShardCheckpointer checkpointer;

		private final String key;

		private long nextCheckpointTimeInMillis;

		private Runnable notifier;

		private volatile ConsumerState state = ConsumerState.NEW;

		private volatile Runnable task;

		private volatile String shardIterator;

		private volatile long sleepUntil;

		private final Runnable processTask = processTask();

		ShardConsumer(KinesisShardOffset shardOffset) {
			this.shardOffset = new KinesisShardOffset(shardOffset);
			this.key = buildCheckpointKeyForShard(shardOffset.getStream(), shardOffset.getShard());
			this.checkpointer = new ShardCheckpointer(KinesisMessageDrivenChannelAdapter.this.checkpointStore,
					this.key);
		}

		/**
		 * Set a {@link Runnable} representing a concurrency barrier between this state machine and
		 * {@link ConsumerInvoker}
		 * @param notifier the {@link Runnable} to use.
		 */
		void setNotifier(Runnable notifier) {
			this.notifier = notifier;
		}

		void stop() {
			this.state = ConsumerState.STOP;
			if (KinesisMessageDrivenChannelAdapter.this.lockRegistry != null) {
				LockCompletableFuture unlockFuture = new LockCompletableFuture(this.key);
				KinesisMessageDrivenChannelAdapter.this.shardConsumerManager.unlock(unlockFuture);
				try {
					unlockFuture.get(KinesisMessageDrivenChannelAdapter.this.lockRenewalTimeout, TimeUnit.MILLISECONDS);
				}
				catch (Exception ex) {
					if (ex instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					logger.info(ex, () -> "The lock for key '" + this.key + "' was not unlocked in time");
				}
			}
			if (this.notifier != null) {
				this.notifier.run();
			}
		}

		void close() {
			stop();
			this.checkpointer.close();
		}

		void execute() {
			if (this.task == null) {
				if (!renewLockIfAny()) {
					return;
				}

				switch (this.state) {
				case NEW:
				case EXPIRED:
					this.task = () -> {
						try {
							if (this.shardOffset.isReset()) {
								this.checkpointer.remove();
							}
							else {
								String checkpoint = this.checkpointer.getCheckpoint();
								if (checkpoint != null) {
									this.shardOffset.setSequenceNumber(checkpoint);
									this.shardOffset.setIteratorType(ShardIteratorType.AFTER_SEQUENCE_NUMBER);
								}
							}
							if (this.state == ConsumerState.NEW) {
								logger.info(() -> "The [" + this + "] has been started.");
							}
							GetShardIteratorRequest shardIteratorRequest = this.shardOffset.toShardIteratorRequest();
							try {
								this.shardIterator = KinesisMessageDrivenChannelAdapter.this.amazonKinesis
										.getShardIterator(shardIteratorRequest)
										.thenApply(GetShardIteratorResponse::shardIterator).join();
							}
							catch (CompletionException ex) {
								if (ex.getCause() instanceof InvalidArgumentException cause && cause.getMessage()
										.contains("has reached max possible value for the shard")) {

									logger.info(() -> "The [" + this.shardOffset + "] has been closed. Skipping...");
								}
								else {
									throw ex;
								}
							}
							if (this.shardIterator == null) {
								// The shard is closed - stop consumer
								this.state = ConsumerState.STOP;
							}
							if (ConsumerState.STOP != this.state) {
								this.state = ConsumerState.CONSUME;
							}
						}
						finally {
							this.task = null;
						}
					};
					break;

				case CONSUME:
					this.task = this.processTask;
					break;

				case SLEEP:
					if (System.currentTimeMillis() >= this.sleepUntil) {
						this.state = ConsumerState.CONSUME;
					}
					this.task = null;
					break;

				case STOP:
					if (this.shardIterator == null) {
						logger.info(() -> "Stopping the [" + this + "] on the checkpoint ["
								+ this.checkpointer.getCheckpoint()
								+ "] because the shard has been CLOSED and exhausted.");
					}
					else {
						logger.info(() -> "Stopping the [" + this + "].");
					}
					this.task = null;
					break;
				}

				if (this.task != null) {
					if (this.notifier != null) {
						this.notifier.run();
					}
					if (KinesisMessageDrivenChannelAdapter.this.concurrency == 0) {
						KinesisMessageDrivenChannelAdapter.this.consumerExecutor.execute(this.task);
					}
				}
			}
		}

		private boolean renewLockIfAny() {
			if (KinesisMessageDrivenChannelAdapter.this.lockRegistry != null && this.state == ConsumerState.CONSUME) {
				LockCompletableFuture renewLockFuture = new LockCompletableFuture(this.key);
				KinesisMessageDrivenChannelAdapter.this.shardConsumerManager.renewLock(renewLockFuture);
				boolean lockRenewed = false;
				try {
					lockRenewed = renewLockFuture.get(KinesisMessageDrivenChannelAdapter.this.lockRenewalTimeout,
							TimeUnit.MILLISECONDS);
				}
				catch (Exception ex) {
					if (ex instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					logger.info(ex, () -> "The lock for key '" + this.key + "' was not renewed in time");
				}

				if (!lockRenewed && this.state == ConsumerState.CONSUME) {
					this.state = ConsumerState.STOP;
					this.checkpointer.close();
					if (this.notifier != null) {
						this.notifier.run();
					}
					if (KinesisMessageDrivenChannelAdapter.this.active) {
						KinesisMessageDrivenChannelAdapter.this.shardConsumerManager
								.addShardToConsume(this.shardOffset);
					}
					return false;
				}
			}
			return true;
		}

		private Runnable processTask() {
			return () -> {
				GetRecordsRequest getRecordsRequest = GetRecordsRequest.builder().shardIterator(this.shardIterator)
						.limit(KinesisMessageDrivenChannelAdapter.this.recordsLimit).build();

				GetRecordsResponse result = null;

				try {
					result = getRecords(getRecordsRequest);
					if (result != null) {
						List<Record> records = result.records();

						if (!records.isEmpty()) {
							processRecords(records);
						}
						this.shardIterator = result.nextShardIterator();
					}
				}
				catch (Exception ex) {
					rewindIteratorOnError(ex, result);
				}
				finally {
					attributesHolder.remove();
					if (result != null) {
						if (this.shardIterator == null) {
							if (KinesisMessageDrivenChannelAdapter.this.lockRegistry != null) {
								KinesisMessageDrivenChannelAdapter.this.shardConsumerManager.shardOffsetsToConsumer
										.remove(this.key);
							}
							// Shard is closed: nothing to consume anymore.
							// Checkpoint endingSequenceNumber to ensure shard is marked exhausted.
							for (Shard shard : readShardList(this.shardOffset.getStream())) {
								if (shard.shardId().equals(this.shardOffset.getShard())) {
									String endingSequenceNumber = shard.sequenceNumberRange().endingSequenceNumber();
									if (endingSequenceNumber != null) {
										checkpointSwallowingProvisioningExceptions(endingSequenceNumber);
									}
									break;
								}
							}

							// Resharding is possible.
							if (KinesisMessageDrivenChannelAdapter.this.applicationEventPublisher != null) {
								KinesisMessageDrivenChannelAdapter.this.applicationEventPublisher.publishEvent(
										new KinesisShardEndedEvent(KinesisMessageDrivenChannelAdapter.this, this.key));
							}
							stop();
						}

						if (ConsumerState.STOP != this.state && result.records().isEmpty()) {
							logger.debug(() -> "No records for [" + this + "] on sequenceNumber ["
									+ this.checkpointer.getLastCheckpointValue() + "]. Suspend consuming for ["
									+ KinesisMessageDrivenChannelAdapter.this.consumerBackoff + "] milliseconds.");
							prepareSleepState();
						}
					}

					this.task = null;
				}
			};
		}

		private void rewindIteratorOnError(Exception ex, @Nullable GetRecordsResponse result) {
			String lastCheckpoint = this.checkpointer.getLastCheckpointValue();
			String highestSequence = this.checkpointer.getHighestSequence();

			if (highestSequence == null) {
				// Haven't reached record process - reuse the current shard iterator.
				logger.info(ex, "getRecords request has thrown exception. "
						+ "No checkpoints - re-request with the current shard iterator.");
			}
			else if (highestSequence.equals(lastCheckpoint) && result != null) {
				logger.info(ex, "Record processor has thrown exception. "
						+ "Ignore since the highest sequence in batch was check-pointed.");
				this.shardIterator = result.nextShardIterator();
			}
			else if (reRequestCurrentShardIterator(lastCheckpoint, result)) {
				// No checkpoints for the shard - reuse the current shard iterator.
				logger.info(ex, "Record processor has thrown exception. "
						+ "No checkpoints - re-request with the current shard iterator.");
			}
			else {
				KinesisShardOffset newOffset = new KinesisShardOffset(this.shardOffset);
				newOffset.setIteratorType(ShardIteratorType.AFTER_SEQUENCE_NUMBER);

				logger.info(ex, () -> "Record processor has thrown exception. "
						+ "Rewind shard iterator after sequence number: " + lastCheckpoint);

				newOffset.setSequenceNumber(lastCheckpoint);
				GetShardIteratorRequest shardIteratorRequest = newOffset.toShardIteratorRequest();
				this.shardIterator = KinesisMessageDrivenChannelAdapter.this.amazonKinesis
						.getShardIterator(shardIteratorRequest).join().shardIterator();
			}
		}

		private boolean reRequestCurrentShardIterator(@Nullable String lastCheckpoint,
				@Nullable GetRecordsResponse result) {

			if (lastCheckpoint == null || result == null) {
				return true;
			}
			List<Record> records = result.records();
			return !records.isEmpty() && new BigInteger(lastCheckpoint)
					.compareTo(new BigInteger(records.get(records.size() - 1).sequenceNumber())) < 0;
		}

		private void checkpointSwallowingProvisioningExceptions(String endingSequenceNumber) {
			try {
				this.checkpointer.checkpoint(endingSequenceNumber);
			}
			catch (ProvisionedThroughputExceededException ignored) {
				// This exception is ignored to guarantee that an exhausted shard is marked as CLOSED
				// even in the case it's not possible to checkpoint. Otherwise, the ShardConsumer is
				// left in an illegal state where the shard iterator is null without any possibility
				// of recovering from it.
				logger.debug(ignored, "Exception while checkpointing empty shards");
			}
		}

		private GetRecordsResponse getRecords(GetRecordsRequest getRecordsRequest) {
			try {
				return KinesisMessageDrivenChannelAdapter.this.amazonKinesis.getRecords(getRecordsRequest).join();
			}
			catch (CompletionException ex) {
				Throwable cause = ex.getCause();
				if (cause instanceof ExpiredIteratorException) {
					// Iterator expired, but this does not mean that shard no longer contains
					// records.
					// Let's acquire iterator again (using checkpointer for iterator start
					// sequence number).
					logger.info(() -> "Shard iterator for [" + ShardConsumer.this + "] expired.\n"
							+ "A new one will be started from the check pointed sequence number.");
					this.state = ConsumerState.EXPIRED;
				}
				else if (cause instanceof ProvisionedThroughputExceededException) {
					logger.warn(() -> "GetRecords request throttled for [" + ShardConsumer.this + "] with the reason: "
							+ cause.getMessage());
					// We are throttled, so let's sleep
					prepareSleepState();
				}
				else {
					throw ex;
				}
			}

			return null;
		}

		private void prepareSleepState() {
			ShardConsumer.this.sleepUntil = System.currentTimeMillis()
					+ KinesisMessageDrivenChannelAdapter.this.consumerBackoff;
			ShardConsumer.this.state = ConsumerState.SLEEP;
		}

		private void processRecords(List<Record> records) {
			logger.trace(() -> "Processing records: " + records + " for [" + ShardConsumer.this + "]");

			this.checkpointer.setHighestSequence(records.get(records.size() - 1).sequenceNumber());

			if (ListenerMode.record.equals(KinesisMessageDrivenChannelAdapter.this.listenerMode)) {
				for (Record record : records) {
					processSingleRecord(record);
					checkpointIfRecordMode(record);
					checkpointIfPeriodicMode(record);
				}
			}
			else if (ListenerMode.batch.equals(KinesisMessageDrivenChannelAdapter.this.listenerMode)) {
				processMultipleRecords(records);
				checkpointIfPeriodicMode(null);
			}
			checkpointIfBatchMode();
		}

		private void processSingleRecord(Record record) {
			performSend(prepareMessageForRecord(record), record);
		}

		private void processMultipleRecords(List<Record> records) {
			AbstractIntegrationMessageBuilder<?> messageBuilder;
			if (KinesisMessageDrivenChannelAdapter.this.embeddedHeadersMapper != null) {
				List<Message<Object>> payload = records.stream().map(this::prepareMessageForRecord)
						.map(AbstractIntegrationMessageBuilder::build).toList();

				messageBuilder = getMessageBuilderFactory().withPayload(payload);
			}
			else if (KinesisMessageDrivenChannelAdapter.this.converter != null) {
				final List<String> partitionKeys = new ArrayList<>();
				final List<String> sequenceNumbers = new ArrayList<>();

				List<Object> payload = records.stream().map(r -> {
					partitionKeys.add(r.partitionKey());
					sequenceNumbers.add(r.sequenceNumber());

					return KinesisMessageDrivenChannelAdapter.this.converter.convert(r.data().asByteArray());
				}).toList();

				messageBuilder = getMessageBuilderFactory().withPayload(payload)
						.setHeader(KinesisHeaders.RECEIVED_PARTITION_KEY, partitionKeys)
						.setHeader(KinesisHeaders.RECEIVED_SEQUENCE_NUMBER, sequenceNumbers);
			}
			else {
				messageBuilder = getMessageBuilderFactory().withPayload(records);
			}

			performSend(messageBuilder, records);
		}

		private AbstractIntegrationMessageBuilder<Object> prepareMessageForRecord(Record record) {
			Object payload = record.data().asByteArray();
			Message<?> messageToUse = null;

			if (KinesisMessageDrivenChannelAdapter.this.embeddedHeadersMapper != null) {
				try {
					messageToUse = KinesisMessageDrivenChannelAdapter.this.embeddedHeadersMapper
							.toMessage((byte[]) payload);

					payload = messageToUse.getPayload();
				}
				catch (Exception ex) {
					logger.warn(ex, "Could not parse embedded headers. Remain payload untouched.");
				}
			}

			if (payload instanceof byte[] && KinesisMessageDrivenChannelAdapter.this.converter != null) {
				payload = KinesisMessageDrivenChannelAdapter.this.converter.convert((byte[]) payload);
			}

			AbstractIntegrationMessageBuilder<Object> messageBuilder = getMessageBuilderFactory().withPayload(payload)
					.setHeader(KinesisHeaders.RECEIVED_PARTITION_KEY, record.partitionKey())
					.setHeader(KinesisHeaders.RECEIVED_SEQUENCE_NUMBER, record.sequenceNumber());

			if (KinesisMessageDrivenChannelAdapter.this.bindSourceRecord) {
				messageBuilder.setHeader(IntegrationMessageHeaderAccessor.SOURCE_DATA, record);
			}

			if (messageToUse != null) {
				messageBuilder.copyHeadersIfAbsent(messageToUse.getHeaders());
			}

			return messageBuilder;
		}

		private void performSend(AbstractIntegrationMessageBuilder<?> messageBuilder, Object rawRecord) {
			messageBuilder.setHeader(KinesisHeaders.RECEIVED_STREAM, this.shardOffset.getStream())
					.setHeader(KinesisHeaders.SHARD, this.shardOffset.getShard());

			if (CheckpointMode.manual.equals(KinesisMessageDrivenChannelAdapter.this.checkpointMode)) {
				messageBuilder.setHeader(KinesisHeaders.CHECKPOINTER, this.checkpointer);
			}

			Message<?> messageToSend = messageBuilder.build();
			setAttributesIfNecessary(rawRecord, messageToSend);
			sendMessage(messageToSend);
		}

		private void checkpointIfBatchMode() {
			if (CheckpointMode.batch.equals(KinesisMessageDrivenChannelAdapter.this.checkpointMode)) {
				this.checkpointer.checkpoint();
			}
		}

		private void checkpointIfRecordMode(Record record) {
			if (CheckpointMode.record.equals(KinesisMessageDrivenChannelAdapter.this.checkpointMode)) {
				this.checkpointer.checkpoint(record.sequenceNumber());
			}
		}

		private void checkpointIfPeriodicMode(@Nullable Record record) {
			if (CheckpointMode.periodic.equals(KinesisMessageDrivenChannelAdapter.this.checkpointMode)
					&& System.currentTimeMillis() > this.nextCheckpointTimeInMillis) {
				if (record == null) {
					this.checkpointer.checkpoint();
				}
				else {
					this.checkpointer.checkpoint(record.sequenceNumber());
				}
				this.nextCheckpointTimeInMillis = System.currentTimeMillis()
						+ KinesisMessageDrivenChannelAdapter.this.checkpointsInterval;
			}
		}

		@Override
		public String toString() {
			return "ShardConsumer{" + "shardOffset=" + this.shardOffset + ", state=" + this.state + '}';
		}

	}

	/**
	 * The {@link SchedulingAwareRunnable} implementation to handle {@link ShardConsumer} instances according to the
	 * {@link #maxConcurrency}. The number of {@link ConsumerInvoker} instances is never higher than
	 * {@link #maxConcurrency}. If there are more shards to consume, the excess {@link ShardConsumer} instances are
	 * spread evenly between running {@link ConsumerInvoker} instances.
	 */
	private final class ConsumerInvoker implements SchedulingAwareRunnable {

		private final Queue<ShardConsumer> consumers = new ConcurrentLinkedQueue<>();

		private final Semaphore processBarrier = new Semaphore(0);

		private final Runnable notifier = this::notifyBarrier;

		ConsumerInvoker(Collection<ShardConsumer> shardConsumers) {
			for (ShardConsumer shardConsumer : shardConsumers) {
				addConsumer(shardConsumer);
			}
		}

		void addConsumer(ShardConsumer shardConsumer) {
			shardConsumer.setNotifier(this.notifier);
			this.consumers.add(shardConsumer);
		}

		/**
		 * The main loop of this {@link ConsumerInvoker} is blocked waiting for the {@link Semaphore} until one if the
		 * managed {@link ShardConsumer} instances releases a permit, essentially notifying that the task to handle is
		 * ready in its state.
		 */
		void notifyBarrier() {
			this.processBarrier.release();
		}

		@Override
		public void run() {
			while (KinesisMessageDrivenChannelAdapter.this.active) {
				try {
					this.processBarrier.acquire();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();

					throw new IllegalStateException("ConsumerInvoker thread [" + this + "] has been interrupted", e);
				}

				for (Iterator<ShardConsumer> iterator = this.consumers.iterator(); iterator.hasNext();) {
					ShardConsumer shardConsumer = iterator.next();
					if (ConsumerState.STOP == shardConsumer.state) {
						iterator.remove();
					}
					else {
						if (shardConsumer.task != null) {
							try {
								shardConsumer.task.run();
							}
							catch (Exception ex) {
								logger.info(() -> "Got an exception " + ex + " during [" + shardConsumer
										+ "] task invocation" + ".\nProcess will be retried on the next iteration.");
							}
						}
					}
				}
				synchronized (KinesisMessageDrivenChannelAdapter.this.consumerInvokers) {
					// The attempt to survive if ShardConsumer has been added during
					// synchronization
					if (this.consumers.isEmpty()) {
						KinesisMessageDrivenChannelAdapter.this.consumerInvokers.remove(this);
						break;
					}
				}
			}
			synchronized (KinesisMessageDrivenChannelAdapter.this.consumerInvokers) {
				KinesisMessageDrivenChannelAdapter.this.consumerInvokers.remove(this);
			}
		}

		@Override
		public boolean isLongLived() {
			return true;
		}

	}

	/**
	 * The {@link SchedulingAwareRunnable} to handle states for distributed locks on shards this channel adapter
	 * consumes from.
	 */
	private final class ShardConsumerManager implements SchedulingAwareRunnable {

		private final Map<String, KinesisShardOffset> shardOffsetsToConsumer = new ConcurrentHashMap<>();

		private final Map<String, Lock> locks = new HashMap<>();

		private final Queue<LockCompletableFuture> forUnlocking = new ConcurrentLinkedQueue<>();

		private final Queue<LockCompletableFuture> forRenewing = new ConcurrentLinkedQueue<>();

		ShardConsumerManager() {
		}

		void addShardToConsume(KinesisShardOffset kinesisShardOffset) {
			String lockKey = buildCheckpointKeyForShard(kinesisShardOffset.getStream(), kinesisShardOffset.getShard());
			this.shardOffsetsToConsumer.put(lockKey, kinesisShardOffset);
		}

		void unlock(LockCompletableFuture unlockFuture) {
			this.forUnlocking.add(unlockFuture);
		}

		void renewLock(LockCompletableFuture renewLockFuture) {
			this.forRenewing.add(renewLockFuture);
		}

		@Override
		public void run() {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					this.shardOffsetsToConsumer.entrySet().removeIf(entry -> {
						boolean remove = true;
						if (KinesisMessageDrivenChannelAdapter.this.lockRegistry != null) {
							String key = entry.getKey();
							Lock lock = KinesisMessageDrivenChannelAdapter.this.lockRegistry.obtain(key);
							try {
								if (lock.tryLock()) {
									this.locks.put(key, lock);
								}
								else {
									remove = false;
								}

							}
							catch (Exception ex) {
								logger.error(ex, "Error during locking: " + lock);
							}
						}

						if (remove) {
							populateConsumer(entry.getValue());
						}

						return remove;
					});

					while (KinesisMessageDrivenChannelAdapter.this.lockRegistry != null) {
						LockCompletableFuture forUnlocking = this.forUnlocking.poll();
						if (forUnlocking != null) {
							Lock lock = this.locks.remove(forUnlocking.lockKey);
							if (lock != null) {
								try {
									lock.unlock();
								}
								catch (Exception ex) {
									logger.error(ex, "Error during unlocking: " + lock);
								}
							}
							forUnlocking.complete(true);
						}
						else {
							break;
						}
					}

					while (KinesisMessageDrivenChannelAdapter.this.lockRegistry != null) {
						LockCompletableFuture lockFuture = this.forRenewing.poll();
						if (lockFuture != null) {
							Lock lock = this.locks.get(lockFuture.lockKey);
							if (lock != null) {
								try {
									if (!renewLockInRegistry(lockFuture)) {
										lockFuture.complete(false);
										this.locks.remove(lockFuture.lockKey);
									}
								}
								catch (Exception ex) {
									lockFuture.complete(false);
									logger.error(ex, () -> "Error during locking: " + lock);
								}
							}
							else {
								lockFuture.complete(false);
							}
						}
						else {
							break;
						}
					}

					sleep(1000, new IllegalStateException(
							"ShardConsumerManager Thread [" + this + "] has been interrupted"), true);
				}
			}
			finally {
				for (Iterator<Lock> iterator = this.locks.values().iterator(); iterator.hasNext();) {
					Lock lock = iterator.next();
					try {
						lock.unlock();
					}
					catch (Exception ex) {
						if (KinesisMessageDrivenChannelAdapter.this.active) {
							logger.error(ex, () -> "Error during unlocking: " + lock);
						}
						else {
							logger.info(ex, () -> "Error during unlocking: " + lock + " while adapter was inactive");
						}
					}
					finally {
						iterator.remove();
					}
				}
			}
		}

		private boolean renewLockInRegistry(LockCompletableFuture renewLockFuture) {
			if (KinesisMessageDrivenChannelAdapter.this.lockRegistry instanceof RenewableLockRegistry<?> renewableLockRegistry) {

				try {
					renewableLockRegistry.renewLock(renewLockFuture.lockKey);
					return renewLockFuture.complete(true);
				}
				catch (IllegalStateException ex) {
					return false;
				}
			}
			else {
				Lock lock = this.locks.get(renewLockFuture.lockKey);
				if (lock.tryLock()) {
					try {
						return renewLockFuture.complete(true);
					}
					finally {
						lock.unlock();
					}
				}
			}
			return false;
		}

		@Override
		public boolean isLongLived() {
			return true;
		}

	}

	/**
	 * The convenient {@link CompletableFuture} extension based on the {@code lockKey}.
	 */
	private static final class LockCompletableFuture extends CompletableFuture<Boolean> {

		private final String lockKey;

		LockCompletableFuture(String lockKey) {
			this.lockKey = lockKey;
		}

	}

}
