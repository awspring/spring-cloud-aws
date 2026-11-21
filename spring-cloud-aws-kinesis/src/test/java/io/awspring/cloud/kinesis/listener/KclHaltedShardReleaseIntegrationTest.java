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
import io.awspring.cloud.kinesis.operations.KinesisTemplate;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proves that a shard halted by the default {@link LoggingErrorHandler} is released after the configured delay and
 * reprocessed from its last checkpoint, without restarting the container (Requirement 3).
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
class KclHaltedShardReleaseIntegrationTest implements LocalstackContainerTest {

	@Test
	void haltedShardIsReleasedAndRedeliveredWithoutRestart() throws Exception {
		KinesisAsyncClient kinesisClient = LocalstackContainerTest.kinesisClient();
		DynamoDbAsyncClient dynamoDbClient = LocalstackContainerTest.dynamoDbClient();
		CloudWatchAsyncClient cloudWatchClient = LocalstackContainerTest.cloudWatchClient();
		String streamName = "kcl-halted-release-stream";
		LocalstackContainerTest.createStream(kinesisClient, streamName, 1).join();

		// Fail the poison record on its first delivery, then let it through after the shard is released and
		// reacquired.
		AtomicInteger poisonDeliveries = new AtomicInteger();
		List<String> delivered = new CopyOnWriteArrayList<>();
		List<String> halted = new CopyOnWriteArrayList<>();

		KclContainerOptions options = KclContainerOptions.builder().gracefulShutdownTimeout(Duration.ofSeconds(10))
				.idleTimeBetweenReadsInMillis(500).maxRecords(100).checkpointMode(KclCheckpointMode.RECORD)
				.haltedShardReleaseDelay(Duration.ofSeconds(5))
				// Shorten the lease failover time so the dropped lease is reacquired quickly on LocalStack.
				.leaseManagementConfigCustomizer(lease -> lease.failoverTimeMillis(3000L)).build();
		KclMessageListenerContainer container = new KclMessageListenerContainer(kinesisClient, dynamoDbClient,
				cloudWatchClient, streamName, "kcl-halted-release-application", options);
		container.setApplicationEventPublisher(event -> {
			if (event instanceof KclShardHaltedEvent haltedEvent) {
				halted.add(haltedEvent.getShardId());
			}
		});
		container.setMessageListener(message -> {
			String payload = new String((byte[]) message.getPayload(), StandardCharsets.UTF_8);
			if ("poison".equals(payload) && poisonDeliveries.incrementAndGet() <= 1) {
				throw new RuntimeException("poison record failing attempt " + poisonDeliveries.get());
			}
			delivered.add(payload);
		});
		container.start();
		try {
			KinesisTemplate template = new KinesisTemplate(kinesisClient, new JsonMapper());
			template.send(streamName, "pk", "before");
			template.send(streamName, "pk", "poison");
			template.send(streamName, "pk", "after");

			// The shard halts on the poison record, is released after the delay, reacquired, and reprocessed from the
			// last checkpoint. The poison record eventually succeeds and the records keep their order.
			Awaitility.await().atMost(Duration.ofMinutes(4))
					.until(() -> delivered.containsAll(List.of("before", "poison", "after")));

			assertThat(delivered).containsSubsequence("before", "poison", "after");
			assertThat(poisonDeliveries.get()).isGreaterThanOrEqualTo(2);
			assertThat(halted).contains("shardId-000000000000");
			assertThat(container.isRunning()).isTrue();
		}
		finally {
			container.stop();
		}
	}

}
