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

import io.awspring.cloud.kinesis.LocalstackContainerTest;
import java.net.URI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.producer.KinesisProducer;
import software.amazon.kinesis.producer.KinesisProducerConfiguration;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
@DisabledOnOs(OS.WINDOWS)
class KplIntegrationTests implements LocalstackContainerTest {

	static final String TEST_STREAM = "TestStreamKpl";

	static KinesisAsyncClient AMAZON_KINESIS;

	static CloudWatchAsyncClient CLOUD_WATCH;

	@Autowired
	MessageChannel kinesisSendChannel;

	@Autowired
	PollableChannel kinesisReceiveChannel;

	@BeforeAll
	static void setup() {
		AMAZON_KINESIS = LocalstackContainerTest.kinesisClient();
		CLOUD_WATCH = LocalstackContainerTest.cloudWatchClient();

		AMAZON_KINESIS.createStream(request -> request.streamName(TEST_STREAM).shardCount(1)).thenCompose(
				result -> AMAZON_KINESIS.waiter().waitUntilStreamExists(request -> request.streamName(TEST_STREAM)))
				.join();
	}

	@Test
	void kinesisInboundOutbound() {
		this.kinesisSendChannel
				.send(MessageBuilder.withPayload("test").setHeader(KinesisHeaders.STREAM, TEST_STREAM).build());

		Message<?> receive = this.kinesisReceiveChannel.receive(30_000);
		assertThat(receive).extracting(Message::getPayload).isEqualTo("test");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class TestConfiguration {

		@Bean
		KinesisProducerConfiguration kinesisProducerConfiguration() {
			URI kinesisUri = LocalstackContainerTest.LOCAL_STACK_CONTAINER
					.getEndpointOverride(LocalStackContainer.Service.KINESIS);
			URI cloudWatchUri = LocalstackContainerTest.LOCAL_STACK_CONTAINER
					.getEndpointOverride(LocalStackContainer.Service.CLOUDWATCH);
			URI stsUri = LocalstackContainerTest.LOCAL_STACK_CONTAINER
					.getEndpointOverride(LocalStackContainer.Service.STS);

			return new KinesisProducerConfiguration()
					.setCredentialsProvider(LocalstackContainerTest.credentialsProvider())
					.setRegion(LocalstackContainerTest.LOCAL_STACK_CONTAINER.getRegion())
					.setKinesisEndpoint(kinesisUri.getHost()).setKinesisPort(kinesisUri.getPort())
					.setCloudwatchEndpoint(cloudWatchUri.getHost()).setCloudwatchPort(cloudWatchUri.getPort())
					.setStsEndpoint(stsUri.getHost()).setStsPort(stsUri.getPort()).setVerifyCertificate(false)
					.setCredentialsRefreshDelay(300000L);
		}

		@Bean
		@ServiceActivator(inputChannel = "kinesisSendChannel")
		MessageHandler kplMessageHandler(KinesisProducerConfiguration kinesisProducerConfiguration) {
			KplMessageHandler kinesisMessageHandler = new KplMessageHandler(
					new KinesisProducer(kinesisProducerConfiguration));
			kinesisMessageHandler.setPartitionKey("1");
			return kinesisMessageHandler;
		}

		@Bean
		KinesisMessageDrivenChannelAdapter kinesisMessageDrivenChannelAdapter(PollableChannel kinesisReceiveChannel) {
			KinesisMessageDrivenChannelAdapter adapter = new KinesisMessageDrivenChannelAdapter(AMAZON_KINESIS,
					TEST_STREAM);
			adapter.setStreamInitialSequence(KinesisShardOffset.trimHorizon());
			adapter.setOutputChannel(kinesisReceiveChannel);
			adapter.setBindSourceRecord(true);
			adapter.setDescribeStreamBackoff(10);
			adapter.setConsumerBackoff(10);
			adapter.setIdleBetweenPolls(1);
			return adapter;
		}

		@Bean
		public PollableChannel kinesisReceiveChannel() {
			return new QueueChannel();
		}

	}

}
