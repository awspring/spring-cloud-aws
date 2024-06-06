/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.sqs.observation;

import static org.assertj.core.api.Assertions.assertThat;

import brave.Tracing;
import brave.handler.MutableSpan;
import brave.test.TestSpanHandler;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;

/**
 * Tests for {@link BatchMessageProcessTracingObservationHandler}.
 *
 * @author Mariusz Sondecki
 */
class BatchMessageProcessTracingObservationHandlerTest {

	private static final String TRACE_ID_HEADER = "X-B3-TraceId";

	private static final String SPAN_ID_HEADER = "X-B3-SpanId";

	private static final String PARENT_SPAN_ID_HEADER = "X-B3-ParentSpanId";

	private final TestSpanHandler testSpanHandler = new TestSpanHandler();

	private final Tracing tracing = Tracing.newBuilder().addSpanHandler(testSpanHandler).build();

	private final io.micrometer.tracing.Tracer tracer = new BraveTracer(tracing.tracer(),
			new BraveCurrentTraceContext(tracing.currentTraceContext()), new BraveBaggageManager());

	private final BatchMessageProcessTracingObservationHandler handler = new BatchMessageProcessTracingObservationHandler(
			tracer, new BravePropagator(tracing));

	@Test
	void shouldCreateLinksForAllMessages() {
		// GIVEN
		TraceContext traceContext1 = tracer.nextSpan().context();
		TraceContext traceContext2 = tracer.nextSpan().context();
		String traceId1 = traceContext1.traceId();
		String spanId1 = traceContext1.spanId();
		String traceId2 = traceContext2.traceId();
		String spanId2 = traceContext2.spanId();
		Collection<MessageHeaders> receivedMessageHeaders = List.of(
				new MessageHeaders(
						Map.of(TRACE_ID_HEADER, traceId1, SPAN_ID_HEADER, spanId1, PARENT_SPAN_ID_HEADER, spanId1)),
				new MessageHeaders(
						Map.of(TRACE_ID_HEADER, traceId2, SPAN_ID_HEADER, spanId2, PARENT_SPAN_ID_HEADER, spanId2)));
		BatchMessagePollingProcessObservationContext context = new BatchMessagePollingProcessObservationContext(
				receivedMessageHeaders);

		// WHEN
		handler.onStart(context);
		handler.onStop(context);

		// THEN
		MutableSpan finishedSpan = testSpanHandler.get(0);
		assertThat(traceId1).isNotEqualTo(traceId2);
		assertThat(finishedSpan.traceId()).isNotEmpty().withFailMessage(
				"A trace ID of the process of the whole batch should differ from trace IDs of received messages")
				.isNotIn(traceId1, traceId2);
		assertThat(finishedSpan.tags()).containsEntry("links[0].traceId", traceId1)
				.containsEntry("links[1].traceId", traceId2)
				.withFailMessage(
						"""
								"links[*].spanId" tags should differ from span IDs of received messages (which will be parent span IDs)""")
				.doesNotContainEntry("links[0].spanId", spanId1).doesNotContainEntry("links[1].spanId", spanId2);
	}
}
