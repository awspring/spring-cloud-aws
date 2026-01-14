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
package io.awspring.cloud.secretsmanager;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Jackson 2 wrapper for deserializing secret from String.
 *
 * @author Maciej Walkowiak
 * @since 4.0.0
 */
@Deprecated
public class Jackson2SecretValueReader implements SecretValueReader {
	private final ObjectMapper objectMapper;

	public Jackson2SecretValueReader() {
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public Map<String, Object> readSecretValue(String secretString) {
		try {
			return objectMapper.readValue(secretString, new TypeReference<>() {
			});
		}
		catch (JacksonException e) {
			throw new SecretParseException(e);
		}
	}
}
