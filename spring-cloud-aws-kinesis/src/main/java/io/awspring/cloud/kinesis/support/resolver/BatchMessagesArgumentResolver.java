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
package io.awspring.cloud.kinesis.support.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
public class BatchMessagesArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter messageConverter;

	public BatchMessagesArgumentResolver(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "messageConverter must not be null");
		this.messageConverter = messageConverter;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Collection.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		Collection<Message<?>> messages = (Collection<Message<?>>) message.getPayload();
		ResolvableType elementType = ResolvableType.forMethodParameter(parameter).asCollection().getGeneric(0);
		Class<?> elementClass = elementType.resolve();
		if (elementClass == null || Object.class.equals(elementClass)) {
			return messages;
		}
		if (Message.class.isAssignableFrom(elementClass)) {
			Class<?> payloadClass = elementType.getGeneric(0).resolve();
			if (payloadClass == null || Object.class.equals(payloadClass)) {
				return messages;
			}
			List<Message<?>> converted = new ArrayList<>(messages.size());
			int index = 0;
			for (Message<?> element : messages) {
				converted.add(MessageBuilder.createMessage(convertPayload(element, payloadClass, index++),
						element.getHeaders()));
			}
			return converted;
		}
		List<Object> converted = new ArrayList<>(messages.size());
		int index = 0;
		for (Message<?> element : messages) {
			converted.add(convertPayload(element, elementClass, index++));
		}
		return converted;
	}

	private Object convertPayload(Message<?> message, Class<?> targetClass, int index) {
		if (targetClass.isInstance(message.getPayload())) {
			return message.getPayload();
		}
		@Nullable
		Object converted = this.messageConverter.fromMessage(message, targetClass);
		if (converted == null) {
			throw new MessageConversionException(message, "Failed to convert batch record at index " + index + " to "
					+ targetClass.getName() + "; the message converter returned null");
		}
		return converted;
	}

}
