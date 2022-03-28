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

import io.awspring.cloud.sns.core.NotificationMessagingTemplate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.support.MessageBuilder;

import static io.awspring.cloud.sns.core.MessageHeaderCodes.NOTIFICATION_SUBJECT_HEADER;

@SpringBootApplication
public class SpringSNSSample {

	private NotificationMessagingTemplate notificationMessagingTemplate;

	public SpringSNSSample(NotificationMessagingTemplate notificationMessagingTemplate) {
		this.notificationMessagingTemplate = notificationMessagingTemplate;
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringSNSSample.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void sendMessage() {
		this.notificationMessagingTemplate.send("snsSpring", MessageBuilder.withPayload("Spring Cloud AWS SNS Sample!")
				.setHeader(NOTIFICATION_SUBJECT_HEADER, "Message subject").build());
	}

}
