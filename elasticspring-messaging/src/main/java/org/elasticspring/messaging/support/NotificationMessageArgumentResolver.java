package org.elasticspring.messaging.support;

import org.elasticspring.messaging.config.annotation.NotificationMessage;
import org.elasticspring.messaging.support.converter.NotificationRequestConverter;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.ClassUtils;

/**
 * @author Alain Sahli
 */
public class NotificationMessageArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;

	public NotificationMessageArgumentResolver() {
		this.converter = new NotificationRequestConverter();
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(NotificationMessage.class) &&
				ClassUtils.isAssignable(String.class, parameter.getParameterType()));
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		return ((NotificationRequestConverter.NotificationRequest) this.converter.fromMessage(message, null)).getMessage();
	}
}
