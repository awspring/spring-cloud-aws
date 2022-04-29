/*
 * Copyright 2013-2021 the original author or authors.
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
package io.awspring.cloud.sns.sample;

import static io.awspring.cloud.sns.core.SnsHeaders.NOTIFICATION_SUBJECT_HEADER;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;

import io.awspring.cloud.sns.core.SnsTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.support.MessageBuilder;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

@SpringBootApplication
public class SpringSNSSample {

	private final SnsTemplate snsTemplate;

	private final SnsClient snsClient;

	private static LocalStackContainer localStack;

	public SpringSNSSample(SnsTemplate snsTemplate, SnsClient snsClient) {
		this.snsTemplate = snsTemplate;
		this.snsClient = snsClient;
	}

	public static void main(String[] args) {
		Testcontainers.exposeHostPorts(8080);
		localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.14.2")).withServices(SNS);
		localStack.start();
		System.setProperty("spring.cloud.aws.sns.region", localStack.getRegion());
		System.setProperty("spring.cloud.aws.sns.endpoint", localStack.getEndpointOverride(SNS).toString());
		System.setProperty("spring.cloud.aws.credentials.access-key", "test");
		System.setProperty("spring.cloud.aws.credentials.secret-key", "test");
		SpringApplication.run(SpringSNSSample.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void sendMessage() {
		String arn = snsClient.createTopic(CreateTopicRequest.builder().name("testTopic").build()).topicArn();
		snsClient.subscribe(SubscribeRequest.builder().protocol("http")
				.endpoint("http://host.testcontainers.internal:8080/testTopic").topicArn(arn).build());
		this.snsTemplate.send(arn, MessageBuilder.withPayload("Spring Cloud AWS SNS Sample!")
				.setHeader(NOTIFICATION_SUBJECT_HEADER, "Some value!").build());
	}

}
