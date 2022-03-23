package io.awspring.cloud.sns.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;

/**
 * @author Agim Emruli
 */
public class NotificationStatusHandlerMethodArgumentResolver
		extends AbstractNotificationMessageHandlerMethodArgumentResolver {

	private final SnsClient snsClient;

	public NotificationStatusHandlerMethodArgumentResolver(SnsClient snsClient) {
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

	private static final class AmazonSnsNotificationStatus implements NotificationStatus {

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
