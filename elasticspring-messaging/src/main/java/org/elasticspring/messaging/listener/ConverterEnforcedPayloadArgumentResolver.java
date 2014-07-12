/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.listener;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.annotation.support.PayloadArgumentResolver;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;

/**
 * This class delegates everything to the {@link org.springframework.messaging.handler.annotation.support.PayloadArgumentResolver}
 * class except when the parameter type is of type {@code String} as we want also {@code String} to be converted.
 *
 * @author Alain Sahli
 */
public class ConverterEnforcedPayloadArgumentResolver extends PayloadArgumentResolver {

	private final MessageConverter converter;

	public ConverterEnforcedPayloadArgumentResolver(MessageConverter converter, Validator validator) {
		super(converter, validator);
		this.converter = converter;
	}

	@Override
	public Object resolveArgument(MethodParameter param, Message<?> message) throws Exception {
		Payload annotation = param.getParameterAnnotation(Payload.class);
		if ((annotation != null) && StringUtils.hasText(annotation.value())) {
			throw new IllegalStateException("@Payload SpEL expressions not supported by this resolver.");
		}

		Object payload = message.getPayload();

		if (isEmptyPayload(payload)) {
			if (annotation == null || annotation.required()) {
				String paramName = getParameterName(param);
				BindingResult bindingResult = new BeanPropertyBindingResult(payload, paramName);
				bindingResult.addError(new ObjectError(paramName, "@Payload param is required"));
				throw new MethodArgumentNotValidException(message, param, bindingResult);
			} else {
				return null;
			}
		}

		Class<?> targetClass = param.getParameterType();
		Object convertedPayload = this.converter.fromMessage(message, targetClass);
		if (convertedPayload == null) {
			throw new MessageConversionException(message,
					"No converter found to convert to " + targetClass + ", message=" + message, null);
		}
		validate(message, param, convertedPayload);

		return convertedPayload;
	}

	private String getParameterName(MethodParameter param) {
		String paramName = param.getParameterName();
		return (paramName == null ? "Arg " + param.getParameterIndex() : paramName);
	}

}
