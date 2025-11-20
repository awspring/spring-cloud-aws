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
package io.awspring.cloud.kinesis.stream.binder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.kinesis.integration.KclMessageDrivenChannelAdapter;
import io.awspring.cloud.kinesis.integration.KinesisShardOffset;
import io.awspring.cloud.kinesis.integration.ListenerMode;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisBinderConfigurationProperties;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisConsumerProperties;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisProducerProperties;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.BDDMockito;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.HeaderMode;
import org.springframework.cloud.stream.binder.PartitionCapableBinderTests;
import org.springframework.cloud.stream.binder.Spy;
import org.springframework.cloud.stream.binder.TestUtils;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeTypeUtils;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.metrics.MetricsLevel;

/**
 * The tests for Kinesis Binder.
 *
 * @author Artem Bilan
 * @author Jacob Severson
 * @author Arnaud Lecollaire
 * @author Minkyu Moon
 *
 * @since 4.0
 */
public class KinesisBinderTests extends
		PartitionCapableBinderTests<KinesisTestBinder, ExtendedConsumerProperties<KinesisConsumerProperties>, ExtendedProducerProperties<KinesisProducerProperties>>
		implements LocalstackContainerTest {

	private static final String CLASS_UNDER_TEST_NAME = KinesisBinderTests.class.getSimpleName();

	private static KinesisAsyncClient AMAZON_KINESIS;

	private static DynamoDbAsyncClient DYNAMO_DB;

	private static CloudWatchAsyncClient CLOUD_WATCH;

	public KinesisBinderTests() {
		this.timeoutMultiplier = 10D;
	}

	@BeforeAll
	public static void setup() {
		AMAZON_KINESIS = LocalstackContainerTest.kinesisClient();
		DYNAMO_DB = LocalstackContainerTest.dynamoDbClient();
		CLOUD_WATCH = LocalstackContainerTest.cloudWatchClient();
	}

	@Test
	@Override
	public void testClean(TestInfo testInfo) {
	}

	@Test
	@Override
	public void testPartitionedModuleSpEL(TestInfo testInfo) {

	}

	@Test
	public void testAutoCreateStreamForNonExistingStream() throws Exception {
		KinesisTestBinder binder = getBinder();
		DirectChannel output = createBindableChannel("output", new BindingProperties());
		ExtendedConsumerProperties<KinesisConsumerProperties> consumerProperties = createConsumerProperties();
		Date testDate = new Date();
		consumerProperties.getExtension()
				.setShardIteratorType(ShardIteratorType.AT_TIMESTAMP.name() + ":" + testDate.getTime());
		String testStreamName = "nonexisting" + System.currentTimeMillis();
		Binding<?> binding = binder.bindConsumer(testStreamName, "test", output, consumerProperties);
		binding.unbind();

		DescribeStreamResponse streamResult = AMAZON_KINESIS
				.describeStream(request -> request.streamName(testStreamName)).join();
		String createdStreamName = streamResult.streamDescription().streamName();
		int createdShards = streamResult.streamDescription().shards().size();
		StreamStatus createdStreamStatus = streamResult.streamDescription().streamStatus();

		assertThat(createdStreamName).isEqualTo(testStreamName);
		assertThat(createdShards)
				.isEqualTo(consumerProperties.getInstanceCount() * consumerProperties.getConcurrency());
		assertThat(createdStreamStatus).isEqualTo(StreamStatus.ACTIVE);

		KinesisShardOffset shardOffset = TestUtils.getPropertyValue(binding, "lifecycle.streamInitialSequence",
				KinesisShardOffset.class);
		assertThat(shardOffset.getIteratorType()).isEqualTo(ShardIteratorType.AT_TIMESTAMP);
		assertThat(Date.from(shardOffset.getTimestamp())).isEqualTo(testDate);
	}

	@Test
	@Override
	@SuppressWarnings("unchecked")
	public void testAnonymousGroup(TestInfo testInfo) throws Exception {
		KinesisTestBinder binder = getBinder();
		ExtendedProducerProperties<KinesisProducerProperties> producerProperties = createProducerProperties();
		producerProperties.setHeaderMode(HeaderMode.none);
		DirectChannel output = createBindableChannel("output", createProducerBindingProperties(producerProperties));

		Binding<MessageChannel> producerBinding = binder.bindProducer(
				String.format("defaultGroup%s0", getDestinationNameDelimiter()), output, producerProperties);

		ExtendedConsumerProperties<KinesisConsumerProperties> consumerProperties = createConsumerProperties();
		consumerProperties.setConcurrency(2);
		consumerProperties.setInstanceCount(3);
		consumerProperties.setInstanceIndex(0);

		QueueChannel input1 = new QueueChannel();
		Binding<MessageChannel> binding1 = binder.bindConsumer(
				String.format("defaultGroup%s0", getDestinationNameDelimiter()), null, input1, consumerProperties);

		consumerProperties.setInstanceIndex(1);

		QueueChannel input2 = new QueueChannel();
		Binding<MessageChannel> binding2 = binder.bindConsumer(
				String.format("defaultGroup%s0", getDestinationNameDelimiter()), null, input2, consumerProperties);

		String testPayload1 = "foo-" + UUID.randomUUID().toString();
		output.send(MessageBuilder.withPayload(testPayload1)
				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN).build());

		Message<byte[]> receivedMessage1 = (Message<byte[]>) receive(input1, 10);
		assertThat(receivedMessage1).isNotNull();
		assertThat(new String(receivedMessage1.getPayload())).isEqualTo(testPayload1);

		Message<byte[]> receivedMessage2 = (Message<byte[]>) receive(input2, 10);
		assertThat(receivedMessage2).isNotNull();
		assertThat(new String(receivedMessage2.getPayload())).isEqualTo(testPayload1);

		binding2.unbind();

		String testPayload2 = "foo-" + UUID.randomUUID();
		output.send(MessageBuilder.withPayload(testPayload2)
				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN).build());

		binding2 = binder.bindConsumer(String.format("defaultGroup%s0", getDestinationNameDelimiter()), null, input2,
				consumerProperties);
		String testPayload3 = "foo-" + UUID.randomUUID();
		output.send(MessageBuilder.withPayload(testPayload3)
				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN).build());

		receivedMessage1 = (Message<byte[]>) receive(input1, 10);
		assertThat(receivedMessage1).isNotNull();
		assertThat(new String(receivedMessage1.getPayload())).isEqualTo(testPayload2);
		receivedMessage1 = (Message<byte[]>) receive(input1, 10);
		assertThat(receivedMessage1).isNotNull();
		assertThat(new String(receivedMessage1.getPayload())).isNotNull();

		receivedMessage2 = (Message<byte[]>) receive(input2, 10);
		assertThat(receivedMessage2).isNotNull();
		assertThat(new String(receivedMessage2.getPayload())).isEqualTo(testPayload1);

		receivedMessage2 = (Message<byte[]>) receive(input2, 10);
		assertThat(receivedMessage2).isNotNull();
		assertThat(new String(receivedMessage2.getPayload())).isEqualTo(testPayload2);

		receivedMessage2 = (Message<byte[]>) receive(input2, 10);
		assertThat(receivedMessage2).isNotNull();
		assertThat(new String(receivedMessage2.getPayload())).isEqualTo(testPayload3);

		producerBinding.unbind();
		binding1.unbind();
		binding2.unbind();
	}

	@Test
	// @Disabled
	public void testProducerErrorChannel() throws Exception {
		KinesisTestBinder binder = getBinder();

		final RuntimeException putRecordException = new RuntimeException("putRecordRequestEx");
		KinesisAsyncClient amazonKinesisMock = mock(KinesisAsyncClient.class);
		BDDMockito.given(amazonKinesisMock.putRecord(any(PutRecordRequest.class)))
				.willAnswer((invocation) -> CompletableFuture.failedFuture(putRecordException));

		new DirectFieldAccessor(binder.getBinder()).setPropertyValue("amazonKinesis", amazonKinesisMock);

		ExtendedProducerProperties<KinesisProducerProperties> producerProps = createProducerProperties();
		producerProps.getExtension().setSync(false);
		producerProps.setErrorChannelEnabled(true);
		producerProps.populateBindingName("foobar");
		DirectChannel moduleOutputChannel = createBindableChannel("output",
				createProducerBindingProperties(producerProps));
		Binding<MessageChannel> producerBinding = binder.bindProducer("ec.0", moduleOutputChannel, producerProps);

		ApplicationContext applicationContext = TestUtils.getPropertyValue(binder.getBinder(), "applicationContext",
				ApplicationContext.class);
		String errorChannelName = testBinder.getBinder().getBinderIdentity() + "." + producerProps.getBindingName()
				+ ".errors";
		SubscribableChannel ec = applicationContext.getBean(errorChannelName, SubscribableChannel.class);
		final AtomicReference<Message<?>> errorMessage = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		ec.subscribe((message) -> {
			errorMessage.set(message);
			latch.countDown();
		});

		String messagePayload = "oops";
		moduleOutputChannel.send(new GenericMessage<>(messagePayload.getBytes()));

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(errorMessage.get()).isInstanceOf(ErrorMessage.class);
		assertThat(errorMessage.get().getPayload()).isInstanceOf(MessageHandlingException.class);
		MessageHandlingException exception = (MessageHandlingException) errorMessage.get().getPayload();
		assertThat(exception.getCause()).isSameAs(putRecordException);
		producerBinding.unbind();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testBatchListener() throws Exception {
		KinesisTestBinder binder = getBinder();
		ExtendedProducerProperties<KinesisProducerProperties> producerProperties = createProducerProperties();
		producerProperties.setHeaderMode(HeaderMode.none);
		DirectChannel output = createBindableChannel("output", createProducerBindingProperties(producerProperties));

		Binding<MessageChannel> outputBinding = binder.bindProducer("testBatchListener", output, producerProperties);

		for (int i = 0; i < 3; i++) {
			output.send(new GenericMessage<>(i));
		}

		ExtendedConsumerProperties<KinesisConsumerProperties> consumerProperties = createConsumerProperties();
		consumerProperties.getExtension().setListenerMode(ListenerMode.batch);
		consumerProperties.setUseNativeDecoding(true);
		consumerProperties.setHeaderMode(HeaderMode.none);

		QueueChannel input = new QueueChannel();
		Binding<MessageChannel> inputBinding = binder.bindConsumer("testBatchListener", null, input,
				consumerProperties);

		Message<List<?>> receivedMessage = (Message<List<?>>) receive(input, 10);
		assertThat(receivedMessage).isNotNull();
		assertThat(receivedMessage.getPayload().size()).isEqualTo(3);

		receivedMessage.getPayload().forEach((r) -> {
			assertThat(r).isInstanceOf(Record.class);
		});

		outputBinding.unbind();
		inputBinding.unbind();
	}

	@Test
	public void testKclWithTimestampAtInitialPositionInStream() throws Exception {
		KinesisBinderConfigurationProperties configurationProperties = new KinesisBinderConfigurationProperties();
		configurationProperties.setKplKclEnabled(true);
		KinesisTestBinder binder = getBinder(configurationProperties);
		DirectChannel output = createBindableChannel("output", new BindingProperties());
		ExtendedConsumerProperties<KinesisConsumerProperties> consumerProperties = createConsumerProperties();
		consumerProperties.setAutoStartup(false);
		Date testDate = new Date();
		consumerProperties.getExtension()
				.setShardIteratorType(ShardIteratorType.AT_TIMESTAMP.name() + ":" + testDate.getTime());
		Binding<?> binding = binder.bindConsumer("testKclStream", "test", output, consumerProperties);

		Lifecycle lifecycle = TestUtils.getPropertyValue(binding, "lifecycle", Lifecycle.class);
		assertThat(lifecycle).isInstanceOf(KclMessageDrivenChannelAdapter.class);

		InitialPositionInStreamExtended initialSequence = TestUtils.getPropertyValue(lifecycle, "streamInitialSequence",
				InitialPositionInStreamExtended.class);

		assertThat(initialSequence).isEqualTo(InitialPositionInStreamExtended.newInitialPositionAtTimestamp(testDate));

		binding.unbind();
	}

	@Test
	public void testKclWithTrimHorizonInitialPositionInStream() throws Exception {
		KinesisBinderConfigurationProperties configurationProperties = new KinesisBinderConfigurationProperties();
		configurationProperties.setKplKclEnabled(true);
		KinesisTestBinder binder = getBinder(configurationProperties);
		DirectChannel output = createBindableChannel("output", new BindingProperties());
		ExtendedConsumerProperties<KinesisConsumerProperties> consumerProperties = createConsumerProperties();
		consumerProperties.setAutoStartup(false);
		consumerProperties.getExtension().setFanOut(false);
		Binding<?> binding = binder.bindConsumer("testKclStream", null, output, consumerProperties);

		Lifecycle lifecycle = TestUtils.getPropertyValue(binding, "lifecycle", Lifecycle.class);
		assertThat(lifecycle).isInstanceOf(KclMessageDrivenChannelAdapter.class);

		InitialPositionInStreamExtended initialSequence = TestUtils.getPropertyValue(lifecycle, "streamInitialSequence",
				InitialPositionInStreamExtended.class);

		assertThat(initialSequence)
				.isEqualTo(InitialPositionInStreamExtended.newInitialPosition(InitialPositionInStream.TRIM_HORIZON));

		assertThat(TestUtils.getPropertyValue(lifecycle, "fanOut", Boolean.class)).isFalse();

		binding.unbind();
	}

	@Test
	public void testMetricsLevelOfKcl() throws Exception {
		KinesisBinderConfigurationProperties configurationProperties = new KinesisBinderConfigurationProperties();
		configurationProperties.setKplKclEnabled(true);
		KinesisTestBinder binder = getBinder(configurationProperties);
		DirectChannel output = createBindableChannel("output", new BindingProperties());
		ExtendedConsumerProperties<KinesisConsumerProperties> consumerProperties = createConsumerProperties();
		consumerProperties.setAutoStartup(false);
		consumerProperties.getExtension().setMetricsLevel(MetricsLevel.NONE);
		Binding<?> binding = binder.bindConsumer("testKclStream", null, output, consumerProperties);

		Lifecycle lifecycle = TestUtils.getPropertyValue(binding, "lifecycle", Lifecycle.class);
		assertThat(lifecycle).isInstanceOf(KclMessageDrivenChannelAdapter.class);

		assertThat(TestUtils.getPropertyValue(lifecycle, "metricsLevel", MetricsLevel.class))
				.isEqualTo(MetricsLevel.NONE);

		binding.unbind();
	}

	@Test
	public void testPartitionCountIncreasedIfAutoAddPartitionsSet() throws Exception {
		KinesisBinderConfigurationProperties configurationProperties = new KinesisBinderConfigurationProperties();

		String stream = "existing" + System.currentTimeMillis();

		AMAZON_KINESIS.createStream(request -> request.streamName(stream).shardCount(2)).join();

		List<Shard> shards = describeStream(stream);

		assertThat(shards.size()).isEqualTo(2);

		configurationProperties.setMinShardCount(4);
		configurationProperties.setAutoAddShards(true);
		KinesisTestBinder binder = getBinder(configurationProperties);

		ExtendedConsumerProperties<KinesisConsumerProperties> consumerProperties = createConsumerProperties();
		DirectChannel output = createBindableChannel("output", new BindingProperties());
		Binding<?> binding = binder.bindConsumer(stream, "test", output, consumerProperties);
		binding.unbind();

		shards = describeStream(stream);

		assertThat(shards.size()).isEqualTo(6);
	}

	private List<Shard> describeStream(String stream) {
		return AMAZON_KINESIS.describeStream(request -> request.streamName(stream))
				.thenCompose(
						reply -> AMAZON_KINESIS.waiter().waitUntilStreamExists(request -> request.streamName(stream)))
				.join().matched().response().get().streamDescription().shards();
	}

	@Override
	protected boolean usesExplicitRouting() {
		return false;
	}

	@Override
	protected String getClassUnderTestName() {
		return CLASS_UNDER_TEST_NAME;
	}

	@Override
	protected KinesisTestBinder getBinder() {
		return getBinder(new KinesisBinderConfigurationProperties());
	}

	private KinesisTestBinder getBinder(KinesisBinderConfigurationProperties kinesisBinderConfigurationProperties) {
		if (this.testBinder == null) {
			this.testBinder = new KinesisTestBinder(AMAZON_KINESIS, DYNAMO_DB, CLOUD_WATCH,
					kinesisBinderConfigurationProperties);
			this.timeoutMultiplier = 20;
		}
		return this.testBinder;
	}

	@Override
	protected ExtendedConsumerProperties<KinesisConsumerProperties> createConsumerProperties() {
		ExtendedConsumerProperties<KinesisConsumerProperties> kinesisConsumerProperties = new ExtendedConsumerProperties<>(
				new KinesisConsumerProperties());
		// set the default values that would normally be propagated by Spring Cloud Stream
		kinesisConsumerProperties.setInstanceCount(1);
		kinesisConsumerProperties.setInstanceIndex(0);
		kinesisConsumerProperties.getExtension().setShardIteratorType(ShardIteratorType.TRIM_HORIZON.name());
		kinesisConsumerProperties.getExtension().setIdleBetweenPolls(10);
		return kinesisConsumerProperties;
	}

	private ExtendedProducerProperties<KinesisProducerProperties> createProducerProperties() {
		return createProducerProperties(null);
	}

	@Override
	protected ExtendedProducerProperties<KinesisProducerProperties> createProducerProperties(TestInfo testInto) {
		ExtendedProducerProperties<KinesisProducerProperties> producerProperties = new ExtendedProducerProperties<>(
				new KinesisProducerProperties());
		producerProperties.setPartitionKeyExpression(new LiteralExpression("1"));
		producerProperties.getExtension().setSync(true);
		return producerProperties;
	}

	@Override
	public Spy spyOn(String name) {
		throw new UnsupportedOperationException("'spyOn' is not used by Kinesis tests");
	}

}
