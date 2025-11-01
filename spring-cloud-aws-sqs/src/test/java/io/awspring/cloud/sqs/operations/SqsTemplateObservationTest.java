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
package io.awspring.cloud.sqs.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.support.observation.SqsTemplateObservation;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * Tests for {@link SqsTemplate} observation support.
 *
 * @author Tomaz Fernandes
 * @author Hyunggeol Lee
 */
class SqsTemplateObservationTest {

	private SqsAsyncClient mockSqsAsyncClient;
	private TestObservationRegistry observationRegistry;
	private SqsTemplate sqsTemplate;
	private String queueName;

	@BeforeEach
	void setUp() {
		mockSqsAsyncClient = mock(SqsAsyncClient.class);
		observationRegistry = TestObservationRegistry.create();
		queueName = "test-queue";

		given(mockSqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).willReturn(
				CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl(queueName).build()));

		UUID messageId = UUID.randomUUID();
		given(mockSqsAsyncClient.sendMessage(any(SendMessageRequest.class))).willReturn(CompletableFuture
				.completedFuture(SendMessageResponse.builder().messageId(messageId.toString()).build()));

		sqsTemplate = SqsTemplate.builder().sqsAsyncClient(mockSqsAsyncClient)
				.configure(options -> options.observationRegistry(observationRegistry)).build();
	}

	@Test
	void shouldTrackObservationWhenSendingMessage() {
		// given
		String payload = "test-payload";

		// when
		sqsTemplate.send(queueName, payload);

		// then
		TestObservationRegistryAssert.then(observationRegistry).hasNumberOfObservationsEqualTo(1)
				.hasSingleObservationThat().hasNameEqualTo("spring.aws.sqs.template")
				.hasLowCardinalityKeyValue("messaging.system", "sqs")
				.hasLowCardinalityKeyValue("messaging.operation", "publish")
				.hasLowCardinalityKeyValue("messaging.destination.name", queueName)
				.hasLowCardinalityKeyValue("messaging.destination.kind", "queue");
	}

	@Test
	void shouldSupportCustomObservationConvention() {
		// given
		SqsTemplateObservation.Convention customConvention = mock(SqsTemplateObservation.Convention.class);
		given(customConvention.supportsContext(any())).willReturn(true);

		String lowCardinalityCustomKeyName = "custom.lowCardinality.key";
		String lowCardinalityCustomValue = "custom-lowCardinality-value";
		String highCardinalityCustomKeyName = "custom.highCardinality.key";
		String highCardinalityCustomValue = "custom-highCardinality-value";
		given(customConvention.getLowCardinalityKeyValues(any()))
				.willReturn(io.micrometer.common.KeyValues.of(lowCardinalityCustomKeyName, lowCardinalityCustomValue));
		given(customConvention.getHighCardinalityKeyValues(any())).willReturn(
				io.micrometer.common.KeyValues.of(highCardinalityCustomKeyName, highCardinalityCustomValue));

		TestObservationRegistry customRegistry = TestObservationRegistry.create();

		SqsTemplate templateWithCustomConvention = SqsTemplate.builder().sqsAsyncClient(mockSqsAsyncClient)
				.configure(
						options -> options.observationRegistry(customRegistry).observationConvention(customConvention))
				.build();

		// when
		templateWithCustomConvention.send(queueName, "test-payload");

		// then
		TestObservationRegistryAssert.then(customRegistry).hasNumberOfObservationsEqualTo(1).hasSingleObservationThat()
				.hasLowCardinalityKeyValue(lowCardinalityCustomKeyName, lowCardinalityCustomValue)
				.hasHighCardinalityKeyValue(highCardinalityCustomKeyName, highCardinalityCustomValue);
	}

	@Test
	void shouldAddMessageIdToObservation() {
		// given
		String payload = "test-payload";
		UUID messageId = UUID.randomUUID();

		given(mockSqsAsyncClient.sendMessage(any(SendMessageRequest.class))).willReturn(CompletableFuture
				.completedFuture(SendMessageResponse.builder().messageId(messageId.toString()).build()));

		// when
		sqsTemplate.send(queueName, payload);

		// then
		TestObservationRegistryAssert.then(observationRegistry).hasNumberOfObservationsEqualTo(1)
				.hasSingleObservationThat().hasHighCardinalityKeyValue("messaging.message.id", messageId.toString());
	}

	@Test
	void shouldNotCreateObservationsWithNoopRegistry() {
		// given
		SqsTemplate defaultTemplate = SqsTemplate.builder().sqsAsyncClient(mockSqsAsyncClient).build();

		// when - sending with a noop registry
		defaultTemplate.send(queueName, "test-payload");

		// then - the message is processed without errors
	}

	@Test
	void shouldCaptureErrorsInObservation() {
		// given
		String payload = "test-payload";
		RuntimeException testException = new RuntimeException(
				"Expected test exception from shouldCaptureErrorsInObservation");

		given(mockSqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
				.willReturn(CompletableFuture.failedFuture(testException));

		try {
			sqsTemplate.send(queueName, payload);
		}
		catch (Exception e) {
			// Expected exception
		}

		// then
		TestObservationRegistryAssert.then(observationRegistry).hasNumberOfObservationsEqualTo(1)
				.hasSingleObservationThat().hasError().assertThatError().isInstanceOf(RuntimeException.class);
	}

	@Test
	void shouldSupportCustomKeyValuesInActiveSending() {
		// given
		String payload = "test-payload";
		UUID messageId = UUID.randomUUID();

		// Mock responses for the SQS client
		given(mockSqsAsyncClient.sendMessage(any(SendMessageRequest.class))).willReturn(CompletableFuture
				.completedFuture(SendMessageResponse.builder().messageId(messageId.toString()).build()));

		given(mockSqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).willReturn(
				CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl(queueName).build()));

		// Create registry to capture observations
		TestObservationRegistry customRegistry = TestObservationRegistry.create();

		// Create a custom convention extending the default one
		SqsTemplateObservation.DefaultConvention customConvention = new SqsTemplateObservation.DefaultConvention() {
			@Override
			protected KeyValues getCustomHighCardinalityKeyValues(SqsTemplateObservation.Context context) {
				return KeyValues.of("order.id", "order-123");
			}

			@Override
			protected KeyValues getCustomLowCardinalityKeyValues(SqsTemplateObservation.Context context) {
				return KeyValues.of("order.type", "electronics");
			}
		};

		// Create message with FIFO headers
		Message<String> message = MessageBuilder.withPayload(payload)
				.setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, "group-xyz")
				.setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, "dedup-abc").build();

		// Create a template with the custom convention
		SqsTemplate templateWithCustomConvention = SqsTemplate.builder().sqsAsyncClient(mockSqsAsyncClient)
				.configure(
						options -> options.observationRegistry(customRegistry).observationConvention(customConvention))
				.build();

		// when - send the message
		templateWithCustomConvention.send(queueName, message);

		// then - verify the observations contain the expected values
		TestObservationRegistryAssert.then(customRegistry).hasNumberOfObservationsEqualTo(1).hasSingleObservationThat()
				// Custom values
				.hasLowCardinalityKeyValue("order.type", "electronics")
				// Default values
				.hasLowCardinalityKeyValue("messaging.system", "sqs")
				.hasLowCardinalityKeyValue("messaging.operation", "publish")
				.hasLowCardinalityKeyValue("messaging.destination.name", queueName)
				.hasLowCardinalityKeyValue("messaging.destination.kind", "queue")
				.hasHighCardinalityKeyValue("messaging.message.id", messageId.toString());
	}

	@Test
	void shouldCreateSeparateObservationsForRetryAfterCacheFailure() {
		// Given - First attempt will fail
		String payload = "test-payload";
		RuntimeException firstException = new RuntimeException("First attempt - queue not found");

		given(mockSqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class)))
			.willReturn(CompletableFuture.failedFuture(firstException));

		// When - First attempt (failure)
		try {
			sqsTemplate.send(queueName, payload);
		}
		catch (Exception e) {
			// Expected
		}

		// Then - Verify first observation has error
		TestObservationRegistryAssert.then(observationRegistry).hasNumberOfObservationsEqualTo(1)
			.hasSingleObservationThat().hasError();

		// Given - Second attempt will succeed (cache was cleared)
		UUID messageId = UUID.randomUUID();
		given(mockSqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).willReturn(
			CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl(queueName).build()));

		given(mockSqsAsyncClient.sendMessage(any(SendMessageRequest.class))).willReturn(CompletableFuture
			.completedFuture(SendMessageResponse.builder().messageId(messageId.toString()).build()));

		// When - Second attempt (success)
		SendResult<String> result = sqsTemplate.send(queueName, payload);

		// Then - Two separate observations should be created
		TestObservationRegistryAssert.then(observationRegistry).hasNumberOfObservationsEqualTo(2);

		assertThat(result).isNotNull();
	}
}
