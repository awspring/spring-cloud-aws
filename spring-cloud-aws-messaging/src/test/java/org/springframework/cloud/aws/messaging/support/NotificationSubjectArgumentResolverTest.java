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

package org.springframework.cloud.aws.messaging.support;

import java.lang.reflect.Method;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.cloud.aws.messaging.config.annotation.NotificationSubject;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationSubjectArgumentResolverTest {

	@Test
	public void supportsParameter_withNotificationSubjectMethodParameter_shouldReturnTrue()
			throws Exception {
		// Arrange
		NotificationSubjectArgumentResolver notificationSubjectArgumentResolver = new NotificationSubjectArgumentResolver();
		Method methodWithNotificationSubjectArgument = this.getClass()
				.getDeclaredMethod("methodWithNotificationSubjectArgument", String.class);
		MethodParameter methodParameter = new MethodParameter(
				methodWithNotificationSubjectArgument, 0);

		// Act
		boolean result = notificationSubjectArgumentResolver
				.supportsParameter(methodParameter);

		// Assert
		assertThat(result).isTrue();
	}

	@SuppressWarnings("EmptyMethod")
	@RuntimeUse
	private void methodWithNotificationSubjectArgument(
			@NotificationSubject String subject) {
	}

	@Test
	public void supportsParameter_withWrongMethodParameter_shouldReturnFalse()
			throws Exception {
		// Arrange
		NotificationSubjectArgumentResolver notificationSubjectArgumentResolver = new NotificationSubjectArgumentResolver();
		Method methodWithMissingAnnotation = this.getClass()
				.getDeclaredMethod("methodWithMissingAnnotation", String.class);
		MethodParameter methodParameter = new MethodParameter(methodWithMissingAnnotation,
				0);

		// Act
		boolean result = notificationSubjectArgumentResolver
				.supportsParameter(methodParameter);

		// Assert
		assertThat(result).isFalse();
	}

	@SuppressWarnings("EmptyMethod")
	@RuntimeUse
	private void methodWithMissingAnnotation(String subject) {
	}

	@Test
	public void supportsParameter_withWrongParameterType_shouldReturnFalse()
			throws Exception {
		// Arrange
		NotificationSubjectArgumentResolver notificationSubjectArgumentResolver = new NotificationSubjectArgumentResolver();
		Method methodWithWrongParameterType = this.getClass()
				.getDeclaredMethod("methodWithWrongParameterType", Long.class);
		MethodParameter methodParameter = new MethodParameter(
				methodWithWrongParameterType, 0);

		// Act
		boolean result = notificationSubjectArgumentResolver
				.supportsParameter(methodParameter);

		// Assert
		assertThat(result).isFalse();
	}

	@SuppressWarnings("EmptyMethod")
	@RuntimeUse
	private void methodWithWrongParameterType(@NotificationSubject Long subject) {
	}

	@Test
	public void resolveArgument_withValidRequestPayload_shouldReturnNotificationSubject()
			throws Exception {
		// Arrange
		NotificationSubjectArgumentResolver notificationSubjectArgumentResolver = new NotificationSubjectArgumentResolver();
		Method methodWithNotificationSubjectArgument = this.getClass()
				.getDeclaredMethod("methodWithNotificationSubjectArgument", String.class);
		MethodParameter methodParameter = new MethodParameter(
				methodWithNotificationSubjectArgument, 0);

		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Subject", "My subject!");
		jsonObject.put("Message", "message");
		String payload = jsonObject.toString();
		Message<String> message = MessageBuilder.withPayload(payload).build();

		// Act
		Object result = notificationSubjectArgumentResolver
				.resolveArgument(methodParameter, message);

		// Assert
		assertThat(String.class.isInstance(result)).isTrue();
		assertThat(result).isEqualTo("My subject!");
	}

}
