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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.Visibility;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.BatchAcknowledgement;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.converter.AbstractMessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Integration tests for SQS payload type inference without type headers.
 *
 * @author Tomaz Fernandes
 */
@SpringBootTest
class SqsPayloadTypeInferenceIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsPayloadTypeInferenceIntegrationTests.class);

	static final String INFERS_SIMPLE_POJO_QUEUE = "infers_simple_pojo_queue";

	static final String INFERS_POJO_WITH_MANY_PARAMETERS_QUEUE = "infers_pojo_with_many_parameters_queue";

	static final String INFERS_BATCH_POJO_QUEUE = "infers_batch_pojo_queue";

	static final String INFERS_NESTED_GENERIC_POJO_QUEUE = "infers_nested_generic_pojo_queue";

	static final String ASYNC_LISTENER_QUEUE = "async_listener_type_inference_queue";

	static final String BATCH_MESSAGE_WRAPPER_QUEUE = "batch_message_wrapper_queue";

	static final String IGNORES_TYPE_HEADER_QUEUE = "ignores_type_header_queue";

	static final String EXPLICIT_PAYLOAD_ANNOTATION_QUEUE = "explicit_payload_annotation_queue";

	static final String STRING_PAYLOAD_QUEUE = "string_payload_queue";

	static final String CUSTOM_CONVERTER_QUEUE = "custom_converter_type_inference_queue";

	static final String ERROR_HANDLER_TEST_QUEUE = "error_handler_type_inference_queue";

	static final String MANUAL_ACK_FACTORY = "manualAckFactory";

	static final String CUSTOM_CONVERTER_FACTORY = "customConverterFactory";

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(createQueue(client, INFERS_SIMPLE_POJO_QUEUE),
				createQueue(client, INFERS_POJO_WITH_MANY_PARAMETERS_QUEUE),
				createQueue(client, INFERS_BATCH_POJO_QUEUE), createQueue(client, INFERS_NESTED_GENERIC_POJO_QUEUE),
				createQueue(client, ASYNC_LISTENER_QUEUE), createQueue(client, BATCH_MESSAGE_WRAPPER_QUEUE),
				createQueue(client, IGNORES_TYPE_HEADER_QUEUE), createQueue(client, EXPLICIT_PAYLOAD_ANNOTATION_QUEUE),
				createQueue(client, STRING_PAYLOAD_QUEUE), createQueue(client, CUSTOM_CONVERTER_QUEUE),
				createQueue(client, ERROR_HANDLER_TEST_QUEUE)).join();
	}

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	SqsTemplate sqsTemplate;

	@Autowired
	PojoCollector pojoCollector;

	@Autowired
	InterceptorPayloadTypeCollector interceptorPayloadTypeCollector;

	@Autowired
	ErrorHandlerPayloadTypeCollector errorHandlerPayloadTypeCollector;

	@Autowired
	AckCallbackPayloadTypeCollector ackCallbackPayloadTypeCollector;

	@Test
	void shouldInferSimplePojoType() throws Exception {
		CountDownLatch ackLatch = new CountDownLatch(1);
		ackCallbackPayloadTypeCollector.registerLatch(INFERS_SIMPLE_POJO_QUEUE, ackLatch);

		TestEvent event = new TestEvent("test-id", "test-payload");
		sqsTemplate.send(INFERS_SIMPLE_POJO_QUEUE, event);
		logger.debug("Sent event to queue {}: {}", INFERS_SIMPLE_POJO_QUEUE, event);

		assertThat(latchContainer.infersSimplePojoLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(pojoCollector.receivedSimplePojos).hasSize(1);
		assertThat(pojoCollector.receivedSimplePojos.get(0)).isEqualTo(event);
		interceptorPayloadTypeCollector.assertPayloadsForQueueContains(INFERS_SIMPLE_POJO_QUEUE, event);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		ackCallbackPayloadTypeCollector.assertPayloadsForQueueContains(INFERS_SIMPLE_POJO_QUEUE, event);
	}

	@Test
	void shouldInferPojoWithManyParameterTypes() throws Exception {
		CountDownLatch ackLatch = new CountDownLatch(1);
		ackCallbackPayloadTypeCollector.registerLatch(INFERS_POJO_WITH_MANY_PARAMETERS_QUEUE, ackLatch);

		TestEvent event = new TestEvent("test-id-2", "test-payload-2");
		sqsTemplate.send(INFERS_POJO_WITH_MANY_PARAMETERS_QUEUE, event);
		logger.debug("Sent event to queue {}: {}", INFERS_POJO_WITH_MANY_PARAMETERS_QUEUE, event);

		assertThat(latchContainer.infersPojoWithManyParametersLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(pojoCollector.receivedPojoWithManyParams).hasSize(1);
		assertThat(pojoCollector.receivedPojoWithManyParams.get(0)).isEqualTo(event);
		interceptorPayloadTypeCollector.assertPayloadsForQueueContains(INFERS_POJO_WITH_MANY_PARAMETERS_QUEUE, event);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		ackCallbackPayloadTypeCollector.assertPayloadsForQueueContains(INFERS_POJO_WITH_MANY_PARAMETERS_QUEUE, event);
	}

	@Test
	void shouldInferBatchPojoType() throws Exception {
		CountDownLatch ackLatch = new CountDownLatch(3);
		ackCallbackPayloadTypeCollector.registerLatch(INFERS_BATCH_POJO_QUEUE, ackLatch);

		List<TestEvent> events = List.of(new TestEvent("batch-1", "payload-1"), new TestEvent("batch-2", "payload-2"),
				new TestEvent("batch-3", "payload-3"));

		sqsTemplate.sendMany(INFERS_BATCH_POJO_QUEUE,
				events.stream().map(e -> MessageBuilder.withPayload(e).build()).toList());
		logger.debug("Sent {} events to queue {}", events.size(), INFERS_BATCH_POJO_QUEUE);

		assertThat(latchContainer.infersBatchPojoLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(pojoCollector.receivedBatchPojos).containsExactlyInAnyOrderElementsOf(events);
		// Interceptor runs before the listener, so payloads are already recorded by now
		interceptorPayloadTypeCollector.assertPayloadsForQueueContainsAll(INFERS_BATCH_POJO_QUEUE, events);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		ackCallbackPayloadTypeCollector.assertPayloadsForQueueContainsAll(INFERS_BATCH_POJO_QUEUE, events);
	}

	@Test
	void shouldInferNestedGenericType() throws Exception {
		CountDownLatch ackLatch = new CountDownLatch(1);
		ackCallbackPayloadTypeCollector.registerLatch(INFERS_NESTED_GENERIC_POJO_QUEUE, ackLatch);

		NestedGenericEvent event = new NestedGenericEvent(new TestEvent("nested-id", "nested-payload"), 42);
		sqsTemplate.send(INFERS_NESTED_GENERIC_POJO_QUEUE, event);
		logger.debug("Sent nested generic event to queue {}: {}", INFERS_NESTED_GENERIC_POJO_QUEUE, event);

		assertThat(latchContainer.infersNestedGenericLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(pojoCollector.receivedNestedGeneric).hasSize(1);
		assertThat(pojoCollector.receivedNestedGeneric.get(0)).isEqualTo(event);
		interceptorPayloadTypeCollector.assertPayloadsForQueueContains(INFERS_NESTED_GENERIC_POJO_QUEUE, event);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		ackCallbackPayloadTypeCollector.assertPayloadsForQueueContains(INFERS_NESTED_GENERIC_POJO_QUEUE, event);
	}

	@Test
	void shouldInferTypeWithAsyncListener() throws Exception {
		CountDownLatch ackLatch = new CountDownLatch(1);
		ackCallbackPayloadTypeCollector.registerLatch(ASYNC_LISTENER_QUEUE, ackLatch);

		TestEvent event = new TestEvent("async-id", "async-payload");
		sqsTemplate.send(ASYNC_LISTENER_QUEUE, event);
		logger.debug("Sent event to async listener queue {}: {}", ASYNC_LISTENER_QUEUE, event);

		assertThat(latchContainer.asyncListenerLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(pojoCollector.receivedAsyncPojos).hasSize(1);
		assertThat(pojoCollector.receivedAsyncPojos.get(0)).isEqualTo(event);
		interceptorPayloadTypeCollector.assertPayloadsForQueueContains(ASYNC_LISTENER_QUEUE, event);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		ackCallbackPayloadTypeCollector.assertPayloadsForQueueContains(ASYNC_LISTENER_QUEUE, event);
	}

	@Test
	void shouldInferTypeFromListOfMessages() throws Exception {
		CountDownLatch ackLatch = new CountDownLatch(3);
		ackCallbackPayloadTypeCollector.registerLatch(BATCH_MESSAGE_WRAPPER_QUEUE, ackLatch);

		List<TestEvent> events = List.of(new TestEvent("batch-msg-1", "payload-1"),
				new TestEvent("batch-msg-2", "payload-2"), new TestEvent("batch-msg-3", "payload-3"));

		sqsTemplate.sendMany(BATCH_MESSAGE_WRAPPER_QUEUE,
				events.stream().map(e -> MessageBuilder.withPayload(e).build()).toList());
		logger.debug("Sent {} events to queue {}", events.size(), BATCH_MESSAGE_WRAPPER_QUEUE);

		assertThat(latchContainer.batchMessageWrapperLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(pojoCollector.receivedBatchMessageWrapperPojos).containsExactlyInAnyOrderElementsOf(events);
		// Interceptor runs before the listener, so payloads are already recorded by now
		interceptorPayloadTypeCollector.assertPayloadsForQueueContainsAll(BATCH_MESSAGE_WRAPPER_QUEUE, events);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		ackCallbackPayloadTypeCollector.assertPayloadsForQueueContainsAll(BATCH_MESSAGE_WRAPPER_QUEUE, events);
	}

	@Test
	void shouldIgnoreTypeHeaderWhenInferenceActive() throws Exception {
		CountDownLatch ackLatch = new CountDownLatch(1);
		ackCallbackPayloadTypeCollector.registerLatch(IGNORES_TYPE_HEADER_QUEUE, ackLatch);

		TestEvent event = new TestEvent("header-test-id", "header-test-payload");
		// Send with __TypeId__ header pointing to a WRONG class - inference should ignore it
		sqsTemplate.send(to -> to.queue(IGNORES_TYPE_HEADER_QUEUE).payload(event)
				.header(SqsHeaders.SQS_DEFAULT_TYPE_HEADER, "com.example.NonExistentClass"));
		logger.debug("Sent event with wrong type header to queue {}: {}", IGNORES_TYPE_HEADER_QUEUE, event);

		assertThat(latchContainer.ignoresTypeHeaderLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(pojoCollector.receivedIgnoresTypeHeaderPojos).hasSize(1);
		assertThat(pojoCollector.receivedIgnoresTypeHeaderPojos.get(0)).isEqualTo(event);
		interceptorPayloadTypeCollector.assertPayloadsForQueueContains(IGNORES_TYPE_HEADER_QUEUE, event);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		ackCallbackPayloadTypeCollector.assertPayloadsForQueueContains(IGNORES_TYPE_HEADER_QUEUE, event);
	}

	@Test
	void shouldInferTypeWithExplicitPayloadAnnotation() throws Exception {
		CountDownLatch ackLatch = new CountDownLatch(1);
		ackCallbackPayloadTypeCollector.registerLatch(EXPLICIT_PAYLOAD_ANNOTATION_QUEUE, ackLatch);

		TestEvent event = new TestEvent("explicit-payload-id", "explicit-payload-data");
		sqsTemplate.send(EXPLICIT_PAYLOAD_ANNOTATION_QUEUE, event);
		logger.debug("Sent event to explicit payload annotation queue {}: {}", EXPLICIT_PAYLOAD_ANNOTATION_QUEUE,
				event);

		assertThat(latchContainer.explicitPayloadAnnotationLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(pojoCollector.receivedExplicitPayloadPojos).hasSize(1);
		assertThat(pojoCollector.receivedExplicitPayloadPojos.get(0)).isEqualTo(event);
		interceptorPayloadTypeCollector.assertPayloadsForQueueContains(EXPLICIT_PAYLOAD_ANNOTATION_QUEUE, event);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		ackCallbackPayloadTypeCollector.assertPayloadsForQueueContains(EXPLICIT_PAYLOAD_ANNOTATION_QUEUE, event);
	}

	@Test
	void shouldHandleStringPayloadWithoutDeserialization() throws Exception {
		CountDownLatch ackLatch = new CountDownLatch(1);
		ackCallbackPayloadTypeCollector.registerLatch(STRING_PAYLOAD_QUEUE, ackLatch);

		String rawJson = "{\"id\":\"raw-json-id\",\"payload\":\"raw-json-payload\"}";
		// Send raw string directly without type header
		sqsTemplate.send(to -> to.queue(STRING_PAYLOAD_QUEUE).payload(rawJson));
		logger.debug("Sent raw JSON string to queue {}: {}", STRING_PAYLOAD_QUEUE, rawJson);

		assertThat(latchContainer.stringPayloadLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(pojoCollector.receivedStringPayloads).hasSize(1);
		assertThat(pojoCollector.receivedStringPayloads.get(0)).isEqualTo(rawJson);
		interceptorPayloadTypeCollector.assertPayloadsForQueueContains(STRING_PAYLOAD_QUEUE, rawJson);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		ackCallbackPayloadTypeCollector.assertPayloadsForQueueContains(STRING_PAYLOAD_QUEUE, rawJson);
	}

	@Test
	void shouldUseCustomConverterWithTypeInference() throws Exception {
		CountDownLatch ackLatch = new CountDownLatch(1);
		ackCallbackPayloadTypeCollector.registerLatch(CUSTOM_CONVERTER_QUEUE, ackLatch);

		// Send snake_case JSON - only a custom ObjectMapper with SNAKE_CASE naming will parse this correctly
		String snakeCaseJson = "{\"event_id\":\"custom-converter-id\",\"event_payload\":\"custom-converter-data\"}";
		sqsTemplate.send(to -> to.queue(CUSTOM_CONVERTER_QUEUE).payload(snakeCaseJson));
		logger.debug("Sent snake_case JSON to custom converter queue {}: {}", CUSTOM_CONVERTER_QUEUE, snakeCaseJson);

		assertThat(latchContainer.customConverterLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(pojoCollector.receivedCustomConverterPojos).hasSize(1);
		SnakeCaseEvent received = pojoCollector.receivedCustomConverterPojos.get(0);
		SnakeCaseEvent expectedEvent = new SnakeCaseEvent("custom-converter-id", "custom-converter-data");
		assertThat(received.getEventId()).isEqualTo("custom-converter-id");
		assertThat(received.getEventPayload()).isEqualTo("custom-converter-data");
		interceptorPayloadTypeCollector.assertPayloadsForQueueContains(CUSTOM_CONVERTER_QUEUE, expectedEvent);
		assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
		ackCallbackPayloadTypeCollector.assertPayloadsForQueueContains(CUSTOM_CONVERTER_QUEUE, expectedEvent);
	}

	@Test
	void errorHandlerShouldReceiveDeserializedPojo() throws Exception {
		// Register a latch for the error handler to signal when it processes the message
		CountDownLatch errorHandlerLatch = new CountDownLatch(1);
		errorHandlerPayloadTypeCollector.registerLatch(ERROR_HANDLER_TEST_QUEUE, errorHandlerLatch);

		TestEvent event = new TestEvent("error-handler-test-id", "error-handler-test-payload");
		sqsTemplate.send(ERROR_HANDLER_TEST_QUEUE, event);
		logger.debug("Sent event to error handler test queue {}: {}", ERROR_HANDLER_TEST_QUEUE, event);

		// Wait for the error handler to process the message
		assertThat(errorHandlerLatch.await(10, TimeUnit.SECONDS)).isTrue();

		// Verify the error handler received the deserialized payload with correct field values
		errorHandlerPayloadTypeCollector.assertPayloadsForQueueContains(ERROR_HANDLER_TEST_QUEUE, event);
	}

	static class InfersSimplePojoListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		PojoCollector pojoCollector;

		@SqsListener(queueNames = INFERS_SIMPLE_POJO_QUEUE, id = "infers-simple-pojo")
		void listen(TestEvent event) {
			logger.debug("Received TestEvent: {}", event);
			pojoCollector.receivedSimplePojos.add(event);
			latchContainer.infersSimplePojoLatch.countDown();
		}

	}

	static class InfersPojoWithManyParametersListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		PojoCollector pojoCollector;

		@SqsListener(queueNames = INFERS_POJO_WITH_MANY_PARAMETERS_QUEUE, factory = MANUAL_ACK_FACTORY, id = "infers-pojo-with-many-params")
		void listen(TestEvent event, @Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName, MessageHeaders headers,
				Acknowledgement ack, Visibility visibility, QueueAttributes queueAttributes) {
			logger.debug("Received TestEvent: {} from queue: {}", event, queueName);
			assertThat(headers).isNotNull();
			assertThat(ack).isNotNull();
			assertThat(visibility).isNotNull();
			assertThat(queueAttributes).isNotNull();
			assertThat(queueName).isEqualTo(INFERS_POJO_WITH_MANY_PARAMETERS_QUEUE);

			pojoCollector.receivedPojoWithManyParams.add(event);
			ack.acknowledge();
			latchContainer.infersPojoWithManyParametersLatch.countDown();
		}

	}

	static class InfersBatchPojoListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		PojoCollector pojoCollector;

		@SqsListener(queueNames = INFERS_BATCH_POJO_QUEUE, factory = MANUAL_ACK_FACTORY, id = "infers-batch-pojo")
		void listen(List<TestEvent> events, BatchAcknowledgement<TestEvent> ack) {
			logger.debug("Received {} TestEvents", events.size());
			pojoCollector.receivedBatchPojos.addAll(events);
			ack.acknowledge();
			// Count down per event to handle multiple batch deliveries
			events.forEach(e -> latchContainer.infersBatchPojoLatch.countDown());
		}

	}

	static class InfersNestedGenericListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		PojoCollector pojoCollector;

		@SqsListener(queueNames = INFERS_NESTED_GENERIC_POJO_QUEUE, id = "infers-nested-generic")
		void listen(Message<NestedGenericEvent> message) {
			logger.debug("Received NestedGenericEvent: {}", message.getPayload());
			pojoCollector.receivedNestedGeneric.add(message.getPayload());
			latchContainer.infersNestedGenericLatch.countDown();
		}

	}

	static class AsyncListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		PojoCollector pojoCollector;

		@SqsListener(queueNames = ASYNC_LISTENER_QUEUE, id = "async-listener")
		CompletableFuture<Void> listen(TestEvent event) {
			logger.debug("Received TestEvent in async listener: {}", event);
			pojoCollector.receivedAsyncPojos.add(event);
			latchContainer.asyncListenerLatch.countDown();
			return CompletableFuture.completedFuture(null);
		}

	}

	static class BatchMessageWrapperListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		PojoCollector pojoCollector;

		@SqsListener(queueNames = BATCH_MESSAGE_WRAPPER_QUEUE, factory = MANUAL_ACK_FACTORY, id = "batch-message-wrapper")
		void listen(List<Message<TestEvent>> messages, BatchAcknowledgement<TestEvent> ack) {
			logger.debug("Received {} messages with Message wrapper", messages.size());
			messages.forEach(msg -> {
				pojoCollector.receivedBatchMessageWrapperPojos.add(msg.getPayload());
				latchContainer.batchMessageWrapperLatch.countDown();
			});
			ack.acknowledge();
		}

	}

	static class IgnoresTypeHeaderListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		PojoCollector pojoCollector;

		@SqsListener(queueNames = IGNORES_TYPE_HEADER_QUEUE, id = "ignores-type-header")
		void listen(TestEvent event) {
			logger.debug("Received TestEvent (type header should have been ignored): {}", event);
			pojoCollector.receivedIgnoresTypeHeaderPojos.add(event);
			latchContainer.ignoresTypeHeaderLatch.countDown();
		}

	}

	static class ExplicitPayloadAnnotationListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		PojoCollector pojoCollector;

		@SqsListener(queueNames = EXPLICIT_PAYLOAD_ANNOTATION_QUEUE, id = "explicit-payload-annotation")
		void listen(@Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName, @Payload TestEvent event) {
			logger.debug("Received TestEvent with explicit @Payload from queue {}: {}", queueName, event);
			assertThat(queueName).isEqualTo(EXPLICIT_PAYLOAD_ANNOTATION_QUEUE);
			pojoCollector.receivedExplicitPayloadPojos.add(event);
			latchContainer.explicitPayloadAnnotationLatch.countDown();
		}

	}

	static class StringPayloadListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		PojoCollector pojoCollector;

		@SqsListener(queueNames = STRING_PAYLOAD_QUEUE, id = "string-payload")
		void listen(String rawPayload) {
			logger.debug("Received raw string payload: {}", rawPayload);
			pojoCollector.receivedStringPayloads.add(rawPayload);
			latchContainer.stringPayloadLatch.countDown();
		}

	}

	static class CustomConverterListener {

		@Autowired
		LatchContainer latchContainer;

		@Autowired
		PojoCollector pojoCollector;

		@SqsListener(queueNames = CUSTOM_CONVERTER_QUEUE, factory = CUSTOM_CONVERTER_FACTORY, id = "custom-converter")
		void listen(SnakeCaseEvent event) {
			logger.debug("Received SnakeCaseEvent: {}", event);
			pojoCollector.receivedCustomConverterPojos.add(event);
			latchContainer.customConverterLatch.countDown();
		}

	}

	static class ErrorHandlerTestListener {

		@SqsListener(queueNames = ERROR_HANDLER_TEST_QUEUE, id = "error-handler-test")
		void listen(TestEvent event) {
			logger.debug("Received TestEvent in error handler test listener (will throw): {}", event);
			throw new RuntimeException("Expected exception to trigger error handler");
		}

	}

	static class PojoCollector {

		final List<TestEvent> receivedSimplePojos = Collections.synchronizedList(new ArrayList<>());

		final List<TestEvent> receivedPojoWithManyParams = Collections.synchronizedList(new ArrayList<>());

		final List<TestEvent> receivedBatchPojos = Collections.synchronizedList(new ArrayList<>());

		final List<NestedGenericEvent> receivedNestedGeneric = Collections.synchronizedList(new ArrayList<>());

		final List<TestEvent> receivedAsyncPojos = Collections.synchronizedList(new ArrayList<>());

		final List<TestEvent> receivedBatchMessageWrapperPojos = Collections.synchronizedList(new ArrayList<>());

		final List<TestEvent> receivedIgnoresTypeHeaderPojos = Collections.synchronizedList(new ArrayList<>());

		final List<TestEvent> receivedExplicitPayloadPojos = Collections.synchronizedList(new ArrayList<>());

		final List<String> receivedStringPayloads = Collections.synchronizedList(new ArrayList<>());

		final List<SnakeCaseEvent> receivedCustomConverterPojos = Collections.synchronizedList(new ArrayList<>());

	}

	/**
	 * Collects payloads received by the interceptor, keyed by queue name. Used to verify that deserialization happens
	 * at MessageSource level (early) rather than at argument resolver level (late).
	 */
	static class InterceptorPayloadTypeCollector {

		final java.util.Map<String, List<Object>> payloadsByQueue = new java.util.concurrent.ConcurrentHashMap<>();

		void recordPayload(String queueName, Object payload) {
			payloadsByQueue.computeIfAbsent(queueName, k -> Collections.synchronizedList(new ArrayList<>()))
					.add(payload);
		}

		void assertPayloadsForQueueContains(String queueName, Object expectedPayload) {
			List<Object> payloads = payloadsByQueue.get(queueName);
			assertThat(payloads).isNotNull().isNotEmpty().contains(expectedPayload);
		}

		void assertPayloadsForQueueContainsAll(String queueName, List<?> expectedPayloads) {
			List<Object> payloads = payloadsByQueue.get(queueName);
			assertThat(payloads).isNotNull().isNotEmpty().containsAll(expectedPayloads);
		}

	}

	/**
	 * Collects payloads received by the error handler, keyed by queue name. Used to verify that deserialization happens
	 * at MessageSource level (early) rather than at argument resolver level (late).
	 */
	static class ErrorHandlerPayloadTypeCollector {

		final java.util.Map<String, List<Object>> payloadsByQueue = new java.util.concurrent.ConcurrentHashMap<>();

		final java.util.Map<String, CountDownLatch> latches = new java.util.concurrent.ConcurrentHashMap<>();

		void registerLatch(String queueName, CountDownLatch latch) {
			latches.put(queueName, latch);
		}

		void recordPayload(String queueName, Object payload) {
			payloadsByQueue.computeIfAbsent(queueName, k -> Collections.synchronizedList(new ArrayList<>()))
					.add(payload);
			CountDownLatch latch = latches.get(queueName);
			if (latch != null) {
				latch.countDown();
			}
		}

		void assertPayloadsForQueueContains(String queueName, Object expectedPayload) {
			List<Object> payloads = payloadsByQueue.get(queueName);
			assertThat(payloads).isNotNull().isNotEmpty().contains(expectedPayload);
		}

	}

	/**
	 * Collects payloads received by the acknowledgement callback, keyed by queue name. Used to verify that
	 * deserialization happens at MessageSource level (early) rather than at argument resolver level (late).
	 */
	static class AckCallbackPayloadTypeCollector {

		final java.util.Map<String, List<Object>> payloadsByQueue = new java.util.concurrent.ConcurrentHashMap<>();

		final java.util.Map<String, CountDownLatch> latches = new java.util.concurrent.ConcurrentHashMap<>();

		void registerLatch(String queueName, CountDownLatch latch) {
			latches.put(queueName, latch);
		}

		void recordPayload(String queueName, Object payload) {
			payloadsByQueue.computeIfAbsent(queueName, k -> Collections.synchronizedList(new ArrayList<>()))
					.add(payload);
			CountDownLatch latch = latches.get(queueName);
			if (latch != null) {
				latch.countDown();
			}
		}

		void assertPayloadsForQueueContains(String queueName, Object expectedPayload) {
			List<Object> payloads = payloadsByQueue.get(queueName);
			assertThat(payloads).isNotNull().isNotEmpty().contains(expectedPayload);
		}

		void assertPayloadsForQueueContainsAll(String queueName, List<?> expectedPayloads) {
			List<Object> payloads = payloadsByQueue.get(queueName);
			assertThat(payloads).isNotNull().isNotEmpty().containsAll(expectedPayloads);
		}

	}

	static class LatchContainer {

		final CountDownLatch infersSimplePojoLatch = new CountDownLatch(1);

		final CountDownLatch infersPojoWithManyParametersLatch = new CountDownLatch(1);

		final CountDownLatch infersBatchPojoLatch = new CountDownLatch(3);

		final CountDownLatch infersNestedGenericLatch = new CountDownLatch(1);

		final CountDownLatch asyncListenerLatch = new CountDownLatch(1);

		final CountDownLatch batchMessageWrapperLatch = new CountDownLatch(3);

		final CountDownLatch ignoresTypeHeaderLatch = new CountDownLatch(1);

		final CountDownLatch explicitPayloadAnnotationLatch = new CountDownLatch(1);

		final CountDownLatch stringPayloadLatch = new CountDownLatch(1);

		final CountDownLatch customConverterLatch = new CountDownLatch(1);

	}

	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class SQSConfiguration {

		@Bean
		public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
				InterceptorPayloadTypeCollector interceptorPayloadTypeCollector,
				ErrorHandlerPayloadTypeCollector errorHandlerPayloadTypeCollector,
				AckCallbackPayloadTypeCollector ackCallbackPayloadTypeCollector) {
			return SqsMessageListenerContainerFactory.builder()
					.sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient)
					.configure(options -> options.maxDelayBetweenPolls(Duration.ofSeconds(1))
							.pollTimeout(Duration.ofSeconds(3)))
					.messageInterceptor(createPayloadTypeRecordingInterceptor(interceptorPayloadTypeCollector))
					.errorHandler(createErrorHandler(errorHandlerPayloadTypeCollector))
					.acknowledgementResultCallback(createAckCallback(ackCallbackPayloadTypeCollector)).build();
		}

		@Bean(name = MANUAL_ACK_FACTORY)
		public SqsMessageListenerContainerFactory<Object> manualAckFactory(
				InterceptorPayloadTypeCollector interceptorPayloadTypeCollector,
				ErrorHandlerPayloadTypeCollector errorHandlerPayloadTypeCollector,
				AckCallbackPayloadTypeCollector ackCallbackPayloadTypeCollector) {
			return SqsMessageListenerContainerFactory.builder()
					.sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient)
					.configure(options -> options.acknowledgementMode(AcknowledgementMode.MANUAL)
							.maxDelayBetweenPolls(Duration.ofSeconds(1)).pollTimeout(Duration.ofSeconds(3))
							.queueAttributeNames(Collections.singletonList(QueueAttributeName.QUEUE_ARN)))
					.messageInterceptor(createPayloadTypeRecordingInterceptor(interceptorPayloadTypeCollector))
					.errorHandler(createErrorHandler(errorHandlerPayloadTypeCollector))
					.acknowledgementResultCallback(createAckCallback(ackCallbackPayloadTypeCollector)).build();
		}

		@Bean(name = CUSTOM_CONVERTER_FACTORY)
		public SqsMessageListenerContainerFactory<Object> customConverterFactory(
				InterceptorPayloadTypeCollector interceptorPayloadTypeCollector,
				ErrorHandlerPayloadTypeCollector errorHandlerPayloadTypeCollector,
				AckCallbackPayloadTypeCollector ackCallbackPayloadTypeCollector) {
			// Create a custom ObjectMapper that uses SNAKE_CASE naming strategy
			ObjectMapper snakeCaseMapper = new ObjectMapper();
			snakeCaseMapper
					.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);

			// Create custom converter with the snake_case ObjectMapper
			SqsMessagingMessageConverter customConverter = new SqsMessagingMessageConverter();
			customConverter.setPayloadMessageConverter(new MappingJackson2MessageConverter() {
				{
					setObjectMapper(snakeCaseMapper);
					setSerializedPayloadClass(String.class);
					setStrictContentTypeMatch(false);
				}
			});

			return SqsMessageListenerContainerFactory.builder()
					.sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient)
					.configure(options -> options.maxDelayBetweenPolls(Duration.ofSeconds(1))
							.pollTimeout(Duration.ofSeconds(3)).messageConverter(customConverter))
					.messageInterceptor(createPayloadTypeRecordingInterceptor(interceptorPayloadTypeCollector))
					.errorHandler(createErrorHandler(errorHandlerPayloadTypeCollector))
					.acknowledgementResultCallback(createAckCallback(ackCallbackPayloadTypeCollector)).build();
		}

		private AsyncMessageInterceptor<Object> createPayloadTypeRecordingInterceptor(
				InterceptorPayloadTypeCollector collector) {
			return new AsyncMessageInterceptor<>() {
				@Override
				public CompletableFuture<Message<Object>> intercept(Message<Object> message) {
					String queueName = (String) message.getHeaders().get(SqsHeaders.SQS_QUEUE_NAME_HEADER);
					logger.debug("Interceptor received message from queue {} with payload type: {}", queueName,
							message.getPayload().getClass().getName());
					collector.recordPayload(queueName, message.getPayload());
					return CompletableFuture.completedFuture(message);
				}

				@Override
				public CompletableFuture<Collection<Message<Object>>> intercept(Collection<Message<Object>> messages) {
					messages.forEach(message -> {
						String queueName = (String) message.getHeaders().get(SqsHeaders.SQS_QUEUE_NAME_HEADER);
						logger.debug("Interceptor received batch message from queue {} with payload type: {}",
								queueName, message.getPayload().getClass().getName());
						collector.recordPayload(queueName, message.getPayload());
					});
					return CompletableFuture.completedFuture(messages);
				}
			};
		}

		private AsyncErrorHandler<Object> createErrorHandler(ErrorHandlerPayloadTypeCollector collector) {
			return new AsyncErrorHandler<>() {
				@Override
				public CompletableFuture<Void> handle(Message<Object> message, Throwable t) {
					String queueName = (String) message.getHeaders().get(SqsHeaders.SQS_QUEUE_NAME_HEADER);
					logger.debug("Error handler received message from queue {} with payload type: {}", queueName,
							message.getPayload().getClass().getName());
					collector.recordPayload(queueName, message.getPayload());
					return CompletableFuture.completedFuture(null);
				}

				@Override
				public CompletableFuture<Void> handle(Collection<Message<Object>> messages, Throwable t) {
					messages.forEach(message -> {
						String queueName = (String) message.getHeaders().get(SqsHeaders.SQS_QUEUE_NAME_HEADER);
						logger.debug("Error handler received batch message from queue {} with payload type: {}",
								queueName, message.getPayload().getClass().getName());
						collector.recordPayload(queueName, message.getPayload());
					});
					return CompletableFuture.completedFuture(null);
				}
			};
		}

		private AcknowledgementResultCallback<Object> createAckCallback(AckCallbackPayloadTypeCollector collector) {
			return new AcknowledgementResultCallback<>() {
				@Override
				public void onSuccess(Collection<Message<Object>> messages) {
					messages.forEach(message -> {
						String queueName = (String) message.getHeaders().get(SqsHeaders.SQS_QUEUE_NAME_HEADER);
						logger.debug("Ack callback received message from queue {} with payload type: {}", queueName,
								message.getPayload().getClass().getName());
						collector.recordPayload(queueName, message.getPayload());
					});
				}

				@Override
				public void onFailure(Collection<Message<Object>> messages, Throwable t) {
					// No need to record failures
				}
			};
		}

		@Bean
		InfersSimplePojoListener infersSimplePojoListener() {
			return new InfersSimplePojoListener();
		}

		@Bean
		InfersPojoWithManyParametersListener infersPojoWithManyParametersListener() {
			return new InfersPojoWithManyParametersListener();
		}

		@Bean
		InfersBatchPojoListener infersBatchPojoListener() {
			return new InfersBatchPojoListener();
		}

		@Bean
		InfersNestedGenericListener infersNestedGenericListener() {
			return new InfersNestedGenericListener();
		}

		@Bean
		AsyncListener asyncListener() {
			return new AsyncListener();
		}

		@Bean
		BatchMessageWrapperListener batchMessageWrapperListener() {
			return new BatchMessageWrapperListener();
		}

		@Bean
		IgnoresTypeHeaderListener ignoresTypeHeaderListener() {
			return new IgnoresTypeHeaderListener();
		}

		@Bean
		ExplicitPayloadAnnotationListener explicitPayloadAnnotationListener() {
			return new ExplicitPayloadAnnotationListener();
		}

		@Bean
		StringPayloadListener stringPayloadListener() {
			return new StringPayloadListener();
		}

		@Bean
		CustomConverterListener customConverterListener() {
			return new CustomConverterListener();
		}

		@Bean
		ErrorHandlerTestListener errorHandlerTestListener() {
			return new ErrorHandlerTestListener();
		}

		@Bean
		PojoCollector pojoCollector() {
			return new PojoCollector();
		}

		@Bean
		InterceptorPayloadTypeCollector interceptorPayloadTypeCollector() {
			return new InterceptorPayloadTypeCollector();
		}

		@Bean
		ErrorHandlerPayloadTypeCollector errorHandlerPayloadTypeCollector() {
			return new ErrorHandlerPayloadTypeCollector();
		}

		@Bean
		AckCallbackPayloadTypeCollector ackCallbackPayloadTypeCollector() {
			return new AckCallbackPayloadTypeCollector();
		}

		@Bean
		LatchContainer latchContainer() {
			return new LatchContainer();
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		SqsTemplate sqsTemplate() {
			return SqsTemplate.builder().sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient())
					.configureDefaultConverter(AbstractMessagingMessageConverter::doNotSendPayloadTypeHeader).build();
		}

	}

	static class TestEvent {

		private String id;

		private String payload;

		public TestEvent() {
		}

		public TestEvent(String id, String payload) {
			this.id = id;
			this.payload = payload;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getPayload() {
			return payload;
		}

		public void setPayload(String payload) {
			this.payload = payload;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			TestEvent testEvent = (TestEvent) o;
			return Objects.equals(id, testEvent.id) && Objects.equals(payload, testEvent.payload);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, payload);
		}

		@Override
		public String toString() {
			return "TestEvent{" + "id='" + id + '\'' + ", payload='" + payload + '\'' + '}';
		}

	}

	static class NestedGenericEvent {

		private TestEvent nestedEvent;

		private int count;

		public NestedGenericEvent() {
		}

		public NestedGenericEvent(TestEvent nestedEvent, int count) {
			this.nestedEvent = nestedEvent;
			this.count = count;
		}

		public TestEvent getNestedEvent() {
			return nestedEvent;
		}

		public void setNestedEvent(TestEvent nestedEvent) {
			this.nestedEvent = nestedEvent;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			NestedGenericEvent that = (NestedGenericEvent) o;
			return count == that.count && Objects.equals(nestedEvent, that.nestedEvent);
		}

		@Override
		public int hashCode() {
			return Objects.hash(nestedEvent, count);
		}

		@Override
		public String toString() {
			return "NestedGenericEvent{" + "nestedEvent=" + nestedEvent + ", count=" + count + '}';
		}

	}

	static class SnakeCaseEvent {

		private String eventId;

		private String eventPayload;

		public SnakeCaseEvent() {
		}

		public SnakeCaseEvent(String eventId, String eventPayload) {
			this.eventId = eventId;
			this.eventPayload = eventPayload;
		}

		public String getEventId() {
			return eventId;
		}

		public void setEventId(String eventId) {
			this.eventId = eventId;
		}

		public String getEventPayload() {
			return eventPayload;
		}

		public void setEventPayload(String eventPayload) {
			this.eventPayload = eventPayload;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			SnakeCaseEvent that = (SnakeCaseEvent) o;
			return Objects.equals(eventId, that.eventId) && Objects.equals(eventPayload, that.eventPayload);
		}

		@Override
		public int hashCode() {
			return Objects.hash(eventId, eventPayload);
		}

		@Override
		public String toString() {
			return "SnakeCaseEvent{" + "eventId='" + eventId + '\'' + ", eventPayload='" + eventPayload + '\'' + '}';
		}

	}

}
