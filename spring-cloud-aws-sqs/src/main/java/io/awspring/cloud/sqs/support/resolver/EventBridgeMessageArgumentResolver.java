package io.awspring.cloud.sqs.support.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.EventBridgeMessage;
import io.awspring.cloud.sqs.support.converter.EventBridgeMessageConverter;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

public class EventBridgeMessageArgumentResolver implements HandlerMethodArgumentResolver {
	private final SmartMessageConverter converter;

	public EventBridgeMessageArgumentResolver(MessageConverter converter, ObjectMapper jsonMapper) {
		this.converter = new EventBridgeMessageConverter(converter, jsonMapper);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(EventBridgeMessage.class);
	}

	@Override
	public Object resolveArgument(MethodParameter par, Message<?> msg) {
		Class<?> parameterType = par.getParameterType();
		return this.converter.fromMessage(msg, parameterType, par);
	}
}
