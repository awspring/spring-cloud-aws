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
package io.awspring.cloud.kinesis.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.awspring.cloud.kinesis.support.converter.EmbeddedHeadersKinesisMessageConverter;
import io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter;
import io.awspring.cloud.kinesis.support.observation.KinesisTemplateObservation;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper;
import org.springframework.messaging.Message;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResultEntry;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
class KinesisTemplateObservationTests {

	private final KinesisAsyncClient client = mock(KinesisAsyncClient.class);

	private final TestObservationRegistry registry = TestObservationRegistry.create();

	@Test
	@DisplayName("send records an observation with messaging and Kinesis key values")
	@SuppressWarnings("unchecked")
	void sendIsObserved() {
		when(this.client.putRecord(any(Consumer.class))).thenReturn(CompletableFuture
				.completedFuture(PutRecordResponse.builder().sequenceNumber("seq-1").shardId("shard-1").build()));

		template().send("orders", "pk-1", "hello");

		TestObservationRegistryAssert.then(this.registry).hasNumberOfObservationsEqualTo(1)
				.hasSingleObservationThat().hasNameEqualTo("spring.aws.kinesis.template")
				.hasContextualNameEqualTo("orders send").hasBeenStopped()
				.hasLowCardinalityKeyValue("messaging.system", "aws_kinesis")
				.hasLowCardinalityKeyValue("messaging.operation", "publish")
				.hasLowCardinalityKeyValue("messaging.destination.name", "orders")
				.hasLowCardinalityKeyValue("messaging.destination.kind", "stream")
				.hasLowCardinalityKeyValue("messaging.batch", "false")
				.hasHighCardinalityKeyValue("messaging.message.id", "seq-1")
				.hasHighCardinalityKeyValue("messaging.kinesis.partition_key", "pk-1")
				.hasHighCardinalityKeyValue("messaging.kinesis.shard_id", "shard-1");
	}

	@Test
	@DisplayName("a failed send records the unwrapped error on the observation")
	@SuppressWarnings("unchecked")
	void failedSendRecordsError() {
		when(this.client.putRecord(any(Consumer.class))).thenReturn(
				CompletableFuture.failedFuture(ResourceNotFoundException.builder().message("no stream").build()));

		assertThatThrownBy(() -> template().send("orders", "pk-1", "hello"))
				.isInstanceOf(ResourceNotFoundException.class);

		TestObservationRegistryAssert.then(this.registry).hasSingleObservationThat().hasBeenStopped()
				.assertThatError().isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("sendBatch records an observation with the record and failed counts")
	void sendBatchIsObserved() {
		when(this.client.putRecords(any(PutRecordsRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(PutRecordsResponse.builder().failedRecordCount(1)
						.records(PutRecordsResultEntry.builder().sequenceNumber("seq-a").shardId("shard-0").build(),
								PutRecordsResultEntry.builder().errorCode("InternalFailure").errorMessage("boom")
										.build())
						.build()));

		template().sendBatch("orders",
				List.of(SendRequest.builder().streamName("orders").partitionKey("pk-0").payload("a").build(),
						SendRequest.builder().streamName("orders").partitionKey("pk-1").payload("b").build()));

		TestObservationRegistryAssert.then(this.registry).hasSingleObservationThat().hasBeenStopped()
				.hasLowCardinalityKeyValue("messaging.batch", "true")
				.hasLowCardinalityKeyValue("messaging.destination.name", "orders")
				.hasHighCardinalityKeyValue("messaging.batch.message_count", "2")
				.hasHighCardinalityKeyValue("messaging.kinesis.batch.failed_count", "1");
	}

	@Test
	@DisplayName("a custom convention replaces the default key values")
	@SuppressWarnings("unchecked")
	void customConventionIsApplied() {
		when(this.client.putRecord(any(Consumer.class))).thenReturn(CompletableFuture
				.completedFuture(PutRecordResponse.builder().sequenceNumber("seq-1").shardId("shard-1").build()));
		KinesisTemplate template = template();
		template.setObservationConvention(new KinesisTemplateObservation.DefaultConvention() {
			@Override
			protected KeyValues getCustomLowCardinalityKeyValues(KinesisTemplateObservation.Context context) {
				return KeyValues.of("team", "orders");
			}
		});

		template.send("orders", "pk-1", "hello");

		TestObservationRegistryAssert.then(this.registry).hasSingleObservationThat()
				.hasLowCardinalityKeyValue("team", "orders")
				.hasLowCardinalityKeyValue("messaging.system", "aws_kinesis");
	}

	@Test
	@DisplayName("with embedded headers the injected trace context is embedded in the sent record")
	void embedsInjectedTraceContextWhenConverterEmbedsHeaders() {
		ArgumentCaptor<PutRecordsRequest> request = ArgumentCaptor.forClass(PutRecordsRequest.class);
		when(this.client.putRecords(request.capture())).thenReturn(CompletableFuture.completedFuture(PutRecordsResponse
				.builder().failedRecordCount(0)
				.records(PutRecordsResultEntry.builder().sequenceNumber("seq-a").shardId("shard-0").build()).build()));

		// A handler that plays the role of a tracer: on start it writes a traceparent into the context carrier.
		this.registry.observationConfig()
				.observationHandler(new ObservationHandler<KinesisTemplateObservation.Context>() {
					@Override
					public boolean supportsContext(Observation.Context context) {
						return context instanceof KinesisTemplateObservation.Context;
					}

					@Override
					public void onStart(KinesisTemplateObservation.Context context) {
						context.getSetter().set(context.getCarrier(), "traceparent", "00-trace-span-01");
					}
				});

		EmbeddedHeadersKinesisMessageConverter converter = new EmbeddedHeadersKinesisMessageConverter(
				new KinesisMessagingMessageConverter());
		KinesisTemplate template = new KinesisTemplate(this.client, converter);
		template.setObservationRegistry(this.registry);

		template.sendBatch("orders",
				List.of(SendRequest.builder().streamName("orders").partitionKey("pk-0").payload("hello").build()));

		byte[] data = request.getValue().records().get(0).data().asByteArray();
		Message<?> decoded = new EmbeddedJsonHeadersMessageMapper().toMessage(data, null);
		assertThat(decoded.getPayload()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
		assertThat(decoded.getHeaders().get("traceparent")).isEqualTo("00-trace-span-01");
	}

	private KinesisTemplate template() {
		KinesisTemplate template = new KinesisTemplate(this.client, new JsonMapper());
		template.setObservationRegistry(this.registry);
		return template;
	}

}
