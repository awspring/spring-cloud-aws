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

import io.awspring.cloud.sns.annotation.handlers.NotificationSubject;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.ServletWebRequest;

class NotificationSubjectHandlerMethodArgumentResolverTest {

	@Test
	void resolveArgument_wrongMessageType_reportsErrors() throws Exception {
		// Arrange
		NotificationSubjectHandlerMethodArgumentResolver resolver = new NotificationSubjectHandlerMethodArgumentResolver();

		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("subscriptionConfirmation.json"));
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
		NotificationSubjectHandlerMethodArgumentResolver resolver = new NotificationSubjectHandlerMethodArgumentResolver();

		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("notificationMessage.json"));
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setContent(subscriptionRequestJsonContent);

		MethodParameter methodParameter = new MethodParameter(
				ReflectionUtils.findMethod(NotificationMethods.class, "subscriptionMethod", NotificationStatus.class),
				0);

		// Act
		Object argument = resolver.resolveArgument(methodParameter, null, new ServletWebRequest(servletRequest), null);

		// Assert
		assertThat(argument).isEqualTo("asdasd");
	}

	@Test
	void supportsParameter_withWrongParameterType_shouldReturnFalse() throws Exception {
		// Arrange
		NotificationSubjectHandlerMethodArgumentResolver resolver = new NotificationSubjectHandlerMethodArgumentResolver();
		Method methodWithWrongParameterType = this.getClass().getDeclaredMethod("methodWithWrongParameterType",
				Integer.class);
		MethodParameter methodParameter = new MethodParameter(methodWithWrongParameterType, 0);

		// Act
		boolean supportsParameter = resolver.supportsParameter(methodParameter);

		// Assert
		assertThat(supportsParameter).isFalse();
	}

	@SuppressWarnings("EmptyMethod")
	private void methodWithWrongParameterType(@NotificationSubject Integer message) {
	}

}
