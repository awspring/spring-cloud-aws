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
package io.awspring.cloud.sqs;

import io.awspring.cloud.core.support.JacksonPresent;
import io.awspring.cloud.sqs.config.MessageConverterFactory;
import io.awspring.cloud.sqs.config.legacy.LegacyJackson2MessageConverterFactory;
import org.springframework.messaging.converter.MessageConverter;

public class DefaultMessageConverterConfiguration {
	public static MessageConverter createDefaultMessageConverter() {
		if (JacksonPresent.isJackson3Present()) {
			return MessageConverterFactory.createDefaultMappingJacksonMessageConverter();
		}
		else if (JacksonPresent.isJackson2Present()) {
			return LegacyJackson2MessageConverterFactory.createDefaultMappingLegacyJackson2MessageConverter();
		}
		throw new IllegalStateException("Sqs integration requires a Jackson 2 or Jackson 3 library on the classpath");
	}
}
