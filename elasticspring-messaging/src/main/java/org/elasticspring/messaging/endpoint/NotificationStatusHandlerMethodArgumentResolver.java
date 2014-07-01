/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.endpoint;

import com.amazonaws.services.sns.AmazonSNS;
import org.springframework.core.MethodParameter;

import java.util.HashMap;

/**
 * @author Agim Emruli
 */
public class NotificationStatusHandlerMethodArgumentResolver extends AbstractNotificationMessageHandlerMethodArgumentResolver {

	private final AmazonSNS amazonSns;

	public NotificationStatusHandlerMethodArgumentResolver(AmazonSNS amazonSns) {
		this.amazonSns = amazonSns;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return NotificationStatus.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	protected Object doResolverArgumentFromNotificationMessage(HashMap<String, String> content) {
		if (!"SubscriptionConfirmation".equals(content.get("Type")) && !"UnsubscribeConfirmation".equals(content.get("Type"))) {
			throw new IllegalArgumentException("NotificationStatus is only available for subscription and unsubscription requests");
		}
		return new AmazonSnsNotificationStatus(this.amazonSns, content.get("TopicArn"), content.get("Token"));
	}

	private static class AmazonSnsNotificationStatus implements NotificationStatus {

		private final AmazonSNS amazonSns;
		private final String topicArn;
		private final String confirmationToken;

		private AmazonSnsNotificationStatus(AmazonSNS amazonSns, String topicArn, String confirmationToken) {
			this.amazonSns = amazonSns;
			this.topicArn = topicArn;
			this.confirmationToken = confirmationToken;
		}

		@Override
		public void confirmSubscription() {
			this.amazonSns.confirmSubscription(this.topicArn, this.confirmationToken);
		}

	}
}