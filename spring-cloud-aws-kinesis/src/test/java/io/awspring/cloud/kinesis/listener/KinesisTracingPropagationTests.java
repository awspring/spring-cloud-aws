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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brave.Tracing;
import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.propagation.TraceContext;
import brave.propagation.tracecontext.TraceContextPropagation;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.kinesis.operations.KinesisTemplate;
import io.awspring.cloud.kinesis.operations.SendRequest;
import io.awspring.cloud.kinesis.support.converter.EmbeddedHeadersKinesisMessageConverter;
import io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResultEntry;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

/**
 * End-to-end trace propagation test driven by a real Brave tracer with W3C propagation. A record is sent with
 * {@link KinesisTemplate} and an {@link EmbeddedHeadersKinesisMessageConverter}, then the embedded bytes are replayed
 * through the KCL listener path with the same converter and registry, and the two spans are checked to share one trace
 * with the send span as the parent of the receive span.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
class KinesisTracingPropagationTests {

	private static final ErrorHandler RETHROWING = failedDelivery -> {
		throw new IllegalStateException(failedDelivery.getCause());
	};

	private final List<MutableSpan> spans = new ArrayList<>();

	private final Tracing tracing = Tracing.newBuilder().currentTraceContext(ThreadLocalCurrentTraceContext.create())
			.propagationFactory(TraceContextPropagation.FACTORY).addSpanHandler(new SpanHandler() {
				@Override
				public boolean end(TraceContext context, MutableSpan span, Cause cause) {
					spans.add(span);
					return true;
				}
			}).build();

	private final Tracer tracer = new BraveTracer(this.tracing.tracer(),
			new BraveCurrentTraceContext(this.tracing.currentTraceContext()), new BraveBaggageManager());

	private final Propagator propagator = new BravePropagator(this.tracing);

	private final TestObservationRegistry registry = createRegistry();

	private TestObservationRegistry createRegistry() {
		TestObservationRegistry observationRegistry = TestObservationRegistry.create();
		observationRegistry.observationConfig().observationHandler(new DefaultTracingObservationHandler(this.tracer));
		observationRegistry.observationConfig()
				.observationHandler(new PropagatingSenderTracingObservationHandler<>(this.tracer, this.propagator));
		observationRegistry.observationConfig()
				.observationHandler(new PropagatingReceiverTracingObservationHandler<>(this.tracer, this.propagator));
		return observationRegistry;
	}

	@AfterEach
	void closeTracing() {
		this.tracing.close();
	}

	@Test
	@DisplayName("the listener span continues the template trace and is its child when headers are embedded")
	void listenerSpanContinuesTemplateTrace() {
		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		ArgumentCaptor<PutRecordsRequest> request = ArgumentCaptor.forClass(PutRecordsRequest.class);
		when(client.putRecords(request.capture())).thenReturn(CompletableFuture.completedFuture(PutRecordsResponse
				.builder().failedRecordCount(0)
				.records(PutRecordsResultEntry.builder().sequenceNumber("seq-1").shardId("shard-1").build()).build()));

		EmbeddedHeadersKinesisMessageConverter converter = new EmbeddedHeadersKinesisMessageConverter(
				new KinesisMessagingMessageConverter());
		KinesisTemplate template = new KinesisTemplate(client, converter);
		template.setObservationRegistry(this.registry);

		template.sendBatch("orders",
				List.of(SendRequest.builder().streamName("orders").partitionKey("pk-1").payload("hello").build()));

		byte[] data = request.getValue().records().get(0).data().asByteArray();

		AtomicReference<String> listenerTraceId = new AtomicReference<>();
		AtomicReference<String> listenerSpanId = new AtomicReference<>();
		KclShardRecordProcessor processor = new KclShardRecordProcessor(converter, null, KclListenerMode.SINGLE_RECORD,
				KclCheckpointMode.MANUAL, 1000L, Duration.ofSeconds(60), message -> {
					listenerTraceId.set(this.tracer.currentSpan().context().traceId());
					listenerSpanId.set(this.tracer.currentSpan().context().spanId());
				}, null, RETHROWING, "orders", this.registry, null, null, null, null, null, null);
		processor.initialize(InitializationInput.builder().shardId("shard-1").build());

		RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
		processor.processRecords(ProcessRecordsInput.builder().records(List.of(record("pk-1", "seq-1", data)))
				.checkpointer(checkpointer).build());

		MutableSpan sendSpan = spanWithName("orders send");
		MutableSpan receiveSpan = spanWithName("orders receive");
		assertThat(receiveSpan.traceId()).as("listener span shares the template trace id")
				.isEqualTo(sendSpan.traceId());
		assertThat(receiveSpan.parentId()).as("template span is the parent of the listener span")
				.isEqualTo(sendSpan.id());
		assertThat(listenerTraceId.get()).isEqualTo(sendSpan.traceId());
		assertThat(listenerSpanId.get()).isEqualTo(receiveSpan.id());
	}

	private MutableSpan spanWithName(String name) {
		return this.spans.stream().filter(span -> name.equals(span.name())).findFirst()
				.orElseThrow(() -> new AssertionError("no span named '" + name + "' in " + this.spans));
	}

	private KinesisClientRecord record(String partitionKey, String sequenceNumber, byte[] body) {
		return KinesisClientRecord.builder().partitionKey(partitionKey).sequenceNumber(sequenceNumber)
				.subSequenceNumber(0L).approximateArrivalTimestamp(Instant.ofEpochMilli(1000L))
				.data(ByteBuffer.wrap(body)).build();
	}

}
