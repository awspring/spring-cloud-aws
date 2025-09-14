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
package io.awspring.cloud.sqs.integration;

import static io.awspring.cloud.sqs.integration.SnsNotificationIntegrationTests.SNS_NOTIFICATION_JSON_QUEUE_NAME;
import static io.awspring.cloud.sqs.integration.SnsNotificationIntegrationTests.SNS_NOTIFICATION_STRING_QUEUE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsListenerConfigurer;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.converter.SnsNotification;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Integration tests for SNS notifications in SQS.
 *
 * @author Damien Chomat
 */
@SpringBootTest
@TestPropertySource(properties = { "sns.notification.string.queue.name=" + SNS_NOTIFICATION_STRING_QUEUE_NAME,
		"sns.notification.json.queue.name=" + SNS_NOTIFICATION_JSON_QUEUE_NAME, })
class SnsNotificationIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(SnsNotificationIntegrationTests.class);
	protected static final String SNS_NOTIFICATION_QUEUE_BASE = "sns_notification_test_queue";
	protected static final String SNS_NOTIFICATION_STRING_QUEUE_NAME = SNS_NOTIFICATION_QUEUE_BASE + "_string";
	protected static final String SNS_NOTIFICATION_JSON_QUEUE_NAME = SNS_NOTIFICATION_QUEUE_BASE + "_json";

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	SqsTemplate sqsTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		createQueue(client, SNS_NOTIFICATION_STRING_QUEUE_NAME).join();
		createQueue(client, SNS_NOTIFICATION_JSON_QUEUE_NAME).join();
	}

	@Test
	void shouldReceiveSnsNotificationWithRequiredFieldsOnly() {
		String type = "Notification";
		String messageId = "message-id-required-only";
		String topicArn = "topic-arn";
		String messageContent = "test-message";
		String timestamp = "2023-01-01T00:00:00Z";

		// Create SNS notification with String payload - required fields only
		String snsJson = """
				{
					"Type": "%s",
					"MessageId": "%s",
					"TopicArn": "%s",
					"Message": "%s",
					"Timestamp": "%s"
				}""".formatted(type, messageId, topicArn, messageContent, timestamp);

		sqsTemplate.send(SNS_NOTIFICATION_STRING_QUEUE_NAME, snsJson);
		await().atMost(Duration.ofSeconds(10)).untilAsserted(
				() -> assertThat(latchContainer.requiredFieldsLatch.await(10, TimeUnit.SECONDS)).isTrue());

		SnsNotification<String> receivedNotification = (SnsNotification<String>) latchContainer
				.getNotification(messageId);
		assertThat(receivedNotification).isNotNull();
		assertThat(receivedNotification.getMessageId()).isEqualTo(messageId);
		assertThat(receivedNotification.getTopicArn()).isEqualTo(topicArn);
		assertThat(receivedNotification.getMessage()).isEqualTo(messageContent);
		assertThat(receivedNotification.getTimestamp()).isEqualTo(timestamp);
		assertThat(receivedNotification.getType()).isEqualTo(type);

		// Optional fields should be empty
		assertThat(receivedNotification.getSubject()).isEmpty();
		assertThat(receivedNotification.getSequenceNumber()).isEmpty();
		assertThat(receivedNotification.getUnsubscribeUrl()).isEmpty();
		assertThat(receivedNotification.getMessageAttributes()).isEmpty();
		assertThat(receivedNotification.getSignature()).isEmpty();
		assertThat(receivedNotification.getSignatureVersion()).isEmpty();
		assertThat(receivedNotification.getSigningCertURL()).isEmpty();
	}

	@Test
	void shouldReceiveSnsNotificationWithAllFields() {
		String type = "Notification";
		String messageId = "message-id-all-fields";
		String sequenceNumber = "10000000000000003000";
		String topicArn = "topic-arn";
		String messageContent = "test-message";
		String timestamp = "2023-01-01T00:00:00Z";
		String unsubscribeUrl = "https://sns.region.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:region:accountId:topicName:uuid";
		String subject = "subject";
		String signatureVersion = "1";
		String signature = "signature-value";
		String signingCertURL = "https://sns.region.amazonaws.com/SimpleNotificationService-certificate.pem";

		// Create SNS notification with String payload - all fields (required and optional)
		String snsJson = """
				{
				  "Type": "%s",
				  "MessageId": "%s",
				  "SequenceNumber": "%s",
				  "TopicArn": "%s",
				  "Message": "%s",
				  "Timestamp": "%s",
				  "UnsubscribeURL": "%s",
				  "Subject": "%s",
				  "SignatureVersion": "%s",
				  "Signature": "%s",
				  "SigningCertURL": "%s",
				  "MessageAttributes": {
					"key": {
					  "Type": "String",
					  "Value": "value"
					}
				  }
				}""".formatted(type, messageId, sequenceNumber, topicArn, messageContent, timestamp, unsubscribeUrl,
				subject, signatureVersion, signature, signingCertURL);

		sqsTemplate.send(SNS_NOTIFICATION_STRING_QUEUE_NAME, snsJson);
		await().atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> assertThat(latchContainer.allFieldsLatch.await(10, TimeUnit.SECONDS)).isTrue());

		SnsNotification<String> receivedNotification = (SnsNotification<String>) latchContainer
				.getNotification(messageId);
		assertThat(receivedNotification).isNotNull();

		// Required fields
		assertThat(receivedNotification.getMessageId()).isEqualTo(messageId);
		assertThat(receivedNotification.getTopicArn()).isEqualTo(topicArn);
		assertThat(receivedNotification.getMessage()).isEqualTo(messageContent);
		assertThat(receivedNotification.getTimestamp()).isEqualTo(timestamp);
		assertThat(receivedNotification.getType()).isEqualTo(type);

		// Optional fields
		assertThat(receivedNotification.getSubject()).isEqualTo(Optional.of(subject));
		assertThat(receivedNotification.getSequenceNumber()).isEqualTo(Optional.of(sequenceNumber));
		assertThat(receivedNotification.getUnsubscribeUrl()).isEqualTo(Optional.of(unsubscribeUrl));
		assertThat(receivedNotification.getSignatureVersion()).isEqualTo(Optional.of(signatureVersion));
		assertThat(receivedNotification.getSignature()).isEqualTo(Optional.of(signature));
		assertThat(receivedNotification.getSigningCertURL()).isEqualTo(Optional.of(signingCertURL));
		assertThat(receivedNotification.getMessageAttributes()).hasSize(1);
		assertThat(receivedNotification.getMessageAttributes().get("key").getType()).isEqualTo("String");
		assertThat(receivedNotification.getMessageAttributes().get("key").getValue()).isEqualTo("value");
	}

	@Test
	void shouldReceiveSnsNotificationWithJsonPayload() throws Exception {
		String type = "Notification";
		String messageId = "006cae73-2988-5eee-b877-ce9349bbb029";
		String sequenceNumber = "10000000000000003000";
		String topicArn = "topic-arn-json";
		TestPayload payload = new TestPayload("test", 123);
		String messageContent = objectMapper.writeValueAsString(payload);
		Instant timestamp = Instant.parse("2023-01-01T00:00:00Z");
		String unsubscribeUrl = "https://sns.region.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:region:accountId:topicName:uuid";
		String subject = "subject-json";

		// Create SNS notification with JSON payload
		String snsJson = """
				{
				  "Type": "%s",
				  "MessageId": "%s",
				  "SequenceNumber": "%s",
				  "TopicArn": "%s",
				  "Message": "%s",
				  "Timestamp": "%s",
				  "UnsubscribeURL": "%s",
				  "Subject": "%s",
				  "MessageAttributes": {
					"key": {
					  "Type": "String",
					  "Value": "value"
					}
				  }
				}""".formatted(type, messageId, sequenceNumber, topicArn, messageContent.replace("\"", "\\\""),
				timestamp, unsubscribeUrl, subject);

		sqsTemplate.send(SNS_NOTIFICATION_JSON_QUEUE_NAME, snsJson);
		await().atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> assertThat(latchContainer.jsonPayloadLatch.await(10, TimeUnit.SECONDS)).isTrue());

		SnsNotification<String> receivedNotification = (SnsNotification<String>) latchContainer
				.getNotification(messageId);
		assertThat(receivedNotification).isNotNull();
		assertThat(receivedNotification.getMessageId()).isEqualTo(messageId);
		assertThat(receivedNotification.getTopicArn()).isEqualTo(topicArn);
		assertThat(receivedNotification.getSubject()).isEqualTo(Optional.of(subject));

		String jsonMessage = receivedNotification.getMessage();
		TestPayload resultPayload = objectMapper.readValue(jsonMessage, TestPayload.class);
		assertThat(resultPayload.getName()).isEqualTo(payload.getName());
		assertThat(resultPayload.getValue()).isEqualTo(payload.getValue());

		assertThat(receivedNotification.getMessageAttributes()).hasSize(1);
		assertThat(receivedNotification.getMessageAttributes().get("key").getType()).isEqualTo("String");
		assertThat(receivedNotification.getMessageAttributes().get("key").getValue()).isEqualTo("value");
	}

	static class SnsNotificationListener {
		private final LatchContainer latchContainer;

		SnsNotificationListener(LatchContainer latchContainer) {
			this.latchContainer = latchContainer;
		}

		@SqsListener("${sns.notification.string.queue.name}")
		public void listenString(SnsNotification<String> notification) {
			LOGGER.info("Received String SNS notification: {}", notification);
			String messageId = notification.getMessageId();
			latchContainer.storeNotification(messageId, notification);

			// Count down the appropriate latch based on the message ID
			if (messageId.equals("message-id-required-only")) {
				LOGGER.info("Counting down requiredFieldsLatch for message ID: {}", messageId);
				latchContainer.requiredFieldsLatch.countDown();
			}
			else if (messageId.equals("message-id-all-fields")) {
				LOGGER.info("Counting down allFieldsLatch for message ID: {}", messageId);
				latchContainer.allFieldsLatch.countDown();
			}
		}

		@SqsListener("${sns.notification.json.queue.name}")
		public void listenJson(SnsNotification<TestPayload> notification) {
			LOGGER.info("Received JSON SNS notification: {}", notification);
			String messageId = notification.getMessageId();
			latchContainer.storeNotification(messageId, notification);
			latchContainer.jsonPayloadLatch.countDown();
		}
	}

	static class LatchContainer {
		// Latches for the test methods
		public final CountDownLatch stringPayloadLatch = new CountDownLatch(1); // Legacy latch, kept for backward
																				// compatibility
		public final CountDownLatch jsonPayloadLatch = new CountDownLatch(1);
		public final CountDownLatch requiredFieldsLatch = new CountDownLatch(1);
		public final CountDownLatch allFieldsLatch = new CountDownLatch(1);

		// Keep the notification storage by message ID for verification
		private final ConcurrentHashMap<String, SnsNotification<?>> receivedSnsNotifications = new ConcurrentHashMap<>();

		public void storeNotification(String messageId, SnsNotification<?> notification) {
			receivedSnsNotifications.put(messageId, notification);
		}

		public SnsNotification<?> getNotification(String messageId) {
			return receivedSnsNotifications.get(messageId);
		}
	}

	static class TestPayload {
		private String name;
		private int value;

		public TestPayload() {
			// Deserialization
		}

		public TestPayload(String name, int value) {
			// Serialization
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}
	}

	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class SQSConfiguration {

		@Bean
		public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory() {
			return SqsMessageListenerContainerFactory.builder()
					.sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient)
					.acknowledgementResultCallback(getAcknowledgementResultCallback())
					.configure(options -> options.maxDelayBetweenPolls(Duration.ofSeconds(5))
							.queueAttributeNames(Collections.singletonList(QueueAttributeName.QUEUE_ARN))
							.pollTimeout(Duration.ofSeconds(5)))
					.build();
		}

		private AcknowledgementResultCallback<Object> getAcknowledgementResultCallback() {
			return new AcknowledgementResultCallback<>() {
			};
		}

		@Bean
		SnsNotificationListener snsNotificationListener(LatchContainer latchContainer) {
			return new SnsNotificationListener(latchContainer);
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
		SqsListenerConfigurer sqsListenerConfigurer(ObjectMapper objectMapper) {
			return registrar -> registrar.setObjectMapper(objectMapper);
		}

		@Bean
		SqsTemplate sqsTemplate() {
			return SqsTemplate.builder().sqsAsyncClient(createAsyncClient()).build();
		}
	}
}
