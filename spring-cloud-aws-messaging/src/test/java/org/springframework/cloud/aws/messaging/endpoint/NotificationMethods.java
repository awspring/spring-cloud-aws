/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.messaging.endpoint;

import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.cloud.aws.messaging.config.annotation.NotificationMessage;
import org.springframework.cloud.aws.messaging.config.annotation.NotificationSubject;
import org.springframework.cloud.aws.messaging.endpoint.annotation.NotificationMessageMapping;
import org.springframework.cloud.aws.messaging.endpoint.annotation.NotificationSubscriptionMapping;

/**
 * @author Agim Emruli
 */
class NotificationMethods {

	@NotificationSubscriptionMapping
	void subscriptionMethod(NotificationStatus notificationStatus) {

	}

	@NotificationMessageMapping
	void handleMethod(@NotificationSubject String subject,
			@NotificationMessage String message) {

	}

	@SuppressWarnings("EmptyMethod")
	@RuntimeUse
	void methodWithIntegerParameterType(@NotificationMessage Integer message) {
	}

}
