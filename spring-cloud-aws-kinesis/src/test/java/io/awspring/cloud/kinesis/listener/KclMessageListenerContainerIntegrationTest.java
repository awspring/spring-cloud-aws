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
import io.awspring.cloud.kinesis.operations.KinesisTemplate;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
class KclMessageListenerContainerIntegrationTest implements LocalstackContainerTest {

	@Test
	void receivesProducedRecords() throws Exception {
		KinesisAsyncClient kinesisClient = LocalstackContainerTest.kinesisClient();
		DynamoDbAsyncClient dynamoDbClient = LocalstackContainerTest.dynamoDbClient();
		CloudWatchAsyncClient cloudWatchClient = LocalstackContainerTest.cloudWatchClient();
		String streamName = "kcl-listener-stream";
		LocalstackContainerTest.createStream(kinesisClient, streamName, 1).join();

		List<String> received = new CopyOnWriteArrayList<>();
		KclContainerOptions options = KclContainerOptions.builder().gracefulShutdownTimeout(Duration.ofSeconds(10))
				.idleTimeBetweenReadsInMillis(500).maxRecords(100).build();
		KclMessageListenerContainer container = new KclMessageListenerContainer(kinesisClient, dynamoDbClient,
				cloudWatchClient, streamName, "kcl-listener-application", options);
		container.setMessageListener(
				message -> received.add(new String((byte[]) message.getPayload(), StandardCharsets.UTF_8)));
		container.start();
		try {
			KinesisTemplate template = new KinesisTemplate(kinesisClient, new JsonMapper());
			template.send(streamName, "partition-key", "hello-kinesis");
			Awaitility.await().atMost(Duration.ofMinutes(3)).until(() -> received.contains("hello-kinesis"));
			assertThat(received).contains("hello-kinesis");
		}
		finally {
			container.stop();
		}
	}

}
