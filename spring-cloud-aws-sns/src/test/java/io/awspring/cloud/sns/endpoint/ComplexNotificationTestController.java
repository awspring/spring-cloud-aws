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

package io.awspring.cloud.sns.endpoint;

import java.io.IOException;

import io.awspring.cloud.sns.annotation.endpoint.NotificationMessageMapping;
import io.awspring.cloud.sns.annotation.endpoint.NotificationSubscriptionMapping;
import io.awspring.cloud.sns.annotation.endpoint.NotificationUnsubscribeConfirmationMapping;
import io.awspring.cloud.sns.annotation.handlers.NotificationMessage;
import io.awspring.cloud.sns.annotation.handlers.NotificationSubject;
import io.awspring.cloud.sns.handlers.NotificationStatus;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Agim Emruli
 */
@Controller
@RequestMapping("/myComplexTopic")
class ComplexNotificationTestController {

	private String subject;

	private Person message;

	String getSubject() {
		return this.subject;
	}

	Person getMessage() {
		return this.message;
	}

	@NotificationSubscriptionMapping
	void handleSubscriptionMessage(NotificationStatus status) throws IOException {
		// We subscribe to start receive the message
		status.confirmSubscription();
	}

	@NotificationMessageMapping
	void handleNotificationMessage(@NotificationSubject String subject, @NotificationMessage Person message) {
		this.subject = subject;
		this.message = message;
	}

	@NotificationUnsubscribeConfirmationMapping
	void handleUnsubscribeMessage(NotificationStatus status) {
		// e.g. the client has been unsubscribed and we want to "re-subscribe"
		status.confirmSubscription();
	}

	static class Person {

		private String firstName;

		private String lastName;

		String getFirstName() {
			return this.firstName;
		}

		void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		String getLastName() {
			return this.lastName;
		}

		void setLastName(String lastName) {
			this.lastName = lastName;
		}

	}

}
