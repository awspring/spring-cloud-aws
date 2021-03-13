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

import io.awspring.cloud.messaging.config.annotation.NotificationMessage;
import io.awspring.cloud.messaging.config.annotation.NotificationSubject;
import io.awspring.cloud.messaging.endpoint.NotificationStatus;
import io.awspring.cloud.messaging.endpoint.annotation.NotificationMessageMapping;
import io.awspring.cloud.messaging.endpoint.annotation.NotificationSubscriptionMapping;
import io.awspring.cloud.messaging.endpoint.annotation.NotificationUnsubscribeConfirmationMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/testTopic")
public class NotificationMappingController {

	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationMappingController.class);

	@NotificationSubscriptionMapping
	public void handleSubscriptionMessage(NotificationStatus status) {
		status.confirmSubscription();
	}

	@NotificationMessageMapping
	public void handleNotificationMessage(@NotificationSubject String subject, @NotificationMessage String message) {
		LOGGER.info("NotificationMessageMapping message is: {}", message);
		LOGGER.info("NotificationMessageMapping subject is: {}" + subject);
	}

	@NotificationUnsubscribeConfirmationMapping
	public void handleUnsubscribeMessage(NotificationStatus status) {
		status.confirmSubscription();
	}

}
