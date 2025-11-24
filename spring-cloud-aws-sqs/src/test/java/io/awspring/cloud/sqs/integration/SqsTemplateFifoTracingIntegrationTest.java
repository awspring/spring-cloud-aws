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
package io.awspring.cloud.sqs.integration;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import io.micrometer.tracing.test.simple.SimpleTraceContext;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for trace context propagation in FIFO queues with SqsTemplate.
 * <p>
 * Verifies that trace headers (traceparent) are correctly propagated from sender to receiver when using
 * {@code sendAsync()} with FIFO queues, including scenarios where queue attributes must be resolved asynchronously on
 * the first call and when they are cached on subsequent calls.
 *
 * @author Igor Quintanilha
 */
@SpringBootTest
public class SqsTemplateFifoTracingIntegrationTest extends BaseSqsIntegrationTest {
	private static final Logger logger = LoggerFactory.getLogger(SqsTemplateFifoTracingIntegrationTest.class);

	private static final String FIFO_QUEUE_NAME = "trace-context-test-queue.fifo";
	private static final String FIFO_CACHE_HIT_QUEUE_NAME = "trace-context-test-queue-cache-hit.fifo";

	@Autowired
	private SqsTemplate sqsTemplate;

	@Autowired
	private TestObservationRegistry observationRegistry;

	@Autowired
	private CurrentTraceContext currentTraceContext;

	@BeforeAll
	static void beforeTests() {
		var client = createAsyncClient();
		createFifoQueue(client, FIFO_QUEUE_NAME, Map.of(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "false")).join();
		createFifoQueue(client, FIFO_CACHE_HIT_QUEUE_NAME, Map.of(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "true")).join();

	}

	@AfterEach
	void cleanupAfterEach() {
		observationRegistry.clear();
	}

	@Test
	void sendAsync_toFifoQueue_shouldPropagateObservationScopeOnFirstCall() {
		var parentObservation = Observation.start("parent-observation", observationRegistry);
		var payload = new TestEvent(UUID.randomUUID().toString());
		String expectedTraceId;

		try (var ignored = parentObservation.openScope()) {
			expectedTraceId = currentTraceContext.context().traceId();
			sqsTemplate.sendAsync(FIFO_QUEUE_NAME, payload).join();
		}
		finally {
			parentObservation.stop();
		}

		logger.info("expectedTraceId={}", expectedTraceId);

		var receivedMessage = sqsTemplate
				.receive(from -> from.queue(FIFO_QUEUE_NAME).pollTimeout(Duration.ofSeconds(5)), TestEvent.class)
				.orElseThrow(() -> new AssertionError("Expected message was not received"));

		assertThat(receivedMessage.getPayload()).isEqualTo(payload);
		var traceparent = (String) receivedMessage.getHeaders().get("traceparent");
		assertThat(traceparent).as("traceparent header should be present").isNotNull();
		assertThat(traceparent).as("traceparent should contain the traceId").contains(expectedTraceId);
	}

	@Test
	void sendAsync_toFifoQueue_shouldCreateObservationOnCallingThreadAfterCacheHit() {
		// Given - Warm up: send a message to populate the queue attribute cache
		var warmupPayload = new TestEvent(UUID.randomUUID().toString());
		sqsTemplate.sendAsync(FIFO_CACHE_HIT_QUEUE_NAME, warmupPayload).join();

		// Drain the warmup message
		sqsTemplate.receive(from -> from.queue(FIFO_CACHE_HIT_QUEUE_NAME).pollTimeout(Duration.ofSeconds(5)), TestEvent.class);

		// Given - Start a NEW observation for the actual test
		var observation = Observation.start("test-send-second", observationRegistry);
		String expectedTraceId;

		var payload = new TestEvent(UUID.randomUUID().toString());
		try (var ignored = observation.openScope()) {
			expectedTraceId = currentTraceContext.context().traceId();
			// When - Second call (cache hit - queue attributes already resolved)
			sqsTemplate.sendAsync(FIFO_CACHE_HIT_QUEUE_NAME, payload).join();
		}
		finally {
			observation.stop();
		}

		logger.info("expectedTraceId={}", expectedTraceId);

		var receivedMessage = sqsTemplate
				.receive(from -> from.queue(FIFO_CACHE_HIT_QUEUE_NAME).pollTimeout(Duration.ofSeconds(5)), TestEvent.class)
				.orElseThrow(() -> new AssertionError("Expected message was not received"));

		assertThat(receivedMessage.getPayload()).isEqualTo(payload);
		var traceparent = (String) receivedMessage.getHeaders().get("traceparent");
		assertThat(traceparent).as("traceparent header should be present").isNotNull();
		assertThat(traceparent).as("traceparent should contain the traceId").contains(expectedTraceId);
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public SqsAsyncClient sqsAsyncClient() {
			return createAsyncClient();
		}

		@Bean
		public Tracer tracer() {
			return new SimpleTracer();
		}

		@Bean
		public CurrentTraceContext currentTraceContext(Tracer tracer) {
			return ((SimpleTracer) tracer).currentTraceContext();
		}

		@Bean
		public Propagator propagator(Tracer tracer) {
			return new SimplePropagator(tracer);
		}

		@Bean
		public ObservationRegistry observationRegistry(Tracer tracer, Propagator propagator) {
			TestObservationRegistry registry = TestObservationRegistry.create();
			registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));
			registry.observationConfig()
					.observationHandler(new PropagatingSenderTracingObservationHandler<>(tracer, propagator));
			registry.observationConfig()
					.observationHandler(new PropagatingReceiverTracingObservationHandler<>(tracer, propagator));
			return registry;
		}

		@Bean
		public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient, ObservationRegistry observationRegistry) {
			return SqsTemplate.builder().sqsAsyncClient(sqsAsyncClient)
					.configure(options -> options.observationRegistry(observationRegistry)).build();
		}
	}

	/**
	 * Simple W3C Trace Context propagator for testing. In production, you would use a library like
	 * micrometer-tracing-bridge-brave or micrometer-tracing-bridge-otel which provide full-featured propagators.
	 */
	static class SimplePropagator implements Propagator {

		private final Tracer tracer;

		SimplePropagator(Tracer tracer) {
			this.tracer = tracer;
		}

		@Override
		public List<String> fields() {
			return List.of("traceparent", "tracestate");
		}

		@Override
		public <C> void inject(TraceContext context, C carrier, Setter<C> setter) {
			// W3C Trace Context format: version-traceId-spanId-flags
			var traceparent = String.format("00-%s-%s-01", context.traceId(), context.spanId());
			setter.set(carrier, "traceparent", traceparent);
		}

		@Override
		public <C> Span.Builder extract(C carrier, Getter<C> getter) {
			var traceparent = getter.get(carrier, "traceparent");
			if (traceparent == null || traceparent.isEmpty()) {
				return tracer.spanBuilder().setNoParent();
			}
			// Parse W3C format: 00-traceId-spanId-01
			String[] parts = traceparent.split("-");
			if (parts.length < 4) {
				return tracer.spanBuilder().setNoParent();
			}
			// Use tracer to create span builder with extracted context
			Span.Builder builder = tracer.spanBuilder();
			var traceContext = new SimpleTraceContext();
			traceContext.setTraceId(parts[1]);
			traceContext.setParentId(parts[2]);
			traceContext.setSpanId(parts[3]);
			builder.setParent(traceContext);
			return builder;
		}
	}

	record TestEvent(String data) {
	}
}
