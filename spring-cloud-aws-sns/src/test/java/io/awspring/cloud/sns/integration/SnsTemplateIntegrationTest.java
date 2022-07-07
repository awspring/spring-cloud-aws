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

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;

import io.awspring.cloud.sns.Person;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

/**
 * Integration tests for {@link SnsTemplate}.
 *
 * @author Matej Nedic
 * @since 3.0
 */
@Testcontainers
class SnsTemplateIntegrationTest {
	private static final String TOPIC_NAME = "my_topic_name";
	private static SnsTemplate snsTemplate;
	private static SnsClient snsClient;

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.0")).withServices(SNS).withReuse(true);

	@BeforeAll
	public static void createSnsTemplate() {
		snsClient = SnsClient.builder().endpointOverride(localstack.getEndpointOverride(DYNAMODB))
				.region(Region.of(localstack.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("noop", "noop")))
				.build();
		snsTemplate = new SnsTemplate(snsClient);
	}

	@Test
	void send_validTextMessage_usesTopicChannel_auto_create() {
		snsTemplate.convertAndSend(TOPIC_NAME, "message");
	}

	@Test
	void send_validTextMessage_usesTopicChannel_send_arn() {
		String topic_arn = snsClient.createTopic(CreateTopicRequest.builder().name(TOPIC_NAME).build()).topicArn();
		snsTemplate.convertAndSend(topic_arn, "message");
	}

	@Test
	void send_validPersonObject_usesTopicChannel_send_arn() {
		String topic_arn = snsClient.createTopic(CreateTopicRequest.builder().name(TOPIC_NAME).build()).topicArn();
		snsTemplate.sendNotification(topic_arn, SnsNotification.builder(new Person("foo")).groupId("groupId")
				.deduplicationId("deduplicationId").header("header-1", "value-1").subject("subject").build());
	}

}
