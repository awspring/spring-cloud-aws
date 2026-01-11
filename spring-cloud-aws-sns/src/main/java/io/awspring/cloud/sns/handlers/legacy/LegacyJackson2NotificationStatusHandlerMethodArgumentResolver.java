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
package io.awspring.cloud.sns.handlers.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import io.awspring.cloud.sns.handlers.NotificationStatus;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;

/**
 *
 * Handles Subscription and Unsubscription events by transforming them to {@link NotificationStatus} which can be used
 * to confirm Subscriptions/Subscriptions.
 *
 * @author Agim Emruli
 */
public class LegacyJackson2NotificationStatusHandlerMethodArgumentResolver
		extends LegacyJackson2AbstractNotificationMessageHandlerMethodArgumentResolver {

	private final SnsClient snsClient;

	public LegacyJackson2NotificationStatusHandlerMethodArgumentResolver(SnsClient snsClient) {
		this.snsClient = snsClient;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return NotificationStatus.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	protected Object doResolveArgumentFromNotificationMessage(JsonNode content, HttpInputMessage request,
			Class<?> parameterType) {
		if (!"SubscriptionConfirmation".equals(content.get("Type").asText())
				&& !"UnsubscribeConfirmation".equals(content.get("Type").asText())) {
			throw new IllegalArgumentException(
					"NotificationStatus is only available for subscription and unsubscription requests");
		}
		return new AmazonSnsNotificationStatus(this.snsClient, content.get("TopicArn").asText(),
				content.get("Token").asText());
	}

	public static final class AmazonSnsNotificationStatus implements NotificationStatus {

		private final SnsClient snsClient;

		private final String topicArn;

		private final String confirmationToken;

		private AmazonSnsNotificationStatus(SnsClient snsClient, String topicArn, String confirmationToken) {
			this.snsClient = snsClient;
			this.topicArn = topicArn;
			this.confirmationToken = confirmationToken;
		}

		@Override
		public void confirmSubscription() {
			this.snsClient.confirmSubscription(
					ConfirmSubscriptionRequest.builder().topicArn(this.topicArn).token(this.confirmationToken).build());
		}

	}

}
