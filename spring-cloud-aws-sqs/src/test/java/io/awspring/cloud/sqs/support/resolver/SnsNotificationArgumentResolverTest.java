/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.support.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.support.converter.SnsNotification;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;

/**
 * Tests for {@link SnsNotificationArgumentResolver}.
 *
 * @author Damien Chomat
 */
class SnsNotificationArgumentResolverTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private SnsNotificationArgumentResolver resolver;

	@Mock
	private MessageConverter messageConverter;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		resolver = new SnsNotificationArgumentResolver(messageConverter, objectMapper);
	}

	@Test
	void shouldSupportParameter() throws Exception {
		// Arrange
		Method method = TestController.class.getMethod("handleMessage",
				io.awspring.cloud.sqs.support.converter.SnsNotification.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		// Act
		boolean result = resolver.supportsParameter(parameter);

		// Assert
		assertThat(result).isTrue();
	}

	@Test
	void shouldResolveArgument() throws Exception {
		// Arrange
		Method method = TestController.class.getMethod("handleMessage",
				io.awspring.cloud.sqs.support.converter.SnsNotification.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		String snsJson = "{" + "\"Type\": \"Notification\"," + "\"MessageId\": \"message-id\","
				+ "\"TopicArn\": \"topic-arn\"," + "\"Subject\": \"subject\"," + "\"Message\": \"message\","
				+ "\"Timestamp\": \"2023-01-01T00:00:00Z\"" + "}";

		Message<String> message = new GenericMessage<>(snsJson);

		when(messageConverter.fromMessage(any(), any())).thenReturn("message");

		// Act
		Object result = resolver.resolveArgument(parameter, message);

		// Assert
		assertThat(result).isInstanceOf(SnsNotification.class);
		SnsNotification<String> notification = (SnsNotification<String>) result;
		assertThat(notification.getMessageId()).isEqualTo("message-id");
		assertThat(notification.getTopicArn()).isEqualTo("topic-arn");
		assertThat(notification.getSubject()).isEqualTo(Optional.of("subject"));
		assertThat(notification.getMessage()).isEqualTo("message");
	}

	static class TestController {
		public void handleMessage(SnsNotification<String> notification) {
			// Method for testing
		}
	}
}
