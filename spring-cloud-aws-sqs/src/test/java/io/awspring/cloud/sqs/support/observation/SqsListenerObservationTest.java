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
package io.awspring.cloud.sqs.support.observation;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.common.docs.KeyName;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Tests for {@link SqsListenerObservation}.
 *
 * @author Tomaz Fernandes
 */
class SqsListenerObservationTest {

	private SqsListenerObservation.SqsSpecifics sqsSpecifics;
	private String queueName;

	@BeforeEach
	void setUp() {
		sqsSpecifics = new SqsListenerObservation.SqsSpecifics();
		queueName = "test-queue";
	}

	@Test
	void shouldCreateContextWithQueueNameAndMessageId() {
		// given
		Message<String> message = MessageBuilder.withPayload("test-payload")
				.setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, queueName).build();

		// when
		SqsListenerObservation.Context context = sqsSpecifics.createContext(message);

		// then
		assertThat(context.getSourceName()).isEqualTo(queueName);
		assertThat(context.getMessageId()).isEqualTo(message.getHeaders().getId().toString());
		assertThat(context.getRemoteServiceName()).isEqualTo("AWS SQS");
	}

	@Test
	void shouldHandleFifoQueueHeaders() {
		// given
		String groupId = "test-group";
		String deduplicationId = "test-dedup-id";

		Message<String> message = MessageBuilder.withPayload("test-payload")
				.setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, queueName)
				.setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, groupId)
				.setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, deduplicationId)
				.build();

		// when
		SqsListenerObservation.Context context = sqsSpecifics.createContext(message);

		// then
		assertThat(context.getMessageGroupId()).isEqualTo(groupId);
		assertThat(context.getMessageDeduplicationId()).isEqualTo(deduplicationId);

		// Verify convention adds these as high cardinality keys
		SqsListenerObservation.DefaultConvention convention = new SqsListenerObservation.DefaultConvention();
		KeyValues highCardinalityKeys = convention.getHighCardinalityKeyValues(context);

		assertThat(highCardinalityKeys.stream()
				.anyMatch(keyValue -> keyValue.getKey().equals("messaging.message.message-group.id")
						&& keyValue.getValue().equals(groupId)))
				.isTrue();

		assertThat(highCardinalityKeys.stream()
				.anyMatch(keyValue -> keyValue.getKey().equals("messaging.message.message-deduplication.id")
						&& keyValue.getValue().equals(deduplicationId)))
				.isTrue();
	}

	@Test
	void shouldReturnEmptyValuesForNonFifoQueue() {
		// given
		Message<String> message = MessageBuilder.withPayload("test-payload")
				.setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, queueName).build();

		// when
		SqsListenerObservation.Context context = sqsSpecifics.createContext(message);

		// then
		assertThat(context.getMessageGroupId()).isEmpty();
		assertThat(context.getMessageDeduplicationId()).isEmpty();

		// Verify convention doesn't add these as high cardinality keys
		SqsListenerObservation.DefaultConvention convention = new SqsListenerObservation.DefaultConvention();
		KeyValues highCardinalityKeys = convention.getSpecificHighCardinalityKeyValues(context);

		assertThat(highCardinalityKeys.stream().count()).isEqualTo(0);
	}

	@Test
	void shouldProvideCorrectConventionAndDocumentation() {
		// when
		var convention = sqsSpecifics.getDefaultConvention();
		var documentation = sqsSpecifics.getDocumentation();

		// then
		assertThat(convention).isInstanceOf(SqsListenerObservation.DefaultConvention.class);
		assertThat(documentation).isInstanceOf(SqsListenerObservation.Documentation.class);

		// Verify convention values
		SqsListenerObservation.DefaultConvention defaultConvention = (SqsListenerObservation.DefaultConvention) convention;
		assertThat(defaultConvention.getMessagingSystem()).isEqualTo("sqs");
		assertThat(defaultConvention.getSourceKind()).isEqualTo("queue");
	}

	@Test
	void shouldSupportCustomKeyValues() {
		// given
		Message<String> message = MessageBuilder.withPayload("test-payload")
				.setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, queueName).setHeader("order-id", "12345")
				.setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, "abcd")
				.setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, "efgh").build();

		SqsListenerObservation.Context context = sqsSpecifics.createContext(message);

		// when
		SqsListenerObservation.DefaultConvention customConvention = new SqsListenerObservation.DefaultConvention() {
			@Override
			protected KeyValues getCustomHighCardinalityKeyValues(SqsListenerObservation.Context context) {
				return KeyValues.of("order.id", "12345");
			}

			@Override
			protected KeyValues getCustomLowCardinalityKeyValues(SqsListenerObservation.Context context) {
				return KeyValues.of("order.type", "custom order");
			}
		};

		// then
		KeyValues highCardinalityKeys = customConvention.getHighCardinalityKeyValues(context);

		// Verify custom key is included
		assertThat(highCardinalityKeys.stream()
				.anyMatch(keyValue -> keyValue.getKey().equals("order.id") && keyValue.getValue().equals("12345")))
				.isTrue();

		// Verify it still includes default keys
		Set<String> allHighCardinalityKeys = highCardinalityKeys.stream().map(KeyValue::getKey)
				.collect(Collectors.toSet());

		assertThat(
				Arrays.stream(SqsListenerObservation.Documentation.HighCardinalityTags.values()).map(KeyName::asString))
				.allMatch(allHighCardinalityKeys::contains);
		assertThat(Arrays.stream(AbstractListenerObservation.Documentation.HighCardinalityTags.values())
				.map(KeyName::asString)).allMatch(allHighCardinalityKeys::contains);

		KeyValues lowCardinalityKeyValues = customConvention.getLowCardinalityKeyValues(context);

		// Verify custom value is included
		assertThat(lowCardinalityKeyValues.stream().anyMatch(
				keyValue -> keyValue.getKey().equals("order.type") && keyValue.getValue().equals("custom order")))
				.isTrue();

		// Verify it still includes default keys
		Set<String> allLowCardinalityKeys = lowCardinalityKeyValues.stream().map(KeyValue::getKey)
				.collect(Collectors.toSet());

		assertThat(Arrays.stream(AbstractListenerObservation.Documentation.LowCardinalityTags.values())
				.map(KeyName::asString)).allMatch(allLowCardinalityKeys::contains);

	}
}
