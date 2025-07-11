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
package io.awspring.cloud.sqs.support.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * Converter that extracts SNS notifications from SQS messages and creates {@link SnsNotification} objects.
 *
 * @author Damien Chomat
 * @since 3.4.1
 */
public class SnsNotificationConverter implements SmartMessageConverter {

	private final ObjectMapper jsonMapper;

	private final MessageConverter payloadConverter;

	/**
	 * Creates a new converter with the given payload converter and JSON mapper.
	 * @param payloadConverter the converter to use for the message payload
	 * @param jsonMapper the JSON mapper to use for parsing the SNS notification
	 */
	public SnsNotificationConverter(MessageConverter payloadConverter, ObjectMapper jsonMapper) {
		Assert.notNull(payloadConverter, "payloadConverter must not be null");
		Assert.notNull(jsonMapper, "jsonMapper must not be null");
		this.payloadConverter = payloadConverter;
		this.jsonMapper = jsonMapper;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object fromMessage(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		Assert.notNull(message, "message must not be null");
		Assert.notNull(targetClass, "target class must not be null");

		Object payload = message.getPayload();

		if (payload instanceof List messages) {
			return fromGenericMessages(messages, targetClass, conversionHint);
		}
		else {
			return fromGenericMessage((GenericMessage<?>) message, targetClass, conversionHint);
		}
	}

	private Object fromGenericMessages(List<GenericMessage<?>> messages, Class<?> targetClass,
			@Nullable Object conversionHint) {
		Type resolvedType = getResolvedType(targetClass, conversionHint);
		Class<?> resolvedClazz = ResolvableType.forType(resolvedType).resolve();

		Object hint = targetClass.isAssignableFrom(List.class) && conversionHint instanceof MethodParameter mp
				? mp.nested()
				: conversionHint;

		return messages.stream().map(message -> fromGenericMessage(message, resolvedClazz, hint)).toList();
	}

	private Object fromGenericMessage(GenericMessage<?> message, Class<?> targetClass,
			@Nullable Object conversionHint) {
		try {
			Type payloadType = getPayloadType(targetClass, conversionHint);
			Class<?> payloadClass = ResolvableType.forType(payloadType).resolve();

			return jsonMapper.readValue(message.getPayload().toString(),
					jsonMapper.getTypeFactory().constructParametricType(SnsNotification.class, payloadClass));
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Error converting SNS notification: " + e.getMessage(), e);
		}
	}

	private Type getPayloadType(Class<?> targetClass, @Nullable Object conversionHint) {
		if (conversionHint instanceof MethodParameter parameter) {
			ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
			if (resolvableType.isAssignableFrom(SnsNotification.class)) {
				return resolvableType.getGeneric(0).getType();
			}
		}
		return String.class;
	}

	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		return fromMessage(message, targetClass, null);
	}

	@Override
	public Message<?> toMessage(Object payload, MessageHeaders headers) {
		throw new UnsupportedOperationException(
				"This converter only supports reading SNS notifications and not writing them");
	}

	@Override
	public Message<?> toMessage(Object payload, MessageHeaders headers, Object conversionHint) {
		throw new UnsupportedOperationException(
				"This converter only supports reading SNS notifications and not writing them");
	}

	private static Type getResolvedType(Class<?> targetClass, @Nullable Object conversionHint) {
		if (conversionHint instanceof MethodParameter param) {
			param = param.nestedIfOptional();
			if (Message.class.isAssignableFrom(param.getParameterType())) {
				param = param.nested();
			}
			Type genericParameterType = param.getNestedGenericParameterType();
			Class<?> contextClass = param.getContainingClass();
			Type resolveType = GenericTypeResolver.resolveType(genericParameterType, contextClass);
			if (resolveType instanceof ParameterizedType parameterizedType) {
				return parameterizedType.getActualTypeArguments()[0];
			}
			else {
				return resolveType;
			}
		}
		return targetClass;
	}
}
