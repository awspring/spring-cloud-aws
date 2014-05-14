package org.elasticspring.messaging.support;

import org.elasticspring.messaging.support.converter.NotificationMessageConverter;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.ClassUtils;

/**
 * @author Alain Sahli
 */
public class SnsPayloadArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;

	public SnsPayloadArgumentResolver() {
		this.converter = new NotificationMessageConverter();
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return ClassUtils.isAssignable(parameter.getParameterType(), NotificationMessage.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		// TODO Alain: Does it make sense to use a message converter here? It's weird to have to pass null as type.
		return this.converter.fromMessage(message, null);
	}
}
