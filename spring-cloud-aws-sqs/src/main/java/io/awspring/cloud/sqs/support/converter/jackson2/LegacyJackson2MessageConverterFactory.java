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
package io.awspring.cloud.sqs.support.converter.jackson2;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.config.JacksonAbstractMessageConverterFactory;
import io.awspring.cloud.sqs.support.converter.AbstractMessageConverterFactory;
import org.springframework.messaging.converter.MessageConverter;

@Deprecated
public class LegacyJackson2MessageConverterFactory extends AbstractMessageConverterFactory {

	private ObjectMapper objectMapper;

	public LegacyJackson2MessageConverterFactory(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public MessageConverter create() {
		return JacksonAbstractMessageConverterFactory.createLegacyJackson2MessageConverter(objectMapper);
	}
}
