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
import java.util.Date;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.metrics.MetricsLevel;
import software.amazon.kinesis.producer.KinesisProducer;
import software.amazon.kinesis.producer.KinesisProducerConfiguration;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
@Disabled("KCL fails with acquiring DB lock when KPL is there as well")
class KplKclIntegrationTests implements LocalstackContainerTest {

	private static final String TEST_STREAM = "TestStreamKplKcl";

	private static KinesisAsyncClient AMAZON_KINESIS;

	private static DynamoDbAsyncClient DYNAMO_DB;

	private static CloudWatchAsyncClient CLOUD_WATCH;

	@Autowired
	private MessageChannel kinesisSendChannel;

	@Autowired
	private PollableChannel kinesisReceiveChannel;

	@Autowired
	private PollableChannel errorChannel;

	@BeforeAll
	static void setup() {
		AMAZON_KINESIS = LocalstackContainerTest.kinesisClient();
		DYNAMO_DB = LocalstackContainerTest.dynamoDbClient();
		CLOUD_WATCH = LocalstackContainerTest.cloudWatchClient();

		AMAZON_KINESIS.createStream(request -> request.streamName(TEST_STREAM).shardCount(1)).thenCompose(
				result -> AMAZON_KINESIS.waiter().waitUntilStreamExists(request -> request.streamName(TEST_STREAM)))
				.join();
	}

	@Test
	void kinesisInboundOutbound() {
		this.kinesisSendChannel
				.send(MessageBuilder.withPayload("test").setHeader(KinesisHeaders.STREAM, TEST_STREAM).build());

		Date now = new Date();
		this.kinesisSendChannel.send(MessageBuilder.withPayload(now).setHeader(KinesisHeaders.STREAM, TEST_STREAM)
				.setHeader("test_header", "TEST_VALUE").build());

		Message<?> receive = this.kinesisReceiveChannel.receive(30_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(now);
		assertThat(receive.getHeaders()).containsEntry("test_header", "TEST_VALUE")
				.containsKey(IntegrationMessageHeaderAccessor.SOURCE_DATA);

		Message<?> errorMessage = this.errorChannel.receive(30_000);
		assertThat(errorMessage).isNotNull();
		assertThat(errorMessage.getHeaders().get(KinesisHeaders.RAW_RECORD)).isNotNull();
		assertThat(((Exception) errorMessage.getPayload()).getMessage())
				.contains("Channel 'kinesisReceiveChannel' expected one of the following data types "
						+ "[class java.util.Date], but received [class java.lang.String]");

		this.kinesisSendChannel
				.send(MessageBuilder.withPayload(new Date()).setHeader(KinesisHeaders.STREAM, TEST_STREAM).build());

		receive = this.kinesisReceiveChannel.receive(30_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders()).containsEntry(KinesisHeaders.RECEIVED_SEQUENCE_NUMBER, String.class);

		receive = this.kinesisReceiveChannel.receive(10);
		assertThat(receive).isNull();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class TestConfiguration {

		@Bean
		public KinesisProducerConfiguration kinesisProducerConfiguration() {
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
		@SuppressWarnings("removal")
		public MessageHandler kplMessageHandler(KinesisProducerConfiguration kinesisProducerConfiguration) {
			KplMessageHandler kinesisMessageHandler = new KplMessageHandler(
					new KinesisProducer(kinesisProducerConfiguration));
			kinesisMessageHandler.setPartitionKey("1");
			kinesisMessageHandler.setEmbeddedHeadersMapper(
					new org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper("test_header"));
			return kinesisMessageHandler;
		}

		@Bean
		@SuppressWarnings("removal")
		public KclMessageDrivenChannelAdapter kclMessageDrivenChannelAdapter(PollableChannel kinesisReceiveChannel,
				PollableChannel errorChannel) {

			KclMessageDrivenChannelAdapter adapter = new KclMessageDrivenChannelAdapter(AMAZON_KINESIS, CLOUD_WATCH,
					DYNAMO_DB, TEST_STREAM);
			adapter.setOutputChannel(kinesisReceiveChannel);
			adapter.setErrorChannel(errorChannel);
			adapter.setErrorMessageStrategy(new KinesisMessageHeaderErrorMessageStrategy());
			adapter.setEmbeddedHeadersMapper(
					new org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper("test_header"));
			adapter.setStreamInitialSequence(
					InitialPositionInStreamExtended.newInitialPosition(InitialPositionInStream.TRIM_HORIZON));
			adapter.setBindSourceRecord(true);
			adapter.setMetricsLevel(MetricsLevel.NONE);
			adapter.setLeaseManagementConfigCustomizer(leaseManagementConfig -> leaseManagementConfig
					.workerUtilizationAwareAssignmentConfig().disableWorkerMetrics(true));
			return adapter;
		}

		@Bean
		public PollableChannel kinesisReceiveChannel() {
			QueueChannel queueChannel = new QueueChannel();
			queueChannel.setDatatypes(Date.class);
			return queueChannel;
		}

		@Bean
		public PollableChannel errorChannel() {
			QueueChannel queueChannel = new QueueChannel();
			queueChannel.addInterceptor(new ChannelInterceptor() {

				@Override
				public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
					if (message instanceof ErrorMessage) {
						throw (RuntimeException) ((ErrorMessage) message).getPayload();
					}
				}

			});
			return queueChannel;
		}

	}

}
