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

import io.awspring.cloud.sns.core.CachingTopicArnResolver;
import io.awspring.cloud.sns.core.DefaultTopicArnResolver;
import io.awspring.cloud.sns.core.SnsHeaders;
import io.awspring.cloud.sns.core.TopicArnResolver;
import io.awspring.cloud.sns.core.batch.converter.DefaultSnsMessageConverter;
import io.awspring.cloud.sns.core.batch.converter.TestPayload;
import io.awspring.cloud.sns.core.batch.executor.DefaultBatchExecutionStrategy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Matej Nedic
 */
@Testcontainers
class SnsBatchTemplateIntegrationTest {

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:latest"));

	private static SnsClient snsClient;
	private static String topicArn;
	private static SnsBatchTemplate snsBatchTemplate;

	@BeforeAll
	static void setUp() {
		snsClient = SnsClient.builder().endpointOverride(localstack.getEndpoint())
				.credentialsProvider(StaticCredentialsProvider
						.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
				.region(Region.of(localstack.getRegion())).build();

		CreateTopicResponse createTopicResponse = snsClient.createTopic(builder -> builder.name("test-topic"));
		topicArn = createTopicResponse.topicArn();

		JacksonJsonMessageConverter jacksonConverter = new JacksonJsonMessageConverter(new JsonMapper());
		jacksonConverter.setSerializedPayloadClass(String.class);
		DefaultSnsMessageConverter messageConverter = new DefaultSnsMessageConverter(jacksonConverter);
		DefaultBatchExecutionStrategy executionStrategy = new DefaultBatchExecutionStrategy(snsClient);
		TopicArnResolver topicArnResolver = new CachingTopicArnResolver(new DefaultTopicArnResolver(snsClient));

		snsBatchTemplate = new SnsBatchTemplate(messageConverter, executionStrategy, topicArnResolver);
	}

	@Test
	void sendsBatchWithDefaultConfiguration() {
		List<Message<String>> messages = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			messages.add(MessageBuilder.withPayload("Message " + i).build());
		}

		BatchResult result = snsBatchTemplate.sendBatch("test-topic", messages);

		assertThat(result.results()).hasSize(5);
		assertThat(result.errors()).isEmpty();
		assertThat(result.isFullySuccessful()).isTrue();
		assertThat(result.hasErrors()).isFalse();

		GetTopicAttributesResponse attributes = snsClient
				.getTopicAttributes(GetTopicAttributesRequest.builder().topicArn(topicArn).build());
		assertThat(attributes.attributes()).isNotEmpty();
	}

	@Test
	void sendsBatchWithMultipleBatches() {
		List<Message<String>> messages = new ArrayList<>();
		for (int i = 1; i <= 12; i++) {
			messages.add(MessageBuilder.withPayload("Message " + i).build());
		}

		BatchResult result = snsBatchTemplate.sendBatch("test-topic", messages);

		assertThat(result.results()).hasSize(12);
		assertThat(result.errors()).isEmpty();
		assertThat(result.isFullySuccessful()).isTrue();
	}

	@Test
	void sendsBatchWithFifoHeaders() {
		List<Message<String>> messages = new ArrayList<>();
		for (int i = 1; i <= 3; i++) {
			messages.add(
					MessageBuilder.withPayload("Message " + i).setHeader(SnsHeaders.MESSAGE_GROUP_ID_HEADER, "group-1")
							.setHeader(SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER, "dedup-" + i).build());
		}

		BatchResult result = snsBatchTemplate.sendBatch("test-topic", messages);

		assertThat(result.results()).hasSize(3);
		assertThat(result.errors()).isEmpty();
	}

	@Test
	void sendsBatchWithCustomHeaders() {
		List<Message<String>> messages = new ArrayList<>();
		messages.add(MessageBuilder.withPayload("Message with headers").setHeader("customHeader", "customValue")
				.setHeader("anotherHeader", 123).build());

		BatchResult result = snsBatchTemplate.sendBatch("test-topic", messages);

		assertThat(result.results()).hasSize(1);
		assertThat(result.errors()).isEmpty();
	}

	@Test
	void sendsBatchWithJsonPayload() {
		List<Message<TestPayload>> messages = new ArrayList<>();
		messages.add(MessageBuilder.withPayload(new TestPayload("Alice", 25)).build());
		messages.add(MessageBuilder.withPayload(new TestPayload("Bob", 30)).build());

		BatchResult result = snsBatchTemplate.sendBatch("test-topic", messages);

		assertThat(result.results()).hasSize(2);
		assertThat(result.errors()).isEmpty();
	}

	@Test
	void sendsBatchWithTopicArnDirectly() {
		List<Message<String>> messages = new ArrayList<>();
		messages.add(MessageBuilder.withPayload("Direct ARN message").build());

		BatchResult result = snsBatchTemplate.sendBatch(topicArn, messages);

		assertThat(result.results()).hasSize(1);
		assertThat(result.errors()).isEmpty();
	}

	@Test
	void convertAndSendWithStringPayloads() {
		List<String> payloads = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			payloads.add("Payload " + i);
		}

		BatchResult result = snsBatchTemplate.convertAndSend("test-topic", payloads);

		assertThat(result.results()).hasSize(5);
		assertThat(result.errors()).isEmpty();
		assertThat(result.isFullySuccessful()).isTrue();
	}

}
