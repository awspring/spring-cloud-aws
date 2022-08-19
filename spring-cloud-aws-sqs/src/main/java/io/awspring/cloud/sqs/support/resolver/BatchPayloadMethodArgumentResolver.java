/*
 * Copyright 2013-2022 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

/**
 * {@link HandlerMethodArgumentResolver} implementation for resolving {@link java.util.List} arguments.
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class BatchPayloadMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;

	@Nullable
	private final Validator validator;

	/**
	 * Create a new {@code BatchPayloadArgumentResolver} with the given {@link MessageConverter}.
	 * @param messageConverter the MessageConverter to use (required)
	 */
	public BatchPayloadMethodArgumentResolver(MessageConverter messageConverter) {
		this(messageConverter, null);
	}

	/**
	 * Create a new {@code BatchPayloadArgumentResolver} with the given {@link MessageConverter} and {@link Validator}.
	 * @param messageConverter the MessageConverter to use (required)
	 * @param validator the Validator to use (optional)
	 */
	public BatchPayloadMethodArgumentResolver(MessageConverter messageConverter, @Nullable Validator validator) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.converter = messageConverter;
		this.validator = validator;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> parameterClass = ResolvableType.forType(parameter.getGenericParameterType()).toClass();
		return Collection.class.isAssignableFrom(parameterClass);
	}

	// @formatter:off
	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		Class<?> targetClass = resolveTargetClass(parameter);
		boolean isMessageParameter = Message.class
				.isAssignableFrom(ResolvableType.forMethodParameter(parameter).getNested(2).toClass());
		return getPayloadAsCollection(message)
			.stream()
			.filter(msg -> Message.class.isAssignableFrom(msg.getClass()))
			.map(Message.class::cast)
			.map(msg -> convertAndValidatePayload(parameter, msg, targetClass, isMessageParameter))
			.collect(Collectors.toList());
	}
	// @formatter:on

	private Collection<?> getPayloadAsCollection(Message<?> message) {
		Object payload = message.getPayload();
		if (payload instanceof Collection && !((Collection<?>) payload).isEmpty()) {
			return (Collection<?>) payload;
		}
		throw new IllegalArgumentException("Payload must be a non-empty Collection: " + message);
	}

	private Object convertAndValidatePayload(MethodParameter parameter, Message<?> message, Class<?> targetClass,
			boolean isMessageParameter) {
		Object convertedPayload = getConvertedPayload(message, targetClass);
		if (convertedPayload == null) {
			throw new MessageConversionException(message,
					"Cannot convert from [" + message.getPayload().getClass().getName() + "] to ["
							+ targetClass.getName() + "] for " + message);
		}
		validate(message, parameter, convertedPayload);
		return isMessageParameter ? MessageBuilder.createMessage(convertedPayload, message.getHeaders())
				: convertedPayload;
	}

	private Object getConvertedPayload(Message<?> message, Class<?> targetClass) {
		return this.converter.fromMessage(message, targetClass);
	}

	private String getParameterName(MethodParameter param) {
		String paramName = param.getParameterName();
		return (paramName != null ? paramName : "Arg " + param.getParameterIndex());
	}

	private final Map<MethodParameter, Class<?>> targetClassCache = new ConcurrentHashMap<>();

	private Class<?> resolveTargetClass(MethodParameter parameter) {
		return targetClassCache.computeIfAbsent(parameter, param -> {
			ResolvableType resolvableType = ResolvableType.forType(parameter.getGenericParameterType());
			Class<?> collectionGenericClass = throwIfObject(resolvableType.getNested(2).toClass(), parameter);
			if (Message.class.isAssignableFrom(collectionGenericClass)) {
				return throwIfObject(resolvableType.getNested(3).toClass(), parameter);
			}
			return collectionGenericClass;
		});
	}

	private Class<?> throwIfObject(Class<?> classToCompare, MethodParameter parameter) {
		if (Object.class.equals(classToCompare)) {
			throw new IllegalArgumentException(String.format(
					"Could not resolve target for parameter %s in method %s from class %s."
							+ " Generic types are required.",
					parameter.getParameterName(), parameter.getMethod().getName(), parameter.getContainingClass()));
		}
		return classToCompare;
	}

	/**
	 * Validate the payload if applicable.
	 * <p>
	 * The default implementation checks for {@code @javax.validation.Valid}, Spring's {@link Validated}, and custom
	 * annotations whose name starts with "Valid".
	 * @param message the currently processed message
	 * @param parameter the method parameter
	 * @param target the target payload object
	 * @throws MethodArgumentNotValidException in case of binding errors
	 */
	protected void validate(Message<?> message, MethodParameter parameter, Object target) {
		if (this.validator == null) {
			return;
		}
		for (Annotation ann : parameter.getParameterAnnotations()) {
			Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
				Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] { hints });
				BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target,
						getParameterName(parameter));
				if (!ObjectUtils.isEmpty(validationHints) && this.validator instanceof SmartValidator) {
					((SmartValidator) this.validator).validate(target, bindingResult, validationHints);
				}
				else {
					this.validator.validate(target, bindingResult);
				}
				if (bindingResult.hasErrors()) {
					throw new MethodArgumentNotValidException(message, parameter, bindingResult);
				}
				break;
			}
		}
	}

}
