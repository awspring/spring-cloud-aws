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

import com.amazonaws.services.sns.AmazonSNS;
import io.awspring.cloud.messaging.core.NotificationMessagingTemplate;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootApplication
public class SpringSNSSample {

	private final NotificationMessagingTemplate notificationMessagingTemplate;

	@Autowired
	public SpringSNSSample(AmazonSNS amazonSns) {
		this.notificationMessagingTemplate = new NotificationMessagingTemplate(amazonSns);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringSNSSample.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringSNSSample.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void sendMessage() {
		this.notificationMessagingTemplate.send("snsSpring",
				MessageBuilder.withPayload("Spring cloud Aws SNS sample!").build());
	}

	@SqsListener("InfrastractureStack-spring-aws")
	private void listenToMessage(GenericMessage message) {
		LOGGER.info("This is message you want to see: {}", message.getPayload());
	}

}
