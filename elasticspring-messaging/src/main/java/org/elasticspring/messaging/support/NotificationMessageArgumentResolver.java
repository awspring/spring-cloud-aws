package org.elasticspring.messaging.support;

import org.elasticspring.messaging.config.annotation.NotificationMessage;
import org.elasticspring.messaging.support.converter.NotificationMessageConverter;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * @author Alain Sahli
 */
public class NotificationMessageArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;

	public NotificationMessageArgumentResolver() {
		this.converter = new NotificationMessageConverter();
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(NotificationMessage.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		return this.converter.fromMessage(message, parameter.getParameterType());
	}
}
