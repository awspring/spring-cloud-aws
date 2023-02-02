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
package io.awspring.cloud.sns.integration;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import io.awspring.cloud.sns.Person;
import io.awspring.cloud.sns.core.TopicsListingTopicArnResolver;
import io.awspring.cloud.sns.core.TopicNotFoundException;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * Integration tests for {@link SnsTemplate}.
 *
 * @author Matej Nedic
 * @since 3.0
 */
@Testcontainers
class SnsTemplateIntegrationTest {
	private static final String TOPIC_NAME = "my_topic_name";
	private static String queueUrl;
	private static SnsTemplate snsTemplate;
	private static SnsClient snsClient;
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static SqsClient sqsClient;

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:1.3.1")).withServices(SNS).withServices(SQS).withReuse(true);

	@BeforeAll
	public static void createSnsTemplate() {
		snsClient = SnsClient.builder().endpointOverride(localstack.getEndpointOverride(SNS))
				.region(Region.of(localstack.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("noop", "noop")))
				.build();
		sqsClient = SqsClient.builder().endpointOverride(localstack.getEndpointOverride(SQS))
				.region(Region.of(localstack.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("noop", "noop")))
				.build();
		MappingJackson2MessageConverter mappingJackson2MessageConverter = new MappingJackson2MessageConverter();
		mappingJackson2MessageConverter.setSerializedPayloadClass(String.class);
		snsTemplate = new SnsTemplate(snsClient, mappingJackson2MessageConverter);
		queueUrl = sqsClient.createQueue(r -> r.queueName("my-queue")).queueUrl();
	}

	@AfterEach
	public void purgeQueue() {
		sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());
	}

	@Test
	void send_validTextMessage_usesTopicChannel_send_arn_read_by_sqs() {
		String topicArn = snsClient.createTopic(CreateTopicRequest.builder().name(TOPIC_NAME).build()).topicArn();
		snsClient.subscribe(r -> r.topicArn(topicArn).protocol("sqs").endpoint(queueUrl));

		snsTemplate.convertAndSend(topicArn, "message");

		await().untilAsserted(() -> {
			ReceiveMessageResponse response = sqsClient.receiveMessage(r -> r.queueUrl(queueUrl));
			assertThat(response.hasMessages()).isTrue();
			JsonNode body = objectMapper.readTree(response.messages().get(0).body());
			assertThat(body.get("Message").asText()).isEqualTo("message");
		});
	}

	@Test
	void send_validPersonObject_usesTopicChannel_send_arn_read_sqs() {
		String topic_arn = snsClient.createTopic(CreateTopicRequest.builder().name(TOPIC_NAME).build()).topicArn();

		snsClient.subscribe(r -> r.topicArn(topic_arn).protocol("sqs").endpoint(queueUrl));

		snsTemplate.convertAndSend(topic_arn, new Person("foo"));

		await().untilAsserted(() -> {
			ReceiveMessageResponse response = sqsClient.receiveMessage(r -> r.queueUrl(queueUrl));
			assertThat(response.hasMessages()).isTrue();
			Person person = objectMapper.readValue(
					objectMapper.readTree(response.messages().get(0).body()).get("Message").asText(), Person.class);
			assertThat(person.getName()).isEqualTo("foo");
		});
	}

	@Nested
	class TopicsListingTopicArnResolverTest {

		@BeforeAll
		public static void beforeAll() {
			createTopics();
		}

		@Test
		void send_test_message_for_existing_topic_name_trigger_trigger_iteration() {
			TopicsListingTopicArnResolver topicsListingTopicArnResolver = new TopicsListingTopicArnResolver(snsClient);
			SnsTemplate snsTemplateTestCache = new SnsTemplate(snsClient, topicsListingTopicArnResolver, null);
			snsTemplateTestCache.sendNotification(TOPIC_NAME + 100, "message content", "subject");
		}

		@Test
		void send_test_message_for_not_existing_topic_name() {
			TopicsListingTopicArnResolver topicsListingTopicArnResolver = new TopicsListingTopicArnResolver(snsClient);
			SnsTemplate snsTemplateTestCache = new SnsTemplate(snsClient, topicsListingTopicArnResolver, null);
			assertThatThrownBy(
				() -> snsTemplateTestCache.sendNotification("Some_random_topic", "message content", "subject"))
				.isInstanceOf(TopicNotFoundException.class);
		}

		private static void createTopics() {
			for (int i = 0; i < 101; i++) {
				snsClient.createTopic(CreateTopicRequest.builder().name(TOPIC_NAME + i).build());
			}
		}
	}


}
