/*
 * Copyright 2013-2024 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;

/**
 * {@link SmartMessageConverter} implementation that returns the payload unchanged if the target class for Serialization
 * / Deserialization matches the payload class.
 *
 * @author Tomaz Fernandes
 * @since 3.3
 */
public class SimpleClassMatchingMessageConverter extends AbstractMessageConverter {

	@Override
	protected boolean supports(Class<?> clazz) {
		return true;
	}

	@Nullable
	@Override
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		Object payload = message.getPayload();
		return payload.getClass().isAssignableFrom(targetClass) ? payload : null;
	}

	@Nullable
	@Override
	protected Object convertToInternal(Object payload, @Nullable MessageHeaders headers,
			@Nullable Object conversionHint) {
		return payload.getClass().isAssignableFrom(getSerializedPayloadClass()) ? payload : null;
	}
}
