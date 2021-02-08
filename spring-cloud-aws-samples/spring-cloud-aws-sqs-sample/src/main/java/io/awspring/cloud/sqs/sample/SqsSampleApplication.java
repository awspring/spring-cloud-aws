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

package io.awspring.cloud.sqs.sample;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootApplication
public class SqsSampleApplication {

	private final QueueMessagingTemplate queueMessagingTemplate;

	@Autowired
	public SqsSampleApplication(AmazonSQSAsync amazonSqs) {
		this.queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SqsSampleApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SqsSampleApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void sendMessage() {
		this.queueMessagingTemplate.send("InfrastructureStack-spring-aws",
				MessageBuilder.withPayload("Spring cloud Aws SQS sample!").build());
		this.queueMessagingTemplate.convertAndSend("InfrastructureStack-aws-pojo", new Person("Joe", "Doe"));
	}

	@SqsListener("InfrastructureStack-spring-aws")
	private void listenToMessage(String message) {
		LOGGER.info("This is message you want to see: {}", message);
	}

	@SqsListener("InfrastructureStack-aws-pojo")
	private void listenToPerson(Person person) {
		LOGGER.info(person.toString());
	}

}
