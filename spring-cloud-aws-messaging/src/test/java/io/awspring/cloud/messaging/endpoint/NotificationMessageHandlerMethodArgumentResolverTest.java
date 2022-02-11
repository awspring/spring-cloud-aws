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

package io.awspring.cloud.messaging.endpoint;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.sns.message.SnsMessageManager;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationMessageHandlerMethodArgumentResolverTest {

	@Test
	void resolveArgument_wrongMessageType_reportsErrors() throws Exception {
		// Arrange
		SnsMessageManager snsMessageManager = mock(SnsMessageManager.class);
		NotificationMessageHandlerMethodArgumentResolver resolver = new NotificationMessageHandlerMethodArgumentResolver(
				snsMessageManager);

		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(new ClassPathResource("subscriptionConfirmation.json", getClass()).getInputStream());
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setContent(subscriptionRequestJsonContent);

		MethodParameter methodParameter = new MethodParameter(
				ReflectionUtils.findMethod(NotificationMethods.class, "subscriptionMethod", NotificationStatus.class),
				0);

		// Assert
		assertThatThrownBy(
				() -> resolver.resolveArgument(methodParameter, null, new ServletWebRequest(servletRequest), null))
						.isInstanceOf(IllegalArgumentException.class)
						.hasMessageContaining("@NotificationMessage annotated parameters are only allowed");
	}

	@Test
	void resolveArgument_notificationMessageTypeWithSubject_reportsErrors() throws Exception {
		// Arrange
		SnsMessageManager snsMessageManager = mock(SnsMessageManager.class);
		NotificationMessageHandlerMethodArgumentResolver resolver = new NotificationMessageHandlerMethodArgumentResolver(
				snsMessageManager);

		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(new ClassPathResource("notificationMessage.json", getClass()).getInputStream());
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setContent(subscriptionRequestJsonContent);

		MethodParameter methodParameter = new MethodParameter(
				ReflectionUtils.findMethod(NotificationMethods.class, "handleMethod", String.class, String.class), 0);

		// Act
		Object argument = resolver.resolveArgument(methodParameter, null, new ServletWebRequest(servletRequest), null);

		// Assert
		assertThat(argument).isEqualTo("asdasd");
	}

	@Test
	void supportsParameter_withIntegerParameterType_shouldReturnFalse() throws Exception {
		// Arrange
		SnsMessageManager snsMessageManager = mock(SnsMessageManager.class);
		NotificationMessageHandlerMethodArgumentResolver resolver = new NotificationMessageHandlerMethodArgumentResolver(
				snsMessageManager);
		MethodParameter methodParameter = new MethodParameter(
				ReflectionUtils.findMethod(NotificationMethods.class, "methodWithIntegerParameterType", Integer.class),
				0);

		// Act
		boolean supportsParameter = resolver.supportsParameter(methodParameter);

		// Assert
		assertThat(supportsParameter).isTrue();
	}

	@Test
	void resolveArgument_notificationMessageTypeWithSubject_reportsErrors_failsVerification() throws Exception {
		// Arrange
		SnsMessageManager snsMessageManager = new SnsMessageManager("eu-east-1");
		NotificationMessageHandlerMethodArgumentResolver resolver = new NotificationMessageHandlerMethodArgumentResolver(
				snsMessageManager);

		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(new ClassPathResource("notificationMessage.json", getClass()).getInputStream());
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setContent(subscriptionRequestJsonContent);

		MethodParameter methodParameter = new MethodParameter(
				ReflectionUtils.findMethod(NotificationMethods.class, "handleMethod", String.class, String.class), 0);

		// Assert
		assertThatThrownBy(() -> resolver.resolveArgument(methodParameter, null, new ServletWebRequest(servletRequest),
				null)).isInstanceOf(SdkClientException.class).hasMessageContaining(
						"igningCertUrl does not match expected endpoint. Expected sns.eu-east-1.amazonaws.com but received endpoint was sns.eu-west-1.amazonaws.com.");
	}

}
