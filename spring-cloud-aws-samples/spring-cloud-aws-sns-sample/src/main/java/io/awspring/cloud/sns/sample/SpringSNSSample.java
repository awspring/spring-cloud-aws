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

import io.awspring.cloud.sns.core.SnsTemplate;
import io.awspring.cloud.sns.sms.core.SnsSmsTemplate;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

@SpringBootApplication
public class SpringSNSSample {

	public static void main(String[] args) {
		SpringApplication.run(SpringSNSSample.class, args);
	}

	@Bean
	public ApplicationRunner applicationRunner(SnsTemplate snsTemplate, SnsClient snsClient, SnsSmsTemplate snsSmsTemplate) {
		return args -> {
			String arn = snsClient.createTopic(CreateTopicRequest.builder().name("testTopic").build()).topicArn();
			snsClient.subscribe(SubscribeRequest.builder().protocol("http")
				.endpoint("http://host.docker.internal:8080/testTopic").topicArn(arn).build());
			snsTemplate.send(arn, MessageBuilder.withPayload("Spring Cloud AWS SNS Sample!")
				.setHeader(NOTIFICATION_SUBJECT_HEADER, "Some value!").build());

			snsSmsTemplate.send("your phone number", "Message to be delivered");
		};
	}

}
