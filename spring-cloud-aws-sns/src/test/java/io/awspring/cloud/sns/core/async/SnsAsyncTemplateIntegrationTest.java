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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Integration tests for {@link SnsAsyncTemplate}.
 *
 * @author Matej Nedic
 * @since 4.0.1
 */
@Testcontainers
class SnsAsyncTemplateIntegrationTest {

	private static final String TOPIC_NAME = "async_topic_name";
	private static SnsAsyncTemplate snsAsyncTemplate;
	private static SnsAsyncClient snsAsyncClient;
	private static SqsClient sqsClient;

	private final JsonMapper jsonMapper = new JsonMapper();

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:4.4.0"));

	@BeforeAll
	public static void createSnsAsyncTemplate() {
		snsAsyncClient = SnsAsyncClient.builder()
			.endpointOverride(localstack.getEndpoint())
			.region(Region.of(localstack.getRegion()))
			.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
			.build();
		
		sqsClient = SqsClient.builder()
			.endpointOverride(localstack.getEndpoint())
			.region(Region.of(localstack.getRegion()))
			.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
			.build();
		
		JacksonJsonMessageConverter jacksonJsonConverter = new JacksonJsonMessageConverter();
		jacksonJsonConverter.setSerializedPayloadClass(String.class);
		
		DefaultSnsPublishMessageConverter converter = new DefaultSnsPublishMessageConverter(jacksonJsonConverter);
		snsAsyncTemplate = new SnsAsyncTemplate(snsAsyncClient, converter);
	}

	@Nested
	class FifoTopics {
		private static String queueUrl;
		private static String queueArn;

		@BeforeAll
		public static void init() {
			queueUrl = sqsClient
				.createQueue(r -> r.queueName("async-queue.fifo")
					.attributes(Map.of(QueueAttributeName.FIFO_QUEUE, "true")))
				.queueUrl();
			queueArn = sqsClient
				.getQueueAttributes(r -> r.queueUrl(queueUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
				.attributes().get(QueueAttributeName.QUEUE_ARN);
		}

		@Test
		void convertAndSend_validTextMessage_usesFifoTopic_sendsToSqs() throws Exception {
			String topicName = "async_fifo_topic.fifo";
			Map<String, String> topicAttributes = new HashMap<>();
			topicAttributes.put("FifoTopic", String.valueOf(true));
			String topicArn = snsAsyncClient
				.createTopic(CreateTopicRequest.builder().name(topicName).attributes(topicAttributes).build())
				.get()
				.topicArn();
			snsAsyncClient.subscribe(r -> r.topicArn(topicArn).protocol("sqs").endpoint(queueArn)).get();

			Map<String, Object> headers = new HashMap<>();
			headers.put(MESSAGE_GROUP_ID_HEADER, "group-id");
			headers.put(MESSAGE_DEDUPLICATION_ID_HEADER, "dedup-id");
			headers.put("custom-fifo-header", "fifo-value");
			CompletableFuture<SnsResult<String>> result = snsAsyncTemplate.convertAndSend(
				topicName, 
				"async message",
				headers
			);

			SnsResult<String> snsResult = result.get();
			assertThat(snsResult.messageId()).isNotNull();
			assertThat(snsResult.message().getPayload()).isEqualTo("async message");
			assertThat(snsResult.message().getHeaders()).containsKeys("id", "timestamp", MESSAGE_GROUP_ID_HEADER, MESSAGE_DEDUPLICATION_ID_HEADER, "custom-fifo-header");
			assertThat(snsResult.message().getHeaders().get(MESSAGE_GROUP_ID_HEADER)).isEqualTo("group-id");
			assertThat(snsResult.message().getHeaders().get(MESSAGE_DEDUPLICATION_ID_HEADER)).isEqualTo("dedup-id");

			await().untilAsserted(() -> {
				ReceiveMessageResponse response = sqsClient.receiveMessage(r -> r.queueUrl(queueUrl));
				assertThat(response.hasMessages()).isTrue();
				JsonNode body = jsonMapper.readTree(response.messages().get(0).body());
				assertThat(body.get("Message").asString()).isEqualTo("async message");
			});
		}

		@AfterEach
		public void purgeQueue() {
			sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());
		}
	}

	@Nested
	class NonFifoTopics {
		private static String queueUrl;
		private static String queueArn;

		@BeforeAll
		public static void init() {
			queueUrl = sqsClient.createQueue(r -> r.queueName("async-standard-queue")).queueUrl();
			queueArn = sqsClient
				.getQueueAttributes(r -> r.queueUrl(queueUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
				.attributes().get(QueueAttributeName.QUEUE_ARN);
		}

		@Test
		void convertAndSend_validTextMessage_sendsToStandardTopic() throws Exception {
			String topicArn = snsAsyncClient
				.createTopic(CreateTopicRequest.builder().name(TOPIC_NAME).build())
				.get()
				.topicArn();
			snsAsyncClient.subscribe(r -> r.topicArn(topicArn).protocol("sqs").endpoint(queueArn)).get();

			Map<String, Object> headers = new HashMap<>();
			headers.put("custom-header", "custom-value");
			headers.put("number-header", 42);
			CompletableFuture<SnsResult<String>> result = snsAsyncTemplate.convertAndSend(topicArn, "async message", headers);

			SnsResult<String> snsResult = result.get();
			assertThat(snsResult.messageId()).isNotNull();
			assertThat(snsResult.sequenceNumber()).isNull();
			assertThat(snsResult.message().getPayload()).isEqualTo("async message");
			assertThat(snsResult.message().getHeaders()).containsKeys("id", "timestamp", "custom-header", "number-header");
			assertThat(snsResult.message().getHeaders().get("custom-header")).isEqualTo("custom-value");
			assertThat(snsResult.message().getHeaders().get("number-header")).isEqualTo(42);

			await().untilAsserted(() -> {
				ReceiveMessageResponse response = sqsClient.receiveMessage(r -> r.queueUrl(queueUrl));
				assertThat(response.hasMessages()).isTrue();
				JsonNode body = jsonMapper.readTree(response.messages().get(0).body());
				assertThat(body.get("Message").asString()).isEqualTo("async message");
			});
		}

		@Test
		void convertAndSend_validPersonObject_sendsToStandardTopic() throws Exception {
			String topicArn = snsAsyncClient
				.createTopic(CreateTopicRequest.builder().name(TOPIC_NAME + "_person").build())
				.get()
				.topicArn();
			snsAsyncClient.subscribe(r -> r.topicArn(topicArn).protocol("sqs").endpoint(queueArn)).get();

			Person person = new Person("John Doe");
			Map<String, Object> headers = new HashMap<>();
			headers.put("person-type", "employee");
			headers.put(NOTIFICATION_SUBJECT_HEADER, "Person Update");
			CompletableFuture<SnsResult<Person>> result = snsAsyncTemplate.convertAndSend(topicArn, person, headers);

			SnsResult<Person> snsResult = result.get();
			assertThat(snsResult.messageId()).isNotNull();
			assertThat(snsResult.message().getPayload()).isEqualTo(person);
			assertThat(snsResult.message().getHeaders()).containsKeys("id", "timestamp", "person-type", NOTIFICATION_SUBJECT_HEADER);
			assertThat(snsResult.message().getHeaders().get("person-type")).isEqualTo("employee");

			await().untilAsserted(() -> {
				ReceiveMessageResponse response = sqsClient.receiveMessage(r -> r.queueUrl(queueUrl));
				assertThat(response.hasMessages()).isTrue();
				Person receivedPerson = jsonMapper.readValue(
					jsonMapper.readTree(response.messages().get(0).body()).get("Message").asString(),
					Person.class
				);
				assertThat(receivedPerson.getName()).isEqualTo("John Doe");
			});
		}

		@Test
		void sendNotification_withSubject_sendsMessageWithSubject() throws Exception {
			String topicArn = snsAsyncClient
				.createTopic(CreateTopicRequest.builder().name(TOPIC_NAME + "_subject").build())
				.get()
				.topicArn();
			snsAsyncClient.subscribe(r -> r.topicArn(topicArn).protocol("sqs").endpoint(queueArn)).get();

			CompletableFuture<SnsResult<Object>> result = snsAsyncTemplate.sendNotification(
				topicArn,
				"message with subject",
				"Test Subject"
			);

			SnsResult<Object> snsResult = result.get();
			assertThat(snsResult.messageId()).isNotNull();

			await().untilAsserted(() -> {
				ReceiveMessageResponse response = sqsClient.receiveMessage(r -> r.queueUrl(queueUrl));
				assertThat(response.hasMessages()).isTrue();
				JsonNode body = jsonMapper.readTree(response.messages().get(0).body());
				assertThat(body.get("Message").asString()).isEqualTo("message with subject");
				assertThat(body.get("Subject").asString()).isEqualTo("Test Subject");
			});
		}

		@Test
		void sendNotification_withSnsNotification_sendsMessageWithHeaders() throws Exception {
			String topicArn = snsAsyncClient
				.createTopic(CreateTopicRequest.builder().name(TOPIC_NAME + "_notification").build())
				.get()
				.topicArn();
			snsAsyncClient.subscribe(r -> r.topicArn(topicArn).protocol("sqs").endpoint(queueArn)).get();

			SnsNotification<String> notification = SnsNotification.builder("notification payload")
				.header(NOTIFICATION_SUBJECT_HEADER, "Notification Subject")
				.header("notification-type", "alert")
				.header("priority", 1)
				.build();

			CompletableFuture<SnsResult<String>> result = snsAsyncTemplate.sendNotification(topicArn, notification);

			SnsResult<String> snsResult = result.get();
			assertThat(snsResult.messageId()).isNotNull();
			assertThat(snsResult.message().getPayload()).isEqualTo("notification payload");
			assertThat(snsResult.message().getHeaders()).containsKeys("id", "timestamp", NOTIFICATION_SUBJECT_HEADER, "notification-type", "priority");

			await().untilAsserted(() -> {
				ReceiveMessageResponse response = sqsClient.receiveMessage(r -> r.queueUrl(queueUrl));
				assertThat(response.hasMessages()).isTrue();
				JsonNode body = jsonMapper.readTree(response.messages().get(0).body());
				assertThat(body.get("Message").asString()).isEqualTo("notification payload");
				assertThat(body.get("Subject").asString()).isEqualTo("Notification Subject");
			});
		}

		@AfterEach
		public void purgeQueue() {
			sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());
		}
	}

	@Test
	void topicExists_shouldReturnFalseForNonExistingTopic() throws Exception {
		String nonExistentTopicArn = "arn:aws:sns:us-east-1:000000000000:nope";

		CompletableFuture<Boolean> result = snsAsyncTemplate.topicExists(nonExistentTopicArn);

		assertThat(result.get()).isFalse();
	}

	@Test
	void topicExists_shouldReturnTrueForExistingTopic() throws Exception {
		String topicName = RandomString.make();
		var topicArn = snsAsyncClient.createTopic(request -> request.name(topicName)).get().topicArn();

		CompletableFuture<Boolean> result = snsAsyncTemplate.topicExists(topicArn);

		assertThat(result.get()).isTrue();
	}
}
