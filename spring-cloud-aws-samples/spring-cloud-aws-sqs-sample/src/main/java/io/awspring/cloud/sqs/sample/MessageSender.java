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

package io.awspring.cloud.sqs.sample;

import java.time.LocalDate;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
class MessageSender {

	private final QueueMessagingTemplate queueMessagingTemplate;

	MessageSender(QueueMessagingTemplate queueMessagingTemplate) {
		this.queueMessagingTemplate = queueMessagingTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void sendMessage() {
		this.queueMessagingTemplate.send("InfrastructureStack-spring-aws",
				MessageBuilder.withPayload("Spring cloud Aws SQS sample!").build());
		this.queueMessagingTemplate.convertAndSend("InfrastructureStack-aws-pojo",
				new Person("Joe", "Doe", LocalDate.of(2000, 1, 12)));
	}

}
