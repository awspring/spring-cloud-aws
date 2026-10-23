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
package io.awspring.cloud.s3;

import java.io.InputStream;
import org.springframework.util.Assert;
import org.springframework.core.ParametrizedTypeReference;
import software.amazon.awssdk.core.sync.RequestBody;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson based implementation of {@link S3ObjectConverter}. Serializes/deserializes objects to/from JSON.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public class Jackson2JsonS3ObjectConverter implements S3ObjectConverter {
	private final JsonMapper jsonMapper;

	public Jackson2JsonS3ObjectConverter(JsonMapper jsonMapper) {
		Assert.notNull(jsonMapper, "jsonMapper is required");
		this.jsonMapper = jsonMapper;
	}

	@Override
	public <T> RequestBody write(T object) {
		Assert.notNull(object, "object is required");
		try {
			return RequestBody.fromBytes(jsonMapper.writeValueAsBytes(object));
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
			return jsonMapper.readValue(is, clazz);
		}
		catch (JacksonException e) {
			throw new S3Exception("Failed to deserialize object from JSON", e);
		}
	}

	@Override
	public <T> T read(InputStream is, ParametrizedTypeReference<T> valueTypeRef) {
		Assert.notNull(is, "InputStream is required");
		Assert.notNull(valueTypeRef, "valueTypeRef is required");
		try {
			return jsonMapper.readValue(is, jsonMapper.constructType(valueTypeRef.getType()));
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
