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
package io.awspring.cloud.sqs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Factory util class to construct {@link org.springframework.messaging.converter.MessageConverter} either Jackson 2 or
 * Jackson 3 specific.
 * @author Matej Nedic
 * @since 4.0.0
 */
public class JacksonAbstractMessageConverterFactory {
	@Deprecated
	public static MappingJackson2MessageConverter createLegacyJackson2MessageConverter(
			@Nullable ObjectMapper objectMapper) {
		MappingJackson2MessageConverter jacksonMessageConverter = new MappingJackson2MessageConverter();
		jacksonMessageConverter.setSerializedPayloadClass(String.class);
		jacksonMessageConverter.setStrictContentTypeMatch(false);
		if (objectMapper != null) {
			jacksonMessageConverter.setObjectMapper(objectMapper);
		}
		return jacksonMessageConverter;
	}

	public static JacksonJsonMessageConverter createJacksonJsonMessageConverter(JsonMapper jsonMapper) {
		JacksonJsonMessageConverter jacksonMessageConverter;
		if (jsonMapper != null) {
			jacksonMessageConverter = new JacksonJsonMessageConverter(jsonMapper);
		}
		else {
			jacksonMessageConverter = new JacksonJsonMessageConverter();
		}
		jacksonMessageConverter.setSerializedPayloadClass(String.class);
		jacksonMessageConverter.setStrictContentTypeMatch(false);
		return jacksonMessageConverter;
	}

	public static JacksonJsonMessageConverter createDefaultMappingJacksonMessageConverter() {
		JacksonJsonMessageConverter messageConverter = new JacksonJsonMessageConverter();
		messageConverter.setSerializedPayloadClass(String.class);
		messageConverter.setStrictContentTypeMatch(false);
		return messageConverter;
	}

	@Deprecated
	public static MappingJackson2MessageConverter createDefaultMappingLegacyJackson2MessageConverter() {
		MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
		messageConverter.setSerializedPayloadClass(String.class);
		messageConverter.setStrictContentTypeMatch(false);
		return messageConverter;
	}

}
