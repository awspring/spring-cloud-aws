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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.awspring.cloud.kinesis.LocalstackContainerTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.StreamDescription;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
class KinesisMessageHandlerIntegrationTests implements LocalstackContainerTest {

	private static final String TEST_STREAM = "TestStream";

	private static KinesisAsyncClient AMAZON_KINESIS;

	private static String TEST_STREAM_SHARD_ID;

	@Autowired
	MessageChannel kinesisSendChannel;

	@BeforeAll
	static void setup() {
		AMAZON_KINESIS = LocalstackContainerTest.kinesisClient();
		WaiterResponse<DescribeStreamResponse> describeStreamResponse = AMAZON_KINESIS
				.createStream(request -> request.streamName(TEST_STREAM).shardCount(1))
				.thenCompose(result -> AMAZON_KINESIS.waiter()
						.waitUntilStreamExists(request -> request.streamName(TEST_STREAM)))
				.join();

		StreamDescription streamDescription = describeStreamResponse.matched().response().get().streamDescription();
		TEST_STREAM_SHARD_ID = streamDescription.shards().get(0).shardId();
	}

	@Test
	void kinesisMessageHandler() {
		String shardIterator = AMAZON_KINESIS
				.getShardIterator(builder -> builder.shardId(TEST_STREAM_SHARD_ID).streamName(TEST_STREAM)
						.shardIteratorType(ShardIteratorType.LATEST))
				.thenApply(GetShardIteratorResponse::shardIterator).join();

		Message<?> message = MessageBuilder.withPayload("message").setHeader(KinesisHeaders.PARTITION_KEY, "testKey")
				.build();

		this.kinesisSendChannel.send(message);

		AtomicReference<Record> recordReference = new AtomicReference<>();
		AtomicReference<String> nextShardIteratorReference = new AtomicReference<>(shardIterator);

		await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
			GetRecordsResponse getRecordsResponse = AMAZON_KINESIS
					.getRecords(builder -> builder.shardIterator(nextShardIteratorReference.get())).join();
			assertThat(getRecordsResponse).isNotNull();
			List<Record> records = getRecordsResponse.records();
			try {
				assertThat(records).isNotEmpty();
			}
			catch (AssertionError ex) {
				nextShardIteratorReference.set(getRecordsResponse.nextShardIterator());
				throw ex;
			}
			recordReference.set(records.get(0));
		});

		assertThat(recordReference.get().partitionKey()).isEqualTo("testKey");
		assertThat(recordReference.get().data().asUtf8String()).isEqualTo("message");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		@ServiceActivator(inputChannel = "kinesisSendChannel")
		public MessageHandler kinesisMessageHandler() {
			KinesisMessageHandler kinesisMessageHandler = new KinesisMessageHandler(AMAZON_KINESIS);
			kinesisMessageHandler.setStream(TEST_STREAM);
			kinesisMessageHandler.setMessageConverter(
					new ConvertingFromMessageConverter(source -> source.toString().getBytes(StandardCharsets.UTF_8)));
			return kinesisMessageHandler;
		}

	}

}
