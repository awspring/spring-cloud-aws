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
package io.awspring.cloud.sns.core.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.awspring.cloud.sns.LocalstackContainerTest;
import io.awspring.cloud.sns.Person;
import io.awspring.cloud.sns.core.CachingTopicArnResolver;
import io.awspring.cloud.sns.core.DefaultTopicArnResolver;
import io.awspring.cloud.sns.core.SnsHeaders;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.TopicArnResolver;
import io.awspring.cloud.sns.core.batch.converter.DefaultSnsMessageConverter;
import io.awspring.cloud.sns.core.batch.executor.SequentialBatchExecutionStrategy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Integration tests for {@link SnsBatchTemplate}
 *
 * @author Matej Nedic
 * @author haroya01
 */
class SnsBatchTemplateIntegrationTest implements LocalstackContainerTest {

	public static final String BATCH_TEST_TOPIC = "batch-test-topic";
	public static final String BATCH_TEST_QUEUE = "batch-test-queue";
	public static final String BATCH_TEST_TOPIC_FIFO = "batch-test-topic.fifo";
	public static final String BATCH_TEST_QUEUE_FIFO = "batch-test-queue.fifo";
	private static final JsonMapper jsonMapper = JsonMapper.builder().build();
	private static SqsClient sqsClient;
	private static SnsBatchTemplate snsBatchTemplate;
	private static String standardTopicArn;
	private static String standardQueueUrl;
	private static String fifoTopicArn;
	private static String fifoQueueUrl;

	@BeforeAll
	static void setUp() {
		SnsClient snsClient = LocalstackContainerTest.snsClient();
		sqsClient = LocalstackContainerTest.sqsClient();

		// Standard queue and Topic
		standardTopicArn = snsClient.createTopic(r -> r.name(BATCH_TEST_TOPIC)).topicArn();
		standardQueueUrl = sqsClient.createQueue(r -> r.queueName(BATCH_TEST_QUEUE)).queueUrl();
		String standardQueueArn = sqsClient
			.getQueueAttributes(r -> r.queueUrl(standardQueueUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
			.attributes().get(QueueAttributeName.QUEUE_ARN);
		snsClient.subscribe(r -> r.topicArn(standardTopicArn).protocol("sqs").endpoint(standardQueueArn));

		// Fifo queue and Topic
		fifoTopicArn = snsClient
			.createTopic(CreateTopicRequest.builder().name(BATCH_TEST_TOPIC_FIFO)
				.attributes(Map.of("FifoTopic", "true", "ContentBasedDeduplication", "true")).build())
			.topicArn();
		fifoQueueUrl = sqsClient
			.createQueue(r -> r.queueName(BATCH_TEST_QUEUE_FIFO)
				.attributes(Map.of(QueueAttributeName.FIFO_QUEUE, "true")))
			.queueUrl();
		String fifoQueueArn = sqsClient
			.getQueueAttributes(r -> r.queueUrl(fifoQueueUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
			.attributes().get(QueueAttributeName.QUEUE_ARN);
		snsClient.subscribe(r -> r.topicArn(fifoTopicArn).protocol("sqs").endpoint(fifoQueueArn));

		JacksonJsonMessageConverter jacksonConverter = new JacksonJsonMessageConverter(new JsonMapper());
		jacksonConverter.setSerializedPayloadClass(String.class);
		DefaultSnsMessageConverter messageConverter = new DefaultSnsMessageConverter(jacksonConverter);
		SequentialBatchExecutionStrategy executionStrategy = new SequentialBatchExecutionStrategy(snsClient);
		TopicArnResolver topicArnResolver = new CachingTopicArnResolver(new DefaultTopicArnResolver(snsClient));
		snsBatchTemplate = new SnsBatchTemplate(messageConverter, executionStrategy, topicArnResolver);
	}

	private static List<software.amazon.awssdk.services.sqs.model.Message> receiveAll(String queueUrl,
																					  int expectedCount) {
		await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {
			int count = getQueueMessageCount(queueUrl);
			assertThat(count).isEqualTo(expectedCount);
		});


		// Visibility timeout is set so While will work only unique message will be polled.
		List<software.amazon.awssdk.services.sqs.model.Message> messages = new ArrayList<>();
		while (messages.size() < expectedCount) {
			var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl)
				.maxNumberOfMessages(10).waitTimeSeconds(2).messageAttributeNames("All").build());
			if (!response.hasMessages() || response.messages().isEmpty()) {
				break;
			}
			messages.addAll(response.messages());
		}
		return messages;
	}

	private static int getQueueMessageCount(String queueUrl) {
		var attrs = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder().queueUrl(queueUrl)
			.attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES).build());
		return Integer.parseInt(
			attrs.attributes().getOrDefault(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES, "0"));
	}

	@Nested
	class StandardTopicTests {

		@AfterEach
		void purgeQueue() {
			sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(standardQueueUrl).build());
		}

		@Test
		void sendsBatchAndVerifiesMessageContent() {
			List<Message<String>> messages = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				messages.add(MessageBuilder.withPayload("Message " + i).build());
			}

			BatchResult result = snsBatchTemplate.sendBatch(BATCH_TEST_TOPIC, messages);

			assertThat(result.results()).hasSize(5);
			assertThat(result.errors()).isEmpty();

			var received = receiveAll(standardQueueUrl, 5);
			List<String> bodies = received.stream().map(m -> {
				JsonNode node = jsonMapper.readTree(m.body());
				return node.get("Message").asString();
			}).toList();
			assertThat(bodies).containsExactlyInAnyOrder(
				"Message 0", "Message 1", "Message 2", "Message 3", "Message 4");
		}

		@Test
		void sendsBatchWithMultipleBatchesAndVerifiesAll() {
			List<Message<String>> messages = new ArrayList<>();
			for (int i = 0; i < 12; i++) {
				messages.add(MessageBuilder.withPayload("Message " + i).build());
			}

			BatchResult result = snsBatchTemplate.sendBatch(BATCH_TEST_TOPIC, messages);

			assertThat(result.results()).hasSize(12);
			assertThat(result.errors()).isEmpty();

			var received = receiveAll(standardQueueUrl, 12);
			List<String> bodies = received.stream().map(m -> {
				JsonNode node = jsonMapper.readTree(m.body());
				return node.get("Message").asString();
			}).toList();
			assertThat(bodies).containsExactlyInAnyOrder(
				"Message 0", "Message 1", "Message 2", "Message 3", "Message 4",
				"Message 5", "Message 6", "Message 7", "Message 8", "Message 9",
				"Message 10", "Message 11");
		}

		@Test
		void sendsBatchWithCustomHeadersAndVerifiesAttributes() {
			List<Message<String>> messages = new ArrayList<>();
			messages.add(MessageBuilder.withPayload("Message with headers").setHeader("customHeader", "customValue")
				.setHeader("anotherHeader", 123).build());

			BatchResult result = snsBatchTemplate.sendBatch(BATCH_TEST_TOPIC, messages);

			assertThat(result.results()).hasSize(1);
			assertThat(result.errors()).isEmpty();

			var received = receiveAll(standardQueueUrl, 1);

			JsonNode snsEnvelope = jsonMapper.readTree(received.get(0).body());
			assertThat(snsEnvelope.get("Message").asString()).isEqualTo("Message with headers");

			JsonNode messageAttributes = snsEnvelope.get("MessageAttributes");
			assertThat(messageAttributes).isNotNull();
			assertThat(messageAttributes.get("customHeader").get("Value").asString()).isEqualTo("customValue");
		}

		@Test
		void sendsBatchWithJsonPayloadAndVerifiesContent() {
			var john = new Person("john");
			var doe = new Person("doe");
			Set<Person> setOfPerson = Set.of(john, doe);
			List<Message<Person>> messages = new ArrayList<>();
			messages.add(MessageBuilder.withPayload(john).build());
			messages.add(MessageBuilder.withPayload(doe).build());

			BatchResult result = snsBatchTemplate.sendBatch(BATCH_TEST_TOPIC, messages);

			assertThat(result.results()).hasSize(2);
			assertThat(result.errors()).isEmpty();

			var received = receiveAll(standardQueueUrl, 2);
			assertThat(received).hasSize(2);

			received.forEach(m -> {
				JsonNode snsEnvelope = jsonMapper.readTree(m.body());
				Person person = jsonMapper.readValue(snsEnvelope.get("Message").asString(), Person.class);
				assertThat(setOfPerson).contains(person);
			});
		}
		@Test
		void sendBatchNotificationsWithHeadersAndVerifiesContent() {
			var john = new Person("john");
			var doe = new Person("doe");
			var test = new Person("test");
			Set<Person> setOfPerson = Set.of(john, doe, test);
			List<SnsNotification<Person>> notifications = List.of(
				SnsNotification.builder(john).header("priority", "high").build(),
				SnsNotification.builder(doe).header("priority", "low").build(),
				SnsNotification.builder(test).header("priority", "medium").build());

			BatchResult result = snsBatchTemplate.sendBatchNotifications(BATCH_TEST_TOPIC, notifications);

			assertThat(result.results()).hasSize(3);
			assertThat(result.errors()).isEmpty();

			var received = receiveAll(standardQueueUrl, 3);
			received.forEach(m -> {
				JsonNode snsEnvelope = jsonMapper.readTree(m.body());
				Person person = jsonMapper.readValue(snsEnvelope.get("Message").asString(), Person.class);
				assertThat(setOfPerson).contains(person);

				JsonNode messageAttributes = snsEnvelope.get("MessageAttributes");
				assertThat(messageAttributes).isNotNull();
				assertThat(messageAttributes.get("priority")).isNotNull();
				assertThat(messageAttributes.get("priority").get("Value").asString())
					.isIn("high", "low", "medium");
			});
		}

		@Test
		void convertAndSendWithStringPayloads() {
			List<String> payloads = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				payloads.add("Payload " + i);
			}

			BatchResult result = snsBatchTemplate.convertAndSend(BATCH_TEST_TOPIC, payloads);

			assertThat(result.results()).hasSize(5);
			assertThat(result.errors()).isEmpty();
			assertThat(result.isFullySuccessful()).isTrue();

			var received = receiveAll(standardQueueUrl, 5);
			assertThat(received).hasSize(5);

			AtomicInteger i = new AtomicInteger(0);
			received.forEach(m -> {
				JsonNode node = jsonMapper.readTree(m.body());
				var message = node.get("Message").asString();
				assertThat(message).isEqualTo("Payload " + i.getAndIncrement());
			});
		}
	}

	@Nested
	class FifoTopicTests {

		@AfterEach
		void purgeQueue() {
			sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(fifoQueueUrl).build());
		}

		@Test
		void sendsBatchWithFifoHeadersAndVerifiesDelivery() {
			List<Message<String>> messages = new ArrayList<>();
			for (int i = 0; i < 3; i++) {
				messages.add(MessageBuilder.withPayload("Fifo Message " + i)
					.setHeader(SnsHeaders.MESSAGE_GROUP_ID_HEADER, "group-1")
					.setHeader(SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER, "dedup-" + i).build());
			}

			BatchResult result = snsBatchTemplate.sendBatch(BATCH_TEST_TOPIC_FIFO, messages);

			assertThat(result.results()).hasSize(3);
			assertThat(result.errors()).isEmpty();

			var received = receiveAll(fifoQueueUrl, 3);
			assertThat(received).hasSize(3);

			AtomicInteger i = new AtomicInteger(0);
			received.forEach(m -> {
				JsonNode node = jsonMapper.readTree(m.body());
				var message = node.get("Message").asString();
				assertThat(message).isEqualTo("Fifo Message " + i.getAndIncrement());
			});
		}

		@Test
		void sendsBatchPreservesOrderWithinGroup() {
			List<Message<String>> messages = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				messages.add(MessageBuilder.withPayload("Ordered " + i)
					.setHeader(SnsHeaders.MESSAGE_GROUP_ID_HEADER, "order-group")
					.setHeader(SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER, "order-dedup-" + i).build());
			}

			BatchResult result = snsBatchTemplate.sendBatch(BATCH_TEST_TOPIC_FIFO, messages);

			assertThat(result.results()).hasSize(5);
			assertThat(result.errors()).isEmpty();

			var received = receiveAll(fifoQueueUrl, 5);
			assertThat(received).hasSize(5);

			AtomicInteger i = new AtomicInteger(0);
			received.forEach(m -> {
				JsonNode node = jsonMapper.readTree(m.body());
				var message = node.get("Message").asString();
				assertThat(message).isEqualTo("Ordered " + i.getAndIncrement());
			});
		}

		@Test
		void deduplicatesMessagesWithSameDeduplicationId() {
			List<Message<String>> messages = new ArrayList<>();
			messages.add(MessageBuilder.withPayload("Unique Message")
				.setHeader(SnsHeaders.MESSAGE_GROUP_ID_HEADER, "dedup-group")
				.setHeader(SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER, "same-dedup-id").build());
			messages.add(MessageBuilder.withPayload("Duplicate Message")
				.setHeader(SnsHeaders.MESSAGE_GROUP_ID_HEADER, "dedup-group")
				.setHeader(SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER, "same-dedup-id").build());
			messages.add(MessageBuilder.withPayload("Another Unique")
				.setHeader(SnsHeaders.MESSAGE_GROUP_ID_HEADER, "dedup-group")
				.setHeader(SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER, "different-dedup-id").build());

			BatchResult result = snsBatchTemplate.sendBatch(BATCH_TEST_TOPIC_FIFO, messages);

			assertThat(result.errors()).isEmpty();

			var received = receiveAll(fifoQueueUrl, 2);
			List<String> bodies2 = received.stream().map(m -> {
				JsonNode node = jsonMapper.readTree(m.body());
				return node.get("Message").asString();
			}).toList();
			assertThat(bodies2).containsExactly("Unique Message", "Another Unique");
		}
	}

}
