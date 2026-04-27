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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.messaging.converter.MessageConversionException;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
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

	@Nullable
	private final SnsMessageManager snsMessageManager;

	public LegacyJackson2NotificationStatusHandlerMethodArgumentResolver(SnsClient snsClient) {
		this(snsClient, null);
	}

	public LegacyJackson2NotificationStatusHandlerMethodArgumentResolver(SnsClient snsClient,
			@Nullable SnsMessageManager snsMessageManager) {
		this.snsClient = snsClient;
		this.snsMessageManager = snsMessageManager;
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

		if (snsMessageManager != null) {
			verifySignature(content.asText());
		}

		return new AmazonSnsNotificationStatus(this.snsClient, content.get("TopicArn").asText(),
				content.get("Token").asText());
	}

	private void verifySignature(String payload) {
		try (InputStream messageStream = new ByteArrayInputStream(payload.getBytes())) {
			// Unmarshalling the message is not needed, but also done here
			snsMessageManager.parseMessage(messageStream);
		}
		catch (IOException e) {
			throw new MessageConversionException("Issue while verifying signature of Payload: '" + payload + "'", e);
		}
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
