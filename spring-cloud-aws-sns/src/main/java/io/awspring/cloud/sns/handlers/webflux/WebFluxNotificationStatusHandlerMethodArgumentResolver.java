/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.sns.handlers.webflux;

import io.awspring.cloud.sns.handlers.NotificationStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;
import tools.jackson.databind.JsonNode;

/**
 * WebFlux argument resolver that handles SNS subscription and unsubscription confirmation events by resolving them to a
 * {@link NotificationStatus} instance, which can be used to confirm or re-confirm subscriptions.
 *
 * <p>
 * This is the reactive counterpart of
 * {@link io.awspring.cloud.sns.handlers.NotificationStatusHandlerMethodArgumentResolver}.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public class WebFluxNotificationStatusHandlerMethodArgumentResolver
		extends WebFluxAbstractNotificationMessageHandlerMethodArgumentResolver {

	private final SnsClient snsClient;

	@Nullable
	private final SnsMessageManager snsMessageManager;

	public WebFluxNotificationStatusHandlerMethodArgumentResolver(SnsClient snsClient) {
		this(snsClient, null);
	}

	public WebFluxNotificationStatusHandlerMethodArgumentResolver(SnsClient snsClient,
			@Nullable SnsMessageManager snsMessageManager) {
		this.snsClient = snsClient;
		this.snsMessageManager = snsMessageManager;
	}

	@Override
	protected Mono<Object> doResolveArgumentFromNotificationMessage(JsonNode content, ServerWebExchange exchange,
			Class<?> parameterType) {
		if (!"SubscriptionConfirmation".equals(content.get("Type").asString())
				&& !"UnsubscribeConfirmation".equals(content.get("Type").asString())) {
			return Mono.error(new IllegalArgumentException(
					"NotificationStatus is only available for subscription and unsubscription requests"));
		}

		if (snsMessageManager != null) {
			verifySignature(content.toString());
		}

		return Mono.just(new AmazonSnsNotificationStatus(this.snsClient, content.get("TopicArn").asString(),
				content.get("Token").asString()));
	}

	private void verifySignature(String payload) {
		snsMessageManager.parseMessage(payload);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return NotificationStatus.class.isAssignableFrom(parameter.getParameterType());
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
