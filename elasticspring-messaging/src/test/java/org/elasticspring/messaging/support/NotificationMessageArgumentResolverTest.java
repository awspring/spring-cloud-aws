package org.elasticspring.messaging.support;

import org.codehaus.jettison.json.JSONObject;
import org.elasticspring.messaging.config.annotation.NotificationMessage;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotificationMessageArgumentResolverTest {

	@Test
	public void supportsParameter_withNotificationMessageMethodParameter_shouldReturnTrue() throws Exception {
		// Arrange
		NotificationMessageArgumentResolver notificationMessageArgumentResolver = new NotificationMessageArgumentResolver();
		Method methodWithNotificationMessageArgument = this.getClass().getDeclaredMethod("methodWithNotificationMessageArgument", String.class);
		MethodParameter methodParameter = new MethodParameter(methodWithNotificationMessageArgument, 0);

		// Act
		boolean result = notificationMessageArgumentResolver.supportsParameter(methodParameter);

		// Assert
		assertTrue(result);
	}

	private void methodWithNotificationMessageArgument(@NotificationMessage String message) {
	}

	@Test
	public void supportsParameter_withWrongMethodParameter_shouldReturnFalse() throws Exception {
		// Arrange
		NotificationMessageArgumentResolver notificationMessageArgumentResolver = new NotificationMessageArgumentResolver();
		Method methodWithWrongMessageArgument = this.getClass().getDeclaredMethod("methodWithWrongMessageArgument", String.class);
		MethodParameter methodParameter = new MethodParameter(methodWithWrongMessageArgument, 0);

		// Act
		boolean result = notificationMessageArgumentResolver.supportsParameter(methodParameter);

		// Assert
		assertFalse(result);
	}

	private void methodWithWrongMessageArgument(String test) {
	}

	@Test
	public void resolveArgument_withValidMessagePayload_shouldReturnNotificationMessage() throws Exception {
		// Arrange
		NotificationMessageArgumentResolver notificationMessageArgumentResolver = new NotificationMessageArgumentResolver();
		Method methodWithNotificationMessageArgument = this.getClass().getDeclaredMethod("methodWithNotificationMessageArgument", String.class);
		MethodParameter methodParameter = new MethodParameter(methodWithNotificationMessageArgument, 0);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Message", "Hello World!");
		String payload = jsonObject.toString();
		Message<String> message = MessageBuilder.withPayload(payload).build();

		// Act
		Object result = notificationMessageArgumentResolver.resolveArgument(methodParameter, message);

		// Assert
		assertTrue(String.class.isInstance(result));
		assertEquals("Hello World!", result);
	}

}