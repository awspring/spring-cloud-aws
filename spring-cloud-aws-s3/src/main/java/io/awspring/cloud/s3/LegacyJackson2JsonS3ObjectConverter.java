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
package io.awspring.cloud.s3;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core..type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.sync.RequestBody;

/**
 * Jackson 2 based implementation of {@link S3ObjectConverter}. Serializes/deserializes objects to/from JSON.
 *
 * @author Maciej Walkowiak
 * @since 4.0
 */
@Deprecated
public class LegacyJackson2JsonS3ObjectConverter implements S3ObjectConverter {
	private final ObjectMapper objectMapper;

	public LegacyJackson2JsonS3ObjectConverter(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "objectMapper is required");
		this.objectMapper = objectMapper;
	}

	@Override
	public <T> RequestBody write(T object) {
		Assert.notNull(object, "object is required");
		try {
			return RequestBody.fromBytes(objectMapper.writeValueAsBytes(object));
		}
		catch (JacksonException e) {
			throw new S3Exception("Failed to serialize object to JSON", e);
		}
	}

	@Override
	public <T> T read(InputStream is, Class<T> clazz) {
		Assert.notNull(is, "InputStream is required");
		Assert.notNull(clazz, "Clazz is required");
		try {
			return objectMapper.readValue(is, clazz);
		}
		catch (IOException e) {
			throw new S3Exception("Failed to deserialize object from JSON", e);
		}
	}

	/**
	 * Reads S3 object from the input stream into a Java object.
	 * @param is - the input stream
	 * @param valueTypeRef - the type reference
	 * @param <T> - the the type of the object
	 * @return deserialized object
	 */
	public <T> T read(InputStream is, TypeReference<T> valueTypeRef) {
		Assert.notNull(is, "InputStream is required");
		Assert.notNull(valueTypeRef, "ValueTypeRef is required");
		try {
			return objectMapper.readValue(is, valueTypeRef);
		}
		catch (IOException e) {
			throw new S3Exception("Failed to deserialize object from JSON", e);
		}
	}

	@Override
	public String contentType() {
		return "application/json";
	}
}
