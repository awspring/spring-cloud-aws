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

package org.springframework.cloud.aws.messaging.endpoint;

import java.lang.reflect.Method;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.cloud.aws.messaging.config.annotation.NotificationSubject;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationSubjectHandlerMethodArgumentResolverTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void resolveArgument_wrongMessageType_reportsErrors() throws Exception {
		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(
				"@NotificationMessage annotated parameters are only allowed");

		NotificationSubjectHandlerMethodArgumentResolver resolver = new NotificationSubjectHandlerMethodArgumentResolver();

		byte[] subscriptionRequestJsonContent = FileCopyUtils.copyToByteArray(
				new ClassPathResource("subscriptionConfirmation.json", getClass())
						.getInputStream());
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setContent(subscriptionRequestJsonContent);

		MethodParameter methodParameter = new MethodParameter(
				ReflectionUtils.findMethod(NotificationMethods.class,
						"subscriptionMethod", NotificationStatus.class),
				0);

		// Act
		resolver.resolveArgument(methodParameter, null,
				new ServletWebRequest(servletRequest), null);

		// Assert
	}

	@Test
	public void resolveArgument_notificationMessageTypeWithSubject_reportsErrors()
			throws Exception {
		// Arrange
		NotificationSubjectHandlerMethodArgumentResolver resolver = new NotificationSubjectHandlerMethodArgumentResolver();

		byte[] subscriptionRequestJsonContent = FileCopyUtils.copyToByteArray(
				new ClassPathResource("notificationMessage.json", getClass())
						.getInputStream());
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setContent(subscriptionRequestJsonContent);

		MethodParameter methodParameter = new MethodParameter(
				ReflectionUtils.findMethod(NotificationMethods.class,
						"subscriptionMethod", NotificationStatus.class),
				0);

		// Act
		Object argument = resolver.resolveArgument(methodParameter, null,
				new ServletWebRequest(servletRequest), null);

		// Assert
		assertThat(argument).isEqualTo("asdasd");
	}

	@Test
	public void supportsParameter_withWrongParameterType_shouldReturnFalse()
			throws Exception {
		// Arrange
		NotificationSubjectHandlerMethodArgumentResolver resolver = new NotificationSubjectHandlerMethodArgumentResolver();
		Method methodWithWrongParameterType = this.getClass()
				.getDeclaredMethod("methodWithWrongParameterType", Integer.class);
		MethodParameter methodParameter = new MethodParameter(
				methodWithWrongParameterType, 0);

		// Act
		boolean supportsParameter = resolver.supportsParameter(methodParameter);

		// Assert
		assertThat(supportsParameter).isFalse();
	}

	@SuppressWarnings("EmptyMethod")
	@RuntimeUse
	private void methodWithWrongParameterType(@NotificationSubject Integer message) {
	}

}
