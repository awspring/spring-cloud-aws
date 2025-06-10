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
package io.awspring.cloud.sqs.listener.sink;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingPipeline;
import io.awspring.cloud.sqs.support.observation.SqsListenerObservation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Tests for Observation support in {@link AbstractMessageProcessingPipelineSink}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class AbstractMessageProcessingPipelineSinkObservationTest {

	private TestMessageProcessingPipelineSink sink;

	private TestObservationRegistry observationRegistry;

	private MessageProcessingPipeline<String> messagePipeline;

	@BeforeEach
	void setUp() {
		observationRegistry = TestObservationRegistry.create();
		messagePipeline = mock(MessageProcessingPipeline.class);

		sink = new TestMessageProcessingPipelineSink();
		sink.setMessagePipeline(messagePipeline);
		sink.setObservationSpecifics(new SqsListenerObservation.SqsSpecifics());

		SqsContainerOptions options = SqsContainerOptions.builder().observationRegistry(observationRegistry).build();

		sink.configure(options);
		sink.setTaskExecutor(new SimpleAsyncTaskExecutor());
	}

	@Test
	void shouldCreateAndCompleteObservation() throws Exception {
		// given
		Message<String> message = MessageBuilder.withPayload("test-payload")
				.setHeader("test-header", "test-header-value").setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, "test-queue")
				.build();

		MessageProcessingContext<String> context = MessageProcessingContext.create();

		when(messagePipeline.process(any(Message.class), eq(context)))
				.thenReturn(CompletableFuture.completedFuture(null));

		// when
		CompletableFuture<Void> result = sink.emit(Collections.singleton(message), context);

		// then
		result.get(); // wait for completion

		then(messagePipeline).should().process(any(Message.class), any());

		TestObservationRegistryAssert.then(observationRegistry).hasNumberOfObservationsEqualTo(1)
				.hasSingleObservationThat().hasNameEqualTo("spring.aws.sqs.listener")
				.hasLowCardinalityKeyValue("messaging.system", "sqs")
				.hasLowCardinalityKeyValue("messaging.operation", "receive")
				.hasLowCardinalityKeyValue("messaging.source.name", "test-queue")
				.hasLowCardinalityKeyValue("messaging.source.kind", "queue");

		// Verify the message was modified to include the observation context
		then(messagePipeline).should().process(org.mockito.ArgumentMatchers
				.<Message> argThat(msg -> msg.getHeaders().containsKey(ObservationThreadLocalAccessor.KEY)), any());
	}

	@Test
	void shouldHandleExceptionAndCompleteObservation() {
		// given
		Message<String> message = MessageBuilder.withPayload("test-payload")
				.setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, "test-queue").build();
		MessageProcessingContext<String> context = MessageProcessingContext.create();

		RuntimeException testException = new RuntimeException("Test exception");
		when(messagePipeline.process(any(Message.class), any()))
				.thenReturn(CompletableFuture.failedFuture(testException));

		// when
		CompletableFuture<Void> result = sink.emit(Collections.singleton(message), context);

		// then
		try {
			result.join(); // should fail
		}
		catch (Exception ignored) {
			// Expected
		}
		TestObservationRegistryAssert.then(observationRegistry).hasNumberOfObservationsEqualTo(1)
				.hasSingleObservationThat().hasNameEqualTo(("spring.aws.sqs.listener")).hasError();
	}

	@Test
	void shouldIncludeFifoSpecificAttributesInObservation() throws Exception {
		// given
		Message<String> message = MessageBuilder.withPayload("test-payload")
				.setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, "test-queue.fifo")
				.setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, "test-group")
				.setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, "test-dedup-id")
				.build();

		MessageProcessingContext<String> context = MessageProcessingContext.create();

		when(messagePipeline.process(any(Message.class), eq(context)))
				.thenReturn(CompletableFuture.completedFuture(null));

		// when
		CompletableFuture<Void> result = sink.emit(Collections.singleton(message), context);

		// then
		result.get(); // wait for completion

		TestObservationRegistryAssert.then(observationRegistry).hasNumberOfObservationsEqualTo(1)
				.hasSingleObservationThat().hasLowCardinalityKeyValue("messaging.system", "sqs")
				.hasLowCardinalityKeyValue("messaging.source.name", "test-queue.fifo")
				.hasHighCardinalityKeyValue("messaging.message.message-group.id", "test-group")
				.hasHighCardinalityKeyValue("messaging.message.message-deduplication.id", "test-dedup-id");
	}
	
	@Test
	void shouldHandleMessageWithoutTraceparentHeader() throws Exception {

		// given
		Message<String> message = MessageBuilder.withPayload("test-payload")
				.setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, "test-queue")
				// Explicitly NOT setting any traceparent or observation related headers
				.build();

		MessageProcessingContext<String> context = MessageProcessingContext.create();

		when(messagePipeline.process(any(Message.class), eq(context)))
				.thenReturn(CompletableFuture.completedFuture(null));

		// when
		CompletableFuture<Void> result = sink.emit(Collections.singleton(message), context);

		// then
		result.get();

		then(messagePipeline).should().process(org.mockito.ArgumentMatchers
				.<Message>argThat(msg -> msg.getHeaders().containsKey(ObservationThreadLocalAccessor.KEY)), any());
		
		// Verify an observation was created despite lack of traceparent
		TestObservationRegistryAssert.then(observationRegistry).hasNumberOfObservationsEqualTo(1)
				.hasSingleObservationThat().hasNameEqualTo("spring.aws.sqs.listener")
			.doesNotHaveParentObservation();
	}

	private static class TestMessageProcessingPipelineSink extends AbstractMessageProcessingPipelineSink<String> {

		@Override
		protected CompletableFuture<Void> doEmit(Collection<Message<String>> messages,
				MessageProcessingContext<String> context) {
			// Process each message using the execute method
			Message<String> message = messages.iterator().next(); // We're only testing with one message
			return execute(message, context);
		}

		@Override
		public void start() {
			// No-op for testing
		}

		@Override
		public void stop() {
			// No-op for testing
		}

		@Override
		public boolean isRunning() {
			return true;
		}
	}
}
