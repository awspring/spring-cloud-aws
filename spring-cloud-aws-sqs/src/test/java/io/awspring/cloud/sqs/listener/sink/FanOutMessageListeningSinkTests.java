/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sqs.listener.sink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import brave.Tracing;
import brave.handler.MutableSpan;
import brave.test.TestSpanHandler;
import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.pipeline.*;
import io.awspring.cloud.sqs.observation.MessageObservationDocumentation;
import io.awspring.cloud.sqs.observation.MessagingOperationType;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.ObservationContextAssert;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Tests for {@link FanOutMessageSink}.
 *
 * @author Mariusz Sondecki
 */
class FanOutMessageListeningSinkTests {

	private static final String TRACE_ID_HEADER = "X-B3-TraceId";

	private static final String SPAN_ID_HEADER = "X-B3-SpanId";

	private static final String PARENT_SPAN_ID_HEADER = "X-B3-ParentSpanId";

	@Test
	void shouldEmitInNestedObservation() {
		// GIVEN
		TestObservationRegistry registry = TestObservationRegistry.create();
		Message<String> messageToEmit = MessageBuilder.withPayload("foo").build();
		List<Message<String>> received = new ArrayList<>(1);

		MessageProcessingConfiguration.Builder<String> configuration = MessageProcessingConfiguration.<String> builder()
				.interceptors(List.of(getInterceptor(registry, Collections.emptyList(), null)))
				.messageListener(getListener(received, registry));

		// WHEN
		emitMessage(registry, Runnable::run, messageToEmit, configuration);

		// THEN
		assertThat(received).containsExactly(messageToEmit);
		TestObservationRegistryAssert.then(registry).hasNumberOfObservationsEqualTo(3)
				.hasHandledContextsThatSatisfy(contexts -> {
					ObservationContextAssert.then(contexts.get(0)).hasNameEqualTo("sqs.single.message.polling.process")
							.hasHighCardinalityKeyValueWithKey(
									MessageObservationDocumentation.HighCardinalityKeyNames.MESSAGE_ID.asString())
							.hasLowCardinalityKeyValue(
									MessageObservationDocumentation.LowCardinalityKeyNames.OPERATION.asString(),
									MessagingOperationType.SINGLE_POLLING_PROCESS.getValue())
							.doesNotHaveParentObservation();

					ObservationContextAssert.then(contexts.get(1)).hasNameEqualTo("sqs.interceptor.process")
							.hasParentObservationContextMatching(
									contextView -> contextView.getName().equals("sqs.single.message.polling.process"));

					ObservationContextAssert.then(contexts.get(2)).hasNameEqualTo("sqs.listener.process")
							.hasHighCardinalityKeyValue("payload", "foo").hasParentObservationContextMatching(
									contextView -> contextView.getName().equals("sqs.interceptor.process"));
				});
	}

	@Test
	void shouldEmitWithTracingContextFromMessage() {
		// GIVEN
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setTaskDecorator(new ContextPropagatingTaskDecorator());

		TestSpanHandler testSpanHandler = new TestSpanHandler();
		Tracing tracing = Tracing.newBuilder().addSpanHandler(testSpanHandler).build();
		io.micrometer.tracing.Tracer tracer = new BraveTracer(tracing.tracer(),
				new BraveCurrentTraceContext(tracing.currentTraceContext()), new BraveBaggageManager());

		ObservationRegistry registry = ObservationRegistry.create();
		registry.observationConfig().observationHandler(
				new PropagatingReceiverTracingObservationHandler<>(tracer, new BravePropagator(tracing)));

		TraceContext b3TraceContext = tracer.nextSpan().context();
		String traceId = b3TraceContext.traceId();
		String spanId = b3TraceContext.spanId();
		List<Map<String, String>> contexts = new ArrayList<>();
		Message<String> messageToEmit = MessageBuilder.withPayload("")
				.copyHeaders(Map.of(TRACE_ID_HEADER, traceId, SPAN_ID_HEADER, spanId, PARENT_SPAN_ID_HEADER, spanId))
				.build();

		MessageProcessingConfiguration.Builder<String> configuration = MessageProcessingConfiguration.<String> builder()
				.interceptors(List.of(getInterceptor(registry, contexts, tracer)))
				.messageListener(getListener(contexts, registry, tracer));

		// WHEN
		emitMessage(registry, taskExecutor, messageToEmit, configuration);

		// THEN
		MutableSpan finishedSpan = testSpanHandler.get(0);
		Map<String, String> context = contexts.get(0);
		assertThat(finishedSpan.traceId()).isEqualTo(traceId).isEqualTo(context.get(TRACE_ID_HEADER));
		assertThat(finishedSpan.id()).isNotEqualTo(spanId).isEqualTo(context.get(SPAN_ID_HEADER));
		assertThat(finishedSpan.parentId()).isEqualTo(spanId).isEqualTo(context.get(PARENT_SPAN_ID_HEADER));
	}

	private void emitMessage(ObservationRegistry registry, TaskExecutor taskExecutor, Message<String> messageToEmit,
			MessageProcessingConfiguration.Builder<String> configuration) {
		AcknowledgementHandler<String> dummyAcknowledgementHandler = new AcknowledgementHandler<>() {
		};

		MessageProcessingPipeline<String> messageProcessingPipeline = MessageProcessingPipelineBuilder
				.<String> first(BeforeProcessingInterceptorExecutionStage::new).then(MessageListenerExecutionStage::new)
				.thenInTheFuture(AfterProcessingInterceptorExecutionStage::new)
				.build(configuration.ackHandler(dummyAcknowledgementHandler).build());

		AbstractMessageProcessingPipelineSink<String> sink = new FanOutMessageSink<>();
		sink.setObservationRegistry(registry);
		sink.setTaskExecutor(taskExecutor);
		sink.setMessagePipeline(messageProcessingPipeline);
		sink.start();
		sink.emit(List.of(messageToEmit), MessageProcessingContext.create()).join();
		sink.stop();
	}

	private AsyncMessageInterceptor<String> getInterceptor(ObservationRegistry registry,
			List<Map<String, String>> contexts, io.micrometer.tracing.Tracer tracer) {
		return new AsyncMessageInterceptor<>() {
			@Override
			public CompletableFuture<Message<String>> intercept(Message<String> message) {
				Observation.createNotStarted("sqs.interceptor.process", registry).start().openScope();
				if (tracer != null) {
					Span span = tracer.currentSpan();
					if (span != null) {
						TraceContext traceContext = span.context();
						contexts.add(Map.of(TRACE_ID_HEADER, traceContext.traceId(), SPAN_ID_HEADER,
								traceContext.spanId(), PARENT_SPAN_ID_HEADER, traceContext.parentId()));
					}
				}
				return AsyncMessageInterceptor.super.intercept(message);
			}

			@Override
			public CompletableFuture<Void> afterProcessing(Message<String> message, Throwable t) {
				Observation observation = registry.getCurrentObservation();
				if (observation != null) {
					Observation.Scope currentScope = observation.getEnclosingScope();
					if (currentScope != null) {
						currentScope.close();
					}
					else {
						fail("A scope for current observation expected");
					}
					observation.stop();
				}
				return AsyncMessageInterceptor.super.afterProcessing(message, t);
			}
		};
	}

	private AsyncMessageListener<String> getListener(List<Message<String>> received, ObservationRegistry registry) {
		return message -> Objects.requireNonNull(Observation.createNotStarted("sqs.listener.process", registry)
				.highCardinalityKeyValue("payload", message.getPayload()).observe(() -> {
					received.add(message);
					return CompletableFuture.completedFuture(null);
				}));
	}

	private AsyncMessageListener<String> getListener(List<Map<String, String>> contexts, ObservationRegistry registry,
			io.micrometer.tracing.Tracer tracer) {
		return message -> Objects
				.requireNonNull(Observation.createNotStarted("sqs.listener.process", registry).observe(() -> {
					Span span = tracer.currentSpan();
					if (span != null) {
						TraceContext traceContext = span.context();
						contexts.add(Map.of(TRACE_ID_HEADER, traceContext.traceId(), SPAN_ID_HEADER,
								traceContext.spanId(), PARENT_SPAN_ID_HEADER, traceContext.parentId()));
					}
					return CompletableFuture.completedFuture(null);
				}));
	}

}
