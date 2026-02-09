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
package io.awspring.cloud.sns.core.async;

import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER;
import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_GROUP_ID_HEADER;
import static io.awspring.cloud.sns.core.SnsHeaders.NOTIFICATION_SUBJECT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.awspring.cloud.sns.Person;
import io.awspring.cloud.sns.core.SnsNotification;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Integration tests for {@link SnsAsyncTemplate} using SNS → SQS subscriptions.
 *
 * @author Matej Nedic
 * @since 4.0.1
 */
@Testcontainers
class SnsAsyncTemplateIntegrationTest {

	private static final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:4.4.0"));

	private static SnsAsyncClient snsAsyncClient;
	private static SqsClient sqsClient;
	private static SnsAsyncTemplate snsAsyncTemplate;
	private static String standardQueueUrl;
	private static String standardTopicArn;
	private static String fifoQueueUrl;
	private static String fifoTopicArn;

	@BeforeAll
	static void setUp() throws Exception {
		StaticCredentialsProvider credentials = StaticCredentialsProvider
			.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));

		snsAsyncClient = SnsAsyncClient.builder()
			.endpointOverride(localstack.getEndpoint())
			.region(Region.of(localstack.getRegion()))
			.credentialsProvider(credentials).build();

		sqsClient = SqsClient.builder()
			.endpointOverride(localstack.getEndpoint())
			.region(Region.of(localstack.getRegion()))
			.credentialsProvider(credentials).build();

		standardQueueUrl = sqsClient.createQueue(r -> r.queueName("async-standard-queue")).queueUrl();
		String standardQueueArn = sqsClient
			.getQueueAttributes(r -> r.queueUrl(standardQueueUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
			.attributes().get(QueueAttributeName.QUEUE_ARN);
		standardTopicArn = snsAsyncClient
			.createTopic(CreateTopicRequest.builder().name("async-standard-topic").build())
			.get().topicArn();
		snsAsyncClient.subscribe(r -> r.topicArn(standardTopicArn).protocol("sqs").endpoint(standardQueueArn)).get();


		fifoQueueUrl = sqsClient
			.createQueue(r -> r.queueName("async-fifo-queue.fifo")
				.attributes(Map.of(QueueAttributeName.FIFO_QUEUE, "true")))
			.queueUrl();
		String fifoQueueArn = sqsClient
			.getQueueAttributes(r -> r.queueUrl(fifoQueueUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
			.attributes().get(QueueAttributeName.QUEUE_ARN);
		fifoTopicArn = snsAsyncClient
			.createTopic(CreateTopicRequest.builder().name("async-fifo-topic.fifo")
				.attributes(Map.of("FifoTopic", "true", "ContentBasedDeduplication", "true")).build())
			.get().topicArn();
		snsAsyncClient.subscribe(r -> r.topicArn(fifoTopicArn).protocol("sqs").endpoint(fifoQueueArn)).get();

		JacksonJsonMessageConverter jackson = new JacksonJsonMessageConverter();
		jackson.setSerializedPayloadClass(String.class);
		snsAsyncTemplate = new SnsAsyncTemplate(snsAsyncClient,
			new DefaultSnsPublishMessageConverter(jackson));
	}

	private static List<software.amazon.awssdk.services.sqs.model.Message> receiveAll(String queueUrl) {
		List<software.amazon.awssdk.services.sqs.model.Message> messages = new ArrayList<>();
		await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(500)).until(() -> {
			var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl)
				.maxNumberOfMessages(10).waitTimeSeconds(2).messageAttributeNames("All").build());
			if (response.hasMessages() && !response.messages().isEmpty()) {
				messages.addAll(response.messages());
			}
			return !messages.isEmpty();
		});
		return messages;
	}

	@Nested
	class StandardTopicTests {

		@AfterEach
		void purgeQueue() {
			sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(standardQueueUrl).build());
		}

		@Test
		void convertAndSendWithHeaders() throws Exception {
			SnsResult<String> result = snsAsyncTemplate.convertAndSend(standardTopicArn, "async message",
				Map.of("custom-header", "custom-value")).get();

			assertThat(result.messageId()).isNotNull();
			assertThat(result.sequenceNumber()).isNull();
			assertThat(result.message().getPayload()).isEqualTo("async message");
			assertThat(result.message().getHeaders()).containsEntry("custom-header", "custom-value");

			var received = receiveAll(standardQueueUrl);
			JsonNode snsEnvelope = jsonMapper.readTree(received.get(0).body());
			assertThat(snsEnvelope.get("Message").asString()).isEqualTo("async message");

			JsonNode messageAttributes = snsEnvelope.get("MessageAttributes");
			assertThat(messageAttributes).isNotNull();
			assertThat(messageAttributes.get("custom-header").get("Value").asString()).isEqualTo("custom-value");
		}

		@Test
		void convertAndSendWithPersonPayload() throws Exception {
			Person person = new Person("John");
			SnsResult<Person> result = snsAsyncTemplate.convertAndSend(standardTopicArn, person,
				Map.of("person-type", "employee", NOTIFICATION_SUBJECT_HEADER, "Person Update")).get();

			assertThat(result.messageId()).isNotNull();
			assertThat(result.message().getPayload()).isEqualTo(person);

			var received = receiveAll(standardQueueUrl);
			JsonNode snsEnvelope = jsonMapper.readTree(received.get(0).body());
			Person receivedPerson = jsonMapper.readValue(snsEnvelope.get("Message").asString(), Person.class);
			assertThat(receivedPerson.getName()).isEqualTo("John");
			assertThat(snsEnvelope.get("Subject").asString()).isEqualTo("Person Update");

			JsonNode messageAttributes = snsEnvelope.get("MessageAttributes");
			assertThat(messageAttributes).isNotNull();
			assertThat(messageAttributes.get("person-type").get("Value").asString()).isEqualTo("employee");
		}

		@Test
		void sendNotificationWithSubject() throws Exception {
			SnsResult<Object> result = snsAsyncTemplate.sendNotification(standardTopicArn,
				"message with subject", "Test Subject").get();

			assertThat(result.messageId()).isNotNull();

			var received = receiveAll(standardQueueUrl);
			JsonNode body = jsonMapper.readTree(received.get(0).body());
			assertThat(body.get("Message").asString()).isEqualTo("message with subject");
			assertThat(body.get("Subject").asString()).isEqualTo("Test Subject");
		}

		@Test
		void sendNotificationWithSnsNotification() throws Exception {
			SnsNotification<String> notification = SnsNotification.builder("notification payload")
				.header(NOTIFICATION_SUBJECT_HEADER, "Notification Subject")
				.header("notification-type", "alert").build();

			SnsResult<String> result = snsAsyncTemplate.sendNotification(standardTopicArn, notification).get();

			assertThat(result.messageId()).isNotNull();
			assertThat(result.message().getPayload()).isEqualTo("notification payload");

			var received = receiveAll(standardQueueUrl);
			JsonNode snsEnvelope = jsonMapper.readTree(received.get(0).body());
			assertThat(snsEnvelope.get("Message").asString()).isEqualTo("notification payload");
			assertThat(snsEnvelope.get("Subject").asString()).isEqualTo("Notification Subject");

			JsonNode messageAttributes = snsEnvelope.get("MessageAttributes");
			assertThat(messageAttributes).isNotNull();
			assertThat(messageAttributes.get("notification-type").get("Value").asString()).isEqualTo("alert");
		}
	}

	@Nested
	class FifoTopicTests {

		@AfterEach
		void purgeQueue() {
			sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(fifoQueueUrl).build());
		}

		@Test
		void convertAndSendWithFifoHeaders() throws Exception {
			SnsResult<String> result = snsAsyncTemplate.convertAndSend(fifoTopicArn, "fifo message",
				Map.of(MESSAGE_GROUP_ID_HEADER, "group-1",
					MESSAGE_DEDUPLICATION_ID_HEADER, "dedup-1",
					"custom-fifo-header", "fifo-value")).get();

			assertThat(result.messageId()).isNotNull();
			assertThat(result.message().getPayload()).isEqualTo("fifo message");
			assertThat(result.message().getHeaders())
				.containsEntry(MESSAGE_GROUP_ID_HEADER, "group-1")
				.containsEntry(MESSAGE_DEDUPLICATION_ID_HEADER, "dedup-1");

			var received = receiveAll(fifoQueueUrl);
			JsonNode snsEnvelope = jsonMapper.readTree(received.get(0).body());
			assertThat(snsEnvelope.get("Message").asString()).isEqualTo("fifo message");

			JsonNode messageAttributes = snsEnvelope.get("MessageAttributes");
			assertThat(messageAttributes).isNotNull();
			assertThat(messageAttributes.get("custom-fifo-header").get("Value").asString()).isEqualTo("fifo-value");
		}
	}

	@Test
	void topicExistsReturnsTrueForExistingTopic() throws Exception {
		String topicArn = snsAsyncClient
			.createTopic(r -> r.name(RandomString.make())).get().topicArn();

		assertThat(snsAsyncTemplate.topicExists(topicArn).get()).isTrue();
	}

	@Test
	void topicExistsReturnsFalseForNonExistingTopic() throws Exception {
		assertThat(snsAsyncTemplate.topicExists(
			"arn:aws:sns:us-east-1:000000000000:nope").get()).isFalse();
	}
}
