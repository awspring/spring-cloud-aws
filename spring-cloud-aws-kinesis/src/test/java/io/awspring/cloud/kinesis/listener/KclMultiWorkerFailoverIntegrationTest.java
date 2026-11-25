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

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.kinesis.LocalstackContainerTest;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.kinesis.coordinator.Scheduler;

/**
 * Runs two KCL containers with distinct worker identifiers against one application (and so one lease table) and a
 * four-shard stream. Both workers must take shard leases and process records, and when one of them goes away, the
 * survivor must take over its shards and process the records written to them afterwards.
 * <p>
 * Every record is written with an explicit hash key (the start of a shard's hash range) so each shard is exercised
 * deterministically. Lease ownership is read from the lease table itself.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
class KclMultiWorkerFailoverIntegrationTest implements LocalstackContainerTest {

	private static final int SHARD_COUNT = 4;

	private static final Duration FAILOVER_TIME = Duration.ofSeconds(6);

	private static final Duration SETTLE_TIMEOUT = Duration.ofMinutes(4);

	private static final Duration TAKEOVER_TIMEOUT = Duration.ofMinutes(3);

	@Test
	void survivingWorkerTakesOverShardsAfterGracefulStop() throws Exception {
		runFailoverScenario("graceful", false);
	}

	@Test
	void survivingWorkerTakesOverShardsAfterAbruptSchedulerShutdown() throws Exception {
		runFailoverScenario("abrupt", true);
	}

	private void runFailoverScenario(String scenario, boolean abrupt) throws Exception {
		String suffix = scenario + "-" + UUID.randomUUID().toString().substring(0, 8);
		String streamName = "kcl-two-workers-stream-" + suffix;
		String applicationName = "kcl-two-workers-app-" + suffix;
		String workerAId = "worker-a-" + suffix;
		String workerBId = "worker-b-" + suffix;

		KinesisAsyncClient kinesisClient = LocalstackContainerTest.kinesisClient();
		DynamoDbAsyncClient dynamoDbClient = LocalstackContainerTest.dynamoDbClient();
		CloudWatchAsyncClient cloudWatchClient = LocalstackContainerTest.cloudWatchClient();
		LocalstackContainerTest.createStream(kinesisClient, streamName, SHARD_COUNT).join();

		// shardId -> starting hash key, so a record can be directed to a given shard.
		Map<String, String> hashKeyByShard = new TreeMap<>();
		for (Shard shard : kinesisClient.listShards(request -> request.streamName(streamName)).join().shards()) {
			hashKeyByShard.put(shard.shardId(), shard.hashKeyRange().startingHashKey());
		}
		assertThat(hashKeyByShard).hasSize(SHARD_COUNT);

		// payload -> workers that processed it (a record must be processed by exactly one
		// worker).
		Map<String, List<String>> processedBy = new ConcurrentHashMap<>();

		KclMessageListenerContainer workerA = createContainer(kinesisClient, dynamoDbClient, cloudWatchClient,
				streamName, applicationName, workerAId, processedBy);
		KclMessageListenerContainer workerB = createContainer(kinesisClient, dynamoDbClient, cloudWatchClient,
				streamName, applicationName, workerBId, processedBy);
		try {
			workerA.start();
			workerB.start();

			// 1. Both workers acquire shard leases, and together they cover every shard.
			Awaitility.await().atMost(SETTLE_TIMEOUT).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
				Map<String, String> owners = leaseOwners(dynamoDbClient, applicationName);
				assertThat(owners.keySet()).containsExactlyInAnyOrderElementsOf(hashKeyByShard.keySet());
				assertThat(owners.values()).contains(workerAId, workerBId);
			});

			// 2. A record into every shard is processed exactly once, and both workers
			// process something.
			List<String> initial = sendToEveryShard(kinesisClient, streamName, hashKeyByShard, "initial");
			awaitProcessed(processedBy, initial, SETTLE_TIMEOUT);
			assertThat(initial).allSatisfy(payload -> assertThat(processedBy.get(payload)).hasSize(1));
			Set<String> initialProcessors = initial.stream().map(payload -> processedBy.get(payload).get(0))
					.collect(Collectors.toSet());
			assertThat(initialProcessors).containsExactlyInAnyOrder(workerAId, workerBId);

			// 3. Remove worker B and remember which shards it held.
			Set<String> shardsOfB = leaseOwners(dynamoDbClient, applicationName).entrySet().stream()
					.filter(entry -> workerBId.equals(entry.getValue())).map(Map.Entry::getKey)
					.collect(Collectors.toSet());
			assertThat(shardsOfB).isNotEmpty().hasSizeLessThan(SHARD_COUNT);

			if (abrupt) {
				// Scheduler.shutdown() stops lease renewal and processing without the
				// cooperative lease hand-off
				// of a graceful shutdown, so worker A can only take the shards after the
				// leases expire.
				abruptlyShutDown(workerB);
			}
			else {
				workerB.stop();
				assertThat(workerB.isRunning()).isFalse();
			}

			// 4. The survivor owns every shard, including those of worker B.
			Awaitility.await().atMost(TAKEOVER_TIMEOUT).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
				Map<String, String> owners = leaseOwners(dynamoDbClient, applicationName);
				assertThat(owners.keySet()).containsExactlyInAnyOrderElementsOf(hashKeyByShard.keySet());
				assertThat(owners.values()).containsOnly(workerAId);
			});

			// 5. New records on every shard, in particular those worker B owned, are
			// processed by worker A only.
			List<String> afterFailover = sendToEveryShard(kinesisClient, streamName, hashKeyByShard, "after");
			awaitProcessed(processedBy, afterFailover, TAKEOVER_TIMEOUT);
			assertThat(afterFailover)
					.allSatisfy(payload -> assertThat(processedBy.get(payload)).as(payload).containsExactly(workerAId));
			assertThat(afterFailover).anySatisfy(payload -> assertThat(shardsOfB).contains(shardOf(payload)));
			assertThat(afterFailover.stream().map(KclMultiWorkerFailoverIntegrationTest::shardOf)
					.collect(Collectors.toSet())).containsExactlyInAnyOrderElementsOf(hashKeyByShard.keySet());
		}
		finally {
			stopQuietly(workerB);
			stopQuietly(workerA);
			deleteQuietly(dynamoDbClient, applicationName);
			kinesisClient.deleteStream(request -> request.streamName(streamName).enforceConsumerDeletion(true))
					.exceptionally(ex -> null).join();
		}
	}

	private static KclMessageListenerContainer createContainer(KinesisAsyncClient kinesisClient,
			DynamoDbAsyncClient dynamoDbClient, CloudWatchAsyncClient cloudWatchClient, String streamName,
			String applicationName, String workerId, Map<String, List<String>> processedBy) {

		KclContainerOptions options = KclContainerOptions.builder().workerIdentifier(workerId)
				.gracefulShutdownTimeout(Duration.ofSeconds(15)).idleTimeBetweenReadsInMillis(500).maxRecords(100)
				.checkpointMode(KclCheckpointMode.RECORD)
				.leaseManagementConfigCustomizer(
						lease -> lease.failoverTimeMillis(FAILOVER_TIME.toMillis()).shardSyncIntervalMillis(2000L))
				.build();
		KclMessageListenerContainer container = new KclMessageListenerContainer(kinesisClient, dynamoDbClient,
				cloudWatchClient, streamName, applicationName, options);
		container.setId(workerId);
		container.setMessageListener(message -> {
			String payload = new String((byte[]) message.getPayload(), StandardCharsets.UTF_8);
			processedBy.computeIfAbsent(payload, key -> new CopyOnWriteArrayList<>()).add(workerId);
		});
		return container;
	}

	private static List<String> sendToEveryShard(KinesisAsyncClient kinesisClient, String streamName,
			Map<String, String> hashKeyByShard, String phase) {

		List<String> payloads = new ArrayList<>();
		hashKeyByShard.forEach((shardId, hashKey) -> {
			String payload = phase + "|" + shardId + "|" + UUID.randomUUID();
			var response = kinesisClient
					.putRecord(request -> request.streamName(streamName).partitionKey("unused-" + shardId)
							.explicitHashKey(hashKey).data(SdkBytes.fromString(payload, StandardCharsets.UTF_8)))
					.join();
			// Guard that the explicit hash key really routed the record to the intended
			// shard.
			assertThat(response.shardId()).isEqualTo(shardId);
			payloads.add(payload);
		});
		return payloads;
	}

	private static void awaitProcessed(Map<String, List<String>> processedBy, List<String> payloads, Duration timeout) {

		Awaitility.await().atMost(timeout).pollInterval(Duration.ofMillis(500))
				.untilAsserted(() -> assertThat(processedBy.keySet()).containsAll(payloads));
		// Allow a duplicate delivery to surface before the caller asserts exactly-once
		// processing.
		Awaitility.await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10)).until(() -> true);
	}

	private static String shardOf(String payload) {
		return payload.split("\\|")[1];
	}

	/**
	 * Reads shardId -> lease owner from the lease table. Leases without an owner are left out.
	 */
	private static Map<String, String> leaseOwners(DynamoDbAsyncClient dynamoDbClient, String tableName) {
		Map<String, String> owners = new TreeMap<>();
		try {
			dynamoDbClient.scanPaginator(request -> request.tableName(tableName)).items().subscribe(item -> {
				AttributeValue key = item.get("leaseKey");
				AttributeValue owner = item.get("leaseOwner");
				if (key != null && owner != null && owner.s() != null) {
					owners.put(key.s(), owner.s());
				}
			}).join();
		}
		catch (RuntimeException ex) {
			// The table does not exist until the first worker creates it.
			return new TreeMap<>();
		}
		return owners;
	}

	private static void abruptlyShutDown(KclMessageListenerContainer container) throws Exception {
		Field field = KclMessageListenerContainer.class.getDeclaredField("scheduler");
		field.setAccessible(true);
		Scheduler scheduler = (Scheduler) field.get(container);
		assertThat(scheduler).isNotNull();
		scheduler.shutdown();
	}

	private static void stopQuietly(KclMessageListenerContainer container) {
		try {
			container.stop();
		}
		catch (RuntimeException ex) {
			// best effort cleanup
		}
	}

	private static void deleteQuietly(DynamoDbAsyncClient dynamoDbClient, String applicationName) {
		// KCL 3.x keeps the lease table plus coordinator state and worker metrics tables
		// next to it.
		for (String table : List.of(applicationName, applicationName + "-CoordinatorState",
				applicationName + "-WorkerMetricStats")) {
			try {
				dynamoDbClient.deleteTable(request -> request.tableName(table)).join();
			}
			catch (RuntimeException ex) {
				// absent or already deleted
			}
		}
	}

}
