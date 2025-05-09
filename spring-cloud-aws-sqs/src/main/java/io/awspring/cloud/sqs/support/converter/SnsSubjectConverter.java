/*
 * Copyright 2013-2023 the original author or authors.
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
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author Alexander Nebel
 * @since 3.3.1
 */
public class SnsSubjectConverter implements MessageConverter {

	private final ObjectMapper objectMapper;

	public SnsSubjectConverter(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "jsonMapper must not be null");
		this.objectMapper = objectMapper;
	}

	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		Assert.notNull(message, "message must not be null");
		Assert.notNull(targetClass, "target class must not be null");

		Object payload = message.getPayload();

		if (!ClassUtils.isAssignable(targetClass, String.class)) {
			throw new MessageConversionException("Subject can only be injected into String assignable Types", null);
		}
		if (payload instanceof List) {
			throw new MessageConversionException("Conversion of List is not supported", null);
		}

		var snsJsonNode = new SnsJsonNode(objectMapper, message.getPayload().toString());
		return snsJsonNode.getSubjectAsString();
	}

	@Override
	public Message<?> toMessage(Object payload, MessageHeaders headers) {
		throw new UnsupportedOperationException(
				"This converter only supports reading a SNS notification and not writing them");
	}
}
