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
package io.awspring.cloud.sns.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.ServletWebRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;

class NotificationStatusHandlerMethodArgumentResolverTest {

	@Test
	void resolveArgument_wrongMessageType_reportsErrors() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		NotificationStatusHandlerMethodArgumentResolver resolver = new NotificationStatusHandlerMethodArgumentResolver(
				snsClient);

		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("notificationMessage.json"));
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setContent(subscriptionRequestJsonContent);

		MethodParameter methodParameter = new MethodParameter(
				ReflectionUtils.findMethod(NotificationMethods.class, "subscriptionMethod", NotificationStatus.class),
				0);

		// Assert
		assertThatThrownBy(
				() -> resolver.resolveArgument(methodParameter, null, new ServletWebRequest(servletRequest), null))
						.isInstanceOf(IllegalArgumentException.class)
						.hasMessageContaining("NotificationStatus is only available");

	}

	@Test
	void resolveArgument_subscriptionRequest_createsValidSubscriptionStatus() throws Exception {
		// Arrange
		SnsClient snsClient = mock(SnsClient.class);
		NotificationStatusHandlerMethodArgumentResolver resolver = new NotificationStatusHandlerMethodArgumentResolver(
				snsClient);

		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("subscriptionConfirmation.json"));

		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setContent(subscriptionRequestJsonContent);

		MethodParameter methodParameter = new MethodParameter(
				ReflectionUtils.findMethod(NotificationMethods.class, "subscriptionMethod", NotificationStatus.class),
				0);

		// Act
		Object resolvedArgument = resolver.resolveArgument(methodParameter, null, new ServletWebRequest(servletRequest),
				null);

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

}
