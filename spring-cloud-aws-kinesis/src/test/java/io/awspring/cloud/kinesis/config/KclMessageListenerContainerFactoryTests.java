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
package io.awspring.cloud.kinesis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.kinesis.listener.KclMessageListenerContainer;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointer;
import io.awspring.cloud.kinesis.operations.KinesisOperations;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders;
import io.awspring.cloud.kinesis.support.resolver.BatchMessagesArgumentResolver;
import io.awspring.cloud.kinesis.support.resolver.CheckpointerArgumentResolver;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.InitialPositionInStream;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
class KclMessageListenerContainerFactoryTests {

	private final KclMessageListenerContainerFactory factory = new KclMessageListenerContainerFactory(
			mock(KinesisAsyncClient.class), mock(DynamoDbAsyncClient.class), mock(CloudWatchAsyncClient.class));

	@Test
	@DisplayName("single-record listener resolves payload and headers")
	void wiresPayloadAndHeaders() throws Exception {
		RecordingListener bean = new RecordingListener();
		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory
				.createContainer(endpoint(bean, "handle", String.class, String.class));

		container.getMessageListener().onMessage(MessageBuilder.withPayload("hello".getBytes(StandardCharsets.UTF_8))
				.setHeader(KinesisMessageHeaders.PARTITION_KEY, "pk-1").build());

		assertThat(bean.payloads).containsExactly("hello");
		assertThat(bean.partitionKeys).containsExactly("pk-1");
	}

	@Test
	@DisplayName("KclCheckpointer is injected from headers for MANUAL checkpointing")
	void injectsCheckpointer() throws Exception {
		RecordingListener bean = new RecordingListener();
		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory
				.createContainer(endpoint(bean, "handleWithCheckpointer", String.class, KclCheckpointer.class));
		KclCheckpointer checkpointer = mock(KclCheckpointer.class);

		container.getMessageListener().onMessage(MessageBuilder.withPayload("hi".getBytes(StandardCharsets.UTF_8))
				.setHeader(KinesisMessageHeaders.CHECKPOINTER, checkpointer).build());

		assertThat(bean.payloads).containsExactly("hi");
		verify(checkpointer).checkpoint();
	}

	@Test
	@DisplayName("a Collection parameter is detected as a batch listener")
	void detectsBatchListener() throws Exception {
		RecordingListener bean = new RecordingListener();
		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory
				.createContainer(endpoint(bean, "handleBatch", List.class));

		assertThat(container.getBatchMessageListener()).isNotNull();
		container.getBatchMessageListener()
				.onMessage(List.of(MessageBuilder.withPayload("a".getBytes(StandardCharsets.UTF_8)).build(),
						MessageBuilder.withPayload("b".getBytes(StandardCharsets.UTF_8)).build()));

		assertThat(bean.batchSizes).containsExactly(2);
	}

	@Test
	@DisplayName("a @Header-annotated collection parameter does not make it a batch listener")
	void headerCollectionParameterIsNotBatch() throws Exception {
		RecordingListener bean = new RecordingListener();
		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory
				.createContainer(endpoint(bean, "handleWithHeaderList", String.class, List.class));

		assertThat(container.getBatchMessageListener()).isNull();
		assertThat(container.getMessageListener()).isNotNull();
	}

	@Test
	@DisplayName("checkpoint mode configured on the factory options is kept when the endpoint does not specify one")
	void checkpointModeFromOptionsPreservedWhenEndpointUnset() throws Exception {
		this.factory.configure(options -> options.checkpointMode(KclCheckpointMode.RECORD));
		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory
				.createContainer(endpoint(new RecordingListener(), "handle", String.class, String.class));

		assertThat(container.getCheckpointMode()).isEqualTo(KclCheckpointMode.RECORD);
	}

	@Test
	@DisplayName("checkpoint mode from the endpoint overrides the factory options")
	void endpointCheckpointModeOverridesOptions() throws Exception {
		this.factory.configure(options -> options.checkpointMode(KclCheckpointMode.RECORD));
		KclListenerEndpoint endpoint = endpointBuilder(new RecordingListener(), "handle", String.class, String.class)
				.checkpointMode(KclCheckpointMode.PERIODIC).build();
		endpoint.setHandlerMethodFactory(handlerMethodFactory());
		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory.createContainer(endpoint);

		assertThat(container.getCheckpointMode()).isEqualTo(KclCheckpointMode.PERIODIC);
	}

	@Test
	@DisplayName("initial position in stream from the endpoint overrides the factory options")
	void endpointInitialPositionOverridesOptions() throws Exception {
		this.factory.configure(options -> options.initialPositionInStream(InitialPositionInStream.TRIM_HORIZON));
		KclListenerEndpoint endpoint = endpointBuilder(new RecordingListener(), "handle", String.class, String.class)
				.initialPositionInStream(InitialPositionInStream.LATEST).build();
		endpoint.setHandlerMethodFactory(handlerMethodFactory());
		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory.createContainer(endpoint);

		assertThat(container.getContainerOptions().getInitialPositionInStream())
				.isEqualTo(InitialPositionInStream.LATEST);
	}

	@Test
	@DisplayName("initial position in stream configured on the factory options is kept when the endpoint does not specify one")
	void initialPositionFromOptionsPreservedWhenEndpointUnset() throws Exception {
		this.factory.configure(options -> options.initialPositionInStream(InitialPositionInStream.LATEST));
		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory
				.createContainer(endpoint(new RecordingListener(), "handle", String.class, String.class));

		assertThat(container.getContainerOptions().getInitialPositionInStream())
				.isEqualTo(InitialPositionInStream.LATEST);
	}

	@Test
	@DisplayName("per-stream identities from the endpoint are applied to the container options")
	void endpointPerStreamIdentitiesAreApplied() throws Exception {
		KclListenerEndpoint endpoint = endpointBuilder(new RecordingListener(), "handle", String.class, String.class)
				.consumerName("arn:aws:kinesis:eu-west-1:123456789012:stream/orders").leaseTableName("orders-leases")
				.metricsNamespace("orders-metrics").build();
		endpoint.setHandlerMethodFactory(handlerMethodFactory());
		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory.createContainer(endpoint);

		assertThat(container.getContainerOptions().getConsumerArn())
				.isEqualTo("arn:aws:kinesis:eu-west-1:123456789012:stream/orders");
		assertThat(container.getContainerOptions().getConsumerName()).isNull();
		assertThat(container.getContainerOptions().getLeaseTableName()).isEqualTo("orders-leases");
		assertThat(container.getContainerOptions().getMetricsNamespace()).isEqualTo("orders-metrics");
	}

	@Test
	@DisplayName("a multi-stream endpoint yields a single container consuming every stream")
	void multiStreamEndpointCreatesOneContainerForAllStreams() throws Exception {
		KclListenerEndpoint endpoint = endpointBuilder(new RecordingListener(), "handle", String.class, String.class)
				.streamNames(List.of("orders", "shipments")).build();
		endpoint.setHandlerMethodFactory(handlerMethodFactory());

		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory.createContainer(endpoint);

		assertThat(container.getStreamNames()).containsExactly("orders", "shipments");
	}

	@Test
	@DisplayName("batch listener converts each record payload to the declared element type")
	void batchListenerConvertsPayloadElements() throws Exception {
		RecordingListener bean = new RecordingListener();
		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory
				.createContainer(endpoint(bean, "handleOrders", List.class));

		container.getBatchMessageListener().onMessage(
				List.of(MessageBuilder.withPayload("{\"id\":\"a\"}".getBytes(StandardCharsets.UTF_8)).build(),
						MessageBuilder.withPayload("{\"id\":\"b\"}".getBytes(StandardCharsets.UTF_8)).build()));

		assertThat(bean.orderIds).containsExactly("a", "b");
	}

	@Test
	@DisplayName("a batch listener receives the KclCheckpointer shared by the batch records")
	void batchListenerReceivesSharedCheckpointer() throws Exception {
		RecordingListener bean = new RecordingListener();
		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory
				.createContainer(endpoint(bean, "handleOrdersWithCheckpointer", List.class, KclCheckpointer.class));
		KclCheckpointer checkpointer = mock(KclCheckpointer.class);

		container.getBatchMessageListener()
				.onMessage(List.of(
						MessageBuilder.withPayload("{\"id\":\"a\"}".getBytes(StandardCharsets.UTF_8))
								.setHeader(KinesisMessageHeaders.CHECKPOINTER, checkpointer).build(),
						MessageBuilder.withPayload("{\"id\":\"b\"}".getBytes(StandardCharsets.UTF_8))
								.setHeader(KinesisMessageHeaders.CHECKPOINTER, checkpointer).build()));

		assertThat(bean.orderIds).containsExactly("a", "b");
		verify(checkpointer).checkpoint();
	}

	@Test
	@DisplayName("a return value is forwarded to the @SendTo stream via KinesisOperations")
	void sendToForwardsReturnValue() throws Exception {
		KinesisOperations operations = mock(KinesisOperations.class);
		this.factory.setKinesisOperations(operations);
		KclListenerEndpoint endpoint = endpointBuilder(new RecordingListener(), "transform", String.class)
				.replyStream("out").build();
		endpoint.setHandlerMethodFactory(handlerMethodFactory());
		KclMessageListenerContainer container = (KclMessageListenerContainer) this.factory.createContainer(endpoint);

		container.getMessageListener()
				.onMessage(MessageBuilder.withPayload("hi".getBytes(StandardCharsets.UTF_8)).build());

		verify(operations).send("out", "HI");
	}

	private KclListenerEndpoint.Builder endpointBuilder(Object bean, String methodName, Class<?>... parameterTypes)
			throws Exception {
		Method method = bean.getClass().getDeclaredMethod(methodName, parameterTypes);
		return KclListenerEndpoint.builder().id("id-" + methodName).streamName("orders").applicationName("app")
				.bean(bean).method(method);
	}

	private KclListenerEndpoint endpoint(Object bean, String methodName, Class<?>... parameterTypes) throws Exception {
		KclListenerEndpoint endpoint = endpointBuilder(bean, methodName, parameterTypes).build();
		endpoint.setHandlerMethodFactory(handlerMethodFactory());
		return endpoint;
	}

	private static DefaultMessageHandlerMethodFactory handlerMethodFactory() {
		DefaultMessageHandlerMethodFactory handlerMethodFactory = new DefaultMessageHandlerMethodFactory();
		JacksonJsonMessageConverter jackson = new JacksonJsonMessageConverter();
		jackson.setStrictContentTypeMatch(false);
		MessageConverter converter = new CompositeMessageConverter(List.<MessageConverter> of(
				new ByteArrayMessageConverter(), new StringMessageConverter(), jackson, new SimpleMessageConverter()));
		handlerMethodFactory.setCustomArgumentResolvers(
				List.of(new CheckpointerArgumentResolver(), new BatchMessagesArgumentResolver(converter)));
		handlerMethodFactory.setMessageConverter(converter);
		handlerMethodFactory.afterPropertiesSet();
		return handlerMethodFactory;
	}

	static class RecordingListener {

		private final List<String> payloads = new ArrayList<>();

		private final List<String> partitionKeys = new ArrayList<>();

		private final List<Integer> batchSizes = new ArrayList<>();

		private final List<String> orderIds = new ArrayList<>();

		void handle(String payload, @Header(KinesisMessageHeaders.PARTITION_KEY) String partitionKey) {
			this.payloads.add(payload);
			this.partitionKeys.add(partitionKey);
		}

		void handleWithCheckpointer(String payload, KclCheckpointer checkpointer) {
			this.payloads.add(payload);
			checkpointer.checkpoint();
		}

		void handleBatch(List<Message<?>> messages) {
			this.batchSizes.add(messages.size());
		}

		void handleWithHeaderList(String payload, @Header("ids") List<String> ids) {
			this.payloads.add(payload);
		}

		void handleOrders(List<Order> orders) {
			orders.forEach(order -> this.orderIds.add(order.getId()));
		}

		void handleOrdersWithCheckpointer(List<Order> orders, KclCheckpointer checkpointer) {
			orders.forEach(order -> this.orderIds.add(order.getId()));
			checkpointer.checkpoint();
		}

		String transform(String payload) {
			return payload.toUpperCase(Locale.ROOT);
		}

	}

	static class Order {

		private String id;

		public String getId() {
			return this.id;
		}

		public void setId(String id) {
			this.id = id;
		}

	}

}
