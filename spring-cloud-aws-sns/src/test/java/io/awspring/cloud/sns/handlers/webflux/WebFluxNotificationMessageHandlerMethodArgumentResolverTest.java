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

import io.awspring.cloud.sns.handlers.NotificationStatus;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Matej Nedic
 */
class WebFluxNotificationMessageHandlerMethodArgumentResolverTest {

	@Test
	void resolveArgument_wrongMessageType_reportsErrors() throws Exception {
		// Arrange
		WebFluxNotificationMessageHandlerMethodArgumentResolver resolver = new WebFluxNotificationMessageHandlerMethodArgumentResolver();

		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("subscriptionConfirmation.json"));

		MockServerHttpRequest request = MockServerHttpRequest.post("/").contentType(MediaType.APPLICATION_JSON)
				.body(new String(subscriptionRequestJsonContent));
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		MethodParameter methodParameter = new MethodParameter(ReflectionUtils
				.findMethod(WebFluxNotificationMethods.class, "subscriptionMethod", NotificationStatus.class), 0);

		// Assert
		assertThatThrownBy(() -> resolver.resolveArgument(methodParameter, null, exchange).block())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("@NotificationMessage annotated parameters are only allowed");
	}

	@Test
	void resolveArgument_notificationMessageTypeWithSubject_reportsErrors() throws Exception {
		// Arrange
		WebFluxNotificationMessageHandlerMethodArgumentResolver resolver = new WebFluxNotificationMessageHandlerMethodArgumentResolver();

		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("notificationMessage.json"));

		MockServerHttpRequest request = MockServerHttpRequest.post("/").contentType(MediaType.APPLICATION_JSON)
				.body(new String(subscriptionRequestJsonContent));
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		MethodParameter methodParameter = new MethodParameter(ReflectionUtils
				.findMethod(WebFluxNotificationMethods.class, "handleMethod", String.class, String.class), 0);

		// Act
		Object argument = resolver.resolveArgument(methodParameter, null, exchange).block();

		// Assert
		assertThat(argument).isEqualTo("asdasd");
	}

	@Test
	void supportsParameter_withIntegerParameterType_shouldReturnTrue() throws Exception {
		// Arrange
		WebFluxNotificationMessageHandlerMethodArgumentResolver resolver = new WebFluxNotificationMessageHandlerMethodArgumentResolver();
		MethodParameter methodParameter = new MethodParameter(ReflectionUtils
				.findMethod(WebFluxNotificationMethods.class, "methodWithIntegerParameterType", Integer.class), 0);

		// Act
		boolean supportsParameter = resolver.supportsParameter(methodParameter);

		// Assert
		assertThat(supportsParameter).isTrue();
	}

	@SuppressWarnings("unchecked")
	@Test
	void resolveArgument_withCustomContentType_deserializesJsonMessage() throws Exception {
		// Arrange
		WebFluxNotificationMessageHandlerMethodArgumentResolver resolver = new WebFluxNotificationMessageHandlerMethodArgumentResolver();

		byte[] complexObjectJsonContent = FileCopyUtils.copyToByteArray(
				getClass().getClassLoader().getResourceAsStream("notificationMessage-complexObject.json"));

		MockServerHttpRequest request = MockServerHttpRequest.post("/").contentType(MediaType.APPLICATION_JSON)
				.body(new String(complexObjectJsonContent));
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		MethodParameter methodParameter = new MethodParameter(
				ReflectionUtils.findMethod(WebFluxNotificationMethods.class, "handleJsonMethod", Map.class), 0);

		// Act
		Map<String, String> argument = (Map<String, String>) resolver.resolveArgument(methodParameter, null, exchange)
				.block();

		// Assert
		assertThat(argument).containsEntry("firstName", "Agim").containsEntry("lastName", "Emruli");
	}

}
