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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.sns.handlers.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;

/**
 * @author Matej Nedic
 */
class WebFluxNotificationStatusHandlerMethodArgumentResolverTest {

	@Test
	void resolveArgument_wrongMessageType_reportsErrors() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		WebFluxNotificationStatusHandlerMethodArgumentResolver resolver = new WebFluxNotificationStatusHandlerMethodArgumentResolver(
				snsClient);

		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("notificationMessage.json"));

		MockServerHttpRequest request = MockServerHttpRequest.post("/").contentType(MediaType.APPLICATION_JSON)
				.body(new String(subscriptionRequestJsonContent));
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		MethodParameter methodParameter = new MethodParameter(ReflectionUtils
				.findMethod(WebFluxNotificationMethods.class, "subscriptionMethod", NotificationStatus.class), 0);

		// Assert
		assertThatThrownBy(() -> resolver.resolveArgument(methodParameter, null, exchange).block())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("NotificationStatus is only available");
	}

	@Test
	void resolveArgument_subscriptionRequest_createsValidSubscriptionStatus() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		WebFluxNotificationStatusHandlerMethodArgumentResolver resolver = new WebFluxNotificationStatusHandlerMethodArgumentResolver(
				snsClient);

		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("subscriptionConfirmation.json"));

		MockServerHttpRequest request = MockServerHttpRequest.post("/").contentType(MediaType.APPLICATION_JSON)
				.body(new String(subscriptionRequestJsonContent));
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		MethodParameter methodParameter = new MethodParameter(ReflectionUtils
				.findMethod(WebFluxNotificationMethods.class, "subscriptionMethod", NotificationStatus.class), 0);

		// Act
		Object resolvedArgument = resolver.resolveArgument(methodParameter, null, exchange).block();

		// Assert
		assertThat(resolvedArgument instanceof NotificationStatus).isTrue();
		((NotificationStatus) resolvedArgument).confirmSubscription();
		verify(snsClient, times(1)).confirmSubscription(ConfirmSubscriptionRequest.builder()
				.topicArn("arn:aws:sns:eu-west-1:111111111111:mySampleTopic")
				.token("1111111111111111111111111111111111111111111111" + "1111111111111111111111111111111111111111111"
						+ "1111111111111111111111111111111111111111111" + "1111111111111111111111111111111111111111111"
						+ "11111111111111111111111111111111111")
				.build());
	}

	@Test
	void resolveArgument_unsubscribeRequest_createsValidSubscriptionStatus() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		WebFluxNotificationStatusHandlerMethodArgumentResolver resolver = new WebFluxNotificationStatusHandlerMethodArgumentResolver(
				snsClient);

		byte[] unsubscribeRequestJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("unsubscribeConfirmation.json"));

		MockServerHttpRequest request = MockServerHttpRequest.post("/").contentType(MediaType.APPLICATION_JSON)
				.body(new String(unsubscribeRequestJsonContent));
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		MethodParameter methodParameter = new MethodParameter(ReflectionUtils
				.findMethod(WebFluxNotificationMethods.class, "subscriptionMethod", NotificationStatus.class), 0);

		// Act
		Object resolvedArgument = resolver.resolveArgument(methodParameter, null, exchange).block();

		// Assert
		assertThat(resolvedArgument instanceof NotificationStatus).isTrue();
		((NotificationStatus) resolvedArgument).confirmSubscription();
		verify(snsClient, times(1)).confirmSubscription(ConfirmSubscriptionRequest.builder()
				.topicArn("arn:aws:sns:eu-west-1:111111111111:mySampleTopic")
				.token("2336412f37fb687f5d51e6e241d638b05824e9e2f6713b42abaeb8607743f5ba91d34edd2b9dabe2f1616ed77c0f8801ee79911d34dca3d210c228af87bd5d9597bf0d6093a1464e03af6650e992ecf54605e020f04ad3d47796045c9f24d902e72e811a1ad59852cad453f40bddfb45")
				.build());
	}

}
