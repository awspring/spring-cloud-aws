/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sns.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import io.awspring.cloud.sns.annotation.handlers.NotificationPayload;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;

/**
 *
 * Handles Subscription and Unsubscription events by transforming them to {@link NotificationPayloads} which can be used
 * to get and verify message.
 *
 * @author kazaff
 */
public class NotificationPayloadHandlerMethodArgumentResolver
		extends AbstractNotificationMessageHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(NotificationPayload.class)
				&& NotificationPayloads.class.isAssignableFrom(parameter.getParameterType()));
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Object doResolveArgumentFromNotificationMessage(JsonNode content, HttpInputMessage request,
			Class<?> parameterType) {

		NotificationPayloads result = new NotificationPayloads();
		result.setType(content.get("Type").asText());
		result.setMessageId(content.get("MessageId").asText());
		result.setTopicArn(content.get("TopicArn").asText());
		result.setMessage(content.get("Message").asText());
		result.setTimestamp(content.get("Timestamp").asText());
		result.setSignatureVersion(content.get("SignatureVersion").asText());
		result.setSigningCertUrl(content.get("SigningCertURL").asText());
		result.setSignature(content.get("Signature").asText());

		if (content.get("Token") != null) {
			result.setToken(content.get("Token").asText());
		}

		if (content.get("Subject") != null) {
			result.setSubject(content.get("Subject").asText());
		}

		if (content.get("SubscribeURL") != null) {
			result.setSubscribeUrl(content.get("SubscribeURL").asText());
		}

		if (content.get("UnsubscribeURL") != null) {
			result.setUnsubscribeUrl(content.get("UnsubscribeURL").asText());
		}

		return result;
	}

}
